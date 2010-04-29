package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.zevents.Zevent;

/**
 * Represents that all conditions of an alert have been satisfied and the alert
 * can be created by listeners
 * @author jhickey
 *
 */
public class AlertConditionsSatisfiedZEvent
    extends Zevent
{

    /**
     *
     * @param alertDefId The alert definition Id
     * @param triggerFiredEvents The events satisfying the alert conditions
     */
    public AlertConditionsSatisfiedZEvent(int alertDefId, TriggerFiredEvent[] triggerFiredEvents) {
        super(new AlertConditionsSatisfiedZEventSource(alertDefId),
              new AlertConditionsSatisfiedZEventPayload(triggerFiredEvents));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !(obj instanceof AlertConditionsSatisfiedZEvent)) {
            return false;
        }
        AlertConditionsSatisfiedZEvent other = (AlertConditionsSatisfiedZEvent) obj;
        return getPayload().equals(other.getPayload()) && getSourceId().equals(other.getSourceId());
    }
    
    public String toString() {
        return getPayload().toString();
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getPayload().hashCode();
        result = prime * result + getSourceId().hashCode();
        return result;
    }
}
