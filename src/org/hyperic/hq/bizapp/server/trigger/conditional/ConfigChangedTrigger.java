/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.trigger.conditional;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ConditionalTriggerSchema;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertCreateException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.shared.ConfigChangedEvent;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * A simple trigger which fires if a config changed event occurs.
 */

public class ConfigChangedTrigger
    extends AbstractTrigger
    implements ConditionalTriggerInterface {
    static {
        // Register the trigger/condition
        ConditionalTriggerInterface.MAP_COND_TRIGGER.put(
            new Integer(EventConstants.TYPE_CFG_CHG),
            ConfigChangedTrigger.class);
    }
    
    private AppdefEntityID id;
    private String         match;

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes(){
        return new Class[] { ConfigChangedEvent.class };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c){
        return new Integer[] { id.getId() };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema(){
        return ConditionalTriggerSchema
            .getConfigSchema(EventConstants.TYPE_CFG_CHG);
    }

    /**
     * @see org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface#getConfigResponse()
     */
    public ConfigResponse getConfigResponse(AppdefEntityID id,
                                            AlertConditionValue cond)
        throws InvalidOptionException, InvalidOptionValueException {
        if (cond.getType() != EventConstants.TYPE_CFG_CHG)
            throw new InvalidOptionValueException(
                "Condition is not a Log Event");

        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_TYPE, String.valueOf(id.getType()));
        resp.setValue(CFG_ID, String.valueOf(id.getID()));
        resp.setValue(CFG_OPTION, cond.getOption());
        return resp;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#init(org.hyperic.hq.events.shared.RegisteredTriggerValue)
     */
    public void init(RegisteredTriggerValue tval)
        throws InvalidTriggerDataException {
        ConfigResponse triggerData;
        String sID, sType;

        // Set the trigger value
        setTriggerValue(tval);

        // Decode the configuration
        try {
            triggerData = ConfigResponse.decode(tval.getConfig());
            sType       = triggerData.getValue(CFG_TYPE);
            sID         = triggerData.getValue(CFG_ID);
            match       = triggerData.getValue(CFG_OPTION);
        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        }

        if(sType == null || sID == null) {
            throw new InvalidTriggerDataException(
                CFG_TYPE   + " = '" + sType  + "' " +
                CFG_ID     + " = '" + sID    + "' ");
        }

        try {
            id = new AppdefEntityID(sType + ":" + sID);
        } catch(NumberFormatException exc){
            throw new InvalidTriggerDataException(
                "Instance type: " + sType + " or id: " + sID +
                " is not a valid number");
        }
    }

    /**
     * @see org.hyperic.hq.events.AbstractEvent#processEvent()
     */
    public void processEvent(AbstractEvent e)
        throws EventTypeException, ActionExecuteException {
        ConfigChangedEvent event;
        
        if(!(e instanceof ConfigChangedEvent)){
            throw new EventTypeException("Invalid event type passed, " +
                                         "expected ConfigChangedEvent");
        }

        // If we didn't fulfill the condition, then don't fire
        event = (ConfigChangedEvent) e;
        
        if (!event.getResource().equals(id))
            return;
        
        if (match == null || match.length() == 0 ||
            event.getMessage().indexOf(match) > -1) {
            try {
                TriggerFiredEvent tfe = new TriggerFiredEvent(getId(), event);
                tfe.setMessage("Config file (" + event.getSource() +
                               ") changed: " + event.getMessage());
                super.fireActions(tfe);
            } catch (AlertCreateException exc) {
                throw new ActionExecuteException(exc);
            } catch (ActionExecuteException exc) {
                throw new ActionExecuteException(exc);
            } catch (SystemException exc) {
                throw new ActionExecuteException(exc);
            }
        }
        else {
            // Let dispatchers know that trigger evaluated to false
            notFired();
        }
    }

}
