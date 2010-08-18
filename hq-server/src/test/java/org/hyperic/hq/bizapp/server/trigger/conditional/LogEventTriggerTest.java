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
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import junit.framework.TestCase;

/**
 * Unit test of {@link LogEventTrigger}
 * @author jhickey
 *
 */
public class LogEventTriggerTest
    extends TestCase
{

    private AlertConditionEvaluator alertConditionEvaluator;

    private static final Integer TRIGGER_ID = Integer.valueOf(12);

    private LogEventTrigger trigger = new LogEventTrigger();

    private static final AppdefEntityID RESOURCE = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER, 9999);

    private void initTrigger(int level, String match) throws EncodingException, InvalidTriggerDataException {
        ConfigResponse config = new ConfigResponse();
        config.setValue(ConditionalTriggerInterface.CFG_ID, RESOURCE.getID());
        config.setValue(ConditionalTriggerInterface.CFG_TYPE, RESOURCE.getType());
        config.setValue(ConditionalTriggerInterface.CFG_OPTION, match);
        config.setValue(ConditionalTriggerInterface.CFG_NAME, String.valueOf(level));
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
     * Verifies that trigger fires if event is received matching expected log
     * level
     * @throws EventTypeException
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     */
    public void testProcessEventLevelMatch() throws EventTypeException, EncodingException, InvalidTriggerDataException {
        initTrigger(LogTrackPlugin.LOGLEVEL_INFO, null);
        ResourceLogEvent event = new ResourceLogEvent(new TrackEvent(RESOURCE,
                                                                     System.currentTimeMillis(),
                                                                     LogTrackPlugin.LOGLEVEL_INFO,
                                                                     "source",
                                                                     "myMessage"));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Firing log event trigger: Level(" +
                                ResourceLogEvent.getLevelString(LogTrackPlugin.LOGLEVEL_INFO) + ") and Match(null)");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that nothing happens if event is received not matching expected
     * log level
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     * @throws EventTypeException
     */
    public void testProcessEventLevelMismatch() throws EncodingException,
                                               InvalidTriggerDataException,
                                               EventTypeException
    {
        initTrigger(LogTrackPlugin.LOGLEVEL_INFO, null);
        ResourceLogEvent event = new ResourceLogEvent(new TrackEvent(RESOURCE,
                                                                     System.currentTimeMillis(),
                                                                     LogTrackPlugin.LOGLEVEL_DEBUG,
                                                                     "source",
                                                                     "myMessage"));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies that trigger fires if event is received matching expected log
     * message
     * @throws EventTypeException
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     */
    public void testProcessEventMessageMatch() throws EventTypeException,
                                              EncodingException,
                                              InvalidTriggerDataException
    {
        initTrigger(LogTrackPlugin.LOGLEVEL_ANY, "myMessage");
        ResourceLogEvent event = new ResourceLogEvent(new TrackEvent(RESOURCE,
                                                                     System.currentTimeMillis(),
                                                                     LogTrackPlugin.LOGLEVEL_DEBUG,
                                                                     "source",
                                                                     "myMessage"));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(TRIGGER_ID, event);
        triggerFired.setMessage("Firing log event trigger: Level(" +
                                ResourceLogEvent.getLevelString(LogTrackPlugin.LOGLEVEL_ANY) + ") and Match(myMessage)");
        alertConditionEvaluator.triggerFired(TriggerFiredEventMatcher.eqTriggerFiredEvent(triggerFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies TriggerNotFired if event is received with log message not
     * matching expected message
     * @throws EventTypeException
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     */
    public void testProcessEventMessageMismatch() throws EventTypeException,
                                                 EncodingException,
                                                 InvalidTriggerDataException
    {
        initTrigger(LogTrackPlugin.LOGLEVEL_INFO, "myMessage");
        ResourceLogEvent event = new ResourceLogEvent(new TrackEvent(RESOURCE,
                                                                     System.currentTimeMillis(),
                                                                     LogTrackPlugin.LOGLEVEL_INFO,
                                                                     "source",
                                                                     "yourMessage"));
        TriggerNotFiredEvent notFired = new TriggerNotFiredEvent(TRIGGER_ID);
        notFired.setTimestamp(event.getTimestamp());
        alertConditionEvaluator.triggerNotFired(TriggerNotFiredEventMatcher.eqTriggerNotFiredEvent(notFired));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies nothing happens if log event is received against the wrong
     * resource
     * @throws EventTypeException
     * @throws EncodingException
     * @throws InvalidTriggerDataException
     */
    public void testProcessEventWrongResource() throws EventTypeException,
                                               EncodingException,
                                               InvalidTriggerDataException
    {
        initTrigger(LogTrackPlugin.LOGLEVEL_INFO, "myMessage");
        ResourceLogEvent event = new ResourceLogEvent(new TrackEvent(new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER,
                                                                                        89765),
                                                                     System.currentTimeMillis(),
                                                                     LogTrackPlugin.LOGLEVEL_INFO,
                                                                     "source",
                                                                     "myMessage"));
        EasyMock.replay(alertConditionEvaluator);
        trigger.processEvent(event);
        EasyMock.verify(alertConditionEvaluator);
    }

    /**
     * Verifies an {@link EventTypeException} occurs if wrong event type is
     * passed in
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
