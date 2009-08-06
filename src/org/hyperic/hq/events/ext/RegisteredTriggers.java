/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004, 2005, 2006],
 * Hyperic, Inc. This file is part of HQ. HQ is free software; you can
 * redistribute it and/or modify it under the terms version 2 of the GNU General
 * Public License as published by the Free Software Foundation. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

/*
 * RegisteredTriggers.java Created on October 1, 2002, 1:50 PM
 */

package org.hyperic.hq.events.ext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.server.session.AlertRegulator;
import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

/**
 * Repository of in memory triggers for event processing
 * @author jhickey
 *
 */
public class RegisteredTriggers implements RegisterableTriggerRepository {

    public static final Integer KEY_ALL = new Integer(0);

    private static final Object INIT_LOCK = new Object();

    private static RegisteredTriggers INSTANCE;

    private Object triggerUpdateLock = new Object();

    private Map triggers = new ConcurrentHashMap();

    private RegisteredTriggerManagerLocal registeredTriggerManager;

    /** Creates a new instance of RegisteredTriggers */
    RegisteredTriggers(RegisteredTriggerManagerLocal registeredTriggerManager) {
        this.registeredTriggerManager = registeredTriggerManager;

    }

    Map getTriggers() {
        return this.triggers;
    }

    void init() {
        registeredTriggerManager.initializeTriggers(this);
    }

    public Collection getInterestedTriggers(Class eventClass, Integer instanceId) {
        HashSet trigs = new HashSet();
        //All alerts are disabled, so no triggers should be processing events
        if (!AlertRegulator.getInstance().alertsAllowed()) {
            return trigs;
        }
        TriggerEventKey key = new TriggerEventKey(eventClass, instanceId.intValue());
        Map triggersById = (Map) triggers.get(key);
        if (triggersById != null) {
            trigs.addAll(triggersById.values());
        }
        //Remove disabled triggers from new set so don't have to synchronize retrieval around concurrent triggers map
        for(Iterator iterator = trigs.iterator();iterator.hasNext();) {
            RegisterableTriggerInterface trigger = (RegisterableTriggerInterface)iterator.next();
            if(! trigger.isEnabled()) {
                iterator.remove();
            }
        }
        return trigs;
    }

    public void addTrigger(RegisterableTriggerInterface trigger) {
        Class[] types = trigger.getInterestedEventTypes();
        for (int i = 0; i < types.length; i++) {
            Class type = types[i];

            // Now get the instances
            Integer[] instances = trigger.getInterestedInstanceIDs(type);

            if (instances == null) // Not really interested in this
                continue;

            for (int j = 0; j < instances.length; j++) {
                Integer instance = instances[j];
                TriggerEventKey key = new TriggerEventKey(type, instance.intValue());
                // Despite using ConcurrentHashMaps - need to synchronize
                // updates due to iteration required by unregisterTrigger
                synchronized (triggerUpdateLock) {
                    Map triggersById = (Map) triggers.get(key);
                    if (triggersById == null) {
                        triggersById = new ConcurrentHashMap();
                    }
                    triggersById.put(trigger.getId(), trigger);
                    triggers.put(key, triggersById);
                }
            }
        }
    }

    void setTriggers(Map triggers) {
        this.triggers = triggers;
    }

    public void removeTrigger(Integer triggerId) {
        synchronized (triggerUpdateLock) {
            for (Iterator triggerMaps = triggers.values().iterator(); triggerMaps.hasNext();) {
                Map triggerIdsToTriggers = (Map) triggerMaps.next();
                triggerIdsToTriggers.remove(triggerId);
                if (triggerIdsToTriggers.isEmpty()) {
                    triggerMaps.remove();
                }
            }
        }
    }


    public void setTriggersEnabled(Collection triggerIds, boolean enabled) {
        Set triggersToProcess = new HashSet();
        synchronized (triggerUpdateLock) {
            for (Iterator triggerMaps = triggers.values().iterator(); triggerMaps.hasNext();) {
                Map triggerIdsToTriggers = (Map) triggerMaps.next();
                for(Iterator idIterator = triggerIds.iterator(); idIterator.hasNext();) {
                    Integer triggerId = (Integer)idIterator.next();
                    RegisterableTriggerInterface trigger = (RegisterableTriggerInterface)triggerIdsToTriggers.get(triggerId);
                    if(trigger != null) {
                        triggersToProcess.add(trigger);
                    }
                }
            }
        }
        //not concerned with thread-safety of "enabled" boolean - willing to deal with some being read with old value during update
        for(Iterator iterator = triggersToProcess.iterator();iterator.hasNext();) {
            RegisterableTriggerInterface trigger = (RegisterableTriggerInterface)iterator.next();
            trigger.setEnabled(enabled);
        }
    }

    private static RegisteredTriggers getInstance() {
        synchronized (INIT_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new RegisteredTriggers(RegisteredTriggerManagerEJBImpl.getOne());
                INSTANCE.init();
            }
        }
        return INSTANCE;
    }

    public static Collection getInterestedTriggers(AbstractEvent event) {
        HashSet trigs = new HashSet();

        // Can't very well look up a null object
        if (event.getInstanceId() != null) {
            // Get the triggers that are interested in this instance
            trigs.addAll(RegisteredTriggers.getInstance()
                                           .getInterestedTriggers(event.getClass(), event.getInstanceId()));
        }
        // Get the triggers that are interested in all instances
        trigs.addAll(RegisteredTriggers.getInstance().getInterestedTriggers(event.getClass(), KEY_ALL));
        return trigs;
    }

    public static boolean isTriggerInterested(AbstractEvent event) {
        // If the event happened more than a day ago, does anyone care?
        final long ONE_DAY = 86400000;
        long current = System.currentTimeMillis();
        if (event.getTimestamp() < current - ONE_DAY)
            return false;

        // Can't very well look up a null object
        if (event.getInstanceId() != null) {
            // Get the triggers that are interested in this instance
            Collection trigs = RegisteredTriggers.getInstance().getInterestedTriggers(event.getClass(),
                                                                                      event.getInstanceId());
            if (trigs.size() > 0)
                return true;
        }

        // Check the triggers that are interested in all instances
        Collection trigs = RegisteredTriggers.getInstance().getInterestedTriggers(event.getClass(), KEY_ALL);
        return (trigs.size() > 0);
    }

    /**
     * Set the static instance. This is expected to be used for testing only and
     * is therefore not synchronized
     * @param instance The static instance to use
     */
    static void setInstance(RegisteredTriggers instance) {
        INSTANCE = instance;
    }
}
