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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.client.AgentClient;
import org.hyperic.hq.bizapp.client.AgentCallbackClient;
import org.hyperic.hq.common.YesOrNo;
import org.hyperic.hq.transport.AgentTransport;
import org.jboss.remoting.InvokerLocator;

/**
 * The class that manages the agent transport lifecycle.
 */
public final class AgentTransportLifecycleImpl implements AgentTransportLifecycle {
    
    private static final Log _log = LogFactory.getLog(AgentTransportLifecycleImpl.class);
    
    private static final String REMOTE_TRANSPORT_LOCATOR_PATH = "ServerInvokerServlet";
    
    private final Object _lock = new Object();
    private final AgentDaemon _agent;
    private final AgentConfig _config;
    private final AgentStorageProvider _storageProvider;
    private final Map _serviceInterfaceName2ServiceInterface;
    private final Map _serviceInterface2ServiceImpl;
    private AgentTransport _agentTransport;
    private InvokerLocator _remoteTransportLocator;
    
    public AgentTransportLifecycleImpl(AgentDaemon agent,
                                       AgentConfig bootConfig, 
                                       AgentStorageProvider storageProvider) {
        _agent = agent;
        _config = bootConfig;
        _storageProvider = storageProvider;
        _serviceInterfaceName2ServiceInterface = new HashMap();
        _serviceInterface2ServiceImpl = new HashMap();
        
        // Normally we don't want 'this' to escape the constructor, but 
        // we made this class final so we don't have to worry about a 
        // not fully initialized instance of a subclass (of this class) 
        // handled by another thread.
        
        // register handler to be notified when the transport layer 
        // configuration is finally set
        _agent.registerNotifyHandler(this, CommandsAPIInfo.NOTIFY_SERVER_SET);
    }
            
    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#startAgentTransport()
     */
    public void startAgentTransport() throws Exception {        
        // Start the agent transport here - only if all configuration properties 
        // are available thru boot props - before starting, register all the services with the agent transport
        
        // Boot properties override stored values        
        ProviderInfo provider = CommandsAPIInfo.getProvider(_storageProvider);
        
        Properties bootProperties = _config.getBootProperties();
        
        boolean isNewTransport = isNewTransport(bootProperties, provider);
        
        if (!isNewTransport) {
            _log.info("Agent is not using new transport.");
            return;
        }
        
        _log.info("Agent is using the new transport. " +
        		  "Looking for properties to start the new transport.");
        
        boolean isUnidirectionalPropertySet = 
            isUnidirectionalPropertySet(bootProperties, provider);
        
        _log.info("Unidirectional property set="+isUnidirectionalPropertySet);
        
        boolean unidirectional = false;
        
        if (isUnidirectionalPropertySet) {
            unidirectional = isUnidirectional(bootProperties, provider);
            
            _log.info("Unidirectional="+unidirectional);
        }
        
        String host = getHost(bootProperties, provider);
        
        if (host == null) {
            _log.info("Host is not currently set.");
        } else {
            _log.info("Host="+host);
        }
        
        int unidirectionalPort = getUndirectionalPort(bootProperties, provider);
        
        if (unidirectionalPort == -1) {
            _log.info("Unidirectional port is not currently set.");
        } else {
            _log.info("Unidirectional port="+unidirectionalPort);                
        }            
        
        String agentToken = getAgentToken(provider);
        
        if (agentToken == null) {
            _log.info("Agent token is not currently set. " +
                      "Registering handler to notify agent transport when token is set.");
        } else {
            _log.info("Agent token="+agentToken);                
        }
        
        long pollingFrequency = getPollingFrequency(bootProperties);
        
        _log.info("Polling frequency="+pollingFrequency);
        
        if (!isUnidirectionalPropertySet) {
            _log.info("Cannot start new transport since we do not " +
            		  "know if the transport is unidirectional.");
            return;
        }
        
        if (host == null) {
            _log.info("Cannot start new transport since we do not know the host.");
            
            return;
        }
        
        if (unidirectional) {
            if (unidirectionalPort == -1) {
                _log.info("Cannot start new transport since we do not " +
                          "know the server port for the unidirectional transport.");
                return;
            }
            
            _log.info("Setting up unidirectional transport");
                        
            InetSocketAddress pollerBindAddr = 
                new InetSocketAddress(host, unidirectionalPort);
            
            if (_config.isProxyServerSet()) {                
                _log.info("Configuring proxy host and port: host="+
                         _config.getProxyIp()+"; port="+_config.getProxyPort());
                
                System.setProperty("https.proxyHost", _config.getProxyIp());
                System.setProperty("https.proxyPort", String.valueOf(_config.getProxyPort()));
            }            
            
            _agentTransport = 
                new AgentTransport(pollerBindAddr, 
                                   REMOTE_TRANSPORT_LOCATOR_PATH, 
                                   true, 
                                   agentToken, 
                                   unidirectional, 
                                   pollingFrequency, 
                                   1);
        } else {
            _log.info("Setting up bidirectional transport");
            // TODO need to implement bidirectional transport and return 
            // an agent transport instead of null
            // do we need to set up a proxy server for http or https protocol?
            _agentTransport = null;
        }        
        
        if (_agentTransport != null) {
            synchronized (_lock) {
                _remoteTransportLocator = _agentTransport.getRemoteEndpointLocator();
            }
            
            // register the services and start the server            
            for (Iterator iter = _serviceInterface2ServiceImpl.entrySet().iterator(); 
                 iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                Class serviceInterface = (Class)entry.getKey();
                Object serviceImpl = (Object)entry.getValue();
                _agentTransport.registerService(serviceInterface, serviceImpl);
            }
            
            _agentTransport.start();
        }

    }
    
    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#stopAgentTransport()
     */
    public void stopAgentTransport() {
        if (_agentTransport != null) {
            try {
                _agentTransport.stop();
            } catch (InterruptedException e) {
            }
            
            _agentTransport = null;
            
            synchronized (_lock) {
               _remoteTransportLocator = null;
            }
        }
    }
    
    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#handleNotification(java.lang.String, java.lang.String)
     */
    public void handleNotification(String msgClass, String msg) {
        ProviderInfo provider = CommandsAPIInfo.getProvider(_storageProvider);
        Properties bootProperties = _config.getBootProperties();
        
        if (!isNewTransport(bootProperties, provider)) {
            _log.info("Stopping agent transport.");
            stopAgentTransport();
            return;
        }
        
        // Start the agent transport if configuration properties were 
        // not available on the original start attempt.
        if (_agentTransport == null) {
            try {
                startAgentTransport();
            } catch (Exception e) {
                _log.error("Failed to start agent transport after agent setup", e);
                return;
            }
        }
        
        // If the agent transport is still not started and we are using 
        // the new transport, then we have a problem!
        if (_agentTransport == null && isUnidirectional(bootProperties, provider)) {            
            _log.error("Failed to start agent transport after agent setup");                
            
            return;
        }
        
        // Update the agent transport agent token
        if (provider == null) {
            _log.error("Agent transport expected agent token set but " +
                       "storage provider does not have token.");
        } else {            
            if (_agentTransport != null) {
                String agentToken = provider.getAgentToken();
                
                _log.info("Updating agent transport with new agent token: "+agentToken);
                
                _agentTransport.updateAgentToken(agentToken);                
            }
        }
    }
    
    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#registerService(java.lang.Class, java.lang.Object)
     */
    public void registerService(Class serviceInterface, Object serviceImpl) {
        Class oldInterface = (Class)_serviceInterfaceName2ServiceInterface.get(serviceInterface.getName());
        
        if (oldInterface == null) {
            _serviceInterfaceName2ServiceInterface.put(serviceInterface.getName(), serviceInterface);
            _serviceInterface2ServiceImpl.put(serviceInterface, serviceImpl);                    
        } else {
            _serviceInterfaceName2ServiceInterface.remove(serviceInterface.getName());
            _serviceInterface2ServiceImpl.remove(oldInterface);
            _serviceInterfaceName2ServiceInterface.put(serviceInterface.getName(), serviceInterface);
            _serviceInterface2ServiceImpl.put(serviceInterface, serviceImpl); 
        }
    }
    
