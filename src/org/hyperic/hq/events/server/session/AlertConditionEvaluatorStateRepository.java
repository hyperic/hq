package org.hyperic.hq.events.server.session;

import java.io.Serializable;
import java.util.Map;

/**
 * Repository to save and access state of {@link AlertConditionEvaluator}s and
 * their associated {@link ExecutionStrategy}s.
 * @author jhickey
 * 
 */
public interface AlertConditionEvaluatorStateRepository {
    /**
     * 
     * @return A Map where key is alert definition ID and value is the
     *         Serializable stored state of the {@link AlertConditionEvaluator}
     *         with that ID.
     */
    Map<Integer, Serializable> getAlertConditionEvaluatorStates();

    /**
     * 
     * @return A Map where key is alert definition ID and value is the
     *         Serializable stored state of the {@link ExecutionStrategy} with
     *         that ID.
     */
    Map<Integer, Serializable> getExecutionStrategyStates();

    /**
     * Persists states of {@link AlertConditionEvaluator}s
     * @param alertConditionEvaluatorStates A Map where key is alert definition
     *        ID and value is the Serializable state of the
     *        {@link AlertConditionEvaluator} with that ID.
     */
    void saveAlertConditionEvaluatorStates(Map<Integer, Serializable> alertConditionEvaluatorStates);

    /**
     * Persists states of {@link ExecutionStrategy}s
     * @param executionStrategyStates A Map where key is alert definition ID and
     *        value is the Serializable state of the {@link ExecutionStrategy}
     *        with that ID.
     */
    void saveExecutionStrategyStates(Map<Integer, Serializable> executionStrategyStates);

}
