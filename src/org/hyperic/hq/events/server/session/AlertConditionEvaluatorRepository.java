package org.hyperic.hq.events.server.session;

/**
 * Repository of {@link AlertConditionEvaluator}s
 * @author jhickey
 *
 */
public interface AlertConditionEvaluatorRepository {

    /**
     * Add to the repository
     * @param alertConditionEvaluator The {@link AlertConditionEvaluator} to add the repository
     */
    void addAlertConditionEvaluator(AlertConditionEvaluator alertConditionEvaluator);

    /**
     *
     * @param alertDefinitionId The ID of the alert definition
     * @return The corresponding {@link AlertConditionEvaluator} or null if none exists
     */
    AlertConditionEvaluator getAlertConditionEvaluatorById(Integer alertDefinitionId);

    /**
     * Remove from the repository
     * @param alertDefinitionId The ID of the alert definition whose {@link AlertConditionEvaluator} should be removed from the repository
     */
    void removeAlertConditionEvaluator(Integer alertDefinitionId);


}
