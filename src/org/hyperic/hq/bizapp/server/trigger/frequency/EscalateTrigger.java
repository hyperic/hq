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
 * EscalateTrigger.java
 *
 * Created on October 21, 2002, 4:06 PM
 */

package org.hyperic.hq.bizapp.server.trigger.frequency;

import java.sql.SQLException;
import java.util.LinkedList;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.shared.EventObjectDeserializer;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.LongConfigOption;

/** The EscalateTrigger is a specialized trigger that can combine multiple
 * conditions and only fire actions when all conditions have been met
 *
 */
public class EscalateTrigger extends AbstractTrigger {
    
    private static String CFG_WATCH      = "watch";
    private static String CFG_AFTER      = "after";
    private static String CFG_TIME_RANGE = "timeRange";

    private final Object lock = new Object();
    
    /** Holds value of property after. */
    private long after;
    
    /** Holds value of property timeRange. */
    private long timeRange;
    
    /** Holds value of property watchId. */
    private Integer watchId;
    
    /** Creates a new instance of EscalateTrigger */
    public EscalateTrigger() {
    }

    /** Process an event from the dispatcher.
     * @param event the Event to process
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if an action throws an exception
     *
     */
    public void processEvent(AbstractEvent event)
        throws EventTypeException, ActionExecuteException {

        // Make sure this event fits our criteria
        if (!(event instanceof TriggerFiredEvent &&
              watchId.equals(event.getInstanceId()))) {
            return;
        }

        TriggerFiredEvent tfe = null;

        try {
            synchronized (lock) {
                EventTrackerLocal eTracker =
                    EventTrackerUtil.getLocalHome().create();

                TriggerFiredEvent tracked = null;
                
                try {
                    // Now get the references
                    LinkedList eventObjectDesers =
                        eTracker.getReferencedEventStreams(getId());

                    // Check to see if we need to fire
                    if (eventObjectDesers.size() > 0) {
                        // Only need to look at the first event                    
                        EventObjectDeserializer deser = 
                            (EventObjectDeserializer) eventObjectDesers.getFirst();
                        
                        tracked = 
                            (TriggerFiredEvent) deserializeEvent(deser, true);

                        if ((tracked.getTimestamp() + getAfter()) >
                            event.getTimestamp())
                            tracked = null;         // Not valid
                    }
                } catch(Exception exc) {
                    throw new ActionExecuteException(
                        "Failed to get referenced streams for trigger id="+
                         getId(), exc);
                }
                
                
                

                if (tracked == null) {          // Not firing
                    // We'll have to track this event
                    try {
                        eTracker.addReference(getId(), event, 0);                       
                    } catch (SQLException e) {
                        throw new ActionExecuteException(
                                "Failed to add event reference for trigger id="+
                                getId(), e);                            
                    }
                    
                    
                } else {                        // Fire
                    tfe = tracked;
                    tfe.setInstanceId(getId());

                    // Get ready to fire, reset EventTracker                    
                    try {
                        eTracker.deleteReference(getId());                          
                    } catch (SQLException exc) {
                        throw new ActionExecuteException(
                                "Failed to delete event references for trigger id="+
                                getId(), exc);                  
                    }
                }
            }
        } catch (NamingException e) {
            return; // No fire since we can't track the events
        } catch (CreateException e) {
            return; // No fire since we can't track the events
        }

        if (tfe != null) {
            try {
                super.fireActions(tfe);
            } catch (AlertCreateException e) {
                throw new ActionExecuteException(e);
            } catch (ActionExecuteException e) {
                throw new ActionExecuteException(e);
            } catch (SystemException e) {
                throw new ActionExecuteException(e);
            }
        }
        else {
            // Now send a NotFired event
            notFired();
        }
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        ConfigSchema res = new ConfigSchema();

        res.addOption(new IntegerConfigOption(CFG_WATCH, "Watch trigger ID",
                                              new Integer(0)));
        res.addOption(new LongConfigOption(CFG_AFTER, "Escalate after (sec)",
                                           new Long(0)));
        res.addOption(new LongConfigOption(CFG_TIME_RANGE, "But within (sec)",
                                           new Long(0)));
        return res;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes() {
        return new Class[] { TriggerFiredEvent.class };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c) {
        // Ask the sub triggers what they are interested in
        if (c.equals(TriggerFiredEvent.class))
            return new Integer[] { watchId };
        
        return null;
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
            String swatch = triggerData.getValue(CFG_WATCH);
            String safter = triggerData.getValue(CFG_AFTER);
            String srange = triggerData.getValue(CFG_TIME_RANGE);

            setWatchId  (new Integer(swatch));
            setAfter    (Long.parseLong(safter) * 1000);    // Turn into ms
            setTimeRange(Long.parseLong(srange) * 1000);    // Turn into ms
        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        } catch (NumberFormatException e) {
            throw new InvalidTriggerDataException(e);
        }
    }

    /** Getter for property after.
     * @return Value of property after.
     *
     */
    public long getAfter() {
        return after;
    }
    
    /** Setter for property after.
     * @param after New value of property after.
     *
     */
    public void setAfter(long val) {
        after = val;
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
    
    /** Getter for property watchId.
     * @return Value of property watchId.
     *
     */
    public Integer getWatchId() {
        return watchId;
    }
    
    /** Setter for property watchId.
     * @param watchId New value of property watchId.
     *
     */
    public void setWatchId(Integer val) {
        watchId = val;
    }
    
}
