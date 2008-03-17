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
import java.util.StringTokenizer;

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

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;

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
    
    private final Object lock = new Object();
    
    private final Object lastFulfillingEventsLock = new Object();

    private final Map currentSharedLockHolders = Collections.synchronizedMap(new HashMap());
        
    // make the lock reentrant just to be safe in preventing deadlocks
    private final ReadWriteLock rwLock = new ReentrantWriterPreferenceReadWriteLock();
    
    private static final ThreadLocal IGNORE_NEXT_EXCLUSIVE_LOCK_RELEASE = 
        new ThreadLocal() {
            protected Object initialValue() {
                return Boolean.FALSE;
            }
        };
       
    private List lastFulfillingEvents = Collections.EMPTY_LIST;

    /** Holds value of property triggerIds. */
    private HashSet triggerIds;

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
     * Acquire the shared lock for processing events. A best effort is made 
     * to reacquire the lock even if the thread is interrupted.
     * 
     * @throws InterruptedException 
     */
    public void acquireSharedLock() throws InterruptedException {
        // We handle the interrupted state since users acquiring a shared 
        // lock should be able to do so to process events.
        boolean acquired = false;
        int counter = 0;
        
        while (!acquired && counter < 10) {
            try {
                rwLock.readLock().acquire();
                acquired = true;
            } catch (InterruptedException e) {
                // interrupted state is cleared - retry
                counter++;
            }            
        }     
        
        if (acquired) {
            currentSharedLockHolders.put(Thread.currentThread(), new Date());
            
            if (log.isDebugEnabled()) {
                log.debug(Thread.currentThread()+ 
                          " currently holds shared lock on trigger id="+getId());            
            }                         
        } else {
            throw new InterruptedException("thread was interrupted attempting " +
                               "to acquire shared lock: "+Thread.currentThread());           
        }
    }
    
    /**
     * Release the shared lock for processing events.
     */
    public void releaseSharedLock() {
        try {
            rwLock.readLock().release();            
        } finally {
            currentSharedLockHolders.remove(Thread.currentThread());
            
            if (log.isDebugEnabled()) {
                log.debug(Thread.currentThread()+ 
                          " currently released shared lock on trigger id="+getId());            
            }            
        }        
    }
    
    /**
     * Retrieve the map of Thread objects -> lock acquisition time currently 
     * holding the shared lock.
     * 
     * @return The map of Thread objects to their lock acquisition time.
     */
    public Map getCurrentSharedLockHolders() {
        Map lockHolders = null;

        synchronized (currentSharedLockHolders) {
            lockHolders = new HashMap(currentSharedLockHolders);
        }

        return lockHolders;
    }

    /**
     * Check if the triggering conditions have been fulfilled, meaning 
     * the state should be flushed.
     * 
     * @return <code>true</code> if the triggering conditions have been fulfilled; 
     *         <code>false</code> if not.
     */
    public boolean triggeringConditionsFulfilled() {
        synchronized (lastFulfillingEventsLock) {
            return !lastFulfillingEvents.isEmpty();            
        }
    }    
    
    /**
     * Release the shared lock and attempt to upgrade to an exclusive lock 
     * for processing events. If the exclusive lock is acquired, it must 
     * be released before others may process events. This method has the 
     * same semantics as calling {@link #releaseSharedLock() releaseSharedLock()} 
     * followed by {@link #attemptExclusiveLock() attemptExclusiveLock()}.
     * 
     * @return <code>true</code> if the exclusive lock has been acquired; 
     *          <code>false</code> otherwise, meaning some other thread 
     *          either has a shared lock or has already acquired the 
     *          exclusive lock.
     * @throws InterruptedException if the thread is interrupted while 
     *                              attempting to acquire the exclusive lock.
     */
    public boolean upgradeSharedLockToExclusiveLock() throws InterruptedException {
        releaseSharedLock();
        return attemptExclusiveLock();
    }
    
    /**
     * Attempt to acquire the exclusive lock for processing events.
     * 
     * @return <code>true</code> if the exclusive lock has been acquired; 
     *          <code>false</code> otherwise, meaning some other thread 
     *          either has a shared lock or has already acquired the 
     *          exclusive lock.
     * @throws InterruptedException
     */
    public boolean attemptExclusiveLock() throws InterruptedException {
        return rwLock.writeLock().attempt(0);        
    }
    
    /**
     * Release the exclusive lock for processing events.
     */
    public void releaseExclusiveLock() {
        if (ignoreNextExclusiveLockRelease()) {
            resetIgnoreNextExclusiveLockRelease();
        } else {
            rwLock.writeLock().release();            
        }
    }
    
    private void setIgnoreNextExclusiveLockRelease() {
        IGNORE_NEXT_EXCLUSIVE_LOCK_RELEASE.set(Boolean.TRUE);
    }
    
    private boolean ignoreNextExclusiveLockRelease() {
        return ((Boolean)IGNORE_NEXT_EXCLUSIVE_LOCK_RELEASE.get()).booleanValue();
    }
    
    private void resetIgnoreNextExclusiveLockRelease() {
        IGNORE_NEXT_EXCLUSIVE_LOCK_RELEASE.set(Boolean.FALSE);    
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

            String delimiters = (AND + OR);
            StringTokenizer st = new StringTokenizer(stids, delimiters, true);
            triggerIds = new HashSet();
            orTriggerIds  = new HashMap();
            ArrayList andTrigIds = new ArrayList();
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
            setTimeRange(Long.parseLong(srange) * 1000);
            
            setDurable(Boolean.valueOf(sdurable).booleanValue());
        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        } catch (NumberFormatException e) {
            throw new InvalidTriggerDataException(e);
        }
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
        if(!(event instanceof TriggerFiredEvent ||
             event instanceof TriggerNotFiredEvent || 
             event instanceof FlushStateEvent))
            throw new EventTypeException(
                "Invalid event type passed: expected TriggerFiredEvent, " +
                "TriggerNotFiredEvent, or FlushStateEvent");
        
        EventTrackerLocal etracker = null;
        
        try {
            etracker = EventTrackerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new ActionExecuteException("Failed to evaluate multi condition " +
            		                          "trigger id="+getId(), e);
        }
        
        TriggerFiredEvent target = prepareTargetEventOnFlush(event, etracker);
                
        if (target != null) {
            this.releaseExclusiveLock();
            
            // HHQ-1791: We only need the exclusive lock when evaluating if 
            // we should fire actions. Release the lock before firing.
            this.setIgnoreNextExclusiveLockRelease();
            
            try {
                // Fire actions using the target event
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

    private TriggerFiredEvent prepareTargetEventOnFlush(AbstractEvent event,
                                                        EventTrackerLocal etracker)
            throws ActionExecuteException {
        
        TriggerFiredEvent target = null;
        
        synchronized (lock) {
            if (event instanceof FlushStateEvent) {
                if (triggeringConditionsFulfilled()) {
                    try {
                        // Since prepareTargetEvent iterates through the 
                        // lastFulfillingEvents, make a copy so we don't 
                        // have to synchronize on the list iteration.                        
                        List copyOfLastFulfillingEvents = null;
                        
                        synchronized (lastFulfillingEventsLock) {
                            copyOfLastFulfillingEvents = 
                                new ArrayList(lastFulfillingEvents);
                        }     
                        
                        target = prepareTargetEvent(copyOfLastFulfillingEvents, etracker);                        

                    } finally {
                        if (target != null) {
                            synchronized (lastFulfillingEventsLock) {
                                lastFulfillingEvents.clear();                                                            
                            }
                        }
                    }
                }                            
            } else {
                List tempLastFulfillingEvents = 
                    checkIfNewEventFulfillsConditions(event, etracker);
                
                synchronized (lastFulfillingEventsLock) {
                    lastFulfillingEvents = tempLastFulfillingEvents;                    
                }
            }            
        }
        
        return target;
    }
    
    /**
     * Check if the new event fulfills the triggering conditions.
     * 
     * @param event The new event.
     * @param etracker The event tracker.
     * @return The list of fulfilling events or an empty list. This list must 
     *          be thread-safe!
     * @throws ActionExecuteException
     */    
    private List checkIfNewEventFulfillsConditions(AbstractEvent event, 
                                                   EventTrackerLocal etracker) 
        throws ActionExecuteException {        
                              
        List events = getPriorEventsForTrigger(etracker);

        // Now add the new event, too
        events.add(event);
        
        // Create a table to keep track
        HashMap fulfilled = new LinkedHashMap();
        
        AbstractEvent toDelete = null;

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
                // Well, we know we ain't firing
                fulfilled.remove(tracked.getInstanceId());
                notFired();
            }
        }

        // If we've got nothing, then just clean up
        if (fulfilled.size() == 0 && events.size() > 0) {
            try {
                etracker.deleteReference(getId());            
            } catch (SQLException e) {
                // It's ok if we can't delete the old events now.
                // We can do it next time.
                log.warn("Failed to remove all references to trigger id="+getId(), e);                
            }
                        
            return Collections.EMPTY_LIST;
        }
        
        // Find out which instance we should be looking for
        Integer[] subIds = getAndTriggerIds();
        Map orIds = getOrTriggerIds();
        
        // Now let's see how well we did
        int orInd = 0;
        
        for (Iterator i = orIds.keySet().iterator(); i.hasNext(); ) {
            Object orId = i.next();
            if (fulfilled.containsKey(orId)) {
                Integer index = (Integer) orIds.get(orId);
                if (orInd < index.intValue()) {
                    orInd = index.intValue();
                }
            }
        }

        // Go through the subIds
        for (int i = orInd; i < subIds.length; i++) {
            // Did not fulfill yet
            if (!fulfilled.containsKey(subIds[i])) {
                fulfilled.clear();
                break;
            }
        }

        try {
            // Clean up unused event
            if (toDelete != null) {
                etracker.updateReference(toDelete.getId(), event);
            } else {
                etracker.addReference(getId(), event, getTimeRange());
            }          
        } catch (SQLException e) {
            throw new ActionExecuteException(
                    "Failed to update event references for trigger id="+getId(), e);
        }
        
        return new ArrayList(fulfilled.values());
    }
    
    private List getPriorEventsForTrigger(EventTrackerLocal etracker) 
        throws ActionExecuteException {
        List events = new ArrayList();
        
        try {
            Collection eventObjectDesers =
                etracker.getReferencedEventStreams(getId());
            if (log.isDebugEnabled())
                log.debug("Get prior events for trigger id="+getId());
        
            for (Iterator iter = eventObjectDesers.iterator(); iter.hasNext(); ) {
                EventObjectDeserializer deser = (EventObjectDeserializer) iter.next();
                events.add(deserializeEvent(deser, true));
            }
        } catch(Exception exc) {
            throw new ActionExecuteException(
                "Failed to get referenced streams for trigger id="+getId(), exc);
        }
        
        return events;
    }
    
    private TriggerFiredEvent prepareTargetEvent(List fulfillingEvents, 
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
        HashSet trigSet = getTriggerIds();
        return (Integer[]) trigSet.toArray(new Integer[trigSet.size()]);
    }

    /** Getter for property triggerIds.
    * @return Value of property triggerIds.
    *
    */
   public HashSet getTriggerIds() {
       return triggerIds;
   }

   /** Setter for property andTriggerIds.
    * @param triggerIds New value of property triggerIds.
    *
    */
   public void setTriggerIds(HashSet val) {
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
