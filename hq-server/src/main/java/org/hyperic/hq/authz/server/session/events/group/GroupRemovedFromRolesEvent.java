package org.hyperic.hq.authz.server.session.events.group;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;

public class GroupRemovedFromRolesEvent extends GroupRelatedEvent {
    
    final Collection<Role> groupOldRoles;

    public GroupRemovedFromRolesEvent(ResourceGroup group, Collection<Role> roles) {
        super(group);
        this.groupOldRoles = roles;
    }

    public Collection<Role> getGroupOldRoles() {
        return groupOldRoles;
    }

}
