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

package org.hyperic.hq.autoinventory.agent;

import org.hyperic.hq.agent.AgentAPIInfo;

public final class AICommandsAPI extends AgentAPIInfo {

    public static final String PROP_SCANCONFIG = "scanConfig";

    private static final byte MAJOR_VER  = 0x00;
    private static final byte MINOR_VER  = 0x00;
    private static final byte BUGFIX_VER = 0x01;

    // Commands the Agent autoinventory plugin knows about
    private static final String commandPrefix = "autoinv:";

    public static final String command_startScan
        = commandPrefix + "startScan";
    public static final String command_stopScan
        = commandPrefix + "stopScan";
    public static final String command_getScanStatus
        = commandPrefix + "getScanStatus";
    public static final String command_pushRuntimeDiscoveryConfig
        = commandPrefix + "pushRuntimeDiscoveryConfig";

    public static final String[] commandSet = {
        command_startScan,
        command_stopScan,
        command_getScanStatus,
        command_pushRuntimeDiscoveryConfig
    };

    public AICommandsAPI(){
        super(MAJOR_VER, MINOR_VER, BUGFIX_VER);
    }
}
