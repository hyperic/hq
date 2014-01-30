/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.events.server.session;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.bizapp.server.shared.HeartbeatCurrentTime;
import org.hyperic.hq.events.FileAlertConditionEvaluatorStateRepository;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEventPayload;
import org.hyperic.hq.zevents.ZeventEnqueuer;

/**
 * Unit test of the {@link CounterExecutionStrategy}
 * @author jhickey
 * 
 */
public class CounterExecutionStrategyTest
    extends TestCase
{

    private ZeventEnqueuer zeventEnqueuer;

    private ArrayList expirations = new ArrayList();
    
    private MyHeartbeatCurrentTime myHeartbeatCurrentTime = new MyHeartbeatCurrentTime();

    public void setUp() throws Exception {
        super.setUp();
        this.zeventEnqueuer = EasyMock.createMock(ZeventEnqueuer.class);
    }

    /**
     * Verifies nothing blows up if a non-List is passed to initialize
     */
    public void testInitializeNotList() {
        // 2 events within 10 minutes
        final long timeRange = (10 * 60 * 1000l);
        CounterExecutionStrategy strategy = new CounterExecutionStrategy(2l, timeRange, zeventEnqueuer, myHeartbeatCurrentTime);
        strategy.initialize(null);
        assertTrue(((List)strategy.getState()).isEmpty());
    }

    /**
     * Verifies that an expired event is cleared from the list on evaluation of
     * a new event
     */
    public void testProcessEventClearOldExpired() {
        // 2 events within 10 minutes
        final long timeRange = (10 * 60 * 1000l);
        CounterExecutionStrategy strategy = new CounterExecutionStrategy(2l, timeRange, zeventEnqueuer, myHeartbeatCurrentTime);
        strategy.initialize(expirations);
        MockEvent mockEvent = new MockEvent(1l, 2);
        // event occurred 11 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (11 * 60 * 1000));

        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, mockEvent);
        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(1234,
                                                                                  new TriggerFiredEvent[] { triggerFired });

        MockEvent mockEvent2 = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent2.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired2 = new TriggerFiredEvent(4, mockEvent2);
        AlertConditionsSatisfiedZEvent event2 = new AlertConditionsSatisfiedZEvent(1235,
                                                                                   new TriggerFiredEvent[] { triggerFired2 });

        EasyMock.replay(zeventEnqueuer);
        strategy.conditionsSatisfied(event);
        strategy.conditionsSatisfied(event2);
        FileAlertConditionEvaluatorStateRepository repo = new FileAlertConditionEvaluatorStateRepository(new File(System.getProperty("user.dir")));
        Map states = new HashMap();
        for (int i = 0; i < 100000; i++) {
            states.put(i, strategy.getState());
        }
        repo.saveExecutionStrategyStates(states);

        EasyMock.verify(zeventEnqueuer);
        List expectedExpirations = new ArrayList();
        expectedExpirations.add(mockEvent2.getTimestamp() + timeRange);
        assertEquals(expectedExpirations, expirations);
    }

    /**
     * Verifies that the {@link AlertConditionsSatisfiedZEvent} is properly
     * enqueued if the expected number of events occur within the specified time
     * range
     * @throws InterruptedException
     */
    public void testProcessEventConditionsMet() throws InterruptedException {
        // 2 events within 10 minutes
        final long timeRange = (10 * 60 * 1000l);
        CounterExecutionStrategy strategy = new CounterExecutionStrategy(2l, timeRange, zeventEnqueuer, myHeartbeatCurrentTime);
        strategy.initialize(expirations);
        MockEvent mockEvent = new MockEvent(1l, 2);
        // event occurred 5 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (5 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, mockEvent);
        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(1234,
                                                                                  new TriggerFiredEvent[] { triggerFired });

        MockEvent mockEvent2 = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent2.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired2 = new TriggerFiredEvent(4, mockEvent2);
        AlertConditionsSatisfiedZEvent event2 = new AlertConditionsSatisfiedZEvent(1235,
                                                                                   new TriggerFiredEvent[] { triggerFired2 });

        zeventEnqueuer.enqueueEvent(event2);

        EasyMock.replay(zeventEnqueuer);
        strategy.conditionsSatisfied(event);
        strategy.conditionsSatisfied(event2);
        EasyMock.verify(zeventEnqueuer);

        assertTrue(expirations.isEmpty());

        AlertConditionsSatisfiedZEventPayload payload = (AlertConditionsSatisfiedZEventPayload) event2.getPayload();
        assertEquals("Occurred 2 times in the span of 10 minutes", payload.getMessage());
        assertEquals(triggerFired2.getTimestamp(), payload.getTimestamp());
    }

    /**
     * Verifies that memory state is not cleared if an exception occurs sending
     * off the {@link AlertConditionsSatisfiedZEvent}
     * @throws InterruptedException
     */
    public void testProcessEventErrorEnqueueing() throws InterruptedException {
        // 1 events within 10 minutes
        final long timeRange = (10 * 60 * 1000l);
        CounterExecutionStrategy strategy = new CounterExecutionStrategy(1l, timeRange, zeventEnqueuer, myHeartbeatCurrentTime);
        strategy.initialize(expirations);
        MockEvent mockEvent = new MockEvent(1l, 2);
        // event occurred 5 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (5 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, mockEvent);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(1234,
                                                                                  new TriggerFiredEvent[] { triggerFired });

        zeventEnqueuer.enqueueEvent(event);
        EasyMock.expectLastCall().andThrow(new InterruptedException("Something Bad!"));
        EasyMock.replay(zeventEnqueuer);
        strategy.conditionsSatisfied(event);
        EasyMock.verify(zeventEnqueuer);
        // The event should remain in the queue for a reasonable attempt at
        // triggering again
        List expectedExpirations = new ArrayList();
        expectedExpirations.add(mockEvent.getTimestamp() + timeRange);
        assertEquals(expectedExpirations, expirations);
    }

    /**
     * Verifies that an event is properly processed and the correct expiration
     * date is stored
     */
    public void testProcessEventUpdatesExpirations() {
        // 3 events within 10 minutes
        final long timeRange = (10 * 60 * 1000l);
        CounterExecutionStrategy strategy = new CounterExecutionStrategy(3l, timeRange, zeventEnqueuer, myHeartbeatCurrentTime);
        strategy.initialize(expirations);
        MockEvent mockEvent = new MockEvent(1l, 2);
        // event occurred 5 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (5 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, mockEvent);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(1234,
                                                                                  new TriggerFiredEvent[] { triggerFired });
        EasyMock.replay(zeventEnqueuer);
        strategy.conditionsSatisfied(event);
        EasyMock.verify(zeventEnqueuer);
        List expectedExpirations = new ArrayList();
        expectedExpirations.add(mockEvent.getTimestamp() + timeRange);
        assertEquals(expectedExpirations, expirations);
    }

    /**
     * Verifies that an expired event is not processed
     */
    public void testProcessExpiredEvent() {
        // 2 events within 10 minutes
        final long timeRange = (10 * 60 * 1000l);
        CounterExecutionStrategy strategy =
            new CounterExecutionStrategy(2l, timeRange, zeventEnqueuer, myHeartbeatCurrentTime);
        strategy.initialize(expirations);
        MockEvent mockEvent = new MockEvent(1l, 2);
        // event occurred 11 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (11 * 60 * 1000));

        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, mockEvent);
        AlertConditionsSatisfiedZEvent event =
            new AlertConditionsSatisfiedZEvent(1234, new TriggerFiredEvent[] { triggerFired });

        EasyMock.replay(zeventEnqueuer);
        strategy.conditionsSatisfied(event);
        EasyMock.verify(zeventEnqueuer);

        assertTrue(expirations.isEmpty());
    }
    
    private class MyHeartbeatCurrentTime implements HeartbeatCurrentTime {
        public long getTimeMillis() {
            return System.currentTimeMillis();
        }
    }

}
