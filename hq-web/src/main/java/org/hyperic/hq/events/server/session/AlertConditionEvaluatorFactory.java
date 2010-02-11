package org.hyperic.hq.events.server.session;


/**
 * Factory for creation of an {@link AlertConditionEvaluator} for a specified
 * {@link AlertDefinition}
 * @author jhickey
 * 
 */
public interface AlertConditionEvaluatorFactory {

    /**
     * 
     * @param alertDefinition The alert definition to process
     * @return An {@link AlertConditionEvaluator} to evaluate if alerts should
     *         fire from the given {@link AlertDefinition}
     */
    AlertConditionEvaluator create(AlertDefinition alertDefinition);
}
