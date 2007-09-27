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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CollectorResult {
    private static Log log =
        LogFactory.getLog(CollectorResult.class.getName());
    boolean collected = false;
    boolean reported = false;
    long timestamp;
    int level = -1;
    String source;
    String message;
    HashMap values;

    CollectorResult() {
        this.timestamp = 0;
        this.values = new HashMap();
    }
    
    CollectorResult(Collector collector) {
        this(collector.result);
    }
    
    CollectorResult(CollectorResult result) {
        this.timestamp = result.timestamp;
        this.level = result.level;
        this.source = result.source;
        this.message = result.message;
        this.values = new HashMap(result.values.size());
        this.values.putAll(result.values);            
    }
    
    public long getTimeStamp() {
        return this.timestamp;
    }

    public int getLevel() {
        return this.level;
    }

    public String getSource() {
        return this.source;
    }

    public String getMessage() {
        return this.message;
    }

    public MetricValue getMetricValue(String attr) {
        Double doubleVal;
        Object value =
            Collector.getCompatValue(this.values, attr);
        
        if (value == null) {
            //XXX check attr is valid
            log.debug("Attribute '" + attr + "' not found");
            return MetricValue.NONE;
        }
        else if (value instanceof Double) {
            doubleVal = (Double)value;
        }
        else {
            try {
                doubleVal = Double.valueOf(value.toString());
            } catch (NumberFormatException e) {
                log.error("NumberFormatException: " +
                          attr + "=" + value);
                return MetricValue.NONE;
            }
        }

        //use timeNow rather than this.timestamp
        //to prevent false-alerts
        long timeNow = System.currentTimeMillis();

        return new MetricValue(doubleVal, timeNow);
    }

    public Map getValues() {
        return this.values;
    }

    public void addValues(Map values) {
        this.values.putAll(values);
    }

    public void setValue(String key, double val) {
        this.values.put(key, new Double(val));
    }

    public void setValue(String key, String val) {
        this.values.put(key, val);
    }
    
    public String toString() {
        String msg = 
            "(" + this.source + ") " + this.message;
        if (this.level != -1) {
            msg =
                "[" +
                LogTrackPlugin.getLogLevelLabel(this.level) +
                "] " + msg; 
        }
        return
            new Date(this.timestamp) +
            " " + msg + " values=" + this.values;
    }
}