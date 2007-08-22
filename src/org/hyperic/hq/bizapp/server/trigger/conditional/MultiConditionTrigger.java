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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.HeartBeatEvent;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisteredTriggers;
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
    extends AbstractTrigger
    implements RegisterableTriggerInterface {
    private final Log log = LogFactory.getLog(MultiConditionTrigger.class);

    public static final String CFG_TRIGGER_IDS = "triggerIds";
    public static final String CFG_TIME_RANGE  = "timeRange";
    public static final String CFG_DURABLE     = "durable";

    public static final String AND = "&";
    public static final String OR  = "|";

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

    /** Hold the value of last time all conditions were fulfilled */
    private long fulfilledTime = 0;
    
    /** Creates a new instance of MultiConditionTrigger */
    public MultiConditionTrigger() {
    }

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

    /** Initialize the trigger with a value object.
     *
     * @param triggerData  Configuration data for the trigger
     *
     * @throws org.hyperic.hq.bizapp.server.trigger.InvalidTriggerDataException indicating that the triggerData
     *                                     was invalid.
     *
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
 
    /** Process an event from the dispatcher.
     * @param event the Event to process
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if an action throws an exception
     *
     */
    public void processEvent(AbstractEvent event)
        throws EventTypeException, ActionExecuteException {
        // If we didn't fulfill the condition, then don't fire
        if(!(event instanceof TriggerFiredEvent ||
             event instanceof TriggerNotFiredEvent ||
             event instanceof HeartBeatEvent))
            throw new EventTypeException(
                "Invalid event type passed, expected TriggerFiredEvent " +
                "or TriggerNotFiredEvent or HeartBeatEvent");
        
        EventTrackerLocal etracker;
        try {
            etracker = EventTrackerUtil.getLocalHome().create();
        } catch (NamingException e) {
            return; // No fire since we can't track the events
        } catch (CreateException e) {
            return; // No fire since we can't track the events
        }
        
        // Create a table to keep track
        HashMap fulfilled = new HashMap();

        // We would only fire if we previously evaluated to fire
        if (event instanceof HeartBeatEvent) {
            // Check to make sure it's been at least 15 seconds since we decided
            // to fire.  If conditions changed, we won't fire incorrectly
            if (fulfilledTime > 0 &&
                event.getTimestamp() > fulfilledTime + 15000) {
                fulfilledTime = 0;

                ArrayList events = new ArrayList();
                try {
                    Collection streams =
                        etracker.getReferencedEventStreams(getId());
                    if (log.isDebugEnabled())
                        log.debug("Get events to fire for " + getId() + " at " +
                                  event);

                    for (Iterator iter = streams.iterator();
                         iter.hasNext(); ) {
                        ObjectInputStream p =
                            (ObjectInputStream) iter.next();                        
                        events.add(deserializeEventFromStream(p, true));
                    }

                    if (!durable) {
                        // Get ready to fire, reset EventTracker
                        etracker.deleteReference(getId());
                    }
                } catch (IOException e) {
                    return; // No fire since we can't track the events
                } catch (ClassNotFoundException e) {
                    return; // No fire since we can't track the events
                } catch (SQLException e) {
                    return; // No fire since we can't track the events
                }
                
                // Message string which tracks the return message
                StringBuffer message = new StringBuffer();
                for (Iterator iter = events.iterator(); iter.hasNext(); ) {
                    AbstractEvent tracked = (AbstractEvent) iter.next();
                    if (tracked instanceof TriggerFiredEvent) {
                        fulfilled.put(tracked.getInstanceId(), tracked);
                        message.append(event);
                        message.append("\n");
                    }
                }

                try {
                    // Get the events that fulfilled this trigger
                    AbstractEvent[] nested = (AbstractEvent[])
                        fulfilled.values().toArray(
                            new AbstractEvent[fulfilled.size()]);
                    TriggerFiredEvent target =
                        new TriggerFiredEvent(getId(), nested);

                    // Set the message
                    target.setMessage(message.toString());

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
            
            return;
        }
        
        // Find out which instance we should be looking for
        try {
            Integer[] subIds = getAndTriggerIds();
            Map orIds = getOrTriggerIds();
            
            AbstractEvent toDelete = null;
            int orInd = 0;
    
            // First, see if we got lucky on some OR condition
            if (event instanceof TriggerFiredEvent &&
                orIds.containsKey(event.getInstanceId())) {
                // See if it's the last one
                Integer index = (Integer) orIds.get(event.getInstanceId());
                orInd = index.intValue();
            }

            // Now see if we have a chance of fulfilling the conditions
            if (orInd == subIds.length) {
                // We got it!
                fulfilled.put(event.getInstanceId(), event);
            }
            else {
                // Look backwards and see if we can fire
                Collection streams =
                    etracker.getReferencedEventStreams(getId());
                if (log.isDebugEnabled())
                    log.debug("Get events to fulfill for " + getId() + " at " +
                              event);

                ArrayList events = new ArrayList();
                for (Iterator iter = streams.iterator(); iter.hasNext(); ) {
                    ObjectInputStream p = (ObjectInputStream) iter.next();
                    events.add(deserializeEventFromStream(p, true));
                }

                // Now add the new event, too
                events.add(event);

                for (Iterator iter = events.iterator(); iter.hasNext(); ) {
                    AbstractEvent tracked = (AbstractEvent) iter.next();
                    
                    // If this tracked event equals the new event, then
                    // the old one is obsolete
                    if (tracked != event &&
                        tracked.getInstanceId().equals(
                        event.getInstanceId())) {
                        toDelete = tracked;
                        continue;
                    }
                    
                    if (tracked instanceof TriggerFiredEvent) {
                        fulfilled.put(tracked.getInstanceId(), tracked);
                    }
                    else {
                        // Well, we know we ain't firing
                        notFired();
                        fulfilled.remove(tracked.getInstanceId());
                    }
                }

                // If we've got nothing, then just clean up
                if (fulfilled.size() == 0) {
                    etracker.deleteReference(getId());
                    fulfilledTime = 0;
                    return;
                }
                
                // Now let's see how well we did
                for (Iterator i = orIds.keySet().iterator();
                    i.hasNext(); ) {
                    Object orId = i.next();
                    if (fulfilled.containsKey(orId)) {
                        Integer index = (Integer) orIds.get(orId);
                        if (orInd < index.intValue()) {
                            orInd = index.intValue();
                        }
                    }
                }
            }

            // Assume we might be able to fire
            fulfilledTime = event.getTimestamp();

            // Go through the subIds
            for (int i = orInd; i < subIds.length; i++) {
                // Did not fulfill
                if (!fulfilled.containsKey(subIds[i])) {
                    fulfilledTime = 0;
                    break;
                }
            }

            // Clean up unused event
            if (toDelete != null) {
                if (getTimeRange() > 0) {
                    etracker.updateReference(getId(),
                                             toDelete.getId(),
                                             event, getTimeRange());
                }
            } else {
                etracker.addReference(getId(),
                                      event, getTimeRange());
            }
        } catch (IOException e) {
            return; // No fire since we can't track the events
        } catch (ClassNotFoundException e) {
            return; // No fire since we can't track the events
        } catch (SQLException e) {
            return; // No fire since we can't track the events
        }
    }

    /** Get the event classes that the trigger is interested in
     * seeing.  This is an optimization, so that a trigger's
     * processEvent() method is called only when a valid event
     * occurs.
     *
     * @return an array of Class objects which implement
     *          the 'Event' interface
     *
     */
    public Class[] getInterestedEventTypes() {
        return new Class[] { TriggerFiredEvent.class,
                             TriggerNotFiredEvent.class,
                             HeartBeatEvent.class };
    }

    /** Get a list of instance IDs specific to a class (as returned
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
    public Integer[] getInterestedInstanceIDs(Class c) {
        if (c.equals(HeartBeatEvent.class)) {
            // Want all of heartbeats
            return new Integer[] { RegisteredTriggers.KEY_ALL };
        }
        
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
