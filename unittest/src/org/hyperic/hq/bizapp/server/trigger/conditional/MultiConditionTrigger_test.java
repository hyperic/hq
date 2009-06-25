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

import org.hyperic.hq.bizapp.server.AbstractMultiConditionTriggerUnittest;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ext.MockTriggerFireStrategy;
import org.hyperic.hq.events.ext.TriggerFireStrategy;
import org.hyperic.hq.events.server.session.MockEventTrackerEJBImpl;
import org.hyperic.hq.events.server.session.MockEventTrackerLocalHome;
import org.hyperic.hq.events.shared.EventTrackerLocalHome;
import org.hyperic.hq.events.shared.EventTrackerUtil;
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
        
        //Below is only effective if EventTrackerUtil.getLocalHome() has never been called - else JNDI lookup won't occur again
        context.rebind(EventTrackerLocalHome.JNDI_NAME, new MockEventTrackerLocalHome(_eventTracker));
       
        //Reset the existing MockLocalHome with a new Mock EJB
        ((MockEventTrackerLocalHome)EventTrackerUtil.getLocalHome()).setEventTracker(_eventTracker);
                
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
    
    public void testOrConditionWithSingleEvent() throws Exception {
		MockMultiConditionTrigger mct =
			createTrigger(new Integer(2100), "1|2", 100000000, false, true);
		AbstractEvent e1 = createEvent(1, true);
		mct.processEvent(e1);
		assertEquals(1, mct.getFireCount());
		
		AbstractEvent e2 = createEvent(2, true);
		mct.processEvent(e2);
		assertEquals(2, mct.getFireCount());
    }
    
    public void testNotFiredPublishing() throws Exception {
    	// notFired() publishing should only happen when a MultiConditionTrigger has
    	// sub-conditions that had explicit notFired conditions, as opposed to not
    	// having any conditions measured at all.  For example:
    	//
    	// 1&2, event 1 (fired) is seen ==> don't publish
    	// 1&2, event 1 (not fired) is seen ==> publish
		MockMultiConditionTrigger mct =
			createTrigger(new Integer(2200), "1&2", 100000000, false, true);
		AbstractEvent e1 = createEvent(1, true);
		mct.processEvent(e1);
		assertEquals(0, mct.getNotFiredCount());
		
		AbstractEvent e2 = createEvent(2, false);
		mct.processEvent(e2);
		assertEquals(1, mct.getNotFiredCount());
    }
    
    public void testOrConditionAndNotFiredPublishing() throws Exception {
    	MockMultiConditionTrigger trigger = 
			createTrigger(new Integer(2201), "1|2|3|4|5", 100000000, false, true);
    	AbstractEvent notFired = createEvent(1, false);
    	AbstractEvent fired = createEvent(2, true);
    	trigger.processEvent(notFired);
    	assertEquals(1, trigger.getNotFiredCount());
    	assertEquals(0, trigger.getFireCount());
    	trigger.processEvent(fired);
    	assertEquals(1, trigger.getNotFiredCount());
    	assertEquals(1, trigger.getFireCount());
    }
    
    public void testHighlyConcurrentFiredAndNotFiredCount() throws Exception {
    	int nThreads = 20;
    	int iterations = 20;
    	int[] eventIds = new int[] { 1, 2, 3, 4, 5 };
    	MockMultiConditionTrigger trigger = 
			createTrigger(new Integer(2300), "1|2|3|4|5", 100000000, false, true);
    	
    	// Tee them up
    	EventBlaster[] blasters = new EventBlaster[nThreads];
    	int[] started = new int[1];
    	started[0] = 0;
    	for (int i = 0; i < nThreads; ++i) {
    		blasters[i] = new EventBlaster(i, nThreads, started, iterations, eventIds, trigger);
    		blasters[i].start();
    	}

    	// Blast away...
    	boolean bail = false;

    	// Assess the damage
    	for (int i = 0; i < nThreads; ++i) {
    		blasters[i].join();
    	}

    	assertEquals(nThreads * iterations, trigger.getNotFiredCount());
    	assertEquals(nThreads * iterations, trigger.getFireCount());
    }
    
    public void testThatAddEventsDontAccumulate() throws Exception {
    	
    	// Non-expiring...
    	Integer tid1 = new Integer(2301);
    	MockMultiConditionTrigger t1 = 
			createTrigger(tid1, "1&2", 0, false, true);
    	AbstractEvent e1 = createEvent(1, true);
    	t1.processEvent(e1);
    	assertEquals(1, _eventTracker.getEventsCount(tid1));
    	AbstractEvent e2 = createEvent(1, true);
    	t1.processEvent(e2);
    	assertEquals(1, _eventTracker.getEventsCount(tid1));
    	
    	// Expiring...
    	Integer tid2 = new Integer(2302);
    	MockMultiConditionTrigger t2 = 
			createTrigger(tid2, "1&2&3", 1000000000, false, true);
    	AbstractEvent e3 = createEvent(1, true);
    	t2.processEvent(e3);
    	assertEquals(1, _eventTracker.getEventsCount(tid2));
    	AbstractEvent e4 = createEvent(1, true);
    	t2.processEvent(e4);
    	assertEquals(1, _eventTracker.getEventsCount(tid2));
    	AbstractEvent e5 = createEvent(2, true);
    	t2.processEvent(e5);
    	assertEquals(2, _eventTracker.getEventsCount(tid2));
    	AbstractEvent e6 = createEvent(2, false);
    	t2.processEvent(e6);
    	assertEquals(2, _eventTracker.getEventsCount(tid2));
    	AbstractEvent e7 = createEvent(2, false);
    	t2.processEvent(e7);
    	assertEquals(2, _eventTracker.getEventsCount(tid2));
    }
    
    public void testStateTrackingForNonExpiring() throws Exception {
    	
    	// Sub-condition fired, then not fired
    	Integer tid1 = new Integer(2302);
    	MockMultiConditionTrigger t1 = 
			createTrigger(tid1, "1&2&3&4", 0, false, true);
    	AbstractEvent e1 = createEvent(1, true);
    	t1.processEvent(e1);
    	AbstractEvent e2 = createEvent(1, false);
    	t1.processEvent(e2);
    	assertEquals(0, t1.getCurrentFulfillingEventsCount());
    	assertEquals(0, _eventTracker.getEventsCount(tid1));
    	
    	// All sub-conditions fire, trigger fires
    	Integer tid2 = new Integer(2303);
    	MockMultiConditionTrigger t2 = 
			createTrigger(tid2, "1&2&3&4", 0, false, true);
    	AbstractEvent e3 = createEvent(1, true);
    	t2.processEvent(e3);
    	AbstractEvent e4 = createEvent(2, true);
    	t2.processEvent(e4);
    	AbstractEvent e5 = createEvent(3, true);
    	t2.processEvent(e5);
    	AbstractEvent e6 = createEvent(4, true);
    	t2.processEvent(e6);
    	// Fired, state should be reset
    	assertEquals(0, t2.getCurrentFulfillingEventsCount());
    	assertEquals(0, _eventTracker.getEventsCount(tid2));
    	assertEquals(1, t2.getFireCount());
    	
    	// Many updated firings, no accumulation
    	Integer tid3 = new Integer(2304);
    	MockMultiConditionTrigger t3 = 
			createTrigger(tid3, "1&2&3&4", 0, false, true);
    	AbstractEvent e7 = createEvent(1, true);
    	t3.processEvent(e7);
    	AbstractEvent e8 = createEvent(2, true);
    	t3.processEvent(e8);
    	AbstractEvent e9 = createEvent(3, true);
    	t3.processEvent(e9);
    	AbstractEvent e10 = createEvent(1, true);
    	t3.processEvent(e10);
    	AbstractEvent e11 = createEvent(2, true);
    	t3.processEvent(e11);
    	AbstractEvent e12 = createEvent(3, true);
    	t3.processEvent(e12);
    	AbstractEvent e13 = createEvent(1, true);
    	t3.processEvent(e13);
    	AbstractEvent e14 = createEvent(2, true);
    	t3.processEvent(e14);
    	AbstractEvent e15 = createEvent(3, true);
    	t3.processEvent(e15);
    	AbstractEvent e16 = createEvent(1, true);
    	t3.processEvent(e16);
    	AbstractEvent e17 = createEvent(2, true);
    	t3.processEvent(e17);
    	AbstractEvent e18 = createEvent(3, true);
    	t3.processEvent(e18);
    	AbstractEvent e19 = createEvent(1, true);
    	t3.processEvent(e19);
    	AbstractEvent e20 = createEvent(2, true);
    	t3.processEvent(e20);
    	AbstractEvent e21 = createEvent(3, true);
    	t3.processEvent(e21);
    	AbstractEvent e22 = createEvent(1, true);
    	t3.processEvent(e22);
    	AbstractEvent e23 = createEvent(2, true);
    	t3.processEvent(e23);
    	AbstractEvent e24 = createEvent(3, true);
    	t3.processEvent(e24);
    	assertEquals(3, t3.getCurrentFulfillingEventsCount());
    	assertEquals(3, _eventTracker.getEventsCount(tid3));
    }
    
    public void testStateTrackingForExpiring() throws Exception {
    	Integer tid1 = new Integer(2305);
    	long expiration1 = 200;
    	MockMultiConditionTrigger t1 = 
			createTrigger(tid1, "1&2&3&4", expiration1, false, true);
    	AbstractEvent e1 = createEvent(1, true);
    	t1.processEvent(e1);
    	
    	// Let that first event expire
    	pause(expiration1);
    	
    	// Expiration will not be noticed until the next event is processed
    	AbstractEvent e2 = createEvent(2, true);
    	t1.processEvent(e2);
    	assertEquals(1, t1.getCurrentFulfillingEventsCount());
    	assertEquals(1, _eventTracker.getEventsCount(tid1));
    	
    	// Loop on events, always waiting just long enough
    	Integer tid2 = new Integer(2306);
    	long expiration2 = 50;
    	MockMultiConditionTrigger t2 = 
			createTrigger(tid2, "1&2&3&4", expiration2, false, true);
    	for (int i = 0; i < 50; ++i) {
    		// Event 1
    		AbstractEvent e = createEvent(1, true);
    		t2.processEvent(e);
        	assertEquals("failure at loop index " + i, 1, t2.getCurrentFulfillingEventsCount());
        	assertEquals("failure at loop index " + i, 1, _eventTracker.getEventsCount(tid2));
    		pause(expiration2);
    		
    		// Event 2
    		e = createEvent(2, true);
    		t2.processEvent(e);
        	assertEquals("failure at loop index " + i, 1, t2.getCurrentFulfillingEventsCount());
        	assertEquals("failure at loop index " + i, 1, _eventTracker.getEventsCount(tid2));
        	pause(expiration2);
        	
        	// Event 3
    		e = createEvent(3, true);
    		t2.processEvent(e);
        	assertEquals("failure at loop index " + i, 1, t2.getCurrentFulfillingEventsCount());
        	assertEquals("failure at loop index " + i, 1, _eventTracker.getEventsCount(tid2));
        	pause(expiration2);
        	
        	// Event 4
    		e = createEvent(4, true);
    		t2.processEvent(e);
        	assertEquals("failure at loop index " + i, 1, t2.getCurrentFulfillingEventsCount());
        	assertEquals("failure at loop index " + i, 1, _eventTracker.getEventsCount(tid2));
        	pause(expiration2);
    	}
    }
    
    private void pause(long time) {
    	try {
    		Thread.sleep(time + 1);
    	} catch (InterruptedException ie) {
    		
    	}
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
    
    private class EventBlaster extends Thread {
    	
		private int total;
    	private int[] gate;
		private int count;
		private int[] eventIds;
		private MultiConditionTrigger trigger;

		EventBlaster(int id, int total, int[] gate, int count, int[] eventIds, MultiConditionTrigger trigger) {
			super("EventBlaster " + id);
			this.total = total;
    		this.gate = gate;
    		this.count = count;
    		this.eventIds = eventIds;
    		this.trigger = trigger;
    	}
    	
    	public void run() {
    		
    		boolean bail = false;
    		synchronized (gate) {
    			if (++gate[0] == total) {
    				gate.notifyAll();
    			} else {
    				try {
    					gate.wait();
    				} catch (InterruptedException ie) {
    					bail = true;
    				}
    			}
    		}
    		
    		if (!bail) {
    			
    			for (int i = 0; i < count; ++i) {
    				int index = i % eventIds.length + 1;
    				AbstractEvent evtFired = createEvent(index, true);
    				AbstractEvent evtNotFired = createEvent(index, false);

    				try {
    					trigger.processEvent(evtFired);
    					yield();
    					trigger.processEvent(evtNotFired);
    					yield();
    				} catch (Exception e) {
    					// shouldn't happen
    					e.printStackTrace();
    				}
    			}
    		}
    	}
    }
}
