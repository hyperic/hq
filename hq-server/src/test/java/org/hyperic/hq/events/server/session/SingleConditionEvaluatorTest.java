package org.hyperic.hq.events.server.session;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

/**
 * Unit test of {@link SingleConditionEvaluator}
 * @author jhickey
 *
 */
public class SingleConditionEvaluatorTest
    extends TestCase
{

    private ExecutionStrategy executionStrategy;

    private static final Integer TEST_ALERT_DEF_ID = Integer.valueOf(4567);

    public void setUp() throws Exception {
        super.setUp();
        this.executionStrategy = EasyMock.createMock(ExecutionStrategy.class);
    }

    /**
     * Verifies an {@link AlertConditionsSatisfiedZEvent} is created and
     * forwarded to the {@link ExecutionStrategy} when a trigger fires
     */
    public void testSingleConditionMet() {
        Integer triggerId = Integer.valueOf(5678);
        SingleConditionEvaluator evaluator = new SingleConditionEvaluator(TEST_ALERT_DEF_ID, executionStrategy);
        MockEvent mockEvent = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(triggerId, mockEvent);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID,
                                                                                  new TriggerFiredEvent[] { triggerFired });
        executionStrategy.conditionsSatisfied(event);
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        EasyMock.verify(executionStrategy);
    }

}
