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
