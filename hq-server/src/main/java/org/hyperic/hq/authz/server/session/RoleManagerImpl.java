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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.events.group.GroupAddedToRolesZevent;
import org.hyperic.hq.authz.server.session.events.group.GroupRemovedFromRolesZevent;
import org.hyperic.hq.authz.server.session.events.role.GroupsAddedToRoleZevent;
import org.hyperic.hq.authz.server.session.events.role.GroupsRemovedFromRoleZevent;
import org.hyperic.hq.authz.server.session.events.role.RoleDeletedZevent;
import org.hyperic.hq.authz.server.session.events.role.RolePermissionsChangedZevent;
import org.hyperic.hq.authz.server.session.events.role.SubjectsAddedToRoleZevent;
import org.hyperic.hq.authz.server.session.events.role.SubjectsRemovedFromRoleZevent;
import org.hyperic.hq.authz.server.session.events.subject.SubjectAddedToRolesZevent;
import org.hyperic.hq.authz.server.session.events.subject.SubjectRemovedFromRolesZevent;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzDuplicateNameException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.authz.values.OwnedRoleValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.server.session.Calendar;
import org.hyperic.hq.common.shared.CalendarManager;
import org.hyperic.hq.zevents.ZeventManager;
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
 * Manipulates Roles and Subjects associated with them. All arguments and return
 * values are value-objects.
 * 
 */
@Service
@Transactional
public class RoleManagerImpl implements RoleManager, ApplicationContextAware {

    private final Log log = LogFactory.getLog(RoleManagerImpl.class);
    private Pager subjectPager;
    private Pager rolePager;
    private Pager groupPager;
    private Pager ownedRolePager;
    private static final String SUBJECT_PAGER = "org.hyperic.hq.authz.server.session.PagerProcessor_subject";
    private static final String ROLE_PAGER = "org.hyperic.hq.authz.server.session.PagerProcessor_role";
    private static final String OWNEDROLE_PAGER = "org.hyperic.hq.authz.server.session.PagerProcessor_ownedRole";
    private static final String GROUP_PAGER = "org.hyperic.hq.authz.server.session.PagerProcessor_resourceGroup";
    private RoleCalendarDAO roleCalendarDAO;
    private OperationDAO operationDAO;
    private ResourceGroupDAO resourceGroupDAO;
    private ResourceTypeDAO resourceTypeDAO;
    private RoleDAO roleDAO;
    private AuthzSubjectDAO authzSubjectDAO;
    private ResourceDAO resourceDAO;
    private CalendarManager calendarManager;
    private PermissionManager permissionManager;
    private ApplicationContext applicationContext;
    private ZeventManager zeventManager;

    @Autowired
    public RoleManagerImpl(RoleCalendarDAO roleCalendarDAO, OperationDAO operationDAO,
                           ResourceGroupDAO resourceGroupDAO, ResourceTypeDAO resourceTypeDAO, RoleDAO roleDAO,
                           AuthzSubjectDAO authzSubjectDAO, ResourceDAO resourceDAO, CalendarManager calendarManager,
                           PermissionManager permissionManager, ZeventManager zeventManager) {
        this.roleCalendarDAO = roleCalendarDAO;
        this.operationDAO = operationDAO;
        this.resourceGroupDAO = resourceGroupDAO;
        this.resourceTypeDAO = resourceTypeDAO;
        this.roleDAO = roleDAO;
        this.authzSubjectDAO = authzSubjectDAO;
        this.resourceDAO = resourceDAO;
        this.calendarManager = calendarManager;
        this.permissionManager = permissionManager;
        this.zeventManager = zeventManager;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        subjectPager = Pager.getPager(SUBJECT_PAGER);
        rolePager = Pager.getPager(ROLE_PAGER);
        groupPager = Pager.getPager(GROUP_PAGER);
        ownedRolePager = Pager.getPager(OWNEDROLE_PAGER);
    }

    /**
     * Validate that a role is ok to be added or updated
     * 
     * @param aRole
     * @throws AuthzDuplicateNameException
     */
    private void validateRole(RoleValue aRole) throws AuthzDuplicateNameException {
        Role role = roleDAO.findByName(aRole.getName());
        if (role != null) {
            throw new AuthzDuplicateNameException("A role named: " + aRole.getName() + " already exists");
        }

    }

    private Role lookupRole(RoleValue role) {
        return lookupRole(role.getId());
    }

    private Role lookupRole(Integer id) {
        return roleDAO.findById(id);
    }

    private ResourceGroup lookupGroup(Integer id) {
        return resourceGroupDAO.findById(id);
    }

    /**
	 * 
	 */
    @Transactional(readOnly=true)
    public boolean isRootRoleMember(AuthzSubject subject) {
        return getRootRoleIfMember(subject) != null;
    }

    @Transactional(readOnly=true)
    private Role getRootRoleIfMember(AuthzSubject subject) {
        // Look up the root role
        Role rootRole = roleDAO.findById(AuthzConstants.rootRoleId);
        // Look up the calling subject
        if (rootRole.getSubjects().contains(subject)) {
            return rootRole;
        }

        return null;
    }

    /**
     * Filter a collection of roleLocal objects to only include those viewable
     * by the specified user
     * 
     *
     */
    private Collection<Role> filterViewableRoles(AuthzSubject who, Collection<Role> roles) throws PermissionException {
        return filterViewableRoles(who, roles, null);
    }

    /**
     * Filter a collection of roleLocal object to only include those viewable by
     * the specific user and not in the list of ids passed in as excluded
     * 
     * @param who - the user
     * @param roles - the list of role locals
     * @param excludeIds - role ids which should be excluded from the return
     *        list
     * 
     * 
     */
    private Collection<Role> filterViewableRoles(AuthzSubject who, Collection<Role> roles, Integer[] excludeIds)
        throws PermissionException {
        try {

            permissionManager.check(who.getId(), resourceTypeDAO.findByName(AuthzConstants.roleResourceTypeName),
                AuthzConstants.rootResourceId, AuthzConstants.roleOpViewRole);
        } catch (PermissionException e) {
            return new ArrayList<Role>(0);
        }

        List<Integer> excludeList = null;
        boolean hasExclude = (excludeIds != null && excludeIds.length > 0);
        if (hasExclude) {
            excludeList = Arrays.asList(excludeIds);
        }

        // Throw out the excludes
        for (Iterator<Role> i = roles.iterator(); i.hasNext();) {
            Role role = i.next();
            Integer pk = role.getId();
            if (hasExclude && excludeList.contains(pk)) {
                i.remove();
            }
        }
        return roles;
    }

