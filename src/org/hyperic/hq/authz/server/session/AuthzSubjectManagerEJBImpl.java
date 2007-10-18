/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.server.session.UserAudit;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoManagerEJBImpl;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

/** Session bean to manipulate Subjects
 *
 *
 * @ejb:bean name="AuthzSubjectManager"
 *      jndi-name="ejb/authz/AuthzSubjectManager"
 *      local-jndi-name="LocalAuthzSubjectManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class AuthzSubjectManagerEJBImpl
    extends AuthzSession implements SessionBean {

    private static final Log _log
        = LogFactory.getLog(AuthzSubjectManagerEJBImpl.class);

    private static final String SUBJECT_PAGER =
        PagerProcessor_subject.class.getName();
    private Pager _subjectPager;

    public AuthzSubjectManagerEJBImpl() {}

    /** 
     * Create a subject.
     * @param whoami The current running user.
     * @param subject The subject to be created.
     * @return Value-object for the new Subject.
     * @ejb:interface-method
     */
    public AuthzSubject createSubject(AuthzSubjectValue whoami, String name,
                                      boolean active, String dsn, String dept,
                                      String email, String first, String last,
                                      String phone, String sms, boolean html)
        throws PermissionException, CreateException 
    {
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(), getRootResourceType(),
                 AuthzConstants.rootResourceId,
                 AuthzConstants.subjectOpCreateSubject);
        AuthzSubjectDAO dao = getSubjectDAO();

        AuthzSubject existing = dao.findByName(name);
        if (existing != null) {
            throw new CreateException("A system user already exists with " +
                                      name);
        }

        AuthzSubject whoamiPojo  = findSubjectById(whoami.getId()); 
        AuthzSubject subjectPojo = dao.create(whoamiPojo, name, active, dsn,
                                              dept, email, first, last, phone, 
                                              sms, html);

        UserAudit.createAudit(whoamiPojo, subjectPojo);
        return subjectPojo;
    }

    /** 
     * Update user settings for the target
     * 
     * @param whoami The current running user.
     * @param target The subject to save.
     * 
     * The rest of the parameters specify settings to update.  If they are 
     * null, then no change will be made to them.
     * @ejb:interface-method
     */
    public void updateSubject(AuthzSubject whoami, AuthzSubject target,
                              Boolean active, String dsn, String dept,
                              String email, String firstName, String lastName, 
                              String phone, String sms, Boolean useHtml)
        throws PermissionException 
    {
        PermissionManager pm = PermissionManagerFactory.getInstance(); 

        if(!whoami.getId().equals(target.getId())) {
            pm.check(whoami.getId(), getRootResourceType().getId(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.perm_viewSubject);
        }
        
        if (active != null && target.getActive() != active.booleanValue()) {
            // Root user can not be disabled
            if (target.getId().equals(AuthzConstants.rootSubjectId)) {
                throw new PermissionException("Cannot change active status of "+
                                              "root user");
            }
            
            target.setActive(active.booleanValue());
            UserAudit.updateAudit(whoami, target, AuthzSubjectField.ACTIVE, 
                                  target.getActive() + "", active + "");
        }
        
        if (dsn != null && !dsn.equals(target.getAuthDsn())) {
            target.setAuthDsn(dsn);
        }
        
        if (dept != null && !dept.equals(target.getDepartment())) {
            target.setDepartment(dept);
            UserAudit.updateAudit(whoami, target, AuthzSubjectField.DEPT, 
                                  target.getDepartment(), dept);
        }
        
        if (email != null && !email.equals(target.getEmailAddress())) {
            target.setEmailAddress(email);
            UserAudit.updateAudit(whoami, target, AuthzSubjectField.EMAIL, 
                                  target.getEmailAddress(), email);
        }
        
        if (useHtml != null && target.getHtmlEmail() != useHtml.booleanValue()){
            target.setHtmlEmail(useHtml.booleanValue());
            UserAudit.updateAudit(whoami, target, AuthzSubjectField.HTML, 
                                  target.getHtmlEmail() + "",
                                  useHtml + "");
        }
        
        if (firstName != null && !firstName.equals(target.getFirstName())) {
            target.setFirstName(firstName);
            UserAudit.updateAudit(whoami, target, AuthzSubjectField.FIRSTNAME, 
                                  target.getFirstName(), firstName);
        }

        if (lastName != null && !lastName.equals(target.getLastName())) {
            target.setFirstName(firstName);
            UserAudit.updateAudit(whoami, target, AuthzSubjectField.LASTNAME, 
                                  target.getLastName(), lastName);
        }

        if (phone != null && !phone.equals(target.getPhoneNumber())) {
            target.setPhoneNumber(phone);
            UserAudit.updateAudit(whoami, target, AuthzSubjectField.PHONE, 
                                  target.getPhoneNumber(), phone);
        }

        if (sms != null && !sms.equals(target.getSMSAddress())) {
            target.setSMSAddress(sms);
            UserAudit.updateAudit(whoami, target, AuthzSubjectField.SMS, 
                                  target.getSMSAddress(), sms);
        }
    }

    /**
     * Check if a subject can modify users  
     * @ejb:interface-method
     */
    public void checkModifyUsers(AuthzSubjectValue caller) 
        throws PermissionException 
    {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(caller.getId(),
                 getRootResourceType(),
                 AuthzConstants.rootResourceId,
                 AuthzConstants.subjectOpModifySubject);
    }

    /** 
     * Delete the specified subject.
     * 
     * @param whoami  The current running user.
     * @param subject The ID of the subject to delete.
     * @ejb:interface-method
     */
    public void removeSubject(AuthzSubjectValue whoami, Integer subject)
        throws RemoveException, PermissionException 
    {
        // no removing of the root user!
        if (subject.equals(AuthzConstants.rootSubjectId)) {
            throw new RemoveException("Root user can not be deleted");
        }

        AuthzSubjectDAO dao = getSubjectDAO();
        AuthzSubject toDelete = dao.findById(subject);

        // XXX Should we do anything special for the "suicide" case?
        // Perhaps a _log message?
        if ( !whoami.getId().equals(subject) ) {
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(whoami.getId(), getRootResourceType().getId(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.perm_removeSubject);
        }
        
        // Reassign all resources to the root user before deleting
        getResourceDAO()
            .reassignResources(subject.intValue(),
                               AuthzConstants.rootSubjectId.intValue());

        dao.remove(toDelete);
    }

    /**
     * @ejb:interface-method
     */
    public AuthzSubject findByAuth(String name, String authDsn) {
        return getSubjectDAO().findByAuth(name, authDsn);
    }
    
    
    /**
     * @ejb:interface-method
     */
    public AuthzSubject findSubjectById(AuthzSubjectValue whoami, Integer id)
        throws PermissionException 
    {
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        // users can see their own entries without requiring special permission
        if(!whoami.getId().equals(id)) {
            pm.check(whoami.getId(), getRootResourceType().getId(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.perm_viewSubject);
        }
        return findSubjectById(id);
    }

    /** 
     * @ejb:interface-method
     */
    public AuthzSubject findSubjectById(Integer id) {
        return getSubjectDAO().findById(id);
    }
    
    /** 
     * @ejb:interface-method
     */
    public AuthzSubject getSubjectById(Integer id) {
        return getSubjectDAO().getById(id);
    }
    
    /** 
     * @ejb:interface-method
     */
    public AuthzSubjectValue findSubjectByName(AuthzSubjectValue whoami,
                                               String name)
        throws PermissionException 
    {
        AuthzSubject sub = getSubjectDAO().findByName(name);
        return sub.getAuthzSubjectValue();
    }

    /** 
     * List all subjects in the system
     * @ejb:interface-method
     * @param excludes the IDs of subjects to exclude from result
     */
    public PageList getAllSubjects(AuthzSubjectValue whoami,
                                   Collection excludes, PageControl pc)
        throws FinderException, PermissionException {
        Collection subjects;
        pc = PageControl.initDefaults(pc, SortAttribute.SUBJECT_NAME);
        
        AuthzSubjectDAO dao = getSubjectDAO();
        // if a user does not have permission to view subjects, 
        // all they can see is their own entry.
        AuthzSubject who = dao.findById(whoami.getId());

        try {
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(whoami.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpViewSubject);
        } catch (PermissionException e) {
            PageList plist = new PageList();

            // return a list with only the one entry.
            plist.add(who.getAuthzSubjectValue());
            plist.setTotalSize(1);
            return plist;
        }

        switch (pc.getSortattribute()) {
        case SortAttribute.SUBJECT_NAME:
            if (who.isRoot())
                subjects = dao.findAllRoot_orderName(excludes,
                                                     pc.isAscending());
            else
                subjects = dao.findAll_orderName(excludes, pc.isAscending());
            break;

        case SortAttribute.FIRST_NAME:
            if (who.isRoot())
                subjects = dao.findAllRoot_orderFirstName(excludes,
                                                          pc.isAscending());
            else
                subjects = dao.findAll_orderFirstName(excludes,
                                                      pc.isAscending());
            break;

        case SortAttribute.LAST_NAME:
            if (who.isRoot())
                subjects = dao.findAllRoot_orderLastName(excludes,
                                                         pc.isAscending());
            else
                subjects = dao.findAll_orderLastName(excludes,
                                                     pc.isAscending());
            break;

        default:
            throw new FinderException("Unrecognized sort attribute: " +
                                      pc.getSortattribute());
        }                
        
        return _subjectPager.seek(subjects, pc.getPagenum(), pc.getPagesize() );
    }

    /** 
     * Get the subjects with the specified ids
     *
     * NOTE: This method returns an empty PageList if a null or
     *       empty array of ids is received.
     * @param ids the subject ids
     * @ejb:interface-method
     */
    public PageList getSubjectsById(AuthzSubjectValue subject,
                                    Integer[] ids,
                                    PageControl pc)
        throws PermissionException {

        // PR7251 - Sometimes and for no good reason, different parts of the UI
        // call this method with an empty ids array. In this case, simply return
        // an empty page list.
        if (ids == null || ids.length == 0) {
            return new PageList();
        }

        // find the requested subjects
        PageList subjects = getSubjectDAO().findById_orderName(ids, pc);

        // check permission unless the list includes only the id of
        // the subject being requested. This is ugly mostly because
        // we're using a list api to possibly look up a single Item
        if(subjects.size() > 0) {
            _log.debug("Checking if Subject: " + subject.getName() +
                      " can list subjects.");
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(subject.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpViewSubject);
        }

        // Need to convert to value objects
        return new PageList(_subjectPager.seek(subjects, PageControl.PAGE_ALL),
                            subjects.getTotalSize());
    }

    /**
     * Find the e-mail of the subject specified by id
     * @param id id of the subject.
     * @return The e-mail address of the subject
     * @ejb:interface-method
     */
    public String getEmailById(Integer id) {
        AuthzSubject subject = getSubjectDAO().findById(id);
        return subject.getEmailAddress();
    }

    /**
     * Find the e-mail of the subject specified by name
     * @param userName Name of the subjects.
     * @return The e-mail address of the subject
     * @ejb:interface-method
     */
    public String getEmailByName(String userName) {
        AuthzSubject subject = getSubjectDAO().findByName(userName);
        return subject.getEmailAddress();
    }

    /**
     * Get the Preferences for a specified user
     * @ejb:interface-method
     */
    public ConfigResponse getUserPrefs(AuthzSubjectValue who, Integer subjId)
        throws PermissionException
    {
        // users can always see their own prefs.
        if(!who.getId().equals(subjId)) { 
            // check that the caller can see users
            PermissionManager pm = PermissionManagerFactory.getInstance();
            pm.check(who.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpViewSubject);
        }

        AuthzSubject targ = getSubjectDAO().findById(subjId);
        Crispo c = targ.getPrefs();
        if (c == null)
            return new ConfigResponse();
        return c.toResponse();
    }

    /**
     * Set the Preferences for a specified user
     * @ejb:interface-method
     */
    public void setUserPrefs(AuthzSubjectValue who, Integer subjId,
                             ConfigResponse prefs) 
        throws PermissionException 
    {
        // check to see if the user attempting the modification
        // is the same as the one being modified
        if(!(who.getId().intValue() == subjId.intValue())) {
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(who.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpModifySubject);
        }

        AuthzSubject targ = getSubjectDAO().findById(subjId);
        
        if (targ.getPrefs() != null)
            CrispoManagerEJBImpl.getOne().update(targ.getPrefs(), prefs);
        else {
            Crispo newPrefs = CrispoManagerEJBImpl.getOne().create(prefs);
            targ.setPrefs(newPrefs);
        }
    }
    
    /**
     * Get the overlord spider subject value. THe overlord is the systems
     * anonymous user and should be used for non-authz operations
     * that require a subject value as one of the params
     * @return the overlord
     * @ejb:interface-method
     */
    public AuthzSubjectValue getOverlord() {
        return getSubjectDAO().findById(AuthzConstants.overlordId)
                              .getAuthzSubjectValue();
    }

    /**
     * @ejb:interface-method
     */
    public AuthzSubject getOverlordPojo() {
        return getSubjectDAO().findById(AuthzConstants.overlordId);
    }

    /**
     * Get the root spider subject value. THe root is the systems
     * unrestricted user which can _log in.
     * @ejb:interface-method
     */
    public AuthzSubjectValue getRoot() {
        return getSubjectDAO().findById(AuthzConstants.rootSubjectId)
                              .getAuthzSubjectValue();
    }
    
    public static AuthzSubjectManagerLocal getOne() {
        try {
            return AuthzSubjectManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() throws CreateException {
        try {
            _subjectPager = Pager.getPager(SUBJECT_PAGER);
        } catch (Exception e) {
            throw new CreateException("Could not create Pager: " + e);
        }
    }
    
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
}
