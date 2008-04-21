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

import org.hyperic.hq.appdef.Agent;

/**
 * The interface for classes that create proxies to agent services.
 */
public interface AgentProxyFactory {

    /**
     * Create a proxy to a synchronous agent service.
     * 
     * @param agent The agent.
     * @param serviceInterface The service interface.
     * @param unidirectional <code>true</code> for a unidirectional transport; 
     *                       <code>false</code> for a bidirectional transport.
     * @return A proxy to the agent service.
     * @throws Exception if an exception occurs acquiring the proxy.
     */
    Object createSyncService(Agent agent, Class serviceInterface,
            boolean unidirectional) throws Exception;

    /**
     * Create a proxy to an asynchronous agent service where proxy method 
     * invocations return immediately.
     * 
     * @param agent The agent.
     * @param serviceInterface The service interface. All interface operations 
     *                         should have a <code>void</code> return type.
     * @param guaranteed <code>true</code> to guarantee message delivery;
     *                   <code>false</code> if guaranteed delivery is not required.                        
     * @param unidirectional <code>true</code> for a unidirectional transport; 
     *                       <code>false</code> for a bidirectional transport.
     * @return A proxy to the agent service.
     * @throws IllegalArgumentException if any of the interface operations do 
     *                                  not have a <code>void</code> return type.
     * @throws Exception if an exception occurs acquiring the proxy.
     */
    Object createAsyncService(Agent agent, Class serviceInterface,
            boolean guaranteed, boolean unidirectional) throws Exception;

    /**
     * When a proxy to an agent service is no longer in use, it should be 
     * destroyed to reclaim resources.
     * 
     * @param proxy The proxy to an agent service. Null values should 
     *              be handled gracefully.
     */
    void destroyService(Object proxy);

}