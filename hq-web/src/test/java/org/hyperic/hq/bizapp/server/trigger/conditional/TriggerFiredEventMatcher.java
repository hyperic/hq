package org.hyperic.hq.bizapp.server.trigger.conditional;

import java.util.Arrays;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.hyperic.hq.events.TriggerFiredEvent;

/**
 * Implementation of {@link IArgumentMatcher} that compares actual and expected
 * {@link TriggerFiredEvent}s by identity, message, instance id, and underlying
 * events
 * @author jhickey
 *
 */
public class TriggerFiredEventMatcher implements IArgumentMatcher {

    /**
     *
     * @param in The expected event
     * @return null
     */
    public static TriggerFiredEvent eqTriggerFiredEvent(TriggerFiredEvent in) {
        EasyMock.reportMatcher(new TriggerFiredEventMatcher(in));
        return null;
    }

    private TriggerFiredEvent expected;

    /**
     *
     * @param expected The expected {@link TriggerFiredEvent}
     */
    public TriggerFiredEventMatcher(TriggerFiredEvent expected) {
        this.expected = expected;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("eqTriggerFiredEvent(");
        buffer.append(expected.getClass().getName());
        buffer.append(" with message \"");
        buffer.append(expected.getMessage());
        buffer.append(" with id \"");
        buffer.append(expected.getId());
        buffer.append(" with instance id \"");
        buffer.append(expected.getInstanceId());
        buffer.append("\")");
    }

    public boolean matches(Object actual) {
        if (!(expected.equals(actual))) {
            return false;
        }
        TriggerFiredEvent actualEvent = (TriggerFiredEvent) actual;
        if (expected.getMessage() == null) {
            if (actualEvent.getMessage() == null) {
                return true;
            }
            return false;
        }
        if (!(expected.getMessage().equals(((TriggerFiredEvent) actual).getMessage()))) {
            return false;
        }
        if (!(expected.getInstanceId().equals(actualEvent.getInstanceId()))) {
            return false;
        }
        return Arrays.equals(expected.getEvents(), actualEvent.getEvents());
    }

}