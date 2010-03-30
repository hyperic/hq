/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.RoleCalendar;
import org.hyperic.hq.authz.server.session.RoleCalendarType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzDuplicateNameException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.RoleManagerLocal;
import org.hyperic.hq.authz.shared.RoleManagerUtil;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.authz.values.OwnedRoleValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Calendar;
import org.hyperic.hq.common.server.session.CalendarManagerEJBImpl;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

/**
 * Use this session bean to manipulate Roles and Subjects associated
 * with them.
 * All arguments and return values are value-objects.
 *
 * @ejb:bean name="RoleManager"
 *      jndi-name="ejb/authz/RoleManager"
 *      local-jndi-name="LocalRoleManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="Required"
 */
public class RoleManagerEJBImpl extends AuthzSession implements SessionBean {

    private final Log _log = LogFactory.getLog(RoleManagerEJBImpl.class);
    private Pager subjectPager = null;
    private Pager rolePager = null;
    private Pager groupPager = null;
    private Pager ownedRolePager = null;
    private final String SUBJECT_PAGER =
        "org.hyperic.hq.authz.server.session.PagerProcessor_subject";
    private final String ROLE_PAGER =
        "org.hyperic.hq.authz.server.session.PagerProcessor_role";
    private final String OWNEDROLE_PAGER =
        "org.hyperic.hq.authz.server.session.PagerProcessor_ownedRole";
    private final String GROUP_PAGER =
        "org.hyperic.hq.authz.server.session.PagerProcessor_resourceGroup";

    /**
     * Validate that a role is ok to be added or updated
     * @param aRole
     * @throws AuthzDuplicateNameException
     */
    private void validateRole(RoleValue aRole)
        throws AuthzDuplicateNameException 
    {
         Role role = getRoleDAO().findByName(aRole.getName());
         if (role != null) {
             throw new AuthzDuplicateNameException("A role named: " +
                                                   aRole.getName() +
                                                   " already exists");
         }

    }
    
    private Role lookupRole(RoleValue role) {
        return lookupRole(role.getId());
    }

    private Role lookupRole(Integer id) {
        return getRoleDAO().findById(id);
    }

    private ResourceGroup lookupGroup(Integer id) {
        return getResourceGroupDAO().findById(id);
    }

    /**
     * @ejb:interface-method
     */
    public boolean isRootRoleMember(AuthzSubject subject)  {
        return getRootRoleIfMember(subject) != null;
    }

    private Role getRootRoleIfMember(AuthzSubject subject) {
        // Look up the root role
        Role rootRole = getRoleDAO().findById(AuthzConstants.rootRoleId);
        // Look up the calling subject
        if (rootRole.getSubjects().contains(subject))
            return rootRole;
        
        return null;
    }

    /** 
     * Filter a collection of roleLocal objects to only include those viewable
     * by the specified user
     * @throws FinderException SQL error looking up roles scope
     */
    private Collection filterViewableRoles(AuthzSubject who, Collection roles) 
        throws PermissionException, FinderException {
        return filterViewableRoles(who, roles, null);
    }
    /**
     * Filter a collection of roleLocal object to only include those viewable
     * by the specific user and not in the list of ids passed in as excluded
     * @param who - the user
     * @param roles - the list of role locals
     * @param excludeIds - role ids which should be excluded from the return list
     * 
     * @throws FinderException SQL error looking up roles scope
     */                                                         
    private Collection filterViewableRoles(AuthzSubject who,
                                           Collection roles, 
                                           Integer[] excludeIds)         
        throws PermissionException, FinderException {
        try {
            PermissionManager pm = PermissionManagerFactory.getInstance();
            ResourceTypeDAO dao =
                new ResourceTypeDAO(DAOFactory.getDAOFactory());
            pm.check(who.getId(),
                     dao.findByName(AuthzConstants.roleResourceTypeName),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.roleOpViewRole);
        } catch (PermissionException e) {
            return new ArrayList(0);
        }
        
        List excludeList = null;
        boolean hasExclude = (excludeIds != null && excludeIds.length > 0);
        if (hasExclude)
            excludeList = java.util.Arrays.asList(excludeIds);
        
        // Throw out the excludes
        for (Iterator i = roles.iterator(); i.hasNext();) {
            Object role = i.next();
            Integer pk = ((Role) role).getId();
            if (hasExclude && excludeList.contains(pk)) {
                i.remove();
            }
        }
        return roles;
    }

