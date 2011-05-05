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
import java.util.Set;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.domain.Role;
import org.hyperic.hq.authz.server.session.ResourceGroupSortField;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

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
                                             ResourceGroupCreateInfo cInfo,
                                             Collection<Role> roles, Collection<Resource> resources)
        throws GroupCreationException, GroupDuplicateNameException;
    

    /**
     * Find the group that has the given ID. Performs authz checking
     * @param whoami user requesting to find the group
     * @return {@link ResourceGroup} or null if it does not exist XXX scottmf,
     *         why is this method called find() but calls dao.get()???
     */
    public ResourceGroup findResourceGroupById(AuthzSubject whoami, Integer id)
        throws PermissionException;
    
    ResourceGroup findResourceGroupByName(String name);

    /**
     * Find the group that has the given ID. Does not do any authz checking
     */
    public ResourceGroup findResourceGroupById(Integer id);


    /**
     * Update some of the fundamentals of groups (name, description, location).
     * If name, description or location are null, the associated properties of
     * the passed group will not change.
     * @throws DuplicateObjectException if an attempt to rename the group would
     *         result in a group with the same name.
     */
    public void updateGroup(AuthzSubject whoami, ResourceGroup group, String name,
                            String description, String location) throws PermissionException,
        GroupDuplicateNameException;

    /**
     * Delete the specified ResourceGroup.
     * @param whoami The current running user.
     * @param group The group to delete.
     */
    public void removeResourceGroup(AuthzSubject whoami, ResourceGroup group)
        throws PermissionException, VetoException;
    
    public void removeResourceGroup(AuthzSubject whoami, Integer groupId)
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
    
    void removeResources(AuthzSubject whoami, ResourceGroup group, Collection<Resource> resources);

    /**
     * Get all the members of a group.
     * @return {@link Resource}s
     */
    public Collection<Resource> getMembers(ResourceGroup g);

    /**
     * Get the member type counts of a group
     */
    public Map<String, Number> getMemberTypes(ResourceGroup g);

    /**
     * Get the # of members in a group
     */
    public int getNumMembers(ResourceGroup g);
    
    Number getGroupCountOfType(ResourceType groupType);
     
    /**
     * Temporary method to convert a ResourceGroup into an AppdefGroupValue
     */
    public AppdefGroupValue getGroupConvert(AuthzSubject subj, ResourceGroup g);
    
    public AppdefGroupValue getGroupConvert(AuthzSubject subj, Integer groupId);
    
    AppdefGroupValue getGroupConvert(AuthzSubject subj, ResourceGroup g,boolean includeMembers);

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
                                                           ResourceType prototype,
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
                                                        org.hyperic.hibernate.PageInfo pInfo);
    
    PageList<Resource> getCompatibleGroups(PageControl pageControl);
    
    PageList<Resource> getMixedGroups(PageControl pageControl);

    /**
     * Get all the resource groups excluding the root resource group.
     */
    public Collection<ResourceGroup> getAllResourceGroups(AuthzSubject subject)
        throws PermissionException;

    /**
     * Change owner of a group.
     */
    public void changeGroupOwner(AuthzSubject subject, ResourceGroup group, AuthzSubject newOwner)
        throws PermissionException;
    
    Collection<ResourceGroup> getGroups(Resource r);
    
    PageList<ResourceGroup> getResourceGroupsById(AuthzSubject whoami, Integer[] ids,
        PageControl pc) throws PermissionException;
    
    PageList<Resource> getCompatibleGroupsContainingType(int resourceTypeId, PageControl pageControl);
    
    Collection<ResourceGroup> getCompatibleResourceGroups(AuthzSubject subject,
        int resourceTypeId);
 
}
