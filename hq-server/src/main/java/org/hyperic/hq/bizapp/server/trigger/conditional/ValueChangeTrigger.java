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

package org.hyperic.hq.bizapp.server.trigger.conditional;

import java.text.MessageFormat;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ConditionalTriggerSchema;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.units.FormattedNumber;

/**
 * The ValueChangeTrigger is a simple trigger which fires when a new
 * MeasurementEvent's value does not match the previous stored
 */

public class ValueChangeTrigger
    extends AbstractTrigger implements ConditionalTriggerInterface {
    static {
        // Register the trigger/condition
        ConditionalTriggerInterface.MAP_COND_TRIGGER.put(new Integer(EventConstants.TYPE_CHANGE),
            ValueChangeTrigger.class);
    }

    private static final MessageFormat MESSAGE_FMT = new MessageFormat(
        "Current value ({0}) differs from previous value ({1}).");

    private final Object lock = new Object();
    private Integer measurementId;
    private MeasurementEvent last = null;
    private final Log log = LogFactory.getLog(ValueChangeTrigger.class);
    private final MeasurementManager measurementManager;
    private final DataManager dataManager;

    public ValueChangeTrigger() {
        this.measurementManager = Bootstrap.getBean(MeasurementManager.class);
        this.dataManager = Bootstrap.getBean(DataManager.class);
    }

    public ValueChangeTrigger(MeasurementManager measurementManager, DataManager dataManager) {
        this.measurementManager = measurementManager;
        this.dataManager = dataManager;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        return ConditionalTriggerSchema.getConfigSchema(EventConstants.TYPE_CHANGE);
    }

    /**
     * @see org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface#getConfigResponse(org.hyperic.hq.appdef.shared.AppdefEntityID,
     *      org.hyperic.hq.events.shared.AlertConditionValue)
     */
    public ConfigResponse getConfigResponse(AppdefEntityID id, AlertConditionValue cond) throws InvalidOptionException,
        InvalidOptionValueException {
        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_ID, String.valueOf(cond.getMeasurementId()));
        return resp;
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#init(org.hyperic.hq.events.shared.RegisteredTriggerValue)
     */
    public void init(RegisteredTriggerValue tval, AlertConditionEvaluator alertConditionEvaluator)
        throws InvalidTriggerDataException {
        setId(tval.getId());
        setAlertConditionEvaluator(alertConditionEvaluator);

        try {
            ConfigResponse triggerData = ConfigResponse.decode(getConfigSchema(), tval.getConfig());
            measurementId = Integer.valueOf(triggerData.getValue(CFG_ID));
        } catch (InvalidOptionException exc) {
            throw new InvalidTriggerDataException(exc);
        } catch (InvalidOptionValueException exc) {
            throw new InvalidTriggerDataException(exc);
        } catch (EncodingException exc) {
            throw new InvalidTriggerDataException(exc);
        }
        initializeLastValue();
    }

    private void initializeLastValue() {
        try {

            Measurement measurement = measurementManager.getMeasurement(measurementId);
            MetricValue val = dataManager.getLastHistoricalData(measurement);
            if (val == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No previous values found for measurement " + measurementId);
                }
                return;
            }
            this.last = new MeasurementEvent(measurementId, val);
        } catch (Exception e) {
            log.error(
                "Error initializing last value.  Changes from previously stored value will not trigger an alert.", e);
        }
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes() {
        return new Class[] { MeasurementEvent.class };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c) {
        return new Integer[] { measurementId };
    }

    MeasurementEvent getLast() {
        return last;
    }

    public void setLast(MeasurementEvent last) {
        this.last = last;
    }

    /**
     * Process an event from the dispatcher.
     * @param event the Event to process
     * @throws ActionExecuteException if an action throws an exception
     */
    public void processEvent(AbstractEvent event) throws EventTypeException {
        // If we didn't fulfill the condition, then don't fire
        if (!(event instanceof MeasurementEvent)) {
            throw new EventTypeException("Invalid event type passed, expected MeasurementEvent");
        }
        MeasurementEvent me = (MeasurementEvent) event;
        if (!me.getInstanceId().equals(measurementId)) {
            throw new EventTypeException("Invalid instance ID passed (" + me.getInstanceId() + ") expected " +
                                         measurementId);
        }
        synchronized (lock) {
            TriggerFiredEvent myEvent = null;
            if (last == null) {
                last = me;
            } else if ((last.getValue().getValue() != me.getValue().getValue()) &&
                       (last.getValue().getTimestamp() < me.getValue().getTimestamp())) {
                // Get ready to fire
                myEvent = prepareTriggerFiredEvent(event);
                double values[] = { me.getValue().getValue(), last.getValue().getValue() };
                FormattedNumber[] fmtValues = UnitsConvert.convertSame(values, me.getUnits(), Locale.getDefault());
                StringBuffer sb = new StringBuffer();
                MESSAGE_FMT.format(fmtValues, sb, null);
                myEvent.setMessage(sb.toString());
                last = me; // Update the last reference
            }

            if (myEvent != null) {
                super.fireActions(myEvent);
            } else {
                notFired(event);
            }
        }
    }

}
