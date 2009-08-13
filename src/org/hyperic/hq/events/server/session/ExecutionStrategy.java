package org.hyperic.hq.events.server.session;

import java.io.Serializable;

import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

/**
 * Determines if an alert should be fired once the conditions associated with an
 * alert definition have been met
 * @author jhickey
 * 
 */
public interface ExecutionStrategy {

    /**
     * Indicates that all conditions associated with an alert definition have
     * been met
     * @param event An {@link AlertConditionsSatisfiedZEvent} to process
     */
    void conditionsSatisfied(AlertConditionsSatisfiedZEvent event);

    /**
     * 
     * @return Any state held by this strategy that should be persisted between
     *         server restarts. May be null if no state saved.
     */
    Serializable getState();

    /**
     * Initializes this strategy
     * @param initialState Any state that was saved by the strategy with this
     *        alertDefinitionId the last time the server was shutdown. May be
     *        null if no state saved.
     */
    void initialize(Serializable initialState);
}
