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

package org.hyperic.hq.bizapp.server.session;

import java.util.Collection;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.bizapp.shared.AuthzBossLocal;
import org.hyperic.hq.bizapp.shared.AuthzBossUtil;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/** 
 * The BizApp's interface to the Authz Subsystem
 *
 * @ejb:bean name="AuthzBoss"
 *      jndi-name="ejb/bizapp/AuthzBoss"
 *      local-jndi-name="LocalAuthzBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class AuthzBossEJBImpl extends BizappSessionEJB 
    implements SessionBean {

    private SessionManager manager    = SessionManager.getInstance();

    protected Log log = LogFactory.getLog(AuthzBossEJBImpl.class.getName());
    protected boolean debug = log.isDebugEnabled();

    public AuthzBossEJBImpl() {}

    /**
     * Check if the current logged in user can administer CAM
     * @return true - if user has adminsterCAM op false otherwise
     * @ejb:interface-method
     */
    public boolean hasAdminPermission(int sessionId)
        throws NamingException, FinderException, 
               SessionTimeoutException, SessionNotFoundException {
        AuthzSubjectValue subject = manager.getSubject(sessionId);
        PermissionManager pm = PermissionManagerFactory.getInstance();
        return pm.hasAdminPermission(subject);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>ResourceTypeValue</code> objects representing every
     * resource type in the system that the user is allowed to view.
     *
     * @ejb:interface-method
     */
    public List getAllResourceTypes(Integer sessionId, PageControl pc)
        throws NamingException, CreateException, FinderException,
               PermissionException, SessionTimeoutException, 
               SessionNotFoundException {
        AuthzSubjectValue subject = manager.getSubject(sessionId.intValue());
        return getResourceManager().getAllResourceTypes(subject, pc);
    }

    /**
     * Return the full <code>List</code> of
     * <code>ResourceTypeValue</code> objects representing every
     * resource type in the system that the user is allowed to view.
     *
     * @ejb:interface-method
     */
    public List getAllResourceTypes(Integer sessionId)
        throws NamingException, CreateException, FinderException,
               PermissionException, SessionTimeoutException, 
               SessionNotFoundException {
        return getAllResourceTypes(sessionId, null);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>OperationValue</code> objects representing every
     * resource type in the system that the user is allowed to view.
     *
     * @ejb:interface-method
     */
    public List getAllOperations(Integer sessionId, PageControl pc)
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException {
        AuthzSubjectValue subject = manager.getSubject(sessionId.intValue());
        PermissionManager pm = PermissionManagerFactory.getInstance();
        return pm.getAllOperations(subject, pc);
    }

    /**
     * Return the full <code>List</code> of
     * <code>OperationValue</code> objects representing every
     * resource type in the system that the user is allowed to view.
     *
     * @ejb:interface-method
     */
    public List getAllOperations(Integer sessionId)
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException {
        return getAllOperations(sessionId, null);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>AuthzSubjectValue</code> objects representing every
     * resource type in the system that the user is allowed to view.
     *
     * @ejb:interface-method
     */
    public PageList getAllSubjects(Integer sessionId, Collection excludes,
                                   PageControl pc)
        throws FinderException, SessionTimeoutException,
               SessionNotFoundException, PermissionException {
        AuthzSubjectValue subject = manager.getSubject(sessionId.intValue());
        return getAuthzSubjectManager().getAllSubjects(subject, excludes, pc);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>AuthzSubjectValue</code> objects corresponding to the specified
     * id values.
     * 
     * @ejb:interface-method
     */
    public PageList getSubjectsById(Integer sessionId, Integer[] ids,
                                    PageControl pc)
        throws PermissionException, SessionTimeoutException,
               SessionNotFoundException {
        AuthzSubjectValue subject = manager.getSubject(sessionId.intValue());
        return getAuthzSubjectManager().getSubjectsById(subject, ids, pc);
    }

    /**
     * Return the full <code>List</code> of
     * <code>AuthzSubjectValue</code> objects representing every
     * resource type in the system that the user is allowed to view.
     *
     * @ejb:interface-method
     */
    public PageList getAllSubjects(Integer sessionId)
        throws FinderException, SessionTimeoutException,
               SessionNotFoundException, PermissionException {
        return getAllSubjects(sessionId, null, null);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>ResourceGroupValue</code> objects representing every
     * resource type in the system that the user is allowed to view.
     *
     * @ejb:interface-method
     */
    public List getAllResourceGroups(Integer sessionId, PageControl pc)
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException {
        AuthzSubjectValue subject = manager.getSubject(sessionId.intValue());
        return getResourceGroupManager().getAllResourceGroups(subject, pc);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>ResourceGroupValue</code> objects corresponding to the
     * specified id values.
     *
     * @ejb:interface-method
     */
    public PageList getResourceGroupsById(Integer sessionId, Integer[] ids,
                                          PageControl pc)
        throws NamingException, FinderException, PermissionException,
               SessionTimeoutException, SessionNotFoundException {
        AuthzSubjectValue subject = manager.getSubject(sessionId.intValue());
        return getResourceGroupManager()
            .getResourceGroupsById(subject, ids, pc);
    }

    /**
     * Remove resources by appdef id
     *
     * @ejb:interface-method
     */
    public void removeResources(AppdefEntityID[] ids) {
        // should do some permission checks here.
        // This is meant to be called from other bosses
        // that need to remove resources as a last step
        // to clean the repo.  i.e., for model use case
        // see AppdefBossEJBImpl.removePlatform();
        getResourceManager().removeResources(ids);
    }

    /**
     * Remove the user identified by the given ids from the subject as well 
     * as principal tables.
     *
     * @ejb:interface-method
     */
    public void removeSubject(Integer sessionId, Integer[] ids)
        throws FinderException, RemoveException, PermissionException,
               SessionTimeoutException, SessionNotFoundException {
        // check for timeout
        AuthzSubjectValue whoami = manager.getSubject(sessionId.intValue());        
        try {
            AuthzSubjectManagerLocal mgr = getAuthzSubjectManager();
            for (int i = 0; i < ids.length; i++) {
                AuthzSubjectValue aSubject = findSubject(sessionId, ids[i]); 
                /* Note: This has not been finalized. At present, however,
                    the consensus is that a user should be able to be deleted
                    if they are logged in. Therefore, this fix may not be
                    needed ...  BUG-4169 - DSE
                if (isLoggedIn(username)) {
                    throw new RemoveException ("User is logged in");
                } 
                */

                // Verify that the user is not trying to delete themself.
                if (whoami.getName().equals(aSubject.getName())) {
                    throw new PermissionException(
                        "Users are not permitted to remove themselves.");
                }
                // reassign ownership of all things appdef
                getAppdefBoss().resetResourceOwnership(
                    sessionId.intValue(), aSubject);
                // reassign ownership of all things authz
                resetResourceOwnership(sessionId.intValue(), aSubject);
                
                // delete in auth
                getAuthManager().deleteUser(whoami, aSubject.getName());
                
                // remove from authz
                mgr.removeSubject(whoami, ids[i]);
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (UpdateException e) {
            rollback();
            throw new RemoveException(
                "Unable to reset ownership of owned resources: " 
                + e.getMessage());   
        } catch (AppdefEntityNotFoundException e) {
            rollback();
            throw new RemoveException(
                "Unable to reset ownership of owned resources: "
                + e.getMessage());
        }
    }
    
    /**
     * Update all the authz resources owned by this user to be owned
     * by the root user. This is done to prevent resources from being
     * orphaned in the UI due to its display restrictions. This method
     * should only get called before a user is about to be deleted
     * @param subject- the user about to be removed
     * 
     */
    private void resetResourceOwnership(int sessionId,
                                        AuthzSubjectValue currentOwner) 
        throws FinderException, UpdateException, PermissionException {
        // first look up the resources by owner
        ResourceValue[] resources
            = getResourceManager().findResourceByOwner(currentOwner);
        AuthzSubjectValue root = getAuthzSubjectManager().getRoot();
        for(int i = 0; i < resources.length; i++) {
            ResourceValue aRes = resources[i];
            String resType = aRes.getResourceTypeValue().getName();    
            if(resType.equals(AuthzConstants.roleResourceTypeName)) {
                getResourceManager().setResourceOwner(
                    this.getOverlord(), aRes, root);
            }
        }
    }
                            
    /**
     * Save a subject
     *
     * @ejb:interface-method
     */
    public void saveSubject(Integer sessionId, AuthzSubjectValue user)
        throws NamingException, FinderException, RemoveException,
               PermissionException, SessionTimeoutException,
               SessionNotFoundException {
        // check for timeout
        AuthzSubjectValue whoami = manager.getSubject(sessionId.intValue());        
        getAuthzSubjectManager().saveSubject(whoami, user);        
    }
    
    /**
     * Create the user identified by the given ids from the subject as well 
     * as principal tables.
     *
     * @ejb:interface-method
     */
    public AuthzSubjectValue createSubject(Integer sessionId,
                                           AuthzSubjectValue user)
        throws NamingException, CreateException, FinderException,
               RemoveException, PermissionException, SessionTimeoutException,
               SessionNotFoundException {
        // check for timeout
        AuthzSubjectValue whoami = manager.getSubject(sessionId.intValue());        

        AuthzSubjectManagerLocal subjMan = getAuthzSubjectManager();
        AuthzSubject subject = subjMan.createSubject(whoami, user);
        return subject.getAuthzSubjectValue();
        
    }

    /**
     * @ejb:interface-method
     */
    public AuthzSubject getCurrentSubject(int sessionid) 
        throws SessionException
    {
        return manager.getSubjectPojo(sessionid);
    }
    
    /**
     * @ejb:interface-method
     */
    public AuthzSubject getCurrentSubject(String name)
        throws SessionException, ApplicationException
    {
        int sessionId = getAuthManager().getUnauthSessionId(name);
        return getCurrentSubject(sessionId);
    }
    
    /**
     * Return the <code>AuthzSubjectValue</code> object identified by
     * the given subject id.
     *
     * @ejb:interface-method
     */
    public AuthzSubjectValue findSubject(Integer sessionId,
                                         Integer subjectId)
        throws NamingException, FinderException, SessionTimeoutException,
               SessionNotFoundException, PermissionException {
        // check for timeout
        AuthzSubjectValue subj = manager.getSubject(sessionId.intValue());
        return getAuthzSubjectManager().findSubjectById(subj, subjectId);
    }

    /**
     * Return the <code>AuthzSubjectValue</code> object identified by
     * the given username.
     *
     * @ejb:interface-method
     */
    public AuthzSubjectValue findSubjectByName(Integer sessionId,
                                               String subjectName)
        throws NamingException, FinderException, SessionTimeoutException,
               SessionNotFoundException, PermissionException {
        // check for timeout
        AuthzSubjectValue subj = manager.getSubject(sessionId.intValue());
        return getAuthzSubjectManager().findSubjectByName(subj, subjectName);
    }

    /**
     * Return the <code>AuthzSubjectValue</code> object identified by
     * the given username. This method should only be used in cases
     * where displaying the user does not require an Authz check. An
     * example of this is when the owner and last modifyer need to 
     * be displayed, and the user viewing the resource does not 
     * have permissions to view other users.
     * See bug #5452 for more information
     * @ejb:interface-method
     */
    public AuthzSubjectValue findSubjectByNameNoAuthz(Integer sessionId,
                                                      String subjectName)
        throws NamingException, FinderException, SessionTimeoutException,
               SessionNotFoundException, PermissionException {
        // check for timeout
        AuthzSubjectValue subj = manager.getSubject(sessionId.intValue());
        AuthzSubjectValue overlord = getOverlord();
        return getAuthzSubjectManager().findSubjectByName(overlord, subjectName);
    }

    /**
     * Return a ConfigResponse matching the UserPreferences
     * @throws ApplicationException
     * @throws ConfigPropertyException
     * @throws LoginException
     * @ejb:interface-method
     */
    public ConfigResponse getUserPrefs(String username)
        throws SessionNotFoundException, ApplicationException,
               ConfigPropertyException {
        int sessionId = getAuthManager().getUnauthSessionId(username);
        AuthzSubjectValue subject = manager.getSubject(sessionId);
        return getUserPrefs(new Integer(sessionId), subject.getId());
    }
    
    /**
     * Return a ConfigResponse matching the UserPreferences
     * @ejb:interface-method
     */
    public ConfigResponse getUserPrefs(Integer sessionId, Integer subjectId) {
        try {
            AuthzSubjectValue who = manager.getSubject(sessionId.intValue());
            return getAuthzSubjectManager().getUserPrefs(who, subjectId);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Set the UserPreferences 
     * @ejb:interface-method
     */
    public void setUserPrefs(Integer sessionId, Integer subjectId,
                             ConfigResponse prefs)
        throws ApplicationException, SessionTimeoutException,
               SessionNotFoundException 
    {
        // log.debug("Invoking setUserPrefs" +
        //         " in AuthzBossEJBImpl " +
        //         " for " + subjectId + " at "+System.currentTimeMillis() +
        //         " prefs = " + prefs);
        AuthzSubjectValue who = manager.getSubject(sessionId.intValue());
        getAuthzSubjectManager().setUserPrefs(who, subjectId, prefs);
        prefs = getUserPrefs(sessionId, subjectId);
        // log.debug("LOADED PREFS=" + prefs);
    }

    /**
     * Get the email of a user by name
     * @ejb:interface-method
     */
    public String getEmailByName(Integer sessionId, String userName) 
        throws FinderException, SessionTimeoutException,
               SessionNotFoundException {
        AuthzSubjectValue who = manager.getSubject(sessionId.intValue());
        return getAuthzSubjectManager().getEmailByName(userName);
    }

    /**
     * Get the email of a user by id
     * @ejb:interface-method
     */
    public String getEmailById(Integer sessionId, Integer userId) 
        throws FinderException, SessionTimeoutException,
               SessionNotFoundException {
        AuthzSubjectValue who = manager.getSubject(sessionId.intValue());
        return getAuthzSubjectManager().getEmailById(userId);
    }
    
    public static AuthzBossLocal getOne() {
        try {
            return AuthzBossUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    /** @ejb:create-method */
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
}
