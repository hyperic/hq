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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.FlushStateEvent;
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
    private final Log log = LogFactory.getLog(MultiConditionTrigger.class);

    public static final String CFG_TRIGGER_IDS = "triggerIds";
    public static final String CFG_TIME_RANGE  = "timeRange";
    public static final String CFG_DURABLE     = "durable";

    public static final String AND = "&";
    public static final String OR  = "|";
    
    protected static class LastFulfillingEventsState {
    	List events;
    	long fireStamp;
    	
    	public LastFulfillingEventsState(List init) {
    		events = init;
    		fireStamp = 0;
		}
    }
    
    protected final LastFulfillingEventsState lastFulfillingEvents =
    	new LastFulfillingEventsState(new ArrayList());

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
    	for (int i = orInd; i < subIds.length; i++) {
    		// Did not fulfill yet
    		if (!fulfilled.contains(subIds[i])) {
    			return false;
    		}
    	}

    	return true;            
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
     * Process an event from the dispatcher.
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
        
        long fireStamp;
        synchronized (lastFulfillingEvents) {
        	fireStamp = lastFulfillingEvents.fireStamp;
        }
        
        EventTrackerLocal etracker = null;
        
        try {
            etracker = EventTrackerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new ActionExecuteException("Failed to evaluate multi condition " +
                                              "trigger id="+getId(), e);
        }
        
        boolean shouldFire = false;
        List fulfilled = addNewEvent(event, etracker, fireStamp);
        if (fulfilled != null) {
        	// Enough events to fire, check to see if we really need to
        	long newStamp;
        	synchronized (lastFulfillingEvents) {
        		newStamp = lastFulfillingEvents.fireStamp;
            	if (newStamp == fireStamp) {
            		// Looks like we have the baton.  Go ahead and fire.
            		// Update the fireStamp so no other thread will try to fire
            		// based on the same conditions
            		lastFulfillingEvents.fireStamp++;
            		
            		// Must clear out the events from this thread, but we don't want to
            		// clear out events that came in later.  The lists fulfilled and
            		// lastFulfillingEvents.events should be the same up to the length
            		// of fulfilled.
            		if (lastFulfillingEvents.events.size() == fulfilled.size()) {
            			lastFulfillingEvents.events.clear();
            		} else {
            			List tempList =
            				new ArrayList(lastFulfillingEvents.events.subList(fulfilled.size(),
            																  lastFulfillingEvents.events.size()));
            			lastFulfillingEvents.events = tempList;
            		}
            		shouldFire = true;
            	}
        	}
        }
                
        if (shouldFire) {
            
            try {
                // Fire actions using the target event
                TriggerFiredEvent target =
                	prepareTargetEvent(fulfilled, etracker);
                super.fireActions(target);
            } catch (AlertCreateException e) {
                throw new ActionExecuteException(e);
            } catch (ActionExecuteException e) {
                throw new ActionExecuteException(e);
            } catch (SystemException e) {
                throw new ActionExecuteException(e);
            }            
        }
    }
    
    /**
     * Check if the new event fulfills the triggering conditions.  Contract:
     * The number of events needed to fulfill a MultiConditionTrigger is
     * at least 2.
     * 
     * @param event The new event.
     * @param etracker The event tracker.
     * @param fireStamp The incident stamp for event firing, used to short-circuit
     * @return true if the new event fulfilled all the conditions of the trigger
     * @throws ActionExecuteException
     */    
    protected List addNewEvent(AbstractEvent event,
    								 EventTrackerLocal etracker,
    								 long fireStamp)
        throws ActionExecuteException {        

    	boolean checkCompletion = false;
    	List result = null;
    	AbstractEvent toDelete = null;
        List events = getEventsForTrigger(event, etracker, fireStamp);

        // Create a table to keep track
    	Map fulfilled = new LinkedHashMap();

        // A null value for events is a sentinel meaning abort
        if (events != null) {
        	
        	// If there are no prior events, then this new event cannot possibly
        	// fulfill the conditions.  Since the new event is already in the events
        	// Collection, the size must be greater than 1 for the conditions
        	// to be fulfilled
        	if (events.size() > 1) {

        		for (Iterator iter = events.iterator(); iter.hasNext(); ) {
        			AbstractEvent tracked = (AbstractEvent) iter.next();

        			// If this tracked event equals the new event, then
        			// the old one is obsolete
        			if (tracked != event &&
        					tracked.getInstanceId().equals(event.getInstanceId())) {
        				toDelete = tracked;
        				continue;
        			}

        			if (tracked instanceof TriggerFiredEvent) {
        				fulfilled.put(tracked.getInstanceId(), tracked);
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
        				notFired();
        			}
        		}

        		// If we've got nothing, then just clean up
        		if (fulfilled.size() == 0) {
        			if (events.size() > 1) {
        				try {
        					etracker.deleteReference(getId());            
        				} catch (SQLException e) {
        					// It's ok if we can't delete the old events now.
        					// We can do it next time.
        					log.warn("Failed to remove all references to trigger id=" +
        							getId(), e);                
        				}
        			}
        		} else {
        			// Continue on to see if we should fire
        			checkCompletion = true;
        		}
        	}

        	if (checkCompletion) {

        		if (!triggeringConditionsFulfilled(fulfilled.values())) {

        			try {
        				// Clean up unused event
        				if (toDelete != null) {
        					// Only need to update reference if event may expire
        					if (getTimeRange() > 0) {
        						try {
        							etracker.updateReference(getId(), toDelete.getId(),
        									event, getTimeRange());
        						} catch (SQLException e) {
        							log.debug("Failed to update event reference for " +
        									"trigger id=" + getId(), e);
        							etracker.addReference(getId(), event, getTimeRange());
        						}
        					}
        				} else if (event instanceof TriggerFiredEvent) {
        					// Only need track TriggerFiredEvent
        					etracker.addReference(getId(), event, getTimeRange());
        				}          
        			} catch (SQLException e) {
        				log.error("Failed to add event reference for trigger id=" +
        						getId(), e);
        			}            
        		} else {
        			// fulfilled!
        			result = events;
        		}
        	}
        }
        
        return result;
    }
    
    /**
     * Get events that interest this trigger, including the new one.
     * Contract: A null return is a sentinel value indicating that
     * this processing should abort due to the
	 * trigger firing during event processing.
     * 
     * @param event      The current event
     * @param etracker   EventTracker
     * @param fireStamp  Identity of last trigger fire incident, used for short-circuiting
     * @return           A list of all prior events, size may be zero but the list will not be null.
     * @throws ActionExecuteException
     */
    private List getEventsForTrigger(AbstractEvent event,
    									  EventTrackerLocal etracker,
    									  long fireStamp) 
        throws ActionExecuteException {
        List events = null;
    	boolean abort = false;

        // Potentially long monitor hold....
        synchronized (lastFulfillingEvents) {
        	if (lastFulfillingEvents.fireStamp > fireStamp) {
        		abort = true;
        	} else if (lastFulfillingEvents.events.isEmpty()) {
        		try {
        			Collection eventObjectDesers =
        				etracker.getReferencedEventStreams(getId());
        			if (log.isDebugEnabled()) {
        				log.debug("Get prior events for trigger id="+getId());
        			}

        			for (Iterator iter = eventObjectDesers.iterator();
        			iter.hasNext(); ) {
        				EventObjectDeserializer deser =
        					(EventObjectDeserializer) iter.next();
        				lastFulfillingEvents.events.add(deserializeEvent(deser, true));
        			}
        		} catch(Exception exc) {
        			throw new ActionExecuteException(
        					"Failed to get referenced streams for trigger id=" +getId(),
        					exc);
        		}
        	}

        	lastFulfillingEvents.events.add(event);
        	events = new ArrayList();
        	long expire = (getTimeRange() > 0 ?
        			System.currentTimeMillis() - getTimeRange() : 0);

        	// Like Collection.addAll(), but with an expiration filter, and no optimization
        	for (Iterator it = lastFulfillingEvents.events.iterator(); it.hasNext(); ) {
        		AbstractEvent mayBeExpired = (AbstractEvent) it.next();
        		if (mayBeExpired.getTimestamp() <= expire) {
        			it.remove();
        		} else {
        			events.add(mayBeExpired);
        		}
        	}
        }

        return events;
    }
    
    private TriggerFiredEvent prepareTargetEvent(Collection fulfillingEvents, 
                                                 EventTrackerLocal etracker) 
        throws ActionExecuteException {
              
        if (!durable) {
            // Get ready to fire, reset EventTracker
            try {
                etracker.deleteReference(getId());
            } catch (SQLException e) {
                throw new ActionExecuteException(
                        "Failed to delete reference for trigger id="+getId(), e);
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
    
    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes() {
        return new Class[] { TriggerFiredEvent.class,
                             TriggerNotFiredEvent.class,
                             FlushStateEvent.class };
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

}
