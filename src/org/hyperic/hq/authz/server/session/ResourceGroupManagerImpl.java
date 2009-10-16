/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.FinderException;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.GroupTypeValue;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.grouping.CritterList;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.grouping.shared.GroupEntry;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use this session bean to manipulate ResourceGroups,
 *
 * All arguments and return values are value-objects.
 *
 */
@Transactional
@Service
public class ResourceGroupManagerImpl
    extends AuthzSession
    implements ResourceGroupManager
{
    private Pager _groupPager;
    private Pager _ownedGroupPager;
    private final String GROUP_PAGER =
        PagerProcessor_resourceGroup.class.getName();
    private final String OWNEDGROUP_PAGER =
        PagerProcessor_ownedResourceGroup.class.getName();

   
    private GroupChangeCallback groupChangeCallback;

   
    private ResourceEdgeDAO resourceEdgeDAO;
    
    private AuthzSubjectManagerLocal authzSubjectManager;
    private EventLogManagerLocal eventLogManager;
    
    private ResourceManager resourceManager;
    private PlatformManagerLocal platformManager;
    private ServerManagerLocal serverManager;
    private ServiceManagerLocal serviceManager;
    private ApplicationManagerLocal applicationManager;

    
    @Autowired
    public ResourceGroupManagerImpl( ResourceEdgeDAO resourceEdgeDAO, AuthzSubjectManagerLocal authzSubjectManager, EventLogManagerLocal eventLogManager,
                                     PlatformManagerLocal platformManager, ServerManagerLocal serverManager, ServiceManagerLocal serviceManager, ApplicationManagerLocal applicationManager,
                                     ResourceManager resourceManager) {
        //this.groupChangeCallback = groupChangeCallback;
        this.resourceEdgeDAO = resourceEdgeDAO;
        this.authzSubjectManager = authzSubjectManager;
        this.eventLogManager = eventLogManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.applicationManager = applicationManager;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
            _groupPager      = Pager.getPager(GROUP_PAGER);
            _ownedGroupPager = Pager.getPager(OWNEDGROUP_PAGER);
    }

    /**
     * Create a resource group.  Currently no permission checking.
     *
     * @param roles     List of {@link Role}s
     * @param resources List of {@link Resource}s
     *
     * 
     */
    public ResourceGroup createResourceGroup(AuthzSubject whoami,
                                             ResourceGroupCreateInfo cInfo,
                                             Collection<Role> roles,
                                             Collection<Resource> resources)
        throws GroupCreationException, GroupDuplicateNameException
    {
        ResourceGroup existing =
            resourceGroupDAO.findByName(cInfo.getName());

        if (existing != null) {
            throw new GroupDuplicateNameException("Group by the name [" +
                                                  cInfo.getName() +
                                                  "] already exists");
        }

        ResourceGroup res = resourceGroupDAO.create(whoami, cInfo, resources, roles);

        resourceEdgeDAO
            .create(res.getResource(), res.getResource(), 0,
                    getContainmentRelation());  // Self-edge

        //groupChangeCallback.postGroupCreate(res);
        return res;
    }

    /**
     * Find the group that has the given ID.  Performs authz checking
     * @param whoami user requesting to find the group
     * @return {@link ResourceGroup} or null if it does not exist
     * XXX scottmf, why is this method called find() but calls dao.get()???
     * 
     */
    public ResourceGroup findResourceGroupById(AuthzSubject whoami, Integer id)
        throws PermissionException
    {
        ResourceGroup group = resourceGroupDAO.get(id);
        if (group == null) {
            return null;
        }
        checkGroupPermission(
            whoami, group.getId(), AuthzConstants.perm_viewResourceGroup);
        return group;
    }

    private void checkGroupPermission(AuthzSubject whoami, Integer group,
                                      Integer op)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), AuthzConstants.authzGroup, group, op);
    }

    /**
     * Find the group that has the given ID.  Does not do any authz checking
     * 
     */
    public ResourceGroup findResourceGroupById(Integer id) {
        return resourceGroupDAO.findById(id);
    }

    /**
     * Find the role that has the given name.
     * @param whoami user requesting to find the group
     * @param name The name of the role you're looking for.
     * @return The value-object of the role of the given name.
     * @throws PermissionException whoami does not have viewResourceGroup
     * on the requested group
     * 
     */
    public ResourceGroup findResourceGroupByName(AuthzSubject whoami,
                                                 String name)
        throws PermissionException
    {
        ResourceGroup group = resourceGroupDAO.findByName(name);

        if (group == null) {
            return null;
        }

        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_viewResourceGroup);
        return group;
    }

    /**
     * 
     */
    public Collection<ResourceGroup> findDeletedGroups() {
        return resourceGroupDAO.findDeletedGroups();
    }

    /**
     * Update some of the fundamentals of groups (name, description, location).
     * If name, description or location are null, the associated properties
     * of the passed group will not change.
     *
     * @throws DuplicateObjectException if an attempt to rename the group would
     *                                  result in a group with the same name.
     * 
     */
    public void updateGroup(AuthzSubject whoami, ResourceGroup group,
                            String name, String description, String location)
        throws PermissionException, GroupDuplicateNameException
    {
        checkGroupPermission(whoami, group.getId(),
                             AuthzConstants.perm_modifyResourceGroup);

        // XXX:  Add Auditing
        if (name != null && !name.equals(group.getName())) {
            ResourceGroup existing = resourceGroupDAO.findByName(name);

            if (existing != null) {
                throw new GroupDuplicateNameException("Group by that name [" +
                                                      name + "] already exists");
            }
            group.setName(name);
            group.getResource().setName(name);
        }

        if (description != null && !description.equals(group.getDescription())){
            group.setDescription(description);
        }

        if (location != null && !location.equals(group.getLocation())) {
            group.setLocation(location);
        }
    }

    /**
     * Remove all groups compatable with the specified resource prototype.
     *
     * @throws VetoException if another subsystem cannot allow it (for
     *                       constraint reasons)
     * 
     */
    public void removeGroupsCompatibleWith(Resource proto)
        throws VetoException
    {
        AuthzSubject overlord =
            authzSubjectManager.getOverlordPojo();

        for (ResourceGroup group : getAllResourceGroups()) {
            if (group.isCompatableWith(proto)) {
                try {
                    removeResourceGroup(overlord, group);
                } catch(PermissionException exc) {
                    log.warn("Perm denied while deleting group [" +
                             group.getName() + " id=" + group.getId() + "]",
                             exc);
                }
            }
        }
    }

    /**
     * Delete the specified ResourceGroup.
     * @param whoami The current running user.
     * @param group The group to delete.
     * 
     */
    public void removeResourceGroup(AuthzSubject whoami, ResourceGroup group)
        throws PermissionException, VetoException
    {
        checkGroupPermission(whoami, group.getId(),
                             AuthzConstants.perm_removeResourceGroup);
        // TODO scottmf, this should be invoking a pre-transaction callback
        eventLogManager.deleteLogs(group.getResource());

        GroupingStartupListener.getCallbackObj().preGroupDelete(group);
        resourceGroupDAO.remove(group);

        resourceGroupDAO.getSession().flush();

        // Send resource delete event
        ResourceDeletedZevent zevent =
            new ResourceDeletedZevent(whoami,
                                      AppdefEntityID.newGroupID(group.getId()));
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
    }

    /**
     * 
     */
    public void addResources(AuthzSubject subj, ResourceGroup group,
                             List<Resource> resources)
        throws PermissionException
    {
        checkGroupPermission(subj, group.getId(),
                             AuthzConstants.perm_modifyResourceGroup);
        addResources(group, resources);
    }

    private void addResources(ResourceGroup group, List<Resource> resources) {
        resourceGroupDAO.addMembers(group, resources);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(group);
    }

    /**
     * Add a resource to a group by resource id and resource type
     * 
     */
    public ResourceGroup addResource(AuthzSubject whoami, ResourceGroup group,
                                     Resource resource)
        throws PermissionException
    {
        checkGroupPermission(whoami, group.getId(),
                             AuthzConstants.perm_modifyResourceGroup);

        resourceGroupDAO.addMembers(group,
                                         Collections.singleton(resource));
        GroupingStartupListener.getCallbackObj().groupMembersChanged(group);
        return group;
    }

    /**
     * RemoveResources from a group.
     * @param whoami The current running user.
     * @param group The group .
     * 
     */
    public void removeResources(AuthzSubject whoami,
                                ResourceGroup group,
                                Collection<Resource> resources)
        throws PermissionException
    {
        checkGroupPermission(whoami, group.getId(),
                             AuthzConstants.perm_modifyResourceGroup);

        resourceGroupDAO.removeMembers(group, resources);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(group);
    }

    /**
     * Sets the criteria list for this group.
     * @param whoami The current running user.
     * @param group This group.
     * @param critters List of critters to associate with this resource group.
     * @throws PermissionException whoami does not own the resource.
     * @throws GroupException critters is not a valid list of criteria.
     * 
     */
    public void setCriteria(AuthzSubject whoami, ResourceGroup group,
                            CritterList critters)
        throws PermissionException, GroupException
   {
        checkGroupPermission(whoami, group.getId(),
                             AuthzConstants.perm_modifyResourceGroup);

        group.setCritterList(critters);
    }

    /**
     * Change the resource contents of a group to the specified list
     * of resources.
     *
     * @param resources  A list of {@link Resource}s to be in the group
     * 
     */
    public void setResources(AuthzSubject whoami,
                             ResourceGroup group, Collection<Resource> resources)
        throws PermissionException
    {
        checkGroupPermission(whoami, group.getId(),
                             AuthzConstants.perm_modifyResourceGroup);

        resourceGroupDAO.setMembers(group, resources);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(group);
    }

    /**
     * List the resources in this group that the caller is authorized to see.
     * @param whoami The current running user.
     * @param groupValue This group.
     * @param pc Paging information for the request
     * @return list of authorized resources in this group.
     * 
     */
    public Collection<Resource> getResources(AuthzSubject whoami, Integer id)
    {
        return PermissionManagerFactory.getInstance()
            .getGroupResources(whoami.getId(), id, Boolean.FALSE);
    }

    /**
     * Get all the resource groups including the root resource group.
     * 
     */
    public List<ResourceGroup> getAllResourceGroups(AuthzSubject subject, PageControl pc)
        throws PermissionException, FinderException
    {
        return getAllResourceGroups(subject, pc, false);
    }

    /**
     * Get all the members of a group.
     *
     * @return {@link Resource}s
     * 
     */
    public Collection<Resource> getMembers(ResourceGroup g) {
        return resourceGroupDAO.getMembers(g);
    }

    /**
     * Get the member type counts of a group
     * 
     */
    public Map<String, Integer> getMemberTypes(ResourceGroup g) {
        return resourceGroupDAO.getMemberTypes(g);
    }

    /**
     * Get all the groups a resource belongs to
     *
     * @return {@link ResourceGroup}s
     * 
     */
    public Collection<ResourceGroup> getGroups(Resource r) {
        return resourceGroupDAO.getGroups(r);
    }

    /**
     * Get the # of groups within HQ inventory
     * 
     */
    public Number getGroupCount() {
        return new Integer(resourceGroupDAO.size());
    }

    /**
     * Returns true if the passed resource is a member of the given group.
     * 
     */
    public boolean isMember(ResourceGroup group, Resource resource) {
        return resourceGroupDAO.isMember(group, resource);
    }


    /**
     * Get the # of members in a group
     * 
     */
    public int getNumMembers(ResourceGroup g) {
        return getMembers(g).size();
    }


    /**
     * Temporary method to convert a ResourceGroup into an AppdefGroupValue
     * 
     */
    public AppdefGroupValue getGroupConvert(AuthzSubject subj, ResourceGroup g)
    {
        AppdefGroupValue retVal = new AppdefGroupValue();
        Collection<Resource> members = getMembers(g);

        // Create our return group vo
        retVal.setId(g.getId());
        retVal.setName(g.getName());
        retVal.setDescription(g.getDescription());
        retVal.setLocation(g.getLocation());
        retVal.setGroupType(g.getGroupType().intValue());
        retVal.setGroupEntType(g.getGroupEntType().intValue());
        retVal.setGroupEntResType(g.getGroupEntResType().intValue());
        retVal.setTotalSize(members.size());
        retVal.setSubject(subj);
        retVal.setClusterId(g.getClusterId().intValue());
        retVal.setMTime(new Long(g.getMtime()));
        retVal.setCTime(new Long(g.getCtime()));
        retVal.setModifiedBy(g.getModifiedBy());
        retVal.setOwner(g.getResource().getOwner().getName());

        // Add the group members
        for (Resource r : members) {
            if (r.getResourceType() != null) {
                GroupEntry ge = new GroupEntry(r.getInstanceId(),
                                               r.getResourceType().getName());
                retVal.addEntry(ge);
            }
        }

        retVal.setAppdefResourceTypeValue(getAppdefResourceTypeValue(subj, g));
        return retVal;
    }

    /**
     * 
     */
    public AppdefResourceType
        getAppdefResourceType(AuthzSubject subject, ResourceGroup group) {
        if (group.isMixed())
            throw new IllegalArgumentException("Group " + group.getId() +
                                               " is a mixed group");
        return getResourceTypeById(group.getGroupEntType().intValue(),
                                       group.getGroupEntResType().intValue());
    }

    private AppdefResourceTypeValue
        getAppdefResourceTypeValue(AuthzSubject subject, ResourceGroup group)
    {
        if (group.isMixed()) {
            AppdefResourceTypeValue res = new GroupTypeValue();
            int iGrpType = group.getGroupType().intValue();
            res.setId(group.getGroupType());
            res.setName(AppdefEntityConstants.getAppdefGroupTypeName(iGrpType));
            return res;
        } else {
            return getAppdefResourceType(subject, group)
                .getAppdefResourceTypeValue();
        }
    }

    private AppdefResourceType getResourceTypeById (int type, int id){
        switch (type) {
        case (AppdefEntityConstants.APPDEF_TYPE_PLATFORM) :
            return getPlatformTypeById(id);
        case (AppdefEntityConstants.APPDEF_TYPE_SERVER) :
            return getServerTypeById(id);
        case (AppdefEntityConstants.APPDEF_TYPE_SERVICE) :
            return getServiceTypeById(id);
        case (AppdefEntityConstants.APPDEF_TYPE_APPLICATION) :
            return getApplicationTypeById(id);
        default:
            throw new IllegalArgumentException("Invalid resource type:" +type);
        }
    }

    private PlatformType getPlatformTypeById (int id) {
        return platformManager.findPlatformType(new Integer(id));
    }

    private ServerType getServerTypeById (int id) {
        return serverManager.findServerType(new Integer(id));
    }

    private ServiceType getServiceTypeById(int id) {
        return serviceManager.findServiceType(new Integer(id));
    }

    private ApplicationType getApplicationTypeById (int id) {
        return applicationManager.findApplicationType(new Integer(id));
    }

    /**
     * Get a list of {@link ResourceGroup}s which are compatible with
     * the specified prototype.
     *
     * Do not return any groups contained within 'excludeGroups' (a list of
     * {@link ResourceGroup}s
     *
     * @param prototype  If specified, the resulting groups must be compatible
     *                   with the prototype.
     *
     * @param pInfo Pageinfo with a sort field of type
     *              {@link ResourceGroupSortField}
     *
     * 
     * */
    public PageList<ResourceGroup> findGroupsNotContaining(AuthzSubject subject,
                                            Resource member, Resource prototype,
                                            Collection<ResourceGroup> excGrps, PageInfo pInfo)
    {
        return resourceGroupDAO.findGroupsClusionary(subject, member,
                                                          prototype, excGrps,
                                                          pInfo, false);
    }

    /**
     * Get a list of {@link ResourceGroup}s which are compatible with
     * the specified prototype.
     *
     * Do not return any groups contained within 'excludeGroups' (a list of
     * {@link ResourceGroup}s
     *
     * @param prototype  If specified, the resulting groups must be compatible
     *                   with the prototype.
     *
     * @param pInfo Pageinfo with a sort field of type
     *              {@link ResourceGroupSortField}
     *
     * 
     * */
    
    public PageList<ResourceGroup> findGroupsContaining(AuthzSubject subject,
                                         Resource member,
                                         Collection<ResourceGroup> excludeGroups,
                                         PageInfo pInfo)
    {
        return resourceGroupDAO.findGroupsClusionary(subject, member,
                                                          null, excludeGroups,
                                                          pInfo, true);
    }

    /**
     * Get all the resource groups excluding the root resource group.
     * 
     */
    public Collection<ResourceGroup> getAllResourceGroups(AuthzSubject subject,
                                           boolean excludeRoot)
        throws PermissionException
    {
        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List<Integer> groupIds;

        /**
         * XXX:  Seems this could be optimized to actually get the real
         *       list of viewable resource groups instead of going through
         *       the perm manager to get the IDs
         */
        try {
            // TODO: G
            groupIds = pm.findOperationScopeBySubject(subject,
                                        AuthzConstants.groupOpViewResourceGroup,
                                        AuthzConstants.groupResourceTypeName);
        } catch(FinderException e) {
            // Makes no sense
            throw new SystemException(e);
        }

        // now build a collection for all of them
        Collection<ResourceGroup> groups = new ArrayList<ResourceGroup>();
        for(int i = 0; i < groupIds.size(); i++) {
            ResourceGroup rgloc = resourceGroupDAO.findById((Integer) groupIds.get(i));
            if (excludeRoot) {
                String name = rgloc.getName();
                if (!name.equals(AuthzConstants.groupResourceTypeName)
                    && !name.equals(AuthzConstants.rootResourceGroupName))
                    groups.add(rgloc);
            } else {
                groups.add(rgloc);
            }
        }

        return groups;
    }

    /**
     * Get all {@link ResourceGroup}s
     *
     * 
     */
    public Collection<ResourceGroup> getAllResourceGroups() {
        return resourceGroupDAO.findAll();
    }


    /**
     * Get all compatible resource groups of the given entity type and
     * resource type.
     *
     * 
     */
    public Collection<ResourceGroup> getCompatibleResourceGroups(AuthzSubject subject,
                                                  Resource resProto)
        throws FinderException, PermissionException
    {
        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List<Integer> groupIds =
            // TODO: G
            pm.findOperationScopeBySubject(subject,
                                           AuthzConstants.groupOpViewResourceGroup,
                                           AuthzConstants.groupResourceTypeName);

        Collection<ResourceGroup> groups = resourceGroupDAO.findCompatible(resProto);
        for (Iterator<ResourceGroup> i = groups.iterator(); i.hasNext(); ) {
            ResourceGroup g = (ResourceGroup)i.next();
            if (!groupIds.contains(g.getId())) {
                i.remove();
            }
        }

        return groups;
    }

    /**
     * Get all the resource groups excluding the root resource group and paged
     */
    private PageList<ResourceGroup> getAllResourceGroups(AuthzSubject subject,
                                         PageControl pc,
                                         boolean excludeRoot)
        throws PermissionException, FinderException
    {
        Collection<ResourceGroup> groups = getAllResourceGroups(subject, excludeRoot);
        // TODO: G
        return _ownedGroupPager.seek(groups, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get the resource groups with the specified ids
     * @param ids the resource group ids
     * @param pc Paging information for the request
     * 
     */
    public PageList<ResourceGroup> getResourceGroupsById(AuthzSubject whoami,
                                          Integer[] ids,
                                          PageControl pc)
        throws PermissionException, FinderException
    {
        if (ids.length == 0)
            return new PageList<ResourceGroup>();

        PageControl allPc = new PageControl();
        // get all roles, sorted but not paged
        allPc.setSortattribute(pc.getSortattribute());
        allPc.setSortorder(pc.getSortorder());
        Collection<ResourceGroup> all = getAllResourceGroups(whoami, false);

        // build an index of ids
        HashSet<Integer> index = new HashSet<Integer>();
        index.addAll(Arrays.asList(ids));
        int numToFind = index.size();

        // find the requested roles
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>(numToFind);
        Iterator<ResourceGroup> i = all.iterator();
        while (i.hasNext() && groups.size() < numToFind) {
            ResourceGroup g = (ResourceGroup) i.next();
            if (index.contains(g.getId()))
                groups.add(g);
        }

        // TODO: G
        PageList<ResourceGroup> plist =
            _groupPager.seek(groups, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(groups.size());

        return plist;
    }

    /**
     * Change owner of a group.
     *
     * 
     */
    public void changeGroupOwner(AuthzSubject subject, ResourceGroup group,
                                 AuthzSubject newOwner)
        throws PermissionException
    {
        resourceManager.setResourceOwner(subject,
                                                         group.getResource(),
                                                         newOwner);
        group.setModifiedBy(newOwner.getName());
    }

    /**
     * Get a ResourceGroup owner's AuthzSubjectValue
     * @param gid The group id
     * @exception FinderException Unable to find a group by id
     * 
     */
    public AuthzSubject getResourceGroupOwner(Integer gid)
        throws FinderException
    {
        Resource gResource =
            resourceManager.findResourceByInstanceId(resourceManager.findResourceTypeByName(
                AuthzConstants.groupResourceTypeName), gid);
        return gResource.getOwner();
    }

    /**
     * 
     */
    public ResourceGroup getResourceGroupByResource(Resource resource) {
        return resourceGroupDAO.findResourceGroup(resource);
    }

    /**
     * Set a ResourceGroup modifiedBy attribute
     * @param whoami user requesting to find the group
     * @param id The ID of the role you're looking for.
     * 
     */
    public void setGroupModifiedBy(AuthzSubject whoami, Integer id) {
        ResourceGroup groupLocal = resourceGroupDAO.findById(id);
        groupLocal.setModifiedBy(whoami.getName());
    }


    /**
     * 
     */
    public void updateGroupType(AuthzSubject subject, ResourceGroup g,
                                int groupType, int groupEntType,
                                int groupEntResType)
        throws PermissionException
    {
        checkGroupPermission(subject, g.getId(),
                             AuthzConstants.perm_modifyResourceGroup);

        g.setGroupType(new Integer(groupType));

        if (groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS ||
            groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC) {
            Resource r = findPrototype(new AppdefEntityTypeID(groupEntType,
                                                              groupEntResType));
            g.setResourcePrototype(r);
        }
    }

    /**
     * Get the maximum collection interval for a scheduled metric within a
     * compatible group of resources.
     *
     * @return The maximum collection time in milliseconds.
     * TODO:  This does not belong here.  Evict, evict!  -- JMT 04/01/08
     * 
     */
    public long getMaxCollectionInterval(ResourceGroup g, Integer templateId) {
        Long max =
            resourceGroupDAO.getMaxCollectionInterval(g, templateId);

        if (max == null) {
            throw new IllegalArgumentException("Invalid template id =" +
                                               templateId + " for resource " +
                                               "group " + g.getId());
        }

        return max.longValue();
    }

    /**
     * Return a List of Measurements that are collecting for the given
     * template ID and group.
     *
     * @param g The group in question.
     * @param templateId The measurement template to query.
     * @return templateId A list of Measurement objects with the given template
     * id in the group that are set to be collected.
     *
     * TODO:  This does not belong here.  Evict, evict!  -- JMT 04/01/08
     * 
     */
    public List<Measurement> getMetricsCollecting(ResourceGroup g, Integer templateId) {

        return resourceGroupDAO.getMetricsCollecting(g, templateId);
    }

    public static ResourceGroupManager getOne() {
      return Bootstrap.getBean(ResourceGroupManager.class);
    }

}
