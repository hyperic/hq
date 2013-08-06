package org.hyperic.hq.authz.server.session.events.role;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;
import java.util.Collection;

/**
 * User: andersonm
 * Date: 7/29/13
 */
public class RoleDeletedZevent extends RoleRelatedZevent {

    private final Collection<ResourceGroup> groups;
    private final Collection<AuthzSubject> subjectsInRole;

    public RoleDeletedZevent(Role role, Collection<ResourceGroup> groups, Collection<AuthzSubject> subjectsInRole) {
        super(role);
        this.groups = groups;
        this.subjectsInRole = subjectsInRole;
    }

    public Collection<ResourceGroup> getGroups() {
        return groups;
    }

    public Collection<AuthzSubject> getSubjectsInRole() {
        return subjectsInRole;
    }
}
