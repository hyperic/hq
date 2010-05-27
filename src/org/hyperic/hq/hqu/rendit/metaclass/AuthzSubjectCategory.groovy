package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.dao.DAOFactory
import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl as AuthMan
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as SubMan
import org.hyperic.hq.authz.server.session.Operation
import org.hyperic.hq.authz.server.session.OperationDAO
import org.hyperic.hq.bizapp.server.session.AuthzBossEJBImpl as AuthzBoss
import org.hyperic.util.config.ConfigResponse

class AuthzSubjectCategory {

    static subMan = SubMan.one
    static authMan = AuthMan.one
    static authzBoss = AuthzBoss.one
    static operationDao = new OperationDAO(DAOFactory.getDAOFactory())

    /**
     * Check if the current user has administration permission
     */
    static boolean isSuperUser(AuthzSubject subject) {
        PermissionManagerFactory.getInstance().hasAdminPermission(subject.id)
    }
    
    /**
     * Check if the AuthzSubject has the ability to run the specified operation
     */
    static boolean hasOperation(AuthzSubject subject, String opName) {
        Operation op = operationDao.getByName(opName)
        if (op == null) {
            return false
        }
        return operationDao.userHasOperation(subject, op)
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

    /**
     * Get the hashed password for a user.
     */
    static getPassword(AuthzSubject subject) {
        def principal = authMan.getPrincipal(subject)
        principal?.password
    }

    /**
     * Update a user's password hash.
     */
    static void updatePassword(AuthzSubject subject, AuthzSubject user,
                               String hash) {
        authMan.changePasswordHash(user, subject.name, hash)
    }

    /**
     * Change the password for a user
     * @param subject The {@link AuthzSubject} to change the password for
     */
    static void changePassword(AuthzSubject subject, AuthzSubject user,
                               String password) {
        authMan.changePassword(user, subject.name, password)
    }

    /**
     * Remove a user from database
     */
    static void remove(AuthzSubject subject, AuthzSubject user) {
        int sessionId = SessionManager.instance.put(user)
        authzBoss.removeSubject(sessionId, [subject.id] as Integer[])
    }
}
