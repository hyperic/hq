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

package org.hyperic.hq.bizapp.shared;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.DoubleConfigOption;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.StringConfigOption;

/**
 * Consolidate all conditional trigger config schemas here so that they can be
 * shared by CLI and bizapp
 */
public class ConditionalTriggerSchema {
    public static final String CFG_ID = "id";

    public static final String CFG_COMPARATOR = "comparator";

    public static final String CFG_NAME = "name";

    public static final String CFG_OPTION = "option";

    public static final String CFG_THRESHOLD = "threshold";

    public static final String CFG_TYPE = "type";

    public final static int OPER_LE = 1;
    public final static int OPER_LT = 2;
    public final static int OPER_EQ = 3;
    public final static int OPER_GT = 4;
    public final static int OPER_GE = 5;
    public final static int OPER_NE = 6;

    public final static String[] OPER_STRS =
        new String[] { "", "<=", "<", "=", ">", ">=", "!=" };

    private ConditionalTriggerSchema() {}

    public static ConfigSchema getConfigSchema(int eventType) {
        IntegerConfigOption type, id;
        EnumerationConfigOption oper;
        IntegerConfigOption mID;

        ConfigSchema res = new ConfigSchema();
        switch (eventType) {
        case EventConstants.TYPE_ALERT:
            res.addOption(new IntegerConfigOption(CFG_ID,
                    "Watch alert definition ID", new Integer(0)));
            break;
        case EventConstants.TYPE_CONTROL:
            StringConfigOption action, status;

            type = new IntegerConfigOption(CFG_TYPE, "Resource Type", null);
            type.setMinValue(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
            type.setMaxValue(AppdefEntityConstants.APPDEF_TYPE_SERVICE);

            id = new IntegerConfigOption(CFG_ID, "Resource ID", null);
            id.setMinValue(0);

            action = new StringConfigOption(CFG_NAME, "Action", null);

            status = new StringConfigOption(CFG_OPTION, "Action status", null);
            res.addOption(type);
            res.addOption(id);
            res.addOption(action);
            res.addOption(status);
            break;
        case EventConstants.TYPE_CUST_PROP:
            StringConfigOption custProp;

            type = new IntegerConfigOption(CFG_TYPE, "Resource Type", null);
            type.setMinValue(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
            type.setMaxValue(AppdefEntityConstants.APPDEF_TYPE_SERVICE);

            id = new IntegerConfigOption(CFG_ID, "Resource ID", null);
            id.setMinValue(0);

            custProp = new StringConfigOption(CFG_NAME, "Custom Property", null);

            res.addOption(type);
            res.addOption(id);
            res.addOption(custProp);
            break;
        case EventConstants.TYPE_LOG:
            IntegerConfigOption logLevel;
            StringConfigOption substring;

            type = new IntegerConfigOption(CFG_TYPE, "Resource Type", null);
            type.setMinValue(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
            type.setMaxValue(AppdefEntityConstants.APPDEF_TYPE_SERVICE);

            id = new IntegerConfigOption(CFG_ID, "Resource ID", null);
            id.setMinValue(0);

            logLevel = new IntegerConfigOption(CFG_NAME, "Log Level",
                    new Integer(-1));
            id.setMinValue(-1);

            substring = new StringConfigOption(CFG_OPTION, "Match Substring",
                    null);

            res.addOption(type);
            res.addOption(id);
            res.addOption(logLevel);
            res.addOption(substring);
            break;
        case EventConstants.TYPE_BASELINE:
            IntegerConfigOption deviate;
            EnumerationConfigOption baselineVal;

            mID = new IntegerConfigOption(CFG_ID, "Measurement ID", null);
            mID.setMinValue(0);

            oper = buildComparatorOption();

            baselineVal = buildBaselineValOption();

            deviate = new IntegerConfigOption(CFG_THRESHOLD,
                    "Threshold Percentage", new Integer(100));
            deviate.setMinValue(0);

            res.addOption(mID);
            res.addOption(oper);
            res.addOption(baselineVal);
            res.addOption(deviate);
            break;
        case EventConstants.TYPE_THRESHOLD:
            DoubleConfigOption thresh;

            mID = new IntegerConfigOption(CFG_ID, "Measurement ID", null);
            mID.setMinValue(0);

            oper = buildComparatorOption();

            thresh = new DoubleConfigOption(CFG_THRESHOLD, "Threshold value",
                    null);
            thresh.setMinValue(0);

            res.addOption(mID);
            res.addOption(oper);
            res.addOption(thresh);
            break;
        case EventConstants.TYPE_CHANGE:
            mID = new IntegerConfigOption(CFG_ID, "Measurement ID", null);
            mID.setMinValue(0);

            res.addOption(mID);
            break;
        case EventConstants.TYPE_CFG_CHG:
            StringConfigOption filename;

            type = new IntegerConfigOption(CFG_TYPE, "Resource Type", null);
            type.setMinValue(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
            type.setMaxValue(AppdefEntityConstants.APPDEF_TYPE_SERVICE);

            id = new IntegerConfigOption(CFG_ID, "Resource ID", null);
            id.setMinValue(0);

            filename = new StringConfigOption(CFG_OPTION, "File name",
                    null);

            res.addOption(type);
            res.addOption(id);
            res.addOption(filename);
            break;
        default:
            break;
        }
        return res;
    }

    /**
     * builds the BaselineValue options
     * 
     * @return EnumerationConfigOption
     */
    private static EnumerationConfigOption buildBaselineValOption() {
    	EnumerationConfigOption baselineVal;
        baselineVal = new EnumerationConfigOption(CFG_OPTION, "Baseline Value",
            MeasurementConstants.BASELINE_OPT_MEAN,
            new String[] {
    	        MeasurementConstants.BASELINE_OPT_MEAN,
                MeasurementConstants.BASELINE_OPT_MIN,
                MeasurementConstants.BASELINE_OPT_MAX
            });
    	return baselineVal;
    }

    /**
     * builds the comparison option
     *
     * @return EnumerationConfigOption
     */
    private static EnumerationConfigOption buildComparatorOption() {
    	return new EnumerationConfigOption(CFG_COMPARATOR,
    	                                   "Comparison operator", ">",
                                           OPER_STRS);    
    }
}
