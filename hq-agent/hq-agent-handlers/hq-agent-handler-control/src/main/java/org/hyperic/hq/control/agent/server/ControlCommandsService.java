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

package org.hyperic.hq.control.agent.server;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.bizapp.client.ControlCallbackClient;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;
import org.hyperic.hq.product.ControlPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginExistsException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * The Control Commands service.
 */
public class ControlCommandsService implements ControlCommandsClient {
    
    private final ControlPluginManager _controlManager;
    private final ControlCallbackClient _client;
    
    public ControlCommandsService(ControlPluginManager controlManager, 
                                  ControlCallbackClient client) {
        _controlManager = controlManager;
        _client = client;
    }

    /**
     * @see org.hyperic.hq.control.agent.client.ControlCommandsClient#controlPluginAdd(java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public void controlPluginAdd(String pluginName, 
                                 String pluginType,
                                 ConfigResponse response) 
        throws AgentRemoteException {
        
        try {
            _controlManager.createControlPlugin(pluginName, pluginType, response);
        } catch (PluginNotFoundException e) {
            throw new AgentRemoteException(e.getMessage());
        } catch (PluginExistsException e) {
            // Must be a config update
            try {
                _controlManager.updateControlPlugin(pluginName, response);
            } catch (Exception exc) {
                throw new AgentRemoteException(exc.getMessage());
            }
        } catch (PluginException e) {
            throw new AgentRemoteException(e.getMessage());
        }
    }

    /**
     * @see org.hyperic.hq.control.agent.client.ControlCommandsClient#controlPluginCommand(java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.lang.String)
     */
    public void controlPluginCommand(String pluginName, 
                                     String pluginType,
                                     Integer id, 
                                     String action, 
                                     String args) 
        throws AgentRemoteException {
        
        String idString = String.valueOf(id);
        String[] pluginArgs = StringUtil.explodeQuoted(args);
        
        controlPluginCommand(pluginName, pluginType, idString, action, pluginArgs);
    }
    
    void controlPluginCommand(String pluginName, 
                              String pluginType,
                              String id, 
                              String pluginAction, 
                              String[] pluginArgs) {
        
        ActionThread actionThread = 
            new ActionThread(pluginName,
                             pluginType,
                             id,
                             pluginAction,
                             pluginArgs,
                             _client, 
                             _controlManager);
        actionThread.start();
        
    }

    /**
     * @see org.hyperic.hq.control.agent.client.ControlCommandsClient#controlPluginRemove(java.lang.String)
     */
    public void controlPluginRemove(String pluginName) throws AgentRemoteException {
        try {
            _controlManager.removeControlPlugin(pluginName);
        } catch (PluginNotFoundException e) {
            // Ok if the plugin no longer exists.
        } catch (PluginException e) {
            throw new AgentRemoteException(e.getMessage());
        }
    }
    
    public final ControlCallbackClient getClient() { return this._client ; }//EOM 

}