    /**
     * Create a role.
     * 
     * @param whoami The current running user.
     * @param role The to be created.
     * @param operations Operations to associate with the new role. Use null if
     *        you want to associate operations later.
     * @param subjectIds Ids of subjects to add to the new role. Use null to add
     *        subjects later.
     * @param groupIds Ids of resource groups to add to the new role. Use null
     *        to add subjects later.
     * @return OwnedRoleValue for the role.
     * 
     * 
     * @throws PermissionException whoami may not perform createResource on the
     *         covalentAuthzRole ResourceType.
     * 
     */
    public Integer createOwnedRole(AuthzSubject whoami, RoleValue role, Operation[] operations, Integer[] subjectIds,
                                   Integer[] groupIds) throws  AuthzDuplicateNameException,
        PermissionException {

        validateRole(role);

        permissionManager.check(whoami.getId(), resourceTypeDAO.findTypeResourceType(), AuthzConstants.rootResourceId,
            AuthzConstants.roleOpCreateRole);

        Role roleLocal = roleDAO.create(whoami, role);

        // Associated operations
        roleLocal.setOperations(toPojos(operations));

        if (subjectIds != null) {
            HashSet<AuthzSubject> sLocals = new HashSet<AuthzSubject>(subjectIds.length);
            for (int si = 0; si < subjectIds.length; si++) {
                sLocals.add(authzSubjectDAO.findById(subjectIds[si]));
            }
            // Associated subjects
            roleLocal.setSubjects(sLocals);
            zeventManager.enqueueEventAfterCommit(new SubjectsAddedToRoleZevent(roleLocal, sLocals));

        }

        if (groupIds != null) {
            HashSet<ResourceGroup> gLocals = new HashSet<ResourceGroup>(groupIds.length);
            for (int gi = 0; gi < groupIds.length; gi++) {
                gLocals.add(lookupGroup(groupIds[gi]));
            }
            // Associated resource groups
            roleLocal.setResourceGroups(gLocals);
            zeventManager.enqueueEventAfterCommit(new GroupsAddedToRoleZevent(roleLocal, groupIds));
        }
        applicationContext.publishEvent(new RoleCreatedEvent(roleLocal));
        return roleLocal.getId();
    }

    /**
     * Delete the specified role.
     * 
     * @param whoami The current running user.
     * @param rolePk The role to delete.
     * @throws ApplicationException Unable to delete the specified entity.
     * 
     */
    public void removeRole(AuthzSubject whoami, Integer rolePk) throws PermissionException, ApplicationException {
        // Don't delete the super user role
        if (rolePk.equals(AuthzConstants.rootRoleId)) {
            throw new ApplicationException("Superuser role cannot be removed");
        }

        Role role = roleDAO.findById(rolePk);

        permissionManager.check(whoami.getId(), role.getResource().getResourceType(), role.getId(),
            AuthzConstants.roleOpRemoveRole);
        applicationContext.publishEvent(new RoleDeleteRequestedEvent(role));
        final Collection<AuthzSubject> subjects = new ArrayList<AuthzSubject>(role.getSubjects());
        Collection<ResourceGroup> assignedResourceGroups = new ArrayList<ResourceGroup>(role.getResourceGroups());

        for (RoleCalendar c : role.getCalendars()) {
            removeCalendar(c);
        }
        roleDAO.remove(role);
        zeventManager.enqueueEventAfterCommit(new RoleDeletedZevent(role, assignedResourceGroups, subjects));
    }

    /**
     * Write the specified entity out to permanent storage.
     * 
     * @param whoami The current running user.
     * @param role The role to save.
     * @throws PermissionException whoami may not perform modifyRole on this
     *         role.
     * 
     */
    public void saveRole(AuthzSubject whoami, RoleValue role) throws AuthzDuplicateNameException, PermissionException {
        Role roleLocal = lookupRole(role);
        if (!roleLocal.getName().equals(role.getName())) {
            // Name has changed... check it
            validateRole(role);
        }

        permissionManager.check(whoami.getId(), roleLocal.getResource().getResourceType(), roleLocal.getId(),
            AuthzConstants.roleOpModifyRole);
        roleLocal.setRoleValue(role);
    }

    /**
     * Change the owner of the role.
     * 
     * @param whoami The current running user.
     * @param id The ID of the role to change
     * @param ownerVal The new owner of the role..
     * @throws PermissionException whoami may not perform modifyRole on this
     *         role.
     * 
     */
    public void changeOwner(AuthzSubject whoami, Integer id, AuthzSubject owner) throws PermissionException {
        Role roleLocal = lookupRole(id);

        permissionManager.check(whoami.getId(), roleLocal.getResource().getResourceType(), roleLocal.getId(),
            AuthzConstants.roleOpModifyRole);

        roleLocal.getResource().setOwner(owner);
    }

    /**
     * Associate operations with this role.
     * 
     * @param whoami The current running user.
     * @param role The role.
     * @param operations The operations to associate with the role.
     * 
     * @throws PermissionException whoami may not perform addOperation on this
     *         role.
     * 
     */
    public void addOperations(AuthzSubject whoami, Role role, Operation[] operations) throws PermissionException {
        Set<Operation> opLocals = toPojos(operations);

        // roleLocal.setWhoami(lookupSubject(whoami));
        Collection<Operation> orgOperations = role.getOperations();
        role.getOperations().addAll(opLocals);
        zeventManager.enqueueEventAfterCommit(new RolePermissionsChangedZevent(role, orgOperations));
    }

    /**
     * Disassociate all operations from this role.
     * 
     * @param whoami The current running user.
     * @param role The role.
     *
     * @throws PermissionException whoami may not perform removeOperation on
     *         this role.
     * 
     */
    public void removeAllOperations(AuthzSubject whoami, Role role) throws PermissionException {
        // roleLocal.setWhoami(lookupSubject(whoami));
        role.getOperations().clear();
    }

