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

package org.hyperic.hq.hqu.rendit.helpers

import java.util.List



import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.authz.server.session.AuthzSubject

/**
 * The UserHelper can be used to find Users in the HQ system.
 */
class UserHelper extends BaseHelper {
    private subjectMan = Bootstrap.getBean(AuthzSubjectManager.class)
    private authMan = Bootstrap.getBean(AuthManager.class)

    UserHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * Find all users
     * @return a List of {@link AuthzSubject}s
     */
    public List getAllUsers() {
        subjectMan.getAllSubjects(user, [], null).collect {
            subjectMan.findSubjectById(it.id)
        }
    }

    /**
     * Find a user by name
     * @return a {@link AuthzSubject}
     */
    public findUser(String name) {
        subjectMan.findSubjectByName(name)
    }

    /**
     * Get a user by id.
     * @return The {@link AuthzSubject} for this id, or null if the id does
     * not exist.
     */
    public getUser(Integer id) {
        subjectMan.getSubjectById(id)
    }

    /**
     * Create a user
     * @return a {@link AuthzSubject}s
     */
    public createUser(String userName, boolean active, String dsn,
                      String dept, String email, String first,
                      String last, String phone, String sms,
                      boolean html) {
        subjectMan.createSubject(user, userName, active, dsn, dept, email,
                                 first, last, phone, sms, html)
    }

    /**
     * Create a user with the given password.
     * @return a {@link AuthzSubject}s
     */
    public createUser(String userName, String pass, boolean active,
                      String dsn, String dept, String email,
                      String first, String last, String phone,
                      String sms, boolean html) {
        def user = subjectMan.createSubject(user, userName, active, dsn,
                                            dept, email, first, last, phone,
                                            sms, html)
        authMan.addUser(user, userName, pass)
        user
    }

    /**
     * Update a user
     * @param found The {@link AuthzSubject} to update.
     */
    public void updateUser(found, boolean active, String dsn,
                           String dept, String email, String first,
                           String last, String phone, String sms, boolean html) {
        subjectMan.updateSubject(user, found, active, dsn, dept, email, first,
                                 last, phone, sms, html)
    }
 }
