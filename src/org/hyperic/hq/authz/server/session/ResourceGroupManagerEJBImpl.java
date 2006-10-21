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
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.dao.ResourceDAO;
import org.hyperic.hibernate.dao.ResourceGroupDAO;
import org.hyperic.hq.authz.Resource;
import org.hyperic.hq.authz.ResourceGroup;
import org.hyperic.hq.authz.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupLocal;
import org.hyperic.hq.authz.shared.ResourceGroupPK;
import org.hyperic.hq.authz.shared.ResourceGroupUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleLocal;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.SystemException;
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
 */
public class ResourceGroupManagerEJBImpl extends AuthzSession implements SessionBean {

    protected Log log = LogFactory.getLog("org.hyperic.hq.authz.server." +
                                          "session.ResourceGroupManagerEJBImpl");
    private Pager resourcePager = null;
    private Pager groupPager = null;
    private Pager ownedGroupPager = null;
    private final String RESOURCE_PAGER =
        "org.hyperic.hq.authz.server.session.PagerProcessor_resource";
    private final String GROUP_PAGER = 
        "org.hyperic.hq.authz.server.session.PagerProcessor_resourceGroup";
    private final String OWNEDGROUP_PAGER = 
        "org.hyperic.hq.authz.server.session.PagerProcessor_ownedResourceGroup";

