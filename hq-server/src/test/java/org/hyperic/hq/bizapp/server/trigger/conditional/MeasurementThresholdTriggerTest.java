/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.bizapp.server.trigger.conditional;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
/**
 * Unit test of {@link MeasurementThresholdTrigger}
 * @author jhickey
 *
 */
public class MeasurementThresholdTriggerTest
    extends TestCase
{
    private AlertConditionEvaluator alertConditionEvaluator;

    private static final Integer TRIGGER_ID = Integer.valueOf(12);

    private MeasurementThresholdTrigger trigger = new MeasurementThresholdTrigger();

    private static final Integer MEASUREMENT_ID = Integer.valueOf(45);

    private void initTrigger(String comparator, double threshold) throws EncodingException, InvalidTriggerDataException
    {
        ConfigResponse config = new ConfigResponse();
        config.setValue(ConditionalTriggerInterface.CFG_ID, MEASUREMENT_ID);

        config.setValue(ConditionalTriggerInterface.CFG_COMPARATOR, comparator);
        config.setValue(ConditionalTriggerInterface.CFG_THRESHOLD, String.valueOf(threshold));
        RegisteredTriggerValue regTrigger = new RegisteredTriggerValue();
        regTrigger.setId(TRIGGER_ID);
        regTrigger.setConfig(config.encode());
        trigger.init(regTrigger, alertConditionEvaluator);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
    }

    /**
     * Verifies that trigger fires if measurement equal to threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventEquals() throws EncodingException, InvalidTriggerDataException, EventTypeException {
        initTrigger("=", 12d);
        MetricValue metricValue = new MetricValue(12d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Metric(45) value " + metricValue + " = 12.0");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger fires if measurement greater than threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventGreaterThan() throws EncodingException, InvalidTriggerDataException, EventTypeException
    {
        initTrigger(">", 9d);
        MetricValue metricValue = new MetricValue(10d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Metric(45) value " + metricValue + " > 9.0");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger fires if measurement greater than or equal to threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventGreaterThanEquals() throws EncodingException,
                                                   InvalidTriggerDataException,
                                                   EventTypeException
    {
        initTrigger(">=", 12d);
        MetricValue metricValue = new MetricValue(12d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Metric(45) value " + metricValue + " >= 12.0");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that RuntimeException is thrown if an invalid operator is set
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventInvalidOperator() throws EncodingException, InvalidTriggerDataException, EventTypeException {
        initTrigger("!=", 12d);
        trigger.setOperator(99);
        MetricValue metricValue = new MetricValue(12d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        EasyMock.replay(alertConditionEvaluator);
        try {
            trigger.processEvent(event);
            fail("A RuntimeException should be thrown if operator is invalid");
        } catch (RuntimeException e) {
            EasyMock.verify(alertConditionEvaluator);
        }
    }


    /**
     * Verifies that trigger fires if measurement less than threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventLessThan() throws EncodingException, InvalidTriggerDataException, EventTypeException {
        initTrigger("<", 12d);
        MetricValue metricValue = new MetricValue(10d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Metric(45) value " + metricValue + " < 12.0");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger fires if measurement less than or equal to threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventLessThanEquals() throws EncodingException,
                                                InvalidTriggerDataException,
                                                EventTypeException
    {
        initTrigger("<=", 12d);
        MetricValue metricValue = new MetricValue(10d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Metric(45) value " + metricValue + " <= 12.0");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger fires if measurement not equal to threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNotEqual() throws EncodingException, InvalidTriggerDataException, EventTypeException {
        initTrigger("!=", 12d);
        MetricValue metricValue = new MetricValue(10d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Metric(45) value " + metricValue + " != 12.0");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger doesn't fire if measurement not equal to threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNotEquals() throws EncodingException, InvalidTriggerDataException, EventTypeException {
        initTrigger("=", 12d);
        MetricValue metricValue = new MetricValue(14d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(TRIGGER_ID);
        triggerNotFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(triggerNotFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger doesn't fire if measurement not greater than threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNotGreaterThan() throws EncodingException,
                                                InvalidTriggerDataException,
                                                EventTypeException
    {
        initTrigger(">", 12d);
        MetricValue metricValue = new MetricValue(11d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(TRIGGER_ID);
        triggerNotFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(triggerNotFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger doesn't fire if measurement not greater than or equal to threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNotGreaterThanEquals() throws EncodingException,
                                                      InvalidTriggerDataException,
                                                      EventTypeException
    {
        initTrigger(">=", 12d);
        MetricValue metricValue = new MetricValue(11d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(TRIGGER_ID);
        triggerNotFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(triggerNotFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger doesn't fire if measurement not less than threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNotLessThan() throws EncodingException, InvalidTriggerDataException, EventTypeException
    {
        initTrigger("<", 12d);
        MetricValue metricValue = new MetricValue(14d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(TRIGGER_ID);
        triggerNotFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(triggerNotFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger doesn't fire if measurement not less than or equal to threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNotLessThanEquals() throws EncodingException,
                                                   InvalidTriggerDataException,
                                                   EventTypeException
    {
        initTrigger("<=", 12d);
        MetricValue metricValue = new MetricValue(14d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(TRIGGER_ID);
        triggerNotFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(triggerNotFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger doesn't fire if measurement not not equal to threshold
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNotNotEquals() throws EncodingException,
                                              InvalidTriggerDataException,
                                              EventTypeException
    {
        initTrigger("!=", 12d);
        MetricValue metricValue = new MetricValue(12d);
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);
        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(TRIGGER_ID);
        triggerNotFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(triggerNotFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that nothing happens if an event with wrong measurement id is received
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventWrongMetric() throws EncodingException, InvalidTriggerDataException, EventTypeException  {
        initTrigger("!=", 12d);
        MetricValue metricValue = new MetricValue(11d);
        MeasurementEvent event = new MeasurementEvent(98123, metricValue);
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that an {@link EventTypeException} is thrown if the wrong event type is passed in
     */
    public void testProcessWrongEventType() {
        EasyMock.replay(alertConditionEvaluator);
        try {
            trigger.processEvent(new MockEvent(3l, 4));
            fail("An EventTypeException should be thrown if an event with type other than ResourceLogEvent is passed in");
        } catch (EventTypeException e) {
            EasyMock.verify(alertConditionEvaluator);
        }
    }
}
