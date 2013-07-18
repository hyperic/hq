package org.hyperic.hq.authz.server.session.events.group;

import org.hyperic.hq.authz.server.session.ResourceGroup;

public class GroupMembersRemovedEvent extends GroupRelatedEvent {

    public GroupMembersRemovedEvent(ResourceGroup group) {
        super(group);
    }

}
