package org.hyperic.hq.events.server.session;

import java.io.Serializable;

import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;

/**
 * Evaluates whether or not an alert should fire when a specific event causes a
 * single alert condition to evaluate to true or false
 * @author jhickey
 *
 */
public interface AlertConditionEvaluator {

    /**
     *
     * @return The ID of the alert definition associated with this evaluator
     */
    Integer getAlertDefinitionId();

    /**
     *
     * @return The {@link ExecutionStrategy} used by this evaluator to fire alert condition satisfied events
     */
    ExecutionStrategy getExecutionStrategy();

    /**
     *
     * @return Any state held by this evaluator that should be persisted between server restarts.  May be null if no state saved.
     */
    Serializable getState();

    /**
     * Initializes this evaluator
     * @param initialState Any state that was saved by the evaluator with this alertDefinitionId the last time the server was shutdown.  May be null if no state saved.
     */
    void initialize(Serializable initialState);

    /**
     * A trigger was fired, indicating an alert condition evaluated to true
     * @param event The {@link TriggerFiredEvent} representing the data that
     *        caused the condition to evaluate to true
     */
    void triggerFired(TriggerFiredEvent event);

    /**
     * A trigger was not fired, indicating that an alert condition evaluated to
     * false
     * @param event The {@link TriggerNotFiredEvent} representing the data that
     *        caused the condition to evaluate to true
     */
    void triggerNotFired(TriggerNotFiredEvent event);

}
