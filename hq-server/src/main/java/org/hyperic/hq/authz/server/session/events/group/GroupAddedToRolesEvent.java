package org.hyperic.hq.authz.server.session.events.group;

import org.hyperic.hq.authz.server.session.ResourceGroup;

public class GroupAddedToRolesEvent extends GroupRelatedEvent {

    public GroupAddedToRolesEvent(ResourceGroup group) {
        super(group);
    }

}
