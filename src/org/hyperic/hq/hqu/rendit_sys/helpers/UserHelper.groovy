package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as SubjectMan

class UserHelper extends BaseHelper {
    private subjectMan = SubjectMan.one

    UserHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * Find all users
     * @return a List of {@link AuthzSubject}s
     */
    public getAllUsers() {
        subjectMan.getAllSubjects(user, [], null).collect {
            subjectMan.findSubjectById(it.id)
        }
    }

    /**
     * Find a user by name
     * @return a {@link AuthzSubject}s
     */
    public findUser(name) {
        subjectMan.findSubjectByName(name)
    }

    /**
     * Create a user
     * @return a {@link AuthzSubject}s
     */
    public createUser(userName, active, dsn, dept, email, first, last, phone, sms, html) { 
        subjectMan.createSubject(user, userName, active, dsn, dept, email, first, last, phone, sms, html)
    }

    /**
     * Update a user
     * @return a {@link AuthzSubject}s
     */
    public updateUser(found, active, dsn, dept, email, first, last, phone, sms, html) {
        subjectMan.updateSubject(user, found, active, dsn, dept, email, first, last, phone, sms, html)
    }

    /**
     * Remove a user from database
     */
    public removeUser(uid) {
        subjectMan.removeSubject(user.valueObject, uid)
    }
 }
