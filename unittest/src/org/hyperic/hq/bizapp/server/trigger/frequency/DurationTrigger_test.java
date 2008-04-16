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
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.timer.Clock;
import org.hyperic.util.timer.UserSpecifiedTimeClock;
import org.mockejb.jndi.MockContextFactory;

/**
 * Tests the DurationTrigger class.
 */
public class DurationTrigger_test extends TestCase {
    
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
    public DurationTrigger_test(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        
        _eventTracker = new MockEventTrackerEJBImpl();
        
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
     * Test the case where the trigger has not seen anything yet worth processing. 
     * The trigger will ignore all events until it sees something that may 
     * cause it to fire in the future. The estimated collection interval will 
     * be estimated by any TriggerNotFiredEvent processed.
     */
    public void testIgnoreUninterestingEvents() throws Exception {
        // The event tracker should never be invoked during this test.
        _eventTracker.setExpectNeverInvoked();
        
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, TWO_MINUTES, FOUR_MINUTES, clock, false);
        
        assertTrue(trigger.isInitial());
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                    trigger.getEstimatedCollectionInterval());
        
        // we should never fire since TriggerNotFiredEvents or HeartBeatEvents
        // before a TriggerFiredEvent are not interesting
        long tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event1 = createTnfe(tid, tick); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isInitial());
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                    trigger.getEstimatedCollectionInterval());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event2 = createTnfe(tid, tick); 
        trigger.processEvent(event2);

        assertTrue(trigger.isInitial());
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                    trigger.getEstimatedCollectionInterval());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        HeartBeatEvent event3 = createHeartBeatEvent(tick);
        trigger.processEvent(event3);
        
        assertTrue(trigger.isInitial());
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                    trigger.getEstimatedCollectionInterval());
        
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event4 = createTnfe(tid, tick); 
        trigger.processEvent(event4);
        
        assertTrue(trigger.isInitial());
        
        // we don't use HeartBeatEvents to estimate the collection interval
        assertEquals(TWO_MINUTES, trigger.getEstimatedCollectionInterval());
        
        verifyExpectations(trigger);
    }
    
    /**
     * Test the case where the trigger processes TriggerFiredEvents until it 
     * fires.
     */
    public void testConstantTriggerFiredEvents() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, TWO_MINUTES, FOUR_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // now add some TriggerFiredEvents until we fire
        long tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue3 = 3;
        TriggerFiredEvent event3 = createTfe(tid, metricValue3, tick, ONE_MINUTE); 
        trigger.processEvent(event3);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event3.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());
    }
    
    /**
     * Test the case where the trigger processes TriggerFiredEvents but fires 
     * because the last event processed is a HeartBeatEvent.
     */
    public void testConstantTriggerFiredEventsWithHeartBeat() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, TWO_MINUTES, FOUR_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // now add some TriggerFiredEvents until we fire
        long tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        HeartBeatEvent event3 = createHeartBeatEvent(tick); 
        trigger.processEvent(event3);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event2.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());        
    }
    
    /**
     * Test the case where the collection interval for trackable events changes.
     * As a by product of this test, we are also checking that the trigger 
     * will fire when the count equals the time range.
     */
    public void testEstimatingCollectionInterval() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
        
        // note that the count equals the time range
        DurationTrigger trigger = 
            createTrigger(tid, FOUR_MINUTES, FOUR_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
                
        // now add some TriggerFiredEvents until we fire
        long tick = _currentTime-SIX_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, TWO_MINUTES); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        // we don't have enough events yet to estimate the collection interval
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                    trigger.getEstimatedCollectionInterval());
        
        tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, TWO_MINUTES); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        // The collection interval is calculated using the timestamps for 
        // 2 consecutive events of the same type
        assertEquals(TWO_MINUTES, event2.getTimestamp()-event1.getTimestamp());
        assertEquals(TWO_MINUTES, trigger.getEstimatedCollectionInterval());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue3 = 3;
        TriggerFiredEvent event3 = createTfe(tid, metricValue3, tick, TWO_MINUTES); 
        trigger.processEvent(event3);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        assertEquals(TWO_MINUTES, trigger.getEstimatedCollectionInterval());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event3.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());        
    }
    
    /**
     * Test the case where events are processed out of order. Only a later 
     * event should be considered.
     */
    public void testReceivingEventsOutOfOrder() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, THREE_MINUTES, FOUR_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                    trigger.getEstimatedCollectionInterval());
        
        // try some not interesting events out of order
        long tick = _currentTime-FIVE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event1 = createTnfe(tid, tick); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isInitial());
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                    trigger.getEstimatedCollectionInterval());
        
        tick = _currentTime-SIX_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event2 = createTnfe(tid, tick); 
        trigger.processEvent(event2);

        assertTrue(trigger.isInitial());
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                    trigger.getEstimatedCollectionInterval());
        
        // now some interesting events
        tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue3 = 3;
        TriggerFiredEvent event3 = createTfe(tid, metricValue3, tick, ONE_MINUTE); 
        trigger.processEvent(event3);

        assertTrue(trigger.isTracking());
        assertEquals(DurationTrigger.MIN_COLLECTION_INTERVAL_MILLIS, 
                trigger.getEstimatedCollectionInterval());
            
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue4 = 4;
        TriggerFiredEvent event4 = createTfe(tid, metricValue4, tick, ONE_MINUTE); 
        trigger.processEvent(event4);

        assertTrue(trigger.isTracking());
        assertEquals(TWO_MINUTES, trigger.getEstimatedCollectionInterval());      
        
        // and one out of order - this one should be ignored
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue5 = 5;
        TriggerFiredEvent event5 = createTfe(tid, metricValue5, tick, ONE_MINUTE); 
        trigger.processEvent(event5);

        assertTrue(trigger.isTracking());
        assertEquals(TWO_MINUTES, trigger.getEstimatedCollectionInterval());
        
        // now the firing event
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue6 = 6;
        TriggerFiredEvent event6 = createTfe(tid, metricValue6, tick, ONE_MINUTE); 
        trigger.processEvent(event6);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event6.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());
        
    }
    
    /**
     * Test the case where the trigger processes TriggerFiredEvents and 
     * TriggerNotFiredEvents until it fires.
     */
    public void testFiresBothTriggerAndTriggerNotFiredEvents() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, THREE_MINUTES, SIX_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // two TriggerFiredEvents
        long tick = _currentTime-SIX_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-FIVE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        // three TriggerNotFiredEvents
        tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event3 = createTnfe(tid, tick); 
        trigger.processEvent(event3);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event4 = createTnfe(tid, tick); 
        trigger.processEvent(event4);
        
        assertTrue(trigger.isTracking());

        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event5 = createTnfe(tid, tick); 
        trigger.processEvent(event5);
        
        assertTrue(trigger.isTracking());
        
        // now TriggerFiredEvents until firing
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue6 = 6;
        TriggerFiredEvent event6 = createTfe(tid, metricValue6, tick, ONE_MINUTE); 
        trigger.processEvent(event6);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue7 = 7;
        TriggerFiredEvent event7 = createTfe(tid, metricValue7, tick, ONE_MINUTE); 
        trigger.processEvent(event7);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event7.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());
    }
    
    /**
     * Test the case where the trigger processes TriggerFiredEvents and 
     * TriggerNotFiredEvents until it fires, where the triggering event is 
     * a TriggerNotFiredEvent.
     */
    public void testFiresBothTriggerAndTriggerNotFiredEventsWithLastTnfe() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, THREE_MINUTES, SIX_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // two TriggerFiredEvents
        long tick = _currentTime-SIX_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-FIVE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        // three TriggerNotFiredEvents
        tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event3 = createTnfe(tid, tick); 
        trigger.processEvent(event3);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event4 = createTnfe(tid, tick); 
        trigger.processEvent(event4);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event5 = createTnfe(tid, tick);
        trigger.processEvent(event5);

        assertTrue(trigger.isTracking());

        // now a TriggerFiredEvent
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue6 = 6;
        TriggerFiredEvent event6 = createTfe(tid, metricValue6, tick, ONE_MINUTE); 
        trigger.processEvent(event6);

        assertTrue(trigger.isTracking());
        
        // and this TriggerNotFiredEvent should cause the trigger to fire
        tick = _currentTime;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event7 = createTnfe(tid, tick); 
        trigger.processEvent(event7);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event6.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());
    }
                
    /**
     * Test the case where the trigger processes TriggerFiredEvents and 
     * TriggerNotFiredEvents until it fires from a HeartBeatEvent.
     */
    public void testFiresBothTriggerAndTriggerNotFiredEventsWithHeartBeat() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, THREE_MINUTES, SIX_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // two TriggerFiredEvents
        long tick = _currentTime-SIX_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-FIVE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        // three TriggerNotFiredEvents
        tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event3 = createTnfe(tid, tick); 
        trigger.processEvent(event3);
        
        assertTrue(trigger.isTracking());        
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event4 = createTnfe(tid, tick); 
        trigger.processEvent(event4);
        
        assertTrue(trigger.isTracking());        
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event5 = createTnfe(tid, tick); 
        trigger.processEvent(event5);

        assertTrue(trigger.isTracking());

        // a TriggerFiredEvent 
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue6 = 6;
        TriggerFiredEvent event6 = createTfe(tid, metricValue6, tick, ONE_MINUTE); 
        trigger.processEvent(event6);

        assertTrue(trigger.isTracking());
        
        // and the last HeartBeatEvent
        tick = _currentTime;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        HeartBeatEvent event7 = createHeartBeatEvent(tick); 
        trigger.processEvent(event7);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event6.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());
    }    
    
    /**
     * Test the case where the trigger processes TriggerFiredEvents and 
     * TriggerNotFiredEvents but does not fire since the count was exceeded
     * outside the given time range. Note that we consider an extra collection 
     * interval as part of the count to account for possible skew between the 
     * agent and server time.
     */
    public void testNotFiresBecauseCountExceededOutsideTimeRange() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, THREE_MINUTES, FOUR_MINUTES, clock, false);
        
        assertTrue(trigger.isInitial());
        
        // two TriggerFiredEvents
        long tick = _currentTime-SIX_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-FIVE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        // three TriggerNotFiredEvents
        tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event3 = createTnfe(tid, tick); 
        trigger.processEvent(event3);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event4 = createTnfe(tid, tick); 
        trigger.processEvent(event4);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event5 = createTnfe(tid, tick);  
        trigger.processEvent(event5);

        assertTrue(trigger.isTracking());

        // now TriggerFiredEvents
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue6 = 6;
        TriggerFiredEvent event6 = createTfe(tid, metricValue6, tick, ONE_MINUTE); 
        trigger.processEvent(event6);

        assertTrue(trigger.isTracking());
        
        tick = _currentTime;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue7 = 7;
        TriggerFiredEvent event7 = createTfe(tid, metricValue7, tick, ONE_MINUTE); 
        trigger.processEvent(event7);
        
        // we shouldn't have fired
        verifyExpectations(trigger);
        
        assertTrue(trigger.isTracking());
    }    
    
    /**
     * Test the case where the trigger processes TriggerFiredEvents and 
     * TriggerNotFiredEvents but does not fire since the count was not 
     * exceeded in the given time range.
     */
    public void testNotFiresBecauseCountNotExceededInTimeRange() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, THREE_MINUTES, SIX_MINUTES, clock, false);
        
        assertTrue(trigger.isInitial());
        
        // two TriggerFiredEvents
        long tick = _currentTime-SIX_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-FIVE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        // three TriggerNotFiredEvents
        tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event3 = createTnfe(tid, tick); 
        trigger.processEvent(event3);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event4 = createTnfe(tid, tick); 
        trigger.processEvent(event4);
        
        assertTrue(trigger.isTracking());

        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event5 = createTnfe(tid, tick);
        trigger.processEvent(event5);

        assertTrue(trigger.isTracking());

        // now a TriggerFiredEvent
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue6 = 6;
        TriggerFiredEvent event6 = createTfe(tid, metricValue6, tick, ONE_MINUTE); 
        trigger.processEvent(event6);
        
        // we shouldn't have fired
        verifyExpectations(trigger);
        
        assertTrue(trigger.isTracking());
    }
    
    /**
     * Test the case where the trigger processes one TriggerFiredEvent, then 
     * waits more than two times the time range. At this point, the trigger 
     * should be reset back to the initial state so it doesn't query the event 
     * tracker. Then when the trigger sees another TriggerFiredEvent it should 
     * start processing events until the trigger fires. 
     */
    public void testResetTriggerStateNoTriggerFiredEventSeenInTimeRange() throws Exception {
        Integer tid = new Integer(1);
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, TWO_MINUTES, TWO_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // one TriggerFiredEvents
        long tick = _currentTime-EIGHT_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        // now 4 minutes of TriggerNotFiredEvents/HeartBeatEvents
        tick = _currentTime-SEVEN_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event2 = createTnfe(tid, tick); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());

        tick = _currentTime-SIX_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event3 = createTnfe(tid, tick); 
        trigger.processEvent(event3);

        assertTrue(trigger.isTracking());

        tick = _currentTime-FIVE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event4 = createTnfe(tid, tick); 
        trigger.processEvent(event4);

        assertTrue(trigger.isTracking());

        tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event5 = createTnfe(tid, tick); 
        trigger.processEvent(event5);

        assertTrue(trigger.isTracking());        
        
        tick = _currentTime-FOUR_MINUTES-30000;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        HeartBeatEvent event6 = createHeartBeatEvent(tick); 
        trigger.processEvent(event6);

        assertTrue(trigger.isTracking());        
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        TriggerNotFiredEvent event7 = createTnfe(tid, tick); 
        trigger.processEvent(event7);
        
        assertEquals(ONE_MINUTE, trigger.getEstimatedCollectionInterval());

        // the trigger should have been reset back to the initial state 
        // because we haven't seen a TriggerFiredEvent in 4 minutes
        assertFalse(trigger.isTracking());
        assertTrue(trigger.isInitial());
        
        // now TriggerFiredEvents until we fire
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue8 = 8;
        TriggerFiredEvent event8 = createTfe(tid, metricValue8, tick, ONE_MINUTE); 
        trigger.processEvent(event8);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue9 = 9;
        TriggerFiredEvent event9 = createTfe(tid, metricValue9, tick, ONE_MINUTE); 
        trigger.processEvent(event9);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime;
        _eventTracker.setCurrentTimeMillis(tick);
        clock.setCurrentTime(tick);
        long metricValue10 = 10;
        TriggerFiredEvent event10 = createTfe(tid, metricValue10, tick, ONE_MINUTE); 
        trigger.processEvent(event10);
                
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event10.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());
    }
    
    /**
     * Test the case where the trigger fires even though the agent time is 
     * one collection interval later than the server time. To do this 
     * we roll back the server time by the skew.
     */
    public void testFiresAgentSkewedOneIntervalLaterThanServer() throws Exception {
        Integer tid = new Integer(1);
        long skew = ONE_MINUTE;
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, TWO_MINUTES, TWO_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // now add some TriggerFiredEvents until we fire
        long tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick-skew);
        clock.setCurrentTime(tick-skew);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick-skew);
        clock.setCurrentTime(tick-skew);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick-skew);
        clock.setCurrentTime(tick-skew);
        long metricValue3 = 3;
        TriggerFiredEvent event3 = createTfe(tid, metricValue3, tick, ONE_MINUTE); 
        trigger.processEvent(event3);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event3.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());
        
    }
    
    /**
     * Test the case where the trigger fires even though the server time is  
     * one collection interval later than the agent time. To do this 
     * we roll forward the server time by the skew.
     */
    public void testFiresServerSkewedOneIntervalLaterThanAgent() throws Exception {
        Integer tid = new Integer(1);
        long skew = ONE_MINUTE;
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, TWO_MINUTES, TWO_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // now add some TriggerFiredEvents until we fire
        long tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick+skew);
        clock.setCurrentTime(tick+skew);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick+skew);
        clock.setCurrentTime(tick+skew);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick+skew);
        clock.setCurrentTime(tick+skew);
        long metricValue3 = 3;
        TriggerFiredEvent event3 = createTfe(tid, metricValue3, tick, ONE_MINUTE); 
        trigger.processEvent(event3);
        
        // we should have fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event3.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());
        
    }
    
    /**
     * Test the case where the trigger fires even though the agent time is 
     * skewed later than the server time. If the agent time is later than 
     * the server time, then we are in good shape because the trigger will 
     * always fire when the count is reached. If the server time is later than 
     * the agent time, then the trigger won't fire if the skew is 2 intervals 
     * or more (see the next unit test). 
     */
    public void testFiresAgentSkewedTwoIntervalsLaterThanServer() throws Exception {
        Integer tid = new Integer(1);
        long skew = 2*ONE_MINUTE;
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, TWO_MINUTES, TWO_MINUTES, clock, true);
        
        assertTrue(trigger.isInitial());
        
        // now add some TriggerFiredEvents until we fire
        long tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick-skew);
        clock.setCurrentTime(tick-skew);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick-skew);
        clock.setCurrentTime(tick-skew);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick-skew);
        clock.setCurrentTime(tick-skew);
        long metricValue3 = 3;
        TriggerFiredEvent event3 = createTfe(tid, metricValue3, tick, ONE_MINUTE); 
        trigger.processEvent(event3);
        
        // we should haved fired and been reset back to the initial state
        verifyExpectations(trigger);
        
        assertTrue(trigger.isInitial());
        
        // check the firing timestamp
        assertEquals("Expected a different firing timestamp.", 
                      event3.getTimestamp(), 
                      trigger.getLastFiringEvent().getTimestamp());    
    }
    
    /**
     * Test the case where the trigger doesn't fire because the server time 
     * is skewed two collection intervals later than the agent time.
     */
    public void testNotFireServerSkewedTwoIntervalsLaterThanAgent() throws Exception {
        Integer tid = new Integer(1);
        long skew = 2*ONE_MINUTE;
        
        UserSpecifiedTimeClock clock = new UserSpecifiedTimeClock();
                
        DurationTrigger trigger = 
            createTrigger(tid, TWO_MINUTES, TWO_MINUTES, clock, false);
        
        assertTrue(trigger.isInitial());
        
        // now add some TriggerFiredEvents until we fire
        long tick = _currentTime-FOUR_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick+skew);
        clock.setCurrentTime(tick+skew);
        long metricValue1 = 1;
        TriggerFiredEvent event1 = createTfe(tid, metricValue1, tick, ONE_MINUTE); 
        trigger.processEvent(event1);
        
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-THREE_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick+skew);
        clock.setCurrentTime(tick+skew);
        long metricValue2 = 2;
        TriggerFiredEvent event2 = createTfe(tid, metricValue2, tick, ONE_MINUTE); 
        trigger.processEvent(event2);

        assertTrue(trigger.isTracking());
        
        tick = _currentTime-TWO_MINUTES;
        _eventTracker.setCurrentTimeMillis(tick+skew);
        clock.setCurrentTime(tick+skew);
        long metricValue3 = 3;
        TriggerFiredEvent event3 = createTfe(tid, metricValue3, tick, ONE_MINUTE); 
        trigger.processEvent(event3);
             
        assertTrue(trigger.isTracking());
        
        tick = _currentTime-ONE_MINUTE;
        _eventTracker.setCurrentTimeMillis(tick+skew);
        clock.setCurrentTime(tick+skew);
        long metricValue4 = 4;
        TriggerFiredEvent event4 = createTfe(tid, metricValue4, tick, ONE_MINUTE); 
        trigger.processEvent(event4);
        
        // we shouldn't have fired
        verifyExpectations(trigger);
        
        assertTrue(trigger.isTracking());  

    }    
    
    private DurationTrigger createTrigger(Integer triggerId, 
                                          long count, 
                                          long timeRange, 
                                          Clock clock, 
                                          boolean expectFire) {
        DurationTrigger trigger = 
            new DurationTrigger(triggerId, count, timeRange, clock);
        
        // We don't want to actually fire actions since that will tie 
        // our unit tests to the database and other subsystems.
        MockTriggerFireStrategy strategy = new MockTriggerFireStrategy();
        
        strategy.setExpectedNumTimesActionsFired((expectFire)?1:0);
        
        trigger.setTriggerFireStrategy(strategy);
        
        return trigger;
    }
    
    private TriggerFiredEvent createTfe(Integer triggerId, 
                                        long metricValue, 
                                        long timestamp, 
                                        long interval) {
        long adjust = TimingVoodoo.roundDownTime(timestamp, interval);
        
        Integer mid = new Integer(100);
        MetricValue value = new MetricValue(metricValue, adjust);
        AbstractEvent rootEvent = new MeasurementEvent(mid, value);
        AbstractEvent[] events = {rootEvent};        
        TriggerFiredEvent event = new TriggerFiredEvent(triggerId, events);
        event.setTimestamp(adjust);
        return event;
    }
    
    private TriggerNotFiredEvent createTnfe(Integer triggerId, long timestamp) {
        TriggerNotFiredEvent event = new TriggerNotFiredEvent(triggerId);
        event.setTimestamp(timestamp);
        return event;
    }
    
    private HeartBeatEvent createHeartBeatEvent(long timestamp) {
        return new HeartBeatEvent(new Date(timestamp));
    }
    
    private void verifyExpectations(DurationTrigger trigger) {
        _eventTracker.verify();
        
        TriggerFireStrategy strategy = trigger.getTriggerFireStrategy();
        
        if (strategy != null && strategy instanceof MockTriggerFireStrategy) {
            ((MockTriggerFireStrategy)strategy).verify();
        } else {
            throw new IllegalStateException("the trigger fire strategy was not set");
        }        
    }

}
