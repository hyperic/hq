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
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
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
import org.hyperic.util.StringUtil;
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
    private static final Integer RELATION_CONTAINMENT_ID = new Integer(1); 
    
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
     */
    public ResourceType createResourceType(AuthzSubject whoami,
                                           ResourceTypeValue typeV,
                                           Operation[] operations) 
    {
        AuthzSubject whoamiPojo = lookupSubject(whoami.getId());
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
    public void removeResourceType(AuthzSubject whoami,
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
    public void saveResourceType(AuthzSubject whoami,
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
    public void addOperations(AuthzSubject whoami,
                              ResourceTypeValue type,
                              Operation[] operations) {
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
    public void removeOperations(AuthzSubject whoami,
                                 ResourceTypeValue type,
                                 Operation[] operations) {
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
    public void removeAllOperations(AuthzSubject whoami,
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
    public ResourceType findResourceTypeByName(String name)
        throws FinderException {
        ResourceType rt =
            DAOFactory.getDAOFactory().getResourceTypeDAO().findByName(name);
        
        if (rt == null)
            throw new FinderException("ResourceType " + name + " not found");
        
        return rt;
    }

    /**
     * Find a resource, acting as a resource prototype.
     * @ejb:interface-method
     */
    public Resource findResourcePrototypeByName(String name) {
        return getResourceDAO().findResourcePrototypeByName(name);
    }
    
    /**
     * Check if there are any resources of a given type
     * 
     * @ejb:interface-method
     */
    public boolean resourcesExistOfType(String typeName) {
        return getResourceDAO().resourcesExistOfType(typeName);
    }
        
    /**
     * Create a resource.
     * 
     * @ejb:interface-method
     */
    public Resource createResource(AuthzSubject owner, ResourceType rt,
                                   Resource prototype, Integer instanceId,
                                   String name, boolean system, Resource parent)
    {
        long start = System.currentTimeMillis();

        Resource res = getResourceDAO().create(rt, prototype, name, owner, 
                                               instanceId, system); 

        DAOFactory daoFact        = DAOFactory.getDAOFactory();
        ResourceEdgeDAO eDAO      = new ResourceEdgeDAO(daoFact);
        ResourceRelation relation = getContainmentRelation();
            
        eDAO.create(res, res, 0, relation);  // Self-edge
        if (parent != null) {
            Collection ancestors = eDAO.findAncestorEdges(parent);
            eDAO.create(res, parent, -1, relation);
            eDAO.create(parent, res, 1, relation);
            
            for (Iterator i=ancestors.iterator(); i.hasNext(); ) {
                ResourceEdge ancestorEdge = (ResourceEdge)i.next();
                
                int distance = ancestorEdge.getDistance() - 1;
                
                eDAO.create(res, ancestorEdge.getTo(), distance, relation);
                eDAO.create(ancestorEdge.getTo(), res, -distance, relation);
            }
        }
        
        ResourceAudit.createResource(res, owner, start, 
                                     System.currentTimeMillis());
        return res;
    }
        
    private ResourceRelation getContainmentRelation() {
        ResourceRelationDAO rDAO = 
            new ResourceRelationDAO(DAOFactory.getDAOFactory());
        return rDAO.findById(RELATION_CONTAINMENT_ID); 
    }
    
    /**
     * Get the Resource entity associated with this ResourceType.
     * @param type This ResourceType.
     * @ejb:interface-method
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
    public ResourceValue findResourceByInstanceId(ResourceType type,
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
     * @ejb:interface-method
     */
    public Resource findResourcePojoByInstanceId(ResourceType type,
                                                 Integer instanceId)
    {
        return getResourceDAO().findByInstanceId(type.getId(), instanceId);
    }

    /**
     * @ejb:interface-method
     */
    public Resource findResourcePojoByInstanceId(Integer typeId,
                                                 Integer instanceId)
    {
        return getResourceDAO().findByInstanceId(typeId, instanceId);
    }

    /**
     * Find the Resource that has the given ID 
     * @param id id for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     * @ejb:interface-method
     */
    public ResourceValue findResourceById(Integer id) {
        return findResourcePojoById(id).getResourceValue();
    }
    
    /**
     * Find's the root (id=0) resource
     * @ejb:interface-method
     */
    public Resource findRootResource() {
        return getResourceDAO().findRootResource();
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
        lookupResource(res);

        // XXX:  Fill this in -- what info can be changed, exactly?
        //resource.setResourceValue(res);
    }

    /**
     * @ejb:interface-method
     */
    public Resource findResource(AppdefEntityID id) {
        return getResourceDAO().findByInstanceId(id.getAuthzTypeId(), 
                                                 id.getId());
    }

    /**
     * @ejb:interface-method
     */
    public Resource findResourcePrototype(AppdefEntityTypeID id) {
        Integer authzType;
        
        switch(id.getType()) {
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
            throw new IllegalArgumentException("Unsupported prototype type: " +
                                               id.getType());
        }
        return getResourceDAO().findByInstanceId(authzType, id.getId());
    }
    
    /**
     * @ejb:interface-method
     */
    public void removeResources(AuthzSubject subject, AppdefEntityID[] ids)
        throws VetoException
    {
        ResourceDeleteCallback cb = AuthzStartupListener.getResourceDeleteCallback();
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
    public void setResourceOwner(AuthzSubject whoami, ResourceValue res,
                                 AuthzSubject newOwner)
        throws PermissionException 
    {
        Resource resource = lookupResource(res);
        setResourceOwner(whoami, resource, newOwner);
    }
    
    /**
     * @ejb:interface-method
     */
    public void setResourceOwner(AuthzSubject whoami, Resource resource,
                                 AuthzSubject newOwner)
        throws PermissionException 
    {
        PermissionManager pm = PermissionManagerFactory.getInstance(); 

        if (pm.hasAdminPermission(whoami.getId()) ||
            getResourceDAO().isOwner(resource, whoami.getId())) {
            resource.setOwner(newOwner);
        } else {
            throw new PermissionException("Only an owner or admin may " +
                                          "reassign ownership.");
        }
    }
    

    /**
     * Get all the resource types
     * @param subject
     * @param pc Paging information for the request
     * @ejb:interface-method
     */
    public List getAllResourceTypes(AuthzSubject subject, PageControl pc) {
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
     */
    public List findViewableInstances(AuthzSubject subject,
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
        return pm.findViewableResources(subject, typeName, resName,
                                        appdefTypeStr, typeId, pc);
    }

    /**
     * Get viewable resources  by "type" OR "resource name"
     *
     * @param subject
     * @return Map of resource values
     * @ejb:interface-method
     */
    public PageList findViewables(AuthzSubject subject, String searchFor,
                                  PageControl pc) {
        ResourceDAO dao = getResourceDAO();
        PermissionManager pm = PermissionManagerFactory.getInstance(); 
        List resIds = pm.findViewableResources(subject, searchFor, pc);
        Pager pager = Pager.getDefaultPager();
        List paged = pager.seek(resIds, pc);

        PageList resources = new PageList();
        for (Iterator it = paged.iterator(); it.hasNext(); ) {
            Integer id = (Integer) it.next();
            resources.add(dao.findById(id));
        }
        
        resources.setTotalSize(resIds.size());
        return resources;
    }

    /**
     * Get viewable resources either by "type" OR "resource name"
     * OR "type AND resource name".
     *
     * @param subject
     * @return Map of resource values
     * @ejb:interface-method
     */
    public Map findAllViewableInstances(AuthzSubject subject) {
        // First get all resource types
        HashMap resourceMap = new HashMap();
    
        Collection resTypes =
            getAllResourceTypes(subject, PageControl.PAGE_ALL);
        for (Iterator it = resTypes.iterator(); it.hasNext(); ) {
            ResourceTypeValue type = (ResourceTypeValue) it.next();
   
            String typeName = type.getName();
                    
            // Now fetch list by the type
            List ids = findViewableInstances(subject,  typeName,  null, null,
                                             null, PageControl.PAGE_ALL);
            if (ids.size() > 0)
                resourceMap.put(typeName, ids);
        }
      
        return resourceMap;
    }

    /**
     * Find all the resources of an authz resource type
     * 
     * @param resourceType 301 for platforms, etc.
     * @param pInfo A pager, using a sort field of {@link ResourceSortField}
     * @return a list of {@link Resource}s
     * @ejb:interface-method
     */
    public List findResourcesOfType(int resourceType, PageInfo pInfo) { 
        return getResourceDAO().findResourcesOfType(resourceType, pInfo);
    }

    /**
     * Find all the resources which have the specified prototype
     * @return a list of {@link Resource}s
     * @ejb:interface-method
     */
    public List findResourcesOfPrototype(Resource proto, PageInfo pInfo) { 
        return getResourceDAO().findResourcesOfPrototype(proto, pInfo);
    }
    
    /**
     * Get all resources which are prototypes of platforms, servers, and
     * services.
     * 
     * @ejb:interface-method
     */
    public List findAllAppdefPrototypes() {
        return getResourceDAO().findAllAppdefPrototypes();
    }
    
    /**
     * Get viewable service resources. Service resources include individual
     * cluster unassigned services as well as service clusters. 
     *
     * @param subject
     * @param pc control
     * @return PageList of resource values
     * @ejb:interface-method
     */
    public PageList findViewableSvcResources(AuthzSubject subject,
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
            
            if (StringUtil.stringExists(res.getName(), resourceName))
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
     */
    public ResourceValue[] findResourceByOwner(AuthzSubject owner) {
        return (ResourceValue[]) this
                .fromPojos(getResourceDAO().findByOwner(owner),
                           org.hyperic.hq.authz.shared.ResourceValue.class);
    }

    /**
     * Gets all the Resources of a particular type owned by the given Subject.
     * @param resTypeName type
     * @param whoami The owner.
     * @return Array of resources owned by the given subject.
     * @exception NamingException
     * @exception FinderException Unable to find a given or dependent entities.
     * @ejb:interface-method
     */
    public ResourceValue[] findResourceByOwnerAndType(AuthzSubject whoami,
                                                      String resTypeName ) {
        AuthzSubject subj = getSubjectDAO().findById(whoami.getId());
        ResourceType resType = getResourceTypeDAO().findByName(resTypeName);
        return (ResourceValue[]) fromPojos(
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
