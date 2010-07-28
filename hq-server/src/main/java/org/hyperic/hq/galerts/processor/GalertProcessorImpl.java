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

package org.hyperic.hq.galerts.processor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefDAO;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventSourceId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * This class acts as the central manager for all in-memory alerting.
 * It performs the following functions:
 * 
 * - Manages lists of active alert definitions
 * - Reloads defs as configurations change
 * - Listens to events from the zevents subsystem
 * - Listens to group membership events from the grouping subsystem 
 *  
 * In the future, it will also:
 * - Come online when a 'master' alerting node is specified in High Availability
 * 
 * Since the galerting subsystem is broken up into 2 pieces, the persisted
 * objects and the runtime objects, the 2 are split into separate packages.
 */
@Component
public class GalertProcessorImpl implements GalertProcessor {
    
    private final Object cfgLock     = new Object();
    private final Log _log = LogFactory.getLog(GalertProcessor.class);
    
    private final ZeventEnqueuer      _zMan;
    
    private GalertDefDAO _defDAO;
    
    // Integer ids -> MemGalertDefs
    private Map _alertDefs = new HashMap();
    
    // ZeventSourceId -> Set of {@link Gtrigger}s
    private Map _listeners = new HashMap();

    @Autowired
    public GalertProcessorImpl(ZeventEnqueuer zEventManager, GalertDefDAO galertDefDAO) {
        _zMan = zEventManager;
        _zMan.addBufferedGlobalListener(new EventListener(this));
        this._defDAO = galertDefDAO;
    }

    /**
     * Entry point to the processor from the {@link EventListener}
     *
     * @param events  A list of {@link Zevent}s to process
     * 
     * TODO:  This needs to be optimized so that the EventListener buffers
     *        up many events and calls this method.  The overhead of creating
     *        and checking session existance is too high on a per-event
     *        basis.
     */
    public void processEvents(final List<Zevent> events) {
        for (Iterator i=events.iterator(); i.hasNext(); ) {
           Zevent z = (Zevent)i.next();
           processEvent(z);
        }
    }
    
    private void processEvent(final Zevent event) {
        ZeventSourceId source = event.getSourceId();
        Set listenerDupe;

        synchronized (cfgLock) {
            Set listeners = (Set)_listeners.get(source);
            
            if (listeners == null)
                return;

            listenerDupe = new HashSet(listeners);
        }
        
        for (Iterator i=listenerDupe.iterator(); i.hasNext(); ) {
            final Gtrigger t = (Gtrigger)i.next();
            
            // Synchronize around all event processing for a trigger, since
            // they keep state and will need to be flushed
            synchronized(t) {
                try {
                   SessionManager.runInSession(new SessionRunner() {
                       public String getName() {
                           return "Event Processor";
                       }
                       public void run() throws Exception {
                           t.processEvent(event);
                       }
                   });
                } catch (Exception e) {
                    _log.warn("Error processing events", e);
                }
            }
        }
    }
    
    /**
     * Load a {@link GalertDef} into the processor, initializing the triggers,
     * strategy, etc.  If the definition is already loaded, it will be
     * re-initialized as though it were loaded for the first time.
     * 
     * If the load or reload fails, the processor will be in the same state
     * as it was prior to the call.
     * 
     * @param defId Id of a {@link GalertDef} to load.
     */
    private void handleUpdate(MemGalertDef def) {
        _log.debug("Handling load/update of alert def: " + def.getName());
        
        synchronized (cfgLock) {
            MemGalertDef oldDef = (MemGalertDef)_alertDefs.get(def.getId());

            // Out with the old
            if (oldDef != null) {
                removeListeners(oldDef);
            }
            
            // In with the new
            addListeners(def);
            _alertDefs.put(def.getId(), def);
            _log.debug("galert[id=" + def.getId() + ",name=" + def.getName() +
                       "] loaded");
        }
    }
    
    /**
     * Remove an alert definition from the processor. 
     */
    private void handleUnload(Integer defId) {
        _log.debug("Handling unload of alertdef[" + defId + "]");
        synchronized (cfgLock) {
            MemGalertDef oldDef = (MemGalertDef)_alertDefs.get(defId);
            
            if (oldDef == null) {
                _log.warn("Attempted to unload defid=" + defId +
                          " but it wasn't loaded");
                return;
            }
            removeListeners(oldDef);
            _alertDefs.remove(defId);
        }
    }
    