    /**
     * Set the operations for this role. To get the operations call
     * getOperations() on the value-object.
     * 
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param operations Operations to associate with this role.
     * 
     * @throws PermissionException whoami is not allowed to perform
     *         setOperations on this role.
     * 
     */
    public void setOperations(AuthzSubject whoami, Integer id, Operation[] operations) throws PermissionException {
        if (operations != null) {
            Role roleLocal = lookupRole(id);

            permissionManager.check(whoami.getId(), roleLocal.getResource().getResourceType(), roleLocal.getId(),
                AuthzConstants.roleOpModifyRole);

            Set<Operation> opLocals = toPojos(operations);
            Collection<Operation> orgOperations = roleLocal.getOperations();
            roleLocal.setOperations(opLocals);
            zeventManager.enqueueEventAfterCommit(new RolePermissionsChangedZevent(roleLocal, orgOperations));
        }
    }

    /**
     * Associate ResourceGroups with this role.
     * 
     * @param whoami The current running user.
     * @param role This role.
     * @param gids The ids of the groups to associate with this role.
     * 
     * @throws PermissionException whoami is not allowed to perform
     *         addResourceGroup on this role.
     * 
     */
    public void addResourceGroups(AuthzSubject whoami, Integer rid, Integer[] gids) throws PermissionException {
        Role roleLocal = roleDAO.findById(rid);
        if (gids!= null && gids.length > 0){
            for (int i = 0; i < gids.length; i++) {
                ResourceGroup group = lookupGroup(gids[i]);
                group.addRole(roleLocal);
            }
            zeventManager.enqueueEventAfterCommit(new GroupsAddedToRoleZevent(roleLocal, gids));
        }
    }
    

    /**
     * Associate ResourceGroup with list of roles.
     * 
     * @param whoami The current running user.
     * @param roles The roles.
     * @param ids The id of the group to associate with the roles.
     * @throws PermissionException whoami is not allowed to perform
     *         addResourceGroup on this role.
     * 
     * 
     */
    public void addResourceGroupRoles(AuthzSubject whoami, Integer gid, Integer[] ids) throws PermissionException {
        addResourceGroupRoles(whoami, gid, ids, false);
    }

    /**
     * Associate ResourceGroup with list of roles.
     * 
     * @param whoami The current running user.
     * @param roles The roles.
     * @param ids The id of the group to associate with the roles.
     * @throws PermissionException whoami is not allowed to perform
     *         addResourceGroup on this role.
     * 
     * 
     */
    public void addResourceGroupRoles(AuthzSubject whoami, Integer gid, Integer[] ids
            , boolean isDuringCalculation) throws PermissionException {
        ResourceGroup group = lookupGroup(gid);
        for (int i = 0; i < ids.length; i++) {
            Role roleLocal = lookupRole(ids[i]);
            group.addRole(roleLocal);
        }
        if (ids!= null && ids.length > 0){
            zeventManager.enqueueEventAfterCommit(new GroupAddedToRolesZevent(group, isDuringCalculation));        
        }
    }

    /**
     * Disassociate ResourceGroups from this role.
     * 
     * @param whoami The current running user.
     * @param id This role.
     * @param gids The ids of the groups to disassociate.
     * 
     * @throws PermissionException whoami is not allowed to perform modifyRole
     *         on this role.
     * 
     */
    public void removeResourceGroups(AuthzSubject whoami, Integer id, Integer[] gids) throws PermissionException {
        Role roleLocal = lookupRole(id);
        Collection<ResourceGroup> groups = new HashSet<ResourceGroup>();
        permissionManager.check(whoami.getId(), roleLocal.getResource().getResourceType(), roleLocal.getId(),
            AuthzConstants.roleOpModifyRole);

        for (int i = 0; i < gids.length; i++) {
            ResourceGroup group = lookupGroup(gids[i]);
            roleLocal.removeResourceGroup(group);
            groups.add(group);
            
        }
        if (gids!= null && gids.length > 0){
            zeventManager.enqueueEventAfterCommit(new GroupsRemovedFromRoleZevent(roleLocal, groups));
        }
    }

    /**
     * Disassociate roles from this ResourceGroup.
     * 
     * @param whoami The current running user.
     * @param role This role.
     * @param ids The ids of the groups to disassociate.
     *
     * @throws PermissionException whoami is not allowed to perform modifyRole
     *         on this role.
     * 
     */
    public void removeResourceGroupRoles(AuthzSubject whoami, Integer gid, Integer[] ids) 
            throws PermissionException {
        removeResourceGroupRoles(whoami, gid, ids, false); 
    }
    
    /**
     * Disassociate roles from this ResourceGroup.
     * 
     * @param whoami The current running user.
     * @param role This role.
     * @param ids The ids of the groups to disassociate.
     * @param isDuringCalculation true/false if we are in the middle of group membership calculation 
     *
     * @throws PermissionException whoami is not allowed to perform modifyRole
     *         on this role.
     * 
     */
    public void removeResourceGroupRoles(AuthzSubject whoami, Integer gid, Integer[] ids, boolean isDuringCalculation) 
            throws PermissionException {

        ResourceGroup group = lookupGroup(gid);
        Collection<Role> roles = new HashSet<Role>();
        for (int i = 0; i < ids.length; i++) {
            Role roleLocal = lookupRole(ids[i]);

            permissionManager.check(whoami.getId(), roleLocal.getResource().getResourceType(), roleLocal.getId(),
                AuthzConstants.roleOpModifyRole);

            roles.add(roleLocal);
            roleLocal.removeResourceGroup(group);
        }
        if (ids!= null && ids.length > 0){
            zeventManager.enqueueEventAfterCommit(new GroupRemovedFromRolesZevent(group, roles, isDuringCalculation));
        }
    }
    
    /**
     * Disassociate all ResourceGroups of this role from this role.
     * 
     * @param whoami The current running user.
     * @param role This role.
     * 
     *
     * @throws PermissionException whoami is not allowed to perform modifyRole
     *         on this role.
     * 
     */
    @SuppressWarnings("unchecked")
    public void removeAllResourceGroups(AuthzSubject whoami, Role role) throws PermissionException {

        permissionManager.check(whoami.getId(), role.getResource().getResourceType(), role.getId(),
            AuthzConstants.roleOpModifyRole);
        Collection<ResourceGroup> groups = role.getResourceGroups();
        role.clearResourceGroups();
        if (groups!= null && groups.size() > 0){
            zeventManager.enqueueEventAfterCommit(new GroupsRemovedFromRoleZevent(role, groups));
        }
    }

