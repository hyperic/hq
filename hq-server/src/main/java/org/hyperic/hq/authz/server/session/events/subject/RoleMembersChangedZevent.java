package org.hyperic.hq.authz.server.session.events.subject;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class RoleMembersChangedZevent extends Zevent {

    private final AuthzSubject subject;
    private final Collection<Role> roles;

    public RoleMembersChangedZevent(AuthzSubject subject, Collection<Role> roles) {
        super(new ZeventSourceId() {}, new ZeventPayload() {});
        this.subject = subject;
        this.roles = roles;
    }

    public AuthzSubject getSubject() {
        return subject;
    }

    public Collection<Role> getRoles() {
        return roles;
    }

}
