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

package org.hyperic.hq.autoinventory.agent.client;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AbstractCommandsClient;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.util.config.ConfigResponse;

/**
 * The AI Commands client that uses the new transport.
 */
public class AICommandsClientImpl 
    extends AbstractCommandsClient implements AICommandsClient {
    
    public AICommandsClientImpl(Agent agent, AgentProxyFactory factory) {
        super(agent, factory);
    }

    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#getScanStatus()
     */
    public ScanStateCore getScanStatus() 
        throws AgentRemoteException, AgentConnectionException, AutoinventoryException {
        
        AICommandsClient proxy = null;
        
        try {
            proxy = (AICommandsClient)getSynchronousProxy(AICommandsClient.class);
            return proxy.getScanStatus();            
        } finally {
            safeDestroyService(proxy);
        }
    }
    
    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#pushRuntimeDiscoveryConfig(int, int, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public void pushRuntimeDiscoveryConfig(int type, 
                                           int id, 
                                           String typeName,
                                           String name, 
                                           ConfigResponse response) {
        
        AICommandsClient proxy = null;
        
        try {
            proxy = (AICommandsClient)getAsynchronousProxy(AICommandsClient.class, false);
            proxy.pushRuntimeDiscoveryConfig(type, id, typeName, name, response); 
        } catch (AgentConnectionException ace) {
            _log.error("Error connecting to agent to push runtime discovery "
                      + "config: " + ace.getMessage());
        } catch (AgentRemoteException are) {
            _log.error("Error sending runtime discover configuration to agent: "
                      + are.getMessage());
        } finally {
            safeDestroyService(proxy);
        }
    }
    
    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#startScan(org.hyperic.hq.autoinventory.ScanConfigurationCore)
     */
    public void startScan(ScanConfigurationCore scanConfig)
            throws AgentRemoteException, AgentConnectionException, AutoinventoryException {
        
        AICommandsClient proxy = null;
        
        try {
            proxy = (AICommandsClient)getAsynchronousProxy(AICommandsClient.class, false);
            proxy.startScan(scanConfig);            
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#stopScan()
     */
    public void stopScan() throws AgentRemoteException, AgentConnectionException {

        AICommandsClient proxy = null;
        
        try {
            proxy = (AICommandsClient)getAsynchronousProxy(AICommandsClient.class, false);
            proxy.stopScan();            
        } finally {
            safeDestroyService(proxy);
        }
    }

}
