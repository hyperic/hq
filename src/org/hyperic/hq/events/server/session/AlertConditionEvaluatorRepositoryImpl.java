package org.hyperic.hq.events.server.session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hyperic.hq.application.ShutdownCallback;

/**
 * Default implementation of{@link AlertConditionEvaluatorRepository} This
 * implementation is NOT thread-safe. Access to add, remove, and get should be
 * synchronized by external callers if concurrent access is expected (current
 * impl of add/get is single-threaded through RegisteredTriggerManager. remove
 * is very unlikely to occur concurrently). This implementation persists the
 * state of {@link AlertConditionEvaluator}s and their {@link ExecutionStrategy}
 * s on server shutdown.
 * @author jhickey
 *
 */
public class AlertConditionEvaluatorRepositoryImpl implements AlertConditionEvaluatorRepository, ShutdownCallback {
    private AlertConditionEvaluatorStateRepository alertConditionEvaluatorStateRepository;
    private Map alertConditionEvaluators = new HashMap();

    /**
     *
     * @param alertConditionEvaluatorStateRepository The
     *        {@link AlertConditionEvaluatorStateRepository} to use for
     *        persisting state on server shutdown
     */
    public AlertConditionEvaluatorRepositoryImpl(AlertConditionEvaluatorStateRepository alertConditionEvaluatorStateRepository)
    {
        this.alertConditionEvaluatorStateRepository = alertConditionEvaluatorStateRepository;
    }

    public void addAlertConditionEvaluator(AlertConditionEvaluator alertConditionEvaluator) {
        alertConditionEvaluators.put(alertConditionEvaluator.getAlertDefinitionId(), alertConditionEvaluator);

    }

    public AlertConditionEvaluator getAlertConditionEvaluatorById(Integer alertDefinitionId) {
        return (AlertConditionEvaluator) alertConditionEvaluators.get(alertDefinitionId);
    }

    public void removeAlertConditionEvaluator(Integer alertDefinitionId) {
        alertConditionEvaluators.remove(alertDefinitionId);
    }

    public void shutdown() {
        Map alertConditionEvaluatorStates = new HashMap();
        Map executionStrategyStates = new HashMap();
        for (Iterator iterator = alertConditionEvaluators.values().iterator(); iterator.hasNext();) {
            AlertConditionEvaluator alertConditionEvaluator = (AlertConditionEvaluator) iterator.next();
            Serializable alertConditionEvaluatorState = alertConditionEvaluator.getState();
            if (alertConditionEvaluatorState != null) {
                alertConditionEvaluatorStates.put(alertConditionEvaluator.getAlertDefinitionId(),
                                                  alertConditionEvaluatorState);
            }
            Serializable executionStrategyState = alertConditionEvaluator.getExecutionStrategy().getState();
            if (executionStrategyState != null) {
                executionStrategyStates.put(alertConditionEvaluator.getAlertDefinitionId(), executionStrategyState);
            }
        }
        if (!alertConditionEvaluatorStates.isEmpty()) {
            alertConditionEvaluatorStateRepository.saveAlertConditionEvaluatorStates(alertConditionEvaluatorStates);
        }
        if (!executionStrategyStates.isEmpty()) {
            alertConditionEvaluatorStateRepository.saveExecutionStrategyStates(executionStrategyStates);
        }
    }

}
