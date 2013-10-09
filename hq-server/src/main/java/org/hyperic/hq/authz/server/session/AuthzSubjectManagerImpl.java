/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.server.session.UserAuditFactory;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.shared.CrispoManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 *
 */
@Service("authzSubjectManager")
@Transactional
public class AuthzSubjectManagerImpl implements AuthzSubjectManager, ApplicationContextAware {

    private final Log log = LogFactory.getLog(AuthzSubjectManagerImpl.class);

    private static final String SUBJECT_PAGER = PagerProcessor_subject.class.getName();
    private Pager subjectPager;
    private AuthzSubjectDAO authzSubjectDAO;
    private ResourceTypeDAO resourceTypeDAO;
    private ResourceDAO resourceDAO;
    private CrispoManager crispoManager;
    private PermissionManager permissionManager;
    private UserAuditFactory userAuditFactory;
    private ApplicationContext applicationContext;

    @Autowired
    public AuthzSubjectManagerImpl(AuthzSubjectDAO authzSubjectDAO,
                                   ResourceTypeDAO resourceTypeDAO, ResourceDAO resourceDAO,
                                   CrispoManager crispoManager,
                                   PermissionManager permissionManager,
                                   UserAuditFactory userAuditFactory) {
        this.authzSubjectDAO = authzSubjectDAO;
        this.resourceTypeDAO = resourceTypeDAO;
        this.resourceDAO = resourceDAO;
        this.crispoManager = crispoManager;
        this.permissionManager = permissionManager;
        this.userAuditFactory = userAuditFactory;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        subjectPager = Pager.getPager(SUBJECT_PAGER);
    }

    /**
     * Find the subject that has the given name and authentication source.
     * @param name Name of the subject.
     * @param authDsn DSN of the authentication source. Authentication sources
     *        are defined externally.
     * @return The value-object of the subject of the given name and
     *         authenticating source.
     */
    @Transactional(readOnly = true)
    public AuthzSubject findSubjectByAuth(String name, String authDsn)
        throws SubjectNotFoundException {
        AuthzSubject subject = authzSubjectDAO.findByAuth(name, authDsn);
        if (subject == null) {
            throw new SubjectNotFoundException("Can't find subject: name=" + name + ",authDsn=" +
                                               authDsn);
        }
        return subject;
    }

    /**
     * Create a subject.
     * @param whoami The current running user.
     * @return Value-object for the new Subject.
     * 
     */
    public AuthzSubject createSubject(AuthzSubject whoami, String name, boolean active, String dsn,
                                      String dept, String email, String first, String last,
                                      String phone, String sms, boolean html)
        throws PermissionException, ApplicationException {

        permissionManager.check(whoami.getId(), resourceTypeDAO.findTypeResourceType(),
            AuthzConstants.rootResourceId, AuthzConstants.subjectOpCreateSubject);

        AuthzSubject existing = authzSubjectDAO.findByName(name);
        if (existing != null) {
            throw new ApplicationException("A system user already exists with " + name);
        }

        AuthzSubject subjectPojo = authzSubjectDAO.create(whoami, name, active, dsn, dept, email,
            first, last, phone, sms, html);

        userAuditFactory.createAudit(whoami, subjectPojo);
        return subjectPojo;
    }

