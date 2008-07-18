package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Operation
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.authz.server.session.RoleManagerEJBImpl as RoleMan

class RoleCategory {
    private static roleMan = RoleMan.one

    static void setSubjects(Role role, AuthzSubject user, Collection subjects) {
        roleMan.removeSubjects(user, role.roleValue,
                               (role.subjects.collect {it.id}) as Integer[])
        roleMan.addSubjects(user, role.roleValue, 
                            (subjects.collect {it.id}) as Integer[])
    }

    static void setGroups(Role role, AuthzSubject user, Collection groups) {
        roleMan.removeResourceGroups(user, role.roleValue,
                                     (role.resourceGroups.collect {it.id}) as Integer[])
        roleMan.addResourceGroups(user, role.roleValue,
                                  (groups.collect {it.id}) as Integer[])
    }

    /**
     * Set the operations for a Role.
     */
    static void setOperations(Role role, AuthzSubject user, Collection ops) {
        roleMan.removeAllOperations(user, role.roleValue)
        roleMan.addOperations(user, role.roleValue, ops as Operation[])
    }

    /**
     * Remove a Role.
     */
    static void remove(Role role, AuthzSubject user) {
        roleMan.removeRole(user, role.id)
    }
}
