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
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.shared.ConfigChangedEvent;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

/**
 * Unit test of {@link ConfigChangedTrigger}
 * @author jhickey
 *
 */
public class ConfigChangedTriggerTest
    extends TestCase
{

    private AlertConditionEvaluator alertConditionEvaluator;

    private static final Integer TRIGGER_ID = Integer.valueOf(12);

    private ConfigChangedTrigger trigger = new ConfigChangedTrigger();

    private static final AppdefEntityID RESOURCE = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER, 9999);

    public void setUp() throws Exception {
        super.setUp();
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
        ConfigResponse config = new ConfigResponse();
        config.setValue(ConditionalTriggerInterface.CFG_ID, RESOURCE.getID());
        config.setValue(ConditionalTriggerInterface.CFG_TYPE, RESOURCE.getType());
        RegisteredTriggerValue regTrigger = new RegisteredTriggerValue();
        regTrigger.setId(TRIGGER_ID);
        regTrigger.setConfig(config.encode());
        trigger.init(regTrigger, alertConditionEvaluator);
    }

    /**
     * Verify trigger is fired when config changed event is received when not
     * matching on source
     * @throws EventTypeException
     */
    public void testProcessEvent() throws EventTypeException {
        ConfigChangedEvent event = new ConfigChangedEvent(new TrackEvent(RESOURCE,
                                                                         System.currentTimeMillis(),
                                                                         LogTrackPlugin.LOGLEVEL_INFO,
                                                                         "source",
                                                                         "myPropString"));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Config file (" + event.getSource() + ") changed: " + event.getMessage());
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies trigger is fired when config changed event is received matching
     * on substring of source
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventFilePartialMatch() throws EncodingException,
                                              InvalidTriggerDataException,
                                              EventTypeException
    {
        ConfigResponse config = new ConfigResponse();
        config.setValue(ConditionalTriggerInterface.CFG_ID, RESOURCE.getID());
        config.setValue(ConditionalTriggerInterface.CFG_TYPE, RESOURCE.getType());
        config.setValue(ConditionalTriggerInterface.CFG_OPTION, "my.properties");
        RegisteredTriggerValue regTrigger = new RegisteredTriggerValue();
        regTrigger.setId(TRIGGER_ID);
        regTrigger.setConfig(config.encode());
        trigger.init(regTrigger, alertConditionEvaluator);

        ConfigChangedEvent event = new ConfigChangedEvent(new TrackEvent(RESOURCE,
                                                                         System.currentTimeMillis(),
                                                                         LogTrackPlugin.LOGLEVEL_INFO,
                                                                         "/some/path/to/my.properties",
                                                                         "myPropString"));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Config file (" + event.getSource() + ") changed: " + event.getMessage());
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);

    }

    /**
     * Verifies trigger is fired when config changed event is received matching
     * full path to source file
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventFileMatch() throws EncodingException,
                                              InvalidTriggerDataException,
                                              EventTypeException
    {
        ConfigResponse config = new ConfigResponse();
        config.setValue(ConditionalTriggerInterface.CFG_ID, RESOURCE.getID());
        config.setValue(ConditionalTriggerInterface.CFG_TYPE, RESOURCE.getType());
        config.setValue(ConditionalTriggerInterface.CFG_OPTION, "/some/path/to/my.properties");
        RegisteredTriggerValue regTrigger = new RegisteredTriggerValue();
        regTrigger.setId(TRIGGER_ID);
        regTrigger.setConfig(config.encode());
        trigger.init(regTrigger, alertConditionEvaluator);

        ConfigChangedEvent event = new ConfigChangedEvent(new TrackEvent(RESOURCE,
                                                                         System.currentTimeMillis(),
                                                                         LogTrackPlugin.LOGLEVEL_INFO,
                                                                         "/some/path/to/my.properties",
                                                                         "myPropString"));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Config file (" + event.getSource() + ") changed: " + event.getMessage());
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);

    }

    /**
     * Verifies TriggerNotFired when source that doesn't match is received
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     * @throws EncodingException
     */
    public void testProcessEventFileNotMatch() throws InvalidTriggerDataException,
                                                 EventTypeException,
                                                 EncodingException
    {
        ConfigResponse config = new ConfigResponse();
        config.setValue(ConditionalTriggerInterface.CFG_ID, RESOURCE.getID());
        config.setValue(ConditionalTriggerInterface.CFG_TYPE, RESOURCE.getType());
        config.setValue(ConditionalTriggerInterface.CFG_OPTION, "your.properties");
        RegisteredTriggerValue regTrigger = new RegisteredTriggerValue();
        regTrigger.setId(TRIGGER_ID);
        regTrigger.setConfig(config.encode());
        trigger.init(regTrigger, alertConditionEvaluator);

        ConfigChangedEvent event = new ConfigChangedEvent(new TrackEvent(RESOURCE,
                                                                         System.currentTimeMillis(),
                                                                         LogTrackPlugin.LOGLEVEL_INFO,
                                                                         "my.properties",
                                                                         "myPropString"));
        TriggerNotFiredEvent notFired = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired));

        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that nothing happens when passing in an event on the wrong
     * resource
     * @throws EventTypeException
     */
    public void testProcessEventWrongResource() throws EventTypeException {
        ConfigChangedEvent event = new ConfigChangedEvent(new TrackEvent(new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                                                                            9998),
                                                                         System.currentTimeMillis(),
                                                                         LogTrackPlugin.LOGLEVEL_INFO,
                                                                         "source",
                                                                         "myPropString"));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that an {@link EventTypeException} is thrown when passing in an
     * event of the wrong type
     */
    public void testProcessWrongEventType() {
        EasyMock.replay(alertConditionEvaluator);
        try {
            trigger.processEvent(new MockEvent(3l, 4));
            fail("An EventTypeException should be thrown if an event with type other than ConfigChangedEvent is passed in");
        } catch (EventTypeException e) {
            EasyMock.verify(alertConditionEvaluator);
        }
    }

}