    /**
     * List the ResourceGroups associated with this resource.
     * @param whoami The current running user.
     * @param resource This resource.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ResourceGroupValue[] getResourceGroups(ResourceValue res)
        throws NamingException, FinderException {
        ResourceLocal resLocal = this.lookupResource(res);
        /** TODO PermissionCheck **/
        return (ResourceGroupValue[])this.fromLocals(resLocal.getResourceGroups(), org.hyperic.hq.authz.shared.ResourceGroupValue.class);
    }

    /**
     * Create a ResourceGroup.
     * @param whoami The current running user.
     * @param group The group o be created.
     * @param roles Roles to associate with the new ResourceGroup. Use null
     * if you want to associate operations later.
     * @param resources Resources to add to the new role. Use null to add subjects later.
     * @return ResourceValue for the role.
     * @exception CreateException Unable to create the specified entity.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami may not perform createResource on the given type.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public ResourceGroupValue createResourceGroup(AuthzSubjectValue whoami,
                                                  ResourceGroupValue group,
                                                  RoleValue[] roles,
                                                  ResourceValue[] resources)
        throws CreateException, FinderException, PermissionException {
        ResourceGroupLocal groupLocal;
		try {
            AuthzSubjectLocal whoamiLocal = getSubjectHome()
                .findByPrimaryKey(whoami.getPrimaryKey());
			groupLocal = getGroupHome().create(whoamiLocal, group);
			
			/* associated resources */
			groupLocal.setResources(this.toLocals(resources));
			
			/* associated roles */
			groupLocal.setRoles(this.toLocals(roles));
			
			/** No Permission Check required for Creating Group
			    Anyone can do this **/
		} catch (CreateException e) {
            rollback();
            throw e;
		} catch (NamingException e) {
			throw new SystemException(e);
		} catch (FinderException e) {
            rollback();
            throw e;
		}

        return groupLocal.getResourceGroupValue();
    }

    /**
     * Find the group that has the given ID.
     * @param whoami user requesting to find the group
     * @param id The ID of the role you're looking for.
     * @return The value-object of the group of the given ID.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami does not have viewResourceGroup
     * operation for requested group.
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ResourceGroupValue findResourceGroupById(AuthzSubjectValue whoami,
                                                    Integer id)
        throws NamingException, FinderException, PermissionException
    {
        ResourceGroupPK pk = new ResourceGroupPK(id);
        ResourceGroupLocal groupLocal = getGroupHome().findByPrimaryKey(pk);

        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(),
                 groupLocal.getResource().getResourceType(),
                 groupLocal.getId(),
                 AuthzConstants.groupOpViewResourceGroup);
        return groupLocal.getResourceGroupValue();
    }

    /**
     * Find the role that has the given name.
     * @param whoami user requesting to find the group
     * @param name The name of the role you're looking for.
     * @return The value-object of the role of the given name.
     * @throws PermissionException whoami does not have viewResourceGroup
     * on the requested group
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ResourceGroupValue findResourceGroupByName(AuthzSubjectValue whoami,
                                                      String name)
        throws PermissionException {
        ResourceGroup resGroup = getResourceGroupDAO().findByName(name);
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(),
                 resGroup.getResource().getResourceType(),
                 resGroup.getId(),
                 AuthzConstants.groupOpViewResourceGroup);
        return resGroup.getResourceGroupValue();
    }

    /**
     * Write the specified entity out to permanent storage.
     * @param whoami The current running user.
     * @param group The ResourceGroup to save.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami may not perform modifyResourceGroup on this group.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void saveResourceGroup(AuthzSubjectValue whoami,
                                  ResourceGroupValue group)
        throws NamingException, FinderException, PermissionException
    {
        ResourceGroupLocal groupLocal = this.lookupGroup(group);
        PermissionManager pm = PermissionManagerFactory.getInstance();
        pm.check(whoami.getId(),
                 groupLocal.getResource().getResourceType(),
                 groupLocal.getId(),
                 AuthzConstants.groupOpModifyResourceGroup);
        // check if the name has changed. If it has, update the resource
        if(!group.getName().equals(groupLocal.getName())) {
            groupLocal.getResource().setName(group.getName());
        }
        groupLocal.setResourceGroupValue(group);
    }

    /**
     * Delete the specified ResourceGroup.
     * @param whoami The current running user.
     * @param group The group to delete.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception RemoveException Unable to delete the specified entity.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeResourceGroup(AuthzSubjectValue whoami,
                                    ResourceGroupValue group)
        throws NamingException, FinderException, RemoveException,
               PermissionException
    {
        ResourceGroupLocal groupLocal =
            getGroupHome().findByPrimaryKey(group.getPrimaryKey());
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 groupLocal.getResource().getResourceType(),
                 groupLocal.getId(),
                 AuthzConstants.groupOpRemoveResourceGroup);
        groupLocal.remove();
    }

    /**
     * Associate resources with this group.
     * @param whoami The current running user.
     * @param group The group.
     * @param resources The resources to associate with the group.
     * @throws PermissionException whoami does not own the resources.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void addResources(AuthzSubjectValue whoami,
                             ResourceGroupValue group,
                             ResourceValue[] resources)
        throws PermissionException {
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
            grpDao.addResource(resGroup, resource);
        }
    }

    /**
     * Add a resource to a group by resource id and resource type
     * @param subjectValue - whoami
     * @param resourceGroup - the group you want to modify
     * @param resId - resource Id
     * @param resourceType - the type of resource
     * @throws PermissionException 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ResourceGroupValue addResource(AuthzSubjectValue whoami,
                                          ResourceGroupValue group,
                                          Integer instId, 
                                          ResourceTypeValue type)
        throws PermissionException {
        ResourceGroupDAO dao = getResourceGroupDAO();
        ResourceGroup resGroup = dao.findByName(group.getName());

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup,
                 resGroup.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        // now look up the resource by type and id
        ResourceType resType = getResourceTypeDAO().findByName(type.getName());
        Resource resource = getResourceDAO().findByInstanceId(resType, instId);
        dao.addResource(resGroup, resource);
        return resGroup.getResourceGroupValue();
    }
 
    /**
     * RemoveResources from a group.
     * @param whoami The current running user.
     * @param group The group .
     * @param resources The resources to disassociate.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception NamingException
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeResources(AuthzSubjectValue whoami,
                                ResourceGroupValue group,
                                ResourceValue[] resVals)
        throws FinderException, NamingException, PermissionException {
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
    }

    /**
     * Disassociate all resources from this group.
     * @param whoami The current running user.
     * @param group The group.
     * @throws PermissionException 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeAllResources(AuthzSubjectValue whoami,
                                   ResourceGroupValue group)
        throws PermissionException {
        ResourceGroupDAO dao = getResourceGroupDAO();
        ResourceGroup resGroup = dao.findByName(group.getName());

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 AuthzConstants.authzGroup,
                 resGroup.getId(),
                 AuthzConstants.perm_modifyResourceGroup);

        dao.removeAllResources(resGroup);
    }

    /**
     * Set the resources for this group.
     * To get the operations call getOperationValues() on the value-object.
     * @param whoami The current running user.
     * @param group This group.
     * @param resources Resources to associate with this role.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami does not own the resource.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void setResources(AuthzSubjectValue whoami,
                             ResourceGroupValue group,
                             ResourceValue[] resources)
        throws NamingException, FinderException, PermissionException {
        AuthzSubjectLocal whoamiLocal =
            getSubjectHome().findByPrimaryKey(whoami.getPrimaryKey());
        ResourceGroupLocal groupLocal =
            getGroupHome().findByPrimaryKey(group.getPrimaryKey());
        Set rLocals = this.toLocals(resources);

        groupLocal.setWhoami(whoamiLocal);

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 groupLocal.getResource().getResourceType(),
                 groupLocal.getId(),
                 AuthzConstants.groupOpModifyResourceGroup);

        groupLocal.removeAllResources();
        groupLocal.addResources(rLocals);
    }

    /**
     * List the resources in this group that the caller is authorized to see.
     * @param whoami The current running user.
     * @param group This group.
     * @param pc Paging information for the request
     * @return list of authorized resources in this group.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami is not allowed to perform listResources in this group.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList getResources(AuthzSubjectValue whoami,
                                 ResourceGroupValue groupValue,
                                 PageControl pc)
        throws NamingException, FinderException, PermissionException
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
                return resourcePager.seek(reversed, pc.getPagenum(),
                                          pc.getPagesize());
            } else {
                return resourcePager.seek(resources, pc.getPagenum(),
                                           pc.getPagesize());
            }
        default:
            throw new IllegalArgumentException("Invalid sort attribute:" +
                                               pc.getSortattribute());
        }
    }

    /**
     * Get all the resource groups including the root resource group.
     * @param subject
     * @return groupList
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List getAllResourceGroups(AuthzSubjectValue subject, PageControl pc)
        throws NamingException, FinderException, PermissionException
    {
        return getAllResourceGroups(subject,pc,false);
    }

    /**
     * Get all the resource groups that contain a particular resource. Exclude the root 
     * resource group.
     * @param subject
     * @return groupList
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList getAllResourceGroupsResourceInclusive(AuthzSubjectValue 
                                                          subject, PageControl pc,
                                                          ResourceValue resource) 
        throws PermissionException, FinderException
    {
        List toBePaged = new ArrayList();

        try {
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
                if (pc.isAscending()) {
                    incGroups = ResourceGroupUtil.getLocalHome()
                        .findContaining_orderName_asc(resource.getInstanceId(),
                            resource.getResourceTypeValue().getId());
                } else {
                    incGroups = ResourceGroupUtil.getLocalHome()
                        .findContaining_orderName_desc(resource.getInstanceId(),
                            resource.getResourceTypeValue().getId());
                }
                break;
            default:
                incGroups = ResourceGroupUtil.getLocalHome()
                    .findContaining_orderName_asc(resource.getInstanceId(),
                        resource.getResourceTypeValue().getId());
            }

            for (Iterator i=incGroups.iterator();i.hasNext();) {
                ResourceGroupLocal rgLoc = (ResourceGroupLocal) i.next();
                if (authGroupIds.contains(rgLoc.getId())) {
                    toBePaged.add(rgLoc);
                }
            }
        }
        catch (NamingException e) {
            throw new SystemException(e);
        }
        return ownedGroupPager.seek(toBePaged, pc.getPagenum(), pc.getPagesize());
    }   

    /**
     * Get all the resource groups excluding the root resource group.
     * @param subject
     * @return groupList
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList getAllResourceGroups(AuthzSubjectValue subject,
                                         PageControl pc,
                                         boolean excludeRoot)
        throws NamingException, FinderException, PermissionException {
        // first get the list of groups subject can view
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        List groupIds =
            pm.findOperationScopeBySubject(subject, 
                                           AuthzConstants.groupOpViewResourceGroup, 
                                           AuthzConstants.groupResourceTypeName, 
                                           PageControl.PAGE_ALL);
        
        // now build a collection for all of them
        Collection groups = new ArrayList();
        for(int i = 0; i < groupIds.size(); i++) {
            ResourceGroupPK anId =
                new ResourceGroupPK((Integer) groupIds.get(i));
            ResourceGroupLocal rgloc = getGroupHome().findByPrimaryKey(anId);
            if (excludeRoot) {
                if (!rgloc.getName().equals(AuthzConstants.groupResourceTypeName))
                    groups.add(rgloc);
            } else {
                groups.add(rgloc);
            }
        }

        return ownedGroupPager.seek(groups, pc.getPagenum(), pc.getPagesize());
    }   

    /** Get the resource groups with the specified ids
     * @param subject
     * @param ids the resource group ids
     * @param pc Paging information for the request
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     *
     */
    public PageList getResourceGroupsById(AuthzSubjectValue whoami,
                                          Integer[] ids,
                                          PageControl pc)
        throws NamingException, FinderException, PermissionException {
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
            if (index.contains(((ResourceGroupPK) g.getPrimaryKey()).getId()))
                groups.add(g);
        }

        PageList plist =
            groupPager.seek(groups, pc.getPagenum(), pc.getPagesize());
        plist.setTotalSize(groups.size());

        return plist;
    }

    /**
     * Associate roles with this group.
     * @param whoami The current running user.
     * @param subject The subject.
     * @param roles The roles to associate with the group.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception NamingException
     * @exception PermissionException whoami may not perform addRole on this group.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void addRoles(AuthzSubjectValue whoami, ResourceGroupValue group,
                         RoleValue[] roles)
        throws FinderException, NamingException, PermissionException {
        Set roleLocals = this.toLocals(roles);
        Iterator it = roleLocals.iterator();
        ResourceGroupLocal groupLocal = this.lookupGroup(group);

        while (it != null && it.hasNext()) {
            RoleLocal role = (RoleLocal)it.next();
            role.setWhoami(this.lookupSubject(whoami));
            role.addResourceGroup(groupLocal);
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
        throws FinderException, NamingException, PermissionException {
        Set roleLocals = this.toLocals(roles);
        Iterator it = roleLocals.iterator();
        ResourceGroupLocal groupLocal = this.lookupGroup(group);

        while (it != null && it.hasNext()) {
            RoleLocal role = (RoleLocal)it.next();
            role.setWhoami(this.lookupSubject(whoami));
            role.removeResourceGroup(groupLocal);
        }
    }

    /**
     * Disassociate all roles from this role.
     * @param whoami The current running user.
     * @param group The group.
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception NamingException
     * @exception PermissionException whoami may not perform removeRole on this group.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeAllRoles(AuthzSubjectValue whoami,
                               ResourceGroupValue group)
        throws FinderException, NamingException, PermissionException {
        ResourceGroupLocal groupLocal = this.lookupGroup(group);
        removeRoles(whoami, group, (RoleValue[])this.fromLocals(groupLocal.getRoles(), org.hyperic.hq.authz.shared.RoleValue.class));
    }

    /**
     * Set the roles for this group.
     * To get the roles call getOperationValues() on the value-object.
     * @param whoami The current running user.
     * @param group This group.
     * @param roles Operations to associate with this group.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami is not allowed to perform setRoles on this group.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void setRoles(AuthzSubjectValue whoami, ResourceGroupValue group,
                         RoleValue[] roles)
        throws NamingException, FinderException, PermissionException {
        this.toLocals(roles);
        
        /* remove all roles */
        removeAllRoles(whoami, group);

        /* add subject to new set of roles */
        addRoles(whoami, group, roles);
    }

    /**
     * List the roles this group belongs to.
     * @param whoami The current running user.
     * @param group This group.
     * @return Array of roles in this role.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @exception PermissionException whoami is not allowed to perform listRoles on this group.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public RoleValue[] getRoles(AuthzSubjectValue whoami,
                                ResourceGroupValue groupValue)
        throws NamingException, FinderException, PermissionException {
        ResourceGroupLocal groupLocal = this.lookupGroup(groupValue);
        /**
         This is no longer required. Viewing dependent entities is
         done based on whether or not you can see the group
        perm.check(this.lookupSubject(whoami),
                   groupLocal.getResource().getResourceType(),
                   groupLocal.getId(),
                   AuthzConstants.groupOpListRoles);
        **/
        return (RoleValue[])this.fromLocals(groupLocal.getRoles(),
                        org.hyperic.hq.authz.shared.RoleValue.class);
    }

    /**
     * Gives you a value-object with updated attributes.
     * With many of the methods actions are performed which update the
     * entity but not the associated value-object. Use this method
     * to sync up your value-object.
     * @param old Your current value-object.
     * @return A new ResourceGroup value-object.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ResourceGroupValue updateResourceGroupValue(ResourceGroupValue old)
        throws NamingException, FinderException {
        ResourceGroupPK pk = new ResourceGroupPK(old.getId());
        ResourceGroupLocal res =
            getGroupHome().findByPrimaryKey(pk);
        ResourceGroupValue newValue = res .getResourceGroupValue();
        return newValue;
    }

    /**
     * Get the Resource entity associated with this ResourceGroup.
     * @param subject This subject.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ResourceValue getResourceGroupResource(ResourceGroupValue group)
        throws NamingException, FinderException {
        ResourceGroupLocal res = getGroupHome().findByPrimaryKey(group.getPrimaryKey());
        return res.getResource().getResourceValue();
    }
    
    /**
     * Get a ResourceGroup owner's AuthzSubjectValue
     * @param group value object
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
     * @exception NamingException - JNDI failure
     * @exception FinderException Unable to find a group by id
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void setGroupModifiedBy(AuthzSubjectValue whoami, Integer id)
        throws NamingException, FinderException {
        ResourceGroupPK pk = new ResourceGroupPK(id);
        ResourceGroupLocal groupLocal = getGroupHome().findByPrimaryKey(pk);
        groupLocal.setModifiedBy(whoami.getName());
    }

    public void ejbCreate() throws CreateException {
        try {
            resourcePager = Pager.getPager(RESOURCE_PAGER);
            groupPager = Pager.getPager(GROUP_PAGER);
            ownedGroupPager = Pager.getPager(OWNEDGROUP_PAGER);
        } catch (Exception e) {
            throw new CreateException("Could not create value pager: " + e);
        }
    }

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}

