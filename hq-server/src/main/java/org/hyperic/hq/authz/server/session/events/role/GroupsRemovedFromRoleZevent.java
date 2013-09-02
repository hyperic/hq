package org.hyperic.hq.authz.server.session.events.role;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;

public class GroupsRemovedFromRoleZevent extends RoleRelatedZevent {
    
    final Collection<ResourceGroup> groups;

    public GroupsRemovedFromRoleZevent(Role roleLocal, Collection<ResourceGroup> groups) {
        super(roleLocal);
        this.groups = groups;
    }
    
    public Collection<ResourceGroup> getGroups() {
        return groups;
    }

}
