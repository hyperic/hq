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
import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.RoleCalendar;
import org.hyperic.hq.authz.server.session.RoleCalendarType;
import org.hyperic.hq.authz.values.OwnedRoleValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for RoleManager.
 */
public interface RoleManager {

    public boolean isRootRoleMember(AuthzSubject subject);

    /**
     * Create a role.
     * @param whoami The current running user.
     * @param role The to be created.
     * @param operations Operations to associate with the new role. Use null if
     *        you want to associate operations later.
     * @param subjectIds Ids of subjects to add to the new role. Use null to add
     *        subjects later.
     * @param groupIds Ids of resource groups to add to the new role. Use null
     *        to add subjects later.
     * @return OwnedRoleValue for the role.
     * @throws PermissionException whoami may not perform createResource on the
     *         covalentAuthzRole ResourceType.
     */
    public Integer createOwnedRole(AuthzSubject whoami, RoleValue role,
                                   org.hyperic.hq.authz.server.session.Operation[] operations,
                                   java.lang.Integer[] subjectIds, java.lang.Integer[] groupIds)
        throws AuthzDuplicateNameException, PermissionException;

    /**
     * Delete the specified role.
     * @param whoami The current running user.
     * @param role The role to delete.
     * 
     */
    public void removeRole(AuthzSubject whoami, Integer rolePk) throws PermissionException, ApplicationException;

    /**
     * Write the specified entity out to permanent storage.
     * @param whoami The current running user.
     * @param role The role to save.
     * @throws PermissionException whoami may not perform modifyRole on this
     *         role.
     */
    public void saveRole(AuthzSubject whoami, RoleValue role)
        throws org.hyperic.hq.authz.shared.AuthzDuplicateNameException, PermissionException;

    /**
     * Change the owner of the role.
     * @param whoami The current running user.
     * @param id The ID of the role to change
     * @param ownerVal The new owner of the role..
     * @throws PermissionException whoami may not perform modifyRole on this
     *         role.
     */
    public void changeOwner(AuthzSubject whoami, Integer id, AuthzSubject owner) throws PermissionException;

    /**
     * Associate operations with this role.
     * @param whoami The current running user.
     * @param role The role.
     * @param operations The operations to associate with the role.
     * @throws PermissionException whoami may not perform addOperation on this
     *         role.
     */
    public void addOperations(AuthzSubject whoami, Role role, org.hyperic.hq.authz.server.session.Operation[] operations)
        throws PermissionException;

    /**
     * Disassociate all operations from this role.
     * @param whoami The current running user.
     * @param role The role.
     * @throws PermissionException whoami may not perform removeOperation on
     *         this role.
     */
    public void removeAllOperations(AuthzSubject whoami, Role role) throws PermissionException;

    /**
     * Set the operations for this role. To get the operations call
     * getOperations() on the value-object.
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param operations Operations to associate with this role.
     
     * @throws PermissionException whoami is not allowed to perform
     *         setOperations on this role.
     */
    public void setOperations(AuthzSubject whoami, Integer id,
                              org.hyperic.hq.authz.server.session.Operation[] operations) throws PermissionException;

    /**
     * Associate ResourceGroups with this role.
     * @param whoami The current running user.
     * @param role This role.
     * @param gids The ids of the groups to associate with this role.
     
     * @throws PermissionException whoami is not allowed to perform
     *         addResourceGroup on this role.
     */
    public void addResourceGroups(AuthzSubject whoami, Integer rid, java.lang.Integer[] gids)
        throws PermissionException;

    /**
     * Associate ResourceGroup with list of roles.
     * @param whoami The current running user.
     * @param roles The roles.
     * @param ids The id of the group to associate with the roles.
     * @throws PermissionException whoami is not allowed to perform
     *         addResourceGroup on this role.
     *
     */
    public void addResourceGroupRoles(AuthzSubject whoami, Integer gid, java.lang.Integer[] ids)
        throws PermissionException;
    
    /**
     * Associate ResourceGroup with list of roles.
     * @param whoami The current running user.
     * @param roles The roles.
     * @param ids The id of the group to associate with the roles.
     * @param isDuringCalculation true/false if we are in the middle of group membership calculation
     * @throws PermissionException whoami is not allowed to perform
     *         addResourceGroup on this role.
     *
     */
    public void addResourceGroupRoles(AuthzSubject whoami, Integer gid, java.lang.Integer[] ids, 
            boolean isDuringCalculation) throws PermissionException;


    /**
     * Disassociate ResourceGroups from this role.
     * @param whoami The current running user.
     * @param id This role.
     * @param gids The ids of the groups to disassociate.
     * 
     * @throws PermissionException whoami is not allowed to perform modifyRole
     *         on this role.
     */
    public void removeResourceGroups(AuthzSubject whoami, Integer id, java.lang.Integer[] gids)
        throws PermissionException;

    /**
     * Disassociate roles from this ResourceGroup.
     * @param whoami The current running user.
     * @param role This role.
     * @param ids The ids of the groups to disassociate.
     *
     * @throws PermissionException whoami is not allowed to perform modifyRole
     *         on this role.
     */
    public void removeResourceGroupRoles(AuthzSubject whoami, Integer gid, java.lang.Integer[] ids)
        throws PermissionException;
    
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
            throws PermissionException;

