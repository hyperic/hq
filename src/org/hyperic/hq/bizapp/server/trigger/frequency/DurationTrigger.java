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

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

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
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.events.server.session.EventTrackerEJBImpl;
import org.hyperic.hq.events.shared.EventObjectDeserializer;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.LongConfigOption;
import org.hyperic.util.timer.Clock;
import org.hyperic.util.timer.ClockFactory;

/** 
 * The DurationTrigger is a simple trigger which fires when the total elapsed 
 * time of triggering events has occurred within a given time window.
 */

public class DurationTrigger extends AbstractTrigger
    implements FrequencyTriggerInterface {
    
    /**
     * The minimum collection interval for trackable events.
     */
    public static final long MIN_COLLECTION_INTERVAL_MILLIS = 60000;
    
    private static final String CFG_COUNT      = "count";
        
    // The possible states for this trigger.
    private static final int INITIAL = 0;
    private static final int TRACKING = 1;
    private static final int FIRING = 2;
    
    // The current state for this trigger.
    private int _currentState;

    private final Log log = LogFactory.getLog(DurationTrigger.class);
    
    private final Object  _lock = new Object();
    
    private final Clock _clock;
    
    private Integer _triggerId;
    private long    _count;
    private long    _timeRange;
    
    private AbstractEvent     _lastTrackableEvent;
    private TriggerFiredEvent _lastTriggerFiredEvent;
    private long              _collectionInterval = MIN_COLLECTION_INTERVAL_MILLIS;
    
    
    /**
     * The default constructor, required by the system when creating instances.
     * When the default constructor is used, then the trigger state must be 
     * initialized explicitly before use.
     * 
     * @see #init(RegisteredTriggerValue)
     */
    public DurationTrigger() {
        resetState();
        _clock = ClockFactory.getInstance().getClock();
    }
    
    /**
     * Creates an instance with the state initialized.
     *
     * @param triggerId The id for the trigger emitting trackable events.
     * @param count The minimum elapsed time of triggering events that will 
     *              cause this trigger to fire.
     * @param timeRange The time range over which we should consider 
     *                   each trackable event (the time window).
     * @param clock The clock.                  
     */
    public DurationTrigger(Integer triggerId, long count, long timeRange, Clock clock) {
        resetState();
        
        _triggerId = triggerId;
        _count = count;
        _timeRange = timeRange;
        _clock = clock;
    }
    
    /**
     * @return <code>true</code> if the trigger is in the initial state.
     */
    public boolean isInitial() {
        synchronized (_lock) {
            return _currentState == INITIAL;
        }
    }
    
    /**
     * @return <code>true</code> if the trigger is currently tracking events.
     */
    public boolean isTracking() {
        synchronized (_lock) {
            return _currentState == TRACKING;
        }
    }
    
    /**
     * Firing is a transient state that a client should never see.
     * 
     * @return <code>true</code> if the trigger is currently firing.
     */
    private boolean isFiring() {
        synchronized (_lock) {
            return _currentState == FIRING;
        }
    }
    
    /**
     * Reset the trigger state.
     */
    private void resetState() {
        synchronized (_lock) {
            _currentState = INITIAL;
            _lastTriggerFiredEvent = null;
        }
    }
    
    /**
     * Retrieve the estimated collection interval for the trackable events.
     * 
     * @return The collection interval or the 
     *         {@link #MIN_COLLECTION_INTERVAL_MILLIS minimum} if no collection 
     *         interval can be estimated yet. The value is based on the timestamp 
     *         difference between two consecutive trackable events of the same 
     *         type (TriggerFiredEvent or TriggerNotFiredEvent). The type is 
     *         important because the timestamp for TriggerFiredEvents is based 
     *         off the voodooed metric value time whereas the timestamp for 
     *         TriggerNotFiredEvents is based off the current system time.
     */
    long getEstimatedCollectionInterval() {
        return _collectionInterval;
    }
    
    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
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
     * @see org.hyperic.hq.bizapp.server.trigger.frequency.FrequencyTriggerInterface#getConfigResponse(java.lang.Integer, long, long)
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

            _triggerId = 
                Integer.valueOf(triggerData.getValue(CFG_TRIGGER_ID));
            _count =
                Long.parseLong(triggerData.getValue(CFG_COUNT)) * 1000;
            _timeRange =
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
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c){
        if (c.equals(HeartBeatEvent.class)) {
            // Want all of heartbeats
            return new Integer[] { RegisteredTriggers.KEY_ALL };
        }
        
        // Same set for both fired and not fired
        return new Integer[] { _triggerId };
    }

    /** 
     * Process an event from the dispatcher.
     * @param event the Event to process
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if an action throws an exception
     */
    public void processEvent(AbstractEvent event)
        throws EventTypeException, ActionExecuteException {

        // If we didn't fulfill the condition, then don't fire
        if(!(event instanceof TriggerFiredEvent    ||
             event instanceof TriggerNotFiredEvent ||
             event instanceof HeartBeatEvent))
            throw new EventTypeException(
                "Invalid event type passed, expected TriggerFiredEvent, " +
                " TriggerNotFiredEvent, or HeartBeatEvent");

        if (!(event instanceof HeartBeatEvent) &&
            !event.getInstanceId().equals(_triggerId))
            throw new EventTypeException("Invalid instance ID passed (" +
                                         event.getInstanceId() + ") expected " +
                                         _triggerId);
         
        EventTrackerLocal eTracker = EventTrackerEJBImpl.getOne();
        
        TriggerFiredEvent fireEvent = null;
                
        synchronized (_lock) {            
            if (receivedOldEvent(event)) {
                return;
            }
                        
            estimateMetricCollectionInterval(event);
                        
            noticeNewEvent(event);
            
            if (shouldStopProcessingEvent(event)) {
                return;
            }
            
            LinkedList eventObjectDesers = getReferencedEventStreams(eTracker);
            
            int numPriorEvents = eventObjectDesers.size();
            
            if (numPriorEvents > 0) {
                // We don't have the very first event. Check if the event 
                // fulfills the trigger conditions.
                fireEvent = 
                    checkIfNewEventFulfillsConditions(event, eventObjectDesers);

                if (fireEvent != null) {
                    _currentState = FIRING;
                }                    

                // If we're not firing and we should track this event type,
                // then set the current event to be tracked.
                if (!isFiring() && isEventTrackable(event)) {
                    _currentState = TRACKING;
                }                    
            } else {
                // We have the very first event. Only track TriggerFiredEvents.
                if (event instanceof TriggerFiredEvent) {
                    _currentState = TRACKING;                    
                }
            }
                        
            verifyCurrentState(fireEvent);
                        
            if (isTracking()) {                
                trackNewEvent(event, eTracker, _collectionInterval);                

                // We are done if we decided only to track the event.
                return;
            }
                        
            if (isFiring()) {
                prepareToFire(eTracker);                    
            }
            
        }

        if (fireEvent != null) {
            try {
                super.fireActions(fireEvent);
            } catch (Exception exc) {
                throw new ActionExecuteException("Error firing actions: " + exc);
            }            
        }
                
    }
    
    /**
     * Verify the current state for this trigger.
     * 
     * @param fireEvent The firing event.
     * @throws IllegalStateException if a corrupted state is detected.
     */
    private void verifyCurrentState(TriggerFiredEvent fireEvent) {
        if (isFiring() && fireEvent == null) {
            throw new IllegalStateException(
                   "If we are firing then there must be a firing event.");
        }
    }
    
    /**
     * Determine if we are looking at an old event.
     * 
     * @param event The new event.
     * @return <code>true</code> if we have an old event; 
     *          <code>false</code> if this is the latest event.
     */
    private boolean receivedOldEvent(AbstractEvent event) {  
        return _lastTrackableEvent != null && event != null &&
               event.getTimestamp() < _lastTrackableEvent.getTimestamp();
    }
    
    /**
     * Notice the new event, setting the appropriate state (the last trackable 
     * event and the last TriggerFiredEvent).
     * 
     * @param event The new event.
     */
    private void noticeNewEvent(AbstractEvent event) {
        if (isEventTrackable(event)) {
            _lastTrackableEvent = event;
        }
        
        if (event instanceof TriggerFiredEvent) {
            _lastTriggerFiredEvent = (TriggerFiredEvent)event;
        }
    }

    /**
     * Determine if we should even bother processing this event. If we are 
     * currently in the initial state (not tracking or firing) and the 
     * new event is a HeartBeatEvent or TriggerNotFiredEvent, then we should 
     * stop processing. Also, if we are tracking events but haven't seen a 
     * TriggerFiredEvent within 2 times the time range (allowing for skew 
     * between agent and server time), then reset the state back to the 
     * initial and don't process this event.
     * 
     * @param event The new event.
     * @return <code>true</code> if we should stop trigger processing immediately; 
     *         <code>false</code> if we should process this event.
     */
    private boolean shouldStopProcessingEvent(AbstractEvent event) {
        if (isInitial() && (event instanceof HeartBeatEvent || 
                            event instanceof TriggerNotFiredEvent)) {
            return true;
        }
        
        // Note that checking if the state should be reset will be driven 
        // by HeartBeatEvents.
        if (isTracking() && _lastTriggerFiredEvent != null) {
            // TimingVoodoo the current clock time to synch this time with 
            // the time stamp reported by the TriggerFiredEvents.
            long adjust = TimingVoodoo.roundDownTime(_clock.currentTimeMillis(), 
                                                     _collectionInterval);
            
            if (_lastTriggerFiredEvent.getTimestamp() < (adjust - 2*_timeRange)) {
                resetState();
                
                return true;                
            }
            
        }
        
        return false;
    }
        
    /**
     * Get the stream of tracked events.
     * 
     * @param eTracker The event tracker.
     * @return The list of EventObjectDeserializer objects representing the 
     *         trigger's tracked state.
     * @throws ActionExecuteException
     */
    private LinkedList getReferencedEventStreams(EventTrackerLocal eTracker)
        throws ActionExecuteException {
        
        LinkedList eventObjectDesers;

        try {
            eventObjectDesers = eTracker.getReferencedEventStreams(getId());
        } catch (Exception e) {
            throw new ActionExecuteException(
                    "Failed to get referenced streams for trigger id="+getId(), e);              
        }
        
        return eventObjectDesers;
    }
    
    /**
     * We can estimate the metric collection interval based off the frequency 
     * of trackable events.
     * 
     * @param event The new event.
     */
    private void estimateMetricCollectionInterval(AbstractEvent event) {
        long estimate = _collectionInterval;
        
        // Note the new event and last trackable event must be of the 
        // same type. This is because TriggerFiredEvent timestamps are 
        // based off the timing voodooed metric value time whereas 
        // TriggerNotFiredEvents are based off the current system time.
        if (isEventTrackable(event) && _lastTrackableEvent != null && 
            _lastTrackableEvent.getClass().isInstance(event)) {
            estimate = event.getTimestamp() - _lastTrackableEvent.getTimestamp();
        }
        
        _collectionInterval = Math.max(estimate, MIN_COLLECTION_INTERVAL_MILLIS);
    }

    /**
     * Track the new event.
     * 
     * @param event The new event.
     * @param eTracker The event tracker.
     * @param collectionInterval The metric collection interval.
     * @throws ActionExecuteException
     */
    private void trackNewEvent(AbstractEvent event, 
                               EventTrackerLocal eTracker, 
                               long collectionInterval) 
        throws ActionExecuteException {
                
        if (isEventTrackable(event)) {
            // We don't need to generate a TriggerNotFiredEvent if processing 
            // a HeartBeatEvent.
            notFired();
            
            // Throw it into the event tracker with a buffer of 1 collection interval.
            long expiration = _timeRange + collectionInterval;
            
            try {
                eTracker.addReference(getId(), event, expiration);                        
            } catch (SQLException e) {
                throw new ActionExecuteException(
                        "Failed to add event reference for trigger id="+
                        getId(), e);                            
            }
        }
    }
    
    /**
     * Prepare the trigger to fire.
     * 
     * @param eTracker The event tracker.
     */
    private void prepareToFire(EventTrackerLocal eTracker) {
        
        try {
            // Get ready to fire, reset trigger state.
            eTracker.deleteReference(getId());                          
        } catch (SQLException exc) {
            // It's ok if we can't delete the old events now.
            // We can do it next time.
            log.warn("Failed to remove all references to trigger id="+getId(), exc);  
        }

        // reset the state before firing
        resetState();
    }

    /**
     * Check if the new event fulfills the triggering conditions.
     * 
     * @param event The new event.
     * @param eventObjectDesers The list of EventObjectDeserializer objects 
     *                          representing the trigger's tracked state.
     * @return The firing event or <code>null</code> if the trigger conditions 
     *         were not fulfilled.
     * @throws ActionExecuteException
     */  
    private TriggerFiredEvent checkIfNewEventFulfillsConditions(
                                                    AbstractEvent event,
                                                    LinkedList eventObjectDesers) 
        throws ActionExecuteException {
        
        long total = getTotalTriggerFiredTime(event, eventObjectDesers);
        
        TriggerFiredEvent targetEvent = null;
        
        if (total >= _count)  {
            targetEvent = prepareTargetEvent(event, eventObjectDesers);
        }
        
        return targetEvent;
    }

    /**
     * Calculate the total trigger fired time using the new event and the 
     * trigger's tracked state.
     * 
     * @param event The new event.   
     * @param eventObjectDesers The list of EventObjectDeserializer objects 
     *                          representing the trigger's tracked state.                      
     * @return The total trigger fired time.
     * @throws ActionExecuteException
     */
    private long getTotalTriggerFiredTime(AbstractEvent event,  
                                          LinkedList eventObjectDesers) 
        throws ActionExecuteException {
        
        // TimingVoodoo the current clock time to synch this time with 
        // the time stamp reported by the TriggerFiredEvents.
        long adjust = TimingVoodoo.roundDownTime(_clock.currentTimeMillis(), 
                                                 _collectionInterval);

        // We don't care about anything older than the specified time range.
        // Add in an extra collection interval to account for skew between 
        // agent and server time.
        final long oldestTick = adjust-(_timeRange+_collectionInterval); 
        
        long lastTick = 0;
        long total = 0;
        
        // Start with the oldest event and move forward in time.
        for (Iterator iter = eventObjectDesers.iterator(); iter.hasNext();) {
            
            EventObjectDeserializer deser = 
                (EventObjectDeserializer) iter.next();
            
            AbstractEvent currentEvent;
            
            try {
                currentEvent = deserializeEvent(deser, true);
            } catch (Exception e) {
                throw new ActionExecuteException(
                "Failed to get deserialize event for trigger id="+getId(), e);   
            }
                                
            if (currentEvent instanceof TriggerFiredEvent) {
                if (lastTick == 0) {
                    // Start the clock.
                    lastTick = Math.max(oldestTick, currentEvent.getTimestamp()); 
                }
            } else {
                // Calculate up to this point.
                if (lastTick > 0) {
                    if (currentEvent.getTimestamp() > lastTick) {
                        total += (currentEvent.getTimestamp() - lastTick);                        
                    }
                    
                    lastTick = 0;
                }
            }                                        
        }
        
        // Now include the new event in the trigger fired time calculation.
        // If lastTick is positive, then we calculate the total using the new 
        // event.
        if (lastTick > 0 && event.getTimestamp() > lastTick) {            
            total += (event.getTimestamp() - lastTick);
        }
                
        return total;
    }

    /**
     * Prepare the target event for the firing actions.
     * 
     * @param event The new event.
     * @param eventObjectDesers The list of EventObjectDeserializer objects 
     *                          representing the trigger's tracked state.
     * @return The target event.
     * @throws ActionExecuteException
     */
    private TriggerFiredEvent prepareTargetEvent(AbstractEvent event,
                                                 LinkedList eventObjectDesers) 
        throws ActionExecuteException {
        
        AbstractEvent mostRecentTfe = null;
        
        // We can fire, so now we look for the most recent TriggerFiredEvent.
        if (event instanceof TriggerFiredEvent) {
            mostRecentTfe = event;
        } else {
            // The current event is not a TriggerFiredEvent, so 
            // now we have to look at the stored events from the 
            // most recent to least recent.
            for (ListIterator iter = 
                eventObjectDesers.listIterator(eventObjectDesers.size()); 
                iter.hasPrevious() && mostRecentTfe == null;) {
                
                EventObjectDeserializer deser = 
                    (EventObjectDeserializer) iter.previous();
                
                AbstractEvent prior;
                
                try {
                    prior = deserializeEvent(deser, true);
                } catch (Exception e) {
                    throw new ActionExecuteException(
                    "Failed to get deserialize event for trigger id="+getId(), e);   
                }
                
                if (prior instanceof TriggerFiredEvent) {
                    mostRecentTfe = prior;
                }
            }
        }
        
        if (mostRecentTfe == null) {
            throw new IllegalStateException("No TriggerFiredEvent found " +
            		                        "for trigger id="+getId());
        }
        
        // Create the target event
        TriggerFiredEvent targetEvent = 
            new TriggerFiredEvent(getId(), mostRecentTfe);

        targetEvent.setMessage("Event " + _triggerId + " occurred " +
                           _count / 1000 + " seconds within " +
                           _timeRange / 1000 + " seconds");
        
        return targetEvent;
    }
    
    /**
     * Is the event something that we should track?
     * 
     * @param event The event.
     * @return <code>true</code> if we should track the event.
     */
    private boolean isEventTrackable(AbstractEvent event) {
        return event instanceof TriggerFiredEvent || 
               event instanceof TriggerNotFiredEvent;
    }
    
    /**
     * Is the event something that may cause this trigger to fire?
     * 
     * @param event the event.
     * @return <code>true</code> if the event may cause this trigger to fire.
     */
    private boolean isEventFireable(AbstractEvent event) {
        return event instanceof TriggerFiredEvent || 
               event instanceof HeartBeatEvent;
    }
}
