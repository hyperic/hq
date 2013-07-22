package org.hyperic.hq.authz.server.session.events.role;

import org.hyperic.hq.authz.server.session.Role;

public class GroupsAddedToRoleZevent extends RoleRelatedZevent {
    
    final Integer[] groups;

    public GroupsAddedToRoleZevent(Role role, Integer[] gids) {
        super(role);
        this.groups = gids;
    }

    public Integer[] getGroups() {
        return groups;
    }

}
