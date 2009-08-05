package org.hyperic.hq.events.ext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;

/**
 * Unit test of {@link RegisteredTriggers}
 * @author jhickey
 *
 */
public class RegisteredTriggersTest
    extends TestCase
{
    private RegisteredTriggerManagerLocal registeredTriggerManager;

    private RegisteredTriggers registeredTriggers;

    private void replay() {
        EasyMock.replay(registeredTriggerManager);
    }

    /**
     * Sets up the tests
     */
    public void setUp() throws Exception {
        super.setUp();
        this.registeredTriggerManager = EasyMock.createMock(RegisteredTriggerManagerLocal.class);
        this.registeredTriggers = new RegisteredTriggers(registeredTriggerManager);
    }

    /**
     * Verifies that the init method makes the proper call to its RTM
     */
    public void testInit() {
        registeredTriggerManager.initializeTriggers(registeredTriggers);
        replay();
        registeredTriggers.init();
        verify();
    }

    /**
     * Verifies successful register of triggers and retrieval through instance
     * and static methods
     */
    public void testRegisterAndRetrieve() {
        Integer triggerId = Integer.valueOf(987);
        Integer trigger2Id = Integer.valueOf(456);
        Integer[] interestedInstances = new Integer[] { 123, 456 };

        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        RegisterableTriggerInterface trigger2 = EasyMock.createMock(RegisterableTriggerInterface.class);

        EasyMock.expect(trigger1.getId()).andReturn(triggerId).times(2);
        EasyMock.expect(trigger1.getInterestedEventTypes()).andReturn(new Class[] { MockEvent.class });
        EasyMock.expect(trigger1.getInterestedInstanceIDs(MockEvent.class)).andReturn(interestedInstances);

        EasyMock.expect(trigger2.getId()).andReturn(trigger2Id).times(2);
        EasyMock.expect(trigger2.getInterestedEventTypes()).andReturn(new Class[] { MockEvent.class });
        EasyMock.expect(trigger2.getInterestedInstanceIDs(MockEvent.class)).andReturn(interestedInstances);

        EasyMock.replay(trigger1, trigger2);
        replay();
        registeredTriggers.addTrigger(trigger1);
        registeredTriggers.addTrigger(trigger2);
        verify();
        EasyMock.verify(trigger1, trigger2);

        Map registeredTriggersMap = registeredTriggers.getTriggers();
        assertEquals(2, registeredTriggersMap.size());
        Map instance1 = (Map) registeredTriggersMap.get(new TriggerEventKey(MockEvent.class, 123));
        assertNotNull(instance1);
        assertEquals(2, instance1.size());
        assertNotNull(instance1.get(987));
        assertNotNull(instance1.get(trigger2Id));

        Map instance2 = (Map) registeredTriggersMap.get(new TriggerEventKey(MockEvent.class, 456));
        assertNotNull(instance2);
        assertEquals(2, instance2.size());
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
     * Verifies that the trigger map is properly cleaned up when the last
     * trigger interested in a specific event is removed
     */
    public void testUnregisterTriggerNoneRemainingForEventKey() {
        Integer triggerId = 567;
        Map testTriggers = new HashMap();
        Map triggersById = new HashMap();
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(triggerId, trigger1);
        testTriggers.put(new TriggerEventKey(MockEvent.class, 123), triggersById);
        registeredTriggers.setTriggers(testTriggers);
        EasyMock.replay(trigger1);
        registeredTriggers.removeTrigger(triggerId);
        EasyMock.verify(trigger1);
        Map trigMap = registeredTriggers.getTriggers();
        assertTrue(trigMap.isEmpty());
    }

    /**
     * Verifies that the trigger map is properly cleaned up when a trigger
     * interested in a specific event is removed, but others remain
     */
    public void testUnregisterTriggerSomeRemaining() {
        Integer triggerId = 567;
        Integer trigger2Id = 9908;

        Map testTriggers = new HashMap();

        Map triggersById = new HashMap();
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(triggerId, trigger1);

        RegisterableTriggerInterface trigger2 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(trigger2Id, trigger2);

        testTriggers.put(new TriggerEventKey(MockEvent.class, 123), triggersById);

        registeredTriggers.setTriggers(testTriggers);
        EasyMock.replay(trigger1);
        registeredTriggers.removeTrigger(triggerId);
        EasyMock.verify(trigger1);
        Map trigMap = registeredTriggers.getTriggers();
        Map actualTriggers = (Map) trigMap.get(new TriggerEventKey(MockEvent.class, 123));
        assertNotNull(actualTriggers);
        assertEquals(1, actualTriggers.size());
        assertEquals(trigger2, (RegisterableTriggerInterface) actualTriggers.get(trigger2Id));
    }

    private void verify() {
        EasyMock.verify(registeredTriggerManager);
    }

}
