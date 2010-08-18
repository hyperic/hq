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
import org.hyperic.hq.appdef.shared.CPropChangeEvent;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

/**
 * Unit test of {@link CustomPropertyTrigger}
 * @author jhickey
 *
 */
public class CustomPropertyTriggerTest
    extends TestCase
{

    private AlertConditionEvaluator alertConditionEvaluator;

    private static final Integer TRIGGER_ID = Integer.valueOf(12);

    private CustomPropertyTrigger trigger = new CustomPropertyTrigger();

    private static final AppdefEntityID RESOURCE = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER, 9999);

    private void initTrigger(String customProperty) throws EncodingException, InvalidTriggerDataException {
        ConfigResponse config = new ConfigResponse();
        config.setValue(ConditionalTriggerInterface.CFG_ID, RESOURCE.getID());
        config.setValue(ConditionalTriggerInterface.CFG_TYPE, RESOURCE.getType());
        config.setValue(ConditionalTriggerInterface.CFG_NAME, customProperty);
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
     * Verifies trigger fires if prop change event is received
     * @throws EventTypeException
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     */
    public void testProcessEvent() throws EventTypeException, EncodingException, InvalidTriggerDataException {
        initTrigger("myProp");
        CPropChangeEvent event = new CPropChangeEvent(RESOURCE, "myProp", "old", "new");
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("New myProp value (new) differs from previous property value (old).");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies nothing happens if event is received with null property value
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventNullPropValue() throws EncodingException,
                                               InvalidTriggerDataException,
                                               EventTypeException
    {
        initTrigger("myProp");
        CPropChangeEvent event = new CPropChangeEvent(RESOURCE, "myProp", null, "new");
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies nothing happens if event is received with wrong property name
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventWrongPropertyName() throws EncodingException,
                                                   InvalidTriggerDataException,
                                                   EventTypeException
    {
        initTrigger("myProp");
        CPropChangeEvent event = new CPropChangeEvent(RESOURCE, "yourProp", "old", "new");
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies nothing happens if event is received with wrong resource
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventWrongResource() throws EncodingException,
                                               InvalidTriggerDataException,
                                               EventTypeException
    {
        initTrigger("myProp");
        CPropChangeEvent event = new CPropChangeEvent(new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                                                         987555), "myProp", "old", "new");
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies an {@link EventTypeException} is thrown if an event of wrong
     * type is passed in
     */
    public void testProcessWrongEventType() {
        EasyMock.replay(alertConditionEvaluator);
        try {
            trigger.processEvent(new MockEvent(3l, 4));
            fail("An EventTypeException should be thrown if an event with type other than CPropChangeEvent is passed in");
        } catch (EventTypeException e) {
            EasyMock.verify(alertConditionEvaluator);
        }
    }

}
