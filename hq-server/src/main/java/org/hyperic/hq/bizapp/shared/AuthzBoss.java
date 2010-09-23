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
package org.hyperic.hq.bizapp.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for AuthzBoss.
 */
public interface AuthzBoss {
    /**
     * Check if the current logged in user can administer CAM
     * @return true - if user has adminsterCAM op false otherwise
     */
    public boolean hasAdminPermission(int sessionId) throws  SessionTimeoutException,
        SessionNotFoundException;

    /**
     * Return a sorted, paged <code>List</code> of <code>ResourceType</code>
     * objects representing every resource type in the system that the user is
     * allowed to view.
     */
    public List<ResourceType> getAllResourceTypes(Integer sessionId, PageControl pc) throws 
         PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Return the full <code>List</code> of <code>ResourceType</code> objects
     * representing every resource type in the system that the user is allowed
     * to view.
     */
    public List<ResourceType> getAllResourceTypes(Integer sessionId) throws 
        PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Return a sorted, paged <code>List</code> of <code>Operation</code>
     * objects representing every resource type in the system that the user is
     * allowed to view.
     */
    public List<Operation> getAllOperations(Integer sessionId, PageControl pc) throws 
        PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Return the full <code>List</code> of <code>Operation</code> objects
     * representing every resource type in the system that the user is allowed
     * to view.
     */
    public List<Operation> getAllOperations(Integer sessionId) throws  PermissionException,
        SessionTimeoutException, SessionNotFoundException;

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>AuthzSubjectValue</code> objects representing every resource type
     * in the system that the user is allowed to view.
     */
    public PageList<AuthzSubjectValue> getAllSubjects(Integer sessionId, Collection<Integer> excludes, PageControl pc)
        throws  SessionTimeoutException, SessionNotFoundException, PermissionException, NotFoundException;

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>AuthzSubjectValue</code> objects corresponding to the specified id
     * values.
     */
    public PageList<AuthzSubjectValue> getSubjectsById(Integer sessionId, Integer[] ids, PageControl pc)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>AuthzSubjectValue</code> objects matching name as substring
     */
    public PageList<AuthzSubject> getSubjectsByName(Integer sessionId, String name, PageControl pc)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>ResourceGroupValue</code> objects representing every resource type
     * in the system that the user is allowed to view.
     */
    public List<ResourceGroupValue> getAllResourceGroups(Integer sessionId, PageControl pc) throws 
        PermissionException, SessionTimeoutException, SessionNotFoundException;

    /**
     * Return a sorted, paged <code>List</code> of
     * <code>ResourceGroupValue</code> objects corresponding to the specified id
     * values.
     */
    public PageList<ResourceGroupValue> getResourceGroupsById(Integer sessionId, Integer[] ids, PageControl pc)
        throws  PermissionException, SessionTimeoutException, SessionNotFoundException;

    public Map<AppdefEntityID, Resource> findResourcesByIds(Integer sessionId, AppdefEntityID[] entities)
        throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Remove the user identified by the given ids from the subject as well as
     * principal tables.
     */
    public void removeSubject(Integer sessionId, Integer[] ids) throws  
        PermissionException, SessionTimeoutException, SessionNotFoundException, ApplicationException;

    /**
     * Update a subject
     */
    public void updateSubject(Integer sessionId, AuthzSubject target, Boolean active, String dsn, String dept,
                              String email, String first, String last, String phone, String sms, Boolean useHtml)
        throws PermissionException, SessionException;

    /**
     * Create the user identified by the given ids from the subject as well as
     * principal tables.
     */
    public AuthzSubject createSubject(Integer sessionId, String name, boolean active, String dsn, String dept,
                                      String email, String first, String last, String phone, String sms, boolean useHtml)
        throws PermissionException,  SessionException, ApplicationException;

    public AuthzSubject getCurrentSubject(int sessionid) throws SessionException;

    public AuthzSubject getCurrentSubject(String name) throws SessionException, ApplicationException;

    /**
     * Return the <code>AuthzSubject</code> object identified by the given
     * subject id.
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     * @throws PermissionException
     */
    public AuthzSubject findSubjectById(Integer sessionId, Integer subjectId) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException;

    /**
     * Return the <code>AuthzSubject</code> object identified by the given
     * username.
     */
    public AuthzSubject findSubjectByName(Integer sessionId, String subjectName) throws 
        SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * Return the <code>AuthzSubject</code> object identified by the given
     * username. This method should only be used in cases where displaying the
     * user does not require an Authz check. An example of this is when the
     * owner and last modifier need to be displayed, and the user viewing the
     * resource does not have permissions to view other users. See bug #5452 for
     * more information
     */
    public AuthzSubject findSubjectByNameNoAuthz(Integer sessionId, String subjectName) throws 
        SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * Return a ConfigResponse matching the UserPreferences
     * @throws ApplicationException
     * @throws ConfigPropertyException
     * @throws LoginException
     */
    public ConfigResponse getUserPrefs(String username) throws SessionNotFoundException, ApplicationException,
        ConfigPropertyException;

    /**
     * Return a ConfigResponse matching the UserPreferences
     */
    public ConfigResponse getUserPrefs(Integer sessionId, Integer subjectId);

    /**
     * Sets the UserPreferences by sending an event to asynchronously persist them after commit.  To update the prefs
     * synchronously use {@link AuthzSubjectManager}
     */
    public void setUserPrefs(Integer sessionId, Integer subjectId, ConfigResponse prefs) throws ApplicationException,
        SessionTimeoutException, SessionNotFoundException;

    /**
     * Get the email of a user by name
     */
    public String getEmailByName(Integer sessionId, String userName) throws  SessionTimeoutException,
        SessionNotFoundException;

    /**
     * Get the email of a user by id
     */
    public String getEmailById(Integer sessionId, Integer userId) throws  SessionTimeoutException,
        SessionNotFoundException;

}
