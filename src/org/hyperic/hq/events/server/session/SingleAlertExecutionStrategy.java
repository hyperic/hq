package org.hyperic.hq.events.server.session;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.zevents.ZeventEnqueuer;

/**
 * Implementation of {@link ExecutionStrategy} that simply enqueues an
 * {@link AlertConditionsSatisfiedZEvent} for processing. This is typically used
 * by alert definitions with a frequency of everytime or once.
 * @author jhickey
 *
 */
public class SingleAlertExecutionStrategy implements ExecutionStrategy {

    private final ZeventEnqueuer zeventEnqueuer;

    private final Log log = LogFactory.getLog(SingleAlertExecutionStrategy.class);

    /**
     *
     * @param zeventEnqueuer The {@link ZeventEnqueuer} to use for sending
     *        {@link AlertConditionsSatisfiedZEvent}s
     */
    public SingleAlertExecutionStrategy(ZeventEnqueuer zeventEnqueuer) {
        this.zeventEnqueuer = zeventEnqueuer;
    }

    public void conditionsSatisfied(AlertConditionsSatisfiedZEvent event) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Firing event " + event);
            }
            zeventEnqueuer.enqueueEvent(event);
        } catch (InterruptedException e) {
            log.warn("Interrupted enqueuing an AlertConditionsSatisfiedZEvent.  Event: " + event +
                     " will not be processed.  Cause: " + e.getMessage());
        }
    }

    public Serializable getState() {
        return null;
    }

    public void initialize(Serializable initialState) {
        // No-Op
    }

}
