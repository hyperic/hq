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

package org.hyperic.hq.ui;

import org.hyperic.hq.control.shared.ControlConstants;

/**
 * Manifest constants for the UI of the HQ application.
 * 
 * The constants are actually organized into more digestable chunks in
 * logical interfaces, e.g. attributes are in AttributeConstants and 
 * numbers are in NumberConstants
 */
public class Constants
    implements RetCodeConstants, NumberConstants, MessageConstants,
               DefaultConstants, TypeConstants, AttrConstants, ParamConstants,
               KeyConstants, StringConstants { 
    
    public static final String APP_VERSION = "HQVersion";
    public static final String APP_BUILD   = "HQBuild";

    public static final String INVENTORY_LOC_TYPE = "Inventory.do";

    public static final String MONITOR_VISIBILITY_LOC = "monitor/Visibility.do";
    
    public static final String MONITOR_CONFIG_LOC = "monitor/Config.do";

    public static final String CONTROL_LOC = "/Control.do";

    /**
     * These two locations are not handled similar to the rest of the above URL
     */
    public static final String ALERT_LOC = "alerts/Alerts.do";

    public static final String ALERT_CONFIG_LOC = "alerts/Config.do";
    /**
     * Status of the current control action.
     */
    public static final String CONTROL_STATUS_ERROR = ControlConstants.STATUS_FAILED;
    public static final String CONTROL_STATUS_INPROGRESS = ControlConstants.STATUS_INPROGRESS;
    public static final String CONTROL_STATUS_COMPLETED = ControlConstants.STATUS_COMPLETED;
    public static final String CONTROL_STATUS_NONE = "none";
}
