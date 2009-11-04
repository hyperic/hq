package org.hyperic.hq.bizapp.server.trigger.conditional;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.hyperic.hq.events.TriggerNotFiredEvent;

/**
 * Implementation of {@link IArgumentMatcher} that compares actual and expected
 * {@link TriggerNotFiredEvent} by identity, instance ID, and timestamp
 * @author jhickey
 *
 */
public class TriggerNotFiredEventMatcher implements IArgumentMatcher {
    /**
     *
     * @param in The expected event
     * @return null
     */
    public static TriggerNotFiredEvent eqTriggerNotFiredEvent(TriggerNotFiredEvent in) {
        EasyMock.reportMatcher(new TriggerNotFiredEventMatcher(in));
        return null;
    }

    private TriggerNotFiredEvent expected;

    /**
     *
     * @param expected The expected {@link TriggerNotFiredEvent}
     */
    public TriggerNotFiredEventMatcher(TriggerNotFiredEvent expected) {
        this.expected = expected;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("eqTriggerNotFiredEvent(");
        buffer.append(expected.getClass().getName());
        buffer.append(" with instance id \"");
        buffer.append(expected.getInstanceId());
        buffer.append(" with timestamp \"");
        buffer.append(expected.getTimestamp());
        buffer.append("\")");

    }

    public boolean matches(Object actual) {
        if (!(expected.equals(actual))) {
            return false;
        }
        TriggerNotFiredEvent actualEvent = (TriggerNotFiredEvent) actual;
        if (!(expected.getInstanceId().equals(actualEvent.getInstanceId()))) {
            return false;
        }
        return expected.getTimestamp() == actualEvent.getTimestamp();
    }

}
