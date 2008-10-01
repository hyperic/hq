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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
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
    private Pager resourceTypePager = null;
    
    private final String RES_TYPE_PAGER =
        PagerProcessor_resourceType.class.getName();

    private ResourceEdgeDAO getResourceEdgeDAO() {
        return new ResourceEdgeDAO(DAOFactory.getDAOFactory());
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
        ResourceType rt = getResourceTypeDAO().findByName(name);
        
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

        ResourceEdgeDAO eDAO = getResourceEdgeDAO();
        ResourceRelation relation = getContainmentRelation();
            
        eDAO.create(res, res, 0, relation);  // Self-edge
        if (parent != null) {
            Collection ancestors = eDAO.findAncestorEdges(parent);
            eDAO.create(res, parent, -1, relation);
            eDAO.create(parent, res, 1, relation);
            
            for (Iterator i = ancestors.iterator(); i.hasNext();) {
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

    /**
     * Get the # of resources within HQ inventory
     * @ejb:interface-method
     */
    public Number getResourceCount() {
        return new Integer(getResourceDAO().size());
    }
    
    /**
     * Get the # of resource types within HQ inventory
     * @ejb:interface-method
     */
    public Number getResourceTypeCount() {
        return new Integer(getResourceTypeDAO().size());
    }
        
    /**
     * Get the Resource entity associated with this ResourceType.
     * @param type This ResourceType.
     * @ejb:interface-method
     */
    public Resource getResourceTypeResource(Integer typeId) {
        ResourceType resourceType = getResourceTypeDAO().findById(typeId);
        return resourceType.getResource();
    }

    /**
     * Find the Resource that has the given instance ID and ResourceType.
     * @param type The ResourceType of the Resource you're looking for.
     * @param instanceId Your ID for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     * @ejb:interface-method
     */
    public Resource findResourceByInstanceId(ResourceType type,
                                             Integer instanceId) {
        Resource resource = getResourceDAO().findByInstanceId(type.getId(),
                                                              instanceId);
        
        if (resource == null) {
            throw new RuntimeException("Unable to find resourceType=" + 
                                       type.getId() + " instanceId=" + 
                                       instanceId);
        }
        return resource;
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
                                                 Integer instanceId) {
        return getResourceDAO().findByInstanceId(typeId, instanceId);
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
    public Resource findResourceByTypeAndInstanceId(String type,
                                                    Integer instanceId) {
        ResourceType resType = getResourceTypeDAO().findByName(type);
        return getResourceDAO().findByInstanceId(resType.getId(), instanceId);
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
        return findPrototype(id);
    }

    /**
     * @ejb:interface-method
     */
    public void removeResource(AuthzSubject subject, Resource r)
        throws VetoException
    {
        long now = System.currentTimeMillis();
        
        ResourceDeleteCallback cb =
            AuthzStartupListener.getResourceDeleteCallback();
        cb.preResourceDelete(r);

        ResourceAudit.deleteResource(r, subject, now, now);
        ResourceEdgeDAO edgeDao = getResourceEdgeDAO();
        edgeDao.deleteEdges(r);
        
        getResourceDAO().remove(r);
    }
    
    /**
     * @ejb:interface-method
     */
    public void removeResources(AuthzSubject subject, AppdefEntityID[] ids)
        throws VetoException
    {
        ResourceDeleteCallback cb =
            AuthzStartupListener.getResourceDeleteCallback();
        ResourceDAO dao = getResourceDAO();
        // No factory method for ResourceEdgeDAO?
        ResourceEdgeDAO edgeDao = getResourceEdgeDAO();

        long now = System.currentTimeMillis();
        
        for (int i=0; i < ids.length; i++) {
            Resource r = dao.findByInstanceId(ids[i].getAuthzTypeId(), 
                                              ids[i].getId());
            cb.preResourceDelete(r);
            ResourceAudit.deleteResource(r, subject, now, now);
            edgeDao.deleteEdges(r);
            dao.remove(r);
        }
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
    
        Collection resTypes = getResourceTypeDAO().findAll();
        for (Iterator it = resTypes.iterator(); it.hasNext(); ) {
            ResourceType type = (ResourceType) it.next();
   
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
     * Find all the resources which are descendents of the given resource
     * @ejb:interface-method
     */
    public List findResourcesByParent(AuthzSubject subject, Resource res) {
        return getResourceDAO().findByResource(subject, res);
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

        return new PageList(ordResources, ordResources.size());
    }

    /**
     * Gets all the Resources owned by the given Subject.
     * @param subject The owner.
     * @return Array of resources owned by the given subject.
     * @ejb:interface-method
     */
    public Collection findResourceByOwner(AuthzSubject owner) {
        return getResourceDAO().findByOwner(owner);
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
            resourceTypePager = Pager.getPager(RES_TYPE_PAGER);
        } catch (Exception e) {
            throw new CreateException("Could not create value pager: " + e);
        }
    }

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
