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

import java.net.InetSocketAddress;

import junit.framework.TestCase;

/**
 * Tests the ServerTransport class.
 */
public class ServerTransport_test extends TestCase {

    /**
     * Creates an instance.
     *
     * @param name
     */
    public ServerTransport_test(String name) {
        super(name);
    }
    
    /**
     * Test the server transport lifecycle. The server transport must be started 
     * to acquire the agent proxy factory. Once stopped, the server transport 
     * can't be started again.
     */
    public void testServerTransportLifecycleEmbeddedServer() throws Exception {
        InetSocketAddress bindAddr = new InetSocketAddress("localhost", 6066);
        
        ServerTransport transport = new ServerTransport(bindAddr, 1);
        
        assertFalse(transport.isReady());
        
        // the server must be started to acquire the agent proxy factory
        try {
            transport.getAgentProxyFactory();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected IllegalStateException instead of: "+e);
        }
        
        transport.start();
        
        assertTrue(transport.isReady());
        
        // should be able to acquire agent proxy factory now
        transport.getAgentProxyFactory();
        
        transport.stop();
        
        // now that the server is stopped, it can't be started again
        assertFalse(transport.isReady());
        
        // the server must be started to acquire the agent proxy factory
        try {
            transport.getAgentProxyFactory();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected IllegalStateException instead of: "+e);
        }
        
        // this shouldn't work
        transport.start();
        
        // now that the server is stopped, it can't be started again
        assertFalse(transport.isReady());
        
        // the server must be started to acquire the agent proxy factory
        try {
            transport.getAgentProxyFactory();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected IllegalStateException instead of: "+e);
        }
    }
    
    /**
     * Test the server transport lifecycle. The server transport must be started 
     * to acquire the agent proxy factory. Once stopped, the server transport 
     * can't be started again.
     */
    public void testServerTransportLifecycleNoEmbeddedServer() throws Exception {        
        ServerTransport transport = new ServerTransport(1);
        
        assertFalse(transport.isReady());
        
        // the server must be started to acquire the agent proxy factory
        try {
            transport.getAgentProxyFactory();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected IllegalStateException instead of: "+e);
        }
        
        transport.start();
        
        assertTrue(transport.isReady());
        
        // should be able to acquire agent proxy factory now
        transport.getAgentProxyFactory();
        
        transport.stop();
        
        // now that the server is stopped, it can't be started again
        assertFalse(transport.isReady());
        
        // the server must be started to acquire the agent proxy factory
        try {
            transport.getAgentProxyFactory();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected IllegalStateException instead of: "+e);
        }
        
        // this shouldn't work
        transport.start();
        
        // now that the server is stopped, it can't be started again
        assertFalse(transport.isReady());
        
        // the server must be started to acquire the agent proxy factory
        try {
            transport.getAgentProxyFactory();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected IllegalStateException instead of: "+e);
        }
    }    
    

}
