package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl
import org.hyperic.hq.authz.shared.PermissionManager
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal
import org.hyperic.util.config.ConfigResponse

class AuthzSubjectCategory {

    static AuthzSubjectManagerLocal subMan = AuthzSubjectManagerEJBImpl.one

    /**
     * Check if the current user has administration permission
     */
    static boolean isSuperUser(AuthzSubject subject) {
		PermissionManagerFactory
			.getInstance()
			.hasAdminPermission(subject.id)
    }

    /**
     * Get a user preference by key
     */
    static String getPreference(AuthzSubject subject, String key,
                                String defaultValue) {
        ConfigResponse cr = subMan.getUserPrefs(subject, subject.id)
        cr.getValue(key, defaultValue)
    }

    /**
     * Set a user preference
     */
    static void setPreference(AuthzSubject subject, String key, String val) {
        ConfigResponse cr = subMan.getUserPrefs(subject, subject.id)
        cr.setValue(key, val)
        subMan.setUserPrefs(subject, subject.id, cr)
    }
    
    static getPassword(AuthzSubject subject) {
        def principal = AuthManagerEJBImpl.one.getPrincipal(subject)
        principal?.password
    }
}
