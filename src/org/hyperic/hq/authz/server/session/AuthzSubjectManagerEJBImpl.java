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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectLocalHome;
import org.hyperic.hq.authz.shared.AuthzSubjectPK;
import org.hyperic.hq.authz.shared.AuthzSubjectUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.jdbc.BlobColumn;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 */
public class AuthzSubjectManagerEJBImpl
    extends AuthzSession implements SessionBean {

    protected static final Log log
        = LogFactory.getLog(AuthzSubjectManagerEJBImpl.class.getName());

    private static final String SUBJECT_PAGER
        = "org.hyperic.hq.authz.server.session.PagerProcessor_subject";
    private Pager subjectPager = null;

    // keep a reference to the overlord
    private AuthzSubjectValue overlord = null;
    private AuthzSubjectValue root = null;

    private static int DBTYPE = -1;

    /** Creates a new instance of AuthzSubjectManagerEJBImpl */
    public AuthzSubjectManagerEJBImpl() {}

    /** Create a subject.
     * @param whoami The current running user.
     * @param subject The subject to be created.
     * @return Value-object for the new Subject.
     * @exception PermissionException whoami may not perform createSubject on the rootResource ResourceType.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public AuthzSubjectPK createSubject(AuthzSubjectValue whoami,
                                        AuthzSubjectValue subject)
        throws CreateException, NamingException, FinderException,
               PermissionException {
        AuthzSubjectLocalHome subjectLome = getSubjectHome();

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(), getRootResourceType(),
                 AuthzConstants.rootResourceId,
                 AuthzConstants.subjectOpCreateSubject);

        // Make sure there's not already a system subject with that name
        try {
            subjectLome.findByAuth(subject.getName(),
                                   AuthzConstants.overlordDsn);
            throw new CreateException("A system user already exists with " +
                                      subject.getName());
        } catch (FinderException e) {
            // continue, we expected not to have found an existing user
        }
        
        AuthzSubjectLocal whoamiLocal =
            subjectLome.findByAuth(whoami.getName(), whoami.getAuthDsn());

        AuthzSubjectLocal subjectLocal =
            subjectLome.create(whoamiLocal, subject);

        try {
            this.insertUserPrefs(subject.getId());
        } catch (Exception e) {
            rollback();
            // Just in case...
            throw new SystemException(e);
        }

        return (AuthzSubjectPK)subjectLocal.getPrimaryKey();
    }


    /** Write the specified entity out to permanent storage.
     * @param whoami The current running user.
     * @param role The subject to save.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami may not perform modifySubject on this subject.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void saveSubject(AuthzSubjectValue whoami,
                            AuthzSubjectValue subject)
        throws NamingException, FinderException, PermissionException {
        AuthzSubjectLocal subjectLocal = this.lookupSubject(subject);

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        // check to see if the user attempting the modification
        // is the same as the one being modified
        if(!(whoami.getId() == subject.getId())) {
            pm.check(whoami.getId(), getRootResourceType().getId(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.perm_viewSubject);
        }
        // Fix for Bug: 5531
        // Root user can not be disabled
        if(subject.getId().equals(AuthzConstants.rootSubjectId)) {
            subject.setActive(true);
        }
        subjectLocal.setAuthzSubjectValue(subject);
        // remove from cache
        VOCache.getInstance().removeSubject(subject.getName());
    }

    /**
     * Check if a subject can modify users  
     * @param subject
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void checkModifyUsers(AuthzSubjectValue caller) 
        throws SystemException, PermissionException, FinderException {
        try {
            PermissionManager pm = PermissionManagerFactory.getInstance();
            pm.check(caller.getId(),
                     getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpModifySubject);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /** Delete the specified subject.
     * @param whoami The current running user.
     * @param subject The ID of the subject to delete.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeSubject(AuthzSubjectValue whoami,
                              Integer subject)
        throws NamingException, FinderException, 
               RemoveException, PermissionException {
        AuthzSubjectLocalHome lome = getSubjectHome();

        AuthzSubjectPK currentUserPK = whoami.getPrimaryKey();
        AuthzSubjectPK userToDeletePK = new AuthzSubjectPK(subject);
        AuthzSubjectLocal userToDelete = null;

        // no removing of the root user!
        if (subject.equals(AuthzConstants.rootSubjectId)) {
            throw new RemoveException("Root user can not be deleted");
        }

        if ( currentUserPK.equals(userToDeletePK) ) {
            // XXX Should we do anything special for the "suicide" case?
            // Perhaps a log message?
            lome.remove(currentUserPK);
            return;
        }

        userToDelete = lome.findByPrimaryKey(userToDeletePK);
        String name = userToDelete.getName();

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(), getRootResourceType().getId(),
                 AuthzConstants.rootResourceId,
                 AuthzConstants.perm_removeSubject);

        deleteUserPrefs(subject);
        lome.remove(userToDeletePK);
        // remove from cache
        VOCache.getInstance().removeSubject(name);
    }

    /** Get the Resource entity associated with this Subject.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ResourceValue getSubjectResource(AuthzSubjectValue subject)
        throws NamingException, FinderException {
        AuthzSubjectLocal local =
            getSubjectHome().findByPrimaryKey(
                subject.getPrimaryKey());
        return local.getResource().getResourceValue();
    }

    /** Find a subject by its id
     * @exception PermissionException whoami does not have the viewSubject
     * permission in any of its roles.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public AuthzSubjectValue findSubjectById(AuthzSubjectValue whoami,
        Integer id)
        throws NamingException, FinderException, PermissionException {

        AuthzSubjectLocal sub = getSubjectHome().findById(id);
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        // users can see their own entries without requiring special permission
        if(!whoami.getId().equals(id)) {
            pm.check(whoami.getId(), getRootResourceType().getId(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.perm_viewSubject);
        }
        return sub.getAuthzSubjectValue();
    }

    /** 
     * Find a subject by its name
     * @exception PermissionException whoami does not have the viewSubject
     * permission in any of its roles.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public AuthzSubjectValue findSubjectByName(AuthzSubjectValue whoami,
        String name)
        throws FinderException, PermissionException {
        // look for the subject in the cache
        AuthzSubjectValue vo;
        try {
            AuthzSubjectLocal sub =
                getSubjectHome().findByName(name);
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            // skip this check if the user is requesting his own entity,
            // as happens in the case of login
            if(!whoami.getName().equals(name)) {
                pm.check(whoami.getId(), getRootResourceType().getId(),
                         AuthzConstants.rootResourceId,
                         AuthzConstants.perm_viewSubject);
            }
            vo = VOCache.getInstance().getAuthzSubject(name);
            if(vo == null) {
                // not in cache. Put it in there
                vo = getSubjectHome().findByName(name).getAuthzSubjectValue();
                VOCache cache = VOCache.getInstance();
                synchronized(cache.getSubjectLock()) {
                    cache.put(name, vo);
                }
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        return vo;
    }

    /** 
     * List all subjects in the system
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList getAllSubjects(AuthzSubjectValue whoami,
                               PageControl pc)
        throws NamingException, FinderException, PermissionException {
        Collection subjects;
        pc = PageControl.initDefaults(pc, SortAttribute.SUBJECT_NAME);
        int attr = pc.getSortattribute();
        AuthzSubjectLocalHome subLocalHome = getSubjectHome();
        PageList plist = new PageList();
        int totalSize = 0;
        // if a user does not have permission to view subjects, 
        // all they can see is their own entry.
        AuthzSubjectLocal whoEJB = lookupSubject(whoami);
        try {
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(whoami.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpViewSubject);
        } catch (PermissionException e) {
            // return a list with only the one entry.
            plist.add(whoEJB.getAuthzSubjectValue());
            plist.setTotalSize(1);
            return plist;
        }

        switch (attr) {
        case SortAttribute.SUBJECT_NAME:
            if (pc.isDescending())
                if (whoEJB.isRoot()) 
                    subjects = subLocalHome.findAllRoot_orderName_desc();
                else
                    subjects = subLocalHome.findAll_orderName_desc();
            else
                if (whoEJB.isRoot()) 
                    subjects = subLocalHome.findAllRoot_orderName_asc();
                else
                    subjects = subLocalHome.findAll_orderName_asc();
            break;

        case SortAttribute.FIRST_NAME:
            if (pc.isDescending())
                if (whoEJB.isRoot()) 
                    subjects = subLocalHome.findAllRoot_orderFirstName_desc();
                else
                    subjects = subLocalHome.findAll_orderFirstName_desc();
            else
                if (whoEJB.isRoot()) 
                    subjects = subLocalHome.findAllRoot_orderFirstName_asc();
                else
                    subjects = subLocalHome.findAll_orderFirstName_asc();
            break;

        case SortAttribute.LAST_NAME:
            if (pc.isDescending())
                if (whoEJB.isRoot()) 
                    subjects = subLocalHome.findAllRoot_orderLastName_desc();
                else
                    subjects = subLocalHome.findAll_orderLastName_desc();
            else
                if (whoEJB.isRoot()) 
                    subjects = subLocalHome.findAllRoot_orderLastName_asc();
                else
                    subjects = subLocalHome.findAll_orderLastName_asc();
            break;

        default:
            throw new FinderException("Unrecognized sort attribute: " + attr);
        }                
        
        plist.setTotalSize(((List)subjects).size());
        return subjectPager.seek(subjects, pc.getPagenum(), pc.getPagesize() );
    }

    /** 
     * Get the subjects with the specified ids
     *
     * NOTE: This method returns an empty PageList if a null or
     *       empty array of ids is received.
     * @param ids the subject ids
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList getSubjectsById(AuthzSubjectValue subject,
                                    Integer[] ids,
                                    PageControl pc)
        throws NamingException, FinderException, PermissionException {

        // PR7251 - Sometimes and for no good reason, different parts of the UI
        // call this method with an empty ids array. In this case, simply return
        // an empty page list.
        if (ids == null || ids.length == 0) {
            return new PageList();
        }

        PageControl allPc = new PageControl(pc);
        // get all subjects, sorted but not paged
        allPc.setSortattribute(pc.getSortattribute());
        allPc.setSortorder(pc.getSortorder());
        List all = getAllSubjects(subject, allPc);
        
        // build an index of ids 
        int numToFind = 0;
        HashMap index = new HashMap();
        for (int i=0; i<ids.length; i++) {
            Integer id = ids[i];
            index.put(id, id);
            numToFind++;
        }

        // check permission unless the list includes only the id of
        // the subject being requested. This is ugly mostly because
        // we're using a list api to possibly look up a single Item
        if(!((index.size() == 1) && index.containsKey(subject.getId()))) {
            log.debug("Checking if Subject: " + subject.getName() +
                " can list subjects.");
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(subject.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpViewSubject);
        }

        // find the requested subjects
        List subjects = new ArrayList(ids.length);
        Iterator i = all.iterator();
        while (i.hasNext()) {
            AuthzSubjectValue s = (AuthzSubjectValue) i.next();
            Integer id = (Integer) index.get(s.getId());
            if (id != null) {
                subjects.add(s);
            }
            if (subjects.size() == numToFind) {
                break;
            }
        }

        // return the appropriate page for the found subjects

        PageList plist = new PageList();
        plist = subjectPager.seek(subjects, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(subjects.size());        
        
        return plist;
    }

    /**
     * Find the e-mail of the subject specified by id
     * @param id id of the subject.
     * @return The e-mail address of the subject
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public String getEmailById(Integer id)
        throws NamingException, FinderException {
        AuthzSubjectLocalHome subjectLome = getSubjectHome();
        AuthzSubjectLocal subject =
            subjectLome.findByPrimaryKey(new AuthzSubjectPK(id));
        return subject.getEmailAddress();
    }

    /**
     * Find the e-mail of the subject specified by name
     * @param name Name of the subjects.
     * @return The e-mail address of the subject
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public String getEmailByName(String userName)
        throws NamingException, FinderException {
        AuthzSubjectLocalHome subjectLome = getSubjectHome();
        AuthzSubjectLocal subject =
            subjectLome.findByName(userName);
        return subject.getEmailAddress();
    }

    /**
     * Get the Preferences for a specified user
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ConfigResponse getUserPrefs(AuthzSubjectValue who, Integer subjId)
        throws NamingException, FinderException, PermissionException,
               EncodingException {
        // users can always see their own prefs.
        if(!who.getId().equals(subjId)) { 
            // check that the caller can see users
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(who.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpViewSubject);
        }

        byte[] bytes = selectUserPrefs(subjId);
        if(bytes == null) {
            return new ConfigResponse(); 
        } else {
            return ConfigResponse.decode(bytes);
        }
    }

    /**
     * Set the Preferences for a specified user
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void setUserPrefs(AuthzSubjectValue who, Integer subjId,
                             ConfigResponse prefs) 
        throws NamingException, EncodingException, FinderException,
               PermissionException {

        // check to see if the user attempting the modification
        // is the same as the one being modified
        if(!(who.getId().intValue() == subjId.intValue())) {
            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(who.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpModifySubject);
        }

        updateUserPrefs(subjId, prefs.encode());
    }

    private static final String SQL_PREFS_INSERT
        = "INSERT INTO EAM_USER_CONFIG_RESP "
        + "(ID, SUBJECT_ID, PREF_RESPONSE) "
        + "VALUES (?,?,NULL)";
    private void insertUserPrefs(Integer subjId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = this.getDBConn();
            ps = conn.prepareStatement(SQL_PREFS_INSERT);
            ps.setInt(1, subjId.intValue());
            ps.setInt(2, subjId.intValue());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, null);
        }
    }
    
    private static final String USERPREF_TABLE    = "EAM_USER_CONFIG_RESP";
    private static final String USERPREF_COL_ID   = "ID";
    private static final String USERPREF_COL_BLOB = "PREF_RESPONSE";
    private void updateUserPrefs(Integer subjId, byte[] prefsByteArr) {
        BlobColumn userPrefs;
        try {
            userPrefs = DBUtil.getBlobColumn(DBTYPE, DATASOURCE, 
                                             USERPREF_TABLE,
                                             USERPREF_COL_ID,
                                             USERPREF_COL_BLOB);
            userPrefs.setId(subjId);
            userPrefs.setBlobData(prefsByteArr);
            userPrefs.update();

        } catch (SQLException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }
    
    private static final String SQL_PREFS_SELECT 
        = "SELECT PREF_RESPONSE FROM EAM_USER_CONFIG_RESP WHERE ID=?";
    private byte[] selectUserPrefs (Integer subjId) {

        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;
        byte[]            data = null;

        try {
            conn = this.getDBConn();
            ps = conn.prepareStatement(SQL_PREFS_SELECT);
            ps.setInt(1, subjId.intValue());
            rs = ps.executeQuery();
            if (rs.next()) {
                data = DBUtil.getBlobColumn(rs, 1);
            }
        } catch (SQLException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, rs);
        }
        return data;
    }

    private static final String SQL_PREFS_DELETE 
        = "DELETE FROM EAM_USER_CONFIG_RESP WHERE ID=?";
    private void deleteUserPrefs(Integer subjId) {

        Connection        conn = null;
        PreparedStatement ps   = null;

        try {
            conn = getDBConn();
            ps = conn.prepareStatement(SQL_PREFS_DELETE);
            ps.setInt(1,subjId.intValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(log, conn, ps, null);
        }
    }

    /**
     * Get the overlord spider subject value. THe overlord is the systems
     * anonymous user and should be used for non-authz operations
     * that require a subject value as one of the params
     * @return the overlord
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public AuthzSubjectValue getOverlord() 
        throws NamingException, FinderException {
        if (overlord == null) {
            overlord = AuthzSubjectUtil.getLocalHome()
                            .findByPrimaryKey(
                                new AuthzSubjectPK(
                                    new Integer(
                                        AuthzConstants.overlordId)))
                                            .getAuthzSubjectValue();
        }
        return overlord;
    }

    /**
     * Get the root spider subject value. THe root is the systems
     * unrestricted user which can log in.
     * @return the overlord
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public AuthzSubjectValue getRoot() 
        throws NamingException, FinderException {
        if (root == null) {
            root = AuthzSubjectUtil.getLocalHome()
                            .findByPrimaryKey(
                                new AuthzSubjectPK( 
                                        AuthzConstants.rootSubjectId))
                                            .getAuthzSubjectValue();
        }
        return root;
    }

    public void ejbCreate() throws CreateException {
        try {
            subjectPager = Pager.getPager(SUBJECT_PAGER);
        } catch (Exception e) {
            throw new CreateException("Could not create Pager: " + e);
        }
        if (DBTYPE == -1) {
            Connection conn = null;
            try {
                conn = getDBConn();
                DBTYPE = DBUtil.getDBType(conn);
            } catch (Exception e) {
                throw new CreateException("Error determining DBType: " + e);
            } finally {
                DBUtil.closeConnection(log, conn);
            }
        }
    }
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
}
