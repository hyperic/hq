/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import org.hyperic.util.config.ConfigResponse;

public interface ControlCommandsClient {

    /**
     * Register a control plugin for use on this agent
     *
     * @param pluginName The plugin name
     * @param pluginType The type of plugin to create
     * @param response The plugin configuration
     * @return The current plugin state
     */
    void controlPluginAdd(String pluginName, String pluginType,
            ConfigResponse response) throws AgentRemoteException,
            AgentConnectionException;

    /**
     * Remove a control plugin on an agent
     *
     * @param pluginName The plugin name
     */
    void controlPluginRemove(String pluginName) throws AgentRemoteException,
            AgentConnectionException;

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
    void controlPluginCommand(String pluginName, String pluginType, Integer id,
            String action, String args) throws AgentRemoteException,
            AgentConnectionException;

}