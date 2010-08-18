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
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

/**
 * Unit test of the {@link ValueChangeTrigger}
 * @author jhickey
 * 
 */
public class ValueChangeTriggerTest
    extends TestCase {

    private AlertConditionEvaluator alertConditionEvaluator;

    private static final Integer TRIGGER_ID = Integer.valueOf(12);

    private ValueChangeTrigger trigger;

    private static final Integer MEASUREMENT_ID = Integer.valueOf(45);

    private Measurement measurement = new Measurement();

    private MeasurementManager measurementManager;

    private DataManager dataManager;

    private void initTrigger() throws EncodingException, InvalidTriggerDataException {
        ConfigResponse measurementConfig = new ConfigResponse();
        measurementConfig.setValue(ConditionalTriggerInterface.CFG_ID, MEASUREMENT_ID);
        RegisteredTriggerValue regTrigger = new RegisteredTriggerValue();
        regTrigger.setId(TRIGGER_ID);
        regTrigger.setConfig(measurementConfig.encode());
        trigger.init(regTrigger, alertConditionEvaluator);
    }

    private void replay() {
        EasyMock.replay(alertConditionEvaluator, measurementManager, dataManager);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
        this.measurementManager = EasyMock.createMock(MeasurementManager.class);
        this.dataManager = EasyMock.createMock(DataManager.class);
        this.trigger = new ValueChangeTrigger(measurementManager,dataManager);
    }


    /**
     * Verifies that nothing blows up if an Exception occurs retrieving stored
     * measurement values when the trigger is initialized
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     */
    public void testInitializeErrorRetrievingLastValue() throws EncodingException, InvalidTriggerDataException {
        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andThrow(new RuntimeException("Problem!"));
        replay();
        initTrigger();
        verify();
        assertNull(trigger.getLast());
    }

    /**
     * Verifies that nothing blows up if there are no stored measurement values
     * when the trigger is initialized
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     */
    public void testInitializeNoLastValue() throws EncodingException, InvalidTriggerDataException {
        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andReturn(measurement);
        EasyMock.expect(dataManager.getLastHistoricalData(measurement))
            .andReturn(null);
        replay();
        initTrigger();
        verify();
        assertNull(trigger.getLast());
    }

    /**
     * Verifies that first event is processed correctly if trigger has no
     * initial value
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNoInitialValue() throws EncodingException, InvalidTriggerDataException,
        EventTypeException {
        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andThrow(new RuntimeException("Problem!"));

        // event occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        TriggerNotFiredEvent triggerNotFired = new TriggerNotFiredEvent(TRIGGER_ID);
        triggerNotFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(triggerNotFired);

        replay();
        initTrigger();
        trigger.processEvent(event);
        verify();
        assertEquals(event, trigger.getLast());
    }

    /**
     * Verifies that an Exception will be thrown if an event is received with
     * the wrong measurement id
     * @throws InvalidTriggerDataException
     * @throws EncodingException
     */
    public void testProcessEventWrongMeasurementId() throws EncodingException, InvalidTriggerDataException {
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(3, metricValue);

        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andReturn(measurement);
        MetricValue lastValue = new MetricValue(2);
        EasyMock.expect(dataManager.getLastHistoricalData(measurement)).andReturn(lastValue);
        replay();
        initTrigger();
        try {
            trigger.processEvent(event);
            fail("An EventTypeException should be thrown if an event with different measurement id is passed in");
        } catch (EventTypeException e) {
            verify();
        }

    }

    /**
     * Verifies that an Exception will be thrown if the wrong event type is
     * received
     * @throws InvalidTriggerDataException
     * @throws EncodingException
     */
    public void testProcessEventWrongType() throws EncodingException, InvalidTriggerDataException {
        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andReturn(measurement);
        MetricValue lastValue = new MetricValue(2);
        EasyMock.expect(dataManager.getLastHistoricalData(measurement))
            .andReturn(lastValue);
        replay();
        initTrigger();
        try {
            trigger.processEvent(new MockEvent(3l, 4));
            fail("An EventTypeException should be thrown if an event with type other than MeasurementEvent is passed in");
        } catch (EventTypeException e) {
            verify();
        }
    }

    /**
     * Verifies a measurement older than the last one stored will not fire the
     * trigger
     * @throws EventTypeException
     * @throws InvalidTriggerDataException
     * @throws EncodingException
     */
    public void testProcessOlderValueChange() throws EventTypeException, EncodingException, InvalidTriggerDataException {
        // initial measurement read 10 minutes ago
        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andReturn(measurement);
       
        MetricValue initialValue = new MetricValue(2d, System.currentTimeMillis() - (10 * 60 * 1000l));
       
        EasyMock.expect(dataManager.getLastHistoricalData(measurement))
            .andReturn(initialValue);

        // occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        // occurred 6 minutes ago
        MetricValue metricValue2 = new MetricValue(11d, System.currentTimeMillis() - (6 * 60 * 1000l));
        MeasurementEvent event2 = new MeasurementEvent(MEASUREMENT_ID, metricValue2);

        // initial fire
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Current value (10) differs from previous value (2).");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));

        TriggerNotFiredEvent notFired2 = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired2.setTimestamp(event2.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired2));

        replay();
        initTrigger();
        trigger.processEvent(event);
        trigger.processEvent(event2);
        verify();
        assertEquals(event, trigger.getLast());
    }

    /**
     * Verifies the trigger fires on measurement value change after change from
     * initial value
     * @throws EventTypeException
     * @throws InvalidTriggerDataException
     * @throws EncodingException
     */
    public void testProcessValueChanged() throws EventTypeException, EncodingException, InvalidTriggerDataException {
        // initial measurement read 10 minutes ago
        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andReturn(measurement);
       
        MetricValue initialValue = new MetricValue(2d, System.currentTimeMillis() - (10 * 60 * 1000l));
       
        EasyMock.expect(dataManager.getLastHistoricalData(measurement))
            .andReturn(initialValue);

        // occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        // occurred 4 minutes ago
        MetricValue metricValue2 = new MetricValue(11d, System.currentTimeMillis() - (4 * 60 * 1000l));
        MeasurementEvent event2 = new MeasurementEvent(MEASUREMENT_ID, metricValue2);

        // initial fire
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Current value (10) differs from previous value (2).");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));

        TriggerFiredEvent triggerFired2 = new TriggerFiredEvent(TRIGGER_ID, event2);
        triggerFired2.setMessage("Current value (11) differs from previous value (10).");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired2));

        replay();
        initTrigger();
        trigger.processEvent(event);
        trigger.processEvent(event2);
        verify();
        assertEquals(event2, trigger.getLast());
    }

    /**
     * Verifies the trigger fires if first measurement event differs from
     * initial value
     * @throws EventTypeException
     * @throws InvalidTriggerDataException
     * @throws EncodingException
     */
    public void testProcessValueChangedFromInitial() throws EventTypeException, EncodingException,
        InvalidTriggerDataException {
        // initial value - measurement read 10 minutes ago
        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andReturn(measurement);
     
        MetricValue initialValue = new MetricValue(2d, System.currentTimeMillis() - (10 * 60 * 1000l));
       
        EasyMock.expect(dataManager.getLastHistoricalData(measurement))
            .andReturn(initialValue);

        // event occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Current value (10) differs from previous value (2).");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));

        replay();
        initTrigger();
        trigger.processEvent(event);
        verify();
        assertEquals(event, trigger.getLast());
    }

    /**
     * Verifies that a measurement with the same value as the last one stored
     * will not fire the trigger
     * @throws EventTypeException
     * @throws InvalidTriggerDataException
     * @throws EncodingException
     */
    public void testProcessValueSame() throws EventTypeException, EncodingException, InvalidTriggerDataException {
        // initial measurement read 10 minutes ago
        EasyMock.expect(measurementManager.getMeasurement(MEASUREMENT_ID)).andReturn(measurement);
        
        MetricValue initialValue = new MetricValue(10d, System.currentTimeMillis() - (10 * 60 * 1000l));
       
        EasyMock.expect(dataManager.getLastHistoricalData(measurement))
            .andReturn(initialValue);

        // occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        TriggerNotFiredEvent notFired1 = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired1.setTimestamp(event.getTimestamp());

        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired1));

        replay();
        initTrigger();
        trigger.processEvent(event);
        verify();
        assertEquals(event, trigger.getLast());
    }

    private void verify() {
        EasyMock.verify(alertConditionEvaluator, measurementManager, dataManager);
    }

}
