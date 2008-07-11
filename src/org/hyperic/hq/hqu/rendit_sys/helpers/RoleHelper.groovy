package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.server.session.RoleManagerEJBImpl as RoleMan
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Role

class RoleHelper extends BaseHelper {

    def roleMan = RoleMan.one

    RoleHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * Find all Roles
     *
     * @return A Collection of Roles in the system.
     */
    public Collection getAllRoles() {
        roleMan.allRoles
    }

    /**
     * Find a Role by name.
     * @param name The role name to search for.
     * @return The Role with the given name, or null if that role does not
     * exist.
     */
    public Role findRoleByName(String name) {
        roleMan.findRoleByName(name)
    }

    /**
     * Find a Role by id.
     * @param id The role id to search for.
     * @return The Role with the given id, or null if that role does not exist.
     */
    public Role findRoleById(int id) {
        roleMan.findRoleById(id)
    }
}
