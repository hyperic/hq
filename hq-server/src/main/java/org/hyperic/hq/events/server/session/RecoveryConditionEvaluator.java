package org.hyperic.hq.events.server.session;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;

/**
 *Implementation of {@link AlertConditionEvaluator} that knows when an
 * AlertTrigger is fired for the alert it is responsible for recovering. It is
 * important to isolate this logic b/c a recovery alert shouldn't fire if
 * condition met events are older than alert fired event (which might happen
 * during a backfill).
 * @author jhickey
 * 
 */
public class RecoveryConditionEvaluator
    extends MultiConditionEvaluator
{

    private final Integer alertTriggerId;

    private TriggerFiredEvent lastAlertFired;

    private final Object monitor = new Object();
    
    private final Integer recoveringFromAlertId;
    
    /**
     * 
     * @param alertDefinitionId The ID of the recovery alert definition
     * @param alertTriggerId The ID of the trigger listening for alert from
     *        which we are to recover
     * @param alertConditions The conditions of the recovery alert - excluding
     *        the "AlertFired" condition. This should just be the conditions
     *        that indicate recovery
     * @param executionStrategy The {@link ExecutionStrategy} to use for firing
     *        AlertConditionsSatisfied events
     */
    public RecoveryConditionEvaluator(Integer alertDefinitionId,
                                      Integer alertTriggerId,
                                      Integer recoveringFromAlertDefId,
                                      Collection alertConditions,
                                      ExecutionStrategy executionStrategy)
    {
        super(alertDefinitionId, alertConditions, 0, executionStrategy);
        this.alertTriggerId = alertTriggerId;
        this.recoveringFromAlertId = recoveringFromAlertDefId;
    }

    /**
     * 
     * @param alertDefinitionId The ID of the recovery alert definition
     * @param alertTriggerId The ID of the trigger listening for alert from
     *        which we are to recover
     * @param alertConditions The conditions of the recovery alert - excluding
     *        the "AlertFired" condition. This should just be the conditions
     *        that indicate recovery
     * @param executionStrategy The {@link ExecutionStrategy} to use for firing
     *        AlertConditionsSatisfied events
     * @param events The collection in which to store trigger events
     */
    public RecoveryConditionEvaluator(Integer alertDefinitionId,
                                      Integer alertTriggerId,
                                      Integer recoveringFromAlertDefId,
                                      Collection alertConditions,
                                      ExecutionStrategy executionStrategy,
                                      Map events)
    {
        super(alertDefinitionId, alertConditions, 0, executionStrategy, events);
        this.alertTriggerId = alertTriggerId;
        this.recoveringFromAlertId = recoveringFromAlertDefId;
    }

    private void evaluateRecoveryConditions(Collection fulfilled) {
        if (fulfilled != null && lastAlertFired != null) {
            for (Iterator iterator = fulfilled.iterator(); iterator.hasNext();) {
                TriggerFiredEvent event = (TriggerFiredEvent) iterator.next();
                // All recovery conditions should be newer than the last alert
                // fired
                if (event.getTimestamp() < this.lastAlertFired.getTimestamp()) {
                    return;
                }
            }
            fulfilled.add(this.lastAlertFired);
            fireConditionsSatisfied(fulfilled);
            this.lastAlertFired = null;
        }
    }

    Integer getAlertTriggerId() {
        return alertTriggerId;
    }
    
    Integer getRecoveringFromAlertDefinitionId() {
        return recoveringFromAlertId;
    }

    /**
     * Anything returned from getState() gets persisted to file on server shutdown,
     * which we don't want to do anymore for the recovery alerts.
     */
    public Serializable getState() {
        return null;
    }
    
    public TriggerFiredEvent getLastAlertFired() {
        return this.lastAlertFired;
    }

    public void initialize(Serializable initialState) {        
        if (initialState != null && initialState instanceof AbstractEvent) {
            this.lastAlertFired = new TriggerFiredEvent(alertTriggerId, 
                                                        (AbstractEvent) initialState);
        }        
    }

    public void triggerFired(TriggerFiredEvent event) {
        synchronized (monitor) {
            if (event.getInstanceId().equals(alertTriggerId)) {
                if (this.lastAlertFired == null || (this.lastAlertFired.getTimestamp() < event.getTimestamp())) {
                    this.lastAlertFired = event;
                }
                evaluateRecoveryConditions(getFulfillingConditions());
            } else {
                Collection recoveryConditions = evaluate(event);
                evaluateRecoveryConditions(recoveryConditions);
            }
        }
    }

    public void triggerNotFired(TriggerNotFiredEvent event) {
        super.triggerNotFired(event);
    }

}
