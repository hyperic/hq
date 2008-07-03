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

package org.hyperic.hq.transport.util;

import junit.framework.TestCase;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Tests the HQInvokerLocator class.
 */
public class HQInvokerLocator_test extends TestCase {
    
    private HQInvokerLocator _locator;
    
    
    /**
     * Creates an instance.
     *
     * @param name
     */
    public HQInvokerLocator_test(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        _locator = 
            new HQInvokerLocator("protocol", "localhost", 8080, null, null, "token");
    }
    
    /**
     * Expect a NullPointerException here.
     */
    public void testNullAgentToken() throws Exception {
        try {
            new HQInvokerLocator("protocol", "localhost", 8080, null, null, null);
            fail("Expected NPE");
        } catch (NullPointerException e) {
            // expected outcome
        }
        
        try {
            _locator.cloneWithNewAgentToken(null);
            fail("Expected NPE");
        } catch (NullPointerException e) {
            // expected outcome
        }
    }
    
    /**
     * Expect an IllegalArgumentException here.
     */
    public void testIllegalAgentTokenName() throws Exception {
        try {
            new HQInvokerLocator("protocol", "localhost", 8080, null, null, HQInvokerLocator.UNKNOWN_AGENT_TOKEN);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected outcome
        }
        
        try {
            _locator.cloneWithNewAgentToken(HQInvokerLocator.UNKNOWN_AGENT_TOKEN);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected outcome
        }
    }
    
    public void testAgentTokenUnknownAtLocatorCreation() {
        HQInvokerLocator locator = new HQInvokerLocator("protocol", "localhost", 8080, null, null);
        
        assertFalse(locator.isAgentTokenKnown());
        assertEquals(HQInvokerLocator.UNKNOWN_AGENT_TOKEN, locator.getAgentToken());
    }
    
    public void testAgentTokenKnownAtLocatorCreation() {
        String agentToken = "newToken";
        HQInvokerLocator locator = _locator.cloneWithNewAgentToken(agentToken);
        
        assertTrue(locator.isAgentTokenKnown());
        assertEquals(agentToken, locator.getAgentToken());
    }

        
    public void testGetAgentToken() {
        assertEquals("token", _locator.getAgentToken());
    }
    
    /**
     * Expect a NPE.
     */
    public void testSetNullMessageDeliveryOptions() throws Exception {
        try {
            _locator.setMessageDeliveryOptions(null);
            fail("Expected a NullPointerException");
        } catch (NullPointerException e) {
            // expected outcome
        }
        
    }
    
    /**
     * Run the test in a new thread to isolate it (since we are storing message
     * delivery options in a thread local).
     */
    public void testGetDefaultMessageDeliveryOptions() throws Exception {
        IsolatedTestRunner runner = new IsolatedTestRunner();
        runner.start();
        
        assertEquals(MessageDeliveryOptions.newSynchronousInstance(), 
                     runner.getMessageDeliveryOptions());
    }
    
    public void testSetMessageDeliveryOptions() {
        MessageDeliveryOptions options = MessageDeliveryOptions.newAsynchronousInstance(true);
        
        _locator.setMessageDeliveryOptions(options);
        
        assertEquals(options, _locator.getMessageDeliveryOptions());
    }
            
    /**
     * Test equals where the agent tokens are the same and different.
     */
    public void testEquals() {
        HQInvokerLocator equalLocator = _locator.cloneWithNewAgentToken(_locator.getAgentToken());
        
        HQInvokerLocator notEqualLocator = _locator.cloneWithNewAgentToken(_locator.getAgentToken()+"_not_equal");
        
        assertTrue(_locator.equals(_locator));
        assertTrue(_locator.equals(equalLocator));
        assertEquals(_locator.hashCode(), equalLocator.hashCode());
        
        assertFalse(_locator.equals(notEqualLocator));
    }
    
    private class IsolatedTestRunner extends Thread {
        
        private final CountDownLatch _latch;
        
        private volatile MessageDeliveryOptions _options;
        
        public IsolatedTestRunner() {
            setDaemon(true);
            _latch = new CountDownLatch(1);
        }
        
        public void run() {
            _options = _locator.getMessageDeliveryOptions();
        }
        
        public MessageDeliveryOptions getMessageDeliveryOptions() throws InterruptedException {
            boolean timedout = !_latch.await(60, TimeUnit.SECONDS);
            
            if (timedout) {
                fail("timed out waiting for message delivery options");
            }
            
            return _options;
        }
    }

}
