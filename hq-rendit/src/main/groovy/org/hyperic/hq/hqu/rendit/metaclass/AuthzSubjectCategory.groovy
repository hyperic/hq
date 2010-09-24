/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.hqu.rendit.metaclass
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;

import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.util.config.ConfigResponse

class AuthzSubjectCategory {

    static subMan = Bootstrap.getBean(AuthzSubjectManager.class)
    static authMan = Bootstrap.getBean(AuthManager.class)
    static authzBoss = Bootstrap.getBean(AuthzBoss.class)
    static operationDao = Bootstrap.getBean(OperationDAO.class)

    /**
     * Check if the current user has administration permission
     */
    static boolean isSuperUser(AuthzSubject subject) {
        PermissionManagerFactory.getInstance().hasAdminPermission(subject.id) 
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
