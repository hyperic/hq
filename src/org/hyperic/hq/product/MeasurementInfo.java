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

package org.hyperic.hq.product;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.measurement.MeasurementConstants;

/** Carry information about measurement templates
 *
 */
public class MeasurementInfo
    implements Cloneable {

    public static final String ATTR_NAME            = "name";
    public static final String ATTR_ALIAS           = "alias";
    public static final String ATTR_TEMPLATE        = "template";
    public static final String ATTR_CATEGORY        = "category";
    public static final String ATTR_GROUP           = "group";
    public static final String ATTR_UNITS           = "units";
    public static final String ATTR_RATE            = "rate";
    public static final String ATTR_COLLECTION_TYPE = "collectionType";
    public static final String ATTR_DEFAULTON       = "defaultOn";
    public static final String ATTR_INDICATOR       = "indicator";
    public static final String ATTR_INTERVAL        = "interval";

    public static final String SEC_RATE     = "1s";     // 1 second
    public static final String MIN_RATE     = "1m";     // 1 minute
    public static final String HOUR_RATE    = "1h";     // 1 hour
    public static final String NO_RATE      = "none";   // no rate
    public static final String DEFAULT_RATE = MIN_RATE; // default 1 min
    public static final String RATE_KEY     = "__RATE__";

    private static final String[] COLL_TYPES =
        MeasurementConstants.COLL_TYPE_NAMES;

    private Map attrs = new HashMap();
    private int     collType  = -1; // Set by default to an invalid value
    private boolean defaultOn = false;
    private boolean indicator = false;
    private long    interval  = -1;
    
    public MeasurementInfo() { }

    public Object clone() {
        MeasurementInfo info = new MeasurementInfo();
        info.attrs = new HashMap();
        info.attrs.putAll(this.attrs);

        info.collType  = this.collType;
        info.defaultOn = this.defaultOn;
        info.interval  = this.interval;
        info.indicator = this.indicator;

        return info;
    }
    
    public void setAttributes(Map attrs) {
        this.attrs.putAll(attrs);

        this.defaultOn =
            getBooleanAttribute(ATTR_DEFAULTON);
        this.indicator =
            getBooleanAttribute(ATTR_INDICATOR);
        String type =
            getAttribute(ATTR_COLLECTION_TYPE);

        //XXX throw ex if invalid type
        for (int i=0; i<MeasurementConstants.COLL_TYPE_NAMES.length; i++) {
            if (MeasurementConstants.COLL_TYPE_NAMES[i].equals(type)) {
                this.collType = i;
                break;
            }
        }

        String interval = getAttribute(ATTR_INTERVAL);
        if (interval == null) {
            this.interval = -1;
        }
        else {
            this.interval = Integer.parseInt(interval);
        }
    }

    public String toXML() {
        return toXML("");
    }
    
    private String attr(String key, boolean val) {
        return attr(key, String.valueOf(val));
    }
    private String attr(String key, long val) {
        return attr(key, String.valueOf(val));
    }
    private String attr(String key, String val) {
        return key + "=\"" + val +  "\"";
    }
    private String attr(String key) {
        return attr(key, getAttribute(key));
    }
    
    public String toXML(String indent) {
        final String xindent = indent + "        ";
        final String NL = "\n";
        
        return
            indent + "<metric " + attr(ATTR_NAME) + NL +

            xindent + attr(ATTR_ALIAS) + NL +

            xindent + attr(ATTR_TEMPLATE) + NL +

            xindent + attr(ATTR_CATEGORY) + NL +

            xindent + attr(ATTR_GROUP) + NL + 

            xindent + attr(ATTR_DEFAULTON, this.defaultOn) + NL +

            xindent + attr(ATTR_INDICATOR, this.indicator) + NL +

            xindent + attr(ATTR_UNITS) + NL +

            xindent + attr(ATTR_COLLECTION_TYPE,
                           COLL_TYPES[this.collType]) + NL +

            xindent + attr(ATTR_INTERVAL, this.interval) + "/>" + NL;
    }

    public Map getAttributes() {
        return this.attrs;
    }

    private void setAttribute(String key, String val) {
        this.attrs.put(key, val);
    }
    
    private boolean getBooleanAttribute(String name) {
        return "true".equals(getAttribute(name));
    }

    private String getAttribute(String name) {
        return (String)this.attrs.get(name);
    }
    
    private String getAttribute(String name, String defVal) {
        String val = getAttribute(name);
        if (val != null) {
            return val;
        }
        else {
            return defVal;
        }
    }

    public String getName() {
        return getAttribute(ATTR_NAME);
    }
    
    public void setName(String name) {
        setAttribute(ATTR_NAME, name);
    }
    
    public String getAlias() {
        return getAttribute(ATTR_ALIAS);
    }
    
    public void setAlias(String alias) {
        setAttribute(ATTR_ALIAS, alias);
    }

    public String getTemplate() {
        return getAttribute(ATTR_TEMPLATE);
    }
    
    public void setTemplate(String template) {
        setAttribute(ATTR_TEMPLATE, template);
    }
    
    public String getCategory() {
        return getAttribute(ATTR_CATEGORY);
    }
    
    public void setCategory(String category) {
        setAttribute(ATTR_CATEGORY, category);
    }

    public String getGroup() {
        return getAttribute(ATTR_GROUP, "");
    }
    
    public void setGroup(String group) {
        setAttribute(ATTR_GROUP, group);
    }

    /**
     * @return boolean
     */
    public boolean isDefaultOn() {
        return this.defaultOn;
    }

    /**
     * Sets the defaultOn.
     * @param defaultOn The defaultOn to set
     */
    public void setDefaultOn(boolean defaultOn) {
        this.defaultOn = true;
    }

    /**
     * @return String
     */
    public String getUnits() {
        return getAttribute(ATTR_UNITS, "");
    }

    /**
     * Sets the units.
     * @param units The units to set
     */
    public void setUnits(String units) {
        setAttribute(ATTR_UNITS, units);
    }

    /**
     * @return long
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Sets the interval.
     * @param interval The interval to set
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * @return boolean
     */
    public boolean isIndicator() {
        return this.indicator;
    }

    /**
     * Sets the designate.
     * @param indicator The designate to set
     */
    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }

    /**
     * @return the collection type
     */
    public int getCollectionType() {
        return collType;
    }

    /**
     * @param i the new collection type
     */
    public void setCollectionType(int i) {
        collType = i;
    }

    /**
     * @return String
     */
    public String getRate() {
        return getAttribute(ATTR_RATE, "");
    }

    /**
     * Sets the rate.
     * @param rate The rate to set
     */
    public void setRate(String rate) {
        setAttribute(ATTR_RATE, rate);
    }

    /**
     * @return Human readable form of the rate
     */
    public String getReadableRate() {
        String rate = getRate();
        if (rate.equals(SEC_RATE)) {
            return "per Second";
        } else if (rate.equals(MIN_RATE)) {
            return "per Minute";
        } else if (rate.equals(HOUR_RATE)) {
            return "per Hour";
        } else {
            throw new IllegalArgumentException("Invalid rate type: " +
                                               rate);
        }
    }
}
