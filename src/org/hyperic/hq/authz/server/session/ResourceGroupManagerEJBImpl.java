/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
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
    private Log _log = LogFactory.getLog(ResourceGroupManagerEJBImpl.class);

    private Pager _resourcePager;
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
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @ejb:interface-method
     */
    public ResourceGroupValue[] getResourceGroups(ResourceValue res) {
        Resource resource = getResourceDAO().findById(res.getId());
        Collection pojos = getResourceGroupDAO().findContaining(resource);
        return (ResourceGroupValue[])
            fromPojos(pojos, ResourceGroupValue.class);
    }

    /**
     * Create a ResourceGroup.
     * @param whoami The current running user.
     * @param group The group o be created.
     * @param roles Roles to associate with the new ResourceGroup. Use null
     *              if you want to associate operations later.
     * @param resources Resources to add to the new role. Use null to add 
     *                  subjects later.
     * @exception PermissionException whoami may not perform createResource 
     *                                on the given type.
     * @ejb:interface-method
     */
    public ResourceGroupValue createResourceGroup(AuthzSubjectValue whoami,
                                                  ResourceGroupValue group,
                                                  RoleValue[] roles,
                                                  ResourceValue[] resources)
        throws PermissionException 
    {
		AuthzSubject whoamiLocal = getSubjectDAO().findById(whoami.getId());
        ResourceGroup res;

        group.setModifiedBy(whoamiLocal.getName());

        res = getResourceGroupDAO().create(whoamiLocal, group);
        res.setResourceSet(toPojos(resources));
        res.setRoles(toPojos(roles));

        GroupingStartupListener.getCallbackObj().postGroupCreate(res);
        return res.getResourceGroupValue();
    }

    /**
     * Find the group that has the given ID.
     * @param whoami user requesting to find the group
     * @param id The ID of the role you're looking for.
     * @ejb:interface-method
     */
    public ResourceGroup findResourceGroupById(AuthzSubjectValue whoami,
                                               Integer id)
        throws PermissionException, FinderException
    {
        ResourceGroup group = getResourceGroupDAO().get(id);
        if (group == null) {
            throw new FinderException("Group id=" + id + " not found.");
        }

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_viewResourceGroup);
        return group;
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
    public ResourceGroup findResourceGroupByName(AuthzSubjectValue whoami,
                                                 String name)
        throws PermissionException, FinderException 
    {
        ResourceGroup group = getResourceGroupDAO().findByName(name);
        if (group == null) {
            throw new FinderException("Group name=" + name + " not found.");
        }

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(), AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_viewResourceGroup);
        return group;
    }

    /**
     * Write the specified entity out to permanent storage.
     * @param whoami The current running user.
     * @param grpVal The ResourceGroup to save.
     * @throws PermissionException whoami may not perform modifyResourceGroup on
     *                             this group.
     * @ejb:interface-method
     */
    public void saveResourceGroup(AuthzSubjectValue whoami,
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
     * Delete the specified ResourceGroup.
     * @param whoami The current running user.
     * @param group The group to delete.
     * @ejb:interface-method
     */
    public void removeResourceGroup(AuthzSubjectValue whoami,
                                    ResourceGroupValue group)
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
    }

    /**
     * Associate resources with this group.
     * @param whoami The current running user.
     * @param group The group.
     * @param resources The resources to associate with the group.
     * @throws PermissionException whoami does not own the resources.
     * @ejb:interface-method
     */
    public void addResources(AuthzSubjectValue whoami,
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
        for (int i = 0; i < resources.length; i++) {
            Resource resource = resDao.findById(resources[i].getId());
            resGroup.addResource(resource);
        }

        GroupingStartupListener.getCallbackObj().groupMembersChanged(resGroup);
    }

    /**
     * Add a resource to a group by resource id and resource type
     * @ejb:interface-method
     */
    public ResourceGroupValue addResource(AuthzSubjectValue whoami,
                                          ResourceGroupValue group,
                                          Integer instId, 
                                          ResourceType type)
        throws PermissionException 
    {
        ResourceGroupDAO dao = getResourceGroupDAO();
        // reassociate group to session
        ResourceGroup resGroup = dao.findByName(group.getName());

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup,
                 resGroup.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        // now look up the resource by type and id
        ResourceType resType = getResourceTypeDAO().findByName(type.getName());
        Resource resource = getResourceDAO().findByInstanceId(resType, instId);
        resGroup.addResource(resource);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(resGroup);
        return resGroup.getResourceGroupValue();
    }
 
    /**
     * RemoveResources from a group.
     * @param whoami The current running user.
     * @param group The group .
     * @ejb:interface-method
     */
    public void removeResources(AuthzSubjectValue whoami,
                                ResourceGroupValue group,
                                ResourceValue[] resVals)
        throws PermissionException 
    {
        ResourceGroupDAO grpDao = getResourceGroupDAO();
        ResourceGroup groupLocal = grpDao.findByName(group.getName());

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup,
                 groupLocal.getId(),
                 AuthzConstants.perm_modifyResourceGroup);
        
        ResourceDAO resDao = getResourceDAO();
        Resource[] resources = new Resource[resVals.length];
        for (int i = 0; i < resVals.length; i++) {
            resources[i] = resDao.findById(resVals[i].getId());
        }
        grpDao.removeResources(groupLocal, resources);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(groupLocal);
    }

    /**
     * Disassociate all resources from this group.
     * @param whoami The current running user.
     * @param group The group.
     * @ejb:interface-method
     */
    public void removeAllResources(AuthzSubjectValue whoami,
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

        dao.removeAllResources(resGroup);
        GroupingStartupListener.getCallbackObj().groupMembersChanged(resGroup);
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
    public void setResources(AuthzSubjectValue whoami,
                             ResourceGroupValue grpVal,
                             ResourceValue[] resources)
        throws PermissionException 
    {
        ResourceGroup group =
            getResourceGroupDAO().findById(grpVal.getId());

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup, group.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        group.setResourceSet(toPojos(resources));
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
    public PageList getResources(AuthzSubjectValue whoami,
                                 ResourceGroupValue groupValue,
                                 PageControl pc) 
    {
        Collection resources;
        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);

        switch (pc.getSortattribute()) {
        case (SortAttribute.RESOURCE_NAME) :
        case (SortAttribute.RESGROUP_NAME) :
            PermissionManager pm = PermissionManagerFactory.getInstance();
            resources = pm.getGroupResources(whoami.getId(),
                                             groupValue.getId(),
                                             Boolean.FALSE);

            if (pc.isDescending()) {
                ArrayList reversed = new ArrayList(resources);
                Collections.reverse(reversed);
                return _resourcePager.seek(reversed, pc.getPagenum(),
                                          pc.getPagesize());
            } else {
                return _resourcePager.seek(resources, pc.getPagenum(),
                                           pc.getPagesize());
            }
        default:
            throw new IllegalArgumentException("Invalid sort attribute:" +
                                               pc.getSortattribute());
        }
    }

    /**
     * Get all the resource groups including the root resource group.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List getAllResourceGroups(AuthzSubjectValue subject, PageControl pc)
        throws PermissionException, FinderException, NamingException 
    {
        return getAllResourceGroups(subject, pc, false);
    }

    /**
     * Get all the resource groups that contain a particular resource. Exclude
     * the root resource group. 
     * 
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList 
        getAllResourceGroupsResourceInclusive(AuthzSubjectValue subject, 
                                              PageControl pc,
                                              ResourceValue resource)
        throws PermissionException, FinderException 
    {
        List toBePaged = new ArrayList();

        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);

        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        List authGroupIds =
            pm.findOperationScopeBySubject(subject, 
                                           AuthzConstants.groupOpViewResourceGroup,
                                           AuthzConstants.groupResourceTypeName, 
                                           pc);

        // Now fetch the ones that are inclusive.
        Collection incGroups;
        switch (pc.getSortattribute()) {
        case SortAttribute.RESOURCE_NAME:
            incGroups = getResourceGroupDAO()
             .findContaining_orderName(resource.getInstanceId(),
                                       resource.getResourceTypeValue().getId(),
                                       pc.isAscending());
            break;
        default:
            incGroups = getResourceGroupDAO()
              .findContaining_orderName(resource.getInstanceId(),
                                        resource.getResourceTypeValue().getId(),
                                        true);
        }

        for (Iterator i=incGroups.iterator();i.hasNext();) {
            ResourceGroup rgLoc = (ResourceGroup) i.next();
            if (authGroupIds.contains(rgLoc.getId())) {
                toBePaged.add(rgLoc);
            }
        }
        return _ownedGroupPager.seek(toBePaged, pc.getPagenum(), 
                                     pc.getPagesize());
    }   

    /**
     * Get all the resource groups excluding the root resource group.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public Collection getAllResourceGroups(AuthzSubjectValue subject,
                                           boolean excludeRoot)
        throws PermissionException, FinderException 
    {
        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        List groupIds =
            pm.findOperationScopeBySubject(subject, 
                                           AuthzConstants.groupOpViewResourceGroup, 
                                           AuthzConstants.groupResourceTypeName, 
                                           PageControl.PAGE_ALL);
        
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
     * Get all the resource groups excluding the root resource group and paged
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList getAllResourceGroups(AuthzSubjectValue subject,
                                         PageControl pc,
                                         boolean excludeRoot)
        throws PermissionException, FinderException, NamingException 
    {
        Collection groups = getAllResourceGroups(subject, excludeRoot);
        return _ownedGroupPager.seek(groups, pc.getPagenum(), pc.getPagesize());
    }   

    /** 
     * Get the resource groups with the specified ids
     * @param ids the resource group ids
     * @param pc Paging information for the request
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     *
     */
    public PageList getResourceGroupsById(AuthzSubjectValue whoami,
                                          Integer[] ids,
                                          PageControl pc)
        throws PermissionException, FinderException, NamingException 
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
     * @param subject The subject.
     * @param group The group to associate the roles with.
     * @param roles The roles to associate with the group.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami may not perform addRole on this group.
     * @ejb:interface-method
     */
    public void addRoles(AuthzSubjectValue subject, ResourceGroupValue group,
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
     * @exception NamingException
     * @exception PermissionException whoami may not perform removeRole on this group.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeRoles(AuthzSubjectValue whoami, ResourceGroupValue group,
                            RoleValue[] roles)
        throws FinderException, NamingException, PermissionException 
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
    public void removeAllRoles(AuthzSubjectValue whoami,
                               ResourceGroupValue group)
        throws FinderException, NamingException, PermissionException 
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
    public void setRoles(AuthzSubjectValue whoami, ResourceGroupValue group,
                         RoleValue[] roles)
        throws NamingException, FinderException, PermissionException 
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
     * @ejb:transaction type="SUPPORTS"
     */
    public RoleValue[] getRoles(AuthzSubjectValue whoami,
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
     * Get a ResourceGroup owner's AuthzSubjectValue
     * @param gid The group id
     * @exception NamingException - JNDI failure
     * @exception FinderException Unable to find a group by id
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public AuthzSubjectValue getResourceGroupOwner(Integer gid)
        throws NamingException, FinderException, CreateException 
    {
        ResourceManagerLocal rmLoc =
            ResourceManagerUtil.getLocalHome().create();

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
    public void setGroupModifiedBy(AuthzSubjectValue whoami, Integer id) {
        ResourceGroup groupLocal = getResourceGroupDAO().findById(id);
        groupLocal.setModifiedBy(whoami.getName());
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
            _resourcePager   = Pager.getPager(RESOURCE_PAGER);
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
