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

package org.hyperic.hq.measurement.agent;

import org.hyperic.hq.agent.AgentAPIInfo;

public final class MeasurementCommandsAPI extends AgentAPIInfo {
    private static final byte MAJOR_VER  = 0x00;
    private static final byte MINOR_VER  = 0x00;
    private static final byte BUGFIX_VER = 0x01;

    // Properties that the measurement commands server defines
    private static final String PROP_PREFIX = 
        "covalent.measurement.";
    public static final String PROP_NAMING_PROVIDER = 
        "covalent.namingProviderURL";
    public static final String PROP_SPOOLDIR =
        PROP_PREFIX + "spoolDir";

    public static final String[] propSet = {
    };

    // Commands the measurement commands server knows about
    private static final String commandPrefix = "rtm:";
    public static final String command_scheduleMeasurements = 
        commandPrefix + "scheduleMeasurements";
    public static final String command_unscheduleMeasurements = 
        commandPrefix + "unscheduleMeasurements";
    public static final String command_getMeasurements = 
        commandPrefix + "getMeasurements";
    public static final String command_setProperties = 
        commandPrefix + "setProperties";
    public static final String command_deleteProperties = 
        commandPrefix + "deleteProperties";

    // Commands for enabling and disabling log and config
    // track plugins.
    public static final String trackPrefix = "track:";
    public static final String command_trackAdd =
        trackPrefix + "trackAdd";
    public static final String command_trackRemove =
        trackPrefix + "trackRemove";

    // Commands for enabling and disabling topN
    public static final String topNPrefix = "topN:";
    public static final String command_scheduleTopn = topNPrefix + "scheduleTopn";
    public static final String command_unscheduleTopn = topNPrefix + "unscheduleTopn";

    public static final String[] commandSet = {
        command_scheduleMeasurements,
        command_unscheduleMeasurements,
        command_getMeasurements,
        command_setProperties,
        command_deleteProperties,
        command_trackAdd,
            command_trackRemove, command_scheduleTopn, command_unscheduleTopn
    };

    public MeasurementCommandsAPI(){
        super(MAJOR_VER, MINOR_VER, BUGFIX_VER);
    }
}
