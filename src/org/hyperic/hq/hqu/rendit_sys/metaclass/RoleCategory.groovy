package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Operation
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.authz.server.session.RoleManagerEJBImpl as RoleMan

class RoleCategory {
    private static roleMan = RoleMan.one

    static void setSubjects(Role role, AuthzSubject user, Collection subjects) {
        roleMan.removeSubjects(user, role.valueObject,
                            (role.subjects.collect {it.id}) as Integer[])
        roleMan.addSubjects(user, role.valueObject, 
                            (subjects.collect {it.id}) as Integer[])
    }

    static void setGroups(Role role, AuthzSubject user, Collection groups) {
        roleMan.removeResourceGroups(user, role.valueObject,
                                     (role.resourceGroups.collect {it.id}) as Integer[])
        roleMan.addResourceGroups(user, role.valueObject,
                                  (groups.collect {it.id}) as Integer[])
    }
    
    static void setOperations(Role role, AuthzSubject user, Collection ops) {
        roleMan.removeAllOperations(user, role.valueObject)
        roleMan.addOperations(user, role.valueObject, ops as Operation[])
    }

    /**
     * Remove a Role.
     */
    static void remove(Role role, AuthzSubject user) {
        roleMan.removeRole(user, role.id)
    }
}
