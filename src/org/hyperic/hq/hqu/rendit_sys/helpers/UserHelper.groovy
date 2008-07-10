package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as SubjectMan
import org.hyperic.hq.authz.shared.AuthzSubjectValue

class UserHelper extends BaseHelper {
    private subjectMan = SubjectMan.one
    private AuthzSubjectValue userValue

    UserHelper(AuthzSubject user) {
        super(user)
        userValue = user.valueObject
    }

    /**
     * Find all users
     * @return a List of {@link AuthzSubject}s
     */
    public getAllUsers() {
        subjectMan.getAllSubjects(userValue, [], null).collect {
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
    public createUser(userName, active, dsn, dept, email, first, last, phone,
                      sms, html) { 
        subjectMan.createSubject(userValue, userName, active, dsn, dept, email,
                                 first, last, phone, sms, html)
    }

    /**
     * Update a user
     * @return a {@link AuthzSubject}s
     */
    public updateUser(found, active, dsn, dept, email, first, last, phone, sms,
                      html) {
        subjectMan.updateSubject(user, found, active, dsn, dept, email, first,
                                 last, phone, sms, html)
    }
     
    /**
     * Update a user's password
     */
    public updateUserPassword(subject, password) {
        AuthManagerEJBImpl.one.changePasswordHash(userValue, subject.name,
                                                  password)
    }

    /**
     * Remove a user from database
     */
    public removeUser(uid) {
        subjectMan.removeSubject(userValue, uid)
    }
 }
