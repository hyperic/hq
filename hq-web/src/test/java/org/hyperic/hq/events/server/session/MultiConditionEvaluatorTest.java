package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

/**
 * Unit test of {@link MultiConditionEvaluator}
 * @author jhickey
 *
 */
public class MultiConditionEvaluatorTest
    extends TestCase
{

    private ExecutionStrategy executionStrategy;

    private Map events = new LinkedHashMap();

    private static final Integer TEST_ALERT_DEF_ID = Integer.valueOf(1234);

    public void setUp() throws Exception {
        super.setUp();
Thread.sleep(5000);
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

        Map expectedEvents = new LinkedHashMap();
        expectedEvents.put(trigger2Id, triggerFired);
        expectedEvents.put(trigger1Id, triggerNotFired);

        assertEquals(expectedEvents, events);
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

        Map expectedEvents = new LinkedHashMap();
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

        MockEvent mockEvent3 = new MockEvent(4l, 5);
        // trigger fired event occurred 12 minutes ago and is already expired
        mockEvent2.setTimestamp(System.currentTimeMillis() - (12 * 60 * 1000));
        TriggerFiredEvent triggerFired3 = new TriggerFiredEvent(trigger1Id, mockEvent3);

        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        // this setTimestamp() is signifying that time has passed since the last
        // trigger fired
        mockEvent.setTimestamp(System.currentTimeMillis() - (12 * 60 * 1000));
        evaluator.triggerFired(triggerFired2);
        EasyMock.verify(executionStrategy);

        Map expectedEvents = new LinkedHashMap();
        assertEquals(expectedEvents, events);
    }

}
