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

package org.hyperic.hq.events.ext;

import java.util.HashMap;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.EscalationMediator;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertConditionLog;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionManagerUtil;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.events.shared.TriggerTrackerLocal;
import org.hyperic.hq.events.shared.TriggerTrackerUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/** Abstract class that defines a trigger, which can fire actions
 */
public abstract class AbstractTrigger implements TriggerInterface {
    private final Log log = LogFactory.getLog(AbstractTrigger.class);
    
    private static boolean systemReady = false;

    private static MBeanServer mServer;
    private static ObjectName readyManName;

    private RegisteredTriggerValue triggerValue = new RegisteredTriggerValue();
    
	public AbstractTrigger() {
		super();
        
        // set the default value
        triggerValue.setId(new Integer(-1));
	}

    private boolean isSystemReady() {
        if (!systemReady) {
            try {
                if (mServer == null) {
                    mServer = (MBeanServer) MBeanServerFactory
                        .findMBeanServer(null).iterator().next();

                    readyManName =
                        new ObjectName("hyperic.jmx:service=NotReadyManager");
                }

                Boolean mbeanReady =
                    (Boolean) mServer.getAttribute(readyManName, "Ready");
                
                systemReady = mbeanReady.booleanValue();
            } catch (AttributeNotFoundException e) {
                // This would be a programmatic error, assume system is up
                systemReady = true;
            } catch (ReflectionException e) {
                // Unable to reflect and get the value, assume system is up
                systemReady = true;
            } catch (MalformedObjectNameException e) {
                // This would be a programmatic error, assume system is up
                systemReady = true;
            } catch (InstanceNotFoundException e) {
                // MBean not deployed yet
            } catch (MBeanException e) {
                // MBean not deployed yet
            }
        }
        
        return systemReady;
    }
    
    protected void publishEvent(AbstractEvent event) {
        Messenger sender = new Messenger();
        sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
    }
    
    protected void notFired() {
        publishEvent(new TriggerNotFiredEvent(getId()));
    }
    
