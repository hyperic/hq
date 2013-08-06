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

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.UserPreferencesUpdatedEvent;
import org.hyperic.hq.authz.server.session.events.subject.SubjectDeletedZevent;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BizApp's interface to the Authz Subsystem
 * 
 */
@Service
@Transactional
public class AuthzBossImpl implements AuthzBoss {

    private SessionManager sessionManager;

    protected Log log = LogFactory.getLog(AuthzBossImpl.class.getName());
    protected boolean debug = log.isDebugEnabled();

    private AppdefBoss appdefBoss;

    private AuthManager authManager;
    
    private AuthBoss authBoss;

    private AuthzSubjectManager authzSubjectManager;

    private ResourceGroupManager resourceGroupManager;

    private ResourceManager resourceManager;

    private PermissionManager permissionManager;
    
    private ZeventEnqueuer zEventEnqueuer;

    @Autowired
    public AuthzBossImpl(SessionManager sessionManager, AppdefBoss appdefBoss, AuthBoss authBoss, AuthManager authManager,
                         AuthzSubjectManager authzSubjectManager, ResourceGroupManager resourceGroupManager,
                         ResourceManager resourceManager, 
                         PermissionManager permissionManager, ZeventEnqueuer zeventEnqueuer) {
        this.sessionManager = sessionManager;
        this.appdefBoss = appdefBoss;
        this.authManager = authManager;
        this.authBoss = authBoss;
        this.authzSubjectManager = authzSubjectManager;
        this.resourceGroupManager = resourceGroupManager;
        this.resourceManager = resourceManager;
        this.permissionManager = permissionManager;
        this.zEventEnqueuer = zeventEnqueuer;
    }

