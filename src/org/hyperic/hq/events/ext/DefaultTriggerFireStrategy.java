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

import java.util.Date;

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
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.ClassicEscalatableCreator;
import org.hyperic.hq.events.server.session.TriggerTrackerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.TriggerTrackerLocal;

/**
 * The default trigger fire strategy.
 */
public class DefaultTriggerFireStrategy implements TriggerFireStrategy {
    
    private static MBeanServer mServer;
    private static ObjectName readyManName;    
    private static boolean systemReady = false;
    
    private static final Log _triggerFiredLog = 
        LogFactory.getLog(AbstractTrigger.class.getName()+".Fired");
    
    private final Object _alertDefEnabledStatusLock = new Object();
    
    private final AlertDefinitionEnabledStatus _uncommitedAlertDefEnabledStatus = 
                                        new AlertDefinitionEnabledStatus(true);
    
    private final AbstractTrigger _trigger;
    
    private final Log _log;
    
    /**
     * Creates an instance.
     *
     * @param trigger The trigger that will invoke this firing strategy.
     */
    public DefaultTriggerFireStrategy(AbstractTrigger trigger) {
        _trigger = trigger;
        _log = trigger.log;
    }

    /**
     * @see org.hyperic.hq.events.ext.TriggerFireStrategy#fireActions(org.hyperic.hq.events.TriggerFiredEvent)
     */
    public void fireActions(TriggerFiredEvent event) throws ActionExecuteException, AlertCreateException {
        
        // If the system is not ready, do nothing
        if (!isSystemReady())
            return;

        AlertDefinitionManagerLocal aman =
            AlertDefinitionManagerEJBImpl.getOne();

        if (!aman.alertsAllowed()) {
            _log.debug("Alert not firing because they are not allowed");
            return;
        }

        // No matter what, send a message to let people know that this trigger
        // has fired
        _trigger.publishEvent(event);

        try {
            Integer adId = aman.getIdFromTrigger(_trigger.getId());
            if (adId == null)
                return;

            AlertDefinition alertDef = null;

            // HQ-902: Retrieving the alert def and checking if the actions 
            // should fire must be guarded by a mutex.
            synchronized (_alertDefEnabledStatusLock) {
                // Check cached alert def status
                if (!_uncommitedAlertDefEnabledStatus.isAlertDefinitionEnabled())
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

            if (_triggerFiredLog.isDebugEnabled()) {
                _triggerFiredLog.debug("Firing actions for trigger with id=" +
                        _trigger.getId() + "; alert def [" +
                        alertDef.getName() + "] with id=" +
                        alertDef.getId()+"; triggering event ["+
                        event+"], event time="+new Date(event.getTimestamp()));    
            }

            EscalatableCreator creator = 
                new ClassicEscalatableCreator(alertDef, event);

            // Now start escalation
            if (alertDef.getEscalation() != null) {
                EscalationManagerEJBImpl.getOne()
                                    .startEscalation(alertDef, creator); 
            } else {
                creator.createEscalatable();
            }

        } catch (FinderException e) {
            throw new ActionExecuteException(
                    "Alert Definition not found for trigger: " + _trigger.getId());
        } catch (PermissionException e) {
            throw new ActionExecuteException(
                    "Overlord does not have permission to disable definition");
        }        
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
    
    private boolean shouldTriggerAlert(AlertDefinitionManagerLocal adman, 
                                       Integer id) {
        Object[] flags = adman.getEnabledAndTriggerId(id);

        // Check stored enabled flag as well as don't fire if it's not up to us 
        // to act
        return flags[0].equals(Boolean.TRUE) && flags[1].equals(_trigger.getId());
    }

    private boolean shouldFireActions(AlertDefinitionManagerLocal aman, 
                                      AlertDefinition alertDef) 
        throws PermissionException {

        if (_log.isDebugEnabled())
            _log.debug("Trigger id " + _trigger.getId() +
                    " causing alert definition id " + alertDef.getId() + 
                    " to fire");

        // See if we need to suppress this trigger        
        if (alertDef.getFrequencyType() == EventConstants.FREQ_NO_DUP) {
            TriggerTrackerLocal tracker = TriggerTrackerEJBImpl.getOne();                

            boolean fire = tracker.fire(_trigger.getId(), _trigger.getFrequency());
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
            _uncommitedAlertDefEnabledStatus.flipEnabledStatus();
            
            HQApp.getInstance().addTransactionListener(new TransactionListener()
            {
                public void afterCommit(boolean success) {
                    _uncommitedAlertDefEnabledStatus.resetEnabledStatus();
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
                _uncommitedAlertDefEnabledStatus.resetEnabledStatus();
            }
        }

    }    

}
