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

import org.easymock.EasyMock;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.zevents.ZeventEnqueuer;

import junit.framework.TestCase;
/**
 * Unit test of {@link SingleAlertExecutionStrategy}
 * @author jhickey
 *
 */
public class SingleAlertExecutionStrategyTest extends TestCase {
    private SingleAlertExecutionStrategy executionStrategy;
    private ZeventEnqueuer zeventEnqueuer;

    public void setUp() throws Exception {
        super.setUp();
        this.zeventEnqueuer = EasyMock.createMock(ZeventEnqueuer.class);
        this.executionStrategy = new SingleAlertExecutionStrategy(zeventEnqueuer);
    }

    /**
     * Verifies that nothing blows up if failure to enqueue an {@link AlertConditionsSatisfiedZEvent}
     * @throws InterruptedException
     */
    public void testErrorEnqueuingEvent() throws InterruptedException {
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, new MockEvent(1l, 2));
        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(1234,
                                                                                  new TriggerFiredEvent[] { triggerFired });
        zeventEnqueuer.enqueueEvent(event);
        EasyMock.expectLastCall().andThrow(new InterruptedException("Something Bad"));
        EasyMock.replay(zeventEnqueuer);
        executionStrategy.conditionsSatisfied(event);
        EasyMock.verify(zeventEnqueuer);
    }

    /**
     * Verifies an {@link AlertConditionsSatisfiedZEvent} is enqueued
     * @throws InterruptedException
     */
    public void testSingleAlert() throws InterruptedException {
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(3, new MockEvent(1l, 2));
        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(1234,
                                                                                  new TriggerFiredEvent[] { triggerFired });
        zeventEnqueuer.enqueueEvent(event);
        EasyMock.replay(zeventEnqueuer);
        executionStrategy.conditionsSatisfied(event);
        EasyMock.verify(zeventEnqueuer);
    }

}
