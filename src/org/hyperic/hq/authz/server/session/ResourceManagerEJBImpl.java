/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.server.session.ApplicationManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ResourcesCleanupZevent;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceEdgeCreateException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ResourceAudit;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;

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
 * @ejb:transaction type="Required"
 */
public class ResourceManagerEJBImpl extends AuthzSession implements SessionBean
{
    private final Log log = LogFactory.getLog(ResourceManagerEJBImpl.class);
    private Pager resourceTypePager = null;
    
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
            createResourceEdges(parent, res, relation, false);
            
            // TODO: Explore calling this when ResourceCreatedZevent
            // is processed instead
            createVirtualResourceEdges(owner, parent, res, system);
        }
        
        ResourceAudit.createResource(res, owner, start, 
                                     System.currentTimeMillis());
        return res;
    }
    
    /**
     * Move a resource.  It is the responsibility of the caller (AppdefManager) to
     * ensure that this resource can be moved to the destination.
     *
     * It's also of note that this method only deals with relinking resource
     * edges to the ancestors of the destination resource.  This means that in
     * the case of Server moves, it's up to the caller to re-link dependent
     * chilren.
     *
     * @ejb:interface-method
     */
    public void moveResource(AuthzSubject owner,
                             Resource target, Resource destination) {

        long start = System.currentTimeMillis();

        ResourceEdgeDAO eDAO = getResourceEdgeDAO();
        ResourceRelation relation = getContainmentRelation();

        // Clean out edges for the current target
        eDAO.deleteEdges(target);

        createResourceEdges(destination, target, relation, true);

        ResourceAudit.moveResource(target, destination, owner, start,
                                   System.currentTimeMillis());
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
        Resource resource = findResourceByInstanceId(type.getId(), instanceId);
        
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
    public Resource findResourceByInstanceId(Integer typeId, Integer instanceId)
    {
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
    public Resource findResourceById(Integer id) {
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
    public Resource findResource(AppdefEntityID aeid) {
        try {
            final Integer id = aeid.getId();
            switch (aeid.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVER :
                    ServerManagerLocal sMan = ServerManagerEJBImpl.getOne();
                    return sMan.findServerById(id).getResource();
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    PlatformManagerLocal pMan = PlatformManagerEJBImpl.getOne();
                    return pMan.findPlatformById(id).getResource();
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE :
                    ServiceManagerLocal svcMan = ServiceManagerEJBImpl.getOne();
                    return svcMan.findServiceById(id).getResource();
                case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                    // XXX not sure about appdef group mapping since 4.0
                    return getResourceDAO().findByInstanceId(
                        aeid.getAuthzTypeId(), id);
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    ApplicationManagerLocal appMan =
                        ApplicationManagerEJBImpl.getOne();
                    AuthzSubject overlord =
                        AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
                    return appMan.findApplicationById(overlord, id).getResource();
                default:
                    return getResourceDAO().findByInstanceId(
                        aeid.getAuthzTypeId(), id);
            }
        } catch (ServerNotFoundException e) {
        } catch (PlatformNotFoundException e) {
        } catch (ServiceNotFoundException e) {
        } catch (ApplicationNotFoundException e) {
        } catch (PermissionException e) {
        }
        return null;
    }

    /**
     * @ejb:interface-method
     */
    public Resource findResourcePrototype(AppdefEntityTypeID id) {
        return findPrototype(id);
    }

    /**
     * Removes the specified resource by nulling out its resourceType.
     * Will not null the resourceType of the resource which is passed in.
     * These resources need to be cleaned up eventually by
     * {@link AppdefBossEJBImpl.removeDeletedResources}.  This may be done in 
     * the background via zevent by issuing a {@link ResourcesCleanupZevent}.
     * @see {@link AppdefBossEJBImpl.removeDeletedResources}
     * @see {@link ResourcesCleanupZevent}
     * @param r {@link Resource} resource to be removed.
     * @param nullResourceType tells the method to null out the resourceType
     * @return AppdefEntityID[] - an array of the resources (including children) deleted
     * @ejb:transaction type="NotSupported"
     * @ejb:interface-method
     */
    public AppdefEntityID[] removeResourcePerms(AuthzSubject subj, Resource r,
                                                boolean nullResourceType)
        throws VetoException, PermissionException
    {
        final ResourceType resourceType = r.getResourceType();

        // Possible this resource has already been marked for deletion
        if (resourceType == null) {
            return new AppdefEntityID[0];
        }
        
        // Make sure user has permission to remove this resource
        final PermissionManager pm = PermissionManagerFactory.getInstance();
        String opName = null;
        Set removed = new HashSet();

        if (resourceType.getId().equals(AuthzConstants.authzPlatform)) {
            opName = AuthzConstants.platformOpRemovePlatform;
        } else if (resourceType.getId().equals(AuthzConstants.authzServer)) {
            opName = AuthzConstants.serverOpRemoveServer;
        } else if (resourceType.getId().equals(AuthzConstants.authzService)) {
            opName = AuthzConstants.serviceOpRemoveService;
        } else if (resourceType.getId().equals(AuthzConstants.authzApplication)) {
            opName = AuthzConstants.appOpRemoveApplication;
        } else if (resourceType.getId().equals(AuthzConstants.authzGroup)) {
            opName = AuthzConstants.groupOpRemoveResourceGroup;
        }

        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        if (debug) watch.markTimeBegin("removeResourcePerms.pmCheck");
        pm.check(subj.getId(), resourceType, r.getInstanceId(), opName);
        if (debug) watch.markTimeEnd("removeResourcePerms.pmCheck");

        ResourceEdgeDAO edgeDao = getResourceEdgeDAO();
        if (debug) watch.markTimeBegin("removeResourcePerms.findEdges");
        Collection edges = edgeDao.findDescendantEdges(r, getContainmentRelation());
        edges.addAll(edgeDao.findDescendantEdges(r, getVirtualRelation()));
        if (debug) watch.markTimeEnd("removeResourcePerms.findEdges");
        for (Iterator it = edges.iterator(); it.hasNext(); ) {
            ResourceEdge edge = (ResourceEdge) it.next();
            // Remove descendents' permissions
            removed.addAll(
                Arrays.asList(removeResourcePerms(subj, edge.getTo(), true)));
        }

        removed.add(new AppdefEntityID(r));
        if (debug) watch.markTimeBegin("removeResource");
        getOne()._removeResource(subj, r, nullResourceType);
        if (debug) watch.markTimeBegin("removeResource");
        if (debug) {
            log.debug(watch);
        }
        return (AppdefEntityID[]) removed.toArray(new AppdefEntityID[0]);
    }
    
    /**
     * @ejb:interface-method
     */
    public void _removeResource(AuthzSubject subj, Resource r,
                                boolean nullResourceType) {
        final boolean debug = log.isDebugEnabled();
        final ResourceEdgeDAO edgeDao = getResourceEdgeDAO();
        final StopWatch watch = new StopWatch();
        if (debug) watch.markTimeBegin("removeResourcePerms.removeEdges");
        // Delete the edges and resource groups
        edgeDao.deleteEdges(r);
        if (debug) watch.markTimeEnd("removeResourcePerms.removeEdges");
        if (nullResourceType) {
            r.setResourceType(null);
        }
        final long now = System.currentTimeMillis();
        if (debug) watch.markTimeBegin("removeResourcePerms.audit");
        ResourceAudit.deleteResource(r, subj, now, now);
        if (debug) watch.markTimeEnd("removeResourcePerms.audit");
        if (debug) {
            log.debug(watch);
        }
    }

    /**
     * @ejb:interface-method
     */
    public void removeResource(AuthzSubject subject, Resource r) throws VetoException {
        if (r == null) {
            return;
        }
        ResourceDeleteCallback cb = AuthzStartupListener.getResourceDeleteCallback();
        cb.preResourceDelete(r);
        final long now = System.currentTimeMillis();
        ResourceAudit.deleteResource(r, subject, now, now);        
        Collection groupBag = r.getGroupBag();
        if (groupBag != null) {
            groupBag.clear();
        }
        getResourceDAO().remove(r);
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
     * services and have a resource of that type in the inventory.
     *
     * @ejb:interface-method
     */
    public List findAppdefPrototypes() {
        return getResourceDAO().findAppdefPrototypes();
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
    public PageList findViewableSvcResources(AuthzSubject subject, String nameFilter,
                                             PageControl pc) {
        AuthzSubject subj = getSubjectDAO().findById(subject.getId());

        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);

        PermissionManager pm = PermissionManagerFactory.getInstance(); 

        // returns a sorted Collection by resourceName
        Collection resources = pm.findServiceResources(subj, Boolean.FALSE);
        
        if (nameFilter != null) {
            for (Iterator it=resources.iterator(); it.hasNext(); ) {
                Resource r = (Resource) it.next();
                if (r == null || r.isInAsyncDeleteState() ||
                        !r.getName().toLowerCase().contains(nameFilter.toLowerCase())) {
                    it.remove();
                }
            }
        }

        Collection ordResources = resources;
        if (pc.isDescending()) {
            ordResources = new ArrayList(resources);
            Collections.reverse((List)ordResources);
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
    
    /**
     * @param parentList {@link List} of {@link Resource}s
     * @return {@link Collection} of {@link ResourceEdge}s
     * @ejb:interface-method
     */
    public Collection findResourceEdges(ResourceRelation relation, List parentList) {
        return getResourceEdgeDAO().findDescendantEdges(parentList, relation);
    }
    
    /**
     * @return {@link Collection} of {@link ResourceEdge}s
     * @ejb:interface-method
     */
    public Collection findResourceEdges(ResourceRelation relation, Resource parent) {
        return getResourceEdgeDAO().findDescendantEdges(parent, relation);
    }
    
    /**
     * 
     * @ejb:interface-method
     */
    public boolean isResourceChildOf(Resource parent, Resource child) {
        return getResourceEdgeDAO().isResourceChildOf(parent, child);
    }

    /**
     * 
     * @ejb:interface-method
     */
    public boolean hasChildResourceEdges(Resource resource, ResourceRelation relation) {
        return getResourceEdgeDAO().hasChildren(resource, relation);
    }

    /**
     * 
     * @ejb:interface-method
     */
    public int getDescendantResourceEdgeCount(Resource resource, ResourceRelation relation) {
        return getResourceEdgeDAO().getDescendantCount(resource, relation);
    }

    /**
     * 
     * @ejb:interface-method
     */
    public Collection findChildResourceEdges(Resource resource, ResourceRelation relation) {
        return getResourceEdgeDAO().findChildEdges(resource, relation);
    }

    /**
     * 
     * @ejb:interface-method
     */
    public Collection findDescendantResourceEdges(Resource resource, ResourceRelation relation) {
        return getResourceEdgeDAO().findDescendantEdges(resource, relation);
    }

    /**
     * 
     * @ejb:interface-method
     */
    public Collection findAncestorResourceEdges(Resource resource, ResourceRelation relation) {
        return getResourceEdgeDAO().findAncestorEdges(resource, relation);
    }
    
    /**
     * 
     * @ejb:interface-method
     */
    public Collection findResourceEdgesByName(String name, ResourceRelation relation) {
        return getResourceEdgeDAO().findByName(name, relation);
    }
    
    /**
     * 
     * @ejb:interface-method
     */
    public ResourceEdge getParentResourceEdge(Resource resource, ResourceRelation relation) {
        return getResourceEdgeDAO().getParentEdge(resource, relation);
    }
    
    /**
     * 
     * @ejb:interface-method
     */
    public boolean hasResourceRelation(Resource resource, ResourceRelation relation) {
        return getResourceEdgeDAO().hasResourceRelation(resource, relation);
    }

    /**
     * 
     * @ejb:interface-method
     */
    public List findResourceEdges(ResourceRelation relation,
                                  Integer resourceId,
                                  List platformTypeIds,
                                  String platformName) {
        if (relation == null 
                || !relation.getId().equals(AuthzConstants.RELATION_NETWORK_ID)) {
            throw new IllegalArgumentException(
                        "Only " + AuthzConstants.ResourceEdgeNetworkRelation
                        + " resource relationships are supported.");
        }
        
        return getResourceEdgeDAO()
                    .findDescendantEdgesByNetworkRelation(
                                resourceId, platformTypeIds, platformName);
    }

    private void createResourceEdges(Resource parent,
                                     Resource child,
                                     ResourceRelation relation,
                                     boolean createSelfEdge) {
        
        ResourceEdgeDAO eDAO = getResourceEdgeDAO();

        // Self-edge
        if (createSelfEdge) {
            eDAO.create(child, child, 0, relation);
        }

        // Direct edges
        eDAO.create(child, parent, -1, relation);
        eDAO.create(parent, child, 1, relation);

        // Ancestor edges to new destination resource
        Collection ancestors = eDAO.findAncestorEdges(parent, relation);
        for (Iterator i = ancestors.iterator(); i.hasNext();) {
            ResourceEdge ancestorEdge = (ResourceEdge)i.next();

            int distance = ancestorEdge.getDistance() - 1;

            eDAO.create(child, ancestorEdge.getTo(), distance, relation);
            eDAO.create(ancestorEdge.getTo(), child, -distance, relation);
        }
    }
    
    /**
     * 
     * @ejb:interface-method
     */
    public void createResourceEdges(AuthzSubject subject,
                                    ResourceRelation relation,
                                    AppdefEntityID parent,
                                    AppdefEntityID[] children)
        throws PermissionException, ResourceEdgeCreateException {
        
        createResourceEdges(subject, relation, parent, children, false);
    }

    /**
     * 
     * @ejb:interface-method
     */
    public void createResourceEdges(AuthzSubject subject,
                                    ResourceRelation relation,
                                    AppdefEntityID parent,
                                    AppdefEntityID[] children,
                                    boolean deleteExisting)
        throws PermissionException, ResourceEdgeCreateException {
        
        if (relation == null) {
            throw new ResourceEdgeCreateException("Resource relation is null");
        }
        
        if (relation.getId().equals(AuthzConstants.RELATION_NETWORK_ID)) {
            createNetworkResourceEdges(subject,
                                       relation,
                                       parent,
                                       children,
                                       deleteExisting);
        } else if (relation.getId().equals(AuthzConstants.RELATION_VIRTUAL_ID)) {
            createVirtualResourceEdges(subject,
                                       relation,
                                       parent,
                                       children);
        } else {
            throw new ResourceEdgeCreateException(
                        "Unsupported resource relation: "
                        + relation.getName());
        }
    }

    private void createVirtualResourceEdges(AuthzSubject subject,
                                            ResourceRelation relation,
                                            AppdefEntityID parent,
                                            AppdefEntityID[] children)
        throws PermissionException, ResourceEdgeCreateException {
        
        Resource parentResource = findResource(parent);
        
        if (parentResource != null 
                && !parentResource.isInAsyncDeleteState()
                && children != null
                && children.length > 0) {

            try {
                ResourceEdgeDAO eDAO = getResourceEdgeDAO();

                if (!hasResourceRelation(parentResource, relation)) {
                    // create self-edge for parent of virtual hierarchy
                    eDAO.create(parentResource, parentResource, 0, relation);
                }
                for (int i=0; i< children.length; i++) {
                    Resource childResource = findResource(children[i]);
                                        
                    // Check if child resource already exists in VM hierarchy
                    ResourceEdge existing = getParentResourceEdge(childResource, relation);

                    if (existing != null) {
                        Resource existingParent = existing.getTo();
                        if (existingParent.getId().equals(parentResource.getId())) {
                            createVirtualResourceEdgesByMacAddress(subject, childResource);
                            
                            // already exists with same parent, so skip
                            if (log.isDebugEnabled()) {
                                log.debug("Skipping. Virtual resource edge already exists: from id=" 
                                              + parentResource.getId()
                                              + ", to id=" + childResource.getId());
                            }
                            continue;
                        } else {
                            // already exists with different parent, assume vMotion occurred
                            if (log.isDebugEnabled()) {
                                log.debug("Virtual resource edge exists with another resource: fromId="
                                          + existingParent.getId()
                                          + ", toId=" + childResource.getId()
                                          + ". Moving to target fromId=" 
                                          + parentResource.getId());
                            }
                                                        
                            // Clean out edges for the current target
                            Collection edges = findDescendantResourceEdges(childResource, relation);
                            for (Iterator e = edges.iterator(); e.hasNext(); ) {
                                ResourceEdge re = (ResourceEdge) e.next();
                                eDAO.deleteEdges(re.getTo(), relation);
                            }
                            eDAO.deleteEdges(childResource, relation);
                        }
                    }
                
                    if (childResource != null && !childResource.isInAsyncDeleteState()) {                    
                        createResourceEdges(parentResource, childResource, relation,
                                            !hasResourceRelation(childResource, relation));
                        
                        createVirtualResourceEdgesByMacAddress(subject, childResource);
                    }
                }
            } catch (Throwable t) {
                throw new ResourceEdgeCreateException(t);
            }
        }        
    }
    
    /**
     * Create virtual resource edges when a resource is created
     */
    private void createVirtualResourceEdges(AuthzSubject owner,
                                            Resource parent, 
                                            Resource res,
                                            boolean system) {
        
        // do not add virtual servers
        
        if (!system && res.getResourceType().getId().equals(AuthzConstants.authzServer)) {
            // TODO: this is a hack because the mac address is not available
            // yet when the platform is created. associate platform to a vm 
            // if necessary when the server is created
            createVirtualResourceEdgesByMacAddress(owner, parent);            

            // virtual resource edges are needed for servers to improve
            // performance of vCenter resource searches
            ResourceRelation virtual = getVirtualRelation();
            // see if parent platform is associated with a vm
            Collection edges = findAncestorResourceEdges(parent, virtual);
            if (!edges.isEmpty()) {
                createResourceEdges(parent, res, virtual, true);
            }
        }
    }
    
    private boolean createVirtualResourceEdgesByMacAddress(AuthzSubject subject,
                                                           Resource resource) {        
        boolean isEdgesCreated = false;
        
        try {
            Platform associatedPlatform = PlatformManagerEJBImpl.getOne()
                        .getAssociatedPlatformByMacAddress(subject, resource);
            
            if (associatedPlatform != null) {
                String vmPrototype = AuthzConstants.platformPrototypeVmwareVsphereVm;
                if (vmPrototype.equals(resource.getPrototype().getName())) {
                    isEdgesCreated = createVirtualResourceEdgesByMacAddress(
                                          resource, associatedPlatform.getResource());
                } else {
                    isEdgesCreated = createVirtualResourceEdgesByMacAddress(
                                          associatedPlatform.getResource(), resource, false);
                }   
            }
        } catch (Exception e) {
            log.error("Could not create virtual resource edge by MAC address"
                          + " for resource[id=" + resource.getId() 
                          + "]: "
                          + e.getMessage(), e);
        }
        
        return isEdgesCreated;
    }

    private boolean createVirtualResourceEdgesByMacAddress(Resource vmResource,
                                                           Resource hqResource) 
        throws ResourceEdgeCreateException {
        
        return createVirtualResourceEdgesByMacAddress(vmResource, hqResource, true);
    }
    
    private boolean createVirtualResourceEdgesByMacAddress(Resource vmResource,
                                                           Resource hqResource,
                                                           boolean createServerEdges) 
        throws ResourceEdgeCreateException {
        
        boolean isEdgesCreated = false;
        
        try {
            String vmPrototype = AuthzConstants.platformPrototypeVmwareVsphereVm;
            
            if (!vmPrototype.equals(vmResource.getPrototype().getName())) {
                throw new ResourceEdgeCreateException("Resource[id=" 
                         + vmResource.getId() + "] is not a " + vmPrototype);
            } else if (vmPrototype.equals(hqResource.getPrototype().getName())) {
                throw new ResourceEdgeCreateException("Resource[id="
                         + hqResource.getId() + "] cannot be a " + vmPrototype);
            }
        
            ResourceRelation relation = getVirtualRelation();
        
            if (getParentResourceEdge(hqResource, relation) == null) {
                createResourceEdges(vmResource, hqResource, relation, true);
            
                if (createServerEdges) {
                    // create virtual resource edges for the servers for the platform.
                    // data is redundant with the containtment resource edges,
                    // but is needed to improve search speed
                    try {
                        Platform hqPlatform = PlatformManagerEJBImpl.getOne()
                                                .findPlatformById(hqResource.getInstanceId());
                    
                        for (Iterator i=hqPlatform.getServers().iterator(); i.hasNext(); ) {
                            Server s = (Server)i.next();
                            ServerType st = s.getServerType();
                            // do not add virtual servers or vCenter server
                            if (!AuthzConstants.serverPrototypeVmwareVcenter.equals(st.getName())
                                    && !st.isVirtual()) {
                                createResourceEdges(hqResource, s.getResource(), relation, true);
                            }
                        }                        
                    } catch (Exception e) {
                        throw new ResourceEdgeCreateException(e.getMessage(), e);
                    }
                }
                
                isEdgesCreated = true;
            }
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("createVirtualResourceEdgesByMacAddress: vmResourceId=" 
                              + vmResource.getId()
                              + ", hqResourceId=" + hqResource.getId()
                              + ", isEdgesCreated=" + isEdgesCreated);
            }
        }
        
        return isEdgesCreated;
    }
    
    private void createNetworkResourceEdges(AuthzSubject subject,
                                            ResourceRelation relation,
                                            AppdefEntityID parent,
                                            AppdefEntityID[] children,
                                            boolean deleteExisting)
        throws PermissionException, ResourceEdgeCreateException {
                
        if (parent == null || !parent.isPlatform()) {
            throw new ResourceEdgeCreateException("Only platforms are supported.");
        }
        
        PlatformManagerLocal platMan = PlatformManagerEJBImpl.getOne();
        Platform parentPlatform = null;
        
        try {
            parentPlatform = platMan.findPlatformById(parent.getId());
        } catch (PlatformNotFoundException pe) {
            throw new ResourceEdgeCreateException("Platform id " + parent.getId() + " not found.");
        }
        List supportedPlatformTypes = new ArrayList(platMan.findSupportedPlatformTypes());

        if (supportedPlatformTypes.contains(parentPlatform.getPlatformType())) {
            throw new ResourceEdgeCreateException(parentPlatform.getPlatformType().getName()
                                + " not supported as a top-level platform type.");
        }
        
        Resource parentResource = parentPlatform.getResource();
        
        // Make sure user has permission to modify resource edges
        final PermissionManager pm = PermissionManagerFactory.getInstance();

        pm.check(subject.getId(), 
                 parentResource.getResourceType(), 
                 parentResource.getInstanceId(), 
                 AuthzConstants.platformOpModifyPlatform);

        // HQ-1670: Should not be able to add a parent resource to
        // a network hierarchy if it has not been configured yet 
        ConfigResponseDB config = ConfigManagerEJBImpl.getOne().getConfigResponse(parent);
        if (config != null) {
            String validationError = config.getValidationError();
            if (validationError != null) {
                throw new ResourceEdgeCreateException("Resource id " + parentResource.getId() 
                                                        + ": " + validationError);
            }
        }
        
        if (parentResource != null 
                && !parentResource.isInAsyncDeleteState()
                && children != null
                && children.length > 0) {

            try {
                if (deleteExisting) {
                    removeResourceEdges(subject, relation, parentResource);
                }
            
                ResourceEdgeDAO eDAO = getResourceEdgeDAO();
                Collection edges = findResourceEdges(relation, parentResource);
                List existing = null;
                Platform childPlatform = null;
                Resource childResource = null;

                if (edges.isEmpty()) {
                    // create self-edge for parent of network hierarchy
                    eDAO.create(parentResource, parentResource, 0, relation);
                }
                for (int i=0; i< children.length; i++) {
                    if (!children[i].isPlatform()) {
                        throw new ResourceEdgeCreateException("Only platforms are supported.");
                    }
                    try {
                        childPlatform = platMan.findPlatformById(children[i].getId());
                        childResource = childPlatform.getResource();
                    
                        if (!supportedPlatformTypes.contains(childPlatform.getPlatformType())) {
                            throw new ResourceEdgeCreateException(childPlatform.getPlatformType().getName()
                                        + " not supported as a dependent platform type.");
                        }
                    } catch (PlatformNotFoundException pe) {
                        throw new ResourceEdgeCreateException ("Platform id " + children[i].getId() + " not found.");
                    }
                
                    // Check if child resource already exists in a network hierarchy
                    // TODO: This needs to be optimized
                    existing = findResourceEdges(relation, childResource.getId(), null, null);

                    if (existing.size() == 1) {
                        ResourceEdge existingChildEdge = (ResourceEdge) existing.get(0);
                        Resource existingParent = existingChildEdge.getFrom();
                        if (existingParent.getId().equals(parentResource.getId())) {
                            // already exists with same parent, so skip
                            continue;
                        } else {
                            // already exists with different parent
                            throw new ResourceEdgeCreateException("Resource id " + childResource.getId()
                                        + " already exists in another network hierarchy.");
                        }
                    } else if (existing.size() > 1) {
                        // a resource can only belong to one network hierarchy
                        // this is a data integrity issue if it happens
                        throw new ResourceEdgeCreateException("Resource id " + childResource.getId()
                                        + " exists in " + existing.size() + " network hierarchies.");
                    }
                
                    if (childResource != null && !childResource.isInAsyncDeleteState()) {                    
                        eDAO.create(parentResource, childResource, 1, relation);
                        eDAO.create(childResource, parentResource, -1, relation);
                    }
                }
            } catch (Throwable t) {
                throw new ResourceEdgeCreateException(t);
            }
        }
    }
    
    /**
     * 
     * @ejb:interface-method
     */
    public void removeResourceEdges(AuthzSubject subject,
                                    ResourceRelation relation,
                                    AppdefEntityID parent,
                                    AppdefEntityID[] children)
        throws PermissionException {
        
        if (relation == null 
                || !relation.getId().equals(AuthzConstants.RELATION_NETWORK_ID)) {
            throw new IllegalArgumentException(
                        "Only " + AuthzConstants.ResourceEdgeNetworkRelation
                        + " resource relationships are supported.");
        }
        
        Resource parentResource = findResource(parent);
        Resource childResource = null;

        // Make sure user has permission to modify resource edges
        final PermissionManager pm = PermissionManagerFactory.getInstance();

        pm.check(subject.getId(), 
                 parentResource.getResourceType(), 
                 parentResource.getInstanceId(), 
                 AuthzConstants.platformOpModifyPlatform);
        
        if (parentResource != null && !parentResource.isInAsyncDeleteState()) {
            ResourceEdgeDAO eDAO = getResourceEdgeDAO();

            for (int i=0; i< children.length; i++) {
                childResource = findResource(children[i]);
                
                if (childResource != null && !childResource.isInAsyncDeleteState()) {
                    eDAO.deleteEdge(parentResource, childResource, relation);
                    eDAO.deleteEdge(childResource, parentResource, relation);
                }
            }
            Collection edges = findResourceEdges(relation, parentResource);
            if (edges.isEmpty()) {
                // remove self-edge for parent of network hierarchy
                eDAO.deleteEdges(parentResource, relation);
            }
        }
    }

    /**
     * 
     * @ejb:interface-method
     */
    public void removeResourceEdges(AuthzSubject subject, 
                                    ResourceRelation relation,
                                    Resource parent) 
        throws PermissionException {
        
        if (relation == null 
                || !relation.getId().equals(AuthzConstants.RELATION_NETWORK_ID)) {
            throw new IllegalArgumentException(
                        "Only " + AuthzConstants.ResourceEdgeNetworkRelation
                        + " resource relationships are supported.");
        }
        
        // Make sure user has permission to modify resource edges
        final PermissionManager pm = PermissionManagerFactory.getInstance();

        pm.check(subject.getId(), 
                 parent.getResourceType(), 
                 parent.getInstanceId(), 
                 AuthzConstants.platformOpModifyPlatform);
        
        getResourceEdgeDAO().deleteEdges(parent, relation);
    }
    
    /**
     * @param {@link Collection} of {@link Resource}s
     * @ejb:interface-method
     */
    public void resourceHierarchyUpdated(AuthzSubject subj, Collection resources) {
        if (resources.size() <= 0) {
            return;
        }
        final List events = new ArrayList();
        final ResourceEdgeDAO dao = getResourceEdgeDAO();
        final ResourceRelation relation = getContainmentRelation();
        for (final Iterator it=resources.iterator(); it.hasNext(); ) {
            final Resource resource = (Resource)it.next();
            events.add(new ResourceUpdatedZevent(subj, new AppdefEntityID(resource)));
            final Collection descendants = dao.findDescendantEdges(resource, relation);
            for (final Iterator xx=descendants.iterator(); xx.hasNext(); ) {
                final Resource r = ((ResourceEdge)xx.next()).getTo();
                events.add(new ResourceUpdatedZevent(subj, new AppdefEntityID(r)));
            }
        }
        ZeventManager.getInstance().enqueueEventsAfterCommit(events);
    }

    /**
     * @return the resource count with prototype of {@link AuthzConstants.authzPlatform}
     * minus resources with the prototype of {@link AuthConstants.platformPrototypeVmwareVsphereVm}
     * @ejb:interface-method
     */
    public int getPlatformCountMinusVsphereVmPlatforms() {
        return getResourceDAO().getPlatformCountMinusVsphereVmPlatforms();
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
            resourceTypePager = Pager.getDefaultPager();
        } catch (Exception e) {
            throw new CreateException("Could not create value pager: " + e);
        }
    }

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
