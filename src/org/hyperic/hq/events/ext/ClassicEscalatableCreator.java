package org.hyperic.hq.events.ext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertCondition;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;
import org.hyperic.hq.events.server.session.ClassicEscalatable;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertValue;

/**
 * This class has the knowledge to create an {@link Escalatable} object
 * based on a {@link TriggerFiredEvent} if the escalation subsytem deems
 * it necessary.
 */
class ClassicEscalatableCreator 
    implements EscalatableCreator
{
    private static final Log _log =
        LogFactory.getLog(ClassicEscalatableCreator.class);

    private AlertDefinition   _def;
    private TriggerFiredEvent _event;
    
    ClassicEscalatableCreator(AlertDefinition def, TriggerFiredEvent event) { 
        _def   = def;
        _event = event;
    }
    
    /**
     * In the classic escalatable architecture, we still need to support the
     * execution of the actions defined for the regular alert defintion 
     * (in addition to executing the actions specified by the escalation).
     * 
     * Here, we generate the alert and also execute the old-skool actions.
     * May or may not be the right place to do that.
     */
    public Escalatable createEscalatable() {
        AlertManagerLocal alertMan = AlertManagerEJBImpl.getOne();

        // Start the Alert object
        AlertValue aVal = new AlertValue();
        aVal.setAlertDefId(_def.getId());

        // Create time is the same as the fired event
        aVal.setCtime(_event.getTimestamp());

        // Now create the alert
        aVal = alertMan.createAlert(aVal);

        // Create the trigger event map
        Map trigMap = new HashMap();
        TriggerFiredEvent[] events = _event.getRootEvents();
        for (int i = 0; i < events.length; i++) {
            trigMap.put(events[i].getInstanceId(), events[i]);
        }
    
        // Create a alert condition logs for every condition
        Collection conds = _def.getConditions();
        for (Iterator i = conds.iterator(); i.hasNext(); ) {
            AlertCondition cond = (AlertCondition)i.next();
            AlertConditionLogValue clog = new AlertConditionLogValue();
            Integer trigId = cond.getTrigger().getId();
            
            clog.setCondition(cond.getAlertConditionValue());
            if (trigMap.containsKey(trigId)) {
                clog.setValue(trigMap.get(trigId).toString());
            } 
            aVal.addConditionLog(clog);
        }
    
        // Update the alert
        // get alert pojo so retrieve array of AlertCondtionLogs
        Alert alert = alertMan.updateAlert(aVal);
        
        // Regardless of whether or not the actions succeed, we will send an
        // AlertFiredEvent
        Messenger sender = new Messenger();
        sender.publishMessage(EventConstants.EVENTS_TOPIC,
                              new AlertFiredEvent(_event, aVal.getId(),
                                                  _def));

        String shortReason = alertMan.getShortReason(alert);
        String longReason  = alertMan.getLongReason(alert);
        
        Collection actions = _def.getActions();
        // Iterate through the actions
        for (Iterator i = actions.iterator(); i.hasNext(); ) {
            Action act = (Action) i.next();

            try {
                act.executeAction(alert, shortReason, longReason);
            } catch(ActionExecuteException e) {
                _log.warn("Error executing action [" + act + "]", e);
            }
        }
        
        return new ClassicEscalatable(alert, shortReason, longReason);
    }
}
