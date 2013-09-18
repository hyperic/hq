package org.hyperic.hq.authz.server.session.events.role;

import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class RoleRelatedZevent extends Zevent {
    
    private final Integer roleId;
    private final String roleName;
    
    public RoleRelatedZevent(Role role) {
        super(new ZeventSourceId() {}, new ZeventPayload() {});
        this.roleId = role.getId();
        this.roleName = role.getName();
    }
    
    public Integer getRoleId() {
        return roleId;
    }
    
    public String getRoleName() {
        return roleName;
    }
}
