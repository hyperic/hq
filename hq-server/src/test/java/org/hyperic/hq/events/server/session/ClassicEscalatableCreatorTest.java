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

package org.hyperic.hq.events.server.session;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
/**
 * Unit test of {@link ClassicEscalatableCreator}
 * @author jhickey
 *
 */
public class ClassicEscalatableCreatorTest
    extends TestCase
{

    private MessagePublisher messagePublisher;
    private AlertManager alertManager;

    private static final Integer TEST_ALERT_DEF_ID = Integer.valueOf(1234);

    public void setUp() throws Exception {
        super.setUp();
        this.messagePublisher = EasyMock.createMock(MessagePublisher.class);
        this.alertManager = EasyMock.createMock(AlertManager.class);
    }

    /**
     * Verifies successful creation of alert and escalatable
     * @throws ActionExecuteException
     * @throws ResourceDeletedException
     */
    public void testCreateEscalatable() throws ActionExecuteException, ResourceDeletedException {
        Integer triggerId = Integer.valueOf(8);
        Integer alertId = 9876;
        MockEvent mockEvent = new MockEvent(3l, 4);
        // trigger fired event occurred 12 minutes ago
        final long timestamp = System.currentTimeMillis() - (12 * 60 * 1000);
        mockEvent.setTimestamp(timestamp);
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(triggerId, mockEvent);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID,
                                                                                  new TriggerFiredEvent[] { triggerFired });
        AlertDefinition alertDef = new AlertDefinition();
        AlertCondition condition = new AlertCondition();
        condition.setType(EventConstants.TYPE_THRESHOLD);
        RegisteredTrigger trigger = new RegisteredTrigger();
        trigger.setId(triggerId);
        condition.setTrigger(trigger);
        alertDef.addCondition(condition);

        ClassicEscalatableCreator creator = new ClassicEscalatableCreator(alertDef,
                                                                          event,
                                                                          messagePublisher,
                                                                          alertManager);

        Resource resource = new Resource(new ResourceType(), null, null, null, null, true);
        alertDef.setResource(resource);
        Alert alert = new Alert();
        alert.setId(alertId);
        alert.setAlertDefinition(alertDef);
        EasyMock.expect(alertManager.createAlert(alertDef, timestamp)).andReturn(alert);
        EasyMock.expect(alertManager.getShortReason(alert)).andReturn("short");
        EasyMock.expect(alertManager.getLongReason(alert)).andReturn("long");

        ActionInterface mockAction = new MockAction();
        AuthzSubject noSubject = null;
        alertManager.logActionDetail(EasyMock.eq(alert),
                                     EasyMock.isA(Action.class),
                                     EasyMock.eq("return!"),
                                     EasyMock.eq(noSubject));
        EasyMock.replay(alertManager, messagePublisher);
        Action action = Action.createAction(mockAction);
        alertDef.addAction(action);
        Escalatable escalatable = creator.createEscalatableNoNotify();
        EasyMock.verify(alertManager, messagePublisher);
        //verify condition log was created
        assertEquals(1,alert.getConditionLog().size());

        assertEquals(alert,escalatable.getAlertInfo());
        assertEquals("long",escalatable.getLongReason());
        assertEquals("short",escalatable.getShortReason());
    }

}
