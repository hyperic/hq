/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

/*
 * RegisteredTriggers.java
 *
 * Created on October 1, 2002, 1:50 PM
 */

package org.hyperic.hq.events.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.server.session.RegisteredTrigger;
import org.hyperic.hq.events.server.session.TriggerChangeCallback;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.collection.IntHashMap;

public class RegisteredTriggers {    
    private final static Log log =
        LogFactory.getLog(RegisteredTriggers.class.getName());
    
    public static final Integer KEY_ALL = new Integer(0);
    
    private static RegisteredTriggers singleton = new RegisteredTriggers();
    
    private final Object lock = new Object();
    
    /** Holds value of property initialized. */
    private boolean initialized = false;
    
    /** Holds value of property keyedByType. */
    private Hashtable keyedByType = new Hashtable();
    
    /** Holds value of property keyedByTrigger. */
    private Hashtable keyedByTrigger = new Hashtable();
    
    /** Creates a new instance of RegisteredTriggers */
    private RegisteredTriggers() {
    }
    
    /** Initializes the cache.
     * @return true if it actually initializes, false otherwise
     */
    private boolean init() {
        synchronized (lock) {
            return doInitUnsynchronized();
        }
    }
    
    private boolean doInitUnsynchronized() {
        if (isInitialized())
            return false;
        
        setInitialized(true);
        
        // Do the initialization here
        boolean successful = false;
        try {
            RegisteredTriggerManagerLocal rtm =
            RegisteredTriggerManagerUtil.getLocalHome().create();

            HashSet toRemove = new HashSet(this.keyedByTrigger.keySet());
            
            Collection triggers = rtm.getAllTriggers();
            for (Iterator i = triggers.iterator(); i.hasNext();) {
                RegisteredTriggerValue tv = (RegisteredTriggerValue) i.next();
                // Try to register each trigger, if exception, then move on
                try {
                    registerTrigger(tv);
                    toRemove.remove(tv.getId());
                } catch (ClassNotFoundException e) {
                    log.error("Error registering trigger", e);
                } catch (InstantiationException e) {
                    log.error("Error registering trigger", e);
                } catch (IllegalAccessException e) {
                    log.error("Error registering trigger", e);
                } catch (InvalidTriggerDataException e) {
                    log.error("Error registering trigger", e);
                }
            }
            
            // Remove old ones
            for (Iterator it = toRemove.iterator(); it.hasNext(); ) {
                this.unregisterTrigger((Integer) it.next());
            }
                   
            HQApp app = HQApp.getInstance();
            app.registerCallbackListener(TriggerChangeCallback.class,
                                         new RegisteredTriggersUpdater());
            successful = true;
        } catch (javax.naming.NamingException e) {
            // Change to warning so that it won't show up in startup log
            log.warn("Error getting RegisteredTriggerManager interface", e);
        } catch (javax.ejb.CreateException e) {
            log.error("Error creating RegisteredTriggerManager instance", e);
        } catch (SystemException e) {
            log.error("Error creating RegisteredTrigger instance", e);
        }
        finally {
            if (!successful)
                setInitialized(false);
        }
        
        return true;                   
    }
    
    public static void reinitialize() {
        singleton.setInitialized(false);
    }
    
    private Map getTriggerMap(Class eventType, Integer instance,
                              boolean create) {
        init(); // Initialize first
        
        Map triggers = new Hashtable();
        
        // Look up by Event type
        if (!keyedByType.containsKey(eventType)) {
            if (create)
                keyedByType.put(eventType, new IntHashMap());
            else
                return triggers;
        }
        
        // Lookup by Instance
        IntHashMap keyedByInstance = (IntHashMap) keyedByType.get(eventType);
        if (!keyedByInstance.containsKey(instance.intValue())) {
            if (create)
                keyedByInstance.put(instance.intValue(), triggers);
            else
                return triggers;
        }
        
        return (Map) keyedByInstance.get(instance.intValue());
    }
    
    public static Collection getInterestedTriggers(AbstractEvent event) {
        HashSet triggers = new HashSet();
        Map trigMap;
        
        // Can't very well look up a null object
        if (event.getInstanceId() != null) {
            // Get the triggers that are interested in this instance
            trigMap = singleton.getTriggerMap(event.getClass(),
                                              event.getInstanceId(),
                                              false);
            
            synchronized (trigMap) {
                triggers.addAll(trigMap.values());
            }
        }

        // Get the triggers that are interested in all instances
        trigMap = singleton.getTriggerMap(event.getClass(), KEY_ALL, false);

        synchronized (trigMap) {
            triggers.addAll(trigMap.values());
        }
        
        return triggers;
    }

