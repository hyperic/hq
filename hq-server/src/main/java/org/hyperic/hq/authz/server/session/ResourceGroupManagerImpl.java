/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2013], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.authz.server.session;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.server.session.events.group.GroupCreatedEvent;
import org.hyperic.hq.authz.server.session.events.group.GroupDeleteRequestedEvent;
import org.hyperic.hq.authz.server.session.events.group.GroupMembersAddedZevent;
import org.hyperic.hq.authz.server.session.events.group.GroupMembersChangedEvent;
import org.hyperic.hq.authz.server.session.events.group.GroupMembersRemovedZevent;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.grouping.shared.GroupEntry;
import org.hyperic.hq.management.server.session.GroupCriteriaDAO;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.quartz.SchedulerException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
public class ResourceGroupManagerImpl implements ResourceGroupManager, ApplicationContextAware {
    private final String BUNDLE = "org.hyperic.hq.authz.Resources";
    private Pager _groupPager;
    private Pager _ownedGroupPager;
    private static final String GROUP_PAGER = PagerProcessor_resourceGroup.class.getName();
    private static final String OWNEDGROUP_PAGER = PagerProcessor_ownedResourceGroup.class.getName();

    private final ResourceEdgeDAO resourceEdgeDAO;

    private final AuthzSubjectManager authzSubjectManager;
    @Autowired
    private EventLogManager eventLogManager;
    private final Log log = LogFactory.getLog(ResourceGroupManagerImpl.class);
    private final ResourceManager resourceManager;
    private final ResourceGroupDAO resourceGroupDAO;
    protected final ResourceDAO resourceDAO;
    private final ResourceRelationDAO resourceRelationDAO;
    private ApplicationContext applicationContext;
    protected GroupCriteriaDAO groupCriteriaDAO;
    protected PermissionManager permissionManager;
    protected ResourceTypeDAO resourceTypeDAO;
    private ZeventManager zeventManager;

    @Autowired
    public ResourceGroupManagerImpl(ResourceEdgeDAO resourceEdgeDAO,
                                    AuthzSubjectManager authzSubjectManager,
                                    ResourceManager resourceManager,
                                    ResourceGroupDAO resourceGroupDAO, ResourceDAO resourceDAO,
                                    ResourceRelationDAO resourceRelationDAO,
                                    GroupCriteriaDAO groupCriteriaDAO,
                                    ResourceTypeDAO resourceTypeDAO,
                                    PermissionManager permissionManager,
                                    ZeventManager zeventManager) {
        this.resourceEdgeDAO = resourceEdgeDAO;
        this.authzSubjectManager = authzSubjectManager;
        this.resourceManager = resourceManager;
        this.resourceGroupDAO = resourceGroupDAO;
        this.resourceDAO = resourceDAO;
        this.resourceRelationDAO = resourceRelationDAO;
        this.groupCriteriaDAO = groupCriteriaDAO;
        this.permissionManager = permissionManager;
        this.resourceTypeDAO = resourceTypeDAO;
        this.zeventManager = zeventManager;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        _groupPager = Pager.getPager(GROUP_PAGER);
        _ownedGroupPager = Pager.getPager(OWNEDGROUP_PAGER);
    }

    /**
     * Create a resource group. Currently no permission checking.
     * 
     * @param roles List of {@link Role}s
     * @param resources List of {@link Resource}s
     */
    public ResourceGroup createResourceGroup(AuthzSubject whoami, ResourceGroupCreateInfo cInfo,
                                             Collection<Role> roles, Collection<Resource> resources)
    throws GroupCreationException, GroupDuplicateNameException {
        ResourceGroup res = createGroup(whoami, cInfo, roles, resources);
        applicationContext.publishEvent(new GroupCreatedEvent(res));
        return res;
    }

    public ResourceGroup createResourceGroup(AuthzSubject whoami, ResourceGroupCreateInfo cInfo, Resource groupResource,
                                             Collection<Role> roles)
    throws GroupCreationException, GroupDuplicateNameException {
        final ResourceGroup res = createGroup(whoami, cInfo, groupResource, roles);
        return res;
    }