    /**
     * Get the # of roles within HQ inventory
     * 
     * 
     */
    @Transactional(readOnly=true)
    public Number getRoleCount() {
        return new Integer(roleDAO.size());
    }

    /**
     * Get the # of subjects within HQ inventory
     * 
     * 
     */
    @Transactional(readOnly=true)
    public Number getSubjectCount() {
        return new Integer(authzSubjectDAO.size());
    }

    /**
     * Get a Role by id
     * 
     * 
     */
    @Transactional(readOnly=true)
    public Role getRoleById(int id) {
        return roleDAO.get(new Integer(id));
    }

    /**
	 * 
	 */
    @Transactional(readOnly=true)
    public Role findRoleById(int id) {
        return lookupRole(new Integer(id));
    }

    /**
	 * 
	 */
    @Transactional(readOnly=true)
    public Role findRoleByName(String name) {
        return roleDAO.findByName(name);
    }

    /**
     * Create a calendar under a role for a specific type. Calendars created in
     * this manner are tied directly to the role and should not be used by other
     * roles.
     * 
     * @throws PermissionException if user is not allowed to modify role
     * 
     */
    public RoleCalendar createCalendar(AuthzSubject whoami, Role r, String calendarName, RoleCalendarType type)
        throws PermissionException {

        permissionManager.check(whoami.getId(), r.getResource().getResourceType(), r.getId(),
            AuthzConstants.roleOpModifyRole);

        Calendar cal = calendarManager.createCalendar(calendarName);
        RoleCalendar res = new RoleCalendar(r, cal, type);
        r.addCalendar(res);
        return res;
    }

    /**
	 * 
	 */
    public boolean removeCalendar(RoleCalendar c) {
        boolean res = c.getRole().removeCalendar(c);
        roleCalendarDAO.remove(c);
        calendarManager.remove(c.getCalendar());
        return res;
    }

    /**
     * Find the owned role that has the given ID.
     * 
     * @param id The ID of the role you're looking for.
     * @return The owned value-object of the role of the given ID.
     * 
     * 
     */
    public OwnedRoleValue findOwnedRoleById(AuthzSubject whoami, Integer id) throws PermissionException {

        Role local = roleDAO.findById(id);

        int numSubjects = authzSubjectDAO.size(local.getSubjects());

        permissionManager.check(whoami.getId(), local.getResource().getResourceType(), id,
            AuthzConstants.roleOpViewRole);

        OwnedRoleValue value = new OwnedRoleValue(local);
        value.setMemberCount(numSubjects);

        return value;
    }

    /**
     * Get operations For a given role id, find the resource types and
     * permissions which are supported by it
     * 
     * @param subject
     * @param roleId
     * @return list - values are lists of operation
     * 
     */
    @Transactional(readOnly=true)
    public List<Operation> getRoleOperations(AuthzSubject subject, Integer roleId)
        throws PermissionException {
      
        // find the role by id
        Role role = roleDAO.findById(roleId);
        return new ArrayList<Operation>(role.getOperations());
    }

    /**
     * @return a list of {@link Role}s
     * 
     */
    @Transactional(readOnly=true)
    public Collection<Role> getAllRoles() {
        return roleDAO.findAll();
    }

    private Collection<Role> getAllRoles(AuthzSubject subject, int sort, boolean asc) {
        switch (sort) {
            default:
            case SortAttribute.ROLE_NAME:
                return roleDAO.findAll_orderName(asc);
        }
    }

