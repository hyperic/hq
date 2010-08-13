package org.hyperic.hq.authz.server.session;

import java.util.List;

import javax.annotation.PostConstruct;

import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listens for resource creations to update any groups that have
 * associated membership critieria
 * @author jhickey
 * 
 */
@Component
public class ResourceGroupMemberUpdater implements ZeventListener<ResourceCreatedZevent> {
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
        zEventManager.addBufferedListener(ResourceCreatedZevent.class, this);
    }

    public void processEvents(List<ResourceCreatedZevent> events) {
        resourceGroupManager.updateGroupMembers(events);
    }
    
    public String toString() {
        return "GroupMemberUpdater";
    }

}
