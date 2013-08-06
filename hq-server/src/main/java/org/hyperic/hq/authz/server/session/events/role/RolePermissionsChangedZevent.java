package org.hyperic.hq.authz.server.session.events.role;

import java.util.Collection;

import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Role;

public class RolePermissionsChangedZevent extends RoleRelatedZevent {
    
    private final Collection<Operation> orgOperations;

    public RolePermissionsChangedZevent(Role role, Collection<Operation> orgOperations) {
        super(role);
        this.orgOperations = orgOperations;
    }

    public Collection<Operation> getOrgOperations() {
        return orgOperations;
    }    
}
