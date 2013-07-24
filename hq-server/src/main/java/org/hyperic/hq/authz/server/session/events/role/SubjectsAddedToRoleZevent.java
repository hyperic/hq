package org.hyperic.hq.authz.server.session.events.role;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;

public class SubjectsAddedToRoleZevent  extends RoleRelatedZevent {

    final Collection<AuthzSubject> subjects;

    public SubjectsAddedToRoleZevent(Role role, Collection<AuthzSubject> subjects) {
        super(role);
        this.subjects = subjects;        
    }

    public Collection<AuthzSubject> getSubjects() {
        return subjects;
    }

}
