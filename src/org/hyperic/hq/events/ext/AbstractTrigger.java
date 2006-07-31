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
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionBasicValue;
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

    private static MBeanServer mServer = null;
    private static ObjectName readyManName = null; 

    /** Holds value of property triggerValue.  set the default property 
     * 
	 */
    private RegisteredTriggerValue triggerValue = new RegisteredTriggerValue();
    
	/**
	 * Constructor for AbstractTrigger.
	 */
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
        this.publishEvent(new TriggerNotFiredEvent(getId()));
    }
    
    /** The utility method which fires the actions of a trigger
     * @param message the message to include in the action(s)
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if an action throws an exception during execution
     * @throws NamingException if EJB lookup fails
     * @throws CreateException if EJB creation fails
     * @return if the actions actually fired
     */    
    protected void fireActions(TriggerFiredEvent event)
        throws ActionExecuteException, AlertCreateException {
            
        // If the system is not ready, do nothing
        if (!this.isSystemReady())
            return;
            
        ActionValue[] actions;
        AlertValue alert = new AlertValue();
        
        // No matter what, send a message to let people know that this trigger
        // has fired
        this.publishEvent(event);

        AlertDefinitionManagerLocal aman;
        AlertManagerLocal alman;
        AlertDefinitionBasicValue adBasic;
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
            Integer adId = aman.getIdFromTrigger(this.getId());
            if (adId == null)
                return;
            
            adBasic = aman.getBasicById(adId);

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
                    adBasic.getWillRecover()) {
            	// Disable the alert definition now that we've fired
                aman.updateAlertDefinitionsEnable(
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
                "Alert Definition not found for trigger: " + this.getId());
        }

        // Regardless of whether or not the actions succeed, we will send an
        // AlertFiredEvent
        this.publishEvent(new AlertFiredEvent(event, alert.getId(), adBasic));
        
        // Iterate through the actions
        for (int i = 0; i < actions.length; i++) {
            ActionValue aval = actions[i];

            try {
                Class ac = Class.forName(aval.getClassname());
                ActionInterface action = (ActionInterface) ac.newInstance();

                // Initialize action
                action.init(ConfigResponse.decode(action.getConfigSchema(),
                                                  aval.getConfig()));

                String detail = action.execute(adBasic, event, alert.getId());
                                   
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
        }
        
        // Store the alert
        alman.updateAlert(alert);
    }   
    
    /** Getter for property id.
     * @return Value of property id.
     *
     */
    public Integer getId() {
        if (this.triggerValue == null)
            return new Integer(0);

        return this.triggerValue.getId();
    }
    
    /** Getter for property frequency.
     * @return Value of property frequency.
     *
     */
    public long getFrequency() {
        if (this.triggerValue == null)
            return 0;

        return this.triggerValue.getFrequency();
    }
    
    /** Getter for property triggerValue.
     * @return Value of property triggerValue.
     *
     */
    public RegisteredTriggerValue getTriggerValue() {
        return this.triggerValue;
    }
    
    /** Setter for property triggerValue.
     * @param triggerValue New value of property triggerValue.
     *
     */
    public void setTriggerValue(RegisteredTriggerValue triggerValue) {
        this.triggerValue = triggerValue;
    }
    
}