    /**
     * Update user settings for the target
     * 
     * @param whoami The current running user.
     * @param target The subject to save.
     * 
     *        The rest of the parameters specify settings to update. If they are
     *        null, then no change will be made to them.
     * 
     */
    public void updateSubject(AuthzSubject whoami, AuthzSubject target, Boolean active, String dsn,
                              String dept, String email, String firstName, String lastName,
                              String phone, String sms, Boolean useHtml) throws PermissionException {

        if (!whoami.getId().equals(target.getId())) {   
            permissionManager.check(whoami.getId(), resourceTypeDAO.findTypeResourceType(),
                AuthzConstants.rootResourceId, AuthzConstants.subjectOpModifySubject);
        }

        if (active != null && target.getActive() != active.booleanValue()) {
            // Root user can not be disabled
            if (target.getId().equals(AuthzConstants.rootSubjectId)) {
                throw new PermissionException("Cannot change active status of " + "root user");
            }

            target.setActive(active.booleanValue());
            userAuditFactory.updateAudit(whoami, target, AuthzSubjectField.ACTIVE, target
                .getActive() +
                                                                                   "", active + "");
        }

        if (dsn != null && !dsn.equals(target.getAuthDsn())) {
            target.setAuthDsn(dsn);
        }

        if (dept != null && !dept.equals(target.getDepartment())) {
            target.setDepartment(dept);
            userAuditFactory.updateAudit(whoami, target, AuthzSubjectField.DEPT, target
                .getDepartment(), dept);
        }

        if (email != null && !email.equals(target.getEmailAddress())) {
            target.setEmailAddress(email);
            userAuditFactory.updateAudit(whoami, target, AuthzSubjectField.EMAIL, target
                .getEmailAddress(), email);
        }

        if (useHtml != null && target.getHtmlEmail() != useHtml.booleanValue()) {
            target.setHtmlEmail(useHtml.booleanValue());
            userAuditFactory.updateAudit(whoami, target, AuthzSubjectField.HTML, target
                .getHtmlEmail() +
                                                                                 "", useHtml + "");
        }

        if (firstName != null && !firstName.equals(target.getFirstName())) {
            target.setFirstName(firstName);
            userAuditFactory.updateAudit(whoami, target, AuthzSubjectField.FIRSTNAME, target
                .getFirstName(), firstName);
        }

        if (lastName != null && !lastName.equals(target.getLastName())) {
            target.setLastName(lastName);
            userAuditFactory.updateAudit(whoami, target, AuthzSubjectField.LASTNAME, target
                .getLastName(), lastName);
        }

        if (phone != null && !phone.equals(target.getPhoneNumber())) {
            target.setPhoneNumber(phone);
            userAuditFactory.updateAudit(whoami, target, AuthzSubjectField.PHONE, target
                .getPhoneNumber(), phone);
        }

        if (sms != null && !sms.equals(target.getSMSAddress())) {
            target.setSMSAddress(sms);
            userAuditFactory.updateAudit(whoami, target, AuthzSubjectField.SMS, target
                .getSMSAddress(), sms);
        }
    }

    /**
     * Check if a subject can modify users
     * 
     */
    @Transactional(readOnly = true)
    public void checkModifyUsers(AuthzSubject caller) throws PermissionException {

        permissionManager.check(caller.getId(), resourceTypeDAO.findTypeResourceType(),
            AuthzConstants.rootResourceId, AuthzConstants.subjectOpModifySubject);
    }

