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

package org.hyperic.hq.bizapp.server.trigger.conditional;

import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.hyperic.hq.bizapp.server.AbstractMultiConditionTriggerUnittest;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.ext.MockTriggerFireStrategy;
import org.hyperic.hq.events.ext.TriggerFireStrategy;
import org.hyperic.hq.events.server.session.MockEventTrackerEJBImpl;
import org.hyperic.hq.events.server.session.MockEventTrackerLocalHome;
import org.hyperic.hq.events.shared.EventTrackerLocalHome;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.mockejb.jndi.MockContextFactory;

/**
 * Tests the DurationTrigger class.
 */
public class MultiConditionTrigger_test extends AbstractMultiConditionTriggerUnittest {
    
    private static final long ZERO_MINUTES = 0;
    private static final long ONE_MINUTE = 1*60*1000;
    private static final long TWO_MINUTES = 2*60*1000;
    private static final long THREE_MINUTES = 3*60*1000;
    private static final long FOUR_MINUTES =4*60*1000;
    private static final long FIVE_MINUTES =5*60*1000;
    private static final long SIX_MINUTES =6*60*1000;
    private static final long SEVEN_MINUTES =7*60*1000;
    private static final long EIGHT_MINUTES =8*60*1000;
    
    // We have to make this a static variable so it survives 
    // across the unit tests. This necessary b/c we need to 
    // reset the event tracker on the same local home between 
    // test runs since the local home is cached.
    private static MockEventTrackerLocalHome _localHome;
    
    private MockEventTrackerEJBImpl _eventTracker;
    
    private long _currentTime;
    

    /**
     * Creates an instance.
     *
     * @param name
     */
    public MultiConditionTrigger_test(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        
        _eventTracker = new MockEventTrackerEJBImpl();
        _eventTracker.setFailOnVerify();
        
        // set the initial context factory
        MockContextFactory.setAsInitial();
        
        // now register this EJB in the JNDI
        InitialContext context = new InitialContext();
        
        // the local home is cached by the EventTrackerUtil so need 
        // to reset the event tracker EJB on the same local home
        if (_localHome == null) {
            _localHome = new MockEventTrackerLocalHome(_eventTracker);          
        } else {
            _localHome.setEventTracker(_eventTracker);
        }
        
        context.rebind(EventTrackerLocalHome.JNDI_NAME, _localHome);
                
        _currentTime = System.currentTimeMillis();
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
        
        MockContextFactory.revertSetAsInitial();
    }
    
    /**
     * Test this framework -- basic sanity test
     */
    public void testBasicFramework() throws Exception {
        // The event tracker should never be invoked during this test.
        _eventTracker.setExpectNeverInvoked();
        
        Integer tid = new Integer(1);
        
        MultiConditionTrigger trigger = 
            createTrigger(tid, "1", FOUR_MINUTES, false);
        
        verifyExpectations(trigger);
    }
    
