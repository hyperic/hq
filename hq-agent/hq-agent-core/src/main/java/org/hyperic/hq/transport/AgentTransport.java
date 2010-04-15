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

package org.hyperic.hq.transport;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.transport.util.TransportUtils;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transporter.TransporterServer;


/**
 * The transport for the HQ agent. Services hosted by this transport should be 
 * registered before the transport is started.
 */
public class AgentTransport {
    
    private final Object _lock = new Object();

    private final PollerClient _pollerClient;
    
    private final TransporterServer _server;
    
    private final boolean _unidirectional;
    
    private final InvokerLocator _remoteEndpointLocator;
    
    private boolean _stopped;
    
    
    /**
     * Creates an instance.
     *
     * @param serverTransportAddr The listening socket address on the server transport.
     * @param path The invoker locator path or <code>null</code>.
     * @param encrypted <code>true</code> if using encrypted communication; 
     *                  <code>false</code> if not encrypted.
     * @param agentToken The agent token uniquely identifying the agent.
     * @param unidirectional <code>true</code> to use a unidirectional transport; 
     *                       <code>false</code> to use a bidirectional transport.
     * @param pollingFrequency The polling frequency in milliseconds.
     *                         This parameter is ignored for bidirectional transports.
     * @param asyncThreadPoolSize The thread pool size for the asynchronous invoker. 
     *                            This parameter is ignored for bidirectional transports.
     * @throws ClassNotFoundException if this is a .ORG instance and attempting 
     *                                to use the unidirectional transport.                           
     * @throws Exception if instance creation fails.
     */    
    public AgentTransport(InetSocketAddress serverTransportAddr, 
                          String path, 
                          boolean encrypted,
                          String agentToken, 
                          boolean unidirectional,
                          long pollingFrequency, 
                          int asyncThreadPoolSize) throws Exception {
        
        _unidirectional = unidirectional;
        
        InvokerLocator remotingServerInvokerLocator;
        
        if (_unidirectional) {
            _pollerClient = createPollerClient(serverTransportAddr, 
                                               path, 
                                               encrypted, 
                                               pollingFrequency, 
                                               agentToken, 
                                               asyncThreadPoolSize);
            // for a unidirectional agent - we can use a local invoker when registering 
            // services - since the poller client is in the agent's vm
            remotingServerInvokerLocator = getLocalInvokerLocator();
            _remoteEndpointLocator = _pollerClient.getRemoteEndpointLocator();
        } else {
            // TODO - need to specify the invoker locator for bidirectional 
            // (both remoting server invoker locator and remote end point invoker locator)
            remotingServerInvokerLocator = null;
            _remoteEndpointLocator = null;
            throw new UnsupportedOperationException("bidirectional not supported yet");
        }
        
        _server = TransporterServer.createTransporterServer(remotingServerInvokerLocator, 
                                                            new BootStrapService());
    }
    
    /**
     * @return The invoker locator for the remote end point to which this 
     *         transport is connected.
     */
    public InvokerLocator getRemoteEndpointLocator() {
        return _remoteEndpointLocator;
    }
    
    /**
     * Create the poller client. A ClassNotFoundException is thrown if this is 
     * a .ORG instance. The unidirectional transport that requires the poller 
     * client is only supported in EE.
     */
    private PollerClient createPollerClient(InetSocketAddress serverTransportAddr, 
                                            String path, 
                                            boolean encrypted, 
                                            long pollingFrequency, 
                                            String agentToken, 
                                            int asyncThreadPoolSize) 
        throws ClassNotFoundException, Exception {
        
        Class clazz;
        
        try {
            clazz = TransportUtils.tryLoadUnidirectionalTransportPollerClient();           
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(
                    "Unidirectional transport is not available in .ORG");
        }        
        
        Constructor constructor = clazz.getConstructor(
                                new Class[]{InetSocketAddress.class, 
                                            String.class, 
                                            Boolean.TYPE, 
                                            Long.TYPE, 
                                            String.class, 
                                            Integer.TYPE});
        
        
        return (PollerClient)constructor.newInstance(
                new Object[]{serverTransportAddr, 
                             path, 
                             Boolean.valueOf(encrypted), 
                             new Long(pollingFrequency), 
                             agentToken, 
                             new Integer(asyncThreadPoolSize)});
    }
    
    /**
     * Register a service to be hosted by this transport.
     * 
     * @param serviceInterface The service interface class.
     * @param serviceImpl The service implementation.
     * @throws IllegalArgumentException if the service does not implement the 
     *                                  interface.
     * @throws Exception if service registration fails.
     */
    public void registerService(Class serviceInterface, Object serviceImpl) 
        throws Exception {
        
        verifyServiceImplementsInterface(serviceInterface, serviceImpl);
        
        _server.addHandler(serviceImpl, serviceInterface.getName());
    }
    
    /**
     * Update the agent token uniquely identifying the agent.
     * 
     * @param agentToken The agent token.
     * @throws NullPointerException if the agent token is <code>null</code>.
     */
    public void updateAgentToken(String agentToken) {
        if (agentToken == null) {
            throw new NullPointerException("agent token is null");
        }
        
        _pollerClient.updateAgentToken(agentToken);
    }

    /**
     * Start the transport.
     * 
     * @throws Exception
     */
    public void start() throws Exception {
        if (isStopped()) {
            return;
        }
        
        _server.start();
        
        if (_unidirectional) {
            _pollerClient.start();            
        }
    }
    
    /**
     * Stop the transport. Once stopped, it cannot be started again.
     */
    public void stop() throws InterruptedException {
        if (_unidirectional) {
            _pollerClient.stop();
        }  
        
        _server.stop();
        setStopped();
    }
    
    private InvokerLocator getLocalInvokerLocator() {
    	// Suspected bug in JBoss: it looks like the stop() method of TransportServer doesn't
    	// internally shut down its Connector in a timely synchronous fashion.  This is mostly
    	// harmless, except in unittests, which create and destroy AgentTransports one after
    	// another.  Putting in params with a timestamp is a cheesy workaround.
    	Map params = new HashMap(1);
    	params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return new InvokerLocator("local", "localhost", -1, null, params);
    }
    
    private void verifyServiceImplementsInterface(Class serviceInterface, Object serviceImpl) {
        Class[] interfaces = serviceImpl.getClass().getInterfaces();
        
        for (int i = 0; i < interfaces.length; i++) {
            Class interfaceClazz = interfaces[i];
            
            if (interfaceClazz.equals(serviceInterface)) {
                return;
            }
        }
        
        throw new IllegalArgumentException("service: "+serviceImpl.getClass()+
                " does not implement interface: "+serviceInterface);
    }
    
    private void setStopped() {
        synchronized (_lock) {
            _stopped = true;
        }
    }
    
    private boolean isStopped() {
        synchronized (_lock) {
            return _stopped;
        }
    }
    
    private static class BootStrapService {
        void doNothing() {
            // no-op
        }
    }
    
}
