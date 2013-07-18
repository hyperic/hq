package org.hyperic.hq.authz.server.session.events.group;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;

public class GroupRemovedFromRoleEvent extends GroupRelatedEvent {
    
    final Role role;

    public GroupRemovedFromRoleEvent(ResourceGroup group, Role role) {
        super(group);
        this.role = role;
    }
    
    public Role getRole() {
        return this.role;
    }

}
