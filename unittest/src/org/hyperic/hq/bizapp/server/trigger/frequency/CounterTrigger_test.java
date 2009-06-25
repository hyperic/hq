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

package org.hyperic.hq.bizapp.server.trigger.frequency;

import java.util.Date;

import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.HeartBeatEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.ext.MockTriggerFireStrategy;
import org.hyperic.hq.events.ext.TriggerFireStrategy;
import org.hyperic.hq.events.server.session.MockEventTrackerEJBImpl;
import org.hyperic.hq.events.server.session.MockEventTrackerLocalHome;
import org.hyperic.hq.events.shared.EventTrackerLocalHome;
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.timer.UserSpecifiedTimeClock;
import org.mockejb.jndi.MockContextFactory;

/**
 * Tests the DurationTrigger class.
 */
public class CounterTrigger_test extends TestCase {
    
    private MockEventTrackerEJBImpl _eventTracker;
    
    private long _currentTime;
    

    /**
     * Creates an instance.
     *
     * @param name
     */
    public CounterTrigger_test(String name) {
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
    
    public void testFiresOnFiredEvents() throws Exception {

        long timeRange = 200;
        
        Integer tid = new Integer(2);
        CounterTrigger trigger = 
            createTrigger(tid, 3, timeRange, true);
        TriggerFiredEvent e1 = createTfe(tid);
        trigger.processEvent(e1);
        TriggerFiredEvent e2 = createTfe(tid);
        trigger.processEvent(e2);
        TriggerFiredEvent e3 = createTfe(tid);
        trigger.processEvent(e3);
        
        verifyExpectations(trigger);
    }
    
    public void testDoesNotFireOnFiredEventsNotInTimeWindow() throws Exception {

        long timeRange = 300;
        long interval = 151;
        
    	Integer tid = new Integer(3);
        CounterTrigger trigger = 
            createTrigger(tid, 3, timeRange, false);
        TriggerFiredEvent e1 = createTfe(tid);
        
        // Must manually manage the time on the mock event tracker
        long now = System.currentTimeMillis();
        _eventTracker.setCurrentTimeMillis(now);
        trigger.processEvent(e1);
        pause(interval);
        
        TriggerFiredEvent e2 = createTfe(tid);
        now = System.currentTimeMillis();
        _eventTracker.setCurrentTimeMillis(now);
        trigger.processEvent(e2);
        pause(interval);
        
        TriggerFiredEvent e3 = createTfe(tid);
        now = System.currentTimeMillis();
        _eventTracker.setCurrentTimeMillis(now);
        trigger.processEvent(e3);
        
        verifyExpectations(trigger);
    	
    }
    
    public void testPurge() throws Exception {
        long timeRange = 100;
        int eventsNeededToFire = 50;
        
    	Integer tid = new Integer(4);
        CounterTrigger trigger = 
            createTrigger(tid, eventsNeededToFire, timeRange, false);
        
        // Must manually manage the time on the mock event tracker
        _eventTracker.setCurrentTimeMillis(System.currentTimeMillis());
        
        // Send one fewer event than what's needed to fire
        for (int i = 1; i < eventsNeededToFire; ++i) {
            TriggerFiredEvent e = createTfe(tid);
            trigger.processEvent(e);
        }
        
        // Should not have fired yet
        MockTriggerFireStrategy strategy =
        	(MockTriggerFireStrategy) trigger.getTriggerFireStrategy();
        strategy.verify();
        
        pause(timeRange + 1);
        _eventTracker.setCurrentTimeMillis(System.currentTimeMillis());

        // Again...
        for (int i = 1; i < eventsNeededToFire; ++i) {
            TriggerFiredEvent e = createTfe(tid);
            trigger.processEvent(e);
        }
        
        strategy.verify();
        pause(timeRange + 1);
        _eventTracker.setCurrentTimeMillis(System.currentTimeMillis());
        
        // ...and again...
        for (int i = 1; i < eventsNeededToFire; ++i) {
            TriggerFiredEvent e = createTfe(tid);
            trigger.processEvent(e);
        }
        
        strategy.verify();
        pause(timeRange + 1);
        _eventTracker.setCurrentTimeMillis(System.currentTimeMillis());
        
        // Now make it fire
        strategy.setExpectedNumTimesActionsFired(1);
        for (int i = 0; i < eventsNeededToFire; ++i) {
            TriggerFiredEvent e = createTfe(tid);
            trigger.processEvent(e);
        }
        
        strategy.verify();
        
        // Check that the expired events are sufficiently purged
        int maxExpired = CounterTrigger.PURGE_THRESHOLD_INCREMENT + eventsNeededToFire;
        assertTrue(_eventTracker.getReferencedEventStreams(tid).size() < maxExpired);
    }
    
    private void pause(long time) {
    	try {
    		Thread.sleep(time);
    	} catch (InterruptedException ie) {
    		
    	}
    }
         
    private CounterTrigger createTrigger(Integer triggerId, 
                                         int count, 
                                         long timeRange,
                                         boolean expectFire) {
    	CounterTrigger trigger = new CounterTrigger(triggerId, count, timeRange);
        
        // We don't want to actually fire actions since that will tie 
        // our unit tests to the database and other subsystems.
        MockTriggerFireStrategy strategy = new MockTriggerFireStrategy();
        
        strategy.setExpectedNumTimesActionsFired((expectFire)?1:0);
        
        trigger.setTriggerFireStrategy(strategy);
        
        return trigger;
    }
    
    private TriggerFiredEvent createTfe(Integer triggerId) {
        Integer mid = new Integer(100);
        // Metric value is pretty irrelevant
        MetricValue value = new MetricValue(1, 1);
        AbstractEvent rootEvent = new MeasurementEvent(mid, value);
        AbstractEvent[] events = {rootEvent};        
        TriggerFiredEvent event = new TriggerFiredEvent(triggerId, events);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
    
    private void verifyExpectations(CounterTrigger trigger) {
        _eventTracker.verify();
        
        TriggerFireStrategy strategy = trigger.getTriggerFireStrategy();
        
        if (strategy != null && strategy instanceof MockTriggerFireStrategy) {
            ((MockTriggerFireStrategy)strategy).verify();
        } else {
            throw new IllegalStateException("the trigger fire strategy was not set");
        }        
    }
}
