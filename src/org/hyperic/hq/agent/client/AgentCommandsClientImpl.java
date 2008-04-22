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

package org.hyperic.hq.agent.client;

import java.io.InputStream;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.transport.AgentProxyFactory;

/**
 * The Agent Commands client that uses the new transport.
 */
public class AgentCommandsClientImpl 
    extends AbstractCommandsClient implements AgentCommandsClient {
    
    private final boolean _agentRegistrationClient;

    public AgentCommandsClientImpl(Agent agent, AgentProxyFactory factory) {
        super(agent, factory);
        _agentRegistrationClient = false;
    }
    
    /**
     * This constructor should only be used during agent registration where 
     * the agent doesn't yet know its agent token and the Agent pojo has not 
     * yet been persisted on the server.
     */
    public AgentCommandsClientImpl(AgentProxyFactory factory, 
                                   String agentAddress, 
                                   int agentPort, 
                                   boolean unidirectional) {
        super(createAgent(agentAddress, agentPort, unidirectional), factory);
        
        _agentRegistrationClient = true;
    }
    
    private static Agent createAgent(String agentAddress, 
                                     int agentPort, 
                                     boolean unidirectional) {
        Agent agent = new Agent();
        agent.setAddress(agentAddress);
        agent.setPort(agentPort);
        agent.setUnidirectional(unidirectional);
        return agent;
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#agentSendFileData(org.hyperic.hq.appdef.shared.AppdefEntityID, org.hyperic.hq.agent.FileData[], java.io.InputStream[])
     */
    public FileDataResult[] agentSendFileData(AppdefEntityID id,
                                              FileData[] destFiles, 
                                              InputStream[] streams)
            throws AgentRemoteException, AgentConnectionException {
        
        assertOnlyPingsAllowedForAgentRegistration();
        
        // TODO need to support file transfer
        throw new UnsupportedOperationException("file transfer not supported");
    }
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#die()
     */
    public void die() throws AgentRemoteException, AgentConnectionException {
        assertOnlyPingsAllowedForAgentRegistration();
        
        AgentCommandsClient proxy = null;
        
        try {
            proxy = (AgentCommandsClient)getAsynchronousProxy(AgentCommandsClient.class, false);
            proxy.die();        
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#ping()
     */
    public long ping() throws AgentRemoteException, AgentConnectionException {
        if (_agentRegistrationClient && getAgent().isUnidirectional()) {
            // The unidirectional client does not work yet since the agent 
            // is not aware of its agent token at this time
            return 0;
        }
        
        AgentCommandsClient proxy = null;
        
        try {
            proxy = (AgentCommandsClient)getSynchronousProxy(AgentCommandsClient.class);
            
            long sendTime = System.currentTimeMillis();
            proxy.ping();
            long recvTime = System.currentTimeMillis();
            return (recvTime-sendTime);
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#restart()
     */
    public void restart() throws AgentRemoteException, AgentConnectionException {
        assertOnlyPingsAllowedForAgentRegistration();
        
        AgentCommandsClient proxy = null;
        
        try {
            proxy = (AgentCommandsClient)getAsynchronousProxy(AgentCommandsClient.class, false);
            proxy.restart();           
        } finally {
            safeDestroyService(proxy);
        }
    }
    
    private void assertOnlyPingsAllowedForAgentRegistration() 
        throws AgentConnectionException {
        
        if (_agentRegistrationClient) {
            throw new AgentConnectionException("Only client ping is allowed");
        }        
    }    

}
