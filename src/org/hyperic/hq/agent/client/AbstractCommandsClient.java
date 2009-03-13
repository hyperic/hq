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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.hq.transport.util.TimeoutException;
import org.hyperic.hq.transport.util.TransportUtils;

/**
 * The abstract class that all Commands Clients using the new transport should 
 * extend.
 */
public abstract class AbstractCommandsClient {

    protected final Log _log = LogFactory.getLog(this.getClass());
    private final Agent _agent;
    private final AgentProxyFactory _factory;

    protected AbstractCommandsClient(Agent agent, AgentProxyFactory factory) {
        _agent = agent;
        _factory = factory;
    }
    
    public Agent getAgent() {
        return _agent;
    }

    protected final void safeDestroyService(Object proxy) {
        try {
            _factory.destroyService(proxy);
        } catch (Throwable t) {
        }
    }
    
    /**
     * Retrieve a synchronous proxy to a remote service.
     * 
     * @param serviceInterface The service interface. It is expected that all 
     *                         service interface operations throw an 
     *                         {@link AgentRemoteException}.
     * @return The synchronous proxy.
     * @throws AgentConnectionException if there is an error creating the proxy.
     * @throws IllegalArgumentException if any of the service interface operations 
     *                                  do not throw an {@link AgentRemoteException}.
     */
    protected final Object getSynchronousProxy(Class serviceInterface) 
        throws AgentConnectionException {
        
        TransportUtils.assertOperationsThrowException(serviceInterface, AgentRemoteException.class);
        
        Object proxy;
    
        try {
            proxy = _factory.createSyncService(_agent, serviceInterface);
        } catch (Exception e) {
            _log.error("Error creating proxy to remote service.", e);
            throw new AgentConnectionException("Error creating proxy to remote service.", e);
        }
        
        if (_agent.isUnidirectional()) {
            // For unidirectional agents, if a synchronous request blocks for 
            // more than the specified timeout (see ResponseHandler#RESPONSE_WAIT_TIME) 
            // waiting for the response, then a TimeoutException is thrown. This 
            // exception will be wrapped in an HQ-specific checked exception 
            // (AgentRemoteException).
            
            final InvocationHandler handler = Proxy.getInvocationHandler(proxy);
                        
            InvocationHandler timeoutExceptionHandler = new InvocationHandler() {

                public Object invoke(Object proxy, Method method, Object[] args) 
                    throws Throwable {
                    
                    try {
                        return handler.invoke(proxy, method, args);
                    } catch (TimeoutException e) {
                        throw new AgentRemoteException(
                                "Timeout occurred waiting on agent response.", e);
                    }
                }

            };

            proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
                                           new Class[]{serviceInterface}, 
                                           timeoutExceptionHandler);
        }
    
        return proxy;
    }

    protected final Object getAsynchronousProxy(Class serviceInterface, 
                                                boolean guaranteed) 
        throws AgentConnectionException {

        Object proxy;

        try {
            proxy = _factory.createAsyncService(_agent, 
                                                serviceInterface, 
                                                guaranteed);
        } catch (Exception e) {
            _log.error("Error creating proxy to remote service.", e);
            throw new AgentConnectionException("Error creating proxy to remote service.", e);
        }

        return proxy;
    }
    
}
