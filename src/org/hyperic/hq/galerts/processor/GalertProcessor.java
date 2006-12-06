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
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.galerts.shared.GalertManagerUtil;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventSourceId;


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
public class GalertProcessor {
    private static final GalertProcessor INSTANCE = new GalertProcessor();
    private static final Object CFG_LOCK = new Object();
    private static final Log _log = LogFactory.getLog(GalertProcessor.class);

    private static boolean _initialized = false;
    
    private final GalertManagerLocal _aMan; 
    private final ZeventManager      _zMan;
    
    // Integer ids -> MemGalertDefs
    private Map _alertDefs = new HashMap();
    
    // ZeventSourceId -> Set of {@link Gtrigger}s
    private Map _listeners = new HashMap();

    
    private GalertProcessor() {
        try {
            _aMan = GalertManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
        _zMan = ZeventManager.getInstance();
        _zMan.addGlobalListener(new EventListener(this));
    }

    /**
     * Entry point to the processor from the {@link EventListener}
     *
     * @param events  A list of {@link Zevent}s to process
     */
    void processEvents(List events) {
        for (Iterator i=events.iterator(); i.hasNext(); ) {
            Zevent z = (Zevent)i.next();
            
            processEvent(z);
        }
    }
    
    private void processEvent(Zevent event) {
        ZeventSourceId source = event.getSourceId();
        Set listenerDupe;

        _log.info("Processing event: " + event);
        synchronized (CFG_LOCK) {
            Set listeners = (Set)_listeners.get(source);
            
            if (listeners == null)
                return;

            listenerDupe = new HashSet(listeners);
        }
        
        for (Iterator i=listenerDupe.iterator(); i.hasNext(); ) {
            Gtrigger t = (Gtrigger)i.next();
            
            t.processEvent(event);
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
        _log.info("Handling load/update of alert def: " + def.getName());
        
        synchronized (CFG_LOCK) {
            MemGalertDef oldDef = (MemGalertDef)_alertDefs.get(def.getId());

            // Out with the old
            if (oldDef != null) {
                removeListeners(oldDef);
            }
            
            // In with the new
            addListeners(def);
            _alertDefs.put(def.getId(), def);
            _log.info("galert[id=" + def.getId() + ",name=" + def.getName() +
                      "] loaded");
        }
    }
    
    /**
     * Remove an alert definition from the processor. 
     */
    private void handleUnload(Integer defId) {
        _log.info("Handling unload of alertdef[" + defId + "]");
        synchronized (CFG_LOCK) {
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
     * Call this if an alert-def is created or updated.  The internal state
     * of the processor will be updated after the current transaction 
     * successfully commits.
     */
    public void alertDefUpdated(GalertDef def) {
        final boolean isUnload = !def.isEnabled(); 
        final Integer defId = def.getId();
        final MemGalertDef memDef;
        
        if (isUnload) {
            memDef = null;
        } else {
            memDef = new MemGalertDef(def);
        }
        
        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                if (success) {
                    if (isUnload) {
                        handleUnload(defId);
                    } else {
                        handleUpdate(memDef);
                    }
                }
            }
        });
    }
    
    /**
     * Call this if an alert-def is deleted.  After the transaction is
     * successfully committed, it will be removed from the processor.
     */
    public void alertDefDeleted(final Integer defId) {
        HQApp.getInstance().addTransactionListener(new TransactionListener() {
            public void afterCommit(boolean success) {
                if (success) {
                    handleUnload(defId);
                }
            }
        });
    }

    /**
     * Called to initialize the state of the processor.  This should be
     * called during application startup with all the alert definitions
     * in the system.
     */
    public void startupInitialize(Collection galertDefs) {
        synchronized (CFG_LOCK) {
            if (_initialized)
                return;
            _initialized = true;
        }
        
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
    
    public static GalertProcessor getInstance() {
        return INSTANCE;
    }
}
