package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl
import org.hyperic.hq.authz.shared.PermissionManager
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.server.session.AuthzSubject

class AuthzSubjectCategory {
    static boolean isSuperUser(AuthzSubject subject) {
		PermissionManagerFactory
			.getInstance()
			.hasAdminPermission(subject.id)
    }
    
    static getPassword(AuthzSubject subject) {
        def principal = AuthManagerEJBImpl.one.getPrincipal(subject)
        principal?.password
    }
}
