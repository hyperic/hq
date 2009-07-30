package org.hyperic.hq.events.server.session;

import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

/**
 * Determines if an alert should be fired once the conditions associated with an
 * alert definition have been met
 * @author jhickey
 *
 */
public interface ExecutionStrategy {

    /**
     * Indicates that all conditions associated with an alert definition have been met
     * @param event An {@link AlertConditionsSatisfiedZEvent} to process
     */
    void conditionsSatisfied(AlertConditionsSatisfiedZEvent event);
}
