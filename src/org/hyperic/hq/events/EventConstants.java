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

package org.hyperic.hq.events;

import org.hyperic.util.ArrayUtil;

/** Constants used in the Events subsystem
 */

public class EventConstants {
    public static final int TYPE_THRESHOLD = 1;
    public static final int TYPE_BASELINE  = 2;
    public static final int TYPE_CONTROL   = 3;
    public static final int TYPE_CHANGE    = 4;
    public static final int TYPE_ALERT     = 5;
    public static final int TYPE_CUST_PROP = 6;
    public static final int TYPE_LOG       = 7;
    public static final int TYPE_CFG_CHG   = 8;
    
    private static final String[] TYPES = {
        "Metric Threshold",
        "Metric Baseline",
        "Control Action",
        "Metric Value Change",
        "",     // Don't want Recovery Alert condition to be visible
        "Custom Property Value Change",
        "Log Event",
        "Config Changed",
    };

    public static String[] getTypes() {
        return TYPES;
    }
    
    public static int getType(String typeStr) {
        int ind = ArrayUtil.find(TYPES, typeStr);
        
        if (ind > -1)
            ind++;
        
        return ind;
    }
    
    public static String getType(int type) {
        return TYPES[type - 1];
    }
    
    public static final int FREQ_EVERYTIME      = 0;
    public static final int FREQ_DURATION       = 1;
    public static final int FREQ_COUNTER        = 2;
    public static final int FREQ_NO_DUP         = 3;
    public static final int FREQ_ONCE           = 4;
    
    public static final int PRIORITY_ALL        = 0;

    /**
     * "! - Low" priority for alerts.
     */
    public static final int PRIORITY_LOW = 1;

    /**
     * "!! - Medium" priority for alerts.
     */
    public static final int PRIORITY_MEDIUM = 2;

    /**
     * "!!! - High" priority for alerts.
     */
    public static final int PRIORITY_HIGH = 3;

    private static final String[] PRIORITIES = {
        "! - Low",
        "!! - Medium",
        "!!! - High"
    };

    public static String[] getPriorities() {
        return PRIORITIES;
    }
    
    public static int getPriority(String priStr) {
        int ind = ArrayUtil.find(PRIORITIES, priStr);
        
        if (ind > -1)
            ind++;
        
        return ind;
    }

    public static String getPriority(int pri) {
        return PRIORITIES[pri - 1];
    }
    
    public static final String EVENTS_TOPIC     = "topic/eventsTopic";

    /**
     * Constant for removing a control action from an alert definition.
     */
    public static final String CONTROL_ACTION_NONE = "none";
    
    public static final Integer TYPE_ALERT_DEF_ID = new Integer(0);
}