    /**
     * Check if the current logged in user can administer CAM
     * @return true - if user has adminsterCAM op false otherwise
     * 
     */
    @Transactional(readOnly=true)
    public boolean hasAdminPermission(int sessionId) throws  SessionTimeoutException,
        SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return permissionManager.hasAdminPermission(subject.getId());
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>ResourceTypeValue</code> objects representing every resource type
     * in the system that the user is allowed to view.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<ResourceType> getAllResourceTypes(Integer sessionId, PageControl pc) throws PermissionException, SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return resourceManager.getAllResourceTypes(subject, pc);
    }

    /**
     * Return the full <code>List</code> of <code>ResourceTypeValue</code>
     * objects representing every resource type in the system that the user is
     * allowed to view.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<ResourceType> getAllResourceTypes(Integer sessionId) throws 
        PermissionException, SessionTimeoutException, SessionNotFoundException {
        return getAllResourceTypes(sessionId, null);
    }

    /**
     * Return a sorted, paged <code>List</code> of <code>OperationValue</code>
     * objects representing every resource type in the system that the user is
     * allowed to view.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<Operation> getAllOperations(Integer sessionId, PageControl pc) throws 
        PermissionException, SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return permissionManager.getAllOperations(subject, pc);
    }

    /**
     * Return the full <code>List</code> of <code>OperationValue</code> objects
     * representing every resource type in the system that the user is allowed
     * to view.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<Operation> getAllOperations(Integer sessionId) throws  PermissionException,
        SessionTimeoutException, SessionNotFoundException {
        return getAllOperations(sessionId, null);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>AuthzSubjectValue</code> objects representing every resource type
     * in the system that the user is allowed to view.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AuthzSubjectValue> getAllSubjects(Integer sessionId, Collection<Integer> excludes, PageControl pc)
        throws  SessionTimeoutException, SessionNotFoundException, PermissionException, NotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return authzSubjectManager.getAllSubjects(subject, excludes, pc);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>AuthzSubjectValue</code> objects corresponding to the specified id
     * values.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AuthzSubjectValue> getSubjectsById(Integer sessionId, Integer[] ids, PageControl pc)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return authzSubjectManager.getSubjectsById(subject, ids, pc);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>AuthzSubjectValue</code> objects matching name as substring
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AuthzSubject> getSubjectsByName(Integer sessionId, String name, PageControl pc)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException {
        sessionManager.getSubject(sessionId);
        return authzSubjectManager.findMatchingName(name, pc);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>ResourceGroupValue</code> objects representing every resource type
     * in the system that the user is allowed to view.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<ResourceGroupValue> getAllResourceGroups(Integer sessionId, PageControl pc) throws 
        PermissionException, SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return resourceGroupManager.getAllResourceGroups(subject, pc);
    }

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>ResourceGroupValue</code> objects corresponding to the specified id
     * values.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ResourceGroupValue> getResourceGroupsById(Integer sessionId, Integer[] ids, PageControl pc)
        throws  PermissionException, SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return resourceGroupManager.getResourceGroupsById(subject, ids, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public Map<AppdefEntityID, Resource> findResourcesByIds(Integer sessionId, AppdefEntityID[] entities)
        throws SessionNotFoundException, SessionTimeoutException {
        // get the user
        sessionManager.getSubject(sessionId);
        Map<AppdefEntityID, Resource> appdefMap = new LinkedHashMap<AppdefEntityID, Resource>();

        // cheaper to find the resource first

        for (int i = 0; i < entities.length; i++) {
            Resource res = resourceManager.findResource(entities[i]);
            if (res != null && !res.isInAsyncDeleteState()) {
                try {
                    appdefMap.put(AppdefUtil.newAppdefEntityId(res), res);
                } catch (IllegalArgumentException e) {
                    // Not a valid appdef resource, continue
                }
            }
        }

        return appdefMap;
    }

    /**
     * Remove the user identified by the given ids from the subject as well as
     * principal tables.
     * 
     * 
     */
    public void removeSubject(Integer sessionId, Integer[] ids) throws 
        PermissionException, SessionTimeoutException, SessionNotFoundException, ApplicationException {
        // check for timeout
        AuthzSubject whoami = sessionManager.getSubject(sessionId);
        try {
            for (int i = 0; i < ids.length; i++) {
                AuthzSubject aSubject = findSubjectById(sessionId, ids[i]);
                /*
                 * Note: This has not been finalized. At present, however, the
                 * consensus is that a user should be able to be deleted if they
                 * are logged in. Therefore, this fix may not be needed ...
                 * BUG-4169 - DSE if (isLoggedIn(username)) { throw new
                 * Exception ("User is logged in"); }
                 */

                // Verify that the user is not trying to delete themself.
                if (whoami.getName().equals(aSubject.getName())) {
                    throw new PermissionException("Users are not permitted to remove themselves.");
                }
                final Collection<Role> roles = new ArrayList<Role>(aSubject.getRoles());                
                final Collection<ResourceGroup> ownedGroups = resourceGroupManager.getResourceGroupsByOwnerId(aSubject);                
                
                // reassign ownership of all things authz
                resetResourceOwnership(sessionId, aSubject);
                // reassign ownership of all things appdef
                appdefBoss.resetResourceOwnership(sessionId.intValue(), aSubject);

                // delete in auth
                authManager.deleteUser(whoami, aSubject.getName());

                // remove from authz
                authzSubjectManager.removeSubject(whoami, ids[i]);
                zEventEnqueuer.enqueueEventAfterCommit(new SubjectDeletedZevent(aSubject,
                        roles, ownedGroups));                
            }
        } catch (UpdateException e) {
            throw new ApplicationException("Unable to reset ownership of owned resources: " + e.getMessage());
        } catch (AppdefEntityNotFoundException e) {
            throw new ApplicationException("Unable to reset ownership of owned resources: " + e.getMessage());
        }
    }

    /**
     * Update all the authz resources owned by this user to be owned by the root
     * user. This is done to prevent resources from being orphaned in the UI due
     * to its display restrictions. This method should only get called before a
     * user is about to be deleted
     * @param subject- the user about to be removed
     * 
     */
    private void resetResourceOwnership(Integer sessionId, AuthzSubject currentOwner) throws 
        UpdateException, PermissionException {
        // first look up the resources by owner
        Collection<Resource> resources = resourceManager.findResourceByOwner(currentOwner);
        for (Resource aRes : resources) {
            String resType = aRes.getResourceType().getName();
            if (resType.equals(AuthzConstants.roleResourceTypeName)|| resType.equals(AuthzConstants.subjectResourceTypeName)) {
                resourceManager.setResourceOwner(authzSubjectManager.getOverlordPojo(), aRes, authzSubjectManager
                    .getOverlordPojo());
            }
        }
    }

    /**
     * Update a subject
     * 
     * 
     */
    public void updateSubject(Integer sessionId, AuthzSubject target, Boolean active, String dsn, String dept,
                              String email, String first, String last, String phone, String sms, Boolean useHtml)
        throws PermissionException, SessionException {
        AuthzSubject whoami = sessionManager.getSubject(sessionId);
        authzSubjectManager.updateSubject(whoami, target, active, dsn, dept, email, first, last, phone, sms, useHtml);
    }

    /**
     * Create the user identified by the given ids from the subject as well as
     * principal tables.
     * 
     * 
     */
    public AuthzSubject createSubject(Integer sessionId, String name, boolean active, String dsn, String dept,
                                      String email, String first, String last, String phone, String sms, boolean useHtml)
        throws PermissionException, SessionException, ApplicationException {
        // check for timeout
        AuthzSubject whoami = sessionManager.getSubject(sessionId);

        return authzSubjectManager.createSubject(whoami, name, active, dsn, dept, email, first, last, phone, sms,
            useHtml);
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public AuthzSubject getCurrentSubject(int sessionid) throws SessionException {
        return sessionManager.getSubject(sessionid);
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public AuthzSubject getCurrentSubject(String name) throws SessionException, ApplicationException {
        int sessionId = authBoss.getUnauthSessionId(name);
        return getCurrentSubject(sessionId);
    }

    /**
     * Return the <code>AuthzSubject</code> object identified by the given
     * subject id.
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     * @throws PermissionException
     * 
     * 
     */
    @Transactional(readOnly=true)
    public AuthzSubject findSubjectById(Integer sessionId, Integer subjectId) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException {
        // check for timeout
        AuthzSubject subj = sessionManager.getSubject(sessionId);
        return authzSubjectManager.findSubjectById(subj, subjectId);
    }

    /**
     * Return the <code>AuthzSubject</code> object identified by the given
     * username.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public AuthzSubject findSubjectByName(Integer sessionId, String subjectName) throws 
        SessionTimeoutException, SessionNotFoundException, PermissionException {
        // check for timeout
        AuthzSubject subj = sessionManager.getSubject(sessionId);
        return authzSubjectManager.findSubjectByName(subj, subjectName);
    }

    /**
     * Return the <code>AuthzSubject</code> object identified by the given
     * username. This method should only be used in cases where displaying the
     * user does not require an Authz check. An example of this is when the
     * owner and last modifier need to be displayed, and the user viewing the
     * resource does not have permissions to view other users. See bug #5452 for
     * more information
     * 
     */
    @Transactional(readOnly=true)
    public AuthzSubject findSubjectByNameNoAuthz(Integer sessionId, String subjectName) throws 
        SessionTimeoutException, SessionNotFoundException, PermissionException {
        // check for timeout
        sessionManager.authenticate(sessionId.intValue());
        return authzSubjectManager.findSubjectByName(subjectName);
    }

    /**
     * Return a ConfigResponse matching the UserPreferences
     * @throws ApplicationException
     * @throws ConfigPropertyException
     * @throws LoginException
     * 
     */
    @Transactional(readOnly=true)
    public ConfigResponse getUserPrefs(String username) throws SessionNotFoundException, ApplicationException,
        ConfigPropertyException {
        int sessionId = authBoss.getUnauthSessionId(username);
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return getUserPrefs(new Integer(sessionId), subject.getId());
    }

    /**
     * Return a ConfigResponse matching the UserPreferences
     * 
     */
    @Transactional(readOnly=true)
    public ConfigResponse getUserPrefs(Integer sessionId, Integer subjectId) {
        try {
            AuthzSubject who = sessionManager.getSubject(sessionId);
            return authzSubjectManager.getUserPrefs(who, subjectId);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Transactional(readOnly=true)
    public void setUserPrefs(Integer sessionId, Integer subjectId, ConfigResponse prefs) throws ApplicationException,
        SessionTimeoutException, SessionNotFoundException {
        if (log.isDebugEnabled()) {
            log.debug("setting preferences for sessionid=" + sessionId +
                ", subjId=" + subjectId);
        }
        AuthzSubject who = sessionManager.getSubject(sessionId); 
        zEventEnqueuer.enqueueEventAfterCommit(new UserPreferencesUpdatedEvent(who.getId(),subjectId,prefs));
    }
   
    /**
     * Get the email of a user by name
     * 
     */
    @Transactional(readOnly=true)
    public String getEmailByName(Integer sessionId, String userName) throws  SessionTimeoutException,
        SessionNotFoundException {
        sessionManager.authenticate(sessionId.intValue());
        return authzSubjectManager.getEmailByName(userName);
    }

    /**
     * Get the email of a user by id
     * 
     */
    @Transactional(readOnly=true)
    public String getEmailById(Integer sessionId, Integer userId) throws  SessionTimeoutException,
        SessionNotFoundException {
        sessionManager.authenticate(sessionId.intValue());
        return authzSubjectManager.getEmailById(userId);
    }
}
