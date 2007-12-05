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

package org.hyperic.hq.bizapp.server.trigger.frequency;

import java.util.LinkedList;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.shared.EventObjectDeserializer;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.LongConfigOption;

/** 
 * The CounterTrigger is a simple trigger which fires when a certain 
 * number of events have occurred within a given time window.
 */

public class ConstantDurationTrigger extends AbstractTrigger
    implements FrequencyTriggerInterface {
    
    private Object  lock = new Object();
    private Integer triggerId;
    private long    timeRange;
    
    public ConstantDurationTrigger() {}

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        ConfigSchema res = new ConfigSchema();
        IntegerConfigOption tid;
        LongConfigOption range;

        tid = new IntegerConfigOption(CFG_TRIGGER_ID,
                                      "Trigger ID emitting events to count",
                                      null);
        tid.setMinValue(0);

        range = new LongConfigOption(CFG_TIME_RANGE,
                                     "Duration of event (in seconds)",
                                     new Long(300));
        range.setMinValue(1);

        res.addOption(tid);
        res.addOption(range);
        return res;
    }

    /**
     * @see org.hyperic.hq.bizapp.server.trigger.frequency.FrequencyTriggerInterface#getConfigResponse(Integer, long, int)
     */
    public ConfigResponse getConfigResponse(Integer tid, long range, long count)
        throws InvalidOptionException, InvalidOptionValueException {
        if (range < 1)
            throw new InvalidOptionValueException(
                "Duration must be greater than 0 seconds");

        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_TRIGGER_ID, tid.toString());
        resp.setValue(CFG_TIME_RANGE, "" + range);
        return resp;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#init(org.hyperic.hq.events.shared.RegisteredTriggerValue)
     */
    public void init(RegisteredTriggerValue tval)
        throws InvalidTriggerDataException {
        // Set the trigger value
        setTriggerValue(tval);

        try {
            ConfigResponse triggerData =
                ConfigResponse.decode(getConfigSchema(),
                                      tval.getConfig());

            triggerId = 
                Integer.valueOf(triggerData.getValue(CFG_TRIGGER_ID));
            timeRange =
                Long.parseLong(triggerData.getValue(CFG_TIME_RANGE)) * 1000;
        } catch(InvalidOptionException exc){
            throw new InvalidTriggerDataException(exc);
        } catch(InvalidOptionValueException exc){
            throw new InvalidTriggerDataException(exc);
        } catch(EncodingException exc){
            throw new InvalidTriggerDataException(exc);
        } 
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes() {
        return new Class[] { TriggerFiredEvent.class,
                             TriggerNotFiredEvent.class };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c) {
        // Same set for both fired and not fired
        return new Integer[] { triggerId };
    }

    /** 
     * Process an event from the dispatcher.
     * @param event the Event to process
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if an action throws an exception
     */
    public void processEvent(AbstractEvent event)
        throws EventTypeException, ActionExecuteException {

        // If we didn't fulfill the condition, then don't fire
        if(!(event instanceof TriggerFiredEvent ||
             event instanceof TriggerNotFiredEvent))
            throw new EventTypeException(
                "Invalid event type passed, expected TriggerFiredEvent " +
                "or TriggerNotFiredEvent");

        AbstractEvent tfe = (AbstractEvent) event;
        if(!tfe.getInstanceId().equals(triggerId))
            throw new EventTypeException("Invalid instance ID passed (" +
                                         tfe.getInstanceId() + ") expected " +
                                         triggerId);

        TriggerFiredEvent myEvent = null;
        synchronized (lock) {
            EventTrackerLocal eTracker;
            
            try {
                eTracker = EventTrackerUtil.getLocalHome().create();
            } catch(NamingException exc){
                return; // No fire since we can't track the events
            } catch(CreateException exc){
                return; // No fire since we can't track the events
            }

            try {
                if (tfe instanceof TriggerNotFiredEvent) {
                    eTracker.deleteReference(getId());
                }
                else {
                    // Now find out if we have a long enough duration
                    LinkedList eventObjectDesers =
                        eTracker.getReferencedEventStreams(getId());

                    if (eventObjectDesers.size() > 0) {
                        // We only need the first event
                        EventObjectDeserializer deser =
                            (EventObjectDeserializer) eventObjectDesers.getFirst();

                        TriggerFiredEvent last =
                            (TriggerFiredEvent) deserializeEvent(deser, true);

                        // Let's see if we have a chance to fire
                        long duration =
                            tfe.getTimestamp() - last.getTimestamp();
                        
                        // See if we've exceeded the time range
                        if (duration >= timeRange)
                            myEvent = new TriggerFiredEvent(getId(), event);
                    }
                    
                    // Track it in the event tracker for twice the time range
                    eTracker.addReference(getId(), tfe,
                                               timeRange * 2);
                }
            } catch (Exception exc) {
                throw new ActionExecuteException(
                        "Failed to get referenced streams for trigger id="+
                         getId()+" : " + exc);                
            }

            if (myEvent == null) {            
                notFired();
                return;
            }

            // Get ready to fire, reset EventTracker
            eTracker.deleteReference(getId());            
        }    

        myEvent.setMessage("Event " + triggerId + " occurred " +
                           " for " + timeRange / 1000 +" seconds");
        try {
            super.fireActions(myEvent);
        } catch (Exception exc) {
            throw new ActionExecuteException(
                "Error firing actions: " + exc);
        }
    }
}
