package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.ext.MockTrigger;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.ext.RegisterableTriggerRepository;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.zevents.ZeventEnqueuer;

/**
 * Unit test of the {@link RegisteredTriggerManagerEJBImpl}
 * @author jhickey
 *
 */
public class RegisteredTriggerManagerEJBImplTest
    extends TestCase
{

    private AlertConditionEvaluatorFactory alertConditionEvaluatorFactory;
    private AlertConditionEvaluator alertConditionEvaluator;
    private TriggerDAOInterface triggerDAO;
    private EventLogManagerLocal eventLogManagerLocal;
    private RegisteredTriggerManagerEJBImpl registeredTriggerManager;
    private RegisterableTriggerRepository registeredTriggerRepository;
    private AlertConditionEvaluatorRepository alertConditionEvaluatorRepository;
    private AlertConditionEvaluatorStateRepository alertConditionEvaluatorStateRepository;
    private ExecutionStrategy executionStrategy;
    
    private ZeventEnqueuer zEventEnqueuer;

    private void replay() {
        EasyMock.replay(alertConditionEvaluatorFactory,
                        alertConditionEvaluator,
                        triggerDAO,
                        registeredTriggerRepository, zEventEnqueuer, alertConditionEvaluatorRepository, alertConditionEvaluatorStateRepository, executionStrategy);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.alertConditionEvaluatorFactory = EasyMock.createMock(AlertConditionEvaluatorFactory.class);
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
        this.triggerDAO = EasyMock.createMock(TriggerDAOInterface.class);
        this.eventLogManagerLocal = EasyMock.createMock(EventLogManagerLocal.class);
        this.registeredTriggerRepository = EasyMock.createMock(RegisterableTriggerRepository.class);
        this.zEventEnqueuer = EasyMock.createMock(ZeventEnqueuer.class);
        this.alertConditionEvaluatorRepository = EasyMock.createMock(AlertConditionEvaluatorRepository.class);
        this.alertConditionEvaluatorStateRepository = EasyMock.createMock(AlertConditionEvaluatorStateRepository.class);
        this.executionStrategy = EasyMock.createMock(ExecutionStrategy.class);
        this.registeredTriggerManager = new RegisteredTriggerManagerEJBImpl();
        registeredTriggerManager.setAlertConditionEvaluatorFactory(alertConditionEvaluatorFactory);
        registeredTriggerManager.setTriggerDAO(triggerDAO);
        registeredTriggerManager.setEventLogManagerLocal(eventLogManagerLocal);
        registeredTriggerManager.setZeventEnqueuer(zEventEnqueuer);
        registeredTriggerManager.setAlertConditionEvaluatorRepository(alertConditionEvaluatorRepository);
        MockTrigger.initialized = false;
        MockTrigger.enabled = false;
    }

    /**
     * Verifies successful disable of triggers
     */
    public void testDisableTriggers() {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);
        List triggerIds = new ArrayList();
        triggerIds.add(triggerId);
        registeredTriggerRepository.setTriggersEnabled(triggerIds, false);
        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(true);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefinitionId,triggerIds, false);
        verify();
    }

    /**
     * Verifies nothing happens if setTriggersEnabled is called before the repository is initialized
     */
    public void testDisableTriggersNotInitialized() {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);
        List triggerIds = new ArrayList();
        triggerIds.add(triggerId);
        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(false);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefinitionId,triggerIds, false);
        verify();
    }

    /**
     * Verify that triggers already initialized can be enabled
     */
    public void testEnableInitializedTriggers() {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);
        List triggerIds = new ArrayList();
        triggerIds.add(triggerId);
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        EasyMock.expect(registeredTriggerRepository.getTriggerById(triggerId)).andReturn(trigger1);
        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(true);
        registeredTriggerRepository.setTriggersEnabled(triggerIds, true);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefinitionId, triggerIds, true);
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

        List triggers = new ArrayList();
        triggers.add(trigger);

        List expectedIds = new ArrayList();
        expectedIds.add(triggerId);

        EasyMock.expect(triggerDAO.findByAlertDefinitionId(alertDefinitionId)).andReturn(triggers);
        replay();
        Collection triggerIds = registeredTriggerManager.getTriggerIdsByAlertDefId(alertDefinitionId);
        verify();
        assertEquals(expectedIds,triggerIds);
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

        List triggers = new ArrayList();
        triggers.add(trigger);
        triggers.add(trigger2);

        List createdEvents = new ArrayList();
        createdEvents.add(new TriggersCreatedZevent(alertDefinitionId));

        EasyMock.expect(triggerDAO.findByAlertDefinitionId(alertDefinitionId)).andReturn(triggers);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(true);
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

        List triggers = new ArrayList();
        triggers.add(trigger);

        List createdEvents = new ArrayList();
        createdEvents.add(new TriggersCreatedZevent(alertDefinitionId));

        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(true);
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

        List triggers = new ArrayList();
        triggers.add(trigger);
        triggers.add(trigger2);

        List createdEvents = new ArrayList();
        createdEvents.add(new TriggersCreatedZevent(alertDefinitionId));

        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(true);
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
     * Verifies that trigger creation is not yet processed if repository has not been initialized
     */
    public void testHandleTriggerCreationNotInitialized() {
        Integer alertDefinitionId = Integer.valueOf(5432);

        List createdEvents = new ArrayList();
        createdEvents.add(new TriggersCreatedZevent(alertDefinitionId));
        
        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(false);

        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.handleTriggerCreatedEvents(createdEvents);
        verify();

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

        Set triggers = new HashSet();
        triggers.add(trigger);
        triggers.add(trigger2);

        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(null);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getStateRepository()).andReturn(alertConditionEvaluatorStateRepository).times(2);
        Map alertConditionEvaluatorStates = new HashMap();
        alertConditionEvaluatorStates.put(alertDefinitionId, "state");
        EasyMock.expect(alertConditionEvaluatorStateRepository.getAlertConditionEvaluatorStates()).andReturn(alertConditionEvaluatorStates);
        alertConditionEvaluator.initialize("state");
        Map executionStrategyStates = new HashMap();
        executionStrategyStates.put(alertDefinitionId, "moreState");
        EasyMock.expect(alertConditionEvaluatorStateRepository.getExecutionStrategyStates()).andReturn(executionStrategyStates);
        EasyMock.expect(alertConditionEvaluator.getExecutionStrategy()).andReturn(executionStrategy);
        executionStrategy.initialize("moreState");
        alertConditionEvaluatorRepository.addAlertConditionEvaluator(alertConditionEvaluator);
        EasyMock.expect(alertConditionEvaluatorRepository.getAlertConditionEvaluatorById(alertDefinitionId)).andReturn(alertConditionEvaluator).times(3);
        registeredTriggerRepository.addTrigger(EasyMock.isA(MockTrigger.class));
        EasyMock.expectLastCall().times(2);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.initializeTriggers();
        verify();
        assertTrue(MockTrigger.initialized);
        assertTrue(MockTrigger.enabled);
    }

    /**
     * Verifies that other triggers will be processed and no Exceptions thrown
     * if an alert definition for a given trigger is not found for some reason
     */
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

        Set triggers = new HashSet();
        triggers.add(trigger);
        triggers.add(trigger2);

        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
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

        Set triggers = new HashSet();
        triggers.add(trigger);
        triggers.add(trigger2);

        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
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
     * if an Exception occurs registering a single trigger
     */
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

        Set triggers = new HashSet();
        triggers.add(trigger);
        triggers.add(trigger2);

        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
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

        Set triggers = new HashSet();
        triggers.add(trigger);

        EasyMock.expect(triggerDAO.findAllEnabledTriggers()).andReturn(triggers);
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
        List triggerIds = new ArrayList();
        triggerIds.add(triggerId);
        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(true);
        EasyMock.expect(registeredTriggerRepository.getTriggerById(triggerId)).andReturn(null);
        zEventEnqueuer.enqueueEvent(EasyMock.isA(TriggersCreatedZevent.class));
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefinitionId, triggerIds, true);
        verify();
    }

    /**
     * Verifies that nothing blows up if an Exception occurs queuing a TriggersCreatedZEvent on first enable
     * @throws InterruptedException
     */
    public void testSetTriggersEnabledLazyInitException() throws InterruptedException {
        Integer triggerId = Integer.valueOf(987);
        Integer alertDefinitionId = Integer.valueOf(5432);
        List triggerIds = new ArrayList();
        triggerIds.add(triggerId);
        EasyMock.expect(registeredTriggerRepository.getTriggerById(triggerId)).andReturn(null);
        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(true);
        zEventEnqueuer.enqueueEvent(EasyMock.isA(TriggersCreatedZevent.class));
        EasyMock.expectLastCall().andThrow(new InterruptedException());
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.setTriggersEnabled(alertDefinitionId, triggerIds, true);
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

        List triggers = new ArrayList();
        triggers.add(trigger);
        triggers.add(trigger2);

        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(true);
        registeredTriggerRepository.removeTrigger(triggerId);
        registeredTriggerRepository.removeTrigger(trigger2Id);
        alertConditionEvaluatorRepository.removeAlertConditionEvaluator(alertDefinitionId);
        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.unregisterTriggers(alertDefinitionId,triggers);
        verify();
    }
    
    /**
     * Verifies that nothing happens when attempting to unregister a trigger if repository is not initialized
     */
    public void testUnregisterTriggersNotInitialized() {
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

        List triggers = new ArrayList();
        triggers.add(trigger);
        triggers.add(trigger2);
        
        EasyMock.expect(registeredTriggerRepository.isInitialized()).andReturn(false);

        replay();
        registeredTriggerManager.setRegisteredTriggerRepository(registeredTriggerRepository);
        registeredTriggerManager.unregisterTriggers(alertDefinitionId, triggers);
        verify();
    }

    private void verify() {
        EasyMock.verify(alertConditionEvaluatorFactory,
                        alertConditionEvaluator,
                        triggerDAO,
                        registeredTriggerRepository, zEventEnqueuer, alertConditionEvaluatorRepository, alertConditionEvaluatorStateRepository, executionStrategy);
    }
}
