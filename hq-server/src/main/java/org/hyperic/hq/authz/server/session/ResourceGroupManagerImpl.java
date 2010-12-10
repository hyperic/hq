/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2010], VMware, Inc.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.grouping.shared.GroupEntry;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageList;
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
   
    private AuthzSubjectManager authzSubjectManager;
    private EventLogManager eventLogManager;
    private final Log log = LogFactory.getLog(ResourceGroupManagerImpl.class);
    private ApplicationContext applicationContext;
   

    @Autowired
    public ResourceGroupManagerImpl(AuthzSubjectManager authzSubjectManager,
                                    EventLogManager eventLogManager) {
        this.authzSubjectManager = authzSubjectManager;
        this.eventLogManager = eventLogManager;
    }

    /**
     * Create a resource group. Currently no permission checking.
     * 
     * @param roles List of {@link Role}s
     * @param resources List of {@link Resource}s
     * 
     * 
     */
    public ResourceGroup createResourceGroup(AuthzSubject whoami, ResourceGroupCreateInfo cInfo,
                                             Collection<Role> roles, Collection<Resource> resources)
        throws GroupCreationException, GroupDuplicateNameException {
        ResourceGroup res = createGroup(whoami, cInfo, roles, resources);
        applicationContext.publishEvent(new GroupCreatedEvent(res));
        return res;
    }

    private ResourceGroup createGroup(AuthzSubject whoami, ResourceGroupCreateInfo cInfo,
                                      Collection<Role> roles, Collection<Resource> resources)
        throws GroupDuplicateNameException, GroupCreationException {
        ResourceGroup existing = ResourceGroup.findResourceGroupByName(cInfo.getName());

        if (existing != null) {
            throw new GroupDuplicateNameException("Group by the name [" + cInfo.getName() +
                                                  "] already exists");
        }

        ResourceGroup res = new ResourceGroup();
        res.setName(cInfo.getName());
        res.setLocation(cInfo.getLocation());
        res.setPrivateGroup(cInfo.isPrivateGroup());
        res.setDescription(cInfo.getDescription());
        res.setModifiedBy(whoami.getName());
        ResourceType groupType = ResourceType.findResourceType(cInfo.getGroupTypeId());
        //TODO throw Exception if type doesn't exist?
        res.persist();
        res.setType(groupType);
        
        //TODO why?
        //resourceEdgeDAO.create(res.getResource(), res.getResource(), 0, resourceRelationDAO.findById(AuthzConstants.RELATION_CONTAINMENT_ID)); // Self-edge
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

            if (event != null && MaintenanceEvent.STATE_RUNNING.equals(event.getState())) {
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
        ResourceGroup group = ResourceGroup.findResourceGroup(id);
        if (group == null) {
            return null;
        }
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_viewResourceGroup);
        return group;
    }

    private void checkGroupPermission(AuthzSubject whoami, Integer group, Integer op)
        throws PermissionException {
        //TODO
        //PermissionManager pm = PermissionManagerFactory.getInstance();
       
        //pm.check(whoami.getId(), AuthzConstants.authzGroup, group, op);
    }

    /**
     * Find the group that has the given ID. Does not do any authz checking
     * 
     */
    @Transactional(readOnly = true)
    public ResourceGroup findResourceGroupById(Integer id) {
        return ResourceGroup.findResourceGroup(id);
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
    public void updateGroup(AuthzSubject whoami, ResourceGroup group, String name,
                            String description, String location) throws PermissionException,
        GroupDuplicateNameException {
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_modifyResourceGroup);

        // XXX: Add Auditing
        if (name != null && !name.equals(group.getName())) {
            ResourceGroup existing = ResourceGroup.findResourceGroupByName(name);

            if (existing != null) {
                throw new GroupDuplicateNameException("Group by that name [" + name +
                                                      "] already exists");
            }
            group.setName(name);
           
        }

        if (description != null && !description.equals(group.getDescription())) {
            group.setDescription(description);
        }

        if (location != null && !location.equals(group.getLocation())) {
            group.setLocation(location);
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
        eventLogManager.deleteLogs(group);
        applicationContext.publishEvent(new GroupDeleteRequestedEvent(group));
        group.remove();


        // Send resource delete event
        ResourceDeletedZevent zevent = new ResourceDeletedZevent(whoami, AppdefUtil.newAppdefEntityId(group));
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
    }
    
    public void removeResourceGroup(AuthzSubject whoami, Integer groupId) throws PermissionException, VetoException {
        ResourceGroup group = ResourceGroup.findResourceGroup(groupId);
        removeResourceGroup(whoami, group);
    }

    private void addResources(ResourceGroup group, Collection<Resource> resources) {
        for(Resource resource: resources) {
            group.addMember(resource);
        }
        applicationContext.publishEvent(new GroupMembersChangedEvent(group));
    }

    /**
     * Add a resource to a group by resource id and resource type
     * 
     */
    public ResourceGroup addResource(AuthzSubject whoami, ResourceGroup group, Resource resource)
        throws PermissionException, VetoException {
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_modifyResourceGroup);

        checkGroupMaintenance(whoami, group);
        addResources(group, Collections.singletonList(resource));
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
            addResources(g, Collections.singletonList(resource));
        }
    }

    public void removeResource(AuthzSubject whoami, Resource resource,
                               Collection<ResourceGroup> groups) throws PermissionException,
        VetoException {
        // Do all of the pre-condition checks first before
        // iterating through removeResources() because
        // ResourceGroupDAO().removeMembers() will commit
        // the changes after each iteration.

        for (ResourceGroup g : groups) {
            checkGroupPermission(whoami, g.getId(), AuthzConstants.perm_modifyResourceGroup);
            checkGroupMaintenance(whoami, g);
        }

        for (ResourceGroup g : groups) {
            removeResources(whoami,g, Collections.singletonList(resource));
        }
    }

    

    public void removeResources(AuthzSubject whoami, ResourceGroup group, Collection<Resource> resources) {
        for(Resource resource: resources) {
            group.removeMember(resource);
        }
        applicationContext.publishEvent(new GroupMembersChangedEvent(group));
    }

    /**
     * Get all the members of a group.
     * 
     * @return {@link Resource}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Resource> getMembers(ResourceGroup g) {
        return g.getMembers();
    }

    /**
     * Get the member type counts of a group
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Number> getMemberTypes(ResourceGroup g) {
        //TODO member types?
        //return resourceGroupDAO.getMemberTypes(g);
        return null;
    }
    
    /**
     * Get the # of members in a group
     * 
     */
    @Transactional(readOnly = true)
    public int getNumMembers(ResourceGroup g) {
        return getMembers(g).size();
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
        retVal.setGroupType(g.getType().getId());
        //TODO don't have res type at the momennt
        //retVal.setGroupEntType(g.getGroupEntType().intValue());
        //retVal.setGroupEntResType(g.getGroupEntResType().intValue());
        retVal.setTotalSize(members.size());
        retVal.setSubject(subj);
        //TODO don't have these at the moment
        //retVal.setClusterId(g.getClusterId().intValue());
        //retVal.setMTime(new Long(g.getMtime()));
        //retVal.setCTime(new Long(g.getCtime()));
        retVal.setModifiedBy(g.getModifiedBy());
        retVal.setOwner(g.getOwner().getName());

        // Add the group members
        for (Resource r : members) {
            if (r.getType() != null) {
                GroupEntry ge = new GroupEntry(r.getId(), r.getType().getName());
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
        //TODO not supporting compat groups
        return null;
        //return resourceGroupDAO.findGroupsClusionary(subject, member, prototype, excGrps, pInfo,
          //  false);
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
        //TODO not supporting compat groups
        return null;
        //return resourceGroupDAO.findGroupsClusionary(subject, member, null, excludeGroups, pInfo,
          //  true);
    }

    /**
     * Get all the resource groups excluding the root resource group.
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceGroup> getAllResourceGroups(AuthzSubject subject)
        throws PermissionException {
        // first get the list of groups subject can view
        
       

        /**
         * XXX: Seems this could be optimized to actually get the real list of
         * viewable resource groups instead of going through the perm manager to
         * get the IDs
         */
        //TODO
       // PermissionManager pm = PermissionManagerFactory.getInstance();
        // List<Integer> groupIds;
//        try {
//            groupIds = pm.findOperationScopeBySubject(subject,
//                AuthzConstants.groupOpViewResourceGroup, AuthzConstants.groupResourceTypeName);
//        } catch (NotFoundException e) {
//            // Makes no sense
//            throw new SystemException(e);
//        }

        // now build a collection for all of them
        Collection<ResourceGroup> groups = getAllResourceGroups();
        return groups;
    }

    private Collection<ResourceGroup> getAllResourceGroups() {
        return ResourceGroup.findAllResourceGroups();
    }

  

   
    /**
     * Change owner of a group.
     * 
     * 
     */
    public void changeGroupOwner(AuthzSubject subject, ResourceGroup group, AuthzSubject newOwner)
        throws PermissionException {
        //TODO this used to call ResourceManager. Perm check?
        group.setOwner(newOwner);
        group.setModifiedBy(newOwner.getName());
    }

    public void updateGroupMembers(List<ResourceCreatedZevent> resourceEvents) {
        for (ResourceCreatedZevent resourceEvent : resourceEvents) {
            updateGroupMember(resourceEvent);
        }
    }

    private void updateGroupMember(ResourceCreatedZevent resourceEvent) {
        final Resource resource = Resource.findResource(resourceEvent.getId());
        final AuthzSubject subject = authzSubjectManager.findSubjectById(resourceEvent
            .getAuthzSubjectId());
        for (ResourceGroup group : getAllResourceGroups()) {
            try {
                //TODO critter stuff
                //CritterList groupCriteria = group.getCritterList();
                //if (isCriteriaMet(groupCriteria, resource)) {
                    try {
                        addResource(subject, group, resource);
                    } catch (Exception e) {
                        log.error("Unable to add resource " + resource + " to group " +
                                  group.getName());
                    }
                //}
            } catch (Exception e) {
                log.error("Unable to process criteria for group " + group.getName() +
                          " while processing event " + resourceEvent +
                          ".  The groups' members may not be updated.");
            }
        }
    }
    
    
    
   public Collection<ResourceGroup> getGroups(Resource r) {
       Set<ResourceGroup> groups = new HashSet<ResourceGroup>();
       Collection<ResourceGroup> allGroups = getAllResourceGroups();
       for(ResourceGroup group: allGroups) {
           if(group.isMember(r)) {
               groups.add(group);
           }
       }
       return groups;
    }

    //TODO remove legacy support
    public boolean isMixed(ResourceGroup group) {
      if(group.getGroupType() == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS || 
          group.getGroupType() == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC) {
          return false;
      }
      return true;
    }

    public int getGroupEntType(ResourceGroup group) {
        //TODO let's say we used a prop to set this on create
//        if (_resourcePrototype == null) {
//            -            return new Integer(-1);
//            -        }
//            -
//            -        Integer type = _resourcePrototype.getResourceType().getId();
//            -        if (type.equals(AuthzConstants.authzPlatformProto)) {
//            -            return new Integer(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
//            -        } else if (type.equals(AuthzConstants.authzServerProto)) {
//            -            return new Integer(AppdefEntityConstants.APPDEF_TYPE_SERVER);
//            -        } else if (type.equals(AuthzConstants.authzServiceProto)) {
//            -            return new Integer(AppdefEntityConstants.APPDEF_TYPE_SERVICE);
//            -        } else {
//            -            return new Integer(-1); // Backwards compat.
//            -        }
        return 1;
    }

    public int getGroupEntResType(ResourceGroup group) {
       //TODO return id of ResourceType for Platform, Server, or Service
        return 1;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
