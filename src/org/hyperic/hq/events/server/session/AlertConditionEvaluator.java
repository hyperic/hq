package org.hyperic.hq.events.server.session;

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
