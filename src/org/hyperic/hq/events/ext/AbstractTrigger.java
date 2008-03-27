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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.shared.EventObjectDeserializer;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

/** 
 * Abstract class that defines a trigger, which can fire actions
 */
public abstract class AbstractTrigger 
    implements TriggerInterface, RegisterableTriggerInterface {
    
    protected final Log log = LogFactory.getLog(AbstractTrigger.class);
    
    private final Object lock = new Object();
            
    private RegisteredTriggerValue triggerValue = new RegisteredTriggerValue();

    private TriggerFireStrategy fireStrategy;
    
    private TriggerFiredEvent lastFiringEvent;
    
    
    public AbstractTrigger() {        
        // set the default value
        triggerValue.setId(new Integer(-1));
        
        setTriggerFireStrategy(new DefaultTriggerFireStrategy(this));
    }
        
    protected final void publishEvent(AbstractEvent event) {
        Messenger.enqueueMessage(event);
    }

    protected final void notFired() {
        publishEvent(new TriggerNotFiredEvent(getId()));
    }
     
    protected final void fireActions(TriggerFiredEvent event) 
        throws ActionExecuteException, AlertCreateException {
        
        TriggerFireStrategy fireStrategy;
        
        synchronized (lock) {
            lastFiringEvent = event;
            fireStrategy = getTriggerFireStrategy();
        }
        
        fireStrategy.fireActions(event);
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
    
    /**
     * @return The last firing event or <code>null</code> if this trigger has 
     *          not fired actions yet.        
     */
    public TriggerFiredEvent getLastFiringEvent() {
        synchronized (lock) {
            return lastFiringEvent;            
        }
    }
    
    /**
     * @return The trigger fire strategy.
     */
    public TriggerFireStrategy getTriggerFireStrategy() {
        synchronized (lock) {
            return fireStrategy;            
        }
    }
    
    /**
     * Set the trigger fire strategy. Doing so will cause this strategy to be 
     * invoked on firing actions instead of the 
     * {@link DefaultTriggerFireStrategy default strategy}.
     * 
     * @param strategy The trigger fire strategy.
     * @see #fireActions(TriggerFiredEvent)
     */
    public void setTriggerFireStrategy(TriggerFireStrategy strategy) {
        synchronized (lock) {
            fireStrategy = strategy;            
        }
    }    
    
}
