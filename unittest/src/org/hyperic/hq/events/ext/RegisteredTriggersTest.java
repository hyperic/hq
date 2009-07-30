package org.hyperic.hq.events.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.server.session.AlertConditionEvaluatorFactory;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.RegisteredTrigger;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
/**
 * Unit test of {@link RegisteredTriggers}
 * @author jhickey
 *
 */
public class RegisteredTriggersTest
    extends TestCase
{
    private RegisteredTriggerManagerLocal registeredTriggerManager;
    private AlertDefinitionManagerLocal alertDefinitionManager;
    private AlertConditionEvaluatorFactory alertConditionEvaluatorFactory;
    private AlertConditionEvaluator alertConditionEvaluator;
    private RegisteredTriggers registeredTriggers;

    private void replay() {
        EasyMock.replay(registeredTriggerManager, alertDefinitionManager, alertConditionEvaluatorFactory, alertConditionEvaluator);
    }

    public void setUp() throws Exception {
        super.setUp();
        this.registeredTriggerManager = EasyMock.createMock(RegisteredTriggerManagerLocal.class);
        this.alertDefinitionManager = EasyMock.createMock(AlertDefinitionManagerLocal.class);
        this.alertConditionEvaluatorFactory = EasyMock.createMock(AlertConditionEvaluatorFactory.class);
        this.alertConditionEvaluator = EasyMock.createMock(AlertConditionEvaluator.class);
        this.registeredTriggers = new RegisteredTriggers(registeredTriggerManager,
                                                         alertDefinitionManager,
                                                         alertConditionEvaluatorFactory);
    }

    /**
     * Verifies that triggers are properly created, initialized, and added to the internal triggers map
     */
    public void testInitializeTriggers() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer alertDefinitionId = Integer.valueOf(5432);

        AlertDefinition alertDef = new AlertDefinition();
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

        List triggers = new ArrayList();
        triggers.add(trigger);
        triggers.add(trigger2);
        
        EasyMock.expect(registeredTriggerManager.getTriggers()).andReturn(triggers);
        EasyMock.expect(alertDefinitionManager.getIdFromTrigger(triggerId)).andReturn(alertDefinitionId).times(2);
        EasyMock.expect(alertDefinitionManager.getIdFromTrigger(trigger2Id)).andReturn(alertDefinitionId).times(2);
        EasyMock.expect(alertDefinitionManager.getByIdNoCheck(alertDefinitionId)).andReturn(alertDef);
        EasyMock.expect(alertConditionEvaluatorFactory.create(alertDef)).andReturn(alertConditionEvaluator);
        replay();
        registeredTriggers.initializeTriggers();
        verify();
        assertTrue(MockTrigger.initialized);

        Map registeredTriggersMap = registeredTriggers.getTriggers();
        assertEquals(2,registeredTriggersMap.size());
        Map instance1 = (Map)registeredTriggersMap.get(new TriggerEventKey(MockEvent.class,123));
        assertNotNull(instance1);
        assertEquals(2,instance1.size());
        assertNotNull(instance1.get(987));
        assertNotNull(instance1.get(trigger2Id));

        Map instance2 = (Map)registeredTriggersMap.get(new TriggerEventKey(MockEvent.class,456));
        assertNotNull(instance2);
        assertEquals(2,instance2.size());
        assertNotNull(instance2.get(987));
        assertNotNull(instance2.get(trigger2Id));

        RegisteredTriggers.setInstance(registeredTriggers);
        Collection interestedTriggers = RegisteredTriggers.getInterestedTriggers(new MockEvent(7l, 123));
        assertEquals(2, interestedTriggers.size());

        Collection interestedTriggers2 = RegisteredTriggers.getInterestedTriggers(new MockEvent(7l, 999));
        assertTrue(interestedTriggers2.isEmpty());

        assertTrue(RegisteredTriggers.isTriggerInterested(new MockEvent(7l, 123)));
        assertFalse(RegisteredTriggers.isTriggerInterested(new MockEvent(7l, 999)));
    }



    /**
     * Verifies that the trigger map is properly cleaned up when the last trigger interested in a specific event is removed
     */
    public void testUnregisterTriggerNoneRemainingForEventKey() {
        Integer triggerId = 567;
        Map testTriggers = new HashMap();
        Map triggersById = new HashMap();
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(triggerId, trigger1);
        testTriggers.put(new TriggerEventKey(MockEvent.class,123), triggersById);
        registeredTriggers.setTriggers(testTriggers);
        EasyMock.replay(trigger1);
        registeredTriggers.unregisterTrigger(triggerId);
        EasyMock.verify(trigger1);
        Map trigMap = registeredTriggers.getTriggers();
        assertTrue(trigMap.isEmpty());
    }

    /**
     * Verifies that the trigger map is properly cleaned up when a trigger interested in a specific event is removed, but others remain
     */
    public void testUnregisterTriggerSomeRemaining() {
        Integer triggerId = 567;
        Integer trigger2Id = 9908;
        Integer instance1 = 123;

        Map testTriggers = new HashMap();


        Map triggersById = new HashMap();
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(triggerId, trigger1);

        RegisterableTriggerInterface trigger2 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(trigger2Id, trigger2);

        testTriggers.put(new TriggerEventKey(MockEvent.class,123), triggersById);

        registeredTriggers.setTriggers(testTriggers);
        EasyMock.replay(trigger1);
        registeredTriggers.unregisterTrigger(triggerId);
        EasyMock.verify(trigger1);
        Map trigMap = registeredTriggers.getTriggers();
        Map actualTriggers = (Map)trigMap.get(new TriggerEventKey(MockEvent.class,123));
        assertNotNull(actualTriggers);
        assertEquals(1,actualTriggers.size());
        assertEquals(trigger2,(RegisterableTriggerInterface)actualTriggers.get(trigger2Id));
    }

    private void verify() {
        EasyMock.verify(registeredTriggerManager, alertDefinitionManager, alertConditionEvaluatorFactory, alertConditionEvaluator);
    }

}
