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

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.security.ServerKeystoreConfig;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.util.security.KeystoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A factory for returning AI Commands clients depending on if the agent uses
 * the legacy or new transport.
 */
@Component
public class AICommandsClientFactory {

    private final AgentManager agentManager;
    
    private final AgentProxyFactory agentProxyFactory;
    private KeystoreConfig keystoreConfig;
    private boolean acceptUnverifiedCertificates;
    
    @Autowired 
    public AICommandsClientFactory(AgentManager agentManager, 
                                   AgentProxyFactory agentProxyFactory,
                                   ServerKeystoreConfig serverKeystoreConfig,
                                   @Value("#{securityProperties['accept.unverified.certificates']}")
                                   boolean acceptUnverifiedCertificates) {

        this.agentManager = agentManager;
        this.agentProxyFactory = agentProxyFactory;
        keystoreConfig = serverKeystoreConfig;
        this.acceptUnverifiedCertificates = acceptUnverifiedCertificates;
    }

    public AICommandsClient getClient(AppdefEntityID aid) throws AgentNotFoundException {

        Agent agent = agentManager.getAgent(aid);

        return getClient(agent);
    }

    public AICommandsClient getClient(String agentToken) throws AgentNotFoundException {

        Agent agent = agentManager.getAgent(agentToken);

        return getClient(agent);
    }

    private AICommandsClient getClient(Agent agent) {
        if (agent.isNewTransportAgent()) {

            return new AICommandsClientImpl(agent, agentProxyFactory);
        } else {
            return new LegacyAICommandsClientImpl(
                new SecureAgentConnection(agent.getAddress(), agent.getPort(),
                                          agent.getAuthToken(), keystoreConfig,
                                          acceptUnverifiedCertificates));
        }
    }

}
