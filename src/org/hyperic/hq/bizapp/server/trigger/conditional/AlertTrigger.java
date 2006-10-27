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
 * AlertTrigger.java
 *
 * Created on October 21, 2002, 4:06 PM
 */

package org.hyperic.hq.bizapp.server.trigger.conditional;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ConditionalTriggerSchema;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.EventTrackerLocal;
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/** The EscalateTrigger is a specialized trigger that can combine multiple
 * conditions and only fire actions when all conditions have been met
 *
 */
public class AlertTrigger extends AbstractTrigger
    implements RegisterableTriggerInterface, ConditionalTriggerInterface {
    static {
        // Register the trigger/condition
        ConditionalTriggerInterface.MAP_COND_TRIGGER.put(
            new Integer(EventConstants.TYPE_ALERT), AlertTrigger.class);
    }

    /** Holds value of property eTracker. */
    private EventTrackerLocal eTracker;

    /** Holds value of property after. */
    private long after;
    
    /** Holds value of property timeRange. */
    private long timeRange;
    
    /** Holds value of property watchId. */
    private Integer watchId;
    
    /** Creates a new instance of EscalateTrigger */
    public AlertTrigger() {
    }

    /** Process an event from the dispatcher.
     * @param event the Event to process
     * @throws org.hyperic.hq.events.ext.ActionExecuteException if an action throws an exception
     *
     */
    public void processEvent(AbstractEvent event)
        throws EventTypeException, ActionExecuteException {

        // Make sure this event fits our criteria
        if (!(event instanceof AlertFiredEvent &&
              watchId.equals(event.getInstanceId()))) {
            return;
        }

        try {
            TriggerFiredEvent tfe = new TriggerFiredEvent(getId(), event);            
            fireActions(tfe);
        } catch (AlertCreateException e) {
            throw new ActionExecuteException(e);
        } catch (ActionExecuteException e) {
            throw new ActionExecuteException(e);
        } catch (SystemException e) {
            throw new ActionExecuteException(e);
        }
    }

    public ConfigSchema getConfigSchema() {
        return ConditionalTriggerSchema
            .getConfigSchema(EventConstants.TYPE_ALERT);
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
        return new Class[] { AlertFiredEvent.class };
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
        // Ask the sub triggers what they are interested in
        if (c.equals(AlertFiredEvent.class))
            return new Integer[] { watchId };
        
        return null;
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
            String swatch = triggerData.getValue(CFG_ID);

            setWatchId  (new Integer(swatch));
        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        } catch (NumberFormatException e) {
            throw new InvalidTriggerDataException(e);
        }
    }

    /** Getter for property eTracker.
     * @return Value of property eTracker.
     *
     */
    public EventTrackerLocal getETracker()
        throws NamingException, CreateException {
        if (eTracker == null)
            eTracker = EventTrackerUtil.getLocalHome().create();

        return eTracker;
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

    /* (non-Javadoc)
     * @see org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface#getConfigResponse(org.hyperic.hq.appdef.shared.AppdefEntityID, org.hyperic.hq.events.shared.AlertConditionValue)
     */
    public ConfigResponse getConfigResponse(AppdefEntityID id,
                                            AlertConditionValue cond)
        throws InvalidOptionException, InvalidOptionValueException {
        if (cond.getType() != EventConstants.TYPE_ALERT)
            throw new InvalidOptionValueException(
                "Condition is not an Alert Fired");

        // We are using the measurement ID field for the alert definition ID
        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_ID, String.valueOf(cond.getMeasurementId()));
        
        return resp;
    }
    
}
