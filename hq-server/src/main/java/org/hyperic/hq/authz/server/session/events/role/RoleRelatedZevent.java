package org.hyperic.hq.authz.server.session.events.role;

import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class RoleRelatedZevent extends Zevent {
    
    private final Role role;
    
    public RoleRelatedZevent(Role role) {
        super(new ZeventSourceId() {}, new ZeventPayload() {});
        this.role = role;
    }
    
    public Role getRole() {
        return role;
    }
}
