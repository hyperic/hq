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
import org.hyperic.hq.agent.client.AbstractCommandsClient;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.util.config.ConfigResponse;

/**
 * The Control Commands client that uses the new transport.
 */
public class ControlCommandsClientImpl 
    extends AbstractCommandsClient implements ControlCommandsClient {
    
    public ControlCommandsClientImpl(Agent agent, AgentProxyFactory factory) {
        super(agent, factory);
    }

    /**
     * @see org.hyperic.hq.control.agent.client.ControlCommandsClient#controlPluginAdd(java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public void controlPluginAdd(String pluginName, 
                                 String pluginType,
                                 ConfigResponse response) 
        throws AgentRemoteException, AgentConnectionException {
        
        ControlCommandsClient proxy = null;
        
        try {
            proxy = (ControlCommandsClient)getSynchronousProxy(ControlCommandsClient.class);
            proxy.controlPluginAdd(pluginName, pluginType, response);            
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.control.agent.client.ControlCommandsClient#controlPluginCommand(java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.lang.String)
     */
    public void controlPluginCommand(String pluginName, String pluginType,
            Integer id, String action, String args)
            throws AgentRemoteException, AgentConnectionException {
        
        ControlCommandsClient proxy = null;
        
        try {
            proxy = (ControlCommandsClient)getAsynchronousProxy(ControlCommandsClient.class, true);
            proxy.controlPluginCommand(pluginName, pluginType, id, action, args);           
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.control.agent.client.ControlCommandsClient#controlPluginRemove(java.lang.String)
     */
    public void controlPluginRemove(String pluginName)
            throws AgentRemoteException, AgentConnectionException {

        ControlCommandsClient proxy = null;
        
        try {
            proxy = (ControlCommandsClient)getSynchronousProxy(ControlCommandsClient.class);
            proxy.controlPluginRemove(pluginName);     
        } finally {
            safeDestroyService(proxy);
        }    
    }

}
