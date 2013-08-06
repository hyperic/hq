package org.hyperic.hq.authz.server.session.events.group;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;

public class GroupRemovedFromRolesZevent extends GroupRelatedZevent {
    
    final Collection<Role> groupOldRoles;

    public GroupRemovedFromRolesZevent(ResourceGroup group, Collection<Role> roles) {
        super(group);
        this.groupOldRoles = roles;
    }

    public Collection<Role> getGroupOldRoles() {
        return groupOldRoles;
    }

}