    /**
     * Disassociate all ResourceGroups of this role from this role.
     * @param whoami The current running user.
     * @param role This role.
     * 
     * 
     * @throws PermissionException whoami is not allowed to perform modifyRole
     *         on this role.
     */
    public void removeAllResourceGroups(AuthzSubject whoami, Role role) throws PermissionException;

    /**
     * Get the # of roles within HQ inventory
     */
    public Number getRoleCount();

    /**
     * Get the # of subjects within HQ inventory
     */
    public Number getSubjectCount();

    /**
     * Get a Role by id
     */
    public Role getRoleById(int id);

    public Role findRoleById(int id);

    public Role findRoleByName(String name);

    /**
     * Create a calendar under a role for a specific type. Calendars created in
     * this manner are tied directly to the role and should not be used by other
     * roles.
     * @throws PermissionException if user is not allowed to modify role
     */
    public RoleCalendar createCalendar(AuthzSubject whoami, Role r, String calendarName, RoleCalendarType type)
        throws PermissionException;

    public boolean removeCalendar(RoleCalendar c);

    /**
     * Find the owned role that has the given ID.
     * @param id The ID of the role you're looking for.
     * @return The owned value-object of the role of the given ID.
     * 
     */
    public OwnedRoleValue findOwnedRoleById(AuthzSubject whoami, Integer id) throws PermissionException;

    /**
     * Get operations For a given role id, find the resource types and
     * permissions which are supported by it
     * 
     * @param subject
     * @param roleId
     * @return list - values are lists of operation
     * 
     */
    public List<Operation> getRoleOperations(AuthzSubject subject, Integer roleId)
        throws PermissionException;

    public Collection<Role> getAllRoles();

    /**
     * List all Roles in the system
     * @param pc Paging information for the request
     * @return List a list of RoleValues
     */
    public List<RoleValue> getAllRoles(AuthzSubject subject, PageControl pc);

    /**
     * List all OwnedRoles in the system
     * @param subject
     * @param pc Paging and sorting information.
     * @return List a list of OwnedRoleValues
     */
    public List<OwnedRoleValue> getAllOwnedRoles(AuthzSubject subject, PageControl pc);

    /**
     * List all Roles in the system, except system roles.
     * @return List a list of OwnedRoleValues that are not system roles
     * @throws NotFoundException if sort attribute is unrecognized
     */
    public PageList<OwnedRoleValue> getAllNonSystemOwnedRoles(AuthzSubject subject, java.lang.Integer[] excludeIds,
                                                              PageControl pc) throws PermissionException, NotFoundException;

    /**
     * Get the roles with the specified ids
     * @param subject
     * @param ids the role ids
     * @param pc Paging information for the request
     * 
     * @throws PermissionException
     */
    public PageList<RoleValue> getRolesById(AuthzSubject whoami, java.lang.Integer[] ids, PageControl pc)
        throws PermissionException;

    /**
     * Associate roles with this subject.
     * @param whoami The current running user.
     * @param subject The subject.
     * @param roles The roles to associate with the subject.
     * @throws PermissionException whoami may not perform addRole on this
     *         subject.
     */
    public void addRoles(AuthzSubject whoami, AuthzSubject subject, java.lang.Integer[] roles)
        throws PermissionException;

    /**
     * Disassociate roles from this subject.
     * @param whoami The current running user.
     * @param subject The subject.
     * @param roles The subjects to disassociate.
     * @throws PermissionException whoami may not perform removeRole on this
     *         subject.
     */
    public void removeRoles(AuthzSubject whoami, AuthzSubject subject, java.lang.Integer[] roles)
        throws PermissionException;

    /**
     * Get the roles for a subject
     * @param whoami
     * @param subject
     * @param pc Paging and sorting information.
     * @return Set of Roles
     */
    public List<RoleValue> getRoles(AuthzSubject subjectValue, PageControl pc) throws PermissionException;

    /**
     * Get the owned roles for a subject.
     * @param whoami
     * @param subject
     * @param pc Paging and sorting information.
     * @return Set of Roles
     */
    public List<OwnedRoleValue> getOwnedRoles(AuthzSubject subject, PageControl pc) throws PermissionException;

    /**
     * Get the owned roles for a subject, except system roles.
     * @param callerSubjectValue is the subject of caller.
     * @param intendedSubjectValue is the subject of intended subject.
     * @param pc The PageControl object for paging results.
     * @return List a list of OwnedRoleValues that are not system roles
     * 
     * 
     * @throws PermissionException caller is not allowed to perform listRoles on
     *         this role.
     * 
     */
    public PageList<OwnedRoleValue> getNonSystemOwnedRoles(AuthzSubject callerSubjectValue,
                                                           AuthzSubject intendedSubjectValue, PageControl pc)
        throws PermissionException;

