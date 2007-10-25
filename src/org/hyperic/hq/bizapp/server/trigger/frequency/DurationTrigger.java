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

import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.HeartBeatEvent;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.events.server.session.EventTrackerEJBImpl;
import org.hyperic.hq.events.shared.EventTrackerLocal;
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

public class DurationTrigger extends AbstractTrigger
    implements RegisterableTriggerInterface, FrequencyTriggerInterface {
    private static final String CFG_COUNT      = "count";

    private Object  lock = new Object();
    private Integer triggerId;
    private long    count;
    private long    timeRange;
    private Log     log;
    
    private AbstractEvent savedLast  = null;
    private long          savedTotal = 0;
    private boolean       savedInit  = false;

    public DurationTrigger(){
        log = LogFactory.getLog(DurationTrigger.class);
    }

    public ConfigSchema getConfigSchema() {
        ConfigSchema res = new ConfigSchema();
        IntegerConfigOption tid;
        LongConfigOption range, count;

        tid = new IntegerConfigOption(CFG_TRIGGER_ID,
                                      "Trigger ID emitting events to count",
                                      null);
        tid.setMinValue(0);

        count = new LongConfigOption(CFG_COUNT,
            "Duration of the event occurence (in seconds)", new Long(1));
        count.setMinValue(1);

        range = new LongConfigOption(CFG_TIME_RANGE,
            "Within time range (in seconds)", new Long(300));
        range.setMinValue(1);

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
            throw new InvalidOptionValueException(
                "Duration must be greater than 0");

        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_TRIGGER_ID, tid.toString());
        resp.setValue(CFG_TIME_RANGE, "" + range);
        resp.setValue(CFG_COUNT, "" + count);
        return resp;
    }

    /** 
     * Initialize the trigger
     *
     * @param tval  Configuration data for the trigger
     *
     * @throws org.hyperic.hq.bizapp.server.trigger.InvalidTriggerDataException
               indicating that the trigger config was invalid.
     *
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
            count =
                Long.parseLong(triggerData.getValue(CFG_COUNT)) * 1000;
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
     * Get the event classes that the trigger is interested in
     * seeing.  This is an optimization, so that a trigger's
     * processEvent() method is called only when a valid event
     * occurs.
     *
     * @return an array of Class objects which implement
     *          the 'Event' interface
     *
     */
    public Class[] getInterestedEventTypes(){
        return new Class[] { TriggerFiredEvent.class,
                             TriggerNotFiredEvent.class,
                             HeartBeatEvent.class };
    }

    /** 
     * Get a list of instance IDs specific to a class (as returned
     * by getInterestedEventTypes) which the trigger is interested
     * in seeing.  These values are specific to the event type, and
     * represent things such as specific measurements.
     *
     * @param c Class to get the interested event IDs for
     *
     * @return An array of integers representing the instance IDs
     *          for the specific event class
     *
     */
    public Integer[] getInterestedInstanceIDs(Class c){
        if (c.equals(HeartBeatEvent.class)) {
            // Want all of heartbeats
            return new Integer[] { RegisteredTriggers.KEY_ALL };
        }
        
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
        AbstractEvent tfe;
        LinkedList events;

        // Bug #8781.  In most cases, the event that satisfies the
        // duration trigger will be the last event fired.  When this
        // is not the case, the incoming event will be a
        // TriggerNotFiredEvent and the previous event will be the one
        // we want.
        AbstractEvent rootEvent = event;

        // If we didn't fulfill the condition, then don't fire
        if(!(event instanceof TriggerFiredEvent    ||
             event instanceof TriggerNotFiredEvent ||
             event instanceof HeartBeatEvent))
            throw new EventTypeException(
                "Invalid event type passed, expected TriggerFiredEvent, " +
                " TriggerNotFiredEvent, or HeartBeatEvent");

        tfe = (AbstractEvent) event;
        if (!(tfe instanceof HeartBeatEvent) &&
            !tfe.getInstanceId().equals(triggerId))
            throw new EventTypeException("Invalid instance ID passed (" +
                                         tfe.getInstanceId() + ") expected " +
                                         triggerId);

        // Let's see if we might fire, if we have some saved data
        if (savedInit) {
            if (savedLast == null) {
                if (tfe instanceof HeartBeatEvent ||
                    tfe instanceof TriggerNotFiredEvent)
                    return;
            }
            else {
                if (tfe instanceof HeartBeatEvent) {
                    if (savedLast instanceof TriggerFiredEvent) {
                        if (tfe.getTimestamp() - savedLast.getTimestamp() +
                                savedTotal < count)
                        return;
                    }
                }
                else if (tfe.getClass().equals(savedLast.getClass()))
                    return;
            }
        }
        
        synchronized (lock) {
            EventTrackerLocal eTracker = EventTrackerEJBImpl.getOne();

            boolean track = false;
            boolean fire = false;
            
            try {
                // Now find out if we have the specified # within the interval
                events = eTracker.getReferencedEventStreams(getId());
                savedInit = true;   // We've looked up previously saved events
                savedTotal = 0;
                
                if (log.isDebugEnabled())
                    log.debug("Look up events for trigger: " + getId());

                if (events.size() > 0) {
                    // Get the last event
                    ObjectInputStream p = (ObjectInputStream) events.getLast();
                    AbstractEvent last = deserializeEventFromStream(p, true);

                    // Bug #8781.  If the current event is a
                    // TriggerNotFiredEvent, we want to use the
                    // previous event as the root event.
                    if (event instanceof TriggerNotFiredEvent) {
                        rootEvent = last;
                        savedLast = last;
                    }

                    // Let's see if we have a chance to fire
                    if (last instanceof TriggerFiredEvent) {
                        long total = 0;
                        long lastTick = 0;
                        
                        // Iterate and add up the time
                        AbstractEvent next;
                        for (Iterator i = events.iterator(); i.hasNext();) {
                            // Deserialize each one, reuse variables
                            p = (ObjectInputStream) i.next();

                            // If last, then we've already deserialized it                            
                            if (i.hasNext())
                                next = deserializeEventFromStream(p, true);
                            else
                                next = last;

                            if (next instanceof TriggerFiredEvent) {
                                // Start the clock
                                if (lastTick == 0)
                                    lastTick = next.getTimestamp();
                                
                                rootEvent = next;
                            }
                            else {
                                // Calculate up to this point
                                if (lastTick > 0) {
                                    total += (next.getTimestamp() - lastTick);
                                    lastTick = 0;
                                }
                            }
                        }
                            
                        // If lastTick is positive, then we add to it
                        if (lastTick > 0)
                            total += (tfe.getTimestamp() - lastTick);
                        
                        // Not enough time
                        if (total < count) {
                            // Now send a NotFired event
                            if (!(tfe instanceof HeartBeatEvent)) {
                                notFired();
                                savedTotal = total;
                            }
                        } else {
                            fire = true;
                        }
                    }
                
                    // Only need to track if last is different class
                    track = !(tfe instanceof HeartBeatEvent) &&
                            !(last.getClass().equals(tfe.getClass()));
                } else {
                    notFired();
                    savedLast = null;

                    // Track if this is the very first fired event
                    if (tfe instanceof TriggerFiredEvent)
                        track = true;
                    else            // Do nothing else
                        return;
                }
            } catch (Exception exc) {
                throw new ActionExecuteException(
                        "Failed to get referenced streams for trigger id="+
                         getId()+" : " + exc);                
            }
            
            /* Make sure we only write once (either delete or add) in this 
             * function otherwise, we have to make sure that things are in the
             * same user transaction, which is a pain */

            if (track) {
                // Throw it into the event tracker with a buffer of 30 seconds
                try {
                    eTracker.addReference(getId(), tfe,
                                          timeRange + 30000);
                    savedLast = tfe;
                    
                    if (log.isDebugEnabled())
                        log.debug("Save last " + tfe.getClass() + " for id: " +
                                  getId());
                } catch (Exception exc) {
                    throw new ActionExecuteException(
                        "Error adding event reference for trigger id="+getId()+
                        " : " + exc);
                }
                return;         // We're done
            }

            if (fire) {
                try {
                    // Get ready to fire, reset EventTracker
                    eTracker.deleteReference(getId());
                } catch (Exception exc) {
                    throw new ActionExecuteException
                        ("Failed to delete referenced events for trigger Id="+
                         getId()+" : " + exc);
                }
            }
            else
                return;
        }

        // Reset the cached values
        savedLast = null;
        savedTotal = 0;

        TriggerFiredEvent myEvent =
            new TriggerFiredEvent(getId(), rootEvent);

        myEvent.setMessage("Event " + triggerId + " occurred " +
                           count / 1000 + " seconds within " +
                           timeRange / 1000 + " seconds");
        try {
            super.fireActions(myEvent);
        } catch (Exception exc) {
            throw new ActionExecuteException
                ("Error firing actions: " + exc);
        }
    }
}
