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

package org.hyperic.hq.livedata.agent.server;

import org.hyperic.hq.agent.server.AgentServerHandler;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.livedata.agent.LiveDataCommandsAPI;
import org.hyperic.hq.livedata.agent.commands.LiveData_args;
import org.hyperic.hq.livedata.agent.commands.LiveData_result;
import org.hyperic.hq.product.LiveDataPluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.OutputStream;

public class LiveDataCommandsServer implements AgentServerHandler {

    private Log _log = LogFactory.getLog(LiveDataCommandsServer.class);
    private LiveDataPluginManager _manager;

    private LiveDataCommandsAPI _commands = new LiveDataCommandsAPI();

    public String[] getCommandSet() {
        return LiveDataCommandsAPI.commandSet;
    }

    public AgentAPIInfo getAPIInfo() {
        return _commands;
    }

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args,
                                            InputStream inStream,
                                            OutputStream outStream)
        throws AgentRemoteException
    {
        if (cmd.equals(LiveDataCommandsAPI.command_getData)) {
            LiveData_args res = new LiveData_args(args);
            return cmdGetData(res);
        } else {
            throw new AgentRemoteException("Unexpected command: " + cmd);
        }
    }

    public void startup(AgentDaemon agent) throws AgentStartException {

        try {
            _manager = (LiveDataPluginManager)agent.
                getPluginManager(ProductPlugin.TYPE_LIVE_DATA);
        } catch (Exception e) {
            throw new AgentStartException("Unable to load live data manager",
                                          e);
        }
    }

    public void shutdown() {
    }

    public LiveData_result cmdGetData(LiveData_args args)
        throws AgentRemoteException
    {
        _log.info("Asked to invoke cmdGetData for " + args.getType());

        try {
            String s = _manager.getData(args.getType(),
                                        args.getCommand(),
                                        args.getConfig());
            LiveData_result res = new LiveData_result();
            res.setResult(s);
            return res;
        } catch (Exception e) {
            throw new AgentRemoteException("Unable to invoke command: " +
                                           e.getMessage(), e);
        }
    }
}
