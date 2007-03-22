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

package org.hyperic.hq.livedata.agent.commands;

import org.hyperic.hq.agent.AgentRemoteValue;

public class LiveData_args extends AgentRemoteValue {

    private static final String PARAM_PLUGIN  = "plugin";
    private static final String PARAM_COMMAND = "command";

    public LiveData_args() {
        super();
    }

    public LiveData_args(AgentRemoteValue val) {
        String type = val.getValue(PARAM_PLUGIN);
        String command = val.getValue(PARAM_COMMAND);

        setConfig(type, command);
    }

    public void setConfig(String type, String command) {
        super.setValue(PARAM_PLUGIN, type);
        super.setValue(PARAM_COMMAND, command);
    }

    public String getPlugin() {
        return getValue(PARAM_PLUGIN);
    }

    public String getCommand() {
        return getValue(PARAM_COMMAND);
    }
}
