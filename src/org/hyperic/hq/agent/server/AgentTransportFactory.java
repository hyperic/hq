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

package org.hyperic.hq.agent.server;

import java.net.InetSocketAddress;
import java.util.Properties;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.client.AgentClient;
import org.hyperic.hq.transport.AgentTransport;

/**
 * A factory for creating agent transports.
 */
public class AgentTransportFactory {
    
    private final AgentConfig _bootConfig;
    private final AgentStorageProvider _storageProvider;
    private final boolean _unidirectional;
    
    /**
     * Creates an instance.
     *
     * @param bootConfig The boot config.
     * @param storageProvider The storage provider.
     * @param unidirectional <code>true</code> if the agent is unidirectional;
     *                       <code>false</code> if the agent is bidirectional.
     */
    public AgentTransportFactory(AgentConfig bootConfig, 
                                 AgentStorageProvider storageProvider, 
                                 boolean unidirectional) {
        _bootConfig = bootConfig;
        _storageProvider = storageProvider;
        _unidirectional = unidirectional;
    }
    
    /**
     * @return An agent transport that has not been started yet.
     */
    public AgentTransport createAgentTransport() throws Exception {                    
        Properties bootProperties = _bootConfig.getBootProperties();
        
        String host = bootProperties.getProperty(AgentClient.QPROP_IPADDR);
        
        String portString = bootProperties.getProperty(AgentClient.QPROP_SSLPORT, 
                                                       String.valueOf(7443));
        int port = Integer.valueOf(portString).intValue();
        
        String pollingFrequencyString = bootProperties.getProperty(
                AgentClient.QPROP_UNI_POLLING_FREQUENCY, String.valueOf(1000));
        
        long pollingFrequency = Long.valueOf(pollingFrequencyString).longValue();
        
        ProviderInfo providerInfo = 
            CommandsAPIInfo.getProvider(_storageProvider);
        
        String agentToken = providerInfo.getAgentToken();
        
        AgentTransport agentTransport;
        
        if (_unidirectional) {
            InetSocketAddress pollerBindAddr = 
                new InetSocketAddress(host, port);
            
            agentTransport = 
                new AgentTransport(pollerBindAddr, 
                                   "transport/ServerInvokerServlet", 
                                   true, 
                                   agentToken, 
                                   true, 
                                   pollingFrequency, 
                                   2);
        } else {
            throw new UnsupportedOperationException("bidirectional agent not supported yet");
        }
        
        return agentTransport;
    }

}
