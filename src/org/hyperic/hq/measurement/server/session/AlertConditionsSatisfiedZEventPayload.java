package org.hyperic.hq.measurement.server.session;

import java.util.Arrays;
import java.util.Date;

import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.zevents.ZeventPayload;

/**
 * Payload of the AlertConditionsSatisfiedZEvent
 * @author jhickey
 *
 */
public class AlertConditionsSatisfiedZEventPayload implements ZeventPayload {
    private TriggerFiredEvent[] triggerFiredEvents;
    private String message;

    /**
     *
     * @param triggerFiredEvents The events satisfying the alert conditions
     */
    public AlertConditionsSatisfiedZEventPayload(TriggerFiredEvent[] triggerFiredEvents) {
        this.triggerFiredEvents = triggerFiredEvents;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AlertConditionsSatisfiedZEventPayload)) {
            return false;
        }
        AlertConditionsSatisfiedZEventPayload other = (AlertConditionsSatisfiedZEventPayload) obj;
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        } else if (!message.equals(other.message)) {
            return false;
        }
        if (!Arrays.equals(triggerFiredEvents, other.triggerFiredEvents)) {
            return false;
        }
        return true;
    }

    /**
     *
     * @return The event message
     */
    public String getMessage() {
        if (this.message == null) {
            StringBuffer message = new StringBuffer();
            for (int i = 0; i < triggerFiredEvents.length; i++) {
                message.append(triggerFiredEvents[i]);
                message.append("\n");
            }
            return message.toString();
        }
        return this.message;
    }

    /**
     *
     * @return The event timestamp - the most recent timestamp of the underlying
     *         {@link TriggerFiredEvent}s
     */
    public long getTimestamp() {
        long timestamp = 0;
        for (int i = 0; i < triggerFiredEvents.length; i++) {
            timestamp = Math.max(timestamp, triggerFiredEvents[i].getTimestamp());
        }
        return timestamp;
    }

    /**
     *
     * @return The events satisfying the alert conditions
     */
    public TriggerFiredEvent[] getTriggerFiredEvents() {
        return triggerFiredEvents;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + Arrays.hashCode(triggerFiredEvents);
        return result;
    }

    /**
     *
     * @param message The message associated with this event
     */
    public void setMessage(String message) {
        this.message = message;
    }

    public String toString() {
        return "[message=" + getMessage() + ", timestamp=" + new Date(getTimestamp()) + "]";
    }
}