    @SuppressWarnings("unchecked")
    private ResourceGroup createGroup(AuthzSubject whoami, ResourceGroupCreateInfo cInfo, Resource groupResource,
                                      Collection<Role> roles)
    throws GroupDuplicateNameException, GroupCreationException {
        return createGroup(whoami, cInfo, roles, Collections.EMPTY_LIST, groupResource);
    }
    
    private ResourceGroup createGroup(AuthzSubject whoami, ResourceGroupCreateInfo cInfo,
                                      Collection<Role> roles, Collection<Resource> resources)
        throws GroupDuplicateNameException, GroupCreationException {
        return createGroup(whoami, cInfo, roles, resources, null);
    }
    
    private ResourceGroup createGroup(AuthzSubject whoami, ResourceGroupCreateInfo cInfo, Collection<Role> roles,
                                      Collection<Resource> resources, Resource groupResource)
    throws GroupDuplicateNameException, GroupCreationException {
        ResourceGroup existing = resourceGroupDAO.findByName(cInfo.getName());
        if (existing != null) {
            throw new GroupDuplicateNameException("Group by the name [" + cInfo.getName() + "] already exists");
        }
        ResourceGroup res = resourceGroupDAO.create(whoami, cInfo, resources, roles, groupResource);
        ResourceRelation relation = resourceRelationDAO.findById(AuthzConstants.RELATION_CONTAINMENT_ID); // Self-edge
        if (groupResource == null) {
            resourceEdgeDAO.create(res.getResource(), res.getResource(), 0, relation);
        }
        applicationContext.publishEvent(new GroupCreatedEvent(res));
        return res;
    }

    /**
     * Do not allow resources to be added or removed from a group if the group
     * has a downtime schedule in progress.
     */
    private void checkGroupMaintenance(AuthzSubject subj, ResourceGroup group)
        throws PermissionException, VetoException {

        try {
            MaintenanceEvent event = PermissionManagerFactory.getInstance()
                .getMaintenanceEventManager().getMaintenanceEvent(subj, group.getId());

            if ((event != null) && MaintenanceEvent.STATE_RUNNING.equals(event.getState())) {
                String msg = ResourceBundle.getBundle(BUNDLE).getString(
                    "resourceGroup.update.error.downtime.running");

                throw new VetoException(MessageFormat.format(msg, new String[] { group.getName() }));
            }
        } catch (SchedulerException se) {
            // This should not happen. Indicates a serious system error.

            String msg = ResourceBundle.getBundle(BUNDLE).getString(
                "resourceGroup.update.error.downtime.scheduler.failure");

            throw new SystemException(MessageFormat.format(msg, new String[] { group.getName() }),
                se);
        }
    }

