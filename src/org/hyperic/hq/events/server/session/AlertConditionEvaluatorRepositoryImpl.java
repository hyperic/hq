package org.hyperic.hq.events.server.session;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.application.HQApp;
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
    private static final Log log = LogFactory.getLog(AlertConditionEvaluatorRepositoryImpl.class);

    private static final Object INIT_LOCK = new Object();
    private static AlertConditionEvaluatorRepositoryImpl INSTANCE;

    private AlertConditionEvaluatorStateRepository alertConditionEvaluatorStateRepository;
    private Map alertConditionEvaluators = new HashMap();

    /**
     *
     * @param alertConditionEvaluatorStateRepository The
     *        {@link AlertConditionEvaluatorStateRepository} to use for
     *        persisting state on server shutdown
     */
    protected AlertConditionEvaluatorRepositoryImpl(AlertConditionEvaluatorStateRepository alertConditionEvaluatorStateRepository)
    {
        this.alertConditionEvaluatorStateRepository = alertConditionEvaluatorStateRepository;
    }

    public void addAlertConditionEvaluator(AlertConditionEvaluator alertConditionEvaluator) {
        synchronized(alertConditionEvaluators) {
            alertConditionEvaluators.put(alertConditionEvaluator.getAlertDefinitionId(), alertConditionEvaluator);
        }
    }
    
    public AlertConditionEvaluator getAlertConditionEvaluatorById(Integer alertDefinitionId) {
        synchronized(alertConditionEvaluators) {
            return (AlertConditionEvaluator) alertConditionEvaluators.get(alertDefinitionId);
        }
    }

    /**
     * ConcurrentModificationException may occur to callers of this method 
     * when iterating through the map. However, since the 
     * AlertConditionEvaluatorDiagnosticService MBean is the only client,
     * it is not a big issue for now.
     */
    public Map getAlertConditionEvaluators() {
        return Collections.unmodifiableMap(alertConditionEvaluators);
    }
    
    public void removeAlertConditionEvaluator(Integer alertDefinitionId) {
        synchronized(alertConditionEvaluators) {
            alertConditionEvaluators.remove(alertDefinitionId);
        }
    }
    
    public AlertConditionEvaluatorStateRepository getStateRepository() {
        return this.alertConditionEvaluatorStateRepository;
    }

    public void shutdown() {
        if (log.isDebugEnabled()) {
            log.debug("shutdown starting on " + this);
        }
        
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
    
    private void initialize() {
        if (log.isDebugEnabled()) {
            log.debug("initialize starting on " + this);
        }

        HQApp.getInstance().registerCallbackListener(ShutdownCallback.class, INSTANCE);
    }
    
    public static AlertConditionEvaluatorRepository getInstance() {
        synchronized (INIT_LOCK) {
            if (INSTANCE == null) {
                AlertConditionEvaluatorStateRepository alertConditionEvaluatorStateRepository = 
                    new FileAlertConditionEvaluatorStateRepository(HQApp.getInstance().getRestartStorageDir());

                INSTANCE = new AlertConditionEvaluatorRepositoryImpl(alertConditionEvaluatorStateRepository);
                INSTANCE.initialize();
            }
        }
        return INSTANCE;
    }
}
