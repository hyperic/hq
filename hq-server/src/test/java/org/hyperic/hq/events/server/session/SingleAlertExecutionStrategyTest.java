package org.hyperic.hq.events.server.session;

import org.easymock.EasyMock;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.zevents.ZeventEnqueuer;

import junit.framework.TestCase;
/**
 * Unit test of {@link SingleAlertExecutionStrategy}
 * @author jhickey
 *
 */
public class SingleAlertExecutionStrategyTest extends TestCase {
    private SingleAlertExecutionStrategy executionStrategy;
    private ZeventEnqueuer zeventEnqueuer;

    public void setUp() throws Exception {
        super.setUp();
        this.zeventEnqueuer = EasyMock.createMock(ZeventEnqueuer.class);
        this.executionStrategy = new SingleAlertExecutionStrategy(zeventEnqueuer);
    }

    /**
     * Verifies that nothing blows up if failure to enqueue an {@link AlertConditionsSatisfiedZEvent}
     * @throws InterruptedException
     */
    public void testErrorEnqueuingEvent() throws InterruptedException {
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, new MockEvent(1l, 2));
        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(1234,
                                                                                  new TriggerFiredEvent[] { triggerFired });
        zeventEnqueuer.enqueueEvent(event);
        EasyMock.expectLastCall().andThrow(new InterruptedException("Something Bad"));
        EasyMock.replay(zeventEnqueuer);
        executionStrategy.conditionsSatisfied(event);
        EasyMock.verify(zeventEnqueuer);
    }

    /**
     * Verifies an {@link AlertConditionsSatisfiedZEvent} is enqueued
     * @throws InterruptedException
     */
    public void testSingleAlert() throws InterruptedException {
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, new MockEvent(1l, 2));
        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(1234,
                                                                                  new TriggerFiredEvent[] { triggerFired });
        zeventEnqueuer.enqueueEvent(event);
        EasyMock.replay(zeventEnqueuer);
        executionStrategy.conditionsSatisfied(event);
        EasyMock.verify(zeventEnqueuer);
    }

}
