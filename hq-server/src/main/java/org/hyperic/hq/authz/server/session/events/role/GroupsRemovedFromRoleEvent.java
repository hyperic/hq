package org.hyperic.hq.authz.server.session.events.role;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;
import org.springframework.context.ApplicationEvent;

public class GroupsRemovedFromRoleEvent extends ApplicationEvent {
    
    final Collection<ResourceGroup> groups;

    public GroupsRemovedFromRoleEvent(Role roleLocal, Collection<ResourceGroup> groups) {
        super(roleLocal);
        this.groups = groups;
    }
    
    public Role getRole() {
        return (Role) getSource();
    }
  
    public Collection<ResourceGroup> getGroups() {
        return groups;
    }

}
