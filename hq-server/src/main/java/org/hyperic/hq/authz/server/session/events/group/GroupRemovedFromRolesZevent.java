package org.hyperic.hq.authz.server.session.events.group;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;

public class GroupRemovedFromRolesZevent extends GroupRelatedZevent {
    
    final Collection<Role> groupOldRoles;
    private final boolean isDuringCalculation;

    public GroupRemovedFromRolesZevent(ResourceGroup group, Collection<Role> roles, boolean isDuringCalculation) {
        super(group);
        this.groupOldRoles = roles;
        this.isDuringCalculation = isDuringCalculation;
    }

    public Collection<Role> getGroupOldRoles() {
        return groupOldRoles;
    }

    public boolean isDuringCalculation() {
        return isDuringCalculation;
    }
}
