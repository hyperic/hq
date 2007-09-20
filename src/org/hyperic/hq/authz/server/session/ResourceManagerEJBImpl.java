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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.AIAudit;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ResourceAudit;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

/**
 * Use this session bean to manipulate Resources, ResourceTypes and
 * ResourceGroups. That is to say, Resources and their derivatives.
 * Alteratively you can say, anything enity that starts with the word Resource.
 *
 * All arguments and return values are value-objects.
 *
 * @ejb:bean name="ResourceManager"
 *      jndi-name="ejb/authz/ResourceManager"
 *      local-jndi-name="LocalResourceManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class ResourceManagerEJBImpl extends AuthzSession implements SessionBean
{

    private Pager resourcePager = null;
    private Pager resourceTypePager = null;
    
    private final String RESOURCE_PAGER =
        PagerProcessor_resource.class.getName();
    private final String RES_TYPE_PAGER =
        PagerProcessor_resourceType.class.getName();
    
    /**
     * Create a ResourceType.
     * @param whoami The current running user.
     * @param type The ResourceType to be created.
     * @param operations Operations to associate with the new role. Use null
     * if you want to associate operations later. Given operations will be
     * created. So make this these are nonexistent operations. Use
     * setOperations() to associate existing Operations.
     * @return Value-object for the ResourceType.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ResourceType createResourceType(AuthzSubjectValue whoami,
                                           ResourceTypeValue typeV,
                                           OperationValue[] operations) 
    {
        AuthzSubject whoamiPojo = this.lookupSubjectPojo(whoami);
        DAOFactory factory = DAOFactory.getDAOFactory();
        ResourceType type =
            factory.getResourceTypeDAO().create(whoamiPojo, typeV);
        Role rootRole =factory.getRoleDAO().findById(AuthzConstants.rootRoleId);
        
        /* create associated operations */
        if (operations != null) {
            for (int i = 0; i < operations.length; i++) {
                Operation op = type.createOperation(operations[i].getName());

                rootRole.addOperation(op);
            }
        }
        return type;
    }

    /**
     * Delete the specified ResourceType.
     * @param whoami The current running user.
     * @param type The type to delete.
     * @ejb:interface-method
     */
    public void removeResourceType(AuthzSubjectValue whoami,
                                   ResourceTypeValue type) {
        ResourceTypeDAO dao = DAOFactory.getDAOFactory().getResourceTypeDAO();
        ResourceType rt = dao.findById(type.getId());
        AuthzSubject who = new AuthzSubjectDAO(DAOFactory.getDAOFactory())
            .findById(whoami.getId());
        dao.remove(who, rt);
    }

    /**
     * Write the specified entity out to permanent storage.
     * @param whoami The current running user.
     * @param type The type to save.
     * @throws PermissionException whoami may not perform modifyResourceType on this role.
     * @ejb:interface-method
     */
    public void saveResourceType(AuthzSubjectValue whoami,
                                 ResourceTypeValue type)
        throws PermissionException {
        ResourceType resType = getResourceTypeDAO().findById(type.getId());

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        pm.check(whoami.getId(),
                 resType.getResource().getResourceType(), resType.getId(),
                 AuthzConstants.typeOpModifyResourceType);

        // XXX:  Fill this in -- what info can be changed, exactly?
        //resType.setResourceTypeValue(type);
    }

    /**
     * Associate operations with this role.
     * @param whoami The current running user.
     * @param type The type.
     * @param operations The operations to associate with the role. These
     * operations will be created. Use setOperations() to associate existing
     * operations.
     * @ejb:interface-method
     */
    public void addOperations(AuthzSubjectValue whoami,
                              ResourceTypeValue type,
                              OperationValue[] operations) {
        ResourceType resType = getResourceTypeDAO().findById(type.getId());
        Collection rtOps = resType.getOperations();
        rtOps.addAll(toPojos(operations));
    }

    /**
     * Disassociate operations from this role.
     * @param whoami The current running user.
     * @param type The type.
     * @param operations The roles to disassociate. These operations will be deleted.
     * @ejb:interface-method
     */
    public void removeOperations(AuthzSubjectValue whoami,
                                 ResourceTypeValue type,
                                 OperationValue[] operations) {
        Set opPojos = toPojos(operations);
        ResourceType resType = getResourceTypeDAO().findById(type.getId());
        for (Iterator it = resType.getOperations().iterator(); it.hasNext(); ) {
            Operation oper = (Operation) it.next();
            if (opPojos.contains(oper)) {
                it.remove();
            }
        }
    }

    /**
     * Disassociate all operations from this role. All operations will be deleted.
     * @param whoami The current running user.
     * @param type The role.
     * @ejb:interface-method
     */
    public void removeAllOperations(AuthzSubjectValue whoami,
                                    ResourceTypeValue type) {
        ResourceType resType = getResourceTypeDAO().findById(type.getId());
        resType.getOperations().clear();
    }

    /**
     * Find the type that has the given name.
     * @param name The name of the type you're looking for.
     * @return The value-object of the type of the given name.
     * @throws FinderException Unable to find a given or dependent entities.
     * @ejb:interface-method
     */
    public ResourceTypeValue findResourceTypeByName(String name)
        throws FinderException {
        ResourceType rt =
            DAOFactory.getDAOFactory().getResourceTypeDAO().findByName(name);
        
        if (rt == null)
            throw new FinderException("ResourceType " + name + " not found");
        
        return rt.getResourceTypeValue();
    }

    /**
     * Create a resource.
     * 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public Resource createResource(AuthzSubjectValue whoami,
                                   ResourceTypeValue rtv, Integer instanceId,
                                   String name, boolean system) 
    {
        long start = System.currentTimeMillis();
        AuthzSubject owner =
            getSubjectDAO().findByAuth(whoami.getName(),
                                       whoami.getAuthDsn());
        ResourceType rt = getResourceTypeDAO().findById(rtv.getId()); 

        Resource res = getResourceDAO().create(rt, name, owner, instanceId, 
                                               system);
        
        ResourceAudit.createResource(res, owner, start, 
                                     System.currentTimeMillis());
        return res;
    }
    
    /**
     * Get the Resource entity associated with this ResourceType.
     * @param type This ResourceType.
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ResourceValue getResourceTypeResource(ResourceTypeValue type) {
        ResourceType resourceType = getResourceTypeDAO().findById(type.getId());
        return resourceType.getResource().getResourceValue();
    }

    /**
     * Find the Resource that has the given instance ID and ResourceType.
     * @param type The ResourceType of the Resource you're looking for.
     * @param instanceId Your ID for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     * @ejb:interface-method
     */
    public ResourceValue findResourceByInstanceId(ResourceTypeValue type,
                                                  Integer instanceId) {
        Resource resource = getResourceDAO().findByInstanceId(type.getId(),
                                                              instanceId);
        
        if (resource == null) {
            throw new RuntimeException("Unable to find resourceType=" + 
                                       type.getId() + " instanceId=" + 
                                       instanceId);
        }
        return resource.getResourceValue();
    }

    /**
     * Find the Resource that has the given ID 
     * @param id id for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public ResourceValue findResourceById(Integer id) {
        return findResourcePojoById(id).getResourceValue();
    }
    
    /**
     * @ejb:interface-method
     */
    public Resource findResourcePojoById(Integer id) {
        return getResourceDAO().findById(id);
    }

    /**
     * Find the Resource that has the given instance ID and ResourceType name.
     * @param type The ResourceType of the Resource you're looking for.
     * @param instanceId Your ID for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     * @ejb:interface-method
     */
    public ResourceValue findResourceByTypeAndInstanceId(String type,
                                                  Integer instanceId) {
        ResourceType resType = getResourceTypeDAO().findByName(type);
        Resource resource = getResourceDAO().findByInstanceId(resType.getId(),
                                                              instanceId);
        return resource.getResourceValue();
    }

    /**
     * Write the specified entity out to permanent storage.
     *
     * @param res The Resource to save.
     * @ejb:interface-method
     */
    public void saveResource(ResourceValue res) {
        lookupResourcePojo(res);

        // XXX:  Fill this in -- what info can be changed, exactly?
        //resource.setResourceValue(res);
    }

    /**
     * Remove all measurements for an instance
     *
     * @ejb:interface-method
     */
    public void removeResources(AuthzSubject subject, AppdefEntityID[] ids) 
        throws VetoException
    {
        ResourceDeleteCallback cb = AuthzStartupListener.getCallbackObj();
        ResourceDAO dao = getResourceDAO();
        long now = System.currentTimeMillis();
        
        for (int i=0; i < ids.length; i++) {
            Resource r = dao.findByInstanceId(ids[i].getAuthzTypeId(), 
                                              ids[i].getId());
            cb.preResourceDelete(r);
            ResourceAudit.deleteResource(r, subject, now, now); 
        }
        getResourceDAO().deleteByInstances(ids);
    }

    /**
     * Set the owner of this Resource.
     * @param whoami The current running user.
     * @param res This Resource.
     * @param newOwner The new owner.
     * @throws PermissionException whoami does not own the resource.
     * @ejb:interface-method
     */
    public void setResourceOwner(AuthzSubjectValue whoami, ResourceValue res,
                                 AuthzSubjectValue newOwner)
        throws PermissionException {
        Resource resource = lookupResourcePojo(res);
        PermissionManager pm = PermissionManagerFactory.getInstance(); 

        if (pm.hasAdminPermission(whoami) ||
            getResourceDAO().isOwner(resource, whoami.getId())) {
            resource.setOwner(lookupSubjectPojo(newOwner));
        }
        else {
            throw new PermissionException("Only an owner or admin may " +
                                          "reassign ownership.");
        }
    }

    /**
     * Get all the resource types
     * @param subject
     * @param pc Paging information for the request
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public List getAllResourceTypes(AuthzSubjectValue subject, PageControl pc) {
        Collection resTypes = getResourceTypeDAO().findAll();
        pc = PageControl.initDefaults(pc, SortAttribute.RESTYPE_NAME);
        return resourceTypePager.seek(resTypes, pc.getPagenum(),
                                      pc.getPagesize());
    }

    /**
     * Get viewable resources either by "type" OR "resource name"
     * OR "type AND resource name".
     *
     * @param subject
     * @return Map of resource values
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public List findViewableInstances(AuthzSubjectValue subject,
                                      String typeName, 
                                      String resName,
                                      String appdefTypeStr,
                                      Integer typeId,
                                      PageControl pc) {
        // Authz type and/or resource name must be specified.
        if (typeName == null) {
            throw new IllegalArgumentException(
                "This method requires a valid authz type name argument");
        }

        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        return pm.findViewableResources(subject, typeName, resName, appdefTypeStr,
                                        typeId, pc);
    }

    /**
     * Get viewable resources either by "type" OR "resource name"
     * OR "type AND resource name".
     *
     * @param subject
     * @return Map of resource values
     * @ejb:interface-method
     * @ejb:transaction type="Required" 
     */
    public Map findAllViewableInstances(AuthzSubjectValue subject) {
        // First get all resource types
        HashMap resourceMap = new HashMap();
    
        Collection resTypes =
            getAllResourceTypes(subject, PageControl.PAGE_ALL);
        for (Iterator it = resTypes.iterator(); it.hasNext(); ) {
            ResourceTypeValue type = (ResourceTypeValue) it.next();
   
            String typeName = type.getName();
                    
            // Now fetch list by the type
            List ids = this.findViewableInstances(subject, 
                                                  typeName, 
                                                  null, null, null,
                                                  PageControl.PAGE_ALL);
            if (ids.size() > 0)
                resourceMap.put(typeName, ids);
        }
      
        return resourceMap;
    }

    
    /**
     * Find all the resources of an authz resource type
     * 
     * @param resourceType 301 for platforms, etc.
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required" 
     */
    public List findResourcesOfType(int resourceType, PageInfo pInfo) { 
        return getResourceDAO().findResourcesOfType(resourceType, pInfo);
    }
    
    /**
     * Get viewable service resources. Service resources include individual
     * cluster unassigned services as well as service clusters. 
     *
     * @param subject
     * @param pc control
     * @return PageList of resource values
     * @ejb:interface-method
     * @ejb:transaction type="Required" 
     */
    public PageList findViewableSvcResources(AuthzSubjectValue subject,
                                             String resourceName,
                                             PageControl pc) {
        Collection resources;

        AuthzSubject subj = getSubjectDAO().findById(subject.getId());

        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);

        PermissionManager pm = PermissionManagerFactory.getInstance(); 

        // Damn I love this code.  -- JMT
        switch(pc.getSortattribute()) {
            case SortAttribute.RESOURCE_NAME:
            default:
                resources = pm.findServiceResources(subj, Boolean.FALSE);
                break;
        }
        
        // TODO: Move filtering into EJBQL
        ArrayList ordResources = new ArrayList(resources.size());
        for (Iterator it = resources.iterator(); it.hasNext();) {
            Resource res = (Resource) it.next();
            
            if (resourceName != null &&
                res.getName().indexOf(resourceName) < 0)
                continue;
            
            if (pc.isDescending())  // Add to head of array list
                ordResources.add(0, res);
            else                    // Add to tail of array list
                ordResources.add(res);
        }

        return resourcePager.seek(ordResources, pc.getPagenum(),
                                  pc.getPagesize());
    }

    /**
     * Gets all the Resources owned by the given Subject.
     * @param subject The owner.
     * @return Array of resources owned by the given subject.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ResourceValue[] findResourceByOwner(AuthzSubjectValue subject) {
        AuthzSubject owner = getSubjectDAO().findById(subject.getId());
        return (ResourceValue[]) this
                .fromPojos(getResourceDAO().findByOwner(owner),
                           org.hyperic.hq.authz.shared.ResourceValue.class);
    }

    /**
     * Gets all the Resources of a particular type owned by the given Subject.
     * @param resTypeName type
     * @param subjVal The owner.
     * @return Array of resources owned by the given subject.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ResourceValue[] findResourceByOwnerAndType(AuthzSubjectValue subjVal,
                                                      String resTypeName ) {
        AuthzSubject subj = getSubjectDAO().findById(subjVal.getId());
        ResourceType resType = getResourceTypeDAO().findByName(resTypeName);
        return (ResourceValue[])this.fromPojos(
            getResourceDAO().findByOwnerAndType(subj,resType),
            org.hyperic.hq.authz.shared.ResourceValue.class);
    }

    public static ResourceManagerLocal getOne() {
        try {
            return ResourceManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() throws CreateException {
        try {
            resourcePager = Pager.getPager(RESOURCE_PAGER);
            resourceTypePager = Pager.getPager(RES_TYPE_PAGER);
        } catch (Exception e) {
            throw new CreateException("Could not create value pager: " + e);
        }
    }

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