    /** The utility method which fires the actions of a trigger
     */    
    protected void fireActions(TriggerFiredEvent event)
        throws ActionExecuteException, AlertCreateException {
            
        // If the system is not ready, do nothing
        if (!isSystemReady())
            return;
            
        ActionValue[] actions;
        AlertValue alert = new AlertValue();
        
        // No matter what, send a message to let people know that this trigger
        // has fired
        publishEvent(event);

        AlertDefinitionManagerLocal aman;
        AlertManagerLocal alman;
        AlertDefinition adBasic;
        try {
            aman = AlertDefinitionManagerUtil.getLocalHome().create();
            alman = AlertManagerUtil.getLocalHome().create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }

        try {
            // See if the alert def is actually enabled and if it's our job to
            // fire the actions
            Integer adId = aman.getIdFromTrigger(getId());
            if (adId == null)
                return;
            
            adBasic = aman.getByIdNoCheck(adId);
            if (adBasic.isEnabled()) {
                if (log.isDebugEnabled())
                    log.debug("Trigger ID " + getId() +
                              " causing alert definition ID " + adId +
                              " to fire");
            }
            else
                return;

            // See if we need to supress this trigger        
            if (adBasic.getFrequencyType() == EventConstants.FREQ_NO_DUP) {
                TriggerTrackerLocal tracker;
                try {
                    tracker = TriggerTrackerUtil.getLocalHome().create();
                } catch (NamingException e) {
                    throw new SystemException(e);
                } catch (CreateException e) {
                    throw new SystemException(e);
                }

                boolean fire = tracker.fire(getId(), getFrequency());
                // The TriggerTracker decided if we are supposed to fire
                if (!fire)
                    return;
            }

            if (adBasic.getFrequencyType() == EventConstants.FREQ_ONCE ||
                    adBasic.isWillRecover()) {
            	// Disable the alert definition now that we've fired
                aman.updateAlertDefinitionsEnable(AuthzSubjectManagerUtil
                    .getLocalHome().create().getOverlord(),
                    new Integer[]{ adBasic.getId() }, false);
            }
            
            if (log.isDebugEnabled())
                log.debug("Firing trigger " + getId() + " actions");

            // Start the Alert object
            alert.setAlertDefId(adBasic.getId());

            // Create time is the same as the fired event
            alert.setCtime(event.getTimestamp());

            // Now create the alert
            alert = alman.createAlert(alert);

            // Create the trigger event map
            HashMap trigMap = new HashMap();
            TriggerFiredEvent[] tfes = event.getRootEvents();
            for (int i = 0; i < tfes.length; i++) {
                trigMap.put(tfes[i].getInstanceId(), tfes[i]);
            }
        
            // Create a alert condition logs for every condition
            AlertConditionValue[] conds = aman.getConditionsById(adId);
            for (int i = 0; i < conds.length; i++) {
                AlertConditionLogValue clog = new AlertConditionLogValue();
                clog.setCondition(conds[i]);
                if (trigMap.containsKey(conds[i].getTriggerId())) {
                    clog.setValue(trigMap.get(
                        conds[i].getTriggerId()).toString());
                } 
                alert.addConditionLog(clog);
            }
        
            // Store the alert
            alman.updateAlert(alert);
            actions = aman.getActionsById(adId);
        } catch (FinderException e) {
            throw new ActionExecuteException(
                "Alert Definition not found for trigger: " + getId());
        } catch (PermissionException e) {
            throw new ActionExecuteException(
                "Overlord does not have permission to disable definition");
        } catch (CreateException e) {
            throw new ActionExecuteException(
                "Cannot create AuthzSubjectManagerLocal");
        } catch (NamingException e) {
            throw new ActionExecuteException(
                "Cannot look up AuthzSubjectManagerLocal interface");
        }

        // Regardless of whether or not the actions succeed, we will send an
        // AlertFiredEvent
        publishEvent(new AlertFiredEvent(event, alert.getId(), adBasic));

        // get alert pojo so retrieve array of AlertCondtionLogs
        Alert alertpojo = alman.findAlertById(alert.getId());
        Escalation esc = alertpojo.getAlertDefinition().getEscalation();
        if (esc != null) {
            // invoke escalation chain
            EscalationMediator emed = EscalationMediator.getInstance();
            emed.startEscalation(esc.getId(), alert.getId());
        } else {
            AlertConditionLog[] logs =
                (AlertConditionLog[])alertpojo
                    .getConditionLog().toArray(new AlertConditionLog[0]);
            // Iterate through the actions
            for (int i = 0; i < actions.length; i++) {
                ActionValue aval = actions[i];

                try {
                    Class ac = Class.forName(aval.getClassname());
                    ActionInterface action = (ActionInterface) ac.newInstance();

                    // Initialize action
                    action.init(ConfigResponse.decode(action.getConfigSchema(),
                        aval.getConfig()));

                    String detail = action.execute(adBasic, logs, alert.getId());
                                   
                    AlertActionLogValue alog = new AlertActionLogValue();
                    alog.setActionId(aval.getId());
                    alog.setDetail(detail);
                
                    alert.addActionLog(alog);
                } catch (ClassNotFoundException e) {
                    // Can't execute if we can't lookup up the class
                    throw new ActionExecuteException(
                        "Action class not found for ID " + aval.getId(), e);
                } catch (InstantiationException e) {
                    // Can't execute if we can't instantiate the object
                    throw new ActionExecuteException(
                        "Cannot instantiate action for ID " + aval.getId(), e);
                } catch (InvalidActionDataException e) {
                    // Can't execute if we can't instantiate the object
                    throw new ActionExecuteException(
                        "Cannot initialize action for ID " + aval.getId(), e);
                } catch (IllegalAccessException e) {
                    // Can't execute if we can't access the class
                    throw new ActionExecuteException(
                        "Cannot access action for ID " + aval.getId(), e);
                } catch (EncodingException e) {
                    // Can't execute if we can't decode the config
                    throw new ActionExecuteException(
                        "Cannot decode action config for ID " + aval.getId(), e);
                } catch (InvalidOptionException e) {
                    // Can't execute if we can't decode the config
                    throw new ActionExecuteException(
                        "Action config contains invalid option for ID " +
                            aval.getId(), e);
                } catch (InvalidOptionValueException e) {
                    // Can't execute if we don't have good config, just log it
                    log.debug("Bad action config value for ID " + aval.getId(), e);
                }
            } // for
        } // else
            // Store the alert
        alman.updateAlert(alert);
    }
    
    public Integer getId() {
        if (triggerValue == null)
            return new Integer(0);

        return triggerValue.getId();
    }
    
    public long getFrequency() {
        if (triggerValue == null)
            return 0;

        return triggerValue.getFrequency();
    }
    
    public RegisteredTriggerValue getTriggerValue() {
        return triggerValue;
    }
    
    public void setTriggerValue(RegisteredTriggerValue tv) {
        triggerValue = tv;
    }
}
