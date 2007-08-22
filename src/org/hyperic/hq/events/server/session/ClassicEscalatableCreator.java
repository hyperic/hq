/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.events.server.session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;

/**
 * This class has the knowledge to create an {@link Escalatable} object
 * based on a {@link TriggerFiredEvent} if the escalation subsytem deems
 * it necessary.
 */
public class ClassicEscalatableCreator 
    implements EscalatableCreator
{
    private static final Log _log =
        LogFactory.getLog(ClassicEscalatableCreator.class);
    
    private AlertDefinition   _def;
    private TriggerFiredEvent _event;
    
    public ClassicEscalatableCreator(AlertDefinition def,
                                     TriggerFiredEvent event) { 
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
        // Create the trigger event map
        Map trigMap = new HashMap();
        TriggerFiredEvent[] events = _event.getRootEvents();
        for (int i = 0; i < events.length; i++) {
            trigMap.put(events[i].getInstanceId(), events[i]);
        }
    
        AlertManagerLocal alertMan = AlertManagerEJBImpl.getOne();

        // Now create the alert
        Alert alert = alertMan.createAlert(_def, _event.getTimestamp());

        // Create a alert condition logs for every condition that triggered the alert
        Collection conds = _def.getConditions();
        for (Iterator i = conds.iterator(); i.hasNext();) {
            AlertCondition cond = (AlertCondition) i.next();
            
            if (shouldCreateConditionLogFor(cond, trigMap)) {
                AlertConditionLogValue clog = new AlertConditionLogValue();
                clog.setCondition(cond.getAlertConditionValue());
                
                Integer trigId = cond.getTrigger().getId();
                clog.setValue(trigMap.get(trigId).toString());
                
                alert.createConditionLog(clog.getValue(), cond);
            }
        }
    
        // Regardless of whether or not the actions succeed, we will send an
        // AlertFiredEvent
        Messenger sender = new Messenger();
        sender.publishMessage(EventConstants.EVENTS_TOPIC,
                              new AlertFiredEvent(_event, alert.getId(), _def));

        String shortReason = alertMan.getShortReason(alert);
        String longReason  = alertMan.getLongReason(alert);
        
        Collection actions = _def.getActions();
        // Iterate through the actions
        for (Iterator i = actions.iterator(); i.hasNext(); ) {
            Action act = (Action) i.next();

            try {
                ActionExecutionInfo execInfo = 
                    new ActionExecutionInfo(shortReason, longReason,
                                            Collections.EMPTY_LIST);
                                            
                String detail = act.executeAction(alert, execInfo);
                
                alertMan.logActionDetail(alert, act, detail, null);
            } catch(Exception e) {
                // For any exception, just log it.  We can't afford not
                // letting the other actions go un-processed.
                _log.warn("Error executing action [" + act + "]", e);
            }
        }
        
        return createEscalatable(alert, shortReason, longReason);
    }
    
    public static Escalatable createEscalatable(Alert alert, String shortReason,
                                                String longReason) 
    {
        return new ClassicEscalatable(alert, shortReason, longReason); 
    }
    
    
    private boolean shouldCreateConditionLogFor(AlertCondition cond, Map triggerMap) {
        if (cond.getType() == EventConstants.TYPE_ALERT) {
            // Don't create a log for recovery alerts, so that we don't
            // get the multi-condition effect in the logs
            return false;
        }
        
        Integer trigId = cond.getTrigger().getId();
        
        return triggerMap.containsKey(trigId);
    }    
    
}
