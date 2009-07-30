package org.hyperic.hq.bizapp.server.trigger.conditional;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;

/**
 * Unit test of {@link AlertTrigger}
 * @author jhickey
 *
 */
public class AlertTriggerTest
    extends TestCase
{

    private AlertConditionEvaluator alertConditionEvaluator;

    private static final Integer TRIGGER_ID = Integer.valueOf(12);

    private AlertTrigger trigger = new AlertTrigger();

    private static final Integer ALERT_DEFINITION_ID = Integer.valueOf(45);

    private static final AppdefEntityID ALERTING_RESOURCE = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                                                               9999);

    public void setUp() throws Exception {
        super.setUp();
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
        ConfigResponse measurementConfig = new ConfigResponse();
        measurementConfig.setValue(ConditionalTriggerInterface.CFG_ID, ALERT_DEFINITION_ID);
        RegisteredTriggerValue regTrigger = new RegisteredTriggerValue();
        regTrigger.setId(TRIGGER_ID);
        regTrigger.setConfig(measurementConfig.encode());
        trigger.init(regTrigger, alertConditionEvaluator);

    }

    /**
     * Verifies trigger is fired on alert fired event
     * @throws EventTypeException
     */
    public void testProcessAlertTrigger() throws EventTypeException {
        AlertFiredEvent event = new AlertFiredEvent(123,
                                                    ALERT_DEFINITION_ID,
                                                    ALERTING_RESOURCE,
                                                    "Server Down",
                                                    System.currentTimeMillis(),
                                                    "Oh No!");
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies nothing happens if alert fired event received with wrong alert
     * definition id
     * @throws EventTypeException
     */
    public void testProcessAlertWrongAlertDefinition() throws EventTypeException {
        AlertFiredEvent event = new AlertFiredEvent(123,
                                                    7654,
                                                    ALERTING_RESOURCE,
                                                    "Server Down",
                                                    System.currentTimeMillis(),
                                                    "Oh No!");
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that an {@link EventTypeException} occurs when passing in an
     * event of invalid type
     */
    public void testProcessWrongEventType() {
        EasyMock.replay(alertConditionEvaluator);
        try {
            trigger.processEvent(new MockEvent(3l, 4));
            fail("An EventTypeException should be thrown if an event with type other than MeasurementEvent is passed in");
        } catch (EventTypeException e) {
            EasyMock.verify(alertConditionEvaluator);
        }
    }
}
