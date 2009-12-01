/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ. HQ is free software; you can redistribute it and/or
 * modify it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 */

package org.hyperic.hq.bizapp.server.trigger.conditional;

import java.text.MessageFormat;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropChangeEvent;
import org.hyperic.hq.bizapp.shared.ConditionalTriggerSchema;
import org.hyperic.hq.events.AbstractEvent;
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
 * A simple trigger which fires if a control event occurs.
 */

public class CustomPropertyTrigger
    extends AbstractTrigger implements ConditionalTriggerInterface
{
    static {
        // Register the trigger/condition
        ConditionalTriggerInterface.MAP_COND_TRIGGER.put(new Integer(EventConstants.TYPE_CUST_PROP),
                                                         CustomPropertyTrigger.class);
    }

    public static final MessageFormat MESSAGE_FMT = new MessageFormat("New {0} value ({1}) differs from previous property value ({2}).");

    private AppdefEntityID id;
    private String customProperty;

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes() {
        return new Class[] { CPropChangeEvent.class };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c) {
        return new Integer[] { id.getId() };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        return ConditionalTriggerSchema.getConfigSchema(EventConstants.TYPE_CUST_PROP);
    }

    /**
     * @see org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface#getConfigResponse()
     */
    public ConfigResponse getConfigResponse(AppdefEntityID id, AlertConditionValue cond) throws InvalidOptionException,
                                                                                        InvalidOptionValueException
    {
        if (cond.getType() != EventConstants.TYPE_CUST_PROP)
            throw new InvalidOptionValueException("Condition is not a Custom Property Change Event");

        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_TYPE, String.valueOf(id.getType()));
        resp.setValue(CFG_ID, String.valueOf(id.getID()));
        resp.setValue(CFG_NAME, cond.getName());
        return resp;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#init(org.hyperic.hq.events.shared.RegisteredTriggerValue)
     */
    public void init(RegisteredTriggerValue tval, AlertConditionEvaluator alertConditionEvaluator) throws InvalidTriggerDataException
    {
        ConfigResponse triggerData;
        String sID, sType;

        setId(tval.getId());
        setAlertConditionEvaluator(alertConditionEvaluator);

        // Decode the configuration
        try {
            triggerData = ConfigResponse.decode(tval.getConfig());
            sType = triggerData.getValue(CFG_TYPE);
            sID = triggerData.getValue(CFG_ID);
            customProperty = triggerData.getValue(CFG_NAME);
        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        }

        if (sType == null || sID == null || customProperty == null) {
            throw new InvalidTriggerDataException(CFG_TYPE + " = '" + sType + "' " + CFG_ID + " = '" + sID + "' " +
                                                  CFG_NAME + " = '" + customProperty + "' ");
        }

        try {
            id = new AppdefEntityID(sType + ":" + sID);
        } catch (NumberFormatException exc) {
            throw new InvalidTriggerDataException("Instance type: " + sType + " or id: " + sID +
                                                  " is not a valid number");
        }
    }

    /**
     * @see org.hyperic.hq.events.AbstractEvent#processEvent()
     */
    public void processEvent(AbstractEvent e) throws EventTypeException {
        if (!(e instanceof CPropChangeEvent)) {
            throw new EventTypeException("Invalid event type passed, " + "expected CPropChangeEvent");
        }

        // If we didn't fulfill the condition, then don't fire
        CPropChangeEvent event = (CPropChangeEvent) e;

        if (!event.getResource().equals(id) || !event.getKey().equals(customProperty) || event.getOldValue() == null) {
            return;
        }

        TriggerFiredEvent tfe = prepareTriggerFiredEvent(event);
        StringBuffer sb = new StringBuffer();

        MESSAGE_FMT.format(new String[] { event.getKey(), event.getNewValue(), event.getOldValue() }, sb, null);

        tfe.setMessage(sb.toString());
        super.fireActions(tfe);
    }

}
