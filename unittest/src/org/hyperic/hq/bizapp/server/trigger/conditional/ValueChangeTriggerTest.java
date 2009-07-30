package org.hyperic.hq.bizapp.server.trigger.conditional;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.config.ConfigResponse;
/**
 * Unit test of the {@link ValueChangeTrigger}
 * @author jhickey
 *
 */
public class ValueChangeTriggerTest
    extends TestCase
{

    private AlertConditionEvaluator alertConditionEvaluator;

    private static final Integer TRIGGER_ID = Integer.valueOf(12);

    private ValueChangeTrigger trigger = new ValueChangeTrigger();

    private static final Integer MEASUREMENT_ID = Integer.valueOf(45);

    public void setUp() throws Exception {
        super.setUp();
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
        ConfigResponse measurementConfig = new ConfigResponse();
        measurementConfig.setValue(ConditionalTriggerInterface.CFG_ID, MEASUREMENT_ID);
        RegisteredTriggerValue regTrigger = new RegisteredTriggerValue();
        regTrigger.setId(TRIGGER_ID);
        regTrigger.setConfig(measurementConfig.encode());
        trigger.init(regTrigger, alertConditionEvaluator);
    }

    /**
     * Verifies that an Exception will be thrown if an event is received with the wrong measurement id
     */
    public void testProcessEventWrongMeasurementId() {
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(3, metricValue);
        EasyMock.replay(alertConditionEvaluator);
        try {
            trigger.processEvent(event);
            fail("An EventTypeException should be thrown if an event with different measurement id is passed in");
        } catch (EventTypeException e) {
            EasyMock.verify(alertConditionEvaluator);
        }

    }

    /**
     * Verifies that an Exception will be thrown if the wrong event type is received
     */
    public void testProcessEventWrongType() {
        EasyMock.replay(alertConditionEvaluator);
        try {
            trigger.processEvent(new MockEvent(3l, 4));
            fail("An EventTypeException should be thrown if an event with type other than MeasurementEvent is passed in");
        } catch (EventTypeException e) {
            EasyMock.verify(alertConditionEvaluator);
        }
    }

    /**
     * Verifies the stored event updates on first measurement event
     * @throws EventTypeException
     */
    public void testProcessMeasurementEvent() throws EventTypeException {
        // occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        TriggerNotFiredEvent notFired = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired));

        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
        assertEquals(event, trigger.getLast());
    }

    /**
     * Verifies a measurement older than the last one stored will not fire the trigger
     * @throws EventTypeException
     */
    public void testProcessOlderValueChange() throws EventTypeException {
        // occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        // occurred 6 minutes ago
        MetricValue metricValue2 = new MetricValue(11d, System.currentTimeMillis() - (6 * 60 * 1000l));
        MeasurementEvent event2 = new MeasurementEvent(MEASUREMENT_ID, metricValue2);

        TriggerNotFiredEvent notFired1 = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired1.setTimestamp(event.getTimestamp());

        TriggerNotFiredEvent notFired2 = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired2.setTimestamp(event2.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired1));
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired2));

        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        trigger.processEvent(event2);
        EasyMock.verify(alertConditionEvaluator);
        assertEquals(event, trigger.getLast());
    }

    /**
     * Verifies the trigger fires on measurement value change
     * @throws EventTypeException
     */
    public void testProcessValueChanged() throws EventTypeException {
        // occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        // occurred 4 minutes ago
        MetricValue metricValue2 = new MetricValue(11d, System.currentTimeMillis() - (4 * 60 * 1000l));
        MeasurementEvent event2 = new MeasurementEvent(MEASUREMENT_ID, metricValue2);

        TriggerNotFiredEvent notFired = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event2);
        triggerFired.setMessage("Current value (11) differs from previous value (10).");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));

        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        trigger.processEvent(event2);
        EasyMock.verify(alertConditionEvaluator);
        assertEquals(event2, trigger.getLast());
    }

    /**
     * Verifies that a measurement with the same value as the last one stored will not fire the trigger
     * @throws EventTypeException
     */
    public void testProcessValueSame() throws EventTypeException {
        // occurred 5 minutes ago
        MetricValue metricValue = new MetricValue(10d, System.currentTimeMillis() - (5 * 60 * 1000l));
        MeasurementEvent event = new MeasurementEvent(MEASUREMENT_ID, metricValue);

        // occurred 3 minutes ago
        MetricValue metricValue2 = new MetricValue(10d, System.currentTimeMillis() - (3 * 60 * 1000l));
        MeasurementEvent event2 = new MeasurementEvent(MEASUREMENT_ID, metricValue2);

        TriggerNotFiredEvent notFired1 = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired1.setTimestamp(event.getTimestamp());

        TriggerNotFiredEvent notFired2 = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired2.setTimestamp(event2.getTimestamp());

        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired1));
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired2));

        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        trigger.processEvent(event2);
        EasyMock.verify(alertConditionEvaluator);
        assertEquals(event, trigger.getLast());
    }

}
