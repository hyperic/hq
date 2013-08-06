package org.hyperic.hq.authz.server.session.events.subject;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;

public class SubjectRemovedFromRolesZevent extends RoleMembersChangedZevent {

    public SubjectRemovedFromRolesZevent(AuthzSubject subject, Collection<Role> roles) {
        super(subject, roles);
    }

}
