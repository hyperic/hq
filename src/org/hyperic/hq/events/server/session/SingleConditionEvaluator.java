package org.hyperic.hq.events.server.session;

import java.io.Serializable;

import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

/**
 * Implementation of {@link AlertConditionEvaluator} that sends a new
 * {@link AlertConditionsSatisfiedZEvent} to its {@link ExecutionStrategy}
 * whenever a trigger is fired, representing the evaluation of a single
 * condition.
 * @author jhickey
 *
 */
public class SingleConditionEvaluator implements AlertConditionEvaluator {
    private final Integer alertDefinitionId;

    private final ExecutionStrategy executionStrategy;

    /**
     *
     * @param alertDefinitionId The ID of the alert definition whose conditions
     *        are being evaluated
     * @param executionStrategy The {@link ExecutionStrategy} to use for firing
     *        an {@link AlertConditionsSatisfiedZEvent} when a condition has
     *        been met
     */
    public SingleConditionEvaluator(Integer alertDefinitionId, ExecutionStrategy executionStrategy) {
        this.alertDefinitionId = alertDefinitionId;
        this.executionStrategy = executionStrategy;
    }

    public Integer getAlertDefinitionId() {
        return this.alertDefinitionId;
    }

    public ExecutionStrategy getExecutionStrategy() {
        return this.executionStrategy;
    }

    public Serializable getState() {
        return null;
    }

    public void initialize(Serializable initialState) {
       //No-Op
    }

    public void triggerFired(TriggerFiredEvent event) {
        executionStrategy.conditionsSatisfied(new AlertConditionsSatisfiedZEvent(alertDefinitionId.intValue(),
                                                                                 new TriggerFiredEvent[] { event }));
    }

    public void triggerNotFired(TriggerNotFiredEvent event) {
        // No-Op
    }

}
