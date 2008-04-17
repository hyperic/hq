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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.transport.util.AsynchronousInvoker;


/**
 * The transport for the HQ server.
 */
public class ServerTransport {
    
    private static final Log _log = LogFactory.getLog(ServerTransport.class);
    
    private final Object _lock = new Object();
    
    private final AsynchronousInvoker _asyncInvoker;
    
    private final AgentProxyFactory _agentProxy;
    
    private final PollerServer _pollerServer;
    
    private boolean _ready;
    
    private boolean _stopped;
    
    /**
     * Creates an instance without an embedded server. This type of transport 
     * may be created when connections to the agent transport are managed via 
     * an external server (such when the ServerInvokerServlet is deployed in a 
     * web container and routes requests to server invocation handlers).
     *
     * @param asyncThreadPoolSize The thread pool size for the asynchronous invoker.
     * @throws Exception if instance creation fails.
     */
    public ServerTransport(int asyncThreadPoolSize) throws Exception {        
        _asyncInvoker = new AsynchronousInvoker(asyncThreadPoolSize);
        
        _agentProxy = createAgentProxyFactory(_asyncInvoker);
        
        _pollerServer = null;
        
        // TODO - need a server for the bidirectional transport as well
    }    
    
    /**
     * Creates an instance with an embedded server.
     *
     * @param pollerServerBindAddr The bind address for the unidirectional poller server.
     * @param asyncThreadPoolSize The thread pool size for the asynchronous invoker.
     * @throws Exception if instance creation fails.
     */
    public ServerTransport(InetSocketAddress pollerServerBindAddr, 
                           int asyncThreadPoolSize) throws Exception {        
        _asyncInvoker = new AsynchronousInvoker(asyncThreadPoolSize);
        
        _agentProxy = createAgentProxyFactory(_asyncInvoker);
        
        _pollerServer = createPollerServer(pollerServerBindAddr);
        
        // TODO need a server for the bidirectional transport as well
    }
    
    /**
     * Create the agent proxy factory. First try to use reflection to load the 
     * EE version. If that fails, then we must have a .ORG instance, so return 
     * the .ORG agent proxy factory.
     */
    private AgentProxyFactory createAgentProxyFactory(AsynchronousInvoker asyncInvoker) 
        throws Exception {
        
        Class clazz;
        
        try {
            clazz = Thread.currentThread().getContextClassLoader().loadClass(
                    "com.hyperic.hq.transport.AgentProxyFactoryImpl");
        } catch (Exception e) {
            // We must have a .ORG instance
            return new AgentProxyFactoryImpl(asyncInvoker);
        }

        Constructor constructor = clazz.getConstructor(
                                new Class[]{AsynchronousInvoker.class});
        
        AgentProxyFactory agentProxyFactory = 
            (AgentProxyFactory)constructor.newInstance(new Object[]{asyncInvoker});
        
        _log.info("Server transport using the following agent proxy factory: "+
                    agentProxyFactory.getClass().getName());
        
        return agentProxyFactory;
    }
    
    /**
     * Create the poller server or return <code>null</code> if this is a 
     * .ORG instance. The unidirectional transport that requires the poller 
     * server is only supported in EE.
     */
    private PollerServer createPollerServer(InetSocketAddress pollerServerBindAddr) 
         throws Exception {
        
        Class clazz;
        
        try {
            clazz = Thread.currentThread().getContextClassLoader().loadClass(
                                    "com.hyperic.hq.transport.PollerServerImpl");
        } catch (ClassNotFoundException e) {
            // We must have a .ORG instance
            _log.info("Unidirectional transport poller server is not enabled. " +
            		  "We must have a .ORG instance.");
            
            return null;
        }
        
        Constructor constructor = clazz.getConstructor(
                                new Class[]{String.class, Integer.TYPE});
        
        
        return (PollerServer)constructor.newInstance(
                new Object[]{pollerServerBindAddr.getHostName(), 
                             Integer.valueOf(pollerServerBindAddr.getPort())});
    }
        
    /**
     * Determine if the server transport is ready to handle queries on agent 
     * services. The transport must be started for this to be <code>true</code>.
     * 
     * @return <code>true</code> if ready to handle queries; 
     *         <code>false</code> otherwise.
     */
    public boolean isReady() {
        synchronized (_lock) {
            return _ready;
        }
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
        
        _asyncInvoker.start();
        
        if (_pollerServer != null) {
            _pollerServer.start();            
        }
        
        setReady(true);
    }
    
    /**
     * Stop the transport. Once stopped, it cannot be started again.
     */
    public void stop() {
        _asyncInvoker.stop();
        
        if (_pollerServer != null) {
            _pollerServer.stop();            
        }
        
        setReady(false);
        setStopped();
    }

    /**
     * Retrieve the factory for acquiring proxies to agent services.
     * 
     * @return The agent proxy factory.
     * @throws IllegalStateException if the server transport is not started.
     */
    public AgentProxyFactory getAgentProxyFactory() {
        if (!isReady() || isStopped()) {
            throw new IllegalStateException("server transport is not started");
        }
        
        return _agentProxy;
    }
    
    private void setReady(boolean ready) {
        synchronized (_lock) {
            _ready = ready;
        }
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

}
