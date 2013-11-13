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
import java.util.Map;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.GroupMember;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.server.session.ResourceGroupSortField;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local interface for ResourceGroupManager.
 */
public interface ResourceGroupManager

{
    /**
     * Create a resource group. Currently no permission checking.
     * @param roles List of {@link Role}s
     * @param resources List of {@link Resource}s
     */
    public ResourceGroup createResourceGroup(AuthzSubject whoami,
                                             ResourceGroup.ResourceGroupCreateInfo cInfo,
                                             Collection<Role> roles,
                                             Collection<Resource> resources)
    throws GroupCreationException, GroupDuplicateNameException;
    

    /**
     * Find the group that has the given ID. Performs authz checking
     * @param whoami user requesting to find the group
     * @return {@link ResourceGroup} or null if it does not exist XXX scottmf,
     *         why is this method called find() but calls dao.get()???
     */
    public ResourceGroup findResourceGroupById(AuthzSubject whoami, Integer id)
        throws PermissionException;

    /**
     * Find the group that has the given ID. Does not do any authz checking
     */
    public ResourceGroup findResourceGroupById(Integer id);

    /**
     * Find the role that has the given name.
     * @param whoami user requesting to find the group
     * @param name The name of the role you're looking for.
     * @return The value-object of the role of the given name.
     * @throws PermissionException whoami does not have viewResourceGroup on the
     *         requested group
     */
    public ResourceGroup findResourceGroupByName(AuthzSubject whoami, String name)
        throws PermissionException;

    public Collection<ResourceGroup> findDeletedGroups();

    /**
     * Update some of the fundamentals of groups (name, description, location).
     * If name, description or location are null, the associated properties of
     * the passed group will not change.
     * @throws DuplicateObjectException if an attempt to rename the group would
     *         result in a group with the same name.
     */
    public ResourceGroup updateGroup(AuthzSubject whoami, ResourceGroup group, String name,
                            String description, String location) throws PermissionException,
        GroupDuplicateNameException;

    /**
     * Remove all groups compatible with the specified resource prototype.
     * @throws VetoException if another subsystem cannot allow it (for
     *         constraint reasons)
     */
    public void removeGroupsCompatibleWith(Resource proto) throws VetoException;

    /**
     * Delete the specified ResourceGroup.
     * @param whoami The current running user.
     * @param group The group to delete.
     */
    public void removeResourceGroup(AuthzSubject whoami, ResourceGroup group)
        throws PermissionException, VetoException;
    
    public void removeResourceGroup(AuthzSubject whoami, Integer groupId)
    throws PermissionException, VetoException;

    public void addResources(AuthzSubject subj, ResourceGroup group, Collection<Resource> resources)
        throws PermissionException, VetoException;
    
    public void addResources(AuthzSubject subj, ResourceGroup group, Collection<Resource> resources, boolean isDuringCalculation)
        throws PermissionException, VetoException;
    
    /**
     * Add a resource to a group by resource id and resource type
     */
    public ResourceGroup addResource(AuthzSubject whoami, ResourceGroup group, Resource resource)
        throws PermissionException, VetoException;

    /**
     * Add a resource to a collection of groups
     * 
     * @param whoami The current running user.
     * @param resource The resource
     * @param groups The groups to add to.
     * 
     * 
     */
    void addResource(AuthzSubject whoami, Resource resource, Collection<ResourceGroup> groups)
        throws PermissionException, VetoException;

    /**
     * RemoveResources from a group.
     * @param whoami The current running user.
     * @param group The group .
     * 
     */
    public void removeResources(AuthzSubject whoami, ResourceGroup group, Collection<Resource> resources) 
            throws PermissionException, VetoException;
    
    /**
     * RemoveResources from a group.
     * @param whoami The current running user.
     * @param group The group .
     * @param resources
     * @param isDuringCalculation true/false if we are in the middle of group membership calculation 
     * @throws PermissionException
     * @throws VetoException
     */
    public void removeResources(AuthzSubject whoami, ResourceGroup group,
                                Collection<Resource> resources, boolean isDuringCalculation) 
                                        throws PermissionException, VetoException;

    /**
     * Remove a resource from a collection of groups
     * 
     * @param whoami The current running user.
     * @param resource The resource
     * @param groups The groups to remove from.
     * 
     * 
     */
    public void removeResource(AuthzSubject whoami, Resource resource,
                               Collection<ResourceGroup> groups) throws PermissionException,
        VetoException;

    /**
     * Change the resource contents of a group to the specified list of
     * resources.
     * @param resources A list of {@link Resource}s to be in the group
     */
    public void setResources(AuthzSubject whoami, ResourceGroup group,
                             Collection<Resource> resources) throws PermissionException,
        VetoException;

    /**
     * List the resources in this group that the caller is authorized to see.
     * @param whoami The current running user.
     * @param groupValue This group.
     * @param pc Paging information for the request
     * @return list of authorized resources in this group.
     */
    public Collection<Resource> getResources(AuthzSubject whoami, Integer id);

    /**
     * Get all the resource groups including the root resource group.
     */
    public List<ResourceGroupValue> getAllResourceGroups(AuthzSubject subject, PageControl pc)
        throws PermissionException;

    /**
     * Get all the members of a group.
     * @return {@link Resource}s
     */
    public List<Resource> getMembers(ResourceGroup g);

    /**
     * Get all the members of {@link List} of groups NOT ORDERED
     * 
     * @return {@link Resource}s
     */
    public List<Resource> getMembers(Collection<ResourceGroup> g);
    
