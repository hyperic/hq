package org.hyperic.hq.authz.server.session.events.group;

import org.hyperic.hq.authz.server.session.ResourceGroup;

public class GroupRemovedFromRolesEvent extends GroupRelatedEvent {
    
    final Integer[] groupOldRoles;

    public GroupRemovedFromRolesEvent(ResourceGroup group, Integer[] ids) {
        super(group);
        this.groupOldRoles = ids;
    }

    public Integer[] getGroupOldRoles() {
        return groupOldRoles;
    }

}