    /**
     * Create a role.
     * @param whoami The current running user.
     * @param role The to be created.
     * @param operations Operations to associate with the new role. Use null
     * if you want to associate operations later.
     * @param subjectIds Ids of subjects to add to the new role. Use null to
     * add subjects later.
     * @param groupIds Ids of resource groups to add to the new role. Use 
     * null to add subjects later.
     * @return OwnedRoleValue for the role.
     * @throws CreateException Unable to create the specified entity.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami may not perform createResource on
     * the covalentAuthzRole ResourceType.
     * @ejb:interface-method
     */
    public Integer createOwnedRole(AuthzSubject whoami,
                                   RoleValue role,
                                   Operation[] operations,
                                   Integer[] subjectIds,
                                   Integer[] groupIds)
        throws FinderException, AuthzDuplicateNameException, PermissionException 
    {
        RoleDAO dao = getRoleDAO();
        validateRole(role);

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), getRootResourceType(),
                 AuthzConstants.rootResourceId,AuthzConstants.roleOpCreateRole);

        Role roleLocal = dao.create(whoami, role);

        // Associated operations
        roleLocal.setOperations(toPojos(operations));

        if (subjectIds != null) {
            HashSet sLocals = new HashSet(subjectIds.length);
            for (int si = 0; si < subjectIds.length; si++) {
                sLocals.add(lookupSubject(subjectIds[si]));
            }
            // Associated subjects 
            roleLocal.setSubjects(sLocals);
        }

        if (groupIds != null) {
            HashSet gLocals = new HashSet(groupIds.length);
            for (int gi=0; gi<groupIds.length; gi++) {
                gLocals.add(lookupGroup(groupIds[gi]));
            }
            // Associated resource groups
            roleLocal.setResourceGroups(gLocals);
        }

        AuthzStartupListener.getRoleCreateCallback().roleCreated(roleLocal);
        return roleLocal.getId();
    }

    /**
     * Delete the specified role.
     * @param whoami The current running user.
     * @param role The role to delete.
     * @throws RemoveException Unable to delete the specified entity.
     * @ejb:interface-method
     */
    public void removeRole(AuthzSubject whoami, Integer rolePk)
        throws RemoveException, PermissionException {
        // Don't delete the super user role
        if (rolePk.equals(AuthzConstants.rootRoleId)) {
            throw new RemoveException("Superuser role cannot be removed");
        }

        RoleDAO dao = getRoleDAO();
        Role role = dao.findById(rolePk);

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), role.getResource().getResourceType(), 
                 role.getId(), AuthzConstants.roleOpRemoveRole);

        AuthzStartupListener.getRoleRemoveCallback().roleRemoved(role);
        for (Iterator i = role.getCalendars().iterator(); i.hasNext();) {
            RoleCalendar c = (RoleCalendar)i.next();
            removeCalendar(c);
        }
        dao.remove(role);
    }

    /**
     * Write the specified entity out to permanent storage.
     * @param whoami The current running user.
     * @param role The role to save.
     * @throws PermissionException whoami may not perform modifyRole on 
     * this role.
     * @ejb:interface-method
     */
    public void saveRole(AuthzSubject whoami, RoleValue role)
        throws AuthzDuplicateNameException, PermissionException {
        Role roleLocal = lookupRole(role);
        if(!roleLocal.getName().equals(role.getName())) {
            // Name has changed... check it
            validateRole(role);
        }

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(), roleLocal.getResource().getResourceType(),
                 roleLocal.getId(), AuthzConstants.roleOpModifyRole);
        roleLocal.setRoleValue(role);
    }

    /**
     * Change the owner of the role.
     * @param whoami The current running user.
     * @param id The ID of the role to change
     * @param ownerVal The new owner of the role..
     * @throws PermissionException whoami may not perform modifyRole 
     * on this role.
     * @ejb:interface-method
     */
    public void changeOwner(AuthzSubject whoami, Integer id, AuthzSubject owner)
        throws PermissionException {
        Role roleLocal = lookupRole(id);

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(), roleLocal.getResource().getResourceType(),
                 roleLocal.getId(), AuthzConstants.roleOpModifyRole);

        roleLocal.getResource().setOwner(owner);
    }

    /**
     * Associate operations with this role.
     * @param whoami The current running user.
     * @param role The role.
     * @param operations The operations to associate with the role.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami may not perform addOperation on
     * this role.
     * @ejb:interface-method
     */
    public void addOperations(AuthzSubject whoami, Role role,
                              Operation[] operations)
        throws PermissionException {
        Set opLocals = toPojos(operations);

//        roleLocal.setWhoami(lookupSubject(whoami));
        role.getOperations().addAll(opLocals);
    }

    /**
     * Disassociate all operations from this role.
     * @param whoami The current running user.
     * @param role The role.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami may not perform removeOperation 
     * on this role.
     * @ejb:interface-method
     */
    public void removeAllOperations(AuthzSubject whoami, Role role)
        throws PermissionException {
//        roleLocal.setWhoami(lookupSubject(whoami));
        role.getOperations().clear();
    }

    /**
     * Set the operations for this role.
     * To get the operations call getOperations() on the value-object.
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param operations Operations to associate with this role.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform
     * setOperations on this role.
     * @ejb:interface-method
     */
    public void setOperations(AuthzSubject whoami, Integer id,
                              Operation[] operations)
        throws PermissionException {
        if (operations != null) {
            Role roleLocal = lookupRole(id);

            PermissionManager pm = PermissionManagerFactory.getInstance(); 
            pm.check(whoami.getId(),
                     roleLocal.getResource().getResourceType(),
                     roleLocal.getId(), AuthzConstants.roleOpModifyRole);

            Set opLocals = toPojos(operations);
            roleLocal.setOperations(opLocals);
        } 
    }

    /**
     * Associate ResourceGroups with this role.
     * @param whoami The current running user.
     * @param role This role.
     * @param gids The ids of the groups to associate with this role.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform
     * addResourceGroup on this role.
     * @ejb:interface-method
     */
    public void addResourceGroups(AuthzSubject whoami, Integer rid,
                                  Integer[] gids)
        throws PermissionException {
        Role roleLocal = getRoleDAO().findById(rid);
        for (int i = 0; i < gids.length; i++) {
            ResourceGroup group = lookupGroup(gids[i]);
            group.addRole(roleLocal);
        }
    }

    /**
     * Associate ResourceGroup with list of roles.
     * @param whoami The current running user.
     * @param roles The roles.
     * @param ids The id of the group to associate with the roles.
     * @throws PermissionException whoami is not allowed to perform 
     * addResourceGroup on this role.
     * @throws FinderException SQL error looking up roles scope
     * @ejb:interface-method
     */
    public void addResourceGroupRoles(AuthzSubject whoami, Integer gid,
                                      Integer[] ids)
        throws PermissionException, FinderException {
        ResourceGroup group = lookupGroup(gid);
        for (int i = 0; i < ids.length; i++) {
            Role roleLocal = lookupRole(ids[i]);
            group.addRole(roleLocal);
        }
    }

    /**
     * Disassociate ResourceGroups from this role.
     * @param whoami The current running user.
     * @param id This role.
     * @param gids The ids of the groups to disassociate.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform 
     * modifyRole on this role.
     * @ejb:interface-method
     */
    public void removeResourceGroups(AuthzSubject whoami, Integer id,
                                     Integer[] gids)
        throws PermissionException {
        Role roleLocal = lookupRole(id);

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), roleLocal.getResource().getResourceType(),
                 roleLocal.getId(), AuthzConstants.roleOpModifyRole);

        for (int i=0; i<gids.length; i++) {
            roleLocal.removeResourceGroup(lookupGroup(gids[i]));
        }
    }

    /**
     * Disassociate roles from this ResourceGroup.
     * @param whoami The current running user.
     * @param role This role.
     * @param ids The ids of the groups to disassociate.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform 
     * modifyRole on this role.
     * @ejb:interface-method
     */
    public void removeResourceGroupRoles(AuthzSubject whoami,
                                         Integer gid, Integer[] ids)
        throws PermissionException {        
        PermissionManager pm = PermissionManagerFactory.getInstance();
            
        ResourceGroup group = lookupGroup(gid);
        for (int i = 0; i < ids.length; i++) {
            Role roleLocal = lookupRole(ids[i]);

            pm.check(whoami.getId(),
                     roleLocal.getResource().getResourceType(),
                     roleLocal.getId(),
                     AuthzConstants.roleOpModifyRole);

            roleLocal.removeResourceGroup(group);
        }
    }

    /**
     * Disassociate all ResourceGroups of this role from this role.
     * @param whoami The current running user.
     * @param role This role.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws NamingException   
     * @throws PermissionException whoami is not allowed to perform
     * modifyRole on this role.
     * @ejb:interface-method
     */
    public void removeAllResourceGroups(AuthzSubject whoami, Role role)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), role.getResource().getResourceType(),
                 role.getId(), AuthzConstants.roleOpModifyRole);
        role.clearResourceGroups();
    }

    /**
      * Get the # of roles within HQ inventory
      * @ejb:interface-method
      */
     public Number getRoleCount() {
         return new Integer(getRoleDAO().size());
     }
     
     /**
      * Get the # of subjects within HQ inventory
      * @ejb:interface-method
      */
     public Number getSubjectCount() {
         AuthzSubjectDAO dao = new AuthzSubjectDAO(DAOFactory.getDAOFactory());
         return new Integer(dao.size());
     }
     
    /**
     * Get a Role by id
     *
     * @ejb:interface-method
     */
    public Role getRoleById(int id) {
        return getRoleDAO().get(new Integer(id));
    }

    /**
     * @ejb:interface-method
     */
    public Role findRoleById(int id){
        return lookupRole(new Integer(id));
    }

    /**
     * @ejb:interface-method
     */
    public Role findRoleByName(String name) {
        return getRoleDAO().findByName(name);
    }
    
    /**
     * Create a calendar under a role for a specific type.  Calendars created
     * in this manner are tied directly to the role and should not be used
     * by other roles.
     * @throws PermissionException if user is not allowed to modify role
     * @ejb:interface-method
     */
    public RoleCalendar createCalendar(AuthzSubject whoami, Role r,
                                       String calendarName, 
                                       RoleCalendarType type)
        throws PermissionException 
    {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), r.getResource().getResourceType(),
                 r.getId(), AuthzConstants.roleOpModifyRole);

        Calendar cal = 
            CalendarManagerEJBImpl.getOne().createCalendar(calendarName);
        RoleCalendar res = new RoleCalendar(r, cal, type);
        r.addCalendar(res);
        return res;
    }
    
    /**
     * @ejb:interface-method
     */
    public boolean removeCalendar(RoleCalendar c) {
        boolean res = c.getRole().removeCalendar(c);
        new RoleCalendarDAO(DAOFactory.getDAOFactory()).remove(c);
        CalendarManagerEJBImpl.getOne().remove(c.getCalendar());
        return res; 
    }
    
    /**
     * Find the owned role that has the given ID.
     * @param id The ID of the role you're looking for.
     * @return The owned value-object of the role of the given ID.
     * @throws FinderException Unable to find a given or dependent entities.
     * @ejb:interface-method
     */
    public OwnedRoleValue findOwnedRoleById(AuthzSubject whoami,
                                            Integer id)
        throws PermissionException {
        RoleDAO dao = getRoleDAO();
        Role local = dao.findById(id);

        int numSubjects = dao.size(local.getSubjects());

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), local.getResource().getResourceType(),
                 id, AuthzConstants.roleOpViewRole);

        OwnedRoleValue value = new OwnedRoleValue(local);
        value.setMemberCount(numSubjects);

        return value;
    }

    /**
     * Get operations
     * For a given role id, find the resource types and permissions
     * which are supported by it
     * @param subject
     * @param roleId
     * @return list - values are lists of operation
     * @ejb:interface-method
     */ 
    public List getRoleOperations(AuthzSubject subject, Integer roleId)
        throws PermissionException {
        // find the role by id
        Role role = getRoleDAO().findById(roleId);
        // now get the operations
        return new ArrayList(role.getOperations());
    }

    /**
     * @return a list of {@link Role}s
     * @ejb:interface-method
     */
    public Collection getAllRoles() {
        return getRoleDAO().findAll();
    }
    
    private Collection getAllRoles(AuthzSubject subject, int sort,
                                   boolean asc) {
        switch (sort) {
        default:
        case SortAttribute.ROLE_NAME:
            return getRoleDAO().findAll_orderName(asc);
        }
    }
    
    /**
     * List all Roles in the system
     * @param pc Paging information for the request
     * @return List a list of RoleValues
     * @ejb:interface-method
     */
    public List getAllRoles(AuthzSubject subject, PageControl pc) 
        throws FinderException {
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        Collection roles = getAllRoles(subject, pc.getSortattribute(),
                                       pc.isAscending());

        return rolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * List all OwnedRoles in the system
     * @param subject
     * @param pc Paging and sorting information.
     * @return List a list of OwnedRoleValues
     * @ejb:interface-method
     */
    public List getAllOwnedRoles(AuthzSubject subject, PageControl pc) {
        Collection roles = getRoleDAO().findAll();
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        return ownedRolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * List all Roles in the system, except system roles.
     * @return List a list of OwnedRoleValues that are not system roles
     * @throws FinderException if sort attribute is unrecognized
     * @ejb:interface-method
     */
    public PageList getAllNonSystemOwnedRoles(AuthzSubject subject,
                                              Integer[] excludeIds,
                                              PageControl pc)
        throws PermissionException, FinderException {
        Collection roles;
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        int attr = pc.getSortattribute();
        switch (attr) {

        case SortAttribute.ROLE_NAME:
            roles = getRoleDAO().findBySystem_orderName(false,
                                                        !pc.isDescending());
            break;

        default:
            throw new FinderException("Unrecognized sort attribute: " + attr);
        }

        // 6729 - if caller is a member of the root role, show it
        // 5345 - allow access to the root role by the root user so it can 
        // be used by others
        Role rootRole = getRootRoleIfMember(subject);
        if (rootRole != null) {
            ArrayList newList = new ArrayList();
            newList.add(rootRole);
            newList.addAll(roles);
            roles = newList;
        }
        
        roles = filterViewableRoles(subject, roles, excludeIds);

        PageList plist =
            ownedRolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(roles.size());
        return plist;
    }

    /** Get the roles with the specified ids
     * @param subject
     * @param ids the role ids
     * @param pc Paging information for the request
     * @throws FinderException
     * @throws PermissionException
     * @ejb:interface-method
     *
     */
    public PageList getRolesById(AuthzSubject whoami, Integer[] ids,
                                 PageControl pc)
        throws PermissionException, FinderException {

        List roles = getRolesByIds(whoami, ids, pc);

        PageList plist = rolePager.seek(roles, pc.getPagenum(),
                                        pc.getPagesize());
        plist.setTotalSize(roles.size());

        return plist;
    }

    private List getRolesByIds(AuthzSubject whoami, Integer[] ids,
                               PageControl pc)
        throws PermissionException, FinderException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(),
                 AuthzConstants.roleResourceTypeName,
                 AuthzConstants.rootResourceId,
                 AuthzConstants.roleOpViewRole);

        Collection all = getAllRoles(whoami, pc.getSortattribute(),
                                     pc.isAscending());

        // build an index of ids
        HashSet index = new HashSet();
        for (int i=0; i<ids.length; i++) {
            Integer id = ids[i];
            index.add(id);
        }
        int numToFind = index.size();

        // find the requested roles
        List roles = new ArrayList(ids.length);
        Iterator i = all.iterator();
        while (i.hasNext() && roles.size() < numToFind) {
            Role r = (Role) i.next();
            if (index.contains(r.getId())) {
                roles.add(r);
            }
        }
        return roles;
    }

    /**
     * Associate roles with this subject.
     * @param whoami The current running user.
     * @param subject The subject.
     * @param roles The roles to associate with the subject.
     * @throws PermissionException whoami may not perform addRole on this 
     * subject.
     * @ejb:interface-method
     */
    public void addRoles(AuthzSubject whoami, AuthzSubject subject,
                         Integer[] roles)
        throws PermissionException  {
        for (int i = 0; i < roles.length; i++) {
            subject.addRole(lookupRole(roles[i]));
        }
    }

    /**
     * Disassociate roles from this subject.
     * @param whoami The current running user.
     * @param subject The subject.
     * @param roles The subjects to disassociate.
     * @throws PermissionException whoami may not perform removeRole on
     * this subject.
     * @ejb:interface-method
     */
    public void removeRoles(AuthzSubject whoami,
                            AuthzSubject subject, Integer[] roles)
        throws PermissionException, FinderException {
        Collection roleLocals = getRolesByIds(whoami, roles,
                                              PageControl.PAGE_ALL);
        
        RoleRemoveFromSubjectCallback callback =
            AuthzStartupListener.getRoleRemoveFromSubjectCallback();
        
        Iterator it = roleLocals.iterator();
        while (it.hasNext()) {
            Role role = (Role) it.next();
            callback.roleRemovedFromSubject(role, subject);
            subject.removeRole(role);
        }
    }

    /**
     * Get the roles for a subject
     * @param whoami 
     * @param subject
     * @param pc Paging and sorting information.
     * @return Set of Roles
     * @ejb:interface-method
     */
    public List getRoles(AuthzSubject subjectValue, PageControl pc)
        throws PermissionException {
        Collection roles = subjectValue.getRoles();
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        return rolePager.seek(roles, pc.getPagenum(), pc.getPagesize()); 
    }

    /**
     * Get the owned roles for a subject.
     * @param whoami 
     * @param subject
     * @param pc Paging and sorting information.
     * @return Set of Roles
     * @ejb:interface-method
     */
    public List getOwnedRoles(AuthzSubject subject, PageControl pc) 
        throws PermissionException {
        Collection roles = subject.getRoles();
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        return ownedRolePager.seek(roles, pc.getPagenum(), pc.getPagesize()); 
    }
    /**
     * Get the owned roles for a subject, except system roles.
     * @param callerSubjectValue is the subject of caller.
     * @param intendedSubjectValue is the subject of intended subject.
     * @param pc The PageControl object for paging results.
     * @return List a list of OwnedRoleValues that are not system roles
     * @ejb:interface-method
     * @throws CreateException indicating ejb creation / container failure.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException caller is not allowed to perform listRoles
     * on this role.
     * @throws FinderException SQL error looking up roles scope
     */
    public PageList getNonSystemOwnedRoles(AuthzSubject callerSubjectValue,
                                           AuthzSubject intendedSubjectValue,
                                           PageControl pc)
        throws PermissionException, FinderException {
        return getNonSystemOwnedRoles(callerSubjectValue, intendedSubjectValue,
                                      null, pc);
    }                  
                  
    /**
     * Get the owned roles for a subject, except system roles.
     * @param callerSubjectValue is the subject of caller.
     * @param intendedSubjectValue is the subject of intended subject.
     * @param pc The PageControl object for paging results.
     * @return List a list of OwnedRoleValues that are not system roles
     * @ejb:interface-method
     * @throws CreateException indicating ejb creation / container failure.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException caller is not allowed to perform listRoles
     * on this role.
     * @throws FinderException SQL error looking up roles scope
     */
    public PageList getNonSystemOwnedRoles(AuthzSubject callerSubjectValue,
                                           AuthzSubject intendedSubjectValue, 
                                           Integer[] excludeIds,
                                           PageControl pc)
       throws PermissionException, FinderException {

        Collection viewableRoles; // used for filtering

        // Fetch all roles presently assigned to the assignee
        Collection roles;

        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);

        switch (pc.getSortattribute()) {
        case SortAttribute.ROLE_NAME:
            roles = getRoleDAO()
                .findBySystemAndSubject_orderName(false,
                                                  intendedSubjectValue.getId(),
                                                  pc.isAscending());
            break;
        case SortAttribute.ROLE_MEMBER_CNT:
            roles = getRoleDAO()
                .findBySystemAndSubject_orderMember(false,
                                                    intendedSubjectValue.getId(),
                                                    pc.isAscending());
            break;
        default:
            throw new IllegalArgumentException("Invalid sort parameter");
        }

        if (isRootRoleMember(intendedSubjectValue)) {
            ArrayList roleList = new ArrayList(roles.size() + 1);

            Role rootRole = getRoleDAO().findById(AuthzConstants.rootRoleId);
            
            // We need to insert into the right place
            boolean done = false;
            for (Iterator it = roles.iterator(); it.hasNext(); ) {
                Role role = (Role) it.next();
                if (!done) {
                    if (pc.getSortattribute() == SortAttribute.ROLE_NAME) {
                        if ((pc.isAscending() &&
                             role.getName().compareTo(rootRole.getName()) > 0)||
                            (pc.isDescending() &&
                             role.getName().compareTo(rootRole.getName()) < 0)){ 
                            roleList.add(rootRole);
                            done = true;
                        }
                    }
                    else if (pc.getSortattribute() ==
                             SortAttribute.ROLE_MEMBER_CNT) {
                        if ((pc.isAscending() && role.getSubjects().size() >
                                rootRole.getSubjects().size()) ||
                            (pc.isDescending() && role.getSubjects().size() <
                                rootRole.getSubjects().size())) {
                            roleList.add(rootRole);
                            done = true;
                        }
                    }
                }
                roleList.add(role);
            }
            
            if (!done) {
                roleList.add(rootRole);
            }
            
            roles = roleList;
        }

        // Filter out only those roles that the caller is able to see.
        viewableRoles = filterViewableRoles(callerSubjectValue, roles, 
                                            excludeIds);

        return ownedRolePager.seek(viewableRoles, pc.getPagenum(),
                                   pc.getPagesize());
    }

    /** List the roles that this subject is not in and that are not
     * one of the specified roles.
     * @param whoami The current running user.
     * @param system If true, then only system roles are returned.
     *  If false, then only non-system roles are returned.
     * @param subjectId The id of the subject.
     * @return List of roles.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform
     * listRoles on this role.
     * @throws FinderException 
     * @ejb:interface-method
     */
    public PageList getAvailableRoles(AuthzSubject whoami,
                                      boolean system,
                                      Integer subjectId,
                                      Integer[] roleIds,
                                      PageControl pc) 
        throws PermissionException, FinderException {
        Collection foundRoles;
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        int attr = pc.getSortattribute();
        switch (attr) {
    
        case SortAttribute.ROLE_NAME:
            foundRoles =
                getRoleDAO().findBySystemAndAvailableForSubject_orderName(
                    system, whoami.getId(), !pc.isDescending());
            break;
    
        default:
            throw new FinderException("Unrecognized sort attribute: " + attr);
        }
    
        HashSet index = new HashSet();
        if (roleIds != null)
            index.addAll(Arrays.asList(roleIds));
    
        Collection roles = new ArrayList();
        Iterator i = foundRoles.iterator();
        while (i.hasNext()) {
            Role r = (Role) i.next();
            if (!index.contains(r.getId())) {
                roles.add(r);
            }
        }
    
        // AUTHZ Check
        // filter the viewable roles
        roles = filterViewableRoles(whoami, roles);
        
        PageList plist = new PageList();
        plist = rolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(roles.size());
        // 6729 - if caller is a member of the root role, show it
        // 5345 - allow access to the root role by the root user so it can
        // be used by others
        if (isRootRoleMember(whoami) && pc.getPagenum() == 0 && 
            !index.contains(AuthzConstants.rootRoleId)) {
            Role role = getRoleDAO()
                    .findAvailableRoleForSubject(AuthzConstants.rootRoleId,
                                                 subjectId);
            if (role == null) {
                return plist;
            }
            OwnedRoleValue rootRoleValue = role.getOwnedRoleValue();
            PageList newList = new PageList();
            newList.add(rootRoleValue);
            newList.addAll(plist);
            newList.setTotalSize(plist.getTotalSize() + 1);
            return newList;
        }
        return plist;
    }

    /** List the roles that this subject is not in and that are not
     * one of the specified roles.
     * @param whoami The current running user.
     * @param system If true, then only system roles are returned.
     *  If false, then only non-system roles are returned.
     * @param groupId The id of the subject.
     * @return List of roles.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform
     * listRoles on this role.
     * @throws FinderException if the sort attribute was not recognized
     * @ejb:interface-method
     */
    public PageList getAvailableGroupRoles(AuthzSubject whoami,
                                           Integer groupId,
                                           Integer[] roleIds,
                                           PageControl pc) 
        throws PermissionException, FinderException {
        Collection foundRoles;
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        int attr = pc.getSortattribute();
        RoleDAO dao = getRoleDAO();
        switch (attr) {
        case SortAttribute.ROLE_NAME:
            foundRoles = dao.findAvailableForGroup(false, groupId);
            break;
        default:
            throw new FinderException("Unrecognized sort attribute: " + attr);
        }

        _log.debug("Found " + foundRoles.size() + " available roles for group "
                   + groupId + " before permission checking");
        
        HashSet index = new HashSet();
        if (roleIds != null)
            index.addAll(Arrays.asList(roleIds));

        // Grep out the specified roles
        ArrayList roles = new ArrayList();
        Iterator i = foundRoles.iterator();
        while (i.hasNext()) {
            Role r = (Role) i.next();
            if (!index.contains(r.getId())) {
                roles.add(r);
            }
        }

        _log.debug("Found " + roles.size() + " available roles for group " +
            groupId + " after exclusions");
  
        // AUTHZ Check - filter the viewable roles
        roles = (ArrayList) filterViewableRoles(whoami, roles);

        if (isRootRoleMember(whoami) && pc.getPagenum() == 0 && 
            !index.contains(AuthzConstants.rootRoleId)) {
            foundRoles = dao.findAvailableForGroup(true, groupId);
            for (Iterator it = foundRoles.iterator(); it.hasNext(); ) {
                Role role = (Role) it.next();
                if (role.getId().equals(AuthzConstants.rootRoleId)) {
                    roles.add(role);
                }
            }
        }

        if (pc.isDescending()) {
            Collections.reverse(roles);
        }

        _log.debug("Found " + roles.size() + " available roles for group " +
            groupId + " after permission checking");
  

        PageList plist = rolePager.seek(roles, pc.getPagenum(),
                                        pc.getPagesize());
        plist.setTotalSize(roles.size());

        return plist;
    }
    
    /**
     * Get the resource groups applicable to a given role.
     * 
     * @ejb:interface-method
     */
    public Collection getResourceGroupsByRole(AuthzSubject subject,
                                              Role role)
        throws PermissionException, FinderException {
        
        ResourceGroupDAO dao = getResourceGroupDAO();
        
        Collection groups = 
            dao.findByRoleIdAndSystem_orderName(role.getId(), false, true);
        
        // now get viewable group pks
        return filterViewableGroups(subject, groups);

    }
    
    /**
     * Get the resource groups applicable to a given role
     * @ejb:interface-method
     */
    public PageList getResourceGroupsByRoleIdAndSystem(AuthzSubject subject, 
                                                       Integer roleId, 
                                                       boolean system,
                                                       PageControl pc)
        throws PermissionException, FinderException 
    {
        // first find the role by its id
        getRoleDAO().findById(roleId);
        
        // now check to make sure the user can list resource groups
        Collection groups;
        pc = PageControl.initDefaults(pc, SortAttribute.RESGROUP_NAME);
        int attr = pc.getSortattribute();
        ResourceGroupDAO dao = getResourceGroupDAO();
        switch (attr) {
        case SortAttribute.RESGROUP_NAME:
            groups =
                dao.findByRoleIdAndSystem_orderName(roleId, system,
                                                    pc.isAscending());
            break;

        default:
            throw new FinderException("Unrecognized sort attribute: " + attr);
        }
        
        // now get viewable group pks
        groups = filterViewableGroups(subject, groups);

        PageList plist = groupPager.seek(groups, pc);
        plist.setTotalSize(groups.size());

        return plist;
    }
    
    /**
     * Return the roles of a group
     * @throws PermissionException 
     * 
     * @ejb:interface-method
     */
    public PageList getResourceGroupRoles(AuthzSubject whoami,
                                          Integer groupId, PageControl pc)
        throws PermissionException {
        ResourceGroup resGrp = getResourceGroupDAO().findById(groupId);

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), AuthzConstants.authzGroup, resGrp.getId(),
                 AuthzConstants.perm_viewResourceGroup);

        Collection roles = resGrp.getRoles();
        
        TreeMap map = new TreeMap();
        for (Iterator it = roles.iterator(); it.hasNext();) {
			Role role = (Role) it.next();
			int attr = pc.getSortattribute();
			switch (attr) {
			case SortAttribute.ROLE_NAME:
			default:
				map.put(role.getName(), role);
			}
		}
        
        ArrayList list = new ArrayList(map.values());
        
        if (pc.isDescending())
            Collections.reverse(list);
            
        PageList plist =
            rolePager.seek(list, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(roles.size());

        return plist;
    }

    /**
     * Filter a collection of groupLocal objects to only include those viewable
     * by the specified user
     */
    private Collection filterViewableGroups(AuthzSubject who, Collection groups)
        throws PermissionException, FinderException
    {
        // finally scope down to only the ones the user can see
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List viewable = pm.findOperationScopeBySubject(who,
           AuthzConstants.groupOpViewResourceGroup,
           AuthzConstants.groupResourceTypeName);
        
        for(Iterator i = groups.iterator(); i.hasNext();) {
            ResourceGroup resGrp = (ResourceGroup) i.next();
            
            if (!viewable.contains(resGrp.getId())) {
                i.remove();
            }
        }
        return groups;
    }

    /**
     * List the groups not in this role and not one of the specified groups.
     * 
     * @param whoami The current running user.
     * @param roleId The id of the role.
     * @return List of groups in this role.
     * @throws PermissionException whoami is not allowed to perform
     *                listGroups on this role.
     * @throws FinderException 
     * @ejb:interface-method
     */
    public PageList getAvailableResourceGroups(AuthzSubject whoami,
                                               Integer roleId,
                                               Integer[] groupIds,
                                               PageControl pc) 
        throws PermissionException, FinderException {
        RoleDAO rlDao = getRoleDAO();
        Role role = rlDao.findById(roleId);
        Collection noRoles;
        Collection otherRoles;
        pc = PageControl.initDefaults(pc, SortAttribute.RESGROUP_NAME);
        int attr = pc.getSortattribute();
        ResourceGroupDAO rgDao = getResourceGroupDAO();
        switch (attr) {
        case SortAttribute.RESGROUP_NAME:
            noRoles = rgDao.findWithNoRoles_orderName(pc.isAscending());
            otherRoles = rgDao.findByNotRoleId_orderName(role.getId(),
                                                         pc.isAscending());
            break;

        default:
            throw new FinderException("Unrecognized sort attribute: " + attr);
        }

        // FIXME- merging these two sorted lists probably causes the
        // final list to not be sorted correctly. fix this by
        // combining the two finders into one!
        // FIX for 6924 - dont include duplicate groups
        for (Iterator i = otherRoles.iterator(); i.hasNext();) {
            ResourceGroup groupEJB = (ResourceGroup) i.next();
            if(!noRoles.contains(groupEJB)) {
                noRoles.add(groupEJB);
            }
        }

        // build an index of groupIds
        int numToFind = (groupIds == null) ? 0 : groupIds.length;
        HashSet index = new HashSet();
        for (int i = 0; i < numToFind; i++) {
            index.add(groupIds[i]);
        }
        
        // Add the groups that the role already owns
        Collection belongs = rgDao.findByRoleIdAndSystem_orderName(roleId,
                                                                   false, true);
        for (Iterator it = belongs.iterator(); it.hasNext(); ) {
            ResourceGroup s = (ResourceGroup) it.next();
            index.add(s.getId());
        }
        
        // grep out the specified groups
        Collection groups = new ArrayList(noRoles.size());
        Iterator i = noRoles.iterator();
        while (i.hasNext()) {
            ResourceGroup s = (ResourceGroup) i.next();
            if (!index.contains(s.getId()))
                groups.add(s);
        }

        // AUTHZ Check
        // finally scope down to only the ones the user can see
        groups = filterViewableGroups(whoami, groups);

        PageList plist =
            groupPager.seek(groups, pc.getPagenum(), pc.getPagesize());
        
        plist.setTotalSize(groups.size());

        return plist;
    }
    
    /** List the subjects in this role.
     * @param whoami The current running user.
     * @param roleId The id of the role.
     * @return List of subjects in this role.
     * @throws PermissionException whoami is not allowed to perform 
     * listSubjects on this role.
     * @throws FinderException if the sort attribute is not recognized
     * @ejb:interface-method
     *
     */
    public PageList getSubjects(AuthzSubject whoami, Integer roleId,
                                PageControl pc) 
        throws PermissionException, FinderException {
        Role roleLocal = getRoleDAO().get(roleId);
        
        if (roleLocal == null) {
            return new PageList();
        }
        
        // check if this user is a member of this role
        boolean roleHasUser = roleLocal.getSubjects().contains(whoami);
        // check whether the user can see subjects other than himself
        try {
            PermissionManager pm = PermissionManagerFactory.getInstance();
            pm.check(whoami.getId(), getRootResourceType(),
                     AuthzConstants.rootResourceId,
                     AuthzConstants.subjectOpViewSubject);
        } catch (PermissionException e) {
            // if the user does not have permission to view subjects
            // but he is in the role, return a collection with only one
            // item... himself.
            if(roleHasUser) {
                PageList subjects = new PageList();
                subjects.add(whoami);
                subjects.setTotalSize(1);
                return subjects;
            }
            // otherwise return an empty list
            // fixes 5628 - user viewing role lacking view subjects
            // causes permissionexception
            return new PageList();    
        }
        Collection subjects;
        pc = PageControl.initDefaults(pc, SortAttribute.SUBJECT_NAME);
        AuthzSubjectDAO dao = new AuthzSubjectDAO(DAOFactory.getDAOFactory());
        switch (pc.getSortattribute()) {
        case SortAttribute.SUBJECT_NAME:
            subjects = dao.findByRoleId_orderName(roleLocal.getId(),
                                                  pc.isAscending());
            break;
        default:
            throw new FinderException("Unrecognized sort attribute: " +
                                      pc.getSortattribute());
        }

        PageList plist = new PageList();
        plist = subjectPager.seek(subjects, pc.getPagenum(), pc.getPagesize());        
        plist.setTotalSize(subjects.size());

        return plist;
    }
    
    /** List the subjects not in this role and not one of the
     * specified subjects.
     * @param whoami The current running user.
     * @param roleId The id of the role.
     * @return List of subjects in this role.
     * @throws FinderException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform
     * listSubjects on this role.
     * @throws FinderException if the sort attribute is not recognized
     * @ejb:interface-method
     *
     */
    public PageList getAvailableSubjects(AuthzSubject whoami,
                                         Integer roleId,
                                         Integer[] subjectIds,
                                         PageControl pc) 
        throws PermissionException, FinderException {
        Role roleLocal = lookupRole(roleId);

        /** TODO PermissionCheck scope for viewSubject **/
        Collection otherRoles;
        pc = PageControl.initDefaults(pc, SortAttribute.SUBJECT_NAME);
        AuthzSubjectDAO dao = new AuthzSubjectDAO(DAOFactory.getDAOFactory());
        switch (pc.getSortattribute()) {
        case SortAttribute.SUBJECT_NAME:
            otherRoles = dao.findByNotRoleId_orderName(roleLocal.getId(),
                                                       pc.isAscending());
            break;
        default:
            throw new FinderException("Unrecognized sort attribute: " +
                                      pc.getSortattribute());
        }

        // build an index of subjectIds
        int numToFind = subjectIds.length;
        HashSet index = new HashSet(Arrays.asList(subjectIds));

        // grep out the specified subjects
        ArrayList subjects = new ArrayList(numToFind);
        for(Iterator i = otherRoles.iterator(); i.hasNext();) {
            AuthzSubject subj = (AuthzSubject)i.next();
            if (!index.contains(subj.getId())) {
                subjects.add(subj);
            }
        }

        PageList plist = new PageList();
        plist = subjectPager.seek(subjects, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(subjects.size());
        
        return plist;
    }
    
    /** Add subjects to this role.
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param sids Ids of ubjects to add to role.
     * @throws PermissionException whoami is not allowed to perform 
     * addSubject on this role.
     * @ejb:interface-method
     */
    public void addSubjects(AuthzSubject whoami, Integer id, Integer[] sids)
        throws PermissionException {
        Role role = lookupRole(id);
        for (int i = 0; i < sids.length; i++) {
            lookupSubject(sids[i]).addRole(role);
        }
    }

    /** Remove subjects from this role.
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param ids The ids of the subjects to remove.
     * @throws PermissionException whoami is not allowed to perform
     * removeSubject on this role.
     * @ejb:interface-method
     *
     */
    public void removeSubjects(AuthzSubject whoami, Integer id, Integer[] ids)
        throws PermissionException {
        Role roleLocal = lookupRole(id);
        for (int i = 0; i < ids.length; i++) {
            AuthzSubject subj = lookupSubject(ids[i]);
            subj.removeRole(roleLocal);
        }
    }
    
    /** 
     * Find all {@link Operation} objects
     * @ejb:interface-method
     */
    public Collection findAllOperations() {
        OperationDAO aDao = new OperationDAO(DAOFactory.getDAOFactory());
        
        return aDao.findAllOrderByName();
    }

    public static RoleManagerLocal getOne() {
        try {
            return RoleManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbPassivate() { }

    public void ejbActivate() { }

    public void ejbRemove() { }

    public void ejbCreate() throws CreateException {
        try {
            subjectPager = Pager.getPager(SUBJECT_PAGER);
            rolePager = Pager.getPager(ROLE_PAGER);
            groupPager = Pager.getPager(GROUP_PAGER);
            ownedRolePager = Pager.getPager(OWNEDROLE_PAGER);
        } catch (Exception e) {
            throw new CreateException("Could not create Pager: " + e);
        }
    }

    public void setSessionContext(javax.ejb.SessionContext ctx) { }
}
