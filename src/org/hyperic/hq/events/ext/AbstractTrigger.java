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
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationMediator;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.events.shared.TriggerTrackerLocal;
import org.hyperic.hq.events.shared.TriggerTrackerUtil;

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
            
        // No matter what, send a message to let people know that this trigger
        // has fired
        publishEvent(event);

        AlertDefinitionManagerLocal aman;
        AlertDefinition adBasic;
        try {
            aman = AlertDefinitionManagerUtil.getLocalHome().create();
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

            // Now start escalation
            EscalationMediator emed = EscalationMediator.getInstance();
            emed.startEscalation(adBasic.getId(), event);
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
