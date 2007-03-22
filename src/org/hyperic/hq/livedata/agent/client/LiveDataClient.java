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

package org.hyperic.hq.livedata.agent.client;

import org.hyperic.hq.livedata.agent.LiveDataCommandsAPI;
import org.hyperic.hq.livedata.agent.commands.LiveData_args;
import org.hyperic.hq.livedata.agent.commands.LiveData_result;
import org.hyperic.hq.livedata.shared.LiveDataException;
import org.hyperic.hq.agent.client.AgentConnection;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.util.config.ConfigResponse;

public class LiveDataClient {

    private LiveDataCommandsAPI _api;
    private AgentConnection _agentConnection;

    public LiveDataClient(AgentConnection agentConnection) {
        _agentConnection = agentConnection;
        _api = new LiveDataCommandsAPI();
    }

    public String getData(String type, String command, ConfigResponse config)
        throws AgentConnectionException, AgentRemoteException,
               LiveDataException
    {
        LiveData_args args = new LiveData_args();

        args.setConfig(type, command, config);

        AgentRemoteValue res =
            _agentConnection.sendCommand(LiveDataCommandsAPI.command_getData,
                                         _api.getVersion(), args);
        LiveData_result val = new LiveData_result(res);

        return val.getResult();
    }
}
