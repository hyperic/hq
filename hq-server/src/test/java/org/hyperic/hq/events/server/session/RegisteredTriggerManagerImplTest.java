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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.AlertConditionEvaluatorStateRepository;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.ext.MockTrigger;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisterableTriggerRepository;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Unit test of the {@link RegisteredTriggerManagerImpl}
 * @author jhickey
 *
 */
public class RegisteredTriggerManagerImplTest
    extends TestCase
{

    private AlertConditionEvaluatorFactory alertConditionEvaluatorFactory;
    private AlertConditionEvaluator alertConditionEvaluator;
    private TriggerDAOInterface triggerDAO;
    private RegisteredTriggerManagerImpl registeredTriggerManager;
    private RegisterableTriggerRepository registeredTriggerRepository;
    private AlertConditionEvaluatorRepository alertConditionEvaluatorRepository;
    private ZeventEnqueuer zEventEnqueuer;
    private AlertDefinitionDAOInterface alertDefinitionDAO;
    private AlertDAO alertDAO;
    private EventLogManager eventLogManager;
    private AlertConditionEvaluatorStateRepository alertConditionEvaluatorStateRepository;
    private ExecutionStrategy executionStrategy;
    private DBUtil dbUtil;

    private void replay() {
        EasyMock.replay(alertConditionEvaluatorFactory,
                        alertConditionEvaluator,
                        triggerDAO,
                        registeredTriggerRepository, zEventEnqueuer, alertConditionEvaluatorRepository, alertDefinitionDAO, eventLogManager, alertConditionEvaluatorStateRepository, executionStrategy);
        org.easymock.classextension.EasyMock.replay(alertDAO, dbUtil);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.alertConditionEvaluatorFactory = EasyMock.createMock(AlertConditionEvaluatorFactory.class);
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
        this.triggerDAO = EasyMock.createMock(TriggerDAOInterface.class);
        this.registeredTriggerRepository = EasyMock.createMock(RegisterableTriggerRepository.class);
        this.zEventEnqueuer = EasyMock.createMock(ZeventEnqueuer.class);
        this.alertConditionEvaluatorRepository = EasyMock.createMock(AlertConditionEvaluatorRepository.class);
        this.alertDefinitionDAO = EasyMock.createMock(AlertDefinitionDAOInterface.class);
        this.alertDAO = org.easymock.classextension.EasyMock.createMock(AlertDAO.class);
        this.dbUtil = org.easymock.classextension.EasyMock.createMock(DBUtil.class);
        this.eventLogManager = EasyMock.createMock(EventLogManager.class);
        this.alertConditionEvaluatorStateRepository = EasyMock.createMock(AlertConditionEvaluatorStateRepository.class);
        this.executionStrategy = EasyMock.createMock(ExecutionStrategy.class);
        this.registeredTriggerManager = new RegisteredTriggerManagerImpl(alertConditionEvaluatorFactory,triggerDAO,zEventEnqueuer,
            alertConditionEvaluatorRepository,alertDefinitionDAO, registeredTriggerRepository, alertDAO, eventLogManager, dbUtil);
        MockTrigger.initialized = false;
        MockTrigger.enabled = false;
    }

    /**
     * Verifies successful disable of triggers
     */
    public void testDisableTriggers() {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);
        List<Integer> triggerIds = new ArrayList<Integer>();
        triggerIds.add(triggerId);
        Map<Integer,List<Integer>> alertDefTriggerMap = new HashMap<Integer,List<Integer>>();
        alertDefTriggerMap.put(alertDefinitionId, triggerIds);
        registeredTriggerRepository.setTriggersEnabled(triggerIds, false);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefTriggerMap, false);
        verify();
    }


    /**
     * Verify that triggers already initialized can be enabled
     */
    public void testEnableInitializedTriggers() {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);
        List<Integer> triggerIds = new ArrayList<Integer>();
        triggerIds.add(triggerId);
        Map<Integer,List<Integer>> alertDefTriggerMap = new HashMap<Integer,List<Integer>>();
        alertDefTriggerMap.put(alertDefinitionId, triggerIds);
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        EasyMock.expect(registeredTriggerRepository.getTriggerById(triggerId)).andReturn(trigger1);
        registeredTriggerRepository.setTriggersEnabled(triggerIds, true);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefTriggerMap, true);
        verify();
    }

    /**
     * Verifies successful retrieval of triggers IDs by alert definition ID
     */
    public void testGetTriggerIdsByAlertDefId() {
        Integer alertDefinitionId = Integer.valueOf(5432);
        Integer triggerId = Integer.valueOf(987);
        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);

        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);
        
        List<Integer> expectedIds = new ArrayList<Integer>();
        expectedIds.add(triggerId);
        
        Map<Integer,List<Integer>> expectedMap = new HashMap<Integer,List<Integer>>();
        expectedMap.put(alertDefinitionId, expectedIds);

        EasyMock.expect(triggerDAO
                            .findTriggerIdsByAlertDefinitionIds(
                                    Collections.singletonList(alertDefinitionId))
                       ).andReturn(expectedMap);
        replay();
        Map<Integer,List<Integer>> triggerMap = registeredTriggerManager
                            .getTriggerIdsByAlertDefIds(
                                    Collections.singletonList(alertDefinitionId));
        verify();
        assertEquals(expectedMap,triggerMap);
    }

    /**
     * Verifies that triggers are successfully created after commit
     */
    public void testHandleTriggerCreation() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        alertDef.setActiveStatus(true);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);

        RegisteredTriggerValue mockTrigger2 = new RegisteredTriggerValue();
        mockTrigger2.setClassname(MockTrigger.class.getName());
        mockTrigger2.setId(trigger2Id);

        RegisteredTrigger trigger2 = new RegisteredTrigger(mockTrigger2);
        trigger2.setId(mockTrigger2.getId());
        trigger2.setAlertDefinition(alertDef);

        List<RegisteredTrigger> triggers = new ArrayList<RegisteredTrigger>();
        triggers.add(trigger);
        triggers.add(trigger2);

        List<TriggersCreatedZevent> createdEvents = new ArrayList<TriggersCreatedZevent>();
        createdEvents.add(new TriggersCreatedZevent(alertDefinitionId));

        EasyMock.expect(triggerDAO.findByAlertDefinitionId(alertDefinitionId)).andReturn(triggers);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        EasyMock.expectLastCall().times(2);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.handleTriggerCreatedEvents(createdEvents);
        verify();
        assertTrue(MockTrigger.initialized);
        assertTrue(MockTrigger.enabled);
    }

    /**
     * Verifies that nothing blows up if an alert def is not found for triggers to be created
     */
    public void testHandleTriggerCreationAlertDefNotFound() {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);

        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());

        List<RegisteredTrigger> triggers = new ArrayList<RegisteredTrigger>();
        triggers.add(trigger);

        List<TriggersCreatedZevent> createdEvents = new ArrayList<TriggersCreatedZevent>();
        createdEvents.add(new TriggersCreatedZevent(alertDefinitionId));

        EasyMock.expect(triggerDAO.findByAlertDefinitionId(alertDefinitionId)).andReturn(triggers);

        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.handleTriggerCreatedEvents(createdEvents);
        verify();
    }

    /**
     * Verifies that other triggers will still be created if an error occurs registering a single trigger
     */
    public void testHandleTriggerCreationErrorRegistering() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        alertDef.setEnabled(true);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);

        RegisteredTriggerValue mockTrigger2 = new RegisteredTriggerValue();
        mockTrigger2.setClassname(MockTrigger.class.getName());
        mockTrigger2.setId(trigger2Id);

        RegisteredTrigger trigger2 = new RegisteredTrigger(mockTrigger2);
        trigger2.setId(mockTrigger2.getId());
        trigger2.setAlertDefinition(alertDef);

        List<RegisteredTrigger> triggers = new ArrayList<RegisteredTrigger>();
        triggers.add(trigger);
        triggers.add(trigger2);

        List<TriggersCreatedZevent> createdEvents = new ArrayList<TriggersCreatedZevent>();
        createdEvents.add(new TriggersCreatedZevent(alertDefinitionId));

        EasyMock.expect(triggerDAO.findByAlertDefinitionId(alertDefinitionId)).andReturn(triggers);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        EasyMock.expectLastCall().andThrow(new RuntimeException("Oh No!"));
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.handleTriggerCreatedEvents(createdEvents);
        verify();
        assertTrue(MockTrigger.initialized);
        assertTrue(MockTrigger.enabled);
    }



    /**
     * Verifies that triggers are properly created, initialized, and added to
     * the repository
     */
    public void testInitializeTriggers() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        alertDef.setActiveStatus(true);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);

        RegisteredTriggerValue mockTrigger2 = new RegisteredTriggerValue();
        mockTrigger2.setClassname(MockTrigger.class.getName());
        mockTrigger2.setId(trigger2Id);

        RegisteredTrigger trigger2 = new RegisteredTrigger(mockTrigger2);
        trigger2.setId(mockTrigger2.getId());
        trigger2.setAlertDefinition(alertDef);

        Set<RegisteredTrigger> triggers = new HashSet<RegisteredTrigger>();
        triggers.add(trigger);
        triggers.add(trigger2);

        registeredTriggerRepository.init();
        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
        EasyMock.expect(eventLogManager.findLastUnfixedAlertFiredEvents()).andReturn(new HashMap<Integer,AlertFiredEvent>(0,1));
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(null);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getStateRepository()).andReturn(alertConditionEvaluatorStateRepository).times(2);
        Map<Integer,Serializable> alertConditionEvaluatorStates = new HashMap<Integer,Serializable>();
        alertConditionEvaluatorStates.put(alertDefinitionId, "state");
        EasyMock.expect(alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates()).andReturn(alertConditionEvaluatorStates);
        alertConditionEvaluator.initialize("state");
        Map<Integer,Serializable> executionStrategyStates = new HashMap<Integer,Serializable>();
        executionStrategyStates.put(alertDefinitionId, "moreState");
        EasyMock.expect(alertConditionEvaluatorStateRepository.getExecutionStrategyStates()).andReturn(executionStrategyStates);
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        executionStrategy.initialize("moreState");
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(alertConditionEvaluator).times(3);
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        EasyMock.expectLastCall().times(2);
        replay();
        registeredTriggerManager.initializeTriggers();
        verify();
        assertTrue(MockTrigger.initialized);
        assertTrue(MockTrigger.enabled);
    }

    /**
     * Verifies that other triggers will be processed and no Exceptions thrown
     * if an alert definition for a given trigger is not found for some reason
     */
    @SuppressWarnings("unchecked")
    public void testInitializeTriggersAlertDefNotFound() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        alertDef.setActiveStatus(true);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());

        RegisteredTriggerValue mockTrigger2 = new RegisteredTriggerValue();
        mockTrigger2.setClassname(MockTrigger.class.getName());
        mockTrigger2.setId(trigger2Id);

        RegisteredTrigger trigger2 = new RegisteredTrigger(mockTrigger2);
        trigger2.setId(mockTrigger2.getId());
        trigger2.setAlertDefinition(alertDef);

        Set<RegisteredTrigger> triggers = new HashSet<RegisteredTrigger>();
        triggers.add(trigger);
        triggers.add(trigger2);

        registeredTriggerRepository.init();
        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
        EasyMock.expect(eventLogManager.findLastUnfixedAlertFiredEvents()).andReturn(new HashMap<Integer,AlertFiredEvent>(0,1));
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(null);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getStateRepository()).andReturn(alertConditionEvaluatorStateRepository).times(2);
        EasyMock.expect(alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates()).andReturn(Collections.EMPTY_MAP);
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        EasyMock.expect(alertConditionEvaluatorStateRepository.getExecutionStrategyStates()).andReturn(Collections.EMPTY_MAP);
        alertConditionEvaluator.initialize(null);
        executionStrategy.initialize(null);
        
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(alertConditionEvaluator);
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.initializeTriggers();
        verify();
        assertTrue(MockTrigger.initialized);
        assertTrue(MockTrigger.enabled);
    }

    /**
     * Verifies that triggers are still initialized if a single trigger cannot be registered
     */
    @SuppressWarnings("unchecked")
    public void testInitializeTriggersErrorRegistering() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        alertDef.setEnabled(true);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);

        RegisteredTriggerValue mockTrigger2 = new RegisteredTriggerValue();
        mockTrigger2.setClassname(MockTrigger.class.getName());
        mockTrigger2.setId(trigger2Id);

        RegisteredTrigger trigger2 = new RegisteredTrigger(mockTrigger2);
        trigger2.setId(mockTrigger2.getId());
        trigger2.setAlertDefinition(alertDef);

        Set<RegisteredTrigger> triggers = new HashSet<RegisteredTrigger>();
        triggers.add(trigger);
        triggers.add(trigger2);

        registeredTriggerRepository.init();
        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
        EasyMock.expect(eventLogManager.findLastUnfixedAlertFiredEvents()).andReturn(new HashMap<Integer,AlertFiredEvent>(0,1));
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(null);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getStateRepository()).andReturn(alertConditionEvaluatorStateRepository).times(2);
        EasyMock.expect(alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates()).andReturn(Collections.EMPTY_MAP);
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        EasyMock.expect(alertConditionEvaluatorStateRepository.getExecutionStrategyStates()).andReturn(Collections.EMPTY_MAP);
        alertConditionEvaluator.initialize(null);
        executionStrategy.initialize(null);
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(alertConditionEvaluator).times(3);
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        EasyMock.expectLastCall().andThrow(new RuntimeException("Oh No!"));
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.initializeTriggers();
        verify();
        assertTrue(MockTrigger.initialized);
        assertTrue(MockTrigger.enabled);
    }

    
    /**
     * Verifies that other triggers will be processed and no Exceptions thrown
     * if an Exception occurs retriving unfixed alert fired events
     */
   public void testInitializeTriggersExceptionInitializing2() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        alertDef.setActiveStatus(true);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);

        RegisteredTriggerValue mockTrigger2 = new RegisteredTriggerValue();
        mockTrigger2.setClassname(MockTrigger.class.getName());
        mockTrigger2.setId(trigger2Id);

        RegisteredTrigger trigger2 = new RegisteredTrigger(mockTrigger2);
        trigger2.setId(mockTrigger2.getId());
        trigger2.setAlertDefinition(alertDef);

        Set<RegisteredTrigger> triggers = new HashSet<RegisteredTrigger>();
        triggers.add(trigger);
        triggers.add(trigger2);

        registeredTriggerRepository.init();
        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
        EasyMock.expect(eventLogManager.findLastUnfixedAlertFiredEvents()).andThrow(new RuntimeException("Oh No!"));
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(null);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getStateRepository()).andReturn(alertConditionEvaluatorStateRepository).times(2);
        Map<Integer,Serializable> alertConditionEvaluatorStates = new HashMap<Integer,Serializable>();
        alertConditionEvaluatorStates.put(alertDefinitionId, "state");
        EasyMock.expect(alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates()).andReturn(alertConditionEvaluatorStates);
        alertConditionEvaluator.initialize("state");
        Map<Integer,Serializable> executionStrategyStates = new HashMap<Integer,Serializable>();
        executionStrategyStates.put(alertDefinitionId, "moreState");
        EasyMock.expect(alertConditionEvaluatorStateRepository.getExecutionStrategyStates()).andReturn(executionStrategyStates);
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        executionStrategy.initialize("moreState");
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(alertConditionEvaluator).times(3);
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        EasyMock.expectLastCall().times(2);
        replay();
        registeredTriggerManager.initializeTriggers();
        verify();
        assertTrue(MockTrigger.initialized);
        assertTrue(MockTrigger.enabled);
    }
    
    /**
     * Verifies that other triggers will be processed and no Exceptions thrown
     * if an Exception occurs registering a single trigger
     */
    @SuppressWarnings("unchecked")
    public void testInitializeTriggersExceptionInitializing() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);
        Integer alertDefinition2Id = Integer.valueOf(8997);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        alertDef.setActiveStatus(true);
        AlertDefinition alertDef2 = new AlertDefinition();
        alertDef2.setId(alertDefinition2Id);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);

        RegisteredTriggerValue mockTrigger2 = new RegisteredTriggerValue();
        mockTrigger2.setClassname(MockTrigger.class.getName());
        mockTrigger2.setId(trigger2Id);

        RegisteredTrigger trigger2 = new RegisteredTrigger(mockTrigger2);
        trigger2.setId(mockTrigger2.getId());
        trigger2.setAlertDefinition(alertDef2);

        Set<RegisteredTrigger> triggers = new HashSet<RegisteredTrigger>();
        triggers.add(trigger);
        triggers.add(trigger2);

        registeredTriggerRepository.init();
        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
        EasyMock.expect(eventLogManager.findLastUnfixedAlertFiredEvents()).andReturn(new HashMap<Integer,AlertFiredEvent>(0,1));
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(null);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getStateRepository()).andReturn(alertConditionEvaluatorStateRepository).times(2);
        EasyMock.expect(alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates()).andReturn(Collections.EMPTY_MAP);
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        EasyMock.expect(alertConditionEvaluatorStateRepository.getExecutionStrategyStates()).andReturn(Collections.EMPTY_MAP);
        alertConditionEvaluator.initialize(null);
        executionStrategy.initialize(null);
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinition2Id)).andReturn(null).times(2);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef2)).andThrow(new RuntimeException("Yikes!"));
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.initializeTriggers();
        verify();
        assertTrue(MockTrigger.initialized);
        assertTrue(MockTrigger.enabled);
    }

    /**
     * Verifies that nothing blows up trying to create a trigger with an invalid
     * classname. We are going to remove old triggers (MultiCondition, Counter,
     * Duration) during upgrade, but this should nicely handle any that are
     * accidentally hanging around for some reason
     * @throws InvalidTriggerDataException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public void testRegisterTriggerInvalidClass() throws InvalidTriggerDataException,
                                                 InstantiationException,
                                                 IllegalAccessException
    {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname("com.fake.nonexistent");
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);

        Set<RegisteredTrigger> triggers = new HashSet<RegisteredTrigger>();
        triggers.add(trigger);

        registeredTriggerRepository.init();
        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
        EasyMock.expect(eventLogManager.findLastUnfixedAlertFiredEvents()).andReturn(new HashMap<Integer,AlertFiredEvent>(0,1));
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(null);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getStateRepository()).andReturn(alertConditionEvaluatorStateRepository).times(2);
        EasyMock.expect(alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates()).andReturn(Collections.EMPTY_MAP);
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        EasyMock.expect(alertConditionEvaluatorStateRepository.getExecutionStrategyStates()).andReturn(Collections.EMPTY_MAP);
        alertConditionEvaluator.initialize(null);
        executionStrategy.initialize(null);
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(alertConditionEvaluator);
        replay();
        registeredTriggerManager.initializeTriggers();
        verify();


    }

    /**
     * Verifies that triggers that have not yet been intialized will be initialized on first enable
     * @throws InterruptedException
     */
    public void testSetTriggersEnabledLazyInit() throws InterruptedException {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);
        List<Integer> triggerIds = new ArrayList<Integer>();
        triggerIds.add(triggerId);
        Map<Integer, List<Integer>> alertDefTriggerMap = new HashMap<Integer, List<Integer>>();
        alertDefTriggerMap.put(alertDefinitionId, triggerIds);
        EasyMock.expect(registeredTriggerRepository.getTriggerById(triggerId)).andReturn(null);
        zEventEnqueuer.enqueueEvents(EasyMock.isA(List.class));
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefTriggerMap, true);
        verify();
    }

    /**
     * Verifies that nothing blows up if an Exception occurs queuing a TriggersCreatedZEvent on first enable
     * @throws InterruptedException
     */
    public void testSetTriggersEnabledLazyInitException() throws InterruptedException {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);
        List<Integer> triggerIds = new ArrayList<Integer>();
        triggerIds.add(triggerId);
        Map<Integer, List<Integer>> alertDefTriggerMap = new HashMap<Integer, List<Integer>>();
        alertDefTriggerMap.put(alertDefinitionId, triggerIds);
        EasyMock.expect(registeredTriggerRepository.getTriggerById(triggerId)).andReturn(null);
        zEventEnqueuer.enqueueEvents(EasyMock.isA(List.class));
        EasyMock.expectLastCall().andThrow(new InterruptedException());
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefTriggerMap, true);
        verify();
    }

    /**
     * Verifies that triggers are successfully removed from the repository
     */
    public void testUnregisterTriggers() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
        alertDef.setId(alertDefinitionId);
        RegisteredTriggerValue mockTrigger = new RegisteredTriggerValue();
        mockTrigger.setClassname(MockTrigger.class.getName());
        mockTrigger.setId(triggerId);

        RegisteredTrigger trigger = new RegisteredTrigger(mockTrigger);
        trigger.setId(mockTrigger.getId());
        trigger.setAlertDefinition(alertDef);

        RegisteredTriggerValue mockTrigger2 = new RegisteredTriggerValue();
        mockTrigger2.setClassname(MockTrigger.class.getName());
        mockTrigger2.setId(trigger2Id);

        RegisteredTrigger trigger2 = new RegisteredTrigger(mockTrigger2);
        trigger2.setId(mockTrigger2.getId());
        trigger2.setAlertDefinition(alertDef);

        List<RegisteredTrigger> triggers = new ArrayList<RegisteredTrigger>();
        triggers.add(trigger);
        triggers.add(trigger2);

        registeredTriggerRepository.removeTrigger(triggerId);
        registeredTriggerRepository.removeTrigger(trigger2Id);
        alertConditionEvaluatorRepository.removeAlertConditionEvaluator(alertDefinitionId);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.unregisterTriggers(alertDefinitionId,triggers);
        verify();
    }
    
    private void verify() {
        EasyMock.verify(alertConditionEvaluatorFactory,
                        alertConditionEvaluator,
                        triggerDAO,
                        registeredTriggerRepository, zEventEnqueuer, alertConditionEvaluatorRepository, alertDefinitionDAO, 
                        eventLogManager, alertConditionEvaluatorStateRepository, executionStrategy);
        org.easymock.classextension.EasyMock.verify(alertDAO, dbUtil);
    }
}