    /**
     * Delete the specified subject.
     * 
     * @param whoami The current running user.
     * @param subject The ID of the subject to delete.
     * 
     */
    public void removeSubject(AuthzSubject whoami, Integer subject) throws PermissionException {
        // no removing of the root user!
        if (subject.equals(AuthzConstants.rootSubjectId)) {
            throw new PermissionException("Root user can not be deleted");
        }

        AuthzSubject toDelete = authzSubjectDAO.findById(subject);

        // XXX Should we do anything special for the "suicide" case?
        // Perhaps a _log message?
        if (!whoami.getId().equals(subject)) {

            permissionManager.check(whoami.getId(), resourceTypeDAO.findTypeResourceType().getId(),
                AuthzConstants.rootResourceId, AuthzConstants.perm_removeSubject);
        }

        // Reassign all resources to the root user before deleting
        resourceDAO.reassignResources(subject.intValue(), AuthzConstants.rootSubjectId.intValue());

        applicationContext.publishEvent(new SubjectDeleteRequestedEvent(toDelete));
     
        authzSubjectDAO.remove(toDelete);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public AuthzSubject findByAuth(String name, String authDsn) {
        return authzSubjectDAO.findByAuth(name, authDsn);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public AuthzSubject findSubjectById(AuthzSubject whoami, Integer id) throws PermissionException {

        // users can see their own entries without requiring special permission
        if (!whoami.getId().equals(id)) {
            permissionManager.check(whoami.getId(), resourceTypeDAO.findTypeResourceType().getId(),
                AuthzConstants.rootResourceId, AuthzConstants.perm_viewSubject);
        }
        return findSubjectById(id);
    }

    @Transactional(readOnly = true)
    public AuthzSubject findSubjectById(Integer id) {
        return authzSubjectDAO.findById(id);
    }
    
    @Transactional(readOnly = true)
    public String findSubjectName(Integer id) {
       return findSubjectById(id).getName();
    }

    /** 
     * 
     */
    @Transactional(readOnly = true)
    public AuthzSubject getSubjectById(Integer id) {
        return authzSubjectDAO.get(id);
    }

    /** 
     * 
     */
    @Transactional(readOnly = true)
    public AuthzSubject findSubjectByName(AuthzSubject whoami, String name)
        throws PermissionException {
        return findSubjectByName(name);
    }

    /** 
     * 
     */
    @Transactional(readOnly = true)
    public AuthzSubject findSubjectByName(String name) {
        return authzSubjectDAO.findByName(name);
    }

    /** 
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AuthzSubject> findMatchingName(String name, PageControl pc) {
        return authzSubjectDAO.findMatchingName(name, pc);
    }

    /**
     * List all subjects in the system
     * 
     * @param excludes the IDs of subjects to exclude from result
     */
    @Transactional(readOnly = true)
    public PageList<AuthzSubjectValue> getAllSubjects(AuthzSubject whoami,
                                                      Collection<Integer> excludes, PageControl pc)
        throws NotFoundException, PermissionException {

        pc = PageControl.initDefaults(pc, SortAttribute.SUBJECT_NAME);

        // if a user does not have permission to view subjects,
        // all they can see is their own entry.
        AuthzSubject who = authzSubjectDAO.findById(whoami.getId());
        Collection<AuthzSubject> subjects;
        try {

            permissionManager.check(whoami.getId(), resourceTypeDAO.findTypeResourceType(),
                AuthzConstants.rootResourceId, AuthzConstants.subjectOpViewSubject);

            if (!permissionManager.hasGuestRole()) {
                if (excludes == null) {
                    excludes = new ArrayList<Integer>(1);
                }
                excludes.add(AuthzConstants.guestId);
            }
        } catch (PermissionException e) {
            PageList<AuthzSubjectValue> plist = new PageList<AuthzSubjectValue>();

            // return a list with only the one entry.
            plist.add(who.getAuthzSubjectValue());
            plist.setTotalSize(1);
            return plist;
        }

        switch (pc.getSortattribute()) {
            case SortAttribute.SUBJECT_NAME:
                if (who.isRoot())
                    subjects = authzSubjectDAO.findAllRoot_orderName(excludes, pc.isAscending());
                else
                    subjects = authzSubjectDAO.findAll_orderName(excludes, pc.isAscending());
                break;

            case SortAttribute.FIRST_NAME:
                if (who.isRoot())
                    subjects = authzSubjectDAO.findAllRoot_orderFirstName(excludes, pc
                        .isAscending());
                else
                    subjects = authzSubjectDAO.findAll_orderFirstName(excludes, pc.isAscending());
                break;

            case SortAttribute.LAST_NAME:
                if (who.isRoot())
                    subjects = authzSubjectDAO
                        .findAllRoot_orderLastName(excludes, pc.isAscending());
                else
                    subjects = authzSubjectDAO.findAll_orderLastName(excludes, pc.isAscending());
                break;

            default:
                throw new NotFoundException("Unrecognized sort attribute: " + pc.getSortattribute());
        }

        return subjectPager.seek(subjects, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get the subjects with the specified ids
     * 
     * NOTE: This method returns an empty PageList if a null or empty array of
     * ids is received.
     * @param ids the subject ids
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AuthzSubjectValue> getSubjectsById(AuthzSubject subject, Integer[] ids,
                                                       PageControl pc) throws PermissionException {

        // PR7251 - Sometimes and for no good reason, different parts of the UI
        // call this method with an empty ids array. In this case, simply return
        // an empty page list.
        if (ids == null || ids.length == 0) {
            return new PageList<AuthzSubjectValue>();
        }

        // find the requested subjects
        PageList<AuthzSubject> subjects = authzSubjectDAO.findById_orderName(ids, pc);

        // check permission unless the list includes only the id of
        // the subject being requested. This is ugly mostly because
        // we're using a list api to possibly look up a single Item
        if (subjects.size() > 0) {
            log.debug("Checking if Subject: " + subject.getName() + " can list subjects.");

            permissionManager.check(subject.getId(), resourceTypeDAO.findTypeResourceType(),
                AuthzConstants.rootResourceId, AuthzConstants.subjectOpViewSubject);
        }

        // Need to convert to value objects
        return new PageList<AuthzSubjectValue>(subjectPager.seek(subjects, PageControl.PAGE_ALL),
            subjects.getTotalSize());
    }
    
    /**
     * Get the subjects with the specified ids
     * 
     * NOTE: This method returns an empty list if a null or empty array of
     * ids is received.
     * @param ids the subject ids
     * 
     */
    @Transactional(readOnly = true)
    public Collection<AuthzSubject> getSubjectsById(AuthzSubject subject, Integer[] ids) throws PermissionException {
        if (ids == null || ids.length == 0) {
            return Collections.emptyList();
        }

        // find the requested subjects
        Collection<AuthzSubject> subjects = authzSubjectDAO.findByIds(ids);

        // check permission unless the list includes only the id of
        // the subject being requested. This is ugly mostly because
        // we're using a list api to possibly look up a single Item
        if (subjects.size() > 0) {
            log.debug("Checking if Subject: " + subject.getName() + " can list subjects.");

            permissionManager.check(subject.getId(), resourceTypeDAO.findTypeResourceType(),
                AuthzConstants.rootResourceId, AuthzConstants.subjectOpViewSubject);
        }

        return subjects;
    }    

    /**
     * Find the e-mail of the subject specified by id
     * @param id id of the subject.
     * @return The e-mail address of the subject
     * 
     */
    @Transactional(readOnly = true)
    public String getEmailById(Integer id) {
        AuthzSubject subject = authzSubjectDAO.findById(id);
        return subject.getEmailAddress();
    }

    /**
     * Find the e-mail of the subject specified by name
     * @param userName Name of the subjects.
     * @return The e-mail address of the subject
     * 
     */
    @Transactional(readOnly = true)
    public String getEmailByName(String userName) {
        AuthzSubject subject = authzSubjectDAO.findByName(userName);
        return subject.getEmailAddress();
    }

    /**
     * Get the Preferences for a specified user
     * 
     */
    @Transactional(readOnly = true)
    public ConfigResponse getUserPrefs(AuthzSubject who, Integer subjId) throws PermissionException {
        // users can always see their own prefs.
        if (!who.getId().equals(subjId)) {
            // check that the caller can see users
            permissionManager.check(who.getId(), resourceTypeDAO.findTypeResourceType(),
                AuthzConstants.rootResourceId, AuthzConstants.subjectOpViewSubject);
        }

        AuthzSubject targ = authzSubjectDAO.findById(subjId);
        Crispo c = targ.getPrefs();
        if (c == null)
            return new ConfigResponse();
        return c.toResponse();
    }

    /**
     * Set the Preferences for a specified user
     * 
     */
    public void setUserPrefs(AuthzSubject who, Integer subjId, ConfigResponse prefs)
        throws PermissionException {
        // check to see if the user attempting the modification
        // is the same as the one being modified
        if (!(who.getId().intValue() == subjId.intValue())) {

            permissionManager.check(who.getId(), resourceTypeDAO.findTypeResourceType(),
                AuthzConstants.rootResourceId, AuthzConstants.subjectOpModifySubject);
        }

        AuthzSubject targ = authzSubjectDAO.findById(subjId);

        if (targ.getPrefs() != null)
            crispoManager.update(targ.getPrefs(), prefs);
        else {
            Crispo newPrefs = crispoManager.create(prefs);
            targ.setPrefs(newPrefs);
        }
    }
    
    public void setUserPrefs(Integer whoId, Integer subjectId, ConfigResponse prefs)
        throws PermissionException, SubjectNotFoundException {
         AuthzSubject who = getSubjectById(whoId);
         if(who == null) {
             throw new SubjectNotFoundException("Subject with id " + whoId + " not found");
         }
         setUserPrefs(who, subjectId, prefs);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public AuthzSubject getOverlordPojo() {
        AuthzSubject overlord = authzSubjectDAO.findById(AuthzConstants.overlordId);
        //initialize name to pass Subject b/w method during non-tx testing
        overlord.getName();
        return overlord;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
