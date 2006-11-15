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

package org.hyperic.hq.control.agent.client;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.client.AgentConnection;
import org.hyperic.hq.control.agent.ControlCommandsAPI;
import org.hyperic.hq.control.agent.commands.ControlPluginAdd_args;
import org.hyperic.hq.control.agent.commands.ControlPluginAdd_result;
import org.hyperic.hq.control.agent.commands.ControlPluginCommand_args;
import org.hyperic.hq.control.agent.commands.ControlPluginRemove_args;

import org.hyperic.util.config.ConfigResponse;

/**
 * An client object representing the methods which can be invoked on the Agent.
 */

public class ControlCommandsClient {
    private AgentConnection    agentConn;
    private ControlCommandsAPI verAPI;

    /**
     * Creates a new ControlCommandsClient object which should communicate
     * through the passed connection object.
     *
     * @param agentConn Connection this object should use when sending 
     *                  commands.
     */

    public ControlCommandsClient(AgentConnection agentConn){
        this.agentConn = agentConn;
        this.verAPI    = new ControlCommandsAPI();
    }

    /**
     * Register a control plugin for use on this agent
     *
     * @param pluginName The plugin name
     * @param pluginType The type of plugin to create
     * @param response The plugin configuration
     * @return The current plugin state
     */
    public void controlPluginAdd(String pluginName, String pluginType,
                                 ConfigResponse response)
        throws  AgentRemoteException, AgentConnectionException
    {
        ControlPluginAdd_args args = new ControlPluginAdd_args();
        args.setConfig(pluginName, pluginType, response);

        AgentRemoteValue val =
            this.agentConn.sendCommand(this.verAPI.command_controlPluginAdd,
                                       this.verAPI.getVersion(), args);

        ControlPluginAdd_result result = new ControlPluginAdd_result(val);

        return;
    }

    /**
     * Remove a control plugin on an agent
     *
     * @param pluginName The plugin name
     */
    public void controlPluginRemove(String pluginName)
        throws AgentRemoteException, AgentConnectionException
    {
        ControlPluginRemove_args args = new ControlPluginRemove_args();
        args.setPluginName(pluginName);

        AgentRemoteValue val =
        this.agentConn.sendCommand(this.verAPI.command_controlPluginRemove,
                                   this.verAPI.getVersion(), args);
    }

    /**
     * Issue a control operation that does not have an id.  Jobs that
     * dont have ids will not have status info report back to the CAM
     * server
     *
     * @param pluginName The plugin id
     * @param pluginType The plugin type
     * @param action The action to perform
     */
    public void controlPluginCommand(String pluginName, String pluginType,
                                     String action)
        throws AgentRemoteException, AgentConnectionException
    {
        ControlPluginCommand_args args = new ControlPluginCommand_args();
        args.setCommand(pluginName, pluginType, action, null);

        AgentRemoteValue val = 
            this.agentConn.sendCommand(
            this.verAPI.command_controlPluginCommand,
            this.verAPI.getVersion(), args);
    }

    /**
     * Issue a control operation on a plugin and associate it with a job id
     *
     * @param pluginName The plugin id
     * @param pluginType The plugin type
     * @paarm id The job id
     * @param action The action to perform
     *
     * @return true if there was a current action in progress, false otherwise
     */
    public void controlPluginCommand(String pluginName, String pluginType,
                                     Integer id, String action,
                                     String args)
        throws AgentRemoteException, AgentConnectionException
    {
        ControlPluginCommand_args cpArgs = new ControlPluginCommand_args();
        cpArgs.setCommand(pluginName, pluginType, id, action, args);

        AgentRemoteValue val =
            this.agentConn.sendCommand(
            this.verAPI.command_controlPluginCommand,
            this.verAPI.getVersion(), cpArgs);
    }
}
