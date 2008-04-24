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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.client.AgentClient;
import org.hyperic.hq.transport.AgentTransport;

/**
 * A factory for creating agent transports.
 */
public class AgentTransportFactory implements AgentNotificationHandler {
    
    private static final Log _log = LogFactory.getLog(AgentTransportFactory.class);
    
    private final AgentDaemon _agent;
    private final AgentConfig _bootConfig;
    private final AgentStorageProvider _storageProvider;
    private AgentTransport _agentTransport;
    
    /**
     * Creates an instance.
     *
     * @param agent The agent.
     * @param bootConfig The boot config.
     * @param storageProvider The storage provider.
     */
    public AgentTransportFactory(AgentDaemon agent,
                                 AgentConfig bootConfig, 
                                 AgentStorageProvider storageProvider) {
        _agent = agent;
        _bootConfig = bootConfig;
        _storageProvider = storageProvider;
    }
    
    /**
     * @return An agent transport that has not been started yet.
     * 
     * @throws ClassNotFoundException if this is a .ORG instance and attempting 
     *                                to use the unidirectional transport.
     */
    public AgentTransport createAgentTransport() throws Exception {                    
        Properties bootProperties = _bootConfig.getBootProperties();
        
        String unidirectionalString = 
            bootProperties.getProperty(AgentClient.QPROP_UNI, 
                                       Boolean.FALSE.toString());
        boolean unidirectional = 
            Boolean.valueOf(unidirectionalString).booleanValue();
        
        String host = bootProperties.getProperty(AgentClient.QPROP_IPADDR);
        
        String portString = bootProperties.getProperty(AgentClient.QPROP_SSLPORT, 
                                                       String.valueOf(7443));
        int port = Integer.valueOf(portString).intValue();
        
        String pollingFrequencyString = bootProperties.getProperty(
                AgentClient.QPROP_UNI_POLLING_FREQUENCY, String.valueOf(1000));
        
        long pollingFrequency = Long.valueOf(pollingFrequencyString).longValue();
        
        ProviderInfo providerInfo = 
            CommandsAPIInfo.getProvider(_storageProvider);
        
        if (providerInfo == null) {
            _log.info("Agent token is not currently set. " +
            		  "Registering handler to notify agent transport when token is set.");
        }
        
        String agentToken = providerInfo.getAgentToken();
                        
        if (unidirectional) {
            _log.info("Setting up unidirectional transport");
            
            InetSocketAddress pollerBindAddr = 
                new InetSocketAddress(host, port);
            
            _agentTransport = 
                new AgentTransport(pollerBindAddr, 
                                   "transport/ServerInvokerServlet", 
                                   true, 
                                   agentToken, 
                                   unidirectional, 
                                   pollingFrequency, 
                                   1);
        } else {
            _log.info("Setting up bidirectional transport");
            // TODO need to implement bidirectional transport and return 
            // an agent transport instead of null
            _agentTransport = null;
        }
        
        // register handler to be notified when the agent token is set (or reset)
        _agent.registerNotifyHandler(this, CommandsAPIInfo.NOTIFY_SERVER_SET);
        
        return _agentTransport;
    }

    /**
     * Notification handles agent token reset.
     * 
     * @see org.hyperic.hq.agent.server.AgentNotificationHandler#handleNotification(java.lang.String, java.lang.String)
     */
    public void handleNotification(String msgClass, String msg) {
        ProviderInfo providerInfo = 
            CommandsAPIInfo.getProvider(_storageProvider);
        
        if (providerInfo == null) {
            _log.error("Agent transport expected agent token set but " +
            		   "storage provider does not have token.");
        } else {
            String agentToken = providerInfo.getAgentToken();
            
            _log.info("Updating agent transport with new agent token: "+agentToken);
            
            if (_agentTransport != null) {
                _agentTransport.updateAgentToken(agentToken);
            }
        }
    }

}
