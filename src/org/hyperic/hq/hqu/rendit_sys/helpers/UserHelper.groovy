package org.hyperic.hq.hqu.rendit.helpers

import java.util.List

import org.hyperic.hq.auth.server.session.AuthManagerEJBImpl as AuthMan
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as SubjectMan
import org.hyperic.hq.authz.shared.AuthzSubjectValue

/**
 * The UserHelper can be used to find Users in the HQ system.
 */
class UserHelper extends BaseHelper {
    private subjectMan = SubjectMan.one
    private authMan = AuthMan.one
    private AuthzSubjectValue userValue

    UserHelper(AuthzSubject user, int sessionId) {
        super(user, sessionId)
        userValue = user.valueObject
    }

    /**
     * Find all users
     * @return a List of {@link AuthzSubjectValues}s
     */
    public List getAllUsers() {
        subjectMan.getAllSubjects(userValue, [], null).collect {
            subjectMan.findSubjectById(it.id)
        }
    }

    /**
     * Find a user by name
     * @return a {@link AuthzSubjectValue}
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
        subjectMan.createSubject(userValue, userName, active, dsn, dept, email,
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
        def user = subjectMan.createSubject(userValue, userName, active, dsn,
                                            dept, email, first, last, phone,
                                            sms, html)
        authMan.addUser(userValue, userName, pass)
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
     
    /**
     * Update a user's password hash.
     */
    public void updateUserPassword(String subject, String hash) {
        authMan.changePasswordHash(userValue, subject.name, hash)
    }

    /**
     * Change the password for a user
     * @param subject The {@link AuthzSubject} to change the password for
     */
    public void changeUserPassword(subject, String password) {
        authMan.changePassword(userValue, subject.name, password)
    }

    /**
     * Remove a user from database
     */
    public void removeUser(Integer id) {
        subjectMan.removeSubject(userValue, id)
    }
 }
