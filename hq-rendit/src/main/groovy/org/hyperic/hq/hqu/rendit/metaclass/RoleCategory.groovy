package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Operation
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.authz.shared.RoleValue

class RoleCategory {
    private static roleMan = Bootstrap.getBean(RoleManager.class)

    static void setSubjects(Role role, AuthzSubject user, Collection subjects) {
        roleMan.removeSubjects(user, role.id,
                               (role.subjects.collect {it.id}) as Integer[])
        roleMan.addSubjects(user, role.id, 
                            (subjects.collect {it.id}) as Integer[]) 
    }

    static void setGroups(Role role, AuthzSubject user, Collection groups) {
        roleMan.removeAllResourceGroups(user, role)
        roleMan.addResourceGroups(user, role.id,
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

        roleMan.saveRole(user, rv)
    }

    /**
     * Set the operations for a Role.
     */
    static void setOperations(Role role, AuthzSubject user, Collection ops) {
        roleMan.setOperations(user, role.id, ops as Operation[])
    }

    /**
     * Remove a Role.
     */
    static void remove(Role role, AuthzSubject user) {
        roleMan.removeRole(user, role.id)
    }
    
    /**
    * Get the Resource Groups for a Role
    */
    static Collection getGroups(Role role, AuthzSubject user) {
        roleMan.getResourceGroupsByRole(user, role)
    }
}
