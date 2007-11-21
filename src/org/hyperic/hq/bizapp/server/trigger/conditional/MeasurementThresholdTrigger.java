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

package org.hyperic.hq.bizapp.server.trigger.conditional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * A simple trigger which fires if a measurement event exceeds a threshold.
 */

public class MeasurementThresholdTrigger extends AbstractTrigger
    implements ConditionalTriggerInterface {
    private Log log = LogFactory.getLog(getClass().getName());
    
    static {
        // Register the trigger/condition
        ConditionalTriggerInterface.MAP_COND_TRIGGER.put(
            new Integer(EventConstants.TYPE_THRESHOLD),
            MeasurementThresholdTrigger.class);
    }
    
    private final int OPER_LE = ConditionalTriggerSchema.OPER_LE;
    private final int OPER_LT = ConditionalTriggerSchema.OPER_LT;
    private final int OPER_EQ = ConditionalTriggerSchema.OPER_EQ;
    private final int OPER_GT = ConditionalTriggerSchema.OPER_GT;
    private final int OPER_GE = ConditionalTriggerSchema.OPER_GE;
    private final int OPER_NE = ConditionalTriggerSchema.OPER_NE;

    private final String[] OPER_STRS = ConditionalTriggerSchema.OPER_STRS;

    private int     operator;
    private double  threshold;
    private Integer metricId;

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedEventTypes()
     */
    public Class[] getInterestedEventTypes(){
        return new Class[] { MeasurementEvent.class };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getInterestedInstanceIDs(java.lang.Class)
     */
    public Integer[] getInterestedInstanceIDs(Class c){
        return new Integer[] { metricId };
    }

    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema(){
        return ConditionalTriggerSchema
            .getConfigSchema(EventConstants.TYPE_THRESHOLD);
    }

    protected ConfigResponse getSharedConfigResponse(AlertConditionValue cond)
        throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_ID, String.valueOf(cond.getMeasurementId()));
        resp.setValue(CFG_THRESHOLD, String.valueOf(cond.getThreshold()));
        resp.setValue(CFG_COMPARATOR, cond.getComparator());
        return resp;
    }
    
    public ConfigResponse getConfigResponse(AppdefEntityID id,
                                            AlertConditionValue cond)
        throws InvalidOptionException, InvalidOptionValueException {
        if (cond.getType() != EventConstants.TYPE_THRESHOLD)
            throw new InvalidOptionValueException(
                "Condition is not a Measurement Threshold");
        return getSharedConfigResponse(cond);
    }

    public ConfigResponse getConfigResponse() {
        ConfigResponse resp = new ConfigResponse();
        resp.setValue(CFG_ID, String.valueOf(getMeasurementID()));
        resp.setValue(CFG_THRESHOLD, String.valueOf(getThreshold()));
        resp.setValue(CFG_COMPARATOR, OPER_STRS[getOperator()]);
        return resp;
    }
    
    /**
     * @see org.hyperic.hq.events.ext.RegisterableTriggerInterface#init(org.hyperic.hq.events.shared.RegisteredTriggerValue)
     */
    public void init(RegisteredTriggerValue tval)
        throws InvalidTriggerDataException
    {
        ConfigResponse triggerData;
        String soperator, sthreshold, smeasID;
        Integer measID;
        int oper;
        double thresh;

        // Set the trigger value
        setTriggerValue(tval);

        // Decode the configuration
        try {
            triggerData = ConfigResponse.decode(tval.getConfig());
            soperator   = triggerData.getValue(CFG_COMPARATOR);
            sthreshold  = triggerData.getValue(CFG_THRESHOLD);
            smeasID     = triggerData.getValue(CFG_ID);
        } catch (EncodingException e) {
            throw new InvalidTriggerDataException(e);
        }

        if(soperator == null || sthreshold == null || smeasID == null){
            throw new InvalidTriggerDataException(
                CFG_COMPARATOR + " = '" + soperator + "' " +
                CFG_THRESHOLD + " = '" + sthreshold + "' " +
                CFG_ID + " = '" + smeasID + "'");
        }

        oper = getOperator(soperator);

        try {
            measID = new Integer(smeasID);
        } catch(NumberFormatException exc){
            throw new InvalidTriggerDataException("Instance id, '" + smeasID +
                                                  "' is not a valid number");
        }

        try {
            thresh = Double.parseDouble(sthreshold);
        } catch(NumberFormatException exc){
            throw new InvalidTriggerDataException("Threshold, '" +
                                                  sthreshold +
                                                  "' is not a valid number");
        }

        operator  = oper;
        threshold = thresh;
        metricId  = measID;
    }

    /**
     * extracts the operator
     */
    protected int getOperator(String soperator)
        throws InvalidTriggerDataException
    {
        int operator = -1;

        for (int i = 1; i < OPER_STRS.length; i++) {
            if (soperator.equals(OPER_STRS[i])) {
                operator = i;
                break;
            }
        }
        
        if (operator == -1)
            throw new InvalidTriggerDataException("Invalid operator, '" +
                                                  soperator + "'");
        return operator;
    }


    public void processEvent(AbstractEvent e)
        throws EventTypeException, ActionExecuteException {
        MeasurementEvent event;
        MetricValue val;
        double compVal;
        boolean fire;

        if(!(e instanceof MeasurementEvent)){
            throw new EventTypeException("Invalid event type passed, " +
                                         "expected MeasurementEvent");
        }

        if (!e.getInstanceId().equals(metricId))
            return;

        // If we didn't fulfill the condition, then don't fire
        event = (MeasurementEvent) e;
        val = event.getValue();
        compVal = val.getValue();

        if (log.isDebugEnabled())
            log.debug("Operator is " + operator);
        
        if(operator == OPER_LE)
            fire = compVal <= threshold;
        else if(operator == OPER_LT)
            fire = compVal < threshold;
        else if(operator == OPER_EQ)
            fire = compVal == threshold;
        else if(operator == OPER_GT)
            fire = compVal > threshold;
        else if(operator ==  OPER_GE)
            fire = compVal >= threshold;
        else if(operator ==  OPER_NE)
            fire = compVal != threshold;
        else {
            // Wow -- we should never get here -- throw an assertion
            throw new RuntimeException("Invalid threshold operation!");
        }

        // Don't do any more if condition was unmet
        if (fire == false) {
            notFired();
            return;
        }
        
        try {
            TriggerFiredEvent tfe = new TriggerFiredEvent(getId(), event);
            tfe.setMessage("Metric(" + metricId + ") value " + val + " "
                           + OPER_STRS[operator] + " " + threshold);
            fireActions(tfe);
        } catch (AlertCreateException exc) {
            throw new ActionExecuteException(exc);
        } catch (ActionExecuteException exc) {
            throw new ActionExecuteException(exc);
        } catch (SystemException exc) {
            throw new ActionExecuteException(exc);
        }
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double val) {
        threshold = val;
    }

    public int getOperator() {
        return operator;
    }

    public void setOperator(int val) {
        operator = val;
    }

    public Integer getMeasurementID() {
        return metricId;
    }

    public void setMeasurementID(Integer val) {
        metricId = val;
    }
}
