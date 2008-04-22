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

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.transport.AgentProxyFactory;

/**
 * A factory for returning Agent Commands clients depending on if the agent 
 * uses the legacy or new transport.
 */
public class AgentCommandsClientFactory {

    private static final AgentCommandsClientFactory INSTANCE = new AgentCommandsClientFactory();

    private AgentCommandsClientFactory() {
    }

    public static AgentCommandsClientFactory getInstance() {
        return INSTANCE;
    }

    public AgentCommandsClient getClient(AppdefEntityID aid) 
        throws AgentNotFoundException {
        
        Agent agent = AgentManagerEJBImpl.getOne().getAgent(aid);

        return getClient(agent);
    }

    public AgentCommandsClient getClient(String agentToken) 
        throws AgentNotFoundException {
        
        Agent agent = AgentManagerEJBImpl.getOne().getAgent(agentToken);

        return getClient(agent);
    }
    
    public AgentCommandsClient getClient(Agent agent) {
        if (agent.isNewTransportAgent()) {
            AgentProxyFactory factory = HQApp.getInstance().getAgentProxyFactory();
            
            return new AgentCommandsClientImpl(agent, factory);
        } else {
            return new LegacyAgentCommandsClientImpl(new SecureAgentConnection(agent));            
        }         
    }    
    
    public AgentCommandsClient getClient(String agentAddress, 
                                         int agentPort, 
                                         String authToken, 
                                         boolean isNewTransportAgent,
                                         boolean unidirectional) {
        if (isNewTransportAgent) {
            AgentProxyFactory factory = 
                HQApp.getInstance().getAgentProxyFactory();
            
            return new AgentCommandsClientImpl(factory, agentAddress, agentPort, unidirectional);
        } else {
            return new LegacyAgentCommandsClientImpl(
                    new SecureAgentConnection(agentAddress, agentPort, authToken));
        }    
    }    
    
}
