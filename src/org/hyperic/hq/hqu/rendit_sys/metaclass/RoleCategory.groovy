package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.authz.server.session.RoleManagerEJBImpl as RoleMan
import org.hyperic.hq.authz.shared.OperationValue
import org.hyperic.hq.authz.shared.AuthzSubjectValue
import org.hyperic.hq.authz.shared.ResourceGroupValue

class RoleCategory {
    private static roleMan = RoleMan.one

    static void setSubjects(Role role, AuthzSubject user, Collection subjects) {
        roleMan.removeSubjects(user.valueObject, role.valueObject,
                            (role.subjects.collect {it.id}) as Integer[])
        roleMan.addSubjects(user.valueObject, role.valueObject, 
                            (subjects.collect {it.id}) as Integer[])
    }

    static void setGroups(Role role, AuthzSubject user, Collection groups) {
        roleMan.removeResourceGroups(user.valueObject, role.valueObject,
                                     (role.resourceGroups.collect {it.id}) as Integer[])
        roleMan.addResourceGroups(user.valueObject, role.valueObject,
                                  (groups.collect {it.id}) as Integer[])
    }
    
    static void setOperations(Role role, AuthzSubject user, Collection ops) {
        roleMan.removeAllOperations(user.valueObject, role.valueObject)
        roleMan.addOperations(user.valueObject, role.valueObject,
                              (ops.collect { it.valueObject }) as OperationValue[])
    }

    static void remove(Role role, AuthzSubject user) {
        roleMan.removeRole(user.valueObject, role.id)
    }
}
