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

package org.hyperic.hq.control.agent;

import org.hyperic.hq.agent.AgentAPIInfo;

public final class ControlCommandsAPI extends AgentAPIInfo {
    private static final byte MAJOR_VER  = 0x00;
    private static final byte MINOR_VER  = 0x00;
    private static final byte BUGFIX_VER = 0x01;

    // Properties that the control commands server defines
    private static final String PROP_PREFIX = 
        "covalent.control.";

    // Commands the control commands server knows about
    private static final String CMD_PREFIX = "control:";
    public static final String command_controlPluginAdd =
        CMD_PREFIX + "controlPluginAdd";
    public static final String command_controlPluginCommand =
        CMD_PREFIX + "controlPluginCommand";
    public static final String command_controlPluginRemove =
        CMD_PREFIX + "controlPluginRemove";
    public static final String command_controlAgtRcvFileData =
        CMD_PREFIX + "controlAgtRcvFileData";
    public static final String command_controlAgtUndoFileData =
        CMD_PREFIX + "controlAgtUndoFileData";

    public static final String[] commandSet = {
        command_controlPluginAdd,
        command_controlPluginCommand,
        command_controlPluginRemove,
        command_controlAgtRcvFileData,
        command_controlAgtUndoFileData
    };

    public ControlCommandsAPI(){
        super(MAJOR_VER, MINOR_VER, BUGFIX_VER);
    }
}
