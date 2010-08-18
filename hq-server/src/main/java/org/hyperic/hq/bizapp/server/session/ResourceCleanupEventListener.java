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

package org.hyperic.hq.bizapp.server.session;

import java.util.HashMap;
import java.util.HashSet;
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