    private void addListeners(MemGalertDef def) {
        Map eventMap = def.getInterestedEvents();
        
        for (Iterator i=eventMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            Gtrigger t = (Gtrigger)ent.getKey();
            Set sourceIds = (Set)ent.getValue();
            
            for (Iterator j=sourceIds.iterator(); j.hasNext(); ) {
                ZeventSourceId sourceId = (ZeventSourceId)j.next();
                Set listeners = (Set)_listeners.get(sourceId);
                
                if (listeners == null) {
                    listeners = new HashSet();
                    _listeners.put(sourceId, listeners);
                }
                listeners.add(t);
            }
        }
    }
    
    private void removeListeners(MemGalertDef def) {
        Map eventMap = def.getInterestedEvents();
            
        // Nuke all the old listeners 
        for (Iterator i=eventMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            Gtrigger t = (Gtrigger)ent.getKey();
            Set sourceIds = (Set)ent.getValue();

            for (Iterator j=sourceIds.iterator(); j.hasNext(); ) {
                ZeventSourceId sourceId = (ZeventSourceId)j.next();
                Set listeners = (Set)_listeners.get(sourceId);

                listeners.remove(t);
            }
        }
    }
    
    /**
     * Called when primitive information has been updated which doens't
     * require a reload of the entire definition.
     */
    public void alertDefUpdated(GalertDef def, final String newName) {
        final Integer defId = def.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            
            public void suspend() {
            }
            
            public void resume() {
            }
            
            public void flush() {
            }
            
            public void beforeCompletion() {
            }
            
            public void beforeCommit(boolean readOnly) {
            }
            
            public void afterCompletion(int status) {
            }
            
            public void afterCommit() {
                synchronized (cfgLock) {
                    MemGalertDef memDef = (MemGalertDef)
                        _alertDefs.get(defId);
                    
                    if (memDef != null)
                        memDef.setName(newName);
                }
            }
        });
    }
    
    /**
     * Call this if an alert-def is created or updated.  The internal state
     * of the processor will be updated after the current transaction 
     * successfully commits.  
     * 
     * This method should be called for major changes to the definition, such
     * as conditions, enablement, etc., since it fully unloads the existing
     * definition and reloads it (meaning that internal state of the 
     * triggers, such as previously processed metrics, etc. will be reset)
     */
    public void loadReloadOrUnload(GalertDef def) {
        final boolean isUnload = !def.isEnabled(); 
        final Integer defId = def.getId();
        final MemGalertDef memDef;
        
        if (isUnload) {
            memDef = null;
        } else {
            memDef = new MemGalertDef(def);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            
            public void suspend() {
            }
            
            public void resume() {
            }
            
            public void flush() {
            }
            
            public void beforeCompletion() {
            }
            
            public void beforeCommit(boolean readOnly) {
            }
            
            public void afterCompletion(int status) {
            }
            
            public void afterCommit() {
                if (isUnload) {
                    handleUnload(defId);
                } else {
                    handleUpdate(memDef);
                }
            }
        });
    }
    
    /**
     * Call this if an alert-def is deleted.  After the transaction is
     * successfully committed, it will be removed from the processor.
     */
    public void alertDefDeleted(final Integer defId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            
            public void suspend() {
            }
            
            public void resume() {
            }
            
            public void flush() {
            }
            
            public void beforeCompletion() {
            }
            
            public void beforeCommit(boolean readOnly) {
            }
            
            public void afterCompletion(int status) {
            }
            
            public void afterCommit() {
                handleUnload(defId);
            }
        });
    }

    /**
     * Called to initialize the state of the processor.  This should be
     * called during application startup with all the alert definitions
     * in the system.
     */
    @Transactional
    public void startupInitialize() {
        Collection galertDefs = _defDAO.findAll();
        for (Iterator i=galertDefs.iterator(); i.hasNext(); ) {
            GalertDef def = (GalertDef)i.next();
            MemGalertDef memDef;

            if (!def.isEnabled())
                continue;
                
            try {
                memDef = new MemGalertDef(def);
                handleUpdate(memDef);
            } catch(Exception e) {
                _log.warn("Unable to load alert definition[id=" + 
                          def.getId() + ",name=" + def.getName(), e);
            }
        }
    }
    
    public boolean validateAlertDef(GalertDef def) {
        try {
            new MemGalertDef(def);
            return true;
        } catch(Exception e) {
            _log.warn("Unable to create alert def [" + def + "]", e);
            return false;
        }
    }
}
