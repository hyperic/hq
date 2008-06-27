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

package org.hyperic.hq.plugin.websphere;

import java.util.Properties;
import java.util.StringTokenizer;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.util.StringUtil;

public class WebsphereMeasurementPlugin
    extends MeasurementPlugin {

    public MetricValue getValue(Metric metric)
        throws PluginException,
        MetricUnreachableException,
        MetricNotFoundException {

        if (WebsphereProductPlugin.useJMX) {
            return super.getValue(metric); //collector
        }
        else {
            MetricValue mValue = null;

            Double val = WebspherePMI.getValue(metric);

            mValue = new MetricValue(val, System.currentTimeMillis());

            return mValue;
        }
    }

    public Properties getCollectorProperties(Metric metric) {
        String domain = metric.getDomainName();
        Properties props = new Properties();
        props.putAll(metric.getProperties());
        props.putAll(metric.getObjectProperties());

        StringTokenizer tok = new StringTokenizer(domain, "/");
        props.setProperty(WebsphereProductPlugin.PROP_SERVER_NODE,
                          tok.nextToken());
        props.setProperty(WebsphereProductPlugin.PROP_SERVER_NAME,
                          tok.nextToken());

        if (tok.hasMoreTokens()) {
            //services only
            props.setProperty("type", tok.nextToken());
            if (tok.hasMoreTokens()) {
                //webapp servlet metrics
                String module = tok.nextToken();
                props.setProperty("Module", module);
            }
        }

        return props;
    }

    public String translate(String template, ConfigResponse config) {
        template = StringUtil.replace(template,
                                      "${admin.vers}",
                                      WebsphereProductPlugin.VERSION_WS5);

        return super.translate(template, config);
    }
}