    /**
     * Simple sanity test on fulfilling the conditions.  Contract: for an encoding of:
     * 1&2|3&4|5&6
     * the conditions are evaluated left-to-right, with no precedence rule
     * of AND over OR (so it's different from Java precedence rules: this
     * evaluates as ((((1&2)|3)&4)|5)&6.
     * 
     * No need to call verify() on these, because each trigger fire implicitly
     * verifies the firing strategy.
     */
    public void testFulfillingEvents() throws Exception {
    	
    	int triggerID = 1001;
    	
    	// 1&2|3&4|5&6 with events 1,2 ==> shouldn't fire.
		MockMultiConditionTrigger mct =
			createTrigger(new Integer(triggerID++), "1&2|3&4|5&6", 1000000000, false, true);

		AbstractEvent e1 = createEvent(1, true);
		mct.processEvent(e1);
		AbstractEvent e2 = createEvent(2, true);
		mct.processEvent(e2);
    	
    	// 1&2|3&4|5&6 with events 5,6 ==> should fire.
		mct = createTrigger(new Integer(triggerID++), "1&2|3&4|5&6", 1000000000, false, true);
		e1 = createEvent(5, true);
		mct.processEvent(e1);
		e2 = createEvent(6, true);
		// e2 should cause the fire
        setExpectedFires(mct, 1);
		mct.processEvent(e2);
    	
    	// 1&2|3&4|5&6 with events 6,2,4,1 ==> should fire.
		mct = createTrigger(new Integer(triggerID++), "1&2|3&4|5&6", 1000000000, false, true);
		e1 = createEvent(6, true);
		mct.processEvent(e1);
		e2 = createEvent(2, true);
		mct.processEvent(e2);
		AbstractEvent e3 = createEvent(4, true);
		mct.processEvent(e3);
		AbstractEvent e4 = createEvent(1, true);
		// e4 should cause the fire
        setExpectedFires(mct, 1);
		mct.processEvent(e4);
    	
    	// ((((1&2)|3)&4)|5)&6 with events 1 (not fired), 2 (fired),
		// 6 (not fired), 5 (fired) ==> should not fire.
		mct = createTrigger(new Integer(triggerID++), "1&2|3&4|5&6", 1000000000, false, true);
		e1 = createEvent(1, false);
		mct.processEvent(e1);
		e2 = createEvent(2, true);
		mct.processEvent(e2);
		e3 = createEvent(6, false);
		mct.processEvent(e3);
		e4 = createEvent(5, true);
		mct.processEvent(e4);
    	
    	// ((((1&2)&3)&4)&5)|6 with events 6 (fired) ==> should fire.
		mct = createTrigger(new Integer(triggerID++), "1&2&3&4&5|6", 1000000000, false, true);
		e1 = createEvent(6, true);
		mct.processEvent(e1);
    	
    	// ((((1|2)&3)&4)&5)&6 with events 1 (fired) ==> should not fire.
		mct = createTrigger(new Integer(triggerID++), "1|2&3&4&5&6", 1000000000, false, true);
		e1 = createEvent(1, true);
		mct.processEvent(e1);
    	
    	// ((((1|2)&3)&4)&5)&6 with events 2 (fired), 3 (fired), 4 (fired),
		// 5 (fired), 6 (fired) ==> should fire.
		mct = createTrigger(new Integer(triggerID++), "1|2&3&4&5&6", 1000000000, false, true);
		e1 = createEvent(2, true);
		mct.processEvent(e1);
		e2 = createEvent(3, true);
		mct.processEvent(e2);
		e3 = createEvent(4, true);
		mct.processEvent(e3);
		e4 = createEvent(5, true);
		mct.processEvent(e4);
		AbstractEvent e5 = createEvent(6, true);
		mct.processEvent(e5);
    	
    	// Mix in not fired and fired.
		// ((((1&2)|3)&4)|5)&6 with events 1 (not fired), 2 (fired),
		// 6 (not fired), 5 (fired), 4 (not fired), 5 (not fired),
		// 6 (fired), 1 (fired), 5 (fired) ==> should fire.
		mct = createTrigger(new Integer(triggerID++), "1&2|3&4|5&6", 1000000000, false, true);
		e1 = createEvent(1, false);
		mct.processEvent(e1);
		e2 = createEvent(2, true);
		mct.processEvent(e2);
		e3 = createEvent(6, false);
		mct.processEvent(e3);
		e4 = createEvent(5, true);
		mct.processEvent(e4);
		e5 = createEvent(4, false);
		mct.processEvent(e5);
		// This next one should negate e4 above
		AbstractEvent e6 = createEvent(5, false);
		mct.processEvent(e6);
		// This next one should negate e3 above
		AbstractEvent e7 = createEvent(6, true);
		mct.processEvent(e7);
		AbstractEvent e8 = createEvent(1, true);
		mct.processEvent(e8);
		// This next one should negate e6 above, which effectively reinstates e4,
		// causing the trigger to fire
		AbstractEvent e9 = createEvent(5, true);
        setExpectedFires(mct, 1);
		mct.processEvent(e9);
    }
    
    public void testTimeRange() throws Exception {
    	// 1&2 with events 1, 2, but 2 is sent after expiration ==> shouldn't fire.
    	int triggerID = 2001;
    	long expire = 200;
		MockMultiConditionTrigger mct =
			createTrigger(new Integer(triggerID++), "1&2", expire, false, true);

		AbstractEvent e1 = createEvent(1, true);
		mct.processEvent(e1);
		
		long now = System.currentTimeMillis();
		long done = now + expire;
		do {
			try {
				Thread.sleep(expire);
			} catch (InterruptedException ie) {

			}
			now = System.currentTimeMillis();
		} while (now < done);
		
		AbstractEvent e2 = createEvent(2, true);
		mct.processEvent(e2);
    }
    
    private void verifyExpectations(MultiConditionTrigger trigger) {
        _eventTracker.verify();
        
        TriggerFireStrategy strategy = trigger.getTriggerFireStrategy();
        
        if (strategy != null && strategy instanceof MockTriggerFireStrategy) {
            ((MockTriggerFireStrategy)strategy).verify();
        } else {
            throw new IllegalStateException("the trigger fire strategy was not set");
        }        
    }
}