    public static boolean isTriggerInterested(AbstractEvent event) {
        Map trigMap = null;
        
        // If the event happened more than a day ago, does anyone care?
        final long ONE_DAY = 86400000;
        long current = System.currentTimeMillis();
        if (event.getTimestamp() < current - ONE_DAY)
            return false;

        // Can't very well look up a null object
        if (event.getInstanceId() != null) {
            // Get the triggers that are interested in this instance
            trigMap = singleton.getTriggerMap(event.getClass(),
                                              event.getInstanceId(),
                                              false);
            if (trigMap != null && trigMap.size() > 0)
                return true;
        }
    
        // Check the triggers that are interested in all instances
        trigMap = singleton.getTriggerMap(event.getClass(), KEY_ALL, false);
        boolean ret = (trigMap != null && trigMap.size() > 0);
        return ret;
    }
    
    public static boolean isTriggerRegistered(Integer tid) {
        return singleton.getKeyedByTrigger().containsKey(tid);
    }
    
    private void registerTrigger(RegisteredTriggerValue tv)
        throws ClassNotFoundException, InstantiationException,
               IllegalAccessException, InvalidTriggerDataException {
        if (init()) // init would have done the work of registering for us
            return;
        
        // First create Trigger
        Class tc = Class.forName(tv.getClassname());
        RegisterableTriggerInterface trigger =
            (RegisterableTriggerInterface) tc.newInstance();
        
        trigger.init(tv);
        
        Class[] types = trigger.getInterestedEventTypes();
        for (int i = 0; i < types.length; i++) {
            Class type = types[i];
            
            // Now get the instances
            Integer[] instances = trigger.getInterestedInstanceIDs(type);
            
            if (instances == null)              // Not really interested in this
                continue;
            
            for (int j = 0; j < instances.length; j++) {
                Integer instance = instances[j];
                
                // Add trigger to registered map
                Map triggers = getTriggerMap(type, instance, true);
                
                synchronized (triggers) {
                    triggers.put(tv.getId(), trigger);
                }
                
                // Reverse register the trigger
                if (!keyedByTrigger.containsKey(tv.getId())) {
                    keyedByTrigger.put(tv.getId(), new ArrayList());
                }
                ArrayList triggerMaps =
                    (ArrayList) keyedByTrigger.get(tv.getId());
                triggerMaps.add(triggers);
            }
        }
    }
    
    private void unregisterTrigger(Integer tvId) {
        // Use the keyedByTrigger table to look up which maps to delete from
        ArrayList triggerMaps = (ArrayList) keyedByTrigger.remove(tvId);
        if (triggerMaps == null) // Can't unregister
            return;
        
        for (Iterator i = triggerMaps.iterator(); i.hasNext();) {
            Map triggers = (Map) i.next();
            triggers.remove(tvId);
        }
    }
    
    /** Getter for property initialized.
     * @return Value of property initialized.
     *
     */
    public boolean isInitialized() {
        synchronized (lock) {
            return this.initialized;            
        }
    }
    
    /** Setter for property initialized.
     * @param initialized New value of property initialized.
     *
     */
    public void setInitialized(boolean initialized) {
        synchronized (lock) {
            this.initialized = initialized;            
        }
    }
    
    /** Getter for property keyedByType.
     * @return Value of property keyedByType.
     *
     */
    public Hashtable getKeyedByType() {
        return this.keyedByType;
    }
    
    /** Getter for property keyedByTrigger.
     * @return Value of property keyedByTrigger.
     *
     */
    public Hashtable getKeyedByTrigger() {
        return this.keyedByTrigger;
    }
    
    public class RegisteredTriggersUpdater implements TriggerChangeCallback {

        public void beforeTriggersDeleted(Collection triggers) {
            for (Iterator it = triggers.iterator(); it.hasNext(); ) {
                RegisteredTrigger trigger = (RegisteredTrigger) it.next();
                unregisterTrigger(trigger.getId());
            }
        }

        public void afterTriggerCreated(RegisteredTrigger trigger) {
            try {
                registerTrigger(trigger.getRegisteredTriggerValue());
            } catch (ClassNotFoundException e) {
                log.error("Error registering trigger", e);
            } catch (InstantiationException e) {
                log.error("Error registering trigger", e);
            } catch (IllegalAccessException e) {
                log.error("Error registering trigger", e);
            } catch (InvalidTriggerDataException e) {
                log.error("Error registering trigger", e);
            }
        }

    }
}
