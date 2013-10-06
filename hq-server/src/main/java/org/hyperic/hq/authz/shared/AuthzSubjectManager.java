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
package org.hyperic.hq.authz.shared;

import java.util.Collection;

import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local interface for AuthzSubjectManager.
 */
public interface AuthzSubjectManager {
    /**
     * Create a subject.
     * @param whoami The current running user.
     * @return Value-object for the new Subject.
     */
    public AuthzSubject createSubject(AuthzSubject whoami, String name, boolean active, String dsn,
                                      String dept, String email, String first, String last,
                                      String phone, String sms, boolean html)
        throws PermissionException, ApplicationException;

    /**
     * Update user settings for the target
     * @param whoami The current running user.
     * @param target The subject to save. The rest of the parameters specify
     *        settings to update. If they are null, then no change will be made
     *        to them.
     */
    public void updateSubject(AuthzSubject whoami, AuthzSubject target, Boolean active, String dsn,
                              String dept, String email, String firstName, String lastName,
                              String phone, String sms, Boolean useHtml) throws PermissionException;

    /**
     * Check if a subject can modify users
     */
    public void checkModifyUsers(AuthzSubject caller) throws PermissionException;

    /**
     * Delete the specified subject.
     * @param whoami The current running user.
     * @param subject The ID of the subject to delete.
     */
    public void removeSubject(AuthzSubject whoami, Integer subject) throws PermissionException;

    public AuthzSubject findByAuth(String name, String authDsn);

    public AuthzSubject findSubjectById(AuthzSubject whoami, Integer id) throws PermissionException;

    public AuthzSubject findSubjectById(Integer id);

    public AuthzSubject getSubjectById(Integer id);

    public AuthzSubject findSubjectByName(AuthzSubject whoami, String name)
        throws PermissionException;

    public AuthzSubject findSubjectByName(String name);
    
    public String findSubjectName(Integer id);

    public PageList<AuthzSubject> findMatchingName(String name, PageControl pc);

    /**
     * List all subjects in the system
     * @param excludes the IDs of subjects to exclude from result
     */
    public PageList<AuthzSubjectValue> getAllSubjects(AuthzSubject whoami,
                                                      java.util.Collection<Integer> excludes,
                                                      PageControl pc) throws PermissionException,
        NotFoundException;

    /**
     * Get the subjects with the specified ids NOTE: This method returns an
     * empty PageList if a null or empty array of ids is received.
     * @param ids the subject ids
     */
    public PageList<AuthzSubjectValue> getSubjectsById(AuthzSubject subject,
                                                       java.lang.Integer[] ids, PageControl pc)
        throws PermissionException;
    
    /**
     * Get the subjects with the specified ids
     * NOTE: This method returns an empty list if a null or empty array of
     * ids is received.
     * @param ids the subject ids
     * 
     */
    @Transactional(readOnly = true)
    public Collection<AuthzSubject> getSubjectsById(AuthzSubject subject, Integer[] ids) 
            throws PermissionException;    

    /**
     * Find the e-mail of the subject specified by id
     * @param id id of the subject.
     * @return The e-mail address of the subject
     */
    public String getEmailById(Integer id);

    /**
     * Find the e-mail of the subject specified by name
     * @param userName Name of the subjects.
     * @return The e-mail address of the subject
     */
    public String getEmailByName(String userName);

    /**
     * Get the Preferences for a specified user
     */
    public ConfigResponse getUserPrefs(AuthzSubject who, Integer subjId) throws PermissionException;

    /**
     * Set the Preferences for a specified user
     */
    public void setUserPrefs(AuthzSubject who, Integer subjId, ConfigResponse prefs)
        throws PermissionException;

    /**
     * Set the Preferences for a specified user
     */
    public void setUserPrefs(Integer whoId, Integer subjectId, ConfigResponse prefs)
        throws PermissionException, SubjectNotFoundException;

    public AuthzSubject getOverlordPojo();

    /**
     * Find the subject that has the given name and authentication source.
     * @param name Name of the subject.
     * @param authDsn DSN of the authentication source. Authentication sources
     *        are defined externally.
     * @return The value-object of the subject of the given name and
     *         authenticating source.
     */
    public AuthzSubject findSubjectByAuth(String name, String authDsn)
        throws SubjectNotFoundException;

}
