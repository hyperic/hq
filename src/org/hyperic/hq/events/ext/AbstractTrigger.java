/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.ejb.FinderException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFireActionsFailureHandler;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.ClassicEscalatableCreator;
import org.hyperic.hq.events.server.session.TriggerTrackerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.EventObjectDeserializer;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.events.shared.TriggerTrackerLocal;

/** Abstract class that defines a trigger, which can fire actions
 */
public abstract class AbstractTrigger 
    implements TriggerInterface, RegisterableTriggerInterface {
    
    private final Log log = LogFactory.getLog(AbstractTrigger.class);
    
    private final Log triggerFiredLog = 
        LogFactory.getLog(AbstractTrigger.class.getName()+".Fired");
    
    private final Object alertDefEnabledStatusLock = new Object();
    
    private final AlertDefinitionEnabledStatus uncommitedAlertDefEnabledStatus = 
                    new AlertDefinitionEnabledStatus(true);
    
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
    
    protected final void publishEvent(AbstractEvent event) {
        Messenger.enqueueMessage(event);
    }
    
    protected final void notFired() {
        publishEvent(new TriggerNotFiredEvent(getId()));
    }
    
    /** 
     * The utility method which fires the actions of a trigger. We assume 
     * that the last trigger operation before firing actions is to clear 
     * any trigger state. If clearing trigger state fails, then event 
     * processing must have been aborted already and the current transaction 
     * rolled back. This will guarantee that the trigger will be able to 
     * resume event processing and possible action firing the next time 
     * an interesting event is processed.
     */   
    protected final void fireActions(TriggerFiredEvent event) 
        throws ActionExecuteException, AlertCreateException {
        
        try {
            doFireActions(event);
        } finally {
            boolean addedTxnListener = false;
            
            try {
                // Try adding a transaction listener that will call any 
                // TriggerFireActionsFailureHandlers if the current 
                // transaction is rolled back.
                HQApp.getInstance()
                    .addTransactionListener(new TransactionListener() {

                    public void afterCommit(boolean success) {
                        if (!success) {
                            invokeTriggerFireActionsFailureHandlers();
                        }
                    }

                    public void beforeCommit() {
                    }
                    
                });
                
                addedTxnListener = true;
            } finally {
                // If the current transaction has been marked for rollback 
                // already, then adding the transaction listener will cause an 
                // exception.  If this occurs, then invoke the 
                // TriggerFireActionsFailureHandlers immediately.
                if (!addedTxnListener) {
                    invokeTriggerFireActionsFailureHandlers();
                }
            }             
        }
    }
    
    private void invokeTriggerFireActionsFailureHandlers() {        
        AlertDefinitionManagerLocal aman = 
            AlertDefinitionManagerEJBImpl.getOne();
        Integer adId = aman.getIdFromTrigger(getId());
            
        if (adId == null) 
            return;
        
        try {
            AlertDefinition alertDef = aman.getByIdNoCheck(adId);
            
            Collection actions = alertDef.getActions();
            
            for (Iterator iter = actions.iterator(); iter.hasNext();) {
                Action action = (Action) iter.next();
                
                ActionInterface actionInterface = action.getInitializedAction();
                
                if (actionInterface instanceof TriggerFireActionsFailureHandler)
                {
                    ((TriggerFireActionsFailureHandler) actionInterface)
                        .onFailure();
                }
            }
        } catch (FinderException e) {
            // If we can't find the alert definition then there's not 
            // much else we can do.
        }        
    }
     
    private void doFireActions(TriggerFiredEvent event)
        throws ActionExecuteException, AlertCreateException {
            
        // If the system is not ready, do nothing
        if (!isSystemReady())
            return;

        AlertDefinitionManagerLocal aman =
            AlertDefinitionManagerEJBImpl.getOne();

        if (!aman.alertsAllowed()) {
            log.debug("Alert not firing because they are not allowed");
            return;
        }
        
        // No matter what, send a message to let people know that this trigger
        // has fired
        publishEvent(event);

        try {
            Integer adId = aman.getIdFromTrigger(getId());
            if (adId == null)
                return;
            
            AlertDefinition alertDef = null;
            
            // HQ-902: Retrieving the alert def and checking if the actions 
            // should fire must be guarded by a mutex.
            synchronized (alertDefEnabledStatusLock) {
                // Check cached alert def status
                if (!uncommitedAlertDefEnabledStatus.isAlertDefinitionEnabled())
                    return;
                
                // Check persisted alert def status
                if (!shouldTriggerAlert(aman, adId))
                    return;
        
                alertDef = aman.getByIdNoCheck(adId);
                
                // See if the alert def is actually enabled and if it's our job
                // to fire the actions
                if (!shouldFireActions(aman, alertDef))
                    return;
            }
            
            if (triggerFiredLog.isDebugEnabled()) {
                triggerFiredLog.debug("Firing actions for trigger with id=" +
                                      getId() + "; alert def [" +
                                      alertDef.getName() + "] with id=" +
                                      alertDef.getId()+"; triggering event ["+
                                      event+"], event time="+new Date(event.getTimestamp()));    
            }
            
            EscalatableCreator creator = 
                new ClassicEscalatableCreator(alertDef, event);
            
            // Now start escalation
            if (alertDef.getEscalation() != null) {
                EscalationManagerEJBImpl.getOne().startEscalation(alertDef,
                                                                  creator); 
            } else {
                creator.createEscalatable();
            }

        } catch (FinderException e) {
            throw new ActionExecuteException(
                "Alert Definition not found for trigger: " + getId());
        } catch (PermissionException e) {
            throw new ActionExecuteException(
                "Overlord does not have permission to disable definition");
        }
    }
    
    private boolean shouldTriggerAlert(AlertDefinitionManagerLocal adman,
                                       Integer id) {
        Object[] flags = adman.getEnabledAndTriggerId(id);
        
        // Check stored enabled flag as well as don't fire if it's not up to us
        // to act
        return flags[0].equals(Boolean.TRUE) && flags[1].equals(getId());
    }

    private boolean shouldFireActions(AlertDefinitionManagerLocal aman, 
                                      AlertDefinition alertDef) 
        throws PermissionException {
        
        if (log.isDebugEnabled())
            log.debug("Trigger id " + getId() +
                      " causing alert definition id " + alertDef.getId() + 
                      " to fire");

        // See if we need to suppress this trigger        
        if (alertDef.getFrequencyType() == EventConstants.FREQ_NO_DUP) {
            TriggerTrackerLocal tracker = TriggerTrackerEJBImpl.getOne();                

            boolean fire = tracker.fire(getId(), getFrequency());
            // The TriggerTracker decided if we are supposed to fire
            if (!fire)
                return false;
        }

        if (alertDef.getFrequencyType() == EventConstants.FREQ_ONCE ||
                alertDef.isWillRecover()) {
            // Disable the alert definition now that we've fired
            boolean succeeded = false;
            
            try {
                succeeded = aman.updateAlertDefinitionInternalEnable(
                          AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo(),
                          alertDef, 
                          false);                
            } finally {
                if (succeeded) {
                    setUncommitedAlertDefEnabledStatusToDisabled();                    
                }
            }
        }
        
        return true;        

    }

    private void setUncommitedAlertDefEnabledStatusToDisabled() {
        boolean addedTxnListener = false;
        
        try {
            uncommitedAlertDefEnabledStatus.flipEnabledStatus();
            
            HQApp.getInstance().addTransactionListener(new TransactionListener()
            {
                public void afterCommit(boolean success) {
                    uncommitedAlertDefEnabledStatus.resetEnabledStatus();
                }

                public void beforeCommit() {
                }
                
            });
            
            addedTxnListener = true;
        } finally {
            // If for any reason we can't add the transaction listener, reset
            // the cached alert definition enabled status immediately so the
            // trigger will continue firing after recovering from this failure
            // case.
            if (!addedTxnListener) {
                uncommitedAlertDefEnabledStatus.resetEnabledStatus();
            }
        }

    }
    
    /**
     * Deserialize an event, providing optional recovery from stream corruption. 
     * The stream may become corrupted during upgrade scenarios since we started 
     * providing serialization version control on events only in HQEE 3.1.1 
     * (Refer to ticket HQ-824). In this case, we would want to clear out the 
     * old events and start fresh.
     * 
     * @param deser The event object deserializer.
     * @param recoverFromCorruption <code>true</code> to delete all events 
     *                              associated with this trigger if the event 
     *                              stream is corrupted; <code>false</code> to 
     *                              ignore the failure, only throwing the 
     *                              exception.
     * @return The deserialized event.
     * @throws IOException if the event stream is corrupted.
     * @throws ClassNotFoundException if the event stream is corrupted.
     */
    protected final AbstractEvent deserializeEvent(EventObjectDeserializer deser, 
                                                   boolean recoverFromCorruption)
        throws IOException, ClassNotFoundException {
                
        boolean isStreamCorrupted = false;    
        AbstractEvent event = null;
        
        try {
            event = deser.deserializeEventObject();                            
        } catch (IOException e) {
            isStreamCorrupted = true;
            throw e;
        } catch (ClassNotFoundException e) {
            isStreamCorrupted = true;
            throw e;
        } finally {
            if (isStreamCorrupted && recoverFromCorruption) {
                log.info("Attempting to recover from event stream corruption " +
                		"by deleting all events associated with trigger id=" +
                		getId());
                
                try {
                    EventTrackerLocal eTracker =
                        EventTrackerUtil.getLocalHome().create();
                    eTracker.deleteReference(getId());
                    log.info("Recovery succeeded for trigger id="+getId());
                } catch (Exception e) {
                    log.info("Recovery failed for trigger id="+getId()+" : "+e);
                }
            }
        }
                    
        return event;
    }    
    
    /**
     * @see org.hyperic.hq.events.TriggerInterface#getId()
     */
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
