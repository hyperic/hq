package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.zevents.ZeventEnqueuer;

/**
 * Default implementation of {@link AlertConditionEvaluatorFactory}
 * @author jhickey
 * 
 */
public class AlertConditionEvaluatorFactoryImpl implements AlertConditionEvaluatorFactory {

    private final ZeventEnqueuer zeventEnqueuer;
    
    private final Log log = LogFactory.getLog(AlertConditionEvaluatorFactoryImpl.class);

    /**
     * 
     * @param zeventEnqueuer The {@link ZeventEnqueuer} to pass to created
     *        {@link AlertConditionEvaluator}s
     */
    public AlertConditionEvaluatorFactoryImpl(ZeventEnqueuer zeventEnqueuer)
    {
        this.zeventEnqueuer = zeventEnqueuer;
    }

    public AlertConditionEvaluator create(AlertDefinition alertDefinition) {
        // range doesn't get reset to 0 if switching from counter freq to
        // everytime. Pass in 0 if not counter freq just to be on the safe side
        long range = 0;
        if (alertDefinition.getFrequencyType() == EventConstants.FREQ_COUNTER) {
            range = alertDefinition.getRange() * 1000l;
        }
        AlertConditionEvaluator evaluator;
        if (alertDefinition.isRecoveryDefinition()) {
            Integer alertTriggerId = Integer.valueOf(0);
            Integer recoveringFromAlertDefId = Integer.valueOf(0);
            List conditions = new ArrayList();
            for (Iterator iter = alertDefinition.getConditions().iterator(); iter.hasNext();) {
                AlertCondition condition = (AlertCondition) iter.next();
                if (condition.getType() == EventConstants.TYPE_ALERT) {
                    alertTriggerId = condition.getTrigger().getId();
                    recoveringFromAlertDefId = Integer.valueOf(condition.getMeasurementId());
                } else {
                    conditions.add(condition);
                }
            }
            evaluator = new RecoveryConditionEvaluator(alertDefinition.getId(),
                                                       alertTriggerId, recoveringFromAlertDefId,
                                                       conditions,
                                                       createExecutionStrategy(alertDefinition));
        } else if (alertDefinition.getConditions().size() > 1) {
            evaluator = new MultiConditionEvaluator(alertDefinition.getId(),
                                                    alertDefinition.getConditions(),
                                                    range,
                                                    createExecutionStrategy(alertDefinition));
        } else {
            evaluator = new SingleConditionEvaluator(alertDefinition.getId(), createExecutionStrategy(alertDefinition));
        }
      
        return evaluator;
    }

    private ExecutionStrategy createExecutionStrategy(AlertDefinition alertDefinition) {
        ExecutionStrategy executionStrategy;
        if (alertDefinition.getFrequencyType() == EventConstants.FREQ_EVERYTIME ||
            alertDefinition.getFrequencyType() == EventConstants.FREQ_ONCE)
        {
            executionStrategy = new SingleAlertExecutionStrategy(zeventEnqueuer);
        } else if (alertDefinition.getFrequencyType() == EventConstants.FREQ_COUNTER) {
            executionStrategy = new CounterExecutionStrategy(alertDefinition.getCount(),
                                                             alertDefinition.getRange() * 1000l,
                                                             zeventEnqueuer);
        } else {
            log.warn("Encountered an alert with unsupported frequency type: " + alertDefinition.getFrequencyType() +
                     ".  This alert will be treated as frequency type everytime.");
            executionStrategy = new SingleAlertExecutionStrategy(zeventEnqueuer);
        }
        return executionStrategy;
    }
}
