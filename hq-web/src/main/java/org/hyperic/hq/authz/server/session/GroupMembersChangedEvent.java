package org.hyperic.hq.authz.server.session;

import org.springframework.context.ApplicationEvent;

/**
 * Indicates that members have been added to or removed from the group
 * @author jhickey
 * 
 */
public class GroupMembersChangedEvent
    extends ApplicationEvent {

    public GroupMembersChangedEvent(ResourceGroup group) {
        super(group);
    }

    public ResourceGroup getGroup() {
        return (ResourceGroup) getSource();
    }

}
