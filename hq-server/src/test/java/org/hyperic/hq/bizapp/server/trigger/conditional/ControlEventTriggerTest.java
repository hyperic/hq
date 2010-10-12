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

import org.easymock.EasyMock;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import junit.framework.TestCase;

/**
 * Unit test of {@link ControlEventTrigger}
 * @author jhickey
 *
 */
public class ControlEventTriggerTest
    extends TestCase
{
    private AlertConditionEvaluator alertConditionEvaluator;

    private static final Integer TRIGGER_ID = Integer.valueOf(12);

    private ControlEventTrigger trigger = new ControlEventTrigger();

    private static final AppdefEntityID RESOURCE = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER, 9999);

    private void initTrigger(String action, String expectedStatus) throws EncodingException,
                                                                  InvalidTriggerDataException
    {
        ConfigResponse config = new ConfigResponse();
        config.setValue(ConditionalTriggerInterface.CFG_ID, RESOURCE.getID());
        config.setValue(ConditionalTriggerInterface.CFG_TYPE, RESOURCE.getType());
        config.setValue(ConditionalTriggerInterface.CFG_OPTION, expectedStatus);
        config.setValue(ConditionalTriggerInterface.CFG_NAME, action);
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
     * Verifies trigger is fired on control event
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessControlEvent() throws EncodingException, InvalidTriggerDataException, EventTypeException {
        initTrigger("start", "Success");
        ControlEvent event = new ControlEvent("admin",
                                              RESOURCE.getType(),
                                              RESOURCE.getId(),
                                              "start",
                                              false,
                                              0l,
                                              "Success");
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Firing control event trigger: start Success");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies nothing happens if event is received for the wrong control
     * action
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessControlEventWrongAction() throws EncodingException,
                                                    InvalidTriggerDataException,
                                                    EventTypeException
    {
        initTrigger("start", "Success");
        ControlEvent event = new ControlEvent("admin",
                                              RESOURCE.getType(),
                                              RESOURCE.getId(),
                                              "stop",
                                              false,
                                              0l,
                                              "Success");
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies nothing happens if control event is received with wrong resource
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessControlEventWrongResource() throws EncodingException,
                                                      InvalidTriggerDataException,
                                                      EventTypeException
    {
        initTrigger("start", "Success");
        ControlEvent event = new ControlEvent("admin", RESOURCE.getType(), 78945, "start", false, 0l, "Success");
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies TriggerNotFired if control event is received with unexpected
     * status
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessControlEventWrongStatus() throws EncodingException,
                                                    InvalidTriggerDataException,
                                                    EventTypeException
    {
        initTrigger("start", "Success");
        ControlEvent event = new ControlEvent("admin",
                                              RESOURCE.getType(),
                                              RESOURCE.getId(),
                                              "start",
                                              false,
                                              0l,
                                              "Failure");
        TriggerNotFiredEvent notFired = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that a {@link EventTypeException} is thrown if an event with
     * wrong type is passed in
     */
    public void testProcessWrongEventType() {
        EasyMock.replay(alertConditionEvaluator);
        try {
            trigger.processEvent(new MockEvent(3l, 4));
            fail("An EventTypeException should be thrown if an event with type other than ControlEvent is passed in");
        } catch (EventTypeException e) {
            EasyMock.verify(alertConditionEvaluator);
        }
    }
}
