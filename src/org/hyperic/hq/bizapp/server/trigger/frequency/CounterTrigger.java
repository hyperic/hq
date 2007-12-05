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

import java.util.Collection;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
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

public class CounterTrigger extends AbstractTrigger
    implements FrequencyTriggerInterface
{
    private static final String CFG_COUNT      = "count";

    private Integer triggerId;
    private int     count;
    private long    timeRange;
    
    public CounterTrigger() {}

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        ConfigSchema res = new ConfigSchema();
        IntegerConfigOption tid, count;
        LongConfigOption range;

        tid = new IntegerConfigOption(CFG_TRIGGER_ID,
                                      "Trigger ID emitting events to count",
                                      null);
        tid.setMinValue(0);

        count = new IntegerConfigOption(CFG_COUNT,
                                        "Number of events which must occur " +
                                        "for trigger to fire", null);
        count.setMinValue(0);

        range = new LongConfigOption(CFG_TIME_RANGE,
                                     "Window to for which to count events " +
                                     "(in seconds)",
                                     new Long(300));
        range.setMinValue(0);

        
        res.addOption(tid);
        res.addOption(count);
        res.addOption(range);
        return res;
    }

    /**
     * @see org.hyperic.hq.bizapp.server.trigger.frequency.FrequencyTriggerInterface#getConfigResponse(Integer, long, int)
     */
    public ConfigResponse getConfigResponse(Integer tid,
                                            long range, long count)
        throws InvalidOptionException, InvalidOptionValueException {
        if (count < 1)
            throw new InvalidOptionValueException("Count must be at least 1");

        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_TRIGGER_ID, tid.toString());
        resp.setValue(CFG_TIME_RANGE, "" + range);
        resp.setValue(CFG_COUNT, "" + count);
        return resp;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#init(org.hyperic.hq.events.shared.RegisteredTriggerValue)
     */
    public void init(RegisteredTriggerValue tval)
        throws InvalidTriggerDataException
    {
        // Set the trigger value
        setTriggerValue(tval);

        try {
            ConfigResponse triggerData =
                ConfigResponse.decode(getConfigSchema(),
                                      tval.getConfig());

            triggerId = 
                Integer.valueOf(triggerData.getValue(CFG_TRIGGER_ID));
            count =
                Integer.parseInt(triggerData.getValue(CFG_COUNT));
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
    public Class[] getInterestedEventTypes(){
        return new Class[] { TriggerFiredEvent.class };
    }
    
    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c){
        return new Integer[] { triggerId };
    }

    /** 
     * Process an event from the dispatcher.
     * @param event the Event to process
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if an action throws an exception
     */
    public void processEvent(AbstractEvent event)
        throws EventTypeException, ActionExecuteException
    {
        
        // If we didn't fulfill the condition, then don't fire
        if(!(event instanceof TriggerFiredEvent))
            throw new EventTypeException("Invalid event type passed, " +
                                         "expected TriggerFiredEvent");

        TriggerFiredEvent tfe = (TriggerFiredEvent) event;
        if(!tfe.getInstanceId().equals(triggerId))
            throw new EventTypeException("Invalid instance ID passed (" +
                                         tfe.getInstanceId() + ") expected " +
                                         triggerId);

        EventTrackerLocal eTracker;
        
        try {
            eTracker = EventTrackerUtil.getLocalHome().create();
        } catch(NamingException exc){
            return; // No fire since we can't track the events
        } catch(CreateException exc){
            return; // No fire since we can't track the events
        }
        
        Collection eventObjectDesers;

        try {
            // Now find out if we have the specified # within the interval
            eventObjectDesers = eTracker.getReferencedEventStreams(getId());
        } catch(Exception exc){
            throw new ActionExecuteException("Failed to get referenced " +
                                             "streams for trigger id="+
                                             getId()+" : " + exc);
        }

        /* Make sure we only write once (either delete or add) in this function
           otherwise, we have to make sure that things are in the same
           user transaction, which is a pain */
        if ((eventObjectDesers.size() + 1) >= count){
            // Get ready to fire, reset EventTracker
            eTracker.deleteReference(getId());
            
            TriggerFiredEvent myEvent = new TriggerFiredEvent(getId(), event);

            myEvent.setMessage("Event " + triggerId + " occurred " +
                               (eventObjectDesers.size() + 1) + " times within " +
                               timeRange / 1000 + " seconds");
            try {
                super.fireActions(myEvent);
            } catch(Exception exc){
                throw new ActionExecuteException("Error firing actions: " +
                                                 exc);
            }
        } else {
            // Throw it into the event tracker
            eTracker.addReference(getId(), tfe, timeRange);
            
            // Now send a NotFired event
            notFired();
        }
    }
}
