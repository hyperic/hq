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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.transport.AgentProxyFactory;

/**
 * The abstract class that all Commands Clients using the new transport should 
 * extend.
 */
public abstract class AbstractCommandsClient {

    protected final Log log = LogFactory.getLog(this.getClass());
    private final Agent _agent;
    private final AgentProxyFactory _factory;

    protected AbstractCommandsClient(Agent agent, AgentProxyFactory factory) {
        _agent = agent;
        _factory = factory;
    }

    protected final void safeDestroyService(Object proxy) {
        try {
            _factory.destroyService(proxy);
        } catch (Throwable t) {
        }
    }

    protected final Object getSynchronousProxy(Class serviceInterface) 
        throws AgentConnectionException {
        
        Object proxy;
    
        try {
            proxy = _factory.createSyncService(_agent, 
                                               serviceInterface, 
                                               _agent.isUnidirectional());
        } catch (Exception e) {
            throw new AgentConnectionException("Error creating proxy to remote service.", e);
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
                                                guaranteed, 
                                                _agent.isUnidirectional());
        } catch (Exception e) {
            throw new AgentConnectionException("Error creating proxy to remote service.", e);
        }

        return proxy;
    }

}
