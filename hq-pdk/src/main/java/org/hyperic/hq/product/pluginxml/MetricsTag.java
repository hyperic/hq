/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.product.pluginxml;

import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

class MetricsTag
    extends ContainerTag implements XmlEndAttrHandler {

    private static final String[] OPTIONAL_ATTRS =
        new String[] { ATTR_NAME, ATTR_INCLUDE };
    
    private static final Log log = LogFactory.getLog(MetricsTag.class);
    
    String metricsName;
    private PluginData data;
    
    public String getName() {
        return "metrics";
    }
    
    MetricsTag(BaseTag parent) {
        super(parent);
        this.data = parent.data;
    }
    
    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }
    
    public XmlTagInfo[] getSubTags() {
        return getMergedSubTags(super.getSubTags(),
                                new XmlTagInfo(new MetricTag(this),
                                               XmlTagInfo.ZERO_OR_MORE));
    }
    
    public void endAttributes() throws XmlAttrException {
        if (!this.collectMetrics) {
            return;
        }
        
        String name = getAttribute(ATTR_NAME);
        
        if (isResourceParent()) {
            if (name != null) {
                String msg =
                    "metric 'name' attribute not allowed " +
                    "when nested in a " + this.parent.getName() + " tag";
                throw new XmlAttrException(msg);
            }

            this.metricsName = ((ResourceTag)this.parent).typeName;
        }
        else {
            if (name == null) {
                throw new XmlAttrException("missing metrics 'name' attribute");
            }

            this.metricsName = name;
        }

        includeMetrics(this.data, this.metricsName,
                       getAttribute(ATTR_INCLUDE)); 
    }
    
    public void endTag() {
        if (!this.collectMetrics) {
            return;
        }
        
        for (int i=0; i<this.includes.size(); i++) {
            String include = (String)this.includes.get(i);
            includeMetrics(this.data, this.metricsName, include);
        }
    }


    static void includeMetrics(PluginData data,
                               String name,
                               String includes) {
    
        if (includes != null) {
            List metrics = data.getMetrics(name, true);
            StringTokenizer tok = new StringTokenizer(includes, ",");
            while (tok.hasMoreTokens()) {
                includeMetrics(data, name, metrics, tok.nextToken());
            }
        }
    }
    
    private static void includeMetrics(PluginData data,
                                       String name,
                                       List metrics,
                                       String include) {

        List includes = data.getMetrics(include, false);

        if (includes == null) {
            log.warn(name + " include not found: " + include);
        }
        else {
            log.trace(includes.size() +
                      " metrics added for " + name +
                      " (included from " + include + ")");
            //need to clone since the template is prefixed
            //with the plugin name and the template may have
            //different filter properties applied when included.
            for (int i=0; i<includes.size(); i++) {
                MeasurementInfo metric = (MeasurementInfo)includes.get(i);
                metric = (MeasurementInfo)metric.clone();
                metric.setTemplate(data.applyFilters(metric.getTemplate()));
                metrics.add(metric);
            }
        }
    }
}
