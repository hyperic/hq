package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.shared.PermissionManager
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.server.session.AuthzSubject

class AuthzSubjectCategory {
    static boolean isSuperUser(AuthzSubject subject) {
		PermissionManagerFactory
			.getInstance()
			.hasAdminPermission(subject.authzSubjectValue)
    }
}
