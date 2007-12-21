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
 * AvertTrigger.java
 *
 * Created on October 21, 2002, 4:06 PM
 */

package org.hyperic.hq.bizapp.server.trigger.frequency;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.HeartBeatEvent;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.events.shared.EventObjectDeserializer;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.LongConfigOption;

/** The AvertTrigger is a specialized trigger that can combine multiple
 * conditions and only fire actions when all conditions have been met
 *
 */
public class AvertTrigger extends AbstractTrigger {
    
    public static String CFG_INIT_TRIGGER  = "initTrigger";
    public static String CFG_AVERT_TRIGGER = "avertTrigger";
    public static String CFG_TIME_RANGE    = "timeRange";

    /** Holds value of property lock. */
    private Object lock = new Object();

    /** Holds value of property timeRange. */
    private long timeRange;
    
    /** Holds value of property initId. */
    private Integer initId;
    
    /** Holds value of property avertId. */
    private Integer avertId;
    
    /** Creates a new instance of AvertTrigger */
    public AvertTrigger() {
    }

    /** Process an event from the dispatcher.
     * @param event the Event to process
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if an action throws an exception
     *
     */
    public void processEvent(AbstractEvent event)
        throws EventTypeException, ActionExecuteException {

        TriggerFiredEvent tfe = null;

        try {
            EventTrackerLocal etracker =
                EventTrackerUtil.getLocalHome().create();

            synchronized (lock) {
                if (event instanceof TriggerFiredEvent) {
                    // Get rid of the references, we've averted
                    if (getAvertId().equals(event.getInstanceId())) {
                        
                        try {
                            etracker.deleteReference(getId());                            
                        } catch (SQLException e) {
                            throw new ActionExecuteException(
                                    "Failed to delete reference for trigger id="+getId(), e);                            
                        }

                        // Send a NotFired event
                        notFired();
                        return;
                    } else if (!getInitId().equals(event.getInstanceId())) {
                        // We don't do anything
                        return;
                    }
                }

                // Now get the references
                LinkedList eventObjectDesers = null;
                
                try {
                    eventObjectDesers = etracker.getReferencedEventStreams(getId());    
                } catch (Exception e) {
                    throw new ActionExecuteException(
                            "Failed to get referenced event streams for trigger id="+
                            getId(), e);   
                }
                
                if (event instanceof HeartBeatEvent) {
                    // Check to see if we need to fire
                    if (eventObjectDesers.size() > 0) {
                        HeartBeatEvent heartbeat = (HeartBeatEvent) event;
                        
                        // We only need the first event
                        EventObjectDeserializer deser =
                            (EventObjectDeserializer) eventObjectDesers.getFirst();

                        TriggerFiredEvent tracked =
                            (TriggerFiredEvent) deserializeEvent(deser, true);        

                        // Check to see if enough time has elapsed
                        if ((tracked.getTimestamp() + getTimeRange()) <
                            heartbeat.getBeat().getTime()) {
                            tfe = tracked;
                            // Reset the instance ID to this trigger to fire
                            tfe.setInstanceId(getId());

                            // Get ready to fire, reset EventTracker
                            try {
                                etracker.deleteReference(getId());
    
                            } catch (SQLException e) {
                                throw new ActionExecuteException(
                                        "Failed to delete reference for trigger id="+
                                        getId(), e);
                            }
                        }
                    }
                } else {
                    // Track it if we don't have any events yet
                    if (eventObjectDesers.size() == 0) {
                        
                        try {
                            etracker.addReference(getId(), event, 0);                            
                        } catch (SQLException e) {
                            throw new ActionExecuteException(
                                    "Failed to add event reference for trigger id="+
                                    getId(), e);                            
                        }
                        
                    }
                }
            }
        } catch (IOException e) {
            return; // No fire since we can't track the events
        } catch (ClassNotFoundException e) {
            return; // No fire since we can't track the events
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
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        ConfigSchema res = new ConfigSchema();
        IntegerConfigOption init, avert;
        LongConfigOption range;

        init = new IntegerConfigOption(CFG_INIT_TRIGGER,
                                       "If trigger(ID) met ", new Integer(0));

        range = new LongConfigOption(CFG_TIME_RANGE,
                                     "Fire after (sec)",
                                     new Long(0));

        avert = new IntegerConfigOption(CFG_AVERT_TRIGGER,
                                        "Unless trigger(ID) occurs",
                                        new Integer(0));

        res.addOption(init);
        res.addOption(range);
        res.addOption(avert);
        return res;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes() {
        return new Class[] { TriggerFiredEvent.class, HeartBeatEvent.class };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c) {
        // Ask the sub triggers what they are interested in
        if (c.equals(TriggerFiredEvent.class)) {
            return new Integer[] { initId, avertId };
        } else if (c.equals(HeartBeatEvent.class)) {
            // Want all of heartbeats
            return new Integer[] { RegisteredTriggers.KEY_ALL };
        }
        
        return null;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#init(org.hyperic.hq.events.shared.RegisteredTriggerValue)
     */
    public void init(RegisteredTriggerValue tval)
        throws InvalidTriggerDataException {
        // Set from the trigger value
        setTriggerValue(tval);

        try {
            ConfigResponse triggerData = ConfigResponse.decode(tval.getConfig());
            String sinit  = triggerData.getValue(CFG_INIT_TRIGGER);
            String srange = triggerData.getValue(CFG_TIME_RANGE);
            String savert = triggerData.getValue(CFG_AVERT_TRIGGER);

            setInitId(new Integer(sinit));
            setAvertId(new Integer(savert));
            
            long range = Long.parseLong(srange);
            setTimeRange(range * 1000);         // Turn into milliseconds
        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        } catch (NumberFormatException e) {
            throw new InvalidTriggerDataException(e);
        }
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
    
    /** Getter for property initId.
     * @return Value of property initId.
     *
     */
    public Integer getInitId() {
        return initId;
    }
    
    /** Setter for property initId.
     * @param initId New value of property initId.
     *
     */
    public void setInitId(Integer val) {
        initId = val;
    }
    
    /** Getter for property avertId.
     * @return Value of property avertId.
     *
     */
    public Integer getAvertId() {
        return avertId;
    }
    
    /** Setter for property avertId.
     * @param avertId New value of property avertId.
     *
     */
    public void setAvertId(Integer val) {
        avertId = val;
    }
    
}
