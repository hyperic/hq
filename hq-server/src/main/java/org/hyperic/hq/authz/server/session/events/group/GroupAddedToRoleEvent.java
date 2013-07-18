package org.hyperic.hq.authz.server.session.events.group;

import org.hyperic.hq.authz.server.session.ResourceGroup;

public class GroupAddedToRoleEvent extends GroupRelatedEvent {

    public GroupAddedToRoleEvent(ResourceGroup group) {
        super(group);
    }

}
