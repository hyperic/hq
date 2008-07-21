package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl as AuthMan
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as SubMan
import org.hyperic.hq.bizapp.server.session.AuthzBossEJBImpl as AuthzBoss
import org.hyperic.util.config.ConfigResponse

class AuthzSubjectCategory {

    static subMan = SubMan.one
    static authMan = AuthMan.one
    static authzBoss = AuthzBoss.one

    /**
     * Check if the current user has administration permission
     */
    static boolean isSuperUser(AuthzSubject subject) {
        PermissionManagerFactory.getInstance().hasAdminPermission(subject.id)
    }
    
    static getPassword(AuthzSubject subject) {
        def principal = authMan.getPrincipal(subject)
        principal?.password
    }

    /**
     * Update a user's password hash.
     */
    static void updatePassword(AuthzSubject subject, AuthzSubject user,
                               String hash) {
        authMan.changePasswordHash(user.authzSubjectValue, subject.name, hash)
    }

    /**
     * Change the password for a user
     * @param subject The {@link AuthzSubject} to change the password for
     */
    static void changePassword(AuthzSubject subject, AuthzSubject user,
                               String password) {
        authMan.changePassword(user.authzSubjectValue, subject.name, password)
    }

    /**
     * Remove a user from database
     */
    static void remove(AuthzSubject subject, AuthzSubject user) {
        int sessionId = SessionManager.instance.put(user)
        authzBoss.removeSubject(sessionId, [subject.id] as Integer[])
    }
}
