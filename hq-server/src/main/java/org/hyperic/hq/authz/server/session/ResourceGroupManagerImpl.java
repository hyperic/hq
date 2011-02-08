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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.GroupTypeValue;
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
import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.dao.ResourceGroupDao;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.paging.PageInfo;
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
    private static final String MIXED = "mixed";

    private static final String GROUP_ENT_RES_TYPE = "groupEntResType";

    private static final String GROUP_ENT_TYPE = "groupEntType";

    private final String BUNDLE = "org.hyperic.hq.authz.Resources";
   
    private AuthzSubjectManager authzSubjectManager;
    private EventLogManager eventLogManager;
    private final Log log = LogFactory.getLog(ResourceGroupManagerImpl.class);
    private ApplicationContext applicationContext;
    private ResourceGroupDao resourceGroupDao;
    private ResourceDao resourceDao;
    private ResourceTypeDao resourceTypeDao;
    private Pager defaultPager;

    @Autowired
    public ResourceGroupManagerImpl(AuthzSubjectManager authzSubjectManager,
                                    EventLogManager eventLogManager, ResourceGroupDao resourceGroupDao,
                                    ResourceDao resourceDao, ResourceTypeDao resourceTypeDao) {
        this.authzSubjectManager = authzSubjectManager;
        this.eventLogManager = eventLogManager;
        this.resourceGroupDao = resourceGroupDao;
        this.resourceDao = resourceDao;
        this.resourceTypeDao = resourceTypeDao;
    }
    
    @PostConstruct
    public void initialize() {
        this.defaultPager = Pager.getDefaultPager();
        //TODO move init logic?
        int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();
        for(int i=0;i< groupTypes.length;i++) {
            if(resourceTypeDao.findByName(AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[i])) == null) {
                ResourceType groupType = resourceTypeDao.create(AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[i]));
                setPropertyType(groupType,"groupEntType",Integer.class,false);
                setPropertyType(groupType,"groupEntResType",Integer.class,true);
                setPropertyType(groupType,"mixed",Boolean.class,true);
            }
        }
    }
    
    private void setPropertyType(ResourceType groupType, String propTypeName, Class<?> type,boolean indexed) {
        PropertyType propType = resourceTypeDao.createPropertyType(propTypeName,type);
        propType.setDescription(propTypeName);
        propType.setHidden(true);
        propType.setIndexed(indexed);
        groupType.addPropertyType(propType);
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
        ResourceGroup res = createGroup(whoami, cInfo, new HashSet<Role>(roles), new HashSet<Resource>(resources));
        applicationContext.publishEvent(new GroupCreatedEvent(res));
        return res;
    }

    private ResourceGroup createGroup(AuthzSubject whoami, ResourceGroupCreateInfo cInfo,
                                      Set<Role> roles, Set<Resource> resources)
        throws GroupDuplicateNameException, GroupCreationException {
        ResourceGroup existing = resourceGroupDao.findByName(cInfo.getName());
        if (existing != null) {
            throw new GroupDuplicateNameException("Group by the name [" + cInfo.getName() +
                                                  "] already exists");
        }
        ResourceType groupType = resourceTypeDao.findByName(AppdefEntityConstants.getAppdefGroupTypeName(cInfo.getGroupTypeId()));
        ResourceGroup res = resourceGroupDao.create(cInfo.getName(), groupType,cInfo.isPrivateGroup());
        res.setLocation(cInfo.getLocation());
        res.setDescription(cInfo.getDescription());
        res.setModifiedBy(whoami.getName());
        for(Resource resource : resources) {
            res.addMember(resource);
        }
        for(Role role: roles) {
            res.addRole(role);
        }
        res.setOwner(whoami);
        if(cInfo.getGroupTypeId() == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS || 
            cInfo.getGroupTypeId() == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC) {
            res.setProperty(MIXED,false);
        }else {
            res.setProperty(MIXED,true);
        }
        res.setProperty(GROUP_ENT_RES_TYPE, cInfo.getGroupEntResType());
        res.setProperty(GROUP_ENT_TYPE, cInfo.getGroupEntType());
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
        ResourceGroup group = resourceGroupDao.findById(id);
        if (group == null) {
            return null;
        }
        checkGroupPermission(whoami, group.getId(), AuthzConstants.perm_viewResourceGroup);
        return group;
    }

    private void checkGroupPermission(AuthzSubject whoami, Integer group, Integer op)
        throws PermissionException {
        //TODO perm check
        //PermissionManager pm = PermissionManagerFactory.getInstance();
       
        //pm.check(whoami.getId(), AuthzConstants.authzGroup, group, op);
    }

    /**
     * Find the group that has the given ID. Does not do any authz checking
     * 
     */
    @Transactional(readOnly = true)
    public ResourceGroup findResourceGroupById(Integer id) {
        return resourceGroupDao.findById(id);
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
            ResourceGroup existing = resourceGroupDao.findByName(name);

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
        AppdefEntityID groupId = AppdefUtil.newAppdefEntityId(group);
        group.remove();
        // Send resource delete event
        ResourceDeletedZevent zevent = new ResourceDeletedZevent(whoami, groupId);
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
    }
    
    public void removeResourceGroup(AuthzSubject whoami, Integer groupId) throws PermissionException, VetoException {
        ResourceGroup group = resourceGroupDao.findById(groupId);
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
        Set<Resource> members = g.getMembers();
        List<Resource> orderedMembers = new ArrayList<Resource>(members);
        Collections.sort(orderedMembers, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                return (o1.getName().compareTo(o2.getName()));
            }
        });
        return orderedMembers;
    }

    /**
     * Get the member type counts of a group
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Number> getMemberTypes(ResourceGroup g) {
        Set<ResourceType> memberTypes = new HashSet<ResourceType>();
        for(Resource member: g.getMembers()) {
            memberTypes.add(member.getType());
        }
      
        List<ResourceType> orderedMemberTypes =  new ArrayList<ResourceType>(memberTypes);
        Collections.sort(orderedMemberTypes, new Comparator<ResourceType>() {
            public int compare(ResourceType o1, ResourceType o2) {
                return (o1.getName().compareTo(o2.getName()));
            }
        });
        
        Map<String, Number> types = new HashMap<String, Number>();
        for(ResourceType memberType: orderedMemberTypes) {
            types.put(memberType.getName(),memberType.getResources().size());
           
        }
        return types;
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
        
        retVal.setId(g.getId());
        retVal.setName(g.getName());
        retVal.setDescription(g.getDescription());
        retVal.setLocation(g.getLocation());
        retVal.setGroupType(AppdefEntityConstants.getAppdefGroupTypeInt(g.getType().getName()));
        retVal.setGroupEntType((Integer)g.getProperty(GROUP_ENT_TYPE));
        retVal.setGroupEntResType((Integer)g.getProperty(GROUP_ENT_RES_TYPE));
        retVal.setTotalSize(members.size());
        retVal.setSubject(subj);
        //TODO don't have these at the moment
        //retVal.setMTime(new Long(g.getMtime()));
        //retVal.setCTime(new Long(g.getCtime()));
        retVal.setModifiedBy(g.getModifiedBy());
        retVal.setOwner(g.getOwner().getName());

        // Add the group members
        for (Resource r : members) {
           GroupEntry ge = new GroupEntry(r.getId(), AppdefUtil.newAppdefEntityId(r).getAuthzTypeName());
           retVal.addEntry(ge); 
        }
        
        if(retVal.isMixed()) {
            AppdefResourceTypeValue res = new GroupTypeValue();
            res.setId(g.getType().getId());
            res.setName(g.getType().getName());
            retVal.setAppdefResourceTypeValue(res);
        }else {
            retVal.setAppdefResourceTypeValue(AppdefResourceValue.getResourceTypeById(retVal.getGroupEntType(),retVal.getGroupEntResType()).getAppdefResourceTypeValue());
        }
        
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
                                                           ResourceType prototype,
                                                           Collection<ResourceGroup> excGrps,
                                                           org.hyperic.hibernate.PageInfo pInfo) {
        return findGroupsClusionary(subject, member, prototype, excGrps, pInfo,
            false);
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
                                                        org.hyperic.hibernate.PageInfo pInfo) {
        return findGroupsClusionary(subject, member, null, excludeGroups, pInfo,
            true);
    }
    
    private PageList<ResourceGroup> findGroupsClusionary(AuthzSubject subject, Resource member,
          ResourceType prototype,
          Collection<ResourceGroup> excludeGroups,
          org.hyperic.hibernate.PageInfo pInfo, boolean inclusive) {
        //TODO
//        ResourceGroupSortField sort = (ResourceGroupSortField) pInfo.getSort();
//        String hql = "from ResourceGroup g where g.system = false and ";
//        
//        if (prototype != null) {
//        hql += " (g.resourcePrototype = :proto ";
//        
//        // Mixed groups, too
//        
//        Integer protoType = prototype.getResourceType().getId();
//        if (protoType.equals(AuthzConstants.authzPlatformProto) ||
//        protoType.equals(AuthzConstants.authzServerProto) ||
//        protoType.equals(AuthzConstants.authzServiceProto)) {
//        hql += " or g.groupType = " + AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;
//        } else if (protoType.equals(AuthzConstants.authzApplicationProto)) {
//        hql += " or g.groupType = " + AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
//        }
//        
//        hql += ") and ";
//        }
//        
//        List<Integer> excludes = new ArrayList<Integer>(excludeGroups.size());
//        for (ResourceGroup g : excludeGroups) {
//        excludes.add(g.getId());
//        }
//        if (!excludes.isEmpty())
//        hql += " g.id not in (:excludes) and ";
//        
//        String inclusionStr = "";
//        if (!inclusive)
//        inclusionStr = " not ";
//        
//        PermissionManager pm = PermissionManagerFactory.getInstance();
//        hql += inclusionStr + " exists ( " + " select m.id from GroupMember m " +
//        " where m.resource = :resource and m.group = g " + ") ";
//        
//        String pmql = pm.getOperableGroupsHQL(subject, "g",
//        inclusive ? AuthzConstants.groupOpViewResourceGroup
//        : AuthzConstants.groupOpModifyResourceGroup);
//        
//        if (pmql.length() > 0)
//        hql += pmql;
//        
//        String countHql = "select count(g.id) " + hql;
//        String actualHql = "select g " + hql + " order by " + sort.getSortString("g");
//        
//        Query q = getSession().createQuery(countHql).setParameter("resource", member);
//        
//        if (!excludes.isEmpty())
//        q.setParameterList("excludes", excludes);
//        
//        if (prototype != null)
//        q.setParameter("proto", prototype);
//        
//        if (pmql.length() > 0)
//        q.setInteger("subjId", subject.getId().intValue());
//        
//        int total = ((Number) (q.uniqueResult())).intValue();
//        q = getSession().createQuery(actualHql).setParameter("resource", member);
//        
//        if (prototype != null)
//        q.setParameter("proto", prototype);
//        
//        if (!excludes.isEmpty())
//        q.setParameterList("excludes", excludes);
//        
//        if (pmql.length() > 0)
//        q.setInteger("subjId", subject.getId().intValue());
//        
//        List<ResourceGroup> vals = (List<ResourceGroup>) pInfo.pageResults(q).list();
//        return new PageList<ResourceGroup>(vals, total);
        return new PageList<ResourceGroup>();
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
        //TODO perm check
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
        return resourceGroupDao.findAll();
    }

    public void changeGroupOwner(AuthzSubject subject, ResourceGroup group, AuthzSubject newOwner)
        throws PermissionException {
        //TODO this used to call ResourceManager. Perm check?
        group.setOwner(newOwner);
        group.setModifiedBy(newOwner.getName());
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
    
    public Number getGroupCountOfType(ResourceType groupType) {
        return groupType.getResources().size();
    }
    
    public PageList<Resource> getCompatibleGroups(PageControl pageControl) {
        PageInfo pageInfo = new PageInfo(pageControl.getPagenum(),pageControl.getPagesize(),pageControl.getSortorder(),"name",String.class);
        return resourceDao.findByIndexedProperty(MIXED, false,pageInfo);
    }

    public PageList<Resource> getMixedGroups(PageControl pageControl) {
        PageInfo pageInfo = new PageInfo(pageControl.getPagenum(),pageControl.getPagesize(),pageControl.getSortorder(),"name",String.class);
        return resourceDao.findByIndexedProperty(MIXED, true,pageInfo);
    }
    
    public PageList<Resource> getCompatibleGroupsContainingType(int resourceTypeId, PageControl pageControl) {
        PageInfo pageInfo = new PageInfo(pageControl.getPagenum(),pageControl.getPagesize(),pageControl.getSortorder(),"name",String.class);
        return resourceDao.findByIndexedProperty(GROUP_ENT_RES_TYPE, resourceTypeId,pageInfo);
    }

    @Transactional(readOnly=true)
    public ResourceGroup findResourceGroupByName(String name) {
       return resourceGroupDao.findByName(name);
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
