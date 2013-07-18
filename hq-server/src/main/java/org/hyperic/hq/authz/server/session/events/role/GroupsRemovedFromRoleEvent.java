package org.hyperic.hq.authz.server.session.events.role;

import org.hyperic.hq.authz.server.session.Role;
import org.springframework.context.ApplicationEvent;

public class GroupsRemovedFromRoleEvent extends ApplicationEvent {
    
    final Integer[] groups;

    public GroupsRemovedFromRoleEvent(Role roleLocal, Integer[] gids) {
        super(roleLocal);
        this.groups = gids;
    }
    
    public Role getRole() {
        return (Role) getSource();
    }
  
    public Integer[] getGroups() {
        return groups;
    }

}
