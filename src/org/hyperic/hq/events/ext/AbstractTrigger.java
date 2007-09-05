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

import java.io.IOException;
import java.io.ObjectInputStream;

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
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.ClassicEscalatableCreator;
import org.hyperic.hq.events.server.session.TriggerTrackerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.events.shared.TriggerTrackerLocal;

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
    
    protected final void publishEvent(AbstractEvent event) {
        Messenger.enqueueMessage(event);
    }
    
    protected final void notFired() {
        publishEvent(new TriggerNotFiredEvent(getId()));
    }
    
    /** The utility method which fires the actions of a trigger
     */    
    protected final void fireActions(TriggerFiredEvent event)
        throws ActionExecuteException, AlertCreateException {
            
        // If the system is not ready, do nothing
        if (!isSystemReady())
            return;
            
        // No matter what, send a message to let people know that this trigger
        // has fired
        publishEvent(event);

        AlertDefinitionManagerLocal aman;
        AlertDefinition alertDef;
        
        aman = AlertDefinitionManagerEJBImpl.getOne();

        try {
            // See if the alert def is actually enabled and if it's our job to
            // fire the actions
            Integer adId = aman.getIdFromTrigger(getId());
            if (adId == null)
                return;
            
            alertDef = aman.getByIdNoCheck(adId);
            if (!alertDef.isEnabled())
                return;
            
            // Don't fire if it's up to us
            if (!alertDef.getActOnTrigger().getId().equals(getId()))
                return;
            
            if (log.isDebugEnabled())
                log.debug("Trigger ID " + getId() +
                          " causing alert definition ID " + adId + " to fire");

            // See if we need to supress this trigger        
            if (alertDef.getFrequencyType() == EventConstants.FREQ_NO_DUP) {
                TriggerTrackerLocal tracker = TriggerTrackerEJBImpl.getOne();                

                boolean fire = tracker.fire(getId(), getFrequency());
                // The TriggerTracker decided if we are supposed to fire
                if (!fire)
                    return;
            }

            if (alertDef.getFrequencyType() == EventConstants.FREQ_ONCE ||
                    alertDef.isWillRecover()) {
                // Disable the alert definition now that we've fired
                aman.updateAlertDefinitionInternalEnable(
                    AuthzSubjectManagerEJBImpl.getOne().getOverlord(),
                    alertDef, false);
            }
            
            if (log.isDebugEnabled())
                log.debug("Firing trigger " + getId() + " actions");

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
    
    /**
     * Deserialize an event from the input stream, providing optional recovery 
     * from stream corruption. The stream may become corrupted during upgrade 
     * scenarios since we started providing serialization version control on 
     * events only in HQEE 3.1.1 (Refer to ticket HQ-824). In this case, we 
     * would want to clear out the old events and start fresh.
     * 
     * @param is The input stream.
     * @param recoverFromCorruption <code>true</code> to delete all events 
     *                              associated with this trigger if the event 
     *                              stream is corrupted; <code>false</code> to 
     *                              ignore the failure, only throwing the 
     *                              exception.
     * @return The deserialized event.
     * @throws IOException if the event stream is corrupted.
     * @throws ClassNotFoundException if the event stream is corrupted.
     */
    protected final AbstractEvent deserializeEventFromStream(ObjectInputStream is, 
                                                    boolean recoverFromCorruption)
        throws IOException, ClassNotFoundException {
                
        boolean isStreamCorrupted = false;    
        AbstractEvent event = null;
        
        try {
            event = (AbstractEvent) is.readObject();                            
        } catch (IOException e) {
            isStreamCorrupted = true;
            throw e;
        } catch (ClassNotFoundException e) {
            isStreamCorrupted = true;
            throw e;
        } finally {
            if (isStreamCorrupted && recoverFromCorruption) {
                log.info("Attempting to recover from event stream corruption by " +
                        "deleting all events associated with trigger id="+getId());
                
                try {
                    EventTrackerLocal eTracker = EventTrackerUtil.getLocalHome().create();
                    eTracker.deleteReference(getId());
                    log.info("Recovery succeeded for trigger id="+getId());
                } catch (Exception e) {
                    log.info("Recovery failed for trigger id="+getId()+" : "+e);
                }
            }
        }
                    
        return event;
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
