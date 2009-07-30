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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.server.session.AlertConditionEvaluatorFactory;
import org.hyperic.hq.events.server.session.AlertConditionEvaluatorFactoryImpl;
import org.hyperic.hq.events.server.session.RegisteredTrigger;
import org.hyperic.hq.events.server.session.TriggerChangeCallback;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.zevents.ZeventManager;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class RegisteredTriggers {
    private final static Log log = LogFactory.getLog(RegisteredTriggers.class.getName());

    public static final Integer KEY_ALL = new Integer(0);

    private static final Object INIT_LOCK = new Object();

    private Object triggerUpdateLock = new Object();

    private static RegisteredTriggers INSTANCE;

    private Map triggers = new ConcurrentHashMap();

    private AlertConditionEvaluatorFactory factory;

    private RegisteredTriggerManagerLocal registeredTriggerManager;

    private AlertDefinitionManagerLocal alertDefinitionManager;

    /** Creates a new instance of RegisteredTriggers */
    RegisteredTriggers(RegisteredTriggerManagerLocal registeredTriggerManager,
                       AlertDefinitionManagerLocal alertDefinitionManager,
                       AlertConditionEvaluatorFactory alertConditionEvaluatorFactory)
    {
        this.registeredTriggerManager = registeredTriggerManager;
        this.alertDefinitionManager = alertDefinitionManager;
        this.factory = alertConditionEvaluatorFactory;
    }

    public static RegisteredTriggers getInstance() throws CreateException, NamingException {
        synchronized (INIT_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new RegisteredTriggers(RegisteredTriggerManagerUtil.getLocalHome().create(),
                                                  AlertDefinitionManagerUtil.getLocalHome().create(),
                                                  new AlertConditionEvaluatorFactoryImpl(ZeventManager.getInstance()));
            }
        }
        return INSTANCE;
    }

    public void init() {
        // create overall Hibernate session in which to call registerTrigger
        boolean sessionCreated = SessionManager.setupSession("RegisteredTriggersInit");
        try {
            initializeTriggers();
            HQApp app = HQApp.getInstance();
            app.registerCallbackListener(TriggerChangeCallback.class, new RegisteredTriggersUpdater());
        } finally {
            if (sessionCreated) {
                SessionManager.cleanupSession(false);
            }
        }
    }

    void initializeTriggers() {
        Collection registeredTriggers = registeredTriggerManager.getTriggers();
        registerTriggers(registeredTriggers);
    }
    
    private void registerTriggers(Collection registeredTriggers) {
        Map alertEvaluators = new HashMap();
        for (Iterator i = registeredTriggers.iterator(); i.hasNext();) {
            RegisteredTrigger tv = (RegisteredTrigger) i.next();
            Integer alertDefId = alertDefinitionManager.getIdFromTrigger(tv.getId());
            if(alertEvaluators.get(alertDefId) == null) {
                alertEvaluators.put(alertDefId, factory.create(alertDefinitionManager.getByIdNoCheck(alertDefId)));
            }
        }
        for (Iterator i = registeredTriggers.iterator(); i.hasNext();) {
            // Try to register each trigger, if exception, then move on
            RegisteredTrigger tv = (RegisteredTrigger) i.next();
            try {
                registerTrigger(tv.getRegisteredTriggerValue(),
                                (AlertConditionEvaluator)alertEvaluators.get(alertDefinitionManager.getIdFromTrigger(tv.getId())));
            } catch (Exception e) {
                log.error("Error registering trigger", e);
            }
        }
    }

    public Collection getInterestedTriggers(Class eventClass, Integer instanceId) {
        HashSet trigs = new HashSet();
        TriggerEventKey key = new TriggerEventKey(eventClass, instanceId.intValue());
        Map triggersById = (Map) triggers.get(key);
        if (triggersById != null) {
            trigs.addAll(triggersById.values());
        }
        return trigs;
    }

    public static Collection getInterestedTriggers(AbstractEvent event) {
        HashSet trigs = new HashSet();

        // Can't very well look up a null object
        if (event.getInstanceId() != null) {
            // Get the triggers that are interested in this instance
            trigs.addAll(INSTANCE.getInterestedTriggers(event.getClass(), event.getInstanceId()));
        }
        // Get the triggers that are interested in all instances
        trigs.addAll(INSTANCE.getInterestedTriggers(event.getClass(), KEY_ALL));
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
            Collection trigs = INSTANCE.getInterestedTriggers(event.getClass(), event.getInstanceId());
            if (trigs.size() > 0)
                return true;
        }

        // Check the triggers that are interested in all instances
        Collection trigs = INSTANCE.getInterestedTriggers(event.getClass(), KEY_ALL);
        return (trigs.size() > 0);
    }

    /**
     * Register a trigger. NOTE: This method should be called within an active
     * Hibernate session/transaction. Trigger init method may interact with a
     * lazily initialized AlertDefinition.
     * @param tv
     * @param alertDef
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvalidTriggerDataException
     */
    private void registerTrigger(RegisteredTriggerValue tv, AlertConditionEvaluator alertConditionEvaluator) throws ClassNotFoundException,
                                                                                     InstantiationException,
                                                                                     IllegalAccessException,
                                                                                     InvalidTriggerDataException
    {
        // First create Trigger
        Class tc = Class.forName(tv.getClassname());
        RegisterableTriggerInterface trigger = (RegisterableTriggerInterface) tc.newInstance();

        trigger.init(tv, alertConditionEvaluator);

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
                    triggersById.put(tv.getId(), trigger);
                    triggers.put(key, triggersById);
                }
            }
        }
    }

    void setTriggers(Map triggers) {
        this.triggers = triggers;
    }

    void unregisterTrigger(Integer tvId) {
        synchronized (triggerUpdateLock) {
            for (Iterator triggerMaps = triggers.values().iterator(); triggerMaps.hasNext();) {
                Map triggerIdsToTriggers = (Map) triggerMaps.next();
                triggerIdsToTriggers.remove(tvId);
                if (triggerIdsToTriggers.isEmpty()) {
                    triggerMaps.remove();
                }
            }
        }
    }

    Map getTriggers() {
        return this.triggers;
    }

    public class RegisteredTriggersUpdater implements TriggerChangeCallback {

        public void beforeTriggersDeleted(final Collection triggers) {

            for (Iterator it = triggers.iterator(); it.hasNext();) {
                RegisteredTrigger trigger = (RegisteredTrigger) it.next();
                unregisterTrigger(trigger.getId());
            }

        }

        public void afterTriggersCreated(final Collection triggers) {
            try {
                HQApp.getInstance().addTransactionListener(new TransactionListener() {
                    public void afterCommit(boolean success) {
                        if (success) {
                            try {
                                registerTriggers(triggers);
                            } catch (Exception e) {
                                log.error("Error registering trigger", e);
                            }
                        }
                    }

                    public void beforeCommit() {
                    }
                });
            } catch (Throwable t) {
                log.error("Error registering trigger", t);
            }

        }
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