    /**
    * Get all the members of a group, in a Map by type.
    * 
    * @return {@link Resource}s
    * 
    */
   @Transactional(readOnly = true)
   public Map<Integer, List<Integer>> getMembersByType(ResourceGroup g);

    /**
     * Get the member type counts of a group
     */
    public Map<String, Number> getMemberTypes(ResourceGroup g);

    /**
     * Get all the groups a resource belongs to
     * @return {@link ResourceGroup}s
     */
    public Collection<ResourceGroup> getGroups(Resource r);

    /**
     * Get the # of groups within HQ inventory
     */
    public Number getGroupCount();

    /**
     * Returns true if the passed resource is a member of the given group.
     */
    public boolean isMember(ResourceGroup group, Resource resource);

    /**
     * Get the # of members in a group
     */
    public int getNumMembers(ResourceGroup g);

    /**
     * Temporary method to convert a ResourceGroup into an AppdefGroupValue
     */
    public AppdefGroupValue getGroupConvert(AuthzSubject subj, ResourceGroup g);
    
    public AppdefGroupValue getGroupConvert(AuthzSubject subj, Integer groupId);

    /**
     * Get a list of {@link ResourceGroup}s which are compatible with the
     * specified prototype. Do not return any groups contained within
     * 'excludeGroups' (a list of {@link ResourceGroup}s
     * @param prototype If specified, the resulting groups must be compatible
     *        with the prototype.
     * @param pInfo Pageinfo with a sort field of type
     *        {@link ResourceGroupSortField}
     */
    public PageList<ResourceGroup> findGroupsNotContaining(AuthzSubject subject, Resource member,
                                                           Resource prototype,
                                                           Collection<ResourceGroup> excGrps,
                                                           org.hyperic.hibernate.PageInfo pInfo);

    /**
     * Get a list of {@link ResourceGroup}s which are compatible with the
     * specified prototype. Do not return any groups contained within
     * 'excludeGroups' (a list of {@link ResourceGroup}s
     * @param prototype If specified, the resulting groups must be compatible
     *        with the prototype.
     * @param pInfo Pageinfo with a sort field of type
     *        {@link ResourceGroupSortField}
     */
    public PageList<ResourceGroup> findGroupsContaining(AuthzSubject subject, Resource member,
                                                        Collection<ResourceGroup> excludeGroups,
                                                        PageInfo pInfo);

    /**
     * Get all the resource groups excluding the root resource group.
     */
    public Collection<ResourceGroup> getAllResourceGroups(AuthzSubject subject, boolean excludeRoot)
        throws PermissionException;

    /**
     * Get all {@link ResourceGroup}s
     */
    public Collection<ResourceGroup> getAllResourceGroups();

    /**
     * Get all compatible resource groups of the given entity type and resource
     * type.
     */
    public Collection<ResourceGroup> getCompatibleResourceGroups(AuthzSubject subject,
                                                                 Resource resProto)
        throws PermissionException, NotFoundException;

    /**
     * Get the resource groups with the specified ids
     * @param ids the resource group ids
     * @param pc Paging information for the request
     */
    public PageList<ResourceGroupValue> getResourceGroupsById(AuthzSubject whoami, Integer[] ids,
                                                              PageControl pc)
        throws PermissionException;
    
    

    /**
     * Get all resource groups owned by with the specified owner
     * @param owner of resource groups
     * @throws PermissionException 
     * 
     */
    public Collection<ResourceGroup>  getResourceGroupsByOwnerId(AuthzSubject whoami) throws PermissionException;

    /**
     * Change owner of a group.
     */
    public void changeGroupOwner(AuthzSubject subject, ResourceGroup group, AuthzSubject newOwner)
        throws PermissionException;

    /**
     * Get a ResourceGroup owner's AuthzSubjectValue
     * @param gid The group id
     * @exception NotFoundException Unable to find a group by id
     */
    public AuthzSubject getResourceGroupOwner(Integer gid) throws NotFoundException;

    public ResourceGroup getResourceGroupByResource(Resource resource);

    /**
     * Set a ResourceGroup modifiedBy attribute
     * @param whoami user requesting to find the group
     * @param id The ID of the role you're looking for.
     */
    public void setGroupModifiedBy(AuthzSubject whoami, Integer id);

    public void updateGroupType(AuthzSubject subject, ResourceGroup g, int groupType,
                                int groupEntType, int groupEntResType) throws PermissionException;

    public void removeGroupsCompatibleWith(String name) throws VetoException;

    public ResourceGroup getGroupById(Integer id);

    public ResourceGroup getResourceGroupById(Integer id);

    /**
     * Checks if a group name already exists for all group types, bypasses permission checking
     * @return true if a group name exists, false otherwise
     */
    public boolean groupNameExists(String name);
    
    public void removeAllMembers(ResourceGroup group);

    /**
     * @param groupResource - Typically this param is null and the behavior is that a resource is create of authzGroup
     *  type. Only used when associating a group with an existing resource.
     */
    public ResourceGroup createResourceGroup(AuthzSubject subj, ResourceGroupCreateInfo cInfo, Resource groupResource,
                                             Collection<Role> roles)
    throws GroupCreationException, GroupDuplicateNameException;

    public List<ResourceGroup> getResourceGroupsByType(int groupType);

    public Collection<GroupMember> getOrphanedResourceGroupMembers();

    public void removeGroupMember(GroupMember m);

}
