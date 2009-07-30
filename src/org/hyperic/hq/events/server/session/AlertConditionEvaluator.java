package org.hyperic.hq.events.server.session;

import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;

public interface AlertConditionEvaluator {

    void triggerFired(TriggerFiredEvent event);

    void triggerNotFired(TriggerNotFiredEvent event);

}
