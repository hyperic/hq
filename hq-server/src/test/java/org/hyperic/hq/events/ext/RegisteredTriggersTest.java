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

package org.hyperic.hq.events.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.server.session.AlertRegulator;

/**
 * Unit test of {@link RegisteredTriggers}
 * @author jhickey
 * 
 */
public class RegisteredTriggersTest
    extends TestCase {

    private RegisteredTriggers registeredTriggers;

    private AlertRegulator alertRegulator;

    private void replay() {
        EasyMock.replay(alertRegulator);
    }

    /**
     * Sets up the tests
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.alertRegulator = EasyMock.createMock(AlertRegulator.class);
        this.registeredTriggers = new RegisteredTriggers(alertRegulator);
    }

    /**
     * Verifies successful disable of triggers
     * @throws InvalidTriggerDataException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void testDisableTriggers() {
        Integer triggerId = Integer.valueOf(987);

        Integer[] interestedInstances = new Integer[] { 123, 456 };

        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);

        EasyMock.expect(trigger1.getId()).andReturn(triggerId).times(2);
        EasyMock.expect(trigger1.getInterestedEventTypes()).andReturn(new Class[] { MockEvent.class });
        EasyMock.expect(trigger1.getInterestedInstanceIDs(MockEvent.class)).andReturn(interestedInstances);

        trigger1.setEnabled(false);

        List<Integer> expectedDisabledTriggers = new ArrayList<Integer>();
        expectedDisabledTriggers.add(triggerId);

        EasyMock.replay(trigger1);
        replay();
        registeredTriggers.addTrigger(trigger1);
        registeredTriggers.setTriggersEnabled(expectedDisabledTriggers, false);
        verify();
        EasyMock.verify(trigger1);
    }

    /**
     * Verifies that no interested triggers are returned if alerts are globally
     * disabled
     * @throws InvalidTriggerDataException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void testGetInterestedTriggersAlertsGloballyDisabled() {
        Integer triggerId = Integer.valueOf(987);

        Integer[] interestedInstances = new Integer[] { 123, 456 };

        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);

        EasyMock.expect(trigger1.getId()).andReturn(triggerId).times(2);
        EasyMock.expect(trigger1.getInterestedEventTypes()).andReturn(new Class[] { MockEvent.class });
        EasyMock.expect(trigger1.getInterestedInstanceIDs(MockEvent.class)).andReturn(interestedInstances);
        EasyMock.expect(alertRegulator.alertsAllowed()).andReturn(false).times(5);
        EasyMock.replay(trigger1);
        replay();
        registeredTriggers.addTrigger(trigger1);
        assertTrue(registeredTriggers.getInterestedTriggers(new MockEvent(3l, 123), 123).isEmpty());

        assertFalse(registeredTriggers.isTriggerInterested(new MockEvent(7l, 123)));
        registeredTriggers.getInterestedTriggers(new MockEvent(3l, 123)).isEmpty();
        verify();
        EasyMock.verify(trigger1);
    }

    /**
     * Verifies that the init method properly clears the internal state
     */
    public void testInit() {
        Integer triggerId = Integer.valueOf(987);
        Integer[] interestedInstances = new Integer[] { 123, 456 };
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        EasyMock.expect(trigger1.getId()).andReturn(triggerId).times(2);
        EasyMock.expect(trigger1.getInterestedEventTypes()).andReturn(new Class[] { MockEvent.class });
        EasyMock.expect(trigger1.getInterestedInstanceIDs(MockEvent.class)).andReturn(interestedInstances);   
        EasyMock.replay(trigger1);
        replay();
        registeredTriggers.addTrigger(trigger1);
        registeredTriggers.init();
        EasyMock.verify(trigger1);
        verify();
        assertTrue(registeredTriggers.getTriggers().isEmpty());
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

        EasyMock.expect(trigger1.isEnabled()).andReturn(true).times(2);
        EasyMock.expect(trigger2.isEnabled()).andReturn(true).times(2);

        EasyMock.replay(trigger1, trigger2);
        EasyMock.expect(alertRegulator.alertsAllowed()).andReturn(true).times(7);
        replay();
        registeredTriggers.addTrigger(trigger1);
        registeredTriggers.addTrigger(trigger2);

        Map<TriggerEventKey, Map<Integer, RegisterableTriggerInterface>> registeredTriggersMap = registeredTriggers
            .getTriggers();
        assertEquals(2, registeredTriggersMap.size());
        Map<Integer, RegisterableTriggerInterface> instance1 = registeredTriggersMap.get(new TriggerEventKey(
            MockEvent.class, 123));
        assertNotNull(instance1);
        assertEquals(2, instance1.size());
        assertNotNull(instance1.get(987));
        assertNotNull(instance1.get(trigger2Id));

        Map<Integer, RegisterableTriggerInterface> instance2 = registeredTriggersMap.get(new TriggerEventKey(
            MockEvent.class, 456));
        assertNotNull(instance2);
        assertEquals(2, instance2.size());
        assertNotNull(instance2.get(987));
        assertNotNull(instance2.get(trigger2Id));
        
        

        Collection<RegisterableTriggerInterface> interestedTriggers = registeredTriggers
            .getInterestedTriggers(new MockEvent(7l, 123));
        assertEquals(2, interestedTriggers.size());

        Collection<RegisterableTriggerInterface> interestedTriggers2 = registeredTriggers
            .getInterestedTriggers(new MockEvent(7l, 999));
        assertTrue(interestedTriggers2.isEmpty());

        assertTrue(registeredTriggers.isTriggerInterested(new MockEvent(7l, 123)));
        assertFalse(registeredTriggers.isTriggerInterested(new MockEvent(7l, 999)));

        EasyMock.verify(trigger1, trigger2);
        verify();
    }

    /**
     * Verifies successful register of triggers and retrieval of enabled
     * triggers only through instance and static methods
     */
    public void testRegisterAndRetrieveTriggerDisabled() {
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

        EasyMock.expect(trigger1.isEnabled()).andReturn(true).times(2);
        EasyMock.expect(trigger2.isEnabled()).andReturn(false).times(2);
        
        EasyMock.expect(alertRegulator.alertsAllowed()).andReturn(true).times(7);

        EasyMock.replay(trigger1, trigger2);
        replay();
        registeredTriggers.addTrigger(trigger1);
        registeredTriggers.addTrigger(trigger2);

        Map<TriggerEventKey, Map<Integer, RegisterableTriggerInterface>> registeredTriggersMap = registeredTriggers
            .getTriggers();
        assertEquals(2, registeredTriggersMap.size());
        Map<Integer, RegisterableTriggerInterface> instance1 = registeredTriggersMap.get(new TriggerEventKey(
            MockEvent.class, 123));
        assertNotNull(instance1);
        assertEquals(2, instance1.size());
        assertNotNull(instance1.get(987));
        assertNotNull(instance1.get(trigger2Id));

        Map<Integer, RegisterableTriggerInterface> instance2 = registeredTriggersMap.get(new TriggerEventKey(
            MockEvent.class, 456));
        assertNotNull(instance2);
        assertEquals(2, instance2.size());
        assertNotNull(instance2.get(987));
        assertNotNull(instance2.get(trigger2Id));

        Collection<RegisterableTriggerInterface> interestedTriggers = registeredTriggers
            .getInterestedTriggers(new MockEvent(7l, 123));
        assertEquals(1, interestedTriggers.size());

        Collection<RegisterableTriggerInterface> interestedTriggers2 = registeredTriggers
            .getInterestedTriggers(new MockEvent(7l, 999));
        assertTrue(interestedTriggers2.isEmpty());

        assertTrue(registeredTriggers.isTriggerInterested(new MockEvent(7l, 123)));
        assertFalse(registeredTriggers.isTriggerInterested(new MockEvent(7l, 999)));

        EasyMock.verify(trigger1, trigger2);
        verify();
    }

    /**
     * Verifies that the trigger map is properly cleaned up when the last
     * trigger interested in a specific event is removed
     */
    public void testUnregisterTriggerNoneRemainingForEventKey() {
        Integer triggerId = 567;
        Map<TriggerEventKey, Map<Integer, RegisterableTriggerInterface>> testTriggers = new HashMap<TriggerEventKey, Map<Integer, RegisterableTriggerInterface>>();
        Map<Integer, RegisterableTriggerInterface> triggersById = new HashMap<Integer, RegisterableTriggerInterface>();
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(triggerId, trigger1);
        testTriggers.put(new TriggerEventKey(MockEvent.class, 123), triggersById);
        registeredTriggers.setTriggers(testTriggers);
        EasyMock.replay(trigger1);
        registeredTriggers.removeTrigger(triggerId);
        EasyMock.verify(trigger1);
        Map<TriggerEventKey, Map<Integer, RegisterableTriggerInterface>> trigMap = registeredTriggers.getTriggers();
        assertTrue(trigMap.isEmpty());
    }

    /**
     * Verifies that the trigger map is properly cleaned up when a trigger
     * interested in a specific event is removed, but others remain
     */
    public void testUnregisterTriggerSomeRemaining() {
        Integer triggerId = 567;
        Integer trigger2Id = 9908;

        Map<TriggerEventKey, Map<Integer, RegisterableTriggerInterface>> testTriggers = new HashMap<TriggerEventKey, Map<Integer, RegisterableTriggerInterface>>();

        Map<Integer, RegisterableTriggerInterface> triggersById = new HashMap<Integer, RegisterableTriggerInterface>();
        RegisterableTriggerInterface trigger1 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(triggerId, trigger1);

        RegisterableTriggerInterface trigger2 = EasyMock.createMock(RegisterableTriggerInterface.class);
        triggersById.put(trigger2Id, trigger2);

        testTriggers.put(new TriggerEventKey(MockEvent.class, 123), triggersById);

        registeredTriggers.setTriggers(testTriggers);
        EasyMock.replay(trigger1);
        registeredTriggers.removeTrigger(triggerId);
        EasyMock.verify(trigger1);
        Map<TriggerEventKey, Map<Integer, RegisterableTriggerInterface>> trigMap = registeredTriggers.getTriggers();
        Map<Integer, RegisterableTriggerInterface> actualTriggers = trigMap.get(new TriggerEventKey(MockEvent.class,
            123));
        assertNotNull(actualTriggers);
        assertEquals(1, actualTriggers.size());
        assertEquals(trigger2, actualTriggers.get(trigger2Id));
    }

    private void verify() {
        EasyMock.verify(alertRegulator);
    }

}
