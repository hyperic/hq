package org.hyperic.hq.bizapp.server.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ResourcesCleanupZevent;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResourceCleanupEventListener implements ZeventListener<ResourcesCleanupZevent>, ResourceCleanupEventListenerRegistrar {

    private AppdefBoss appdefBoss;

    private final Log log = LogFactory.getLog(ResourceCleanupEventListener.class);
    
    private ZeventEnqueuer zEventManager;

    @Autowired
    public ResourceCleanupEventListener(AppdefBoss appdefBoss, ZeventEnqueuer zEventManager) {
        this.appdefBoss = appdefBoss;
        this.zEventManager = zEventManager;
    }
    
    @Transactional
    public void registerResourceCleanupListener() {
        // Add listener to remove alert definition and alerts after resources
        // are deleted.
        HashSet<Class<ResourcesCleanupZevent>> events = new HashSet<Class<ResourcesCleanupZevent>>();
        events.add(ResourcesCleanupZevent.class);
        zEventManager.addBufferedListener(events, this);
        zEventManager.enqueueEventAfterCommit(new ResourcesCleanupZevent());
    }

    public void processEvents(List<ResourcesCleanupZevent> events) {
        if (events != null && !events.isEmpty()) {
            try {
                Map<Integer,List<AppdefEntityID>> agentCache = buildAsyncDeleteAgentCache(events);
                appdefBoss.removeDeletedResources(agentCache);
            } catch (Exception e) {
                log.error("removeDeletedResources() failed", e);
            }                        
        }
    }
    
    /**
     * @param zevents {@link List} of {@link ResourcesCleanupZevent}
     * 
     * @return {@link Map} of {@link Integer} of agentIds 
     * to {@link List} of {@link AppdefEntityID}s
     */
    @SuppressWarnings("unchecked")
    private Map<Integer,List<AppdefEntityID>> buildAsyncDeleteAgentCache(List<ResourcesCleanupZevent> zevents) {
        Map<Integer,List<AppdefEntityID>> masterCache = new HashMap<Integer,List<AppdefEntityID>>();
        
        for (ResourcesCleanupZevent z : zevents) {
            if (z.getAgents() != null) {
                Map<Integer,List<AppdefEntityID>> cache = z.getAgents();
                
                for (Integer agentId : cache.keySet() ) {
                    
                    List<AppdefEntityID> newResources = cache.get(agentId);
                    List<AppdefEntityID> resources = masterCache.get(agentId);
                    if (resources == null) {
                        resources = newResources;
                    } else {
                        resources.addAll(newResources);
                    }
                    masterCache.put(agentId, resources);
                }
            }
        }
        
        return masterCache;
    }

    public String toString() {
        return "AppdefBoss.removeDeletedResources";
    }
}
