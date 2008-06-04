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

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.ApplicationManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.GroupTypeValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.CritterList;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.grouping.shared.GroupEntry;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

/**
 * Use this session bean to manipulate ResourceGroups,
 *
 * All arguments and return values are value-objects.
 *
 * @ejb:bean name="ResourceGroupManager"
 *      jndi-name="ejb/authz/ResourceGroupManager"
 *      local-jndi-name="LocalResourceGroupManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class ResourceGroupManagerEJBImpl 
    extends AuthzSession 
    implements SessionBean 
{
    private Pager _groupPager;
    private Pager _ownedGroupPager;
    private final String RESOURCE_PAGER =
        PagerProcessor_resource.class.getName();
    private final String GROUP_PAGER = 
        PagerProcessor_resourceGroup.class.getName();
    private final String OWNEDGROUP_PAGER =
        PagerProcessor_ownedResourceGroup.class.getName();

    /**
     * List the ResourceGroups associated with this resource.
     * @param res This resource.
     * @ejb:interface-method
     */
    public ResourceGroupValue[] getResourceGroups(ResourceValue res) {
        Resource resource = getResourceDAO().findById(res.getId());
        Collection pojos = getResourceGroupDAO().findContaining(resource);
        return (ResourceGroupValue[])
            fromPojos(pojos, ResourceGroupValue.class);
    }

    /**
     * For compatible groups, get the Resource prototype for the given
     * appdef entity type and id.
     * @param atype The appdef type, one of AppdefEntityConstants.APPDEF_TYPE*
     * @param aid The appdef instance id.
     * @return The Resource prototype for this type and id or null if one
     * does not exist.
     * @ejb:interface-method
     * @TODO As we migrate to a Resource based inventory this method should go.
     */
    public Resource getResourceGroupPrototype(int atype, int aid) {

        ResourceDAO dao = getResourceDAO();

        Integer type;
        switch (atype) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                type = AuthzConstants.authzPlatformProto;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                type = AuthzConstants.authzServerProto;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                type = AuthzConstants.authzServiceProto;
                break;
            default:
                return null;
        }

        return dao.findByInstanceId(type, new Integer(aid));
    }

    /**
     * Create a resource group.  Currently no permission checking.
     * 
     * @param roles     List of {@link Role}s
     * @param resources List of {@link Resource}s
     * 
     * @ejb:interface-method
     */
    public ResourceGroup createResourceGroup(AuthzSubject whoami,
                                             ResourceGroupCreateInfo cInfo,
                                             Collection roles,
                                             Collection resources)
        throws GroupCreationException, GroupDuplicateNameException
    {
        ResourceGroup existing = 
            getResourceGroupDAO().findByName(cInfo.getName());
        
        if (existing != null) {
            throw new GroupDuplicateNameException("Group by the name [" + 
                                                  cInfo.getName() + 
                                                  "] already exists"); 
        }
        
        ResourceGroup res = getResourceGroupDAO().create(whoami, cInfo,
                                                         resources, roles);

        GroupingStartupListener.getCallbackObj().postGroupCreate(res);
        return res;
    }
    
    /**
     * Find the group that has the given ID.  Performs authz checking
     * @param whoami user requesting to find the group
     * @ejb:interface-method
     */
    public ResourceGroup findResourceGroupById(AuthzSubject whoami,
                                               Integer id)
        throws PermissionException
    {
        ResourceGroup group = getResourceGroupDAO().get(id);
        if (group == null) {
            return null;
        }

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_viewResourceGroup);
        return group;
    }

    /**
     * Find the group that has the given ID.  Does not do any authz checking
     * @ejb:interface-method
     */
    public ResourceGroup findResourceGroupById(Integer id) {
        return getResourceGroupDAO().findById(id);
    }
    
    /**
     * Find the role that has the given name.
     * @param whoami user requesting to find the group
     * @param name The name of the role you're looking for.
     * @return The value-object of the role of the given name.
     * @throws PermissionException whoami does not have viewResourceGroup
     * on the requested group
     * @ejb:interface-method
     */
    public ResourceGroup findResourceGroupByName(AuthzSubject whoami,
                                                 String name)
        throws PermissionException
    {
        ResourceGroup group = getResourceGroupDAO().findByName(name);

        if (group == null)
            return null;
        
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_viewResourceGroup);
        return group;
    }

    /**
     * Update some of the fundamentals of groups (name, description, location).
     * If name, description or location are null, the associated properties
     * of the passed group will not change.
     * 
     * @throws DuplicateObjectException if an attempt to rename the group would
     *                                  result in a group with the same name.
     * @ejb:interface-method
     */
    public void updateGroup(AuthzSubject whoami, ResourceGroup group,
                            String name, String description, String location) 
        throws PermissionException, DuplicateObjectException
    {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        // XXX:  Add Auditing
        if (name != null && !name.equals(group.getName())) {
            ResourceGroup existing = getResourceGroupDAO().findByName(name);
            
            if (existing != null) {
                throw new DuplicateObjectException("Group by that name [" +
                                                   name + "] already exists",
                                                   existing);
            }
            group.setName(name);
        }
        
        if (description != null && !description.equals(group.getDescription())){
            group.setDescription(description);
        }
        
        if (location != null && !location.equals(group.getLocation())) {
            group.setLocation(location);
        }
    }
    
    /**
     * Write the specified entity out to permanent storage.
     * @param whoami The current running user.
     * @param grpVal The ResourceGroup to save.
     * @throws PermissionException whoami may not perform modifyResourceGroup on
     *                             this group.
     * @ejb:interface-method
     */
    public void saveResourceGroup(AuthzSubject whoami,
                                  ResourceGroupValue grpVal)
        throws PermissionException 
    {
        ResourceGroup group = lookupGroup(grpVal);
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        // check if the name has changed. If it has, update the resource
        if (!grpVal.getName().equals(group.getName())) {
            Resource resource =
                getResourceDAO().findByInstanceId(AuthzConstants.authzGroup,
                                                  group.getId());

            resource.setName(grpVal.getName());
        }
        group.setResourceGroupValue(grpVal);
    }

    /**
     * Remove all groups compatable with the specified resource prototype.
     * 
     * @throws VetoException if another subsystem cannot allow it (for 
     *                       constraint reasons)
     * @ejb:interface-method
     */
    public void removeGroupsCompatibleWith(Resource proto)
        throws VetoException
    {
        AuthzSubject overlord = 
            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        
        for (Iterator i=getAllResourceGroups().iterator(); i.hasNext(); ) {
            ResourceGroup group = (ResourceGroup)i.next();
            
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
     * @ejb:interface-method
     */
    public void removeResourceGroup(AuthzSubject whoami, ResourceGroup group)
        throws PermissionException, VetoException
    {
        ResourceGroupDAO dao = getResourceGroupDAO();
        ResourceGroup resGrp = dao.findById(group.getId());
        PermissionManager pm = PermissionManagerFactory.getInstance(); 

        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup, resGrp.getId(),
                 AuthzConstants.perm_removeResourceGroup);

        GroupingStartupListener.getCallbackObj().preGroupDelete(resGrp);
        dao.remove(resGrp);

        // Send resource delete event
        ResourceDeletedZevent zevent =
            new ResourceDeletedZevent(whoami,
                                      AppdefEntityID.newGroupID(group.getId()));
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
    }

    /**
     * Associate resources with this group.
     * @param whoami The current running user.
     * @param group The group.
     * @param resources The resources to associate with the group.
     * @throws PermissionException whoami does not own the resources.
     * @ejb:interface-method
     */
    public void addResources(AuthzSubject whoami,
                             ResourceGroupValue group,
                             ResourceValue[] resources)
        throws PermissionException 
    {
        ResourceGroupDAO grpDao = getResourceGroupDAO();
        ResourceGroup resGroup = grpDao.findByName(group.getName());
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup,
                 group.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        ResourceDAO resDao = getResourceDAO();
        List resourcePojos = new ArrayList(resources.length);
        for (int i = 0; i < resources.length; i++) {
            Resource resource = resDao.findById(resources[i].getId());
            resourcePojos.add(resource);
        }
        
        grpDao.addMembers(resGroup, resourcePojos);

        GroupingStartupListener.getCallbackObj().groupMembersChanged(resGroup);
    }

    /**
     * Add a resource to a group by resource id and resource type
     * @ejb:interface-method
     */
    public ResourceGroup addResource(AuthzSubject whoami, ResourceGroup group, 
                                     Resource resource)
        throws PermissionException 
    {
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(), AuthzConstants.authzGroup,
                 group.getId(), AuthzConstants.perm_modifyResourceGroup);

        getResourceGroupDAO().addMembers(group, 
                                         Collections.singleton(resource));
        GroupingStartupListener.getCallbackObj().groupMembersChanged(group);
        return group;
    }
 
    /**
     * RemoveResources from a group.
     * @param whoami The current running user.
     * @param group The group .
     * @ejb:interface-method
     */
    public void removeResources(AuthzSubject whoami,
                                ResourceGroup group,
                                Collection resources)
        throws PermissionException 
    {
        ResourceGroupDAO grpDao = getResourceGroupDAO();

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup,
                 group.getId(),
                 AuthzConstants.perm_modifyResourceGroup);
        
        grpDao.removeMembers(group, resources);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(group);
    }

    /**
     * Disassociate all resources from this group.
     * @param whoami The current running user.
     * @param group The group.
     * @ejb:interface-method
     */
    public void removeAllResources(AuthzSubject whoami,
                                   ResourceGroupValue group)
        throws PermissionException 
    {
        ResourceGroupDAO dao = getResourceGroupDAO();
        ResourceGroup resGroup = dao.findByName(group.getName());

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup,
                 resGroup.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        dao.removeAllMembers(resGroup);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(resGroup);
    }

    /**
     * Sets the criteria list for this group.
     * @param whoami The current running user.
     * @param group This group.
     * @param critters List of critters to associate with this resource group.
     * @throws PermissionException whoami does not own the resource.
     * @throws GroupException critters is not a valid list of criteria.
     * @ejb:interface-method
     */
    public void setCriteria(AuthzSubject whoami, ResourceGroup group, CritterList critters) 
        throws PermissionException, GroupException 
   {
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_modifyResourceGroup);
        
        group.setCritterList(critters);
    }
    
    /**
     * Set the resources for this group.
     * To get the operations call getOperationValues() on the value-object.
     * @param whoami The current running user.
     * @param grpVal This group.
     * @param resources Resources to associate with this role.
     * @throws PermissionException whoami does not own the resource.
     * @ejb:interface-method
     */
    public void setResources(AuthzSubject whoami,
                             ResourceGroupValue grpVal,
                             ResourceValue[] resources)
        throws PermissionException 
    {
        ResourceGroupDAO gDao = getResourceGroupDAO();
        ResourceGroup group = gDao.findById(grpVal.getId());

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        gDao.setMembers(group, toPojos(resources));
        GroupingStartupListener.getCallbackObj().groupMembersChanged(group);
    }

    /**
     * Change the resource contents of a group to the specified list
     * of resources.
     * 
     * @param resources  A list of {@link Resource}s to be in the group
     * @ejb:interface-method
     */
    public void setResources(AuthzSubject whoami,
                             ResourceGroup group, Collection resources) 
        throws PermissionException 
    {
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        getResourceGroupDAO().setMembers(group, resources);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(group);
    }

    /**
     * List the resources in this group that the caller is authorized to see.
     * @param whoami The current running user.
     * @param groupValue This group.
     * @param pc Paging information for the request
     * @return list of authorized resources in this group.
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public Collection getResources(AuthzSubject whoami, Integer id)
    {
        return PermissionManagerFactory.getInstance()
            .getGroupResources(whoami.getId(), id, Boolean.FALSE);
    }

    /**
     * Get all the resource groups including the root resource group.
     * @ejb:interface-method
     */
    public List getAllResourceGroups(AuthzSubject subject, PageControl pc)
        throws PermissionException, FinderException 
    {
        return getAllResourceGroups(subject, pc, false);
    }

    /**
     * Get all the members of a group.
     * 
     * @return {@link Resource}s
     * @ejb:interface-method
     */
    public Collection getMembers(ResourceGroup g) {
        return getResourceGroupDAO().getMembers(g);
    }
    
    /**
     * Get all the members of a group, identified by the resource passed in
     * @ejb:interface-method
     */
    public Collection getMembers(Resource r) {
        ResourceGroup g = getResourceGroupDAO().findByResource(r);
        return getMembers(g);
    }

    /**
     * Get all the groups a resource belongs to
     * 
     * @return {@link ResourceGroup}s
     * @ejb:interface-method
     */
    public Collection getGroups(Resource r) {
        return getResourceGroupDAO().getGroups(r);
    }
    
    /**
     * Returns true if the passed resource is a member of the given group.
     * @ejb:interface-method
     */
    public boolean isMember(ResourceGroup group, Resource resource) {
        return getResourceGroupDAO().isMember(group, resource);
    }
    
    
    /**
     * Get the # of members in a group
     * @ejb:interface-method
     */
    public int getNumMembers(ResourceGroup g) {
        return getMembers(g).size();
    }
    
    
    /**
     * Temporary method to convert a ResourceGroup into an AppdefGroupValue
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AppdefGroupValue convertGroup(AuthzSubject subj, ResourceGroup g) {
        AppdefGroupValue retVal = new AppdefGroupValue();
        Collection members = getMembers(g);
        
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
        retVal.setOwner(subj.getName());
                
        // Add the group members
        for (Iterator i = members.iterator(); i.hasNext();) {
            Resource r= (Resource) i.next();
            GroupEntry ge = new GroupEntry(r.getInstanceId(),
                                           r.getResourceType().getName());
            retVal.addEntry(ge);
        }

        retVal.setAppdefResourceTypeValue(getAppdefResourceTypeValue(subj, g));
        return retVal;
    }
    
    /**
     * @ejb:interface-method
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
        return PlatformManagerEJBImpl.getOne()
                .findPlatformType(new Integer(id));
    }

    private ServerType getServerTypeById (int id) {
        return ServerManagerEJBImpl.getOne().findServerType(new Integer(id));
    }

    private ServiceType getServiceTypeById(int id) {
        return ServiceManagerEJBImpl.getOne().findServiceType(new Integer(id));
    }

    private ApplicationType getApplicationTypeById (int id) {
        return ApplicationManagerEJBImpl.getOne()
                   .findApplicationType(new Integer(id));
    }
    
    /**
     * Get a list of {@link ResourceGroup}s which are compatable with
     * the specified prototype.
     * 
     * Do not return any groups contained within 'excludeGroups' (a list of
     * {@link ResourceGroup}s
     * 
     * @param prototype  If specified, the resulting groups must be compatable
     *                   with the prototype.  
     * 
     * @param pInfo Pageinfo with a sort field of type 
     *              {@link ResourceGroupSortField}
     * 
     * @ejb:interface-method
     * */
    public PageList findGroupsNotContaining(AuthzSubject subject, 
                                            Resource member, Resource prototype,
                                            Collection excGrps, PageInfo pInfo) 
    {
        return getResourceGroupDAO().findGroupsClusionary(subject, member,
                                                          prototype, excGrps,
                                                          pInfo, false);
    }

    /**
     * Get a list of {@link ResourceGroup}s which are compatable with
     * the specified prototype.
     * 
     * Do not return any groups contained within 'excludeGroups' (a list of
     * {@link ResourceGroup}s
     * 
     * @param prototype  If specified, the resulting groups must be compatable
     *                   with the prototype.  
     * 
     * @param pInfo Pageinfo with a sort field of type 
     *              {@link ResourceGroupSortField}
     * 
     * @ejb:interface-method
     * */
    public PageList findGroupsContaining(AuthzSubject subject, 
                                         Resource member,
                                         Collection excludeGroups, 
                                         PageInfo pInfo) 
    {
        return getResourceGroupDAO().findGroupsClusionary(subject, member,
                                                          null, excludeGroups,
                                                          pInfo, true);
    }
    
    /**
     * Get all the resource groups excluding the root resource group.
     * @ejb:interface-method
     */
    public Collection getAllResourceGroups(AuthzSubject subject,
                                           boolean excludeRoot)
        throws PermissionException
    {
        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        List groupIds;
        
        /**
         * XXX:  Seems this could be optimized to actually get the real
         *       list of viewable resource groups instead of going through
         *       the perm manager to get the IDs
         */
        try {
            groupIds = pm.findOperationScopeBySubject(subject, 
                                        AuthzConstants.groupOpViewResourceGroup, 
                                        AuthzConstants.groupResourceTypeName, 
                                        PageControl.PAGE_ALL);
        } catch(FinderException e) {
            // Makes no sense
            throw new SystemException(e);
        }
        
        // now build a collection for all of them
        Collection groups = new ArrayList();
        ResourceGroupDAO dao = getResourceGroupDAO();
        for(int i = 0; i < groupIds.size(); i++) {
            ResourceGroup rgloc = dao.findById((Integer) groupIds.get(i));
            if (excludeRoot) {
                if (!rgloc.getName()
                        .equals(AuthzConstants.groupResourceTypeName))
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
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public Collection getAllResourceGroups() {
        return getResourceGroupDAO().findAll();
    }
    

    /**
     * Get all compatible resource groups of the given entity type and
     * resource type.
     *
     * @ejb:interface-method
     */
    public Collection getCompatibleResourceGroups(AuthzSubject subject,
                                                  Resource resProto)
        throws FinderException, PermissionException
    {
        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List groupIds =
            pm.findOperationScopeBySubject(subject,
                                           AuthzConstants.groupOpViewResourceGroup,
                                           AuthzConstants.groupResourceTypeName,
                                           PageControl.PAGE_ALL);

        Collection groups = getResourceGroupDAO().findCompatible(resProto);
        for (Iterator i = groups.iterator(); i.hasNext(); ) {
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
    private PageList getAllResourceGroups(AuthzSubject subject,
                                         PageControl pc,
                                         boolean excludeRoot)
        throws PermissionException, FinderException 
    {
        Collection groups = getAllResourceGroups(subject, excludeRoot);
        return _ownedGroupPager.seek(groups, pc.getPagenum(), pc.getPagesize());
    }   

    /** 
     * Get the resource groups with the specified ids
     * @param ids the resource group ids
     * @param pc Paging information for the request
     * @ejb:interface-method
     */
    public PageList getResourceGroupsById(AuthzSubject whoami,
                                          Integer[] ids,
                                          PageControl pc)
        throws PermissionException, FinderException 
    {
        if (ids.length == 0)
            return new PageList();
        
        PageControl allPc = new PageControl();
        // get all roles, sorted but not paged
        allPc.setSortattribute(pc.getSortattribute());
        allPc.setSortorder(pc.getSortorder());
        List all = getAllResourceGroups(whoami, allPc);

        // build an index of ids
        HashSet index = new HashSet();
        index.addAll(Arrays.asList(ids));
        int numToFind = index.size();

        // find the requested roles
        List groups = new ArrayList(numToFind);
        Iterator i = all.iterator();
        while (i.hasNext() && groups.size() < numToFind) {
            ResourceGroupValue g = (ResourceGroupValue) i.next();
            if (index.contains(g.getId()))
                groups.add(g);
        }

        PageList plist =
            _groupPager.seek(groups, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(groups.size());

        return plist;
    }

    /**
     * Associate roles with this group.
     * @param whoami The subject.
     * @param group The group to associate the roles with.
     * @param roles The roles to associate with the group.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami may not perform addRole on this group.
     * @ejb:interface-method
     */
    public void addRoles(AuthzSubject whoami, ResourceGroupValue group,
                         RoleValue[] roles) 
    {
        Set roleLocals = toPojos(roles);
        Iterator it = roleLocals.iterator();
        ResourceGroup groupLocal = lookupGroup(group);

        while (it != null && it.hasNext()) {
            Role role = (Role)it.next();
            groupLocal.addRole(role);
        }
    }

    /**
     * Disassociate roles from this group.
     * @param whoami The current running user.
     * @param group The group.
     * @param roles The roles to disassociate.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami may not perform removeRole on this group.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeRoles(AuthzSubject whoami, ResourceGroupValue group,
                            RoleValue[] roles)
        throws FinderException, PermissionException 
    {
        Set roleLocals = toPojos(roles);
        Iterator it = roleLocals.iterator();
        ResourceGroup groupLocal = lookupGroup(group);

        while (it != null && it.hasNext()) {
            Role role = (Role)it.next();
            groupLocal.removeRole(role);
        }
    }

    /**
     * Disassociate all roles from this role.
     * @param whoami The current running user.
     * @param group The group.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami may not perform removeRole on this group.
     * @ejb:interface-method
     */
    public void removeAllRoles(AuthzSubject whoami,
                               ResourceGroupValue group)
        throws FinderException, PermissionException 
    {
        ResourceGroup groupLocal = lookupGroup(group);
        removeRoles(whoami, group, (RoleValue[])fromLocals(groupLocal.getRoles(), 
                                                           RoleValue.class));
    }

    /**
     * Set the roles for this group.
     * To get the roles call getOperationValues() on the value-object.
     * @param whoami The current running user.
     * @param group This group.
     * @param roles Operations to associate with this group.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami is not allowed to perform setRoles 
     *                                on this group.
     * @ejb:interface-method
     */
    public void setRoles(AuthzSubject whoami, ResourceGroupValue group,
                         RoleValue[] roles)
        throws FinderException, PermissionException 
    {
        toPojos(roles);
        
        /* remove all roles */
        removeAllRoles(whoami, group);

        /* add subject to new set of roles */
        addRoles(whoami, group, roles);
    }

    /**
     * List the roles this group belongs to.
     * @param whoami The current running user.
     * @param groupValue This group.
     * @return Array of roles in this role.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami is not allowed to perform listRoles 
     *                                on this group.
     * @ejb:interface-method
     */
    public RoleValue[] getRoles(AuthzSubject whoami,
                                ResourceGroupValue groupValue) 
    {
        ResourceGroup groupLocal = lookupGroup(groupValue);
        /**
         This is no longer required. Viewing dependent entities is
         done based on whether or not you can see the group
        perm.check(this.lookupSubject(whoami),
                   groupLocal.getResource().getResourceType(),
                   groupLocal.getId(),
                   AuthzConstants.groupOpListRoles);
        **/
        return (RoleValue[])fromLocals(groupLocal.getRoles(), RoleValue.class);
    }

    /**
     * Change owner of a group.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void changeGroupOwner(AuthzSubject subject, ResourceGroup group,
                                 AuthzSubject newOwner)
        throws PermissionException 
    {
        ResourceManagerEJBImpl.getOne().setResourceOwner(subject, 
                                                         group.getResource(), 
                                                         newOwner);
        group.setModifiedBy(newOwner.getName());
    }
    
    /**
     * Get a ResourceGroup owner's AuthzSubjectValue
     * @param gid The group id
     * @exception FinderException Unable to find a group by id
     * @ejb:interface-method
     */
    public AuthzSubject getResourceGroupOwner(Integer gid)
        throws FinderException 
    {
        ResourceManagerLocal rmLoc = ResourceManagerEJBImpl.getOne();

        ResourceValue gResource =
            rmLoc.findResourceByInstanceId(rmLoc.findResourceTypeByName(
                AuthzConstants.groupResourceTypeName), gid);
        return gResource.getAuthzSubjectValue();
    }


    /**
     * Set a ResourceGroup modifiedBy attribute
     * @param whoami user requesting to find the group
     * @param id The ID of the role you're looking for.
     * @ejb:interface-method
     */
    public void setGroupModifiedBy(AuthzSubject whoami, Integer id) {
        ResourceGroup groupLocal = getResourceGroupDAO().findById(id);
        groupLocal.setModifiedBy(whoami.getName());
    }

    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void updateGroupType(AuthzSubject subject, ResourceGroup g,
                                int groupType, int groupEntType,
                                int groupEntResType)
        throws PermissionException
    {
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(subject.getId(),
                 AuthzConstants.authzGroup,
                 g.getId(), AuthzConstants.perm_modifyResourceGroup);
        
        g.setGroupType(new Integer(groupType));

        Resource r = getResourceGroupPrototype(groupEntType,
                                               groupEntResType);
        g.setResourcePrototype(r);
    }

    /**
     * Get the maximum collection interval for a scheduled metric within a
     * compatible group of resources.
     *
     * @return The maximum collection time in milliseconds.
     * TODO:  This does not belong here.  Evict, evict!  -- JMT 04/01/08 
     * @ejb:interface-method
     */
    public long getMaxCollectionInterval(ResourceGroup g, Integer templateId) {
        Long max =
            getResourceGroupDAO().getMaxCollectionInterval(g, templateId);

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
     * @ejb:interface-method
     */
    public List getMetricsCollecting(ResourceGroup g, Integer templateId) {

        return getResourceGroupDAO().getMetricsCollecting(g, templateId);
    }
    
    public static ResourceGroupManagerLocal getOne() {
        try {
            return ResourceGroupManagerUtil.getLocalHome().create();
        } catch(Exception e) { 
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() throws CreateException {
        try {
            Pager.getPager(RESOURCE_PAGER);
            _groupPager      = Pager.getPager(GROUP_PAGER);
            _ownedGroupPager = Pager.getPager(OWNEDGROUP_PAGER);
        } catch (Exception e) {
            throw new CreateException("Could not create value pager: " + e);
        }
    }

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
