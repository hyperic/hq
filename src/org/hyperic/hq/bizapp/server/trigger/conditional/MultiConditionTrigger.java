/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

/*
 * MultiConditionTrigger.java
 *
 * Created on October 21, 2002, 4:06 PM
 */

package org.hyperic.hq.bizapp.server.trigger.conditional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.LongConfigOption;
import org.hyperic.util.config.StringConfigOption;

/** The MultiConditionTrigger is a specialized trigger that can combine multiple
 * conditions and only fire actions when all conditions have been met
 *
 */
public class MultiConditionTrigger
    extends AbstractTrigger {
    private static final Log log = LogFactory.getLog(MultiConditionTrigger.class);
    
    public static final String CFG_TRIGGER_IDS = "triggerIds";
    public static final String CFG_TIME_RANGE  = "timeRange";
    public static final String CFG_DURABLE     = "durable";

    public static final String AND = "&";
    public static final String OR  = "|";
    
    /** Holds value of property triggerIds. */
    private Set triggerIds;

    /** Holds value of property andTriggerIds. */
    private Integer[] andTriggerIds;

    /** Holds value of property orTriggerIds. */
    private Map orTriggerIds;

    /** Holds value of property timeRange. */
    private long timeRange;
    
    /** Holds value of property durable. */
    private boolean durable;
    
    /** Creates a new instance of MultiConditionTrigger */
    public MultiConditionTrigger() {
    	baton = new Baton();
    }
    
    protected static class Baton {
    	// Container class for all things synchronized.  This class does no
    	// synchronization on its own: concurrency correctness must be
    	// ensured by the calling code.
    	
    	// The thread currently holding the baton
    	Thread processor;
    	
    	// The list of events that the baton holder must process
    	LinkedList eventsToProcess;
    	
    	// Prior persisted state, if any.  This will usually be empty/null.
    	LinkedList priorState;
    	
    	Baton() {
    		eventsToProcess = new LinkedList();
    		priorState = null;
    	}
    	
    	Thread getProcessor() {
    		return processor;
    	}
    	
    	/*
    	 * NOTE: returns the actual list reference, not a copy.  
    	 */
    	LinkedList getEventsToProcess() {
    		return eventsToProcess;
    	}
    	
    	/*
    	 * NOTE: returns the actual list reference, not a copy.  
    	 */
    	LinkedList getPriorState() {
    		return priorState;
    	}
    	
    	void setPriorState(LinkedList priorState) {
    		this.priorState = priorState;
    	}
    	
    	// Must be done under lock!
    	boolean grab() {
    		boolean result = false;
    		if (processor == null) {
    			processor = Thread.currentThread();
    			result = true;
    		} else {
    			if (processor.equals(Thread.currentThread())) {
    				// Should not happen!
    				log.error("Illegal monitor state in Baton");
    				// But...the best thing to do in this (impossible?) scenario is to proceed
    				result = true;
    			}
    		}
    		
    		return result;
    	}

    	// Must be done under lock!
		public void release() {
			processor = null;
		}
    }
    
    protected Baton baton;
    
    /** 
     * Process an event from the dispatcher.  This is the main entrypoint method.
     * This method must:
     * 1) Examine current running state.  If another thread is already processing for
     *     this trigger, simply put the event on a queue for that thread to pick up,
     *     and then exit.
     * 2) If the current thread is to process the event (i.e. no other thread currently
     *     processing), then determine if event stream initialization from the database
     *     must be done.  This is necessary on startup, or on failover in an HA instance.
     * 3) Once all necessary event stream initialization is done, process the events in
     *     the order they are received.  Processing of an event consists of examining
     *     the current event and all prior events that have not caused a trigger fire.
     *     Also, the event must be added to permanent storage until the trigger fires,
     *     because this enables failover to do the right thing.  Handle expiring events.
     * 
     * @param event the Event to process
     * @throws ActionExecuteException if an action throws an exception
     */
    public void processEvent(AbstractEvent event)
        throws EventTypeException, ActionExecuteException {
    	
        // If we didn't fulfill the condition, then don't fire
        if (!(event instanceof TriggerFiredEvent ||
              event instanceof TriggerNotFiredEvent)) {
            throw new EventTypeException(
                "Invalid event type passed: expected TriggerFiredEvent " +
                "or TriggerNotFiredEvent");
        }
        
        if (log.isDebugEnabled()) {
        	log.debug("processEvent for event " + event);
        }
        
        LinkedList eventsToProcess = getEventsToProcess(event);
        if (eventsToProcess != null) {
        	// We have the baton, continue
        	evaluateEventList(eventsToProcess);
        }
    }
    
    private LinkedList getEventsToProcess(AbstractEvent event) {
    	LinkedList result = null;
    	synchronized (baton) {
    		if (baton.grab()) {
    			// Drain the event queue and continue processing
    			result = new LinkedList(baton.getEventsToProcess());
    			baton.getEventsToProcess().clear();
    			result.add(event);
    		} else {
    			// Queue the event for the baton holder to process
    			baton.getEventsToProcess().add(event);
    		}
    	}
    	
    	return result;
    }
    
    /**
     * Internal workhorse method.  This method is not synchronized because it assumes it is
     * running only when it has the baton.  Chew through the list of events until it is emptied.
     * It is known at the start of this method that eventsToProcess.size() > 0.
     */
	void evaluateEventList(LinkedList eventsToProcess) {
		
        EventTrackerLocal etracker = null;
        try {
            etracker = EventTrackerUtil.getLocalHome().create();
        } catch (Exception e) {
            log.error("Internal error, cannot create event tracker", e);
        }
        		
        boolean drainedQueue = false;
        boolean doDebug = log.isDebugEnabled();
        Set persistedEventsToDelete = new HashSet();
        // It guaranteed that, if we're here, there is at least one event in the queue
        do {
        	
        	AbstractEvent current = (AbstractEvent) eventsToProcess.removeFirst();
        	
            if (doDebug) {
            	log.debug("evaluating event " + current);
            }
                    	
        	// Evaluate one-by-one.  Requirements:
        	//
        	// 1. Evaluate events in the order they came in, starting with the event
        	//     at position startFrom.
        	// 2. Filter out expired events based on the timestamp of the "current" event.
        	//     If this trigger has an expiration of N milliseconds, then any events
        	//     with a timestamp less than (current.getTimestamp() - N) is considered
        	//     to be expired.
        	// 3. Once triggering conditions are met, fire and get rid of all previous events,
        	//     per the feature requirements for this trigger.
        	// 4. Leave events that occurred after the triggering event for subsequent
        	//     evaluation.
        	// 5. A MultiConditionTrigger can fire with a single triggering condition,
        	//     for example, if the conditions are "a OR b" and a fires.  This means
        	//     the list of conditions need to be evaluated front-to-back when there
        	//     are any OR conditions.  If there are only AND conditions, then the
        	//     minimum is the number of AND conditions.
        	//
        	// Other things to note:
        	//
        	// - a TriggerNotFiredEvent has no useful meaning if there are no prior
        	//    TriggerFiredEvents for the condition
        	// - a TriggerFiredEvent followed (not necessarily immediately followed, if
        	//    the TriggerNotFiredEvent occurs before sufficient fulfilling conditions)
        	//    by a TriggerNotFiredEvent for the same condition effectively neutralizes
        	//    both events
			if (baton.getPriorState() == null) {
				
				// First time seeing this trigger.  There may be backed-up events
				LinkedList persistedStream = getPersistedReferencedEvents(etracker);
				baton.setPriorState(persistedStream);
			}
        	
        	if (doDebug) {
        		log.debug("Prior state for event " + current + " is " + baton.getPriorState());
        	}
        	
        	Collection fulfilled = doSingleEvaluation(baton.getPriorState(),
        											  current,
        											  etracker,
        											  persistedEventsToDelete);
        	if (fulfilled != null) {
        		if (doDebug) {
        			log.debug("Trigger " + this + " firing on event " + current);
        		}
        		
        		fire(fulfilled, etracker);
        	}
        	
        	if (eventsToProcess.isEmpty()) {
        		synchronized (baton) {
        			if (baton.getEventsToProcess().isEmpty()) {
        				// Looks like we're done here...cleanup and go home
        				etracker.deleteEvents(persistedEventsToDelete);
            			if (log.isDebugEnabled()) {
            				log.debug("MultiConditionTrigger trigger id=" + getId() +
            						" deleting event set size=" + persistedEventsToDelete.size());
            			}
        				baton.release();
        				drainedQueue = true;
        			} else {
        				// New events have come in while we were processing
        				eventsToProcess.addAll(baton.getEventsToProcess());
        				baton.getEventsToProcess().clear();
        			}
        		}
        	}
        	
        	if (!drainedQueue) {
        		if (log.isDebugEnabled()) {
        			log.debug("Event queue size is " + baton.getEventsToProcess().size() +
        					  ", continuing.");
        		}
        	}
        	
        } while (!drainedQueue);
	}
	
	/**
	 * Evaluate this trigger based on a set of prior events and a new, incoming event.  This
	 * method should only be called when the set of prior events is not sufficient to cause
	 * a trigger fire.
	 * 
	 * @param priorEvents   A live, mutable collection of prior events that will be
	 *                       updated based on this evaluation
	 * @param event         The incoming event
	 * @param etracker      Persisted event tracker, used to persist state changes.  These
	 *                       state changes must be made in tandem with the in-memory changes.
	 * @parem persistedEventsToDelete
	 *                      Set that gets added to in this method.  Objects put into this set
	 *                       will be deleted in one operation when this class completes a cycle
	 *                       of event processing (i.e. drains the queue).
	 * @return              The collection of events that causes this trigger to fire.  Null
	 *                       if the incoming event does not cause the trigger to fire.
	 */
    private Collection doSingleEvaluation(LinkedList priorEvents,
    									  AbstractEvent event,
    									  EventTrackerLocal etracker,
    									  Set persistedEventsToDelete) {
    	
    	Collection result = null;
    	
    	long timeRange = getTimeRange();
    	long expire = Long.MIN_VALUE;
    	if (timeRange > 0) {
    		expire = System.currentTimeMillis() - timeRange;
    	}
    	
    	AbstractEvent toUpdate = null;

    	// Create a table to keep track
    	Map fulfilled = new LinkedHashMap();
    	Map subTriggersNotFiring = null;

    	// Add in the current event
    	priorEvents.add(event);

    	for (Iterator iter = priorEvents.iterator(); iter.hasNext(); ) {

    		AbstractEvent tracked = (AbstractEvent) iter.next();
    		if (tracked.getTimestamp() >= expire) {

    			if (tracked != event &&
    					tracked.getInstanceId().equals(event.getInstanceId())) {

    				// If this tracked event equals the new event, then
    				// the old one is obsolete.  There can be only one event
    				// to delete, because of the way the event stream is
    				// evaluated -- every step along the way filters out any
    				// previous obsolete event.
    				
    				// Mark old event for update in permanent storage.  It's state will
    				// be overwritten with the new state.
    				toUpdate = tracked;
    				
    				// Delete previous obsolete event from the list of events to consider in
    				// evaluations.  In the common case, the list comes from memory, not from
    				// the persisted event stream.  This remove only affects the in-memory
    				// stream.  The persisted stream is handled by the updateReference() call.
    				iter.remove();
    			}

    			if (tracked instanceof TriggerFiredEvent) {
    				fulfilled.put(tracked.getInstanceId(), tracked);

    				// If there were any previous "not fired" events for the 
    				// sub-trigger, get rid of them
    				if (subTriggersNotFiring != null) {
    					subTriggersNotFiring.remove(tracked.getInstanceId());
    				}
    			} else { // TriggerNotFiredEvent
    				fulfilled.remove(tracked.getInstanceId());

    				// For this trigger we should publish not fired events only 
    				// when we have tracked that one of the sub trigger does 
    				// not currently fulfill the conditions. Otherwise, the 
    				// DurationTrigger when applied as a damper to a recovery 
    				// alert definition will not work correctly (remember that 
    				// recovery alert definitions use the MultiConditionTrigger).
    				// Once the recovery condition is met for the recovery alert 
    				// definition we only want the listening DurationTrigger to 
    				// receive a TriggerNotFiredEvent when the conditions are not 
    				// met, not because the primary alert definition has not
    				// currently fired.
    				if (subTriggersNotFiring == null) {
    					subTriggersNotFiring = new HashMap();
    				}
    				subTriggersNotFiring.put(tracked.getInstanceId(), tracked);
    			}
    		} else {
    			// Get rid of the expired event.  Since the prior events are in time order,
    			// an expired event cannot have any useful purpose once it is identified
    			// as being expired.
    			// INVARIANT: toUpdate != tracked here, because toUpdate is only
    			// assigned a value if tracked is not expired.
    			deleteEvent(tracked, persistedEventsToDelete);
    			iter.remove();
    		}
    	}

    	// If we've got nothing, then just clean up.  This condition means that any
    	// prior events were TriggerNotFiredEvents and are just laying around
    	// taking up space, or there were some TriggerFiredEvents that were later
    	// made obsolete by TriggerNotFiredEvents.
    	boolean checkCompletion = true;
    	boolean sendNotFired = false;
    	if (fulfilled.size() == 0) {

    		// Not a single event fulfills the conditions of this trigger.  No way we're firing,
    		// so don't even do the evaluation.
    		checkCompletion = false;
    		if (subTriggersNotFiring != null && subTriggersNotFiring.size() > 0) {
    			sendNotFired = true;
    		}
    	}

    	if (checkCompletion) {

    		if (!triggeringConditionsFulfilled(fulfilled.values())) {

    			try {
    				// Clean up unused event, track the current one, if necessary
    				boolean track = true;
    				if (toUpdate != null) {
    					if (timeRange > 0) {
        					// If the trigger has an expiration, update its state.  Updating
    						// state means (1) set the event object BLOB to the current event,
    						// and (2) update the expiration time to reflect the current time.
    						// This code will always update a NON-EXPIRED trigger condition.
    						updateStateForEvent(etracker, toUpdate);
        					
        					// If we updated, we will not track.
    						track = false;
    					} else {
    						// No expiration, but the old state is made obsolete by the new
    						// state.  Delete the old state, it will be replaced with the new state.
    						deleteEvent(toUpdate, persistedEventsToDelete);
    					}
    				}
    				
    				if (event instanceof TriggerNotFiredEvent) {
    					// Only need track TriggerFiredEvent
    					track = false;
    				}

    				if (track) {
    					etracker.addReference(getId(), event, getTimeRange());
    	    			if (log.isDebugEnabled()) {
    	    				log.debug("MultiConditionTrigger trigger id=" + getId() +
    	    						" adding reference.");
    	    			}
    				}
    				
    	    		if (subTriggersNotFiring != null && subTriggersNotFiring.size() > 0) {
    	    			sendNotFired = true;
    	    		}

    			} catch (SQLException e) {
    				log.error("Failed to add event reference for trigger id=" +
    						getId(), e);
    			}            
    		} else {
    			// fulfilled!
    			result = fulfilled.values();
    			resetState(etracker, priorEvents);
    		}
    	} else {
    		// Don't even evaluate, just start over
    		resetState(etracker, priorEvents);
    	}
    	
    	if (sendNotFired) {
    		publishNotFired();
    	}

    	return result;
    }
    
    protected void resetState(EventTrackerLocal etracker, Collection priorEvents) {
		
		// reset state
    	priorEvents.clear();
		try {
			// This deletes all persisted state for this trigger
			etracker.deleteReference(getId());
			if (log.isDebugEnabled()) {
				log.debug("MultiConditionTrigger trigger id=" + getId() +
						" deleting references");
			}
		} catch (SQLException e) {
			// It's bad, but not unrecoverable, if we can't delete the old events now.
			// We can do it next time.
			log.warn("Failed to remove all references to trigger id=" +
					 getId(), e);                
		}
    }
    
    protected void updateStateForEvent(EventTrackerLocal etracker, AbstractEvent toUpdate) {
		try {
			etracker.updateReference(getId(), toUpdate.getId(),
									 toUpdate, getTimeRange());
			if (log.isDebugEnabled()) {
				log.debug("MultiConditionTrigger trigger id=" + getId() +
						" updating references for teid " + toUpdate.getId());
			}
		} catch (SQLException e) {
			log.debug("Failed to update event reference for " +
					"trigger id=" + getId(), e);
		}
    }
    
    protected void deleteEvent(AbstractEvent toDelete,
    						   Set persistedEventsToDelete) {
    	persistedEventsToDelete.add(toDelete.getId());
    }
    
    /**
     * Publish a TriggerNotFiredEvent.  The notFired() method is (as of this writing)
     * final in the superclass, so this wrapper method exists to separate out that
     * implementation detail.
     */
    protected void publishNotFired() {
    	notFired();
    }
    
    /**
     * Check if the triggering conditions have been fulfilled, meaning 
     * the state should be flushed.  Contract: for an encoding of:
     * 1&2|3&4|5&6
     * the conditions are evaluated left-to-right, with no precedence rule
     * of AND over OR (so it's different from Java precedence rules).
     * 
     * @return <code>true</code> if the triggering conditions have been fulfilled; 
     *         <code>false</code> if not.
     */
    public boolean triggeringConditionsFulfilled(Collection events) {

    	// Find out which instance we should be looking for
    	Integer[] subIds = getAndTriggerIds();
    	Map orIds = getOrTriggerIds();
    	
    	boolean result = true;

    	Set fulfilled = new HashSet();
    	for (Iterator it = events.iterator(); it.hasNext(); ){
    		AbstractEvent event = (AbstractEvent) it.next();
    		fulfilled.add(event.getInstanceId());
    	}

    	// Now let's see how well we did
    	int orInd = 0;

    	for (Iterator i = orIds.keySet().iterator(); i.hasNext(); ) {
    		Object orId = i.next();
    		if (fulfilled.contains(orId)) {
    			Integer index = (Integer) orIds.get(orId);
    			if (orInd < index.intValue()) {
    				orInd = index.intValue();
    			}
    		}
    	}

    	// Go through the subIds
    	for (int i = orInd; result == true && i < subIds.length; i++) {
    		// Did not fulfill yet
    		if (!fulfilled.contains(subIds[i])) {
    			result = false;
    		}
    	}

    	return result;            
    }
    
    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        ConfigSchema res = new ConfigSchema();

        StringConfigOption tids =
            new StringConfigOption(
                    CFG_TRIGGER_IDS,
                    "Sub trigger IDs (separated by '&' and '|')", "");
        res.addOption(tids);

        LongConfigOption range =
            new LongConfigOption(CFG_TIME_RANGE, "Conditions met within (sec)",
                                 new Long(0));
        res.addOption(range);

        BooleanConfigOption durable =
            new BooleanConfigOption(CFG_DURABLE, "Send NotFired Events", false);
        res.addOption(durable);

        return res;
    }

    public ConfigResponse getConfigResponse(String stids, boolean endure,
                                            long range)
        throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_TRIGGER_IDS, stids);
        resp.setValue(CFG_DURABLE, String.valueOf(endure));
        resp.setValue(CFG_TIME_RANGE, String.valueOf(range));
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
            ConfigResponse triggerData = ConfigResponse.decode(tval.getConfig());
            String stids    = triggerData.getValue(CFG_TRIGGER_IDS);
            String srange   = triggerData.getValue(CFG_TIME_RANGE);
            String sdurable = triggerData.getValue(CFG_DURABLE);

            init(stids,
            	 Long.parseLong(srange) * 1000,
            	 Boolean.valueOf(sdurable).booleanValue());

        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        } catch (NumberFormatException e) {
            throw new InvalidTriggerDataException(e);
        }
    }
    
    public void init(String stids, long trange, boolean durable) {

        triggerIds = new HashSet();
        orTriggerIds  = new HashMap();
        ArrayList andTrigIds = new ArrayList();
        
        String delimiters = (AND + OR);
        StringTokenizer st = new StringTokenizer(stids, delimiters, true);
        int i = 0;
        while (st.hasMoreTokens()) {
            boolean bAnd = true;
            String tok = st.nextToken();
            if (tok.equals(AND) || tok.equals(OR)) {
                bAnd = tok.equals(AND);
                tok = st.nextToken();
            }

            Integer tid = new Integer(tok);

            if (bAnd) {
                // Put it in the AND list
            	andTrigIds.add(tid);
                i++;
            } else {
                // Put it in the OR list
            	orTriggerIds.put(tid, new Integer(i));
            }
            
            // All trigger ID's go into triggerIds
            triggerIds.add(tid);
        }

        andTriggerIds = (Integer[]) andTrigIds.toArray(
                new Integer[andTrigIds.size()]);

        // Turn timerange into milliseconds
        setTimeRange(trange);
        
        setDurable(durable);
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
        // Ask the sub triggers what they are interested in
        // Same set for both fired and not fired
        Set trigSet = getTriggerIds();
        return (Integer[]) trigSet.toArray(new Integer[trigSet.size()]);
    }

    /** Getter for property triggerIds.
    * @return Value of property triggerIds.
    *
    */
   public Set getTriggerIds() {
       return triggerIds;
   }

   /** Setter for property triggerIds.
    * @param triggerIds New value of property triggerIds.
    *
    */
   public void setTriggerIds(Set val) {
       triggerIds = val;
   }

    /** Getter for property andTriggerIds.
    * @return Value of property andTriggerIds.
    *
    */
   public Integer[] getAndTriggerIds() {
       return andTriggerIds;
   }

   /** Setter for property andTriggerIds.
    * @param andTriggerIds New value of property andTriggerIds.
    *
    */
   public void setAndTriggerIds(Integer[] val) {
       andTriggerIds = val;
   }

    /** Getter for property orTriggerIds.
    * @return Value of property orTriggerIds.
    *
    */
   public Map getOrTriggerIds() {
       return orTriggerIds;
   }
   
   /** Setter for property orTriggerIds.
    * @param orTriggerIds New value of property orTriggerIds.
    *
    */
   public void setTriggerIds(Map val) {
       orTriggerIds = val;
   }

    /** Getter for property timeRange.
     * @return Value of property timeRange.
     *
     */
    public long getTimeRange() {
        return timeRange;
    }
    
    /** Setter for property timeRange.
     * @param timeRange New value of property timeRange.
     *
     */
    public void setTimeRange(long val) {
        timeRange = val;
    }
    
    /**
     * Returns the durable.
     * @return boolean
     */
    public boolean isDurable() {
        return durable;
    }

    /**
     * Sets the durable.
     * @param durable The durable to set
     */
    public void setDurable(boolean val) {
        durable = val;
    }
    
    public int hashCode() {
    	
    	int hash = 12;
    	hash = 37 * hash + MultiConditionTrigger.class.hashCode();
    	hash = 37 * hash + getId().hashCode();
    	
    	return hash;
    }
    
    protected void fire(Collection fulfilled, EventTrackerLocal etracker) {
        try {
            // Fire actions using the target event.
            TriggerFiredEvent target =
            	prepareTargetEvent(fulfilled, etracker);
            super.fireActions(target);
        } catch (Exception e) {
            log.error("Error creating alert: ", e);
        }            
    }
        
    private TriggerFiredEvent prepareTargetEvent(Collection fulfillingEvents, 
                                                 EventTrackerLocal etracker) 
        throws ActionExecuteException {
              
        if (!durable) {
            // Get ready to fire, reset EventTracker
            try {
    			etracker.deleteReference(getId());
    			if (log.isDebugEnabled()) {
    				log.debug("MultiConditionTrigger trigger id=" + getId() +
    						" deleting references");
    			}
      
            } catch (SQLException e) {
            	// Log the error, but we still want to fire
            	log.error("Failed to delete reference for trigger id=" +
            			   getId(), e);
            }
        }                
        
        // Message string which tracks the return message
        StringBuffer message = new StringBuffer();
        for (Iterator iter = fulfillingEvents.iterator(); iter.hasNext(); ) {
            AbstractEvent tracked = (AbstractEvent) iter.next();
            if (tracked instanceof TriggerFiredEvent) {
                message.append(tracked);
                message.append("\n");
            }
        }
        
        // Get the events that fulfilled this trigger
        AbstractEvent[] nested = (AbstractEvent[])
            fulfillingEvents.toArray(
                new AbstractEvent[fulfillingEvents.size()]);
        
        TriggerFiredEvent target = new TriggerFiredEvent(getId(), nested);

        // Set the message
        target.setMessage(message.toString());

        return target;
    }
    
    LinkedList getPersistedReferencedEvents(EventTrackerLocal etracker) {
		// Events not in memory, must get from serialized stream.
		// XXX is this only significant on startup or in an HA environment?
		// Seems like once this class starts processing events, the only time
		// the collection would be empty is immediately after a fire, and
		// then the referenced streams would also be empty.  The only exception
		// is in a failover (HA) condition.
    	
    	LinkedList result = new LinkedList();
    	
		try {
			List eventObjectDesers =
				etracker.getReferencedEventStreams(getId());
			if (log.isDebugEnabled()) {
				log.debug("Get prior events for trigger id="+getId());
			}

			for (Iterator iter = eventObjectDesers.iterator();
				 iter.hasNext(); ) {
				
				// Go through the list in order, first to last.  For any TriggerNotFiredEvent,
				// remove any previous TriggerFiredEvents for the same condition (instanceId),
				// and then discard the event
				EventObjectDeserializer deser =
					(EventObjectDeserializer) iter.next();
				result.add(deserializeEvent(deser, true));
			}
		} catch (Exception exc) {
			log.error("Internal error getting referenced streams for trigger id=" +
					  getId() + ", some events may have been dropped", exc);
		}
		
		return result;
    }    
}
