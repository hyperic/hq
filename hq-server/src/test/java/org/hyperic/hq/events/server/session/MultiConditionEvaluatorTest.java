/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.product.MetricValue;
import org.junit.Ignore;

/**
 * Unit test of {@link MultiConditionEvaluator}
 * @author jhickey
 *
 */
@Ignore
public class MultiConditionEvaluatorTest
    extends TestCase
{

    private ExecutionStrategy executionStrategy;

    private Map<Integer, AbstractEvent> events;

    private static final Integer TEST_ALERT_DEF_ID = Integer.valueOf(1234);

    public void setUp() throws Exception {
        super.setUp();
        this.events = new LinkedHashMap<Integer, AbstractEvent>();
        this.executionStrategy = EasyMock.createMock(ExecutionStrategy.class);

    }

    /**
     * Verifies that an {@link AlertConditionsSatisfiedZEvent} is not created if
     * one of 2 required conditions is not met
     */
    public void testMultiAndConditionNotMet() {
        long timeRange = 10 * 60 * 1000;
        Integer trigger1Id = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition1 = new AlertCondition();
        RegisteredTrigger trigger1 = new RegisteredTrigger();

        trigger1.setId(trigger1Id);
        condition1.setTrigger(trigger1);
        condition1.setRequired(true);
        conditions.add(condition1);

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        MultiConditionEvaluator evaluator = new MultiConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                        conditions,
                                                                        timeRange,
                                                                        executionStrategy,
                                                                        events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(trigger1Id);
        // event occurred 4 minutes ago
        triggerNotFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerNotFired(triggerNotFired);
        EasyMock.verify(executionStrategy);

        Map<Integer, AbstractEvent> expectedEvents = new LinkedHashMap<Integer, AbstractEvent>();
        expectedEvents.put(trigger2Id, triggerFired);
        expectedEvents.put(trigger1Id, triggerNotFired);

        assertEquals(expectedEvents, events);
    }

    /**
     * This validates HQ-3280 and verifies that an {@link AlertConditionsSatisfiedZEvent} 
     * is not created if one of 2 required conditions for the same metric is not met.
     */
    public void testMultiAndTwoConditionsWithSameMetricNotMet() {
        long timeRange = 0;
        Integer measurementId = Integer.valueOf(11);
        Integer trigger1Id = Integer.valueOf(1111);
        Integer trigger2Id = Integer.valueOf(2222);

        // alert condition 1 and alert condition 2 are on the same measurement
        // and evaluates to the direct opposites of each other. for example,
        // alert condition 1 is " = 1 " while alert condition 2 is " != 1 ".
        // so when a metric triggers a TriggerFiredEvent, a corresponding
        // TriggerNotFiredEvent is also fired for the other "opposite" trigger
        // so the alert should never fire.

        List conditions = new ArrayList();

        AlertCondition condition1 = new AlertCondition();
        RegisteredTrigger trigger1 = new RegisteredTrigger();

        trigger1.setId(trigger1Id);
        condition1.setTrigger(trigger1);
        condition1.setMeasurementId(measurementId.intValue());
        condition1.setRequired(true);
        condition1.setComparator("=");
        condition1.setThreshold(1);
        conditions.add(condition1);

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setMeasurementId(measurementId.intValue());
        condition2.setRequired(true);
        condition2.setComparator("!=");
        condition2.setThreshold(1);
        conditions.add(condition2);

        MultiConditionEvaluator evaluator = new MultiConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                        conditions,
                                                                        timeRange,
                                                                        executionStrategy,
                                                                        events);        
        // event occurred 6 minutes ago
        long time1 = System.currentTimeMillis() - (6 * 60 * 1000);
        MetricValue metric1 = new MetricValue(1, time1);
        MeasurementEvent measEvent1 = new MeasurementEvent(measurementId, metric1);
        TriggerFiredEvent trigger1Fired = new TriggerFiredEvent(trigger1Id, measEvent1);
        TriggerNotFiredEvent trigger2NotFired = new TriggerNotFiredEvent(trigger2Id);
        trigger2NotFired.setTimestamp(time1);
        
        // event occurred 1 minute ago
        long time2 = System.currentTimeMillis() - (1 * 60 * 1000);
        MetricValue metric2 = new MetricValue(0, time2);
        MeasurementEvent measEvent2 = new MeasurementEvent(measurementId, metric2);
        TriggerFiredEvent trigger2Fired = new TriggerFiredEvent(trigger2Id, measEvent2);
        TriggerNotFiredEvent trigger1NotFired = new TriggerNotFiredEvent(trigger1Id);
        trigger1NotFired.setTimestamp(time2);

        // send the events in a sequence so that both triggers are fired
        // but a AlertConditionsSatisfiedZEvent is not created
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(trigger1Fired);
        evaluator.triggerNotFired(trigger2NotFired);
        evaluator.triggerFired(trigger2Fired);
        evaluator.triggerNotFired(trigger1NotFired);
        EasyMock.verify(executionStrategy);
        
        Map<Integer, AbstractEvent> expectedEvents = new LinkedHashMap<Integer, AbstractEvent>();
        expectedEvents.put(trigger1Id, trigger1NotFired);
        expectedEvents.put(trigger2Id, trigger2Fired);

        // the triggered events for the second measurement event should be the current events
        assertEquals(expectedEvents, events);
    }

    /**
     * This validates HQ-3280 and verifies that an {@link AlertConditionsSatisfiedZEvent} 
     * is created if all required conditions for the same metric are met and the
     * same measurement event is the source of all fired triggers.
     */
    public void testMultiAndTwoConditionsWithSameMetricMet() {
        long timeRange = 0;
        Integer measurementId = Integer.valueOf(555);
        Integer trigger1Id = Integer.valueOf(9999);
        Integer trigger2Id = Integer.valueOf(2);

        List conditions = new ArrayList();

        AlertCondition condition1 = new AlertCondition();
        RegisteredTrigger trigger1 = new RegisteredTrigger();

        trigger1.setId(trigger1Id);
        condition1.setTrigger(trigger1);
        condition1.setMeasurementId(measurementId.intValue());
        condition1.setRequired(true);
        condition1.setComparator(">");
        condition1.setThreshold(50);
        conditions.add(condition1);

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setMeasurementId(measurementId.intValue());
        condition2.setRequired(true);
        condition2.setComparator("<");
        condition2.setThreshold(1100);
        conditions.add(condition2);

        MultiConditionEvaluator evaluator = new MultiConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                        conditions,
                                                                        timeRange,
                                                                        executionStrategy,
                                                                        events);        
        // event occurred 6 minutes ago
        long time1 = System.currentTimeMillis() - (6 * 60 * 1000);
        MetricValue metric1 = new MetricValue(1200, time1);
        MeasurementEvent measEvent1 = new MeasurementEvent(measurementId, metric1);
        TriggerFiredEvent trigger1Fired1 = new TriggerFiredEvent(trigger1Id, measEvent1);
        TriggerNotFiredEvent trigger2NotFired = new TriggerNotFiredEvent(trigger2Id);
        trigger2NotFired.setTimestamp(time1);
        
        // event occurred 1 minute ago
        long time2 = System.currentTimeMillis() - (1 * 60 * 1000);
        MetricValue metric2 = new MetricValue(777, time2);
        MeasurementEvent measEvent2 = new MeasurementEvent(measurementId, metric2);
        TriggerFiredEvent trigger1Fired2 = new TriggerFiredEvent(trigger1Id, measEvent2);
        TriggerFiredEvent trigger2Fired = new TriggerFiredEvent(trigger2Id, measEvent2);

        TriggerFiredEvent[] triggerFiredEvents = new TriggerFiredEvent[] {trigger1Fired2, trigger2Fired};
        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(), 
        																		  triggerFiredEvents);
        executionStrategy.conditionsSatisfied(event);

        // send the events in a sequence so that both triggers are fired
        // with different measurement events
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(trigger1Fired1);
        evaluator.triggerNotFired(trigger2NotFired);
        evaluator.triggerFired(trigger2Fired);
        evaluator.triggerFired(trigger1Fired2);
        EasyMock.verify(executionStrategy);
        
        assertTrue(events.isEmpty());
    }

    /**
     * This validates HQ-3280 and verifies that an {@link AlertConditionsSatisfiedZEvent} 
     * is created if all required conditions for the same metric are met and the
     * same measurement event is the source of all fired triggers.
     */
    public void testMultiAndThreeConditionsWithSameMetricMet() {
        long timeRange = 0;
        Integer measurementId = Integer.valueOf(555);
        Integer trigger1Id = Integer.valueOf(111);
        Integer trigger2Id = Integer.valueOf(222);
        Integer trigger3Id = Integer.valueOf(333);

        List conditions = new ArrayList();

        AlertCondition condition1 = new AlertCondition();
        RegisteredTrigger trigger1 = new RegisteredTrigger();

        trigger1.setId(trigger1Id);
        condition1.setTrigger(trigger1);
        condition1.setMeasurementId(measurementId.intValue());
        condition1.setRequired(true);
        condition1.setComparator(">");
        condition1.setThreshold(50);
        conditions.add(condition1);

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setMeasurementId(measurementId.intValue());
        condition2.setRequired(true);
        condition2.setComparator(">");
        condition2.setThreshold(500);
        conditions.add(condition2);

        AlertCondition condition3 = new AlertCondition();
        RegisteredTrigger trigger3 = new RegisteredTrigger();

        trigger3.setId(trigger3Id);
        condition3.setTrigger(trigger3);
        condition3.setMeasurementId(measurementId.intValue());
        condition3.setRequired(true);
        condition3.setComparator("<");
        condition3.setThreshold(1000);
        conditions.add(condition3);

        MultiConditionEvaluator evaluator = new MultiConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                        conditions,
                                                                        timeRange,
                                                                        executionStrategy,
                                                                        events);        
        // event occurred 6 minutes ago
        long time1 = System.currentTimeMillis() - (6 * 60 * 1000);
        MetricValue metric1 = new MetricValue(1500, time1);
        MeasurementEvent measEvent1 = new MeasurementEvent(measurementId, metric1);
        TriggerFiredEvent trigger1Fired1 = new TriggerFiredEvent(trigger1Id, measEvent1);
        TriggerFiredEvent trigger2Fired1 = new TriggerFiredEvent(trigger2Id, measEvent1);
        TriggerNotFiredEvent trigger3NotFired = new TriggerNotFiredEvent(trigger3Id);
        trigger3NotFired.setTimestamp(time1);
        
        // event occurred 1 minute ago
        long time2 = System.currentTimeMillis() - (1 * 60 * 1000);
        MetricValue metric2 = new MetricValue(750, time2);
        MeasurementEvent measEvent2 = new MeasurementEvent(measurementId, metric2);
        TriggerFiredEvent trigger1Fired2 = new TriggerFiredEvent(trigger1Id, measEvent2);
        TriggerFiredEvent trigger2Fired2 = new TriggerFiredEvent(trigger2Id, measEvent2);
        TriggerFiredEvent trigger3Fired = new TriggerFiredEvent(trigger3Id, measEvent2);

        TriggerFiredEvent[] triggerFiredEvents = new TriggerFiredEvent[] {trigger1Fired2, 
        																  trigger2Fired2,
        																  trigger3Fired};
        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID.intValue(), 
        																		  triggerFiredEvents);
        executionStrategy.conditionsSatisfied(event);

        // send the events in a sequence so that all triggers are fired
        // with different measurement events
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(trigger1Fired1);
        evaluator.triggerFired(trigger2Fired1);
        evaluator.triggerNotFired(trigger3NotFired);
        evaluator.triggerFired(trigger3Fired);
        evaluator.triggerFired(trigger2Fired2);
        evaluator.triggerFired(trigger1Fired2);
        EasyMock.verify(executionStrategy);
        
        assertTrue(events.isEmpty());
    }

    /**
     * Verifies that conditions are evaluated properly when time range is 0 (no
     * expiration date)
     */
    public void testMultiConditionNoExpirationDate() {
        long timeRange = 0;
        Integer trigger1Id = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition1 = new AlertCondition();
        RegisteredTrigger trigger1 = new RegisteredTrigger();

        trigger1.setId(trigger1Id);
        condition1.setTrigger(trigger1);
        condition1.setRequired(true);
        conditions.add(condition1);

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        MultiConditionEvaluator evaluator = new MultiConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                        conditions,
                                                                        timeRange,
                                                                        executionStrategy,
                                                                        events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent mockEvent2 = new MockEvent(3l, 4);
        // trigger fired event occurred 12 minutes ago
        mockEvent2.setTimestamp(System.currentTimeMillis() - (12 * 60 * 1000));
        TriggerFiredEvent triggerFired2 = new TriggerFiredEvent(trigger1Id, mockEvent2);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID,
                                                                                  new TriggerFiredEvent[] { triggerFired,
                                                                                                           triggerFired2 });
        executionStrategy.conditionsSatisfied(event);

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerFired(triggerFired2);
        EasyMock.verify(executionStrategy);

        assertTrue(events.isEmpty());
    }

    /**
     * Verifies that an {@link AlertConditionsSatisfiedZEvent} is created if one
     * condition in a (1|2) scenario is met
     */
    public void testMultiOrConditionMet() {
        long timeRange = 10 * 60 * 1000;
        int trigger1Id = 5678;
        int trigger2Id = 55;

        List conditions = new ArrayList();

        AlertCondition condition1 = new AlertCondition();
        RegisteredTrigger trigger1 = new RegisteredTrigger();

        trigger1.setId(trigger1Id);
        condition1.setTrigger(trigger1);
        condition1.setRequired(true);
        conditions.add(condition1);

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(false);
        conditions.add(condition2);

        MultiConditionEvaluator evaluator = new MultiConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                        conditions,
                                                                        timeRange,
                                                                        executionStrategy,
                                                                        events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID,
                                                                                  new TriggerFiredEvent[] { triggerFired });

        executionStrategy.conditionsSatisfied(event);
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        EasyMock.verify(executionStrategy);
        assertTrue(events.isEmpty());
    }

    /**
     * Verifies that an event associated with a trigger will not be counted if
     * timestamp indicates the event is older than one we have already processed
     * for the same trigger
     */
    public void testOlderEventsNotUpdating() {
        long timeRange = 10 * 60 * 1000;
        Integer trigger1Id = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition1 = new AlertCondition();
        RegisteredTrigger trigger1 = new RegisteredTrigger();

        trigger1.setId(trigger1Id);
        condition1.setTrigger(trigger1);
        condition1.setRequired(true);
        conditions.add(condition1);

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        MultiConditionEvaluator evaluator = new MultiConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                        conditions,
                                                                        timeRange,
                                                                        executionStrategy,
                                                                        events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(trigger1Id);
        // event occurred 4 minutes ago
        triggerNotFired.setTimestamp(System.currentTimeMillis() - (4 * 60 * 1000));

        MockEvent mockEvent2 = new MockEvent(3l, 4);
        // trigger fired event occurred 5 minutes ago
        mockEvent2.setTimestamp(System.currentTimeMillis() - (5 * 60 * 1000));
        TriggerFiredEvent triggerFired2 = new TriggerFiredEvent(trigger1Id, mockEvent2);

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        evaluator.triggerNotFired(triggerNotFired);
        // sending the TFE with an older timestamp than the TNF should leave the
        // TNF in the events map and not pass the evaluation
        evaluator.triggerFired(triggerFired2);
        EasyMock.verify(executionStrategy);

        Map<Integer, AbstractEvent> expectedEvents = new LinkedHashMap<Integer, AbstractEvent>();
        expectedEvents.put(trigger2Id, triggerFired);
        expectedEvents.put(trigger1Id, triggerNotFired);

        assertEquals(expectedEvents, events);
    }

    /**
     * Verifies that expired events are not counted in condition evaluation
     */
    public void testProcessExpiredEvent() {
        long timeRange = 10 * 60 * 1000;
        Integer trigger1Id = Integer.valueOf(5678);
        Integer trigger2Id = Integer.valueOf(55);

        List conditions = new ArrayList();

        AlertCondition condition1 = new AlertCondition();
        RegisteredTrigger trigger1 = new RegisteredTrigger();

        trigger1.setId(trigger1Id);
        condition1.setTrigger(trigger1);
        condition1.setRequired(true);
        conditions.add(condition1);

        AlertCondition condition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();

        trigger2.setId(trigger2Id);
        condition2.setTrigger(trigger2);
        condition2.setRequired(true);
        conditions.add(condition2);

        MultiConditionEvaluator evaluator = new MultiConditionEvaluator(TEST_ALERT_DEF_ID,
                                                                        conditions,
                                                                        timeRange,
                                                                        executionStrategy,
                                                                        events);

        MockEvent mockEvent = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(trigger2Id, mockEvent);

        MockEvent mockEvent2 = new MockEvent(3l, 4);
        // trigger fired event occurred 12 minutes ago and is already expired
        mockEvent2.setTimestamp(System.currentTimeMillis() - (12 * 60 * 1000));
        TriggerFiredEvent triggerFired2 = new TriggerFiredEvent(trigger1Id, mockEvent2);

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        // this setTimestamp() is signifying that time has passed since the last
        // trigger fired
        mockEvent.setTimestamp(System.currentTimeMillis() - (12 * 60 * 1000));
        evaluator.triggerFired(triggerFired2);
        EasyMock.verify(executionStrategy);

        Map<Integer, AbstractEvent> expectedEvents = new LinkedHashMap<Integer, AbstractEvent>();
        assertEquals(expectedEvents, events);
    }

}