    /**
     * @see org.hyperic.hq.agent.server.AgentTransportLifecycle#getRemoteTransportLocator()
     */
    public InvokerLocator getRemoteTransportLocator() {
        synchronized (_lock) {
            return _remoteTransportLocator;
        }
    }    
    
    private boolean isNewTransport(Properties bootProperties, ProviderInfo provider) {
        boolean isNewTransport = false;
        
        String isNewTransportString = 
            bootProperties.getProperty(AgentClient.QPROP_NEWTRANSPORT);
        
        if (isNewTransportString == null) {
            if (provider != null) {
                isNewTransport = provider.isNewTransport();                
            }
        } else {
            isNewTransport = 
                YesOrNo.valueFor(isNewTransportString).toBoolean().booleanValue();
        }
        
        return isNewTransport;
    }
    
    private boolean isUnidirectionalPropertySet(Properties bootProperties, ProviderInfo provider) {        
        String isUnidirectionalString = 
            bootProperties.getProperty(AgentClient.QPROP_UNI);
        
        if (isUnidirectionalString != null) {
            return true;
        } else if (provider != null) {
            return provider.isNewTransport();
        } else {
            return false;
        }  
    }
    
    private boolean isUnidirectional(Properties bootProperties, ProviderInfo provider) {
        boolean isUnidirectional = false;
        
        String isUnidirectionalString = 
            bootProperties.getProperty(AgentClient.QPROP_UNI);
        
        if (isUnidirectionalString == null && provider != null) {
            if (provider.isNewTransport()) {
                isUnidirectional = provider.isUnidirectional();
            }
        } else {
            isUnidirectional = 
                YesOrNo.valueFor(isUnidirectionalString).toBoolean().booleanValue();
        }
        
        return isUnidirectional;    
    }
    
    private String getHost(Properties bootProperties, ProviderInfo provider) {
        String host = bootProperties.getProperty(AgentClient.QPROP_IPADDR);
                
        if (host == null && provider != null) {
            host = AgentCallbackClient.getHostFromProviderURL(provider.getProviderAddress());
        }
        
        return host;              
    }   
    
    private int getUndirectionalPort(Properties bootProperties, ProviderInfo provider) {
        int port = -1;
        
        String portString = 
            bootProperties.getProperty(AgentClient.QPROP_SSLPORT);
        
        if (portString == null) {
            if (provider != null && provider.isNewTransport()) {
                port = provider.getUnidirectionalPort();
            }
        } else {
            port = Integer.valueOf(portString).intValue();
        }
        
        return port;              
    }   

    private long getPollingFrequency(Properties bootProperties) {
        String pollingFrequencyString = bootProperties.getProperty(
                AgentClient.QPROP_UNI_POLLING_FREQUENCY);
        
        if (pollingFrequencyString == null) {
            return 1000;
        } else {
            return Long.valueOf(pollingFrequencyString).longValue();            
        }
    }
    
    private String getAgentToken(ProviderInfo provider) {
        if (provider != null) {
            return provider.getAgentToken();
        } else {
            return null;
        }
    }

}