    /**
     * List all Roles in the system
     * 
     * @param pc Paging information for the request
     * @return List a list of RoleValues
     * 
     */
    @Transactional(readOnly=true)
    public List<RoleValue> getAllRoles(AuthzSubject subject, PageControl pc)  {
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        Collection<Role> roles = getAllRoles(subject, pc.getSortattribute(), pc.isAscending());

        return rolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * List all OwnedRoles in the system
     * 
     * @param subject
     * @param pc Paging and sorting information.
     * @return List a list of OwnedRoleValues
     * 
     */
    @Transactional(readOnly=true)
    public List<OwnedRoleValue> getAllOwnedRoles(AuthzSubject subject, PageControl pc) {
        Collection<Role> roles = roleDAO.findAll();
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        return ownedRolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * List all Roles in the system, except system roles.
     * 
     * @return List a list of OwnedRoleValues that are not system roles
     * @throws NotFoundException if sort attribute is unrecognized
     * 
     */
    @Transactional(readOnly=true)
    public PageList<OwnedRoleValue> getAllNonSystemOwnedRoles(AuthzSubject subject, Integer[] excludeIds, PageControl pc)
        throws PermissionException, NotFoundException {

        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        int attr = pc.getSortattribute();
        Collection<Role> roles;
        switch (attr) {

            case SortAttribute.ROLE_NAME:
                roles = roleDAO.findBySystem_orderName(false, !pc.isDescending());
                break;

            default:
                throw new NotFoundException("Unrecognized sort attribute: " + attr);
        }

        // 6729 - if caller is a member of the root role, show it
        // 5345 - allow access to the root role by the root user so it can
        // be used by others
        Role rootRole = getRootRoleIfMember(subject);
        if (rootRole != null) {
            ArrayList<Role> newList = new ArrayList<Role>();
            newList.add(rootRole);
            newList.addAll(roles);
            roles = newList;
        }

        roles = filterViewableRoles(subject, roles, excludeIds);

        PageList<OwnedRoleValue> plist = ownedRolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(roles.size());
        return plist;
    }

    /**
     * Get the roles with the specified ids
     * 
     * @param subject
     * @param ids the role ids
     * @param pc Paging information for the request
     * 
     * @throws PermissionException
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<RoleValue> getRolesById(AuthzSubject whoami, Integer[] ids, PageControl pc)
        throws PermissionException {

        List<Role> roles = getRolesByIds(whoami, ids, pc);

        PageList<RoleValue> plist = rolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(roles.size());

        return plist;
    }

    private List<Role> getRolesByIds(AuthzSubject whoami, Integer[] ids, PageControl pc) throws PermissionException {

        permissionManager.check(whoami.getId(), AuthzConstants.roleResourceTypeName, AuthzConstants.rootResourceId,
            AuthzConstants.roleOpViewRole);

        Collection<Role> all = getAllRoles(whoami, pc.getSortattribute(), pc.isAscending());

        // build an index of ids
        HashSet<Integer> index = new HashSet<Integer>();
        for (int i = 0; i < ids.length; i++) {
            Integer id = ids[i];
            index.add(id);
        }
        int numToFind = index.size();

        // find the requested roles
        List<Role> roles = new ArrayList<Role>(ids.length);
        Iterator<Role> i = all.iterator();
        while (i.hasNext() && roles.size() < numToFind) {
            Role r = i.next();
            if (index.contains(r.getId())) {
                roles.add(r);
            }
        }
        return roles;
    }

    /**
     * Associate roles with this subject.
     * 
     * @param whoami The current running user.
     * @param subject The subject.
     * @param roles The roles to associate with the subject.
     * @throws PermissionException whoami may not perform addRole on this
     *         subject.
     * 
     */
    public void addRoles(AuthzSubject whoami, AuthzSubject subject, Integer[] roles) throws PermissionException {
        Collection<Role> roleObjects = new HashSet<Role>();
        for (int i = 0; i < roles.length; i++) {
            Role roleLocal = lookupRole(roles[i]);
            roleObjects.add(roleLocal);
            subject.addRole(roleLocal);
        }
        if (roles!= null && roles.length > 0){
            zeventManager.enqueueEventAfterCommit(new SubjectAddedToRolesZevent(subject, roleObjects));
        }
    }

    /**
     * Disassociate roles from this subject.
     * 
     * @param whoami The current running user.
     * @param subject The subject.
     * @param roles The subjects to disassociate.
     * @throws PermissionException whoami may not perform removeRole on this
     *         subject.
     * 
     */
    public void removeRoles(AuthzSubject whoami, AuthzSubject subject, Integer[] roles) throws PermissionException {
        Collection<Role> roleLocals = getRolesByIds(whoami, roles, PageControl.PAGE_ALL);
   
        for (Role role : roleLocals) {
            subject.removeRole(role);
        }
        if (roles!= null && roles.length > 0){
            zeventManager.enqueueEventAfterCommit(new SubjectRemovedFromRolesZevent(subject, roleLocals));
        }
    }

    /**
     * Get the roles for a subject
     * 
     * @param whoami
     * @param subject
     * @param pc Paging and sorting information.
     * @return Set of Roles
     * 
     */
    @Transactional(readOnly=true)
    public List<RoleValue> getRoles(AuthzSubject subjectValue, PageControl pc) throws PermissionException {
        Collection<Role> roles = subjectValue.getRoles();
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        return rolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get the owned roles for a subject.
     * 
     * @param whoami
     * @param subject
     * @param pc Paging and sorting information.
     * @return Set of Roles
     * 
     */
    @Transactional(readOnly=true)
    public List<OwnedRoleValue> getOwnedRoles(AuthzSubject subject, PageControl pc) throws PermissionException {
        Collection<Role> roles = subject.getRoles();
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        return ownedRolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get the owned roles for a subject, except system roles.
     * 
     * @param callerSubjectValue is the subject of caller.
     * @param intendedSubjectValue is the subject of intended subject.
     * @param pc The PageControl object for paging results.
     * @return List a list of OwnedRoleValues that are not system roles
     *  
     * @throws PermissionException caller is not allowed to perform listRoles on
     *         this role.
  
     */
    @Transactional(readOnly=true)
    public PageList<OwnedRoleValue> getNonSystemOwnedRoles(AuthzSubject callerSubjectValue,
                                                           AuthzSubject intendedSubjectValue, PageControl pc)
        throws PermissionException {
        return getNonSystemOwnedRoles(callerSubjectValue, intendedSubjectValue, null, pc);
    }

    /**
     * Get the owned roles for a subject, except system roles.
     * 
     * @param callerSubjectValue is the subject of caller.
     * @param intendedSubjectValue is the subject of intended subject.
     * @param pc The PageControl object for paging results.
     * @return List a list of OwnedRoleValues that are not system roles
     * 
     * @throws PermissionException caller is not allowed to perform listRoles on
     *         this role.
     */
    @Transactional(readOnly=true)
    public PageList<OwnedRoleValue> getNonSystemOwnedRoles(AuthzSubject callerSubjectValue,
                                                           AuthzSubject intendedSubjectValue, Integer[] excludeIds,
                                                           PageControl pc) throws PermissionException {

        // Fetch all roles presently assigned to the assignee
        Collection<Role> roles;

        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);

        switch (pc.getSortattribute()) {
            case SortAttribute.ROLE_NAME:
                roles = roleDAO.findBySystemAndSubject_orderName(false, intendedSubjectValue.getId(), pc.isAscending());
                break;
            case SortAttribute.ROLE_MEMBER_CNT:
                roles = roleDAO.findBySystemAndSubject_orderMember(false, intendedSubjectValue.getId(), pc
                    .isAscending());
                break;
            default:
                throw new IllegalArgumentException("Invalid sort parameter");
        }

        if (isRootRoleMember(intendedSubjectValue)) {
            ArrayList<Role> roleList = new ArrayList<Role>(roles.size() + 1);

            Role rootRole = roleDAO.findById(AuthzConstants.rootRoleId);

            // We need to insert into the right place
            boolean done = false;
            for (Role role : roles) {
                if (!done) {
                    if (pc.getSortattribute() == SortAttribute.ROLE_NAME) {
                        if ((pc.isAscending() && role.getName().compareTo(rootRole.getName()) > 0) ||
                            (pc.isDescending() && role.getName().compareTo(rootRole.getName()) < 0)) {
                            roleList.add(rootRole);
                            done = true;
                        }
                    } else if (pc.getSortattribute() == SortAttribute.ROLE_MEMBER_CNT) {
                        if ((pc.isAscending() && role.getSubjects().size() > rootRole.getSubjects().size()) ||
                            (pc.isDescending() && role.getSubjects().size() < rootRole.getSubjects().size())) {
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
        Collection<Role> viewableRoles = filterViewableRoles(callerSubjectValue, roles, excludeIds);

        return ownedRolePager.seek(viewableRoles, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * List the roles that this subject is not in and that are not one of the
     * specified roles.
     * 
     * @param whoami The current running user.
     * @param system If true, then only system roles are returned. If false,
     *        then only non-system roles are returned.
     * @param subjectId The id of the subject.
     * @return List of roles.
     * @throws NotFoundException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform listRoles on
     *         this role.
     *
     * 
     */
    @Transactional(readOnly=true)
    public PageList<RoleValue> getAvailableRoles(AuthzSubject whoami, boolean system, Integer subjectId,
                                                 Integer[] roleIds, PageControl pc) throws PermissionException,
        NotFoundException{
        Collection<Role> foundRoles;
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        int attr = pc.getSortattribute();
        switch (attr) {

            case SortAttribute.ROLE_NAME:
                foundRoles = roleDAO.findBySystemAndAvailableForSubject_orderName(system, whoami.getId(), !pc
                    .isDescending());
                break;

            default:
                throw new NotFoundException("Unrecognized sort attribute: " + attr);
        }

        HashSet<Integer> index = new HashSet<Integer>();
        if (roleIds != null) {
            index.addAll(Arrays.asList(roleIds));
        }

        Collection<Role> roles = new ArrayList<Role>();

        for (Role r : foundRoles) {
            if (!index.contains(r.getId())) {
                roles.add(r);
            }
        }

        // AUTHZ Check
        // filter the viewable roles
        roles = filterViewableRoles(whoami, roles);

        PageList<RoleValue> plist = new PageList<RoleValue>();
        plist = rolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(roles.size());
        // 6729 - if caller is a member of the root role, show it
        // 5345 - allow access to the root role by the root user so it can
        // be used by others
        if (isRootRoleMember(whoami) && pc.getPagenum() == 0 && !index.contains(AuthzConstants.rootRoleId)) {
            Role role = roleDAO.findAvailableRoleForSubject(AuthzConstants.rootRoleId, subjectId);
            if (role == null) {
                return plist;
            }
            OwnedRoleValue rootRoleValue = role.getOwnedRoleValue();
            PageList<RoleValue> newList = new PageList<RoleValue>();
            newList.add(rootRoleValue);
            newList.addAll(plist);
            newList.setTotalSize(plist.getTotalSize() + 1);
            return newList;
        }
        return plist;
    }

    /**
     * List the roles that this subject is not in and that are not one of the
     * specified roles.
     * 
     * @param whoami The current running user.
     * @param system If true, then only system roles are returned. If false,
     *        then only non-system roles are returned.
     * @param groupId The id of the subject.
     * @return List of roles.
     * @throws NotFoundException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform listRoles on
     *         this role.
     * @throws NotFoundException if the sort attribute was not recognized
     * 
     */
    @Transactional(readOnly=true)
    public PageList<RoleValue> getAvailableGroupRoles(AuthzSubject whoami, Integer groupId, Integer[] roleIds,
                                                      PageControl pc) throws PermissionException, NotFoundException {
        Collection<Role> foundRoles;
        pc = PageControl.initDefaults(pc, SortAttribute.ROLE_NAME);
        int attr = pc.getSortattribute();

        switch (attr) {
            case SortAttribute.ROLE_NAME:
                foundRoles = roleDAO.findAvailableForGroup(false, groupId);
                break;
            default:
                throw new NotFoundException("Unrecognized sort attribute: " + attr);
        }

        log.debug("Found " + foundRoles.size() + " available roles for group " + groupId +
                  " before permission checking");

        HashSet<Integer> index = new HashSet<Integer>();
        if (roleIds != null) {
            index.addAll(Arrays.asList(roleIds));
        }

        // Grep out the specified roles
        ArrayList<Role> roles = new ArrayList<Role>();

        for (Role r : foundRoles) {
            if (!index.contains(r.getId())) {
                roles.add(r);
            }
        }

        log.debug("Found " + roles.size() + " available roles for group " + groupId + " after exclusions");

        // AUTHZ Check - filter the viewable roles
        roles = (ArrayList) filterViewableRoles(whoami, roles);

        if (isRootRoleMember(whoami) && pc.getPagenum() == 0 && !index.contains(AuthzConstants.rootRoleId)) {
            foundRoles = roleDAO.findAvailableForGroup(true, groupId);
            for (Role role : foundRoles) {
                if (role.getId().equals(AuthzConstants.rootRoleId)) {
                    roles.add(role);
                }
            }
        }

        if (pc.isDescending()) {
            Collections.reverse(roles);
        }

        log.debug("Found " + roles.size() + " available roles for group " + groupId + " after permission checking");

        PageList<RoleValue> plist = rolePager.seek(roles, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(roles.size());

        return plist;
    }
    
    /**
     * Get the resource groups applicable to a given role.
     * @throws NotFoundException 
     *
    */
    @Transactional(readOnly=true)
    public Collection<ResourceGroup> getResourceGroupsByRole(AuthzSubject subject,Role role)
        throws PermissionException, NotFoundException {
           Collection<ResourceGroup> groups =
                   resourceGroupDAO.findByRoleIdAndSystem_orderName(role.getId(), false, true);
            
           // now get viewable group pks
           return filterViewableGroups(subject, groups);
    }
    
    
    /**
     * Get the resource groups applicable to a given role.
     * @throws NotFoundException 
     *
    */
    @Transactional(readOnly=true)
    public Collection<ResourceGroup> getResourceGroupsByRoleAndGroupType(AuthzSubject subject,Role role, int groupType)
        throws PermissionException, NotFoundException {
           Collection<ResourceGroup> viewableGroups = getResourceGroupsByRole(subject,role);
           return  filterByGroupType(viewableGroups, groupType);
    }
    
    /**
     * Filter a collection of groupLocal objects to only include those viewable
     * by the specified user
     */
    private Collection<ResourceGroup> filterByGroupType(Collection<ResourceGroup> groups, int groupType) {

        for (Iterator<ResourceGroup> i = groups.iterator(); i.hasNext();) {
            ResourceGroup resGrp = i.next();

            if (resGrp.getGroupType() != groupType) {
                i.remove();
            }
        }
        return groups;
    }

    /**
     * Get the resource groups applicable to a given role
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ResourceGroupValue> getResourceGroupsByRoleIdAndSystem(AuthzSubject subject, Integer roleId,
                                                                           boolean system, PageControl pc)
        throws PermissionException, NotFoundException {
        // first find the role by its id
        roleDAO.findById(roleId);

        // now check to make sure the user can list resource groups
        Collection<ResourceGroup> groups;
        pc = PageControl.initDefaults(pc, SortAttribute.RESGROUP_NAME);
        int attr = pc.getSortattribute();

        switch (attr) {
            case SortAttribute.RESGROUP_NAME:
                groups = resourceGroupDAO.findByRoleIdAndSystem_orderName(roleId, system, pc.isAscending());
                break;

            default:
                throw new NotFoundException("Unrecognized sort attribute: " + attr);
        }

        // now get viewable group pks
        groups = filterViewableGroups(subject, groups);

        PageList<ResourceGroupValue> plist = groupPager.seek(groups, pc);
        plist.setTotalSize(groups.size());

        return plist;
    }

    /**
     * Return the roles of a group
     * 
     * @throws PermissionException
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<RoleValue> getResourceGroupRoles(AuthzSubject whoami, Integer groupId, PageControl pc)
        throws PermissionException {
        ResourceGroup resGrp = resourceGroupDAO.findById(groupId);

        permissionManager.check(whoami.getId(), AuthzConstants.authzGroup, resGrp.getId(),
            AuthzConstants.perm_viewResourceGroup);

        Collection<Role> roles = resGrp.getRoles();

        TreeMap<String, Role> map = new TreeMap<String, Role>();
        for (Role role : roles) {
            int attr = pc.getSortattribute();
            switch (attr) {
                case SortAttribute.ROLE_NAME:
                default:
                    map.put(role.getName(), role);
            }
        }

        ArrayList<Role> list = new ArrayList<Role>(map.values());

        if (pc.isDescending()) {
            Collections.reverse(list);
        }

        PageList<RoleValue> plist = rolePager.seek(list, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(roles.size());

        return plist;
    }

    /**
     * Filter a collection of groupLocal objects to only include those viewable
     * by the specified user
     */
    private Collection<ResourceGroup> filterViewableGroups(AuthzSubject who, Collection<ResourceGroup> groups)
        throws PermissionException, NotFoundException {
        // finally scope down to only the ones the user can see

        Collection<Integer> viewable = permissionManager.findOperationScopeBySubject(who,
            AuthzConstants.groupOpViewResourceGroup, AuthzConstants.groupResourceTypeName);

        for (Iterator<ResourceGroup> i = groups.iterator(); i.hasNext();) {
            ResourceGroup resGrp = i.next();

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
     * @throws PermissionException whoami is not allowed to perform listGroups
     *         on this role.
     * @throws NotFoundException
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ResourceGroupValue> getAvailableResourceGroups(AuthzSubject whoami, Integer roleId,
                                                                   Integer[] groupIds, PageControl pc)
        throws PermissionException, NotFoundException {

        Role role = roleDAO.findById(roleId);
        Collection<ResourceGroup> noRoles;
        Collection<ResourceGroup> otherRoles;
        pc = PageControl.initDefaults(pc, SortAttribute.RESGROUP_NAME);
        int attr = pc.getSortattribute();

        switch (attr) {
            case SortAttribute.RESGROUP_NAME:
                noRoles = resourceGroupDAO.findWithNoRoles_orderName(pc.isAscending());
                otherRoles = resourceGroupDAO.findByNotRoleId_orderName(role.getId(), pc.isAscending());
                break;

            default:
                throw new NotFoundException("Unrecognized sort attribute: " + attr);
        }

        // FIXME- merging these two sorted lists probably causes the
        // final list to not be sorted correctly. fix this by
        // combining the two finders into one!
        // FIX for 6924 - dont include duplicate groups
        for (ResourceGroup group : otherRoles) {
            if (!noRoles.contains(group)) {
                noRoles.add(group);
            }
        }

        // build an index of groupIds
        int numToFind = (groupIds == null) ? 0 : groupIds.length;
        HashSet<Integer> index = new HashSet<Integer>();
        for (int i = 0; i < numToFind; i++) {
            index.add(groupIds[i]);
        }

        // Add the groups that the role already owns
        Collection<ResourceGroup> belongs = resourceGroupDAO.findByRoleIdAndSystem_orderName(roleId, false, true);
        for (ResourceGroup s : belongs) {
            index.add(s.getId());
        }

        // grep out the specified groups
        Collection<ResourceGroup> groups = new ArrayList<ResourceGroup>(noRoles.size());
        for (ResourceGroup s : noRoles) {
            if (!index.contains(s.getId())) {
                groups.add(s);
            }
        }

        // AUTHZ Check
        // finally scope down to only the ones the user can see
        groups = filterViewableGroups(whoami, groups);

        PageList<ResourceGroupValue> plist = groupPager.seek(groups, pc.getPagenum(), pc.getPagesize());

        plist.setTotalSize(groups.size());

        return plist;
    }

    /**
     * List the subjects in this role.
     * 
     * @param whoami The current running user.
     * @param roleId The id of the role.
     * @return List of subjects in this role.
     * @throws PermissionException whoami is not allowed to perform listSubjects
     *         on this role.
     * @throws NotFoundException if the sort attribute is not recognized
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AuthzSubjectValue> getSubjects(AuthzSubject whoami, Integer roleId, PageControl pc)
        throws PermissionException, NotFoundException {
        Role roleLocal = roleDAO.get(roleId);

        if (roleLocal == null) {
            return new PageList<AuthzSubjectValue>();
        }

        // check if this user is a member of this role
        boolean roleHasUser = roleLocal.getSubjects().contains(whoami);
        // check whether the user can see subjects other than himself
        try {

            permissionManager.check(whoami.getId(), resourceTypeDAO.findTypeResourceType(),
                AuthzConstants.rootResourceId, AuthzConstants.subjectOpViewSubject);
        } catch (PermissionException e) {
            // if the user does not have permission to view subjects
            // but he is in the role, return a collection with only one
            // item... himself.
            if (roleHasUser) {
                PageList<AuthzSubjectValue> subjects = new PageList<AuthzSubjectValue>();
                subjects.add(whoami.getAuthzSubjectValue());
                subjects.setTotalSize(1);
                return subjects;
            }
            // otherwise return an empty list
            // fixes 5628 - user viewing role lacking view subjects
            // causes permissionexception
            return new PageList<AuthzSubjectValue>();
        }
        Collection<AuthzSubject> subjects;
        pc = PageControl.initDefaults(pc, SortAttribute.SUBJECT_NAME);

        switch (pc.getSortattribute()) {
            case SortAttribute.SUBJECT_NAME:
                subjects = authzSubjectDAO.findByRoleId_orderName(roleLocal.getId(), pc.isAscending());
                break;
            default:
                throw new NotFoundException("Unrecognized sort attribute: " + pc.getSortattribute());
        }

        PageList<AuthzSubjectValue> plist = new PageList<AuthzSubjectValue>();
        plist = subjectPager.seek(subjects, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(subjects.size());

        return plist;
    }

    /**
     * List the subjects not in this role and not one of the specified subjects.
     * 
     * @param whoami The current running user.
     * @param roleId The id of the role.
     * @return List of subjects in this role.
     * @throws NotFoundException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform listSubjects
     *         on this role.
     * @throws NotFoundException if the sort attribute is not recognized
     * 
     * 
     */
    @Transactional(readOnly=true)
    public PageList<AuthzSubjectValue> getAvailableSubjects(AuthzSubject whoami, Integer roleId, Integer[] subjectIds,
                                                            PageControl pc) throws PermissionException, NotFoundException {
        Role roleLocal = lookupRole(roleId);

        /** TODO PermissionCheck scope for viewSubject **/
        Collection<AuthzSubject> otherRoles;
        pc = PageControl.initDefaults(pc, SortAttribute.SUBJECT_NAME);

        switch (pc.getSortattribute()) {
            case SortAttribute.SUBJECT_NAME:
                otherRoles = authzSubjectDAO.findByNotRoleId_orderName(roleLocal.getId(), pc.isAscending());
                break;
            default:
                throw new NotFoundException("Unrecognized sort attribute: " + pc.getSortattribute());
        }

        // build an index of subjectIds
        int numToFind = subjectIds.length;
        HashSet<Integer> index = new HashSet<Integer>(Arrays.asList(subjectIds));

        // grep out the specified subjects
        ArrayList<AuthzSubject> subjects = new ArrayList<AuthzSubject>(numToFind);
        for (AuthzSubject subj : otherRoles) {
            if (!index.contains(subj.getId())) {
                subjects.add(subj);
            }
        }

        PageList<AuthzSubjectValue> plist = new PageList<AuthzSubjectValue>();
        plist = subjectPager.seek(subjects, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(subjects.size());

        return plist;
    }
    
    @Transactional(readOnly=true)
    public Collection<Role> getRoles(AuthzSubject subj) {
        subj = authzSubjectDAO.get(subj.getId());
        final Collection<Role> roles = subj.getRoles();
        final Collection<Role> rtn = new ArrayList<Role>(roles.size());
        for (final Role role : roles) {
            final Role r = roleDAO.get(role.getId());
            if (r == null) {
                continue;
            }
            rtn.add(r);
        }
        return rtn;
    }

    /**
     * Add subjects to this role.
     * 
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param sids Ids of subjects to add to role.
     * @throws PermissionException whoami is not allowed to perform addSubject
     *         on this role.
     * 
     */
    public void addSubjects(AuthzSubject whoami, Integer id, Integer[] sids) throws PermissionException {
        Role role = lookupRole(id);
        Collection<AuthzSubject> subjects = new HashSet<AuthzSubject>();
        if (sids!= null && sids.length > 0){
            for (int i = 0; i < sids.length; i++) {
                AuthzSubject subj = authzSubjectDAO.findById(sids[i]);
                subjects.add(subj);
                subj.addRole(role);
            }
            zeventManager.enqueueEventAfterCommit(new SubjectsAddedToRoleZevent(role, subjects));
        }
    }

    /**
     * Remove subjects from this role.
     * 
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param ids The ids of the subjects to remove.
     * @throws PermissionException whoami is not allowed to perform
     *         removeSubject on this role.
     * 
     * 
     */
    public void removeSubjects(AuthzSubject whoami, Integer id, Integer[] ids) throws PermissionException {
        Role roleLocal = lookupRole(id);
        Collection<AuthzSubject> subjects = new HashSet<AuthzSubject>();
        for (int i = 0; i < ids.length; i++) {
            AuthzSubject subj = authzSubjectDAO.findById(ids[i]);
            subjects.add(subj);
            subj.removeRole(roleLocal);
        }
        if (ids!= null && ids.length > 0){
            zeventManager.enqueueEventAfterCommit(new SubjectsRemovedFromRoleZevent(roleLocal, subjects));
        }
    }

    /**
     * Find all {@link Operation} objects
     * 
     * 
     */
    @Transactional(readOnly=true)
    public Collection<Operation> findAllOperations() {
        return operationDAO.findAllOrderByName();
    }

    protected Set toPojos(Object[] vals) {
        Set ret = new HashSet();
        if (vals == null || vals.length == 0) {
            return ret;
        }
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] instanceof Operation) {
                ret.add(vals[i]);
            } else if (vals[i] instanceof ResourceValue) {
                ret.add(lookupResource((ResourceValue) vals[i]));
            } else if (vals[i] instanceof RoleValue) {
                ret.add(roleDAO.findById(((RoleValue) vals[i]).getId()));
            } else if (vals[i] instanceof ResourceGroupValue) {
                ret.add(resourceGroupDAO.findById(((ResourceGroupValue) vals[i]).getId()));
            } else {
                log.error("Invalid type.");
            }

        }
        return ret;
    }

    private Resource lookupResource(ResourceValue resource) {
        if (resource.getId() == null) {
            ResourceType type = resource.getResourceType();
            return resourceDAO.findByInstanceId(type, resource.getInstanceId());
        }
        return resourceDAO.findById(resource.getId());
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
       this.applicationContext = applicationContext;
    }

    public void checkCanModify(AuthzSubject authzSubject) throws PermissionException {
        permissionManager.check(authzSubject.getId(), AuthzConstants.authzRole, AuthzConstants.perm_modifyRole);
    }

}
