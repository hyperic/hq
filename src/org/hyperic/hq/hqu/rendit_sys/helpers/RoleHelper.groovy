package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.server.session.RoleManagerEJBImpl as RoleMan
import org.hyperic.hq.authz.server.session.AuthzSubject

class RoleHelper extends BaseHelper {

    def roleMan = RoleMan.one

    RoleHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * Find all Roles
     */
    public Collection getAllRoles() {
        return roleMan.allRoles
    }
}