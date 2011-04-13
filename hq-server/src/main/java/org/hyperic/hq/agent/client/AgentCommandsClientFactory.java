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
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A factory for returning Agent Commands clients depending on if the agent uses
 * the legacy or new transport.
 * TODO remove agentProxyFactory
 */
@Component
public class AgentCommandsClientFactory {

    private final AgentProxyFactory agentProxyFactory;

    @Autowired
    public AgentCommandsClientFactory(AgentProxyFactory agentProxyFactory) {
        this.agentProxyFactory = agentProxyFactory;
    }

    public AgentCommandsClient getClient(Agent agent) {
        if (agent.isNewTransportAgent()) {
            return new AgentCommandsClientImpl(agent, agentProxyFactory);
        } else {
            return new LegacyAgentCommandsClientImpl(new SecureAgentConnection(agent.getAddress(),
                agent.getPort(), agent.getAuthToken()));
        }
    }

    public AgentCommandsClient getClient(String agentAddress, int agentPort, String authToken,
                                         boolean isNewTransportAgent, boolean unidirectional) {
        if (isNewTransportAgent) {
            return new AgentCommandsClientImpl(agentProxyFactory, agentAddress, agentPort, unidirectional);
        } else {
            return new LegacyAgentCommandsClientImpl(new SecureAgentConnection(agentAddress,
                agentPort, authToken));
        }
    }
    
    /*private OperationService operationService;

    @Autowired
    public AgentCommandsClientFactory(AgentProxyFactory agentProxyFactory, OperationService operationService) {
        this.agentProxyFactory = agentProxyFactory;
        this.operationService = operationService;
    }

    *//**
     * Returns the implementation of AgentCommandsClient to perform communication operations.
     * @param agentAddress        the agent address
     * @param agentPort           the agent port
     * @param authToken           the auth token
     * @param isNewTransportAgent true if agent is new transport agent, false if not. NOTE: will do away with this
     * @param unidirectional      whether or not this is a unidirectional agent
     * @return org.hyperic.hq.amqp.AmqpAgentCommandsClient
     *//*
    public AgentCommandsClient getClient(String agentAddress, int agentPort, String authToken, boolean isNewTransportAgent, boolean unidirectional) {
        return getClient(Agent.create(agentAddress, agentPort, unidirectional, authToken, isNewTransportAgent));
    }

    *//**
     * Returns the implementation of AgentCommandsClient to perform communication operations.
     * @param agent the Agent
     * @return org.hyperic.hq.amqp.AmqpAgentCommandsClient
     *//*
    public AgentCommandsClient getClient(Agent agent) {
        if (agent.isUnidirectional())
            throw new UnsupportedOperationException("unidirectional transport not supported.");

        return new AmqpCommandOperationService(operationService, createClient(agent), agent.isUnidirectional());
    }

    *//**
     * Handles logic for creation of the appropriate legacy client implementation.
     * @param agent
     * @return the client to return
     * @see org.hyperic.hq.agent.client.AgentCommandsClient
     *//*
    private AgentCommandsClient createClient(Agent agent) {
        return agent.getIsNewTransportAgent() ? new AgentCommandsClientImpl(agent, agentProxyFactory)
                : new LegacyAgentCommandsClientImpl(new SecureAgentConnection(agent.getAddress(), agent.getPort(), agent.getAuthToken()));
    }*/

    /*
    private final AgentProxyFactory agentProxyFactory;

    @Autowired
    public AgentCommandsClientFactory(AgentProxyFactory agentProxyFactory) {
        this.agentProxyFactory = agentProxyFactory;
    }
    public AgentCommandsClient getClient(Agent agent) {
        if (agent.isNewTransportAgent()) {
            return new AgentCommandsClientImpl(agent, agentProxyFactory);
        } else {
            return new LegacyAgentCommandsClientImpl(new SecureAgentConnection(agent.getAddress(),
                agent.getPort(), agent.getAuthToken()));
        }
    }

    public AgentCommandsClient getClient(String agentAddress, int agentPort, String authToken,
                                         boolean isNewTransportAgent, boolean unidirectional) {
        if (isNewTransportAgent) {
            return new AgentCommandsClientImpl(agentProxyFactory, agentAddress, agentPort, unidirectional);
        } else {
            return new LegacyAgentCommandsClientImpl(new SecureAgentConnection(agentAddress,
                agentPort, authToken));
        }
    }*/

}
