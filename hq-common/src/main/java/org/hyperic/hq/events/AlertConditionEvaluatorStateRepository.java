package org.hyperic.hq.events;

import java.io.Serializable;
import java.util.Map;





/**
 * Repository to save and access state of AlertConditionEvaluators and
 * their associated ExecutionStrategys.
 * @author jhickey
 * 
 */
public interface AlertConditionEvaluatorStateRepository {
    /**
     * 
     * @return A Map where key is alert definition ID and value is the
     *         Serializable stored state of the AlertConditionEvaluator
     *         with that ID.
     */
    Map<Integer, Serializable> getAlertConditionEvaluatorStates();

    /**
     * 
     * @return A Map where key is alert definition ID and value is the
     *         Serializable stored state of the ExecutionStrategy with
     *         that ID.
     */
    Map<Integer, Serializable> getExecutionStrategyStates();

    /**
     * Persists states of AlertConditionEvaluators
     * @param alertConditionEvaluatorStates A Map where key is alert definition
     *        ID and value is the Serializable state of the
     *        AlertConditionEvaluator with that ID.
     */
    void saveAlertConditionEvaluatorStates(Map<Integer, Serializable> alertConditionEvaluatorStates);

    /**
     * Persists states of  ExecutionStrategys
     * @param executionStrategyStates A Map where key is alert definition ID and
     *        value is the Serializable state of the  ExecutionStrategy
     *        with that ID.
     */
    void saveExecutionStrategyStates(Map<Integer, Serializable> executionStrategyStates);

}
