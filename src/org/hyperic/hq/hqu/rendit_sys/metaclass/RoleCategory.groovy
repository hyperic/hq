package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Role
import org.hyperic.hq.authz.server.session.RoleManagerEJBImpl as RoleMan
import org.hyperic.hq.authz.shared.AuthzSubjectValue
import org.hyperic.hq.authz.shared.ResourceGroupValue

class RoleCategory {
    private static roleMan = RoleMan.one

    static void setSubjects(Role role, AuthzSubject user, Collection subjects) {
        roleMan.setSubjects(user.valueObject, role.valueObject, 
                            (subjects.collect {it.valueObject}) as AuthzSubjectValue[])
    }

    static void setGroups(Role role, AuthzSubject user, Collection groups) {
        roleMan.setResourceGroups(user.valueObject, role.valueObject,
                                  (groups.collect {it.valueObject}) as ResourceGroupValue[])
    }
}
