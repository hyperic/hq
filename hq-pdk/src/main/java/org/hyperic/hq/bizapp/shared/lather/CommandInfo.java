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

package org.hyperic.hq.bizapp.shared.lather;

public class CommandInfo {
    public static final String CMD_PING                    = "ping";
    public static final String CMD_USERISVALID             = "userIsValid";
    public static final String CMD_MEASUREMENT_GET_SERVER_TIME = "measurementGetServerTime";
    public static final String CMD_MEASUREMENT_GET_CONFIGS = "measurementGetConfigs";
    public static final String CMD_MEASUREMENT_SEND_REPORT =  "measurementSendReport";
    public static final String CMD_REGISTER_AGENT          = "registerAgent";
    public static final String CMD_UPDATE_AGENT            = "updateAgent";
    public static final String CMD_AI_SEND_REPORT          = "aiSendReport";
    public static final String CMD_AI_SEND_RUNTIME_REPORT  = "aiSendRuntimeReport";
    public static final String CMD_TRACK_SEND_LOG          = "trackSendLog";
    public static final String CMD_TRACK_SEND_CONFIG_CHANGE = "trackSendConfigChange";
    public static final String CMD_CONTROL_GET_PLUGIN_CONFIG = "controlGetPluginConfig";
    public static final String CMD_CONTROL_SEND_COMMAND_RESULT = "controlSendCommandResult";
    public static final String CMD_PLUGIN_SEND_REPORT = "pluginSendReport";
    public static final String CMD_TOPN_SEND_REPORT = "topNSendReport";
    
    public static final String[] ALL_COMMANDS = {
        CMD_PING,
        CMD_USERISVALID,
        CMD_MEASUREMENT_GET_SERVER_TIME,
        CMD_MEASUREMENT_GET_CONFIGS,
        CMD_MEASUREMENT_SEND_REPORT,
        CMD_REGISTER_AGENT,
        CMD_UPDATE_AGENT,
        CMD_AI_SEND_REPORT,
        CMD_AI_SEND_RUNTIME_REPORT,
        CMD_TRACK_SEND_LOG,
        CMD_TRACK_SEND_CONFIG_CHANGE,
        CMD_CONTROL_GET_PLUGIN_CONFIG,
        CMD_CONTROL_SEND_COMMAND_RESULT,
        CMD_PLUGIN_SEND_REPORT,
            CMD_TOPN_SEND_REPORT,
    };

    public static final String[] SECURE_COMMANDS = {
        CMD_MEASUREMENT_GET_SERVER_TIME,
        CMD_MEASUREMENT_GET_CONFIGS,
        CMD_MEASUREMENT_SEND_REPORT,
        CMD_AI_SEND_REPORT,
        CMD_AI_SEND_RUNTIME_REPORT,
        CMD_TRACK_SEND_LOG,
        CMD_TRACK_SEND_CONFIG_CHANGE,
        CMD_CONTROL_GET_PLUGIN_CONFIG,
        CMD_CONTROL_SEND_COMMAND_RESULT,
    };

}
