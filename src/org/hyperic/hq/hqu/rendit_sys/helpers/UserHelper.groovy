package org.hyperic.hq.hqu.rendit.helpers

import java.util.List

import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as SubjectMan
import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl as AuthMan

class UserHelper extends BaseHelper {
    private subjectMan = SubjectMan.one
    private authMan = AuthMan.one

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
     * @return a {@link AuthzSubject}s
     */
    public AuthzSubject findUser(name) {
        subjectMan.findSubjectByName(name)
    }

    /**
     * Create a user
     * @return a {@link AuthzSubject}s
     */
    public AuthzSubject createUser(userName, active, dsn, dept, email,
                                   first, last, phone,
                                   sms, html) {
        subjectMan.createSubject(user, userName, active, dsn, dept, email,
                                 first, last, phone, sms, html)
    }

    /**
     * Create a user with the given password.
     * @return a {@link AuthzSubject}s
     */
    public AuthzSubject createUser(userName, pass, active, dsn, dept, email,
                                   first, last, phone, sms, html) {
        def user = subjectMan.createSubject(user, userName, active, dsn,
                                            dept, email, first, last, phone,
                                            sms, html)
        authMan.addUser(user, userName, pass)
        user
    }

    /**
     * Update a user
     */
    public void updateUser(found, active, dsn, dept, email, first,
                           last, phone, sms, html) {
        subjectMan.updateSubject(user, found, active, dsn, dept, email, first,
                                 last, phone, sms, html)
    }
     
    /**
     * Update a user's password
     */
    public updateUserPassword(subject, password) {
        AuthManagerEJBImpl.one.changePasswordHash(userValue, subject, password)
    }

    /**
     * Remove a user from database
     */
    public removeUser(uid) {
        subjectMan.removeSubject(user, uid)
    }
 }
