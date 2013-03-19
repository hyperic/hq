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
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;

/**
 * Unit test of {@link SingleConditionEvaluator}
 * @author jhickey
 *
 */
public class SingleConditionEvaluatorTest
    extends TestCase
{

    private ExecutionStrategy executionStrategy;

    private static final Integer TEST_ALERT_DEF_ID = Integer.valueOf(4567);

    public void setUp() throws Exception {
        super.setUp();
        this.executionStrategy = EasyMock.createMock(ExecutionStrategy.class);
    }

    /**
     * Verifies an {@link AlertConditionsSatisfiedZEvent} is created and
     * forwarded to the {@link ExecutionStrategy} when a trigger fires
     */
    public void testSingleConditionMet() {
        Integer triggerId = Integer.valueOf(5678);
        SingleConditionEvaluator evaluator = new SingleConditionEvaluator(TEST_ALERT_DEF_ID, executionStrategy);
        MockEvent mockEvent = new MockEvent(2l, 3);
        // event occurred 3 minutes ago
        mockEvent.setTimestamp(System.currentTimeMillis() - (3 * 60 * 1000));
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(triggerId, mockEvent);

        AlertConditionsSatisfiedZEvent event = new AlertConditionsSatisfiedZEvent(TEST_ALERT_DEF_ID,
                                                                                  new TriggerFiredEvent[] { triggerFired });
        EasyMock.expect(executionStrategy.conditionsSatisfied(event)).andReturn(true);
        EasyMock.replay(executionStrategy);
        evaluator.triggerFired(triggerFired);
        EasyMock.verify(executionStrategy);
    }

}
