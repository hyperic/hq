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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.Metric;
import org.hyperic.util.filter.TokenReplacer;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlUnAttrHandler;

class MetricTag
    extends BaseTag
    implements XmlEndAttrHandler, XmlUnAttrHandler {

    private static final Log log = LogFactory.getLog(MetricTag.class);

    private static final Set VALID_UNITS =
        new HashSet(Arrays.asList(MeasurementConstants.VALID_UNITS));

    private static final Set VALID_CATEGORIES =
        new HashSet(Arrays.asList(MeasurementConstants.VALID_CATEGORIES));

    private static final String[] REQUIRED_ATTRS = {
    };

    private static final String ATTR_ALIAS =
        MeasurementInfo.ATTR_ALIAS;
    
    private static final String ATTR_CAT =
        MeasurementInfo.ATTR_CATEGORY;
    
    private static final String ATTR_DEFAULTON =
        MeasurementInfo.ATTR_DEFAULTON;
    
    private static final String ATTR_INDICATOR =
        MeasurementInfo.ATTR_INDICATOR;
    
    private static final String ATTR_TYPE =
        MeasurementInfo.ATTR_COLLECTION_TYPE;
    
    private static final String ATTR_UNITS =
        MeasurementInfo.ATTR_UNITS;
    
    private static final String ATTR_INTERVAL =
        MeasurementInfo.ATTR_INTERVAL;
    
    private static final String ATTR_GROUP =
        MeasurementInfo.ATTR_GROUP;
    
    private static final String ATTR_RATE =
        MeasurementInfo.ATTR_RATE;
    
    private static final String ATTR_TEMPLATE =
        MeasurementInfo.ATTR_TEMPLATE;
    
    private static final String[] OPTIONAL_ATTRS = {
        ATTR_NAME,
        ATTR_ALIAS,
        ATTR_CAT,
        ATTR_DEFAULTON,
        ATTR_INDICATOR,
        ATTR_TYPE,
        ATTR_UNITS,
        ATTR_INTERVAL,
        ATTR_GROUP,
        ATTR_RATE,
        ATTR_TEMPLATE,
    };

    private void addFilter(String key) {
        String val = getAttribute(key);
        if (val != null) {
            this.replacer.addFilter(key, val);
        }
    }

    private String metricsName;
    private TokenReplacer replacer;
    
    MetricTag(BaseTag parent) {
        super(parent);
        this.replacer = new TokenReplacer();
    }
    
    public String getName() {
        return "metric";
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }

    public void handleUnknownAttribute(String name, String value) {
        this.replacer.addFilter(name, Metric.encode(value));
    }

    private String filter(String val) {
        //plugin.xml filters
        val = this.data.applyFilters(val);

        //replace attributes within the metric tag
        val = this.replacer.replaceTokens(val);

        return val;
    }

    //allows an attribute to be defined via <filter name="attr" ...>
    private void filterAttribute(String attr)
        throws XmlAttrException {

        //first try <metric> defined attribute
        String val = getAttribute(attr);
        if (val == null) {
            //next try <filter> defined attribute
            val = this.data.getFilter(attr);
        }

        if ((val == null) && isResourceParent()) {
            //not defined by <metric> or <filter>
            //attempt to derived from elsewhere if possible

            //check if <config include="foo"/> was defined
            //within a <platform,server,service> tag
            String configName = 
                ((ResourceTag)this.parent).configName;

            if (attr.equals(ATTR_TEMPLATE)) {
                //<config name="foo"> generates a template
                //based on its config options, use it.
                if (configName != null) {
                    val = "${" + configName + ".template}:${alias}";
                }
            }
        }
        
        if (val == null) {
            throw new XmlAttrException("Missing attribute: " + attr);
        }

        val = filter(val);

        this.props.put(attr, val);
    }
    
    public void endAttributes() throws XmlAttrException {
        if (!this.collectMetrics) {
            return;
        }
        if (isResourceParent()) {
            this.metricsName = ((ResourceTag)this.parent).typeName;
        }
        else {
            this.metricsName = ((MetricsTag)this.parent).metricsName;
        }

        if (!this.collectMetrics) {
            return;
        }

        if (isSet(ATTR_ALIAS)) {
            addFilter(ATTR_ALIAS); //name <filter> may want to use ${alias}
        }
        //allow name to be defined by a <filter>
        filterAttribute(ATTR_NAME);
        addFilter(ATTR_NAME);

        //next make sure alias is set,
        //defaults to name =~ s/\W+//g;
        if (notSet(ATTR_ALIAS)) {
            String name =
                getAttribute(ATTR_NAME);

            StringBuffer buf = new StringBuffer();
            for (int i=0; i<name.length(); i++) {
                char c = name.charAt(i);
                if (!Character.isWhitespace(c)) {
                    buf.append(c);
                }
            }
            this.props.put(ATTR_ALIAS,
                           buf.toString());
            addFilter(ATTR_ALIAS);
        }

        //finally the template (which may have an ${alias} ref)
        filterAttribute(ATTR_TEMPLATE);

        this.replacer.clear();

        this.data.addMetric(this.metricsName, createMetric());
    }

    private boolean notSet(String attr) {
        return getAttribute(attr) == null;
    }
    
    private boolean isSet(String attr) {
        return getAttribute(attr) != null;
    }

    private boolean isTrue(String attr) {
        return "true".equals(getAttribute(attr));
    }

    /**
     * Generic method for creating a new MeasurementInfo
     * instance, filling in class fields using given attributes Map.
     */
    private MeasurementInfo createMetric()
        throws XmlAttrException {
        MeasurementInfo metric = new MeasurementInfo();

        try {
            metric.setAttributes(this.props);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if (metric.getName().equals(Metric.ATTR_AVAIL)) {
            if (notSet(ATTR_CAT)) {
                metric.setCategory(MeasurementConstants.CAT_AVAILABILITY);
            }
            if (notSet(ATTR_UNITS)) {
                metric.setUnits(MeasurementConstants.UNITS_PERCENTAGE);
            }
        }
        else {
            if (notSet(ATTR_UNITS)) {
                metric.setUnits(MeasurementConstants.UNITS_NONE);
            }
            if (notSet(ATTR_CAT)) {
                metric.setCategory(MeasurementConstants.CAT_UTILIZATION);
            }
        }

        if (notSet(ATTR_TYPE)) {
            metric.setCollectionType(MeasurementConstants.COLL_TYPE_DYNAMIC);
        }

        if (isTrue(ATTR_INDICATOR) &&
            notSet(ATTR_DEFAULTON))
        {
            metric.setDefaultOn(true);
        }

        metric.setCategory(metric.getCategory().toUpperCase());

        try {
            validateMetric(metric);
        } catch (XmlAttrException e) {
            String msg =
                e.getMessage() + " (metric=" + this.props + ")";
            throw new XmlAttrException(msg);
        }

        return metric;
    }

    private XmlAttrException invalidAttr(String attr, String val) {
        return invalidAttr(attr, val, null);
    }
    
    private XmlAttrException invalidAttr(String attr, String val, String why) {
        String msg =
            "Invalid " + attr + "=\"" + val + "\"";
        if (why != null) {
            msg += " - " + why;
        }
        return new XmlAttrException(msg);
    }

    //XXX still need to check: 
    //- there is an availability indicator somewhere
    //- measurement plugin w/ no measurements
    //- duplicate metric names
    //- dupliate metric aliases
    //- duplicate metric templates
    private void validateMetric(MeasurementInfo metric)
        throws XmlAttrException {

        String expect, actual, reason;
        String units = metric.getUnits();
        String cat = metric.getCategory();
        String alias = metric.getAlias();
        String attrType = this.props.getProperty(ATTR_TYPE); 
        int type = metric.getCollectionType();

        //if MeasurementInfo couldn't convert, then will be -1
        if (type == -1) {
            throw invalidAttr(ATTR_TYPE, attrType);   
        }

        if (!VALID_UNITS.contains(units)) {
            throw invalidAttr(ATTR_UNITS, units);
        }
        
        if (!VALID_CATEGORIES.contains(cat)) {
            throw invalidAttr(ATTR_CAT, cat);
        }

        if ((reason = validateAlias(alias)) != null) {
            throw invalidAttr(ATTR_ALIAS, alias, reason);
        }
        
        if (cat.equals(MeasurementConstants.CAT_AVAILABILITY) &&
            metric.isIndicator())
        {
            reason = Metric.ATTR_AVAIL + " indicator with ";

            expect = Metric.ATTR_AVAIL;
            actual = alias;
            if (!expect.equals(actual)) {
                throw invalidAttr(ATTR_ALIAS, actual,
                                  reason + ATTR_ALIAS + " != " + expect);
            }

            expect = MeasurementConstants.UNITS_PERCENTAGE;
            actual = units;
            if (!expect.equals(actual)) {
                throw invalidAttr(ATTR_UNITS, actual,
                                  reason + ATTR_UNITS + " != " + expect); 
            }
            
            int expectType = MeasurementConstants.COLL_TYPE_DYNAMIC;
            expect = MeasurementConstants.COLL_TYPE_NAMES[expectType];
            actual = attrType; 
            if (type != expectType) {
                throw invalidAttr(ATTR_TYPE, actual,
                                  reason + ATTR_TYPE + " != " + expect);
            }
        }
    }
    
    private String validateAlias(String alias) {
        final int min = 3;
        final int max = 100; //sql/measurement-schema.xml
        
        if (alias.length() < min) {
            return "< " + min + " chars";
        }

        if (alias.length() > max) {
            return "> " + max + " chars";
        }

        return null;
    }
}
