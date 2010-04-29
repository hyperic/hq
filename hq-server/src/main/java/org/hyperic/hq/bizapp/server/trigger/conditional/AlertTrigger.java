/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004, 2005, 2006],
 * Hyperic, Inc. This file is part of HQ. HQ is free software; you can
 * redistribute it and/or modify it under the terms version 2 of the GNU General
 * Public License as published by the Free Software Foundation. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

/*
 * AlertTrigger.java Created on October 21, 2002, 4:06 PM
 */

package org.hyperic.hq.bizapp.server.trigger.conditional;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ConditionalTriggerSchema;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * The AlertTrigger fires when an alert is fired from a specified alert definition.
 *
 */
public class AlertTrigger
    extends AbstractTrigger implements ConditionalTriggerInterface
{
    static {
        // Register the trigger/condition
        ConditionalTriggerInterface.MAP_COND_TRIGGER.put(new Integer(EventConstants.TYPE_ALERT), AlertTrigger.class);
    }

    /** Holds value of property timeRange. */
    private long timeRange;


    private Integer alertDefinitionId;

    /** Creates a new instance of AlertTrigger */
    public AlertTrigger() {
    }

    public void processEvent(AbstractEvent event) throws EventTypeException {
        if (!(event instanceof AlertFiredEvent)) {
            throw new EventTypeException("Invalid event type passed, expected AlertFiredEvent");
        }
        if(!(alertDefinitionId.equals(event.getInstanceId()))) {
            return;
        }
        TriggerFiredEvent tfe = prepareTriggerFiredEvent(event);
        fireActions(tfe);
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        return ConditionalTriggerSchema.getConfigSchema(EventConstants.TYPE_ALERT);
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes() {
        return new Class[] { AlertFiredEvent.class };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c) {
        // Ask the sub triggers what they are interested in
        if (c.equals(AlertFiredEvent.class))
            return new Integer[] { alertDefinitionId };

        return null;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#init(org.hyperic.hq.events.shared.RegisteredTriggerValue)
     */
    public void init(RegisteredTriggerValue tval, AlertConditionEvaluator alertConditionEvaluator) throws InvalidTriggerDataException
    {
        setId(tval.getId());
        setAlertConditionEvaluator(alertConditionEvaluator);

        try {
            ConfigResponse triggerData = ConfigResponse.decode(tval.getConfig());
            String alertDefId = triggerData.getValue(CFG_ID);

            setAlertDefinitionId(new Integer(alertDefId));
        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        } catch (NumberFormatException e) {
            throw new InvalidTriggerDataException(e);
        }
    }

    /**
     * Getter for property timeRange.
     * @return Value of property timeRange.
     *
     */
    public long getTimeRange() {
        return timeRange;
    }

    /**
     * Setter for property timeRange.
     * @param timeRange New value of property timeRange.
     *
     */
    public void setTimeRange(long val) {
        timeRange = val;
    }

    /**
     *
     * @return Value of property alertDefinitionId
     *
     */
    public Integer getAlertDefinitionId() {
        return alertDefinitionId;
    }

    /**
     *
     * @param alertDefinitionId New value of property alertDefinitionId.
     *
     */
    public void setAlertDefinitionId(Integer val) {
        alertDefinitionId = val;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface
     * #getConfigResponse(org.hyperic.hq.appdef.shared.AppdefEntityID,
     * org.hyperic.hq.events.shared.AlertConditionValue)
     */
    public ConfigResponse getConfigResponse(AppdefEntityID id, AlertConditionValue cond) throws InvalidOptionException,
                                                                                        InvalidOptionValueException
    {
        if (cond.getType() != EventConstants.TYPE_ALERT)
            throw new InvalidOptionValueException("Condition is not an Alert Fired");

        // We are using the measurement ID field for the alert definition ID
        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_ID, String.valueOf(cond.getMeasurementId()));

        return resp;
    }

}