    /**
     * Get the owned roles for a subject, except system roles.
     * @param callerSubjectValue is the subject of caller.
     * @param intendedSubjectValue is the subject of intended subject.
     * @param pc The PageControl object for paging results.
     * @return List a list of OwnedRoleValues that are not system roles
     *
     * 
     * @throws PermissionException caller is not allowed to perform listRoles on
     *         this role.
     * 
     */
    public PageList<OwnedRoleValue> getNonSystemOwnedRoles(AuthzSubject callerSubjectValue,
                                                           AuthzSubject intendedSubjectValue,
                                                           java.lang.Integer[] excludeIds, PageControl pc)
        throws PermissionException;

    /**
     * List the roles that this subject is not in and that are not one of the
     * specified roles.
     * @param whoami The current running user.
     * @param system If true, then only system roles are returned. If false,
     *        then only non-system roles are returned.
     * @param subjectId The id of the subject.
     * @return List of roles.
     * @throws NotFoundException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform listRoles on
     *         this role.
     * 
     */
    public PageList<RoleValue> getAvailableRoles(AuthzSubject whoami, boolean system, Integer subjectId,
                                                 java.lang.Integer[] roleIds, PageControl pc)
        throws PermissionException, NotFoundException;

    /**
     * List the roles that this subject is not in and that are not one of the
     * specified roles.
     * @param whoami The current running user.
     * @param system If true, then only system roles are returned. If false,
     *        then only non-system roles are returned.
     * @param groupId The id of the subject.
     * @return List of roles.
     * @throws NotFoundException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform listRoles on
     *         this role.
     * @throws NotFoundException if the sort attribute was not recognized
     */
    public PageList<RoleValue> getAvailableGroupRoles(AuthzSubject whoami, Integer groupId,
                                                      java.lang.Integer[] roleIds, PageControl pc)
        throws PermissionException, NotFoundException;
    
    Collection<ResourceGroup> getResourceGroupsByRole(AuthzSubject subject,Role role)
        throws PermissionException, NotFoundException;
    
    Collection<ResourceGroup> getResourceGroupsByRoleAndGroupType(AuthzSubject subject,Role role, int groupType)
            throws PermissionException, NotFoundException;    

    /**
     * Get the resource groups applicable to a given role
     */
    public PageList<ResourceGroupValue> getResourceGroupsByRoleIdAndSystem(AuthzSubject subject, Integer roleId,
                                                                           boolean system, PageControl pc)
        throws PermissionException, NotFoundException;

    /**
     * Return the roles of a group
     * @throws PermissionException
     */
    public PageList<RoleValue> getResourceGroupRoles(AuthzSubject whoami, Integer groupId, PageControl pc)
        throws PermissionException;

    /**
     * List the groups not in this role and not one of the specified groups.
     * @param whoami The current running user.
     * @param roleId The id of the role.
     * @return List of groups in this role.
     * @throws PermissionException whoami is not allowed to perform listGroups
     *         on this role.
     * @throws NotFoundException
     */
    public PageList<ResourceGroupValue> getAvailableResourceGroups(AuthzSubject whoami, Integer roleId,
                                                                   java.lang.Integer[] groupIds, PageControl pc)
        throws PermissionException, NotFoundException;

    /**
     * List the subjects in this role.
     * @param whoami The current running user.
     * @param roleId The id of the role.
     * @return List of subjects in this role.
     * @throws PermissionException whoami is not allowed to perform listSubjects
     *         on this role.
     * @throws NotFoundException if the sort attribute is not recognized
     */
    public PageList<AuthzSubjectValue> getSubjects(AuthzSubject whoami, Integer roleId, PageControl pc)
        throws PermissionException, NotFoundException;

    /**
     * List the subjects not in this role and not one of the specified subjects.
     * @param whoami The current running user.
     * @param roleId The id of the role.
     * @return List of subjects in this role.
     * @throws NotFoundException Unable to find a given or dependent entities.
     * @throws PermissionException whoami is not allowed to perform listSubjects
     *         on this role.
     * @throws NotFoundException if the sort attribute is not recognized
     */
    public PageList<AuthzSubjectValue> getAvailableSubjects(AuthzSubject whoami, Integer roleId,
                                                            java.lang.Integer[] subjectIds, PageControl pc)
        throws PermissionException, NotFoundException;

    /**
     * Add subjects to this role.
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param sids Ids of ubjects to add to role.
     * @throws PermissionException whoami is not allowed to perform addSubject
     *         on this role.
     */
    public void addSubjects(AuthzSubject whoami, Integer id, java.lang.Integer[] sids) throws PermissionException;

    /**
     * Remove subjects from this role.
     * @param whoami The current running user.
     * @param id The ID of the role.
     * @param ids The ids of the subjects to remove.
     * @throws PermissionException whoami is not allowed to perform
     *         removeSubject on this role.
     */
    public void removeSubjects(AuthzSubject whoami, Integer id, java.lang.Integer[] ids) throws PermissionException;

    /**
     * Find all {@link Operation} objects
     */
    public Collection<Operation> findAllOperations();

    public Collection<Role> getRoles(AuthzSubject subj);

    public void checkCanModify(AuthzSubject authzSubject) throws PermissionException;

}
