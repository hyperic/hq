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
import org.hyperic.hq.livedata.shared.LiveDataResult;
import org.hyperic.hq.agent.client.AgentConnection;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.util.config.ConfigResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.thoughtworks.xstream.XStream;

/**
 * The Live Data Commands client that uses the legacy transport.
 */
public class LegacyLiveDataCommandsClientImpl implements LiveDataCommandsClient {

    private LiveDataCommandsAPI _api;
    private AgentConnection _conn;
    private static Log _log = LogFactory.getLog(LegacyLiveDataCommandsClientImpl.class);

    public LegacyLiveDataCommandsClientImpl(AgentConnection agentConnection) {
        _conn = agentConnection;
        _api = new LiveDataCommandsAPI();
    }

    /**
     * Helper method to validate the XStream serialization on the server.  In
     * some cases (i.e. outdated Agents) we may get objects that cannot be
     * serialized back to their original form.
     *
     * @param xml The xml representation of the object.
     */
    private void serializeData(String xml) {
        XStream xstream = new XStream();
        xstream.fromXML(xml);
    }

    /**
     * @see org.hyperic.hq.livedata.agent.client.LiveDataCommandsClient#getData(org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public LiveDataResult getData(AppdefEntityID id, String type,
                                  String command,
                                  ConfigResponse config)
    {
        try {
            LiveData_args args = new LiveData_args();

            args.setConfig(type, command, config);

            AgentRemoteValue res =
                _conn.sendCommand(LiveDataCommandsAPI.command_getData,
                                  _api.getVersion(), args);
            LiveData_result val = new LiveData_result(res);
            String xml = val.getResult();

            try {
                serializeData(xml);
                return new LiveDataResult(id, xml);
            } catch (Throwable t) {
                String err = LiveDataCommandsClientFactory.BUNDLE.format("error.serialization");
                _log.warn(err, t);
                return new LiveDataResult(id, t, err);
            }
        } catch (Exception e) {
            return new LiveDataResult(id, e, e.getMessage());
        }
    }
}
