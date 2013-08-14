package org.hyperic.hq.authz.server.session.events.subject;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

import java.util.Collection;

/**
 * User: andersonm
 * Date: 7/30/13
 */
public class SubjectDeletedZevent extends Zevent {

    private final Collection<Role> roles;
    private final Collection<ResourceGroup> groups;
    private final AuthzSubject subject;

    public SubjectDeletedZevent(AuthzSubject subject, Collection<Role> roles, Collection<ResourceGroup> groups) {
        super(new ZeventSourceId() {} , new ZeventPayload() {});
        this.subject = subject;
        this.roles = roles;
        this.groups = groups;
    }

    public Collection<ResourceGroup> getGroups() {
        return groups;
    }

    public Collection<Role> getRoles() {
        return roles;
    }

    public AuthzSubject getSubject() {
        return subject;
    }

}
