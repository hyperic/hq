package org.hyperic.hq.bizapp.server.session;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
        for (Iterator<ResourcesCleanupZevent> i = events.iterator(); i.hasNext();) {
            try {
                appdefBoss.removeDeletedResources();
            } catch (Exception e) {
                log.error("removeDeletedResources() failed", e);
            }
            // Only need to run this once
            break;
        }
    }

    public String toString() {
        return "AppdefBoss.removeDeletedResources";
    }
}
