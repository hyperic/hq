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

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.zevents.ZeventEnqueuer;

/**
 * Unit test of {@link AlertConditionEvaluatorFactory}
 * @author jhickey
 * 
 */
public class AlertConditionEvaluatorFactoryImplTest
    extends TestCase
{
    private AlertConditionEvaluatorFactory factory;
    private ZeventEnqueuer zeventEnqueuer;
   
   

    public void setUp() throws Exception {
        this.zeventEnqueuer = EasyMock.createMock(ZeventEnqueuer.class);
        this.factory = new AlertConditionEvaluatorFactoryImpl(zeventEnqueuer);
    }
    
    private void replay() {
        EasyMock.replay(zeventEnqueuer);
    }
    
    private void verify() {
        EasyMock.verify(zeventEnqueuer);
    }

    /**
     * Verifies an {@link AlertConditionEvaluator} is created
     */
    public void testCreateAlertConditionEvaluator() {
        Integer alertDefinitionId = Integer.valueOf(8899);
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setFrequencyType(EventConstants.FREQ_EVERYTIME);
        alertDefinition.setId(alertDefinitionId);

        AlertCondition alertCondition = new AlertCondition();
        RegisteredTrigger trigger = new RegisteredTrigger();
        trigger.setId(1234);
        alertCondition.setTrigger(trigger);
        alertCondition.setType(EventConstants.TYPE_ALERT);
        alertDefinition.addCondition(alertCondition);

        AlertCondition alertCondition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();
        trigger2.setId(12345);
        alertCondition2.setTrigger(trigger2);
        alertCondition2.setType(EventConstants.TYPE_CONTROL);
        alertDefinition.addCondition(alertCondition2);
        replay();
        AlertConditionEvaluator evaluator = factory.create(alertDefinition);
        verify();
        assertTrue(evaluator instanceof RecoveryConditionEvaluator);
        RecoveryConditionEvaluator recoveryConditionEvaluator = (RecoveryConditionEvaluator) evaluator;
        assertTrue(recoveryConditionEvaluator.getExecutionStrategy() instanceof SingleAlertExecutionStrategy);
        assertEquals(0l, recoveryConditionEvaluator.getTimeRange());
        Set<Integer> expectedTriggerIds = new HashSet<Integer>();
        expectedTriggerIds.add(12345);
        assertEquals(expectedTriggerIds, recoveryConditionEvaluator.getTriggerIds());
        assertEquals(Integer.valueOf(1234), recoveryConditionEvaluator.getAlertTriggerId());
    }

    /**
     * Verifies proper creation of an execution strategy for alert definition
     * with multiple conditions and counter frequency
     */
    public void testCreateCounterMultiCondition() {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setFrequencyType(EventConstants.FREQ_COUNTER);
        alertDefinition.setCount(3l);
        alertDefinition.setRange(4l);

        AlertCondition alertCondition = new AlertCondition();
        RegisteredTrigger trigger = new RegisteredTrigger();
        trigger.setId(1234);
        alertCondition.setTrigger(trigger);
        alertCondition.setType(EventConstants.TYPE_THRESHOLD);
        alertDefinition.addCondition(alertCondition);

        AlertCondition alertCondition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();
        trigger2.setId(12345);
        alertCondition2.setTrigger(trigger2);
        alertCondition2.setType(EventConstants.TYPE_CONTROL);
        alertDefinition.addCondition(alertCondition2);

   
        replay();
        AlertConditionEvaluator evaluator = factory.create(alertDefinition);
        verify();
        assertTrue(evaluator instanceof MultiConditionEvaluator);
        MultiConditionEvaluator multiConditionEvaluator = (MultiConditionEvaluator) evaluator;
        assertTrue(multiConditionEvaluator.getExecutionStrategy() instanceof CounterExecutionStrategy);
        assertEquals(4000l, multiConditionEvaluator.getTimeRange());
        CounterExecutionStrategy executionStrategy = (CounterExecutionStrategy) multiConditionEvaluator.getExecutionStrategy();
        assertEquals(3l, executionStrategy.getCount());
        assertEquals(4000l, executionStrategy.getTimeRange());
        Set<Integer> expectedTriggerIds = new HashSet<Integer>();
        expectedTriggerIds.add(1234);
        expectedTriggerIds.add(12345);
        assertEquals(expectedTriggerIds, multiConditionEvaluator.getTriggerIds());
    }

    /**
     * Verifies proper creation of an execution strategy for alert definition
     * with single condition and counter frequency
     */
    public void testCreateCounterSingleCondition() {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setFrequencyType(EventConstants.FREQ_COUNTER);
        alertDefinition.setCount(3l);
        alertDefinition.setRange(4l);
        AlertCondition alertCondition = new AlertCondition();
        RegisteredTrigger trigger = new RegisteredTrigger();
        trigger.setId(1234);
        alertCondition.setTrigger(trigger);
        alertCondition.setType(EventConstants.TYPE_THRESHOLD);
        alertDefinition.addCondition(alertCondition);
        replay();
        AlertConditionEvaluator evaluator = factory.create(alertDefinition);
        verify();
        assertTrue(evaluator instanceof SingleConditionEvaluator);
        assertTrue(((SingleConditionEvaluator) evaluator).getExecutionStrategy() instanceof CounterExecutionStrategy);
        CounterExecutionStrategy executionStrategy = (CounterExecutionStrategy) ((SingleConditionEvaluator) evaluator).getExecutionStrategy();
        assertEquals(3l, executionStrategy.getCount());
        assertEquals(4000l, executionStrategy.getTimeRange());
    }

    /**
     * Verifies proper creation of an execution strategy for a recovery alert
     * definition
     */
    public void testCreateRecoveryCondition() {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setFrequencyType(EventConstants.FREQ_EVERYTIME);

        AlertCondition alertCondition = new AlertCondition();
        RegisteredTrigger trigger = new RegisteredTrigger();
        trigger.setId(1234);
        alertCondition.setTrigger(trigger);
        alertCondition.setType(EventConstants.TYPE_ALERT);
        alertDefinition.addCondition(alertCondition);

        AlertCondition alertCondition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();
        trigger2.setId(12345);
        alertCondition2.setTrigger(trigger2);
        alertCondition2.setType(EventConstants.TYPE_CONTROL);
        alertDefinition.addCondition(alertCondition2);

     
        replay();
        AlertConditionEvaluator evaluator = factory.create(alertDefinition);
        verify();
        assertTrue(evaluator instanceof RecoveryConditionEvaluator);
        RecoveryConditionEvaluator recoveryConditionEvaluator = (RecoveryConditionEvaluator) evaluator;
        assertTrue(recoveryConditionEvaluator.getExecutionStrategy() instanceof SingleAlertExecutionStrategy);
        assertEquals(0l, recoveryConditionEvaluator.getTimeRange());
        Set<Integer> expectedTriggerIds = new HashSet<Integer>();
        expectedTriggerIds.add(12345);
        assertEquals(expectedTriggerIds, recoveryConditionEvaluator.getTriggerIds());
        assertEquals(Integer.valueOf(1234), recoveryConditionEvaluator.getAlertTriggerId());
    }

    /**
     * Verifies handling of unsupported frequency type by using
     * SingleAlertExecutionStrategy (frequency everytime)
     */
    public void testCreateUnsupportedFrequencyType() {
        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setFrequencyType(1);

        AlertCondition alertCondition2 = new AlertCondition();
        RegisteredTrigger trigger2 = new RegisteredTrigger();
        trigger2.setId(12345);
        alertCondition2.setTrigger(trigger2);
        alertCondition2.setType(EventConstants.TYPE_CONTROL);
        alertDefinition.addCondition(alertCondition2);

     
        replay();
        AlertConditionEvaluator evaluator = factory.create(alertDefinition);
        verify();
        assertTrue(evaluator instanceof SingleConditionEvaluator);

        SingleConditionEvaluator singleConditionEvaluator = (SingleConditionEvaluator) evaluator;
        assertTrue(singleConditionEvaluator.getExecutionStrategy() instanceof SingleAlertExecutionStrategy);

    }
}
