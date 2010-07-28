package org.hyperic.hq.authz.server.session;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listens for resource creations and deletions to update any groups that have
 * associated membership critieria
 * @author jhickey
 * 
 */
@Component
public class ResourceGroupMemberUpdater implements ZeventListener<ResourceZevent> {
    private final ZeventEnqueuer zEventManager;
    private final ResourceGroupManager resourceGroupManager;

    @Autowired
    public ResourceGroupMemberUpdater(ZeventEnqueuer zEventManager,
                                      ResourceGroupManager resourceGroupManager) {
        this.zEventManager = zEventManager;
        this.resourceGroupManager = resourceGroupManager;
    }

    @PostConstruct
    public void subscribe() {
        Set<Class<?>> events = new HashSet<Class<?>>();
        events.add(ResourceCreatedZevent.class);
        events.add(ResourceDeletedZevent.class);
        zEventManager.addBufferedListener(events, this);
    }

    public void processEvents(List<ResourceZevent> events) {
        resourceGroupManager.updateGroupMembers(events);
    }

}
