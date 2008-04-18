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
 * Tests the AgentTransport class. The unidirectional transport is not available 
 * in .ORG.
 */
public class AgentTransport_test extends TestCase {

    private AgentTransport _agentTransport;
    
    /**
     * Creates an instance.
     *
     * @param name
     */
    public AgentTransport_test(String name) {
        super(name);
    }
    
    /**
     * This is the .ORG instance so we expect a ClassNotFoundException.
     */
    public void testUseUnidirectionalTransport() throws Exception {
        try {
            InetSocketAddress addr = new InetSocketAddress("localhost", 6066);
            new AgentTransport(addr, null, false, "token", true, 10, 2);
            fail("Expected ClassNotFoundException.");
        } catch (ClassNotFoundException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expected ClassNotFoundException instead of: "+e);
        }
    }
    
}