    /**
     * Find the group that has the given ID. Performs authz checking
     * @param whoami user requesting to find the group
     * @return {@link ResourceGroup} or null if it does not exist XXX scottmf,
     *         why is this method called find() but calls dao.get()???
     * 
     */
    @Transactional(readOnly = true)
    public ResourceGroup findResourceGroupById(AuthzSubject whoami, Integer id)
        throws PermissionException {
        ResourceGroup group = resourceGroupDAO.get(id);
        if (group == null) {
            return null;
        }
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_viewResourceGroup);
        return group;
    }

    private void checkGroupPermission(AuthzSubject whoami, Integer group, Integer op)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), AuthzConstants.authzGroup, group, op);
    }

    /**
     * Find the group that has the given ID. Does not do any authz checking
     * 
     */
    @Transactional(readOnly = true)
    public ResourceGroup findResourceGroupById(Integer id) {
        return resourceGroupDAO.findById(id);
    }

    @Transactional(readOnly = true)
    public ResourceGroup getResourceGroupById(Integer id) {
        return resourceGroupDAO.get(id);
    }

    /**
     * Find the role that has the given name.
     * @param whoami user requesting to find the group
     * @param name The name of the role you're looking for.
     * @return The value-object of the role of the given name.
     * @throws PermissionException whoami does not have viewResourceGroup on the
     *         requested group
     * 
     */
    @Transactional(readOnly = true)
    public ResourceGroup findResourceGroupByName(AuthzSubject whoami, String name)
        throws PermissionException {
        ResourceGroup group = resourceGroupDAO.findByName(name);

        if (group == null) {
            return null;
        }

        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_viewResourceGroup);
        return group;
    }
    
    @Transactional(readOnly = true)
    public boolean groupNameExists(String name) {
        return resourceGroupDAO.findByName(name) != null;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceGroup> findDeletedGroups() {
        return resourceGroupDAO.findDeletedGroups();
    }

    /**
     * Update some of the fundamentals of groups (name, description, location).
     * If name, description or location are null, the associated properties of
     * the passed group will not change.
     * 
     * @throws DuplicateObjectException if an attempt to rename the group would
     *         result in a group with the same name.
     * 
     */
    public ResourceGroup updateGroup(AuthzSubject whoami, ResourceGroup group, String name,
                            String description, String location) throws PermissionException,
        GroupDuplicateNameException {
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_modifyResourceGroup);

        // XXX: Add Auditing
        if ((name != null) && !name.equals(group.getName())) {
            ResourceGroup existing = resourceGroupDAO.findByName(name);

            if (existing != null) {
                throw new GroupDuplicateNameException("Group by that name [" + name +
                                                      "] already exists");
            }
            group.setName(name);
            group.getResource().setName(name);
        }

        if ((description != null) && !description.equals(group.getDescription())) {
            group.setDescription(description);
        }

        if ((location != null) && !location.equals(group.getLocation())) {
            group.setLocation(location);
        }
        
        return group;
    }

    public void removeGroupsCompatibleWith(String resourceType) throws VetoException {
        Resource proto = resourceManager.findResourcePrototypeByName(resourceType);
        removeGroupsCompatibleWith(proto);
    }

    /**
     * Remove all groups compatable with the specified resource prototype.
     * 
     * @throws VetoException if another subsystem cannot allow it (for
     *         constraint reasons)
     * 
     */
    public void removeGroupsCompatibleWith(Resource proto) throws VetoException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        for (ResourceGroup group : getAllResourceGroups()) {
            if (group.isCompatableWith(proto)) {
                try {
                    removeResourceGroup(overlord, group);
                } catch (PermissionException exc) {
                    log.warn("Perm denied while deleting group [" + group.getName() + " id=" +
                             group.getId() + "]", exc);
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
        throws PermissionException, VetoException {
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_removeResourceGroup);
        // TODO scottmf, this should be invoking a pre-transaction callback
        eventLogManager.deleteLogs(group.getResource());
        applicationContext.publishEvent(new GroupDeleteRequestedEvent(group));
        final Resource resource = group.getResource();
        /**
         * bug HQ-4526 - don't remove from resource table if it's
         * policy.(authzPolicy) In policy, the removal of the resource is done
         * in policyManagerImpl.
         **/
        boolean removeResource = resource.getResourceType().getId().equals(AuthzConstants.authzGroup);
        resourceGroupDAO.remove(group, removeResource);
        resourceGroupDAO.getSession().flush();
        // Send resource delete event
        ResourceDeletedZevent zevent = new ResourceDeletedZevent(whoami, AppdefEntityID.newGroupID(group.getId()));
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
    }
    
    public void removeResourceGroup(AuthzSubject whoami, Integer groupId) throws PermissionException, VetoException {
        ResourceGroup group = resourceGroupDAO.findById(groupId);
        removeResourceGroup(whoami, group);
    }
    
    
    /**
     * 
     */
    public void addResources(AuthzSubject subj, ResourceGroup group, Collection<Resource> resources)
        throws PermissionException, VetoException {
        addResources(subj, group, resources, false);
    }
    
    /**
     * 
     */
    public void addResources(AuthzSubject subj, ResourceGroup group, Collection<Resource> resources, boolean isDuringCalculation)
        throws PermissionException, VetoException {
        checkGroupPermission(subj, group.getId(), AuthzConstants.perm_modifyResourceGroup);
        checkGroupMaintenance(subj, group);
        addResources(group, resources, isDuringCalculation);
    }

    private void addResources(ResourceGroup group, Collection<Resource> resources, boolean isDuringCalculation) {
        resourceGroupDAO.addMembers(group, resources);
        applicationContext.publishEvent(new GroupMembersChangedEvent(group));
        zeventManager.enqueueEventAfterCommit(new GroupMembersAddedZevent(group, resources, isDuringCalculation));
    }

    /**
     * Add a resource to a group by resource id and resource type
     * 
     */
    public ResourceGroup addResource(AuthzSubject whoami, ResourceGroup group, Resource resource)
        throws PermissionException, VetoException {
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_modifyResourceGroup);

        checkGroupMaintenance(whoami, group);
        addResources(group, Collections.singletonList(resource), false);
        return group;
    }

    public void addResource(AuthzSubject whoami, Resource resource, Collection<ResourceGroup> groups)
        throws PermissionException, VetoException {
        // Do all of the pre-condition checks first before
        // iterating through addResources() because
        // ResourceGroupDAO().addMembers() will commit
        // the changes after each iteration.

        for (ResourceGroup g : groups) {
            checkGroupPermission(whoami, g.getId(), AuthzConstants.perm_modifyResourceGroup);
            checkGroupMaintenance(whoami, g);
        }

        for (ResourceGroup g : groups) {
            addResources(g, Collections.singletonList(resource), false);
        }
    }

    public void removeResource(AuthzSubject whoami, Resource resource,
                               Collection<ResourceGroup> groups) 
                                       throws PermissionException, VetoException {
        // Do all of the pre-condition checks first before
        // iterating through removeResources() because
        // ResourceGroupDAO().removeMembers() will commit
        // the changes after each iteration.

        for (ResourceGroup g : groups) {
            checkGroupPermission(whoami, g.getId(), AuthzConstants.perm_modifyResourceGroup);
            checkGroupMaintenance(whoami, g);
        }

        for (ResourceGroup g : groups) {
            removeResources(g, Collections.singletonList(resource), false);
        }
    }
    
    /**
     * RemoveResources from a group.
     * @param whoami The current running user.
     * @param group The group .
     * 
     */
    public void removeResources(AuthzSubject whoami, ResourceGroup group, Collection<Resource> resources) 
            throws PermissionException, VetoException {
        removeResources(whoami, group, resources, false);
    }

    /**
     * RemoveResources from a group.
     * @param whoami The current running user.
     * @param group The group .
     * 
     */
    public void removeResources(AuthzSubject whoami, ResourceGroup group,
                                Collection<Resource> resources, boolean isDuringCalculation) 
                                        throws PermissionException, VetoException {
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_modifyResourceGroup);
        checkGroupMaintenance(whoami, group);
        removeResources(group, resources, isDuringCalculation);
    }

    private void removeResources(ResourceGroup group, Collection<Resource> resources, boolean isDuringCalculation) {
        resourceGroupDAO.removeMembers(group, resources);
        applicationContext.publishEvent(new GroupMembersChangedEvent(group));
        zeventManager.enqueueEventAfterCommit(new GroupMembersRemovedZevent(group, isDuringCalculation));
    }

    /**
     * Change the resource contents of a group to the specified list of
     * resources.
     * 
     * @param resources A list of {@link Resource}s to be in the group
     * 
     */
    public void setResources(AuthzSubject whoami, ResourceGroup group, Collection<Resource> resources)
    throws PermissionException, VetoException {
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_modifyResourceGroup);
        checkGroupMaintenance(whoami, group);
        final Set<Resource> origMembers = new HashSet<Resource>(resourceGroupDAO.getMembers(group));
        applicationContext.publishEvent(new GroupMembersChangedEvent(group));
        final Collection<Resource> added = new ArrayList<Resource>();
        for (final Resource r : resources) {
            if (!origMembers.contains(r)) {
                added.add(r);
            }
        }
        resourceGroupDAO.setMembers(group, resources);
        zeventManager.enqueueEventAfterCommit(new GroupMembersAddedZevent(group, added, false));
    }

    /**
     * List the resources in this group that the caller is authorized to see.
     * @param whoami The current running user.
     * @param groupValue This group.
     * @param pc Paging information for the request
     * @return list of authorized resources in this group.
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Resource> getResources(AuthzSubject whoami, Integer id) {
        return PermissionManagerFactory.getInstance().getGroupResources(whoami.getId(), id,
            Boolean.FALSE);
    }

    /**
     * Get all the resource groups including the root resource group.
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceGroupValue> getAllResourceGroups(AuthzSubject subject, PageControl pc)
        throws PermissionException {
        return getAllResourceGroups(subject, pc, false);
    }

    /**
     * Get all the members of a group.
     * 
     * @return {@link Resource}s
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> getMembers(ResourceGroup g) {
        return resourceGroupDAO.getMembers(g);
    }

    @Transactional(readOnly = true)
    public List<Resource> getMembers(Collection<ResourceGroup> groups) {
        return resourceGroupDAO.getMembers(groups);
    }

    /**
    * Get all the members of a group, in a Map by type.
    * 
    * @return {@link Resource}s
    * 
    */
   @Transactional(readOnly = true)
   public Map<Integer, List<Integer>> getMembersByType(ResourceGroup g)
   {
        List<Resource> members = getMembers(g);
        Map<Integer, List<Integer>> membersByType = new HashMap<Integer, List<Integer>>();
        for (final Iterator<Resource> it=members.iterator(); it.hasNext(); ) {
            final Resource member = it.next();
            Integer protoId = member.getPrototype().getId();
            List<Integer> resourceIds = membersByType.get(protoId);
            if (resourceIds == null){
                resourceIds = new ArrayList<Integer>();
            }
            resourceIds.add(member.getId());
            membersByType.put(protoId, resourceIds);
        }
        return membersByType;
    }

    /**
     * Get the member type counts of a group
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Number> getMemberTypes(ResourceGroup g) {
        return resourceGroupDAO.getMemberTypes(g);
    }

    /**
     * Get all the groups a resource belongs to
     * 
     * @return {@link ResourceGroup}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceGroup> getGroups(Resource r) {
        return resourceGroupDAO.getGroups(r);
    }

    /**
     * Get the # of groups within HQ inventory
     * 
     */
    @Transactional(readOnly = true)
    public Number getGroupCount() {
        return new Integer(resourceGroupDAO.size());
    }

    /**
     * Returns true if the passed resource is a member of the given group.
     * 
     */
    @Transactional(readOnly = true)
    public boolean isMember(ResourceGroup group, Resource resource) {
        return resourceGroupDAO.isMember(group, resource);
    }

    /**
     * Get the # of members in a group
     * 
     */
    @Transactional(readOnly = true)
    public int getNumMembers(ResourceGroup g) {
        return resourceGroupDAO.getNumMembers(g);
    }
    
    @Transactional(readOnly = true)
    public AppdefGroupValue getGroupConvert(AuthzSubject subject, Integer groupId) {
        ResourceGroup group = findResourceGroupById(groupId);
        return getGroupConvert(subject, group);
    }

    /**
     * Temporary method to convert a ResourceGroup into an AppdefGroupValue
     * 
     */
    @Transactional(readOnly = true)
    public AppdefGroupValue getGroupConvert(AuthzSubject subj, ResourceGroup g) {
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
                GroupEntry ge = new GroupEntry(r.getInstanceId(), r.getResourceType().getName());
                retVal.addEntry(ge);
            }
        }

        retVal.setAppdefResourceTypeValue(retVal.getAppdefResourceTypeValue(subj, g));
        return retVal;
    }

    /**
     * Get a list of {@link ResourceGroup}s which are compatible with the
     * specified prototype.
     * 
     * Do not return any groups contained within 'excludeGroups' (a list of
     * {@link ResourceGroup}s
     * 
     * @param prototype If specified, the resulting groups must be compatible
     *        with the prototype.
     * 
     * @param pInfo Pageinfo with a sort field of type
     *        {@link ResourceGroupSortField}
     * 
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ResourceGroup> findGroupsNotContaining(AuthzSubject subject, Resource member,
                                                           Resource prototype,
                                                           Collection<ResourceGroup> excGrps,
                                                           PageInfo pInfo) {
        return resourceGroupDAO.findGroupsClusionary(subject, member, prototype, excGrps, pInfo,
            false, false);
    }

    /**
     * Get a list of {@link ResourceGroup}s which are compatible with the
     * specified prototype.
     * 
     * Do not return any groups contained within 'excludeGroups' (a list of
     * {@link ResourceGroup}s
     * 
     * @param prototype If specified, the resulting groups must be compatible
     *        with the prototype.
     * 
     * @param pInfo Pageinfo with a sort field of type
     *        {@link ResourceGroupSortField}
     * 
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ResourceGroup> findGroupsContaining(AuthzSubject subject, Resource member,
                                                        Collection<ResourceGroup> excludeGroups,
                                                        PageInfo pInfo) {
        return resourceGroupDAO.findGroupsClusionary(subject, member, null, excludeGroups, pInfo,
            true, false);
    }

    /**
     * Get all the resource groups excluding the root resource group.
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceGroup> getAllResourceGroups(AuthzSubject subject, boolean excludeRoot)
        throws PermissionException {
        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance();
        Collection<Integer> groupIds;

        /**
         * XXX: Seems this could be optimized to actually get the real list of
         * viewable resource groups instead of going through the perm manager to
         * get the IDs
         */
        try {
            // TODO: G
            groupIds = pm.findOperationScopeBySubject(subject,
                AuthzConstants.groupOpViewResourceGroup, AuthzConstants.groupResourceTypeName);
        } catch (NotFoundException e) {
            // Makes no sense
            throw new SystemException(e);
        }

        // now build a collection for all of them
        Collection<ResourceGroup> groups = new ArrayList<ResourceGroup>();
        for (Integer groupId : groupIds) {
            if (groupId == null) {
                continue;
            }
            ResourceGroup rgloc = resourceGroupDAO.findById(groupId);
            if (excludeRoot) {
                String name = rgloc.getName();
                if (!name.equals(AuthzConstants.groupResourceTypeName) &&
                    !name.equals(AuthzConstants.rootResourceGroupName)) {
                    groups.add(rgloc);
                }
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
    @Transactional(readOnly = true)
    public Collection<ResourceGroup> getAllResourceGroups() {
        return resourceGroupDAO.findAll();
    }

    /**
     * Get all compatible resource groups of the given entity type and resource
     * type.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceGroup> getCompatibleResourceGroups(AuthzSubject subject,
                                                                 Resource resProto)
        throws PermissionException, NotFoundException {
        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance();
        Collection<Integer> groupIds =
            pm.findOperationScopeBySubject(subject, AuthzConstants.groupOpViewResourceGroup,
                                           AuthzConstants.groupResourceTypeName);
        Collection<ResourceGroup> groups = resourceGroupDAO.findCompatible(resProto);
        for (Iterator<ResourceGroup> i = groups.iterator(); i.hasNext();) {
            ResourceGroup g = i.next();
            if (!groupIds.contains(g.getId())) {
                i.remove();
            }
        }

        return groups;
    }

    /**
     * Get all the resource groups excluding the root resource group and paged
     */
    @Transactional(readOnly = true)
    private PageList<ResourceGroupValue> getAllResourceGroups(AuthzSubject subject, PageControl pc,
                                                              boolean excludeRoot)
        throws PermissionException {
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
    @Transactional(readOnly = true)
    public PageList<ResourceGroupValue> getResourceGroupsById(AuthzSubject whoami, Integer[] ids,
                                                              PageControl pc)
        throws PermissionException {
        if (ids.length == 0) {
            return new PageList<ResourceGroupValue>();
        }

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
        while (i.hasNext() && (groups.size() < numToFind)) {
            ResourceGroup g = i.next();
            if (index.contains(g.getId())) {
                groups.add(g);
            }
        }

        // TODO: G
        PageList<ResourceGroupValue> plist = _groupPager.seek(groups, pc.getPagenum(), pc
            .getPagesize());
        plist.setTotalSize(groups.size());

        return plist;
    }
    
    /**
     * Get all resource groups owned by with the specified owner
     * @param owner of resource groups
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceGroup>  getResourceGroupsByOwnerId(AuthzSubject whoami)
        throws PermissionException {

        Collection<ResourceGroup> all = getAllResourceGroups(whoami, false);

        List<ResourceGroup> ownerGroups = new ArrayList<ResourceGroup>();
        Iterator<ResourceGroup> i = all.iterator();
        while (i.hasNext()) {
            ResourceGroup g = i.next();
            if (whoami.equals(g.getResource().getOwner())) {
                ownerGroups.add(g);
            }
        }

        return ownerGroups;
    }

    /**
     * Change owner of a group.
     * 
     * 
     */
    public void changeGroupOwner(AuthzSubject subject, ResourceGroup group, AuthzSubject newOwner)
        throws PermissionException {
        resourceManager.setResourceOwner(subject, group.getResource(), newOwner);
        group.setModifiedBy(newOwner.getName());
    }

    /**
     * Get a ResourceGroup owner's AuthzSubjectValue
     * @param gid The group id
     * @exception NotFoundException Unable to find a group by id
     * 
     */
    @Transactional(readOnly = true)
    public AuthzSubject getResourceGroupOwner(Integer gid) throws NotFoundException {
        Resource gResource = resourceManager.findResourceByInstanceId(resourceManager
            .findResourceTypeByName(AuthzConstants.groupResourceTypeName), gid);
        return gResource.getOwner();
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
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
    public void updateGroupType(AuthzSubject subject, ResourceGroup g, int groupType,
                                int groupEntType, int groupEntResType) throws PermissionException {
        checkGroupPermission(subject, g.getId(), AuthzConstants.perm_modifyResourceGroup);

        g.setGroupType(new Integer(groupType));

        if ((groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS) ||
            (groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC)) {
            Resource r = findPrototype(new AppdefEntityTypeID(groupEntType, groupEntResType));
            g.setResourcePrototype(r);
        }
    }

    private Resource findPrototype(AppdefEntityTypeID id) {
        Integer authzType;
        switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                authzType = AuthzConstants.authzPlatformProto;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                authzType = AuthzConstants.authzServerProto;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                authzType = AuthzConstants.authzServiceProto;
                break;
            default:
                throw new IllegalArgumentException("Unsupported prototype type: " + id.getType());
        }
        return resourceDAO.findByInstanceId(authzType, id.getId());
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ResourceGroup getGroupById(Integer id) {
        if (id == null) {
            return null;
        }
        return resourceGroupDAO.get(id);
    }

    public void removeAllMembers(ResourceGroup group) {
        resourceGroupDAO.removeAllMembers(group);
    }
    
    @Transactional(readOnly=true)
    public List<ResourceGroup> getResourceGroupsByType(int groupType) {
        return resourceGroupDAO.getGroupsByType(groupType);
    }

    public Collection<GroupMember> getOrphanedResourceGroupMembers() {
        return resourceGroupDAO.getOrphanedResourceGroupMembers();
    }

    public void removeGroupMember(GroupMember m) {
        resourceGroupDAO.getSession().delete(m);
    }
}
