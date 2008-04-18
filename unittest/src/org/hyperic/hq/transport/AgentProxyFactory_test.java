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

import junit.framework.TestCase;

import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.transport.util.AsynchronousInvoker;

/**
 * Tests the AgentProxyFactory class.
 */
public class AgentProxyFactory_test extends TestCase {

    private AgentProxyFactory _agentProxyFactory;
    
    /**
     * Creates an instance.
     *
     * @param name
     */
    public AgentProxyFactory_test(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        _agentProxyFactory = new AgentProxyFactoryImpl(new AsynchronousInvoker(1));
    }
    
    /**
     * Async service operations must all have a void return type.
     */
    public void testCreateAsyncServiceNonVoidReturnType() throws Exception {        
        try {
            AgentValue agent = new AgentValue();
            _agentProxyFactory.createAsyncService(agent, BadAsyncService.class, false, true);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected IllegalArgumentException instead of: "+e);
        }
    }
    
    /**
     * This should be allowed.
     */
    public void testDestroyNullProxy() {
        _agentProxyFactory.destroyService(null);
    }
    
    private interface BadAsyncService extends GoodAsyncService {        
        Object badOperation();
    }
    
    private interface GoodAsyncService {
        void goodOperation();
    }

}
