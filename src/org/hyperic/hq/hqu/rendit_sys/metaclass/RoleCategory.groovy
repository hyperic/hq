package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.authz.server.session.RoleManagerEJBImpl as RoleMan
import org.hyperic.hq.authz.shared.OperationValue
import org.hyperic.hq.authz.shared.RoleValue

class RoleCategory {
    private static roleMan = RoleMan.one

    static void setSubjects(Role role, AuthzSubject user, Collection subjects) {
        roleMan.removeSubjects(user.authzSubjectValue, role.getRoleValue(),
                               (role.subjects.collect {it.id}) as Integer[])
        roleMan.addSubjects(user.authzSubjectValue, role.roleValue, 
                            (subjects.collect {it.id}) as Integer[])
    }

    static void setGroups(Role role, AuthzSubject user, Collection groups) {
        roleMan.removeAllResourceGroups(user.authzSubjectValue, role)
        roleMan.addResourceGroups(user.authzSubjectValue,  role.roleValue,
                                  (groups.collect {it.id}) as Integer[])
    }
   
    /**
     * Update a Role
     */
    static void update(Role role, AuthzSubject user,
                       String name, String description) {

        RoleValue rv = role.getRoleValue()
        if (name) {
            rv.setName(name)
        }
        if (description) {
            rv.setDescription(description)
        }

        roleMan.saveRole(user.authzSubjectValue, rv)
    }

    /**
     * Set the operations for a Role.
     */
    static void setOperations(Role role, AuthzSubject user, Collection ops) {
        roleMan.removeAllOperations(user.authzSubjectValue, role.roleValue)
        roleMan.addOperations(user.authzSubjectValue, role.roleValue,
                              ops as OperationValue[])
    }

    /**
     * Remove a Role.
     */
    static void remove(Role role, AuthzSubject user) {
        roleMan.removeRole(user.authzSubjectValue, role.id)
    }
}
