/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc. This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify it under the terms
 * version 2 of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.authz.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ResourceAuditFactory;
import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Relationship;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.reference.RelationshipTypes;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use this session bean to manipulate Resources, ResourceTypes and
 * ResourceGroups. That is to say, Resources and their derivatives.
 * Alternatively you can say, anything entity that starts with the word
 * Resource.
 * 
 * All arguments and return values are value-objects.
 * 
 */
@Transactional
@Service
public class ResourceManagerImpl implements ResourceManager, ApplicationContextAware {

    private final Log log = LogFactory.getLog(ResourceManagerImpl.class);
    private Pager resourceTypePager = null;
   
    private AuthzSubjectManager authzSubjectManager;
    private AuthzSubjectDAO authzSubjectDAO;
    private ZeventEnqueuer zeventManager;
   
    private PermissionManager permissionManager;
    private ResourceAuditFactory resourceAuditFactory;
    private ApplicationContext applicationContext;
    private ResourceDao resourceDao;
    private ResourceTypeDao resourceTypeDao;

    @Autowired
    public ResourceManagerImpl(AuthzSubjectManager authzSubjectManager,
                               AuthzSubjectDAO authzSubjectDAO,
                               ZeventEnqueuer zeventManager, PermissionManager permissionManager,
                               ResourceAuditFactory resourceAuditFactory, ResourceDao resourceDao,
                               ResourceTypeDao resourceTypeDao) {
        this.authzSubjectManager = authzSubjectManager;
        this.authzSubjectDAO = authzSubjectDAO;
        this.zeventManager = zeventManager;
        this.permissionManager = permissionManager;
        resourceTypePager = Pager.getDefaultPager();
        this.resourceAuditFactory = resourceAuditFactory;
        this.resourceDao = resourceDao;
        this.resourceTypeDao = resourceTypeDao;
    }

    /**
     * Find the type that has the given name.
     * @param name The name of the type you're looking for.
     * @return The value-object of the type of the given name.
     * @throws NotFoundException Unable to find a given or dependent entities.
     * 
     */
    @Transactional(readOnly = true)
    public ResourceType findResourceTypeByName(String name)  {     
        ResourceType rt = resourceTypeDao.findByName(name);
        return rt;
    }

    /**
     * Check if there are any resources of a given type
     * 
     * 
     */
    @Transactional(readOnly = true)
    public boolean resourcesExistOfType(String typeName) {
        return resourceTypeDao.findByName(typeName).hasResources();
    }

    /**
     * Create a resource.
     * 
     * 
     */
    public Resource createResource(AuthzSubject owner, ResourceType rt, String name, boolean system, Resource parent) {
        long start = System.currentTimeMillis();

        Resource res = new Resource();
        res.setName(name);
        res.setOwner(owner);
        res.persist();

        Relationship<ResourceType> relation = getContainmentRelation();

        //TODO what did we need the self-edge for?
        //resourceEdgeDAO.create(res, res, 0, relation); // Self-edge
        if (parent != null) {
            parent.relateTo(res, RelationshipTypes.CONTAINS);
            createResourceEdges(parent, res, relation, false);
        }

        resourceAuditFactory.createResource(res, owner, start, System.currentTimeMillis());
        return res;
    }

    /**
     * Move a resource. It is the responsibility of the caller (AppdefManager)
     * to ensure that this resource can be moved to the destination.
     * 
     * It's also of note that this method only deals with relinking resource
     * edges to the ancestors of the destination resource. This means that in
     * the case of Server moves, it's up to the caller to re-link dependent
     * children.
     * 
     * 
     */
    public void moveResource(AuthzSubject owner, Resource target, Resource destination) {
        long start = System.currentTimeMillis();

        Relationship<ResourceType> relation = getContainmentRelation();

        target.removeRelationships();

        createResourceEdges(destination, target, relation, true);

        resourceAuditFactory.moveResource(target, destination, owner, start, System
            .currentTimeMillis());
    }

    /**
     * Get the # of resources within HQ inventory
     * 
     */
    @Transactional(readOnly = true)
    public Number getResourceCount() {
        return resourceDao.count();
    }

    /**
     * Get the # of resource types within HQ inventory
     * 
     */
    @Transactional(readOnly = true)
    public Number getResourceTypeCount() {
        return resourceTypeDao.count();
    }

    /**
     * Find's the root (id=0) resource
     * 
     */
    @Transactional(readOnly = true)
    public Resource findRootResource() {
        return resourceDao.findRoot();
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Resource findResourceById(Integer id) {
        return resourceDao.findById(id);
    }

    /**
     * 
     */
    public void removeResource(AuthzSubject subject, Resource r) throws VetoException {
        if (r == null) {
            return;
        }
        applicationContext.publishEvent(new ResourceDeleteRequestedEvent(r));

        final long now = System.currentTimeMillis();
        //TODO audit
        //resourceAuditFactory.deleteResource(findResourceById(AuthzConstants.authzHQSystem),
          //  subject, now, now);
        Collection<ResourceGroup> groups = r.getResourceGroups();
        for(ResourceGroup group:groups) {
            group.removeMember(r);
        }
        r.remove();
    }

    /**
     * 
     */
    public void setResourceOwner(AuthzSubject whoami, Resource resource, AuthzSubject newOwner)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        if (pm.hasAdminPermission(whoami.getId()) || resource.isOwner(whoami.getId())) {
            resource.setOwner(newOwner);
            resource.setModifiedBy(whoami.getName());
            resource.merge();
        } else {
            throw new PermissionException("Only an owner or admin may " + "reassign ownership.");
        }
    }

    /**
     * Get all the resource types
     * @param subject
     * @param pc Paging information for the request
     * 
     */
    @Transactional(readOnly = true)
    public List<ResourceType> getAllResourceTypes(AuthzSubject subject, PageControl pc) {
        Collection<ResourceType> resTypes = resourceTypeDao.findAll();
        pc = PageControl.initDefaults(pc, SortAttribute.RESTYPE_NAME);
        return resourceTypePager.seek(resTypes, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get viewable resources either by "type" OR "resource name" OR
     * "type AND resource name".
     * 
     * @param subject
     * @return Map of resource values
     * 
     */
    @Transactional(readOnly = true)
    public List<Integer> findViewableInstances(AuthzSubject subject, String typeName,
                                               String resName, String appdefTypeStr,
                                               Integer typeId, PageControl pc) {
        // Authz type and/or resource name must be specified.
        if (typeName == null) {
            throw new IllegalArgumentException(
                "This method requires a valid authz type name argument");
        }

        PermissionManager pm = PermissionManagerFactory.getInstance();
        return pm.findViewableResources(subject, typeName, resName, appdefTypeStr, typeId, pc);
    }

    /**
     * Get viewable resources by "type" OR "resource name"
     * 
     * @param subject
     * @return Map of resource values
     * 
     */
    @Transactional(readOnly = true)
    public PageList<Resource> findViewables(AuthzSubject subject, String searchFor, PageControl pc) {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List<Integer> resIds = pm.findViewableResources(subject, searchFor, pc);
        Pager pager = Pager.getDefaultPager();
        List<Integer> paged = pager.seek(resIds, pc);

        PageList<Resource> resources = new PageList<Resource>();
        for (Integer id : paged) {
            resources.add(resourceDao.findById(id));
        }

        resources.setTotalSize(resIds.size());
        return resources;
    }

    /**
     * Get viewable resources either by "type" OR "resource name" OR
     * "type AND resource name".
     * 
     * @param subject
     * @return Map of resource values
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, List<Integer>> findAllViewableInstances(AuthzSubject subject) {
        // First get all resource types
        Map<String, List<Integer>> resourceMap = new HashMap<String, List<Integer>>();

        Collection<ResourceType> resTypes = resourceTypeDao.findAll();
        for (ResourceType type : resTypes) {
            String typeName = type.getName();

            // Now fetch list by the type
            List<Integer> ids = findViewableInstances(subject, typeName, null, null, null,
                PageControl.PAGE_ALL);
            if (ids.size() > 0) {
                resourceMap.put(typeName, ids);
            }
        }
        return resourceMap;
    }

    /**
     * Find all the resources which are descendants of the given resource
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> findResourcesByParent(AuthzSubject subject, Resource res) {
        return new ArrayList<Resource>(res.getResourcesFrom(getContainmentRelation().getName()));
    }

    /**
     * Find all the resources of an authz resource type
     * 
     * @param resourceType 301 for platforms, etc.
     * @param pInfo A pager, using a sort field of {@link ResourceSortField}
     * @return a list of {@link Resource}s
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> findResourcesOfType(int resourceType, PageInfo pInfo) {
        //TODO paging and sorting
        return new ArrayList<Resource>(resourceTypeDao.findById(resourceType).getResources());
    }

    /**
     * Gets all the Resources owned by the given Subject.
     * @param subject The owner.
     * @return Array of resources owned by the given subject.
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Resource> findResourceByOwner(AuthzSubject owner) {
        return resourceDao.findByOwner(owner);
    }

    /**
     * 
     * @param parentList {@link List} of {@link Resource}s
     * @return {@link Collection} of {@link ResourceEdge}s
     */
    @Transactional(readOnly = true)
    public Collection<Relationship<Resource>> findResourceEdges(Relationship<ResourceType> relation,
                                                      List<Resource> parentList) {
      //TODO this was findDescEdges before, so I guess it returned relationships all the way down.  Find out why
        Set<Relationship<Resource>> edges = new HashSet<Relationship<Resource>>();
        for(Resource parent: parentList) {
            edges.addAll(parent.getRelationshipsFrom(relation.getName()));
        }
        return edges;
    }

    /**
     * 
     * @return {@link Collection} of {@link ResourceEdge}s
     */
    @Transactional(readOnly = true)
    public Collection<Relationship<Resource>> findResourceEdges(Relationship<ResourceType> relation, Resource parent) {
      //TODO this was findDescEdges before, so I guess it returned relationships all the way down.  Find out why
        return parent.getRelationshipsFrom(relation.getName());
    }

    @Transactional(readOnly = true)
    public boolean isResourceChildOf(Resource parent, Resource child) {
        //TODO isResourceChildOf before sometimes when 2 levels deep in case of plaform/server
        return parent.isRelatedTo(child, getContainmentRelation().getName());
    }

    @Transactional(readOnly = true)
    public boolean hasChildResourceEdges(Resource resource, Relationship<ResourceType> relation) {
        return resource.getResourcesFrom(getContainmentRelation().getName()).isEmpty();
    }

    @Transactional(readOnly = true)
    public int getDescendantResourceEdgeCount(Resource resource, Relationship<ResourceType> relation) {
        //TODO this traversed all distances before
        return resource.getResourcesFrom(getContainmentRelation().getName()).size();
    }

    @Transactional(readOnly = true)
    public Collection<Relationship<Resource>> findChildResourceEdges(Resource resource,
    		Relationship<ResourceType> relation) {
        return resource.getRelationshipsFrom(relation.getName());
    }

    @Transactional(readOnly = true)
    public Collection<Relationship<Resource>> findDescendantResourceEdges(Resource resource,
    		Relationship<ResourceType> relation) {
      //TODO this was findDescEdges before, so I guess it returned relationships all the way down.  Find out why
        return resource.getRelationshipsFrom(relation.getName());
    }

    @Transactional(readOnly = true)
    public Collection<Relationship<Resource>> findAncestorResourceEdges(Resource resource,
    		Relationship<ResourceType> relation) {
      //TODO this was findAncestoryEdges before, so I guess it returned relationships all the way up.  Find out why
        return resource.getRelationshipsTo(relation.getName());
    }

    @Transactional(readOnly = true)
    public Collection<Relationship<Resource>> findResourceEdgesByName(String name, Relationship<ResourceType> relation) {
        //TODO guard against NPE and this is a terrible method name
        return resourceDao.findByName(name).getRelationshipsFrom(relation.getName());
        
    }

    @Transactional(readOnly = true)
    public Relationship<Resource> getParentResourceEdge(Resource resource, Relationship<ResourceType> relation) {
        //TODO not really enforcing only one relationship of a certain type..
        return resource.getRelationshipsTo(relation.getName()).iterator().next();
    }

    @Transactional(readOnly = true)
    public boolean hasResourceTypeRelation(Resource resource, Relationship<ResourceType> relation) {
        return resource.getRelationshipsFrom(relation.getName()).isEmpty();
    }


    private void createResourceEdges(Resource parent, Resource child, Relationship<ResourceType> relation,
                                     boolean createSelfEdge) {

        // Self-edge
        if (createSelfEdge) {
            //TODO why?
            //resourceEdgeDAO.create(child, child, 0, relation);
        }

        // Direct edges
        parent.relateTo(child, relation.getName());
      
//        // Ancestor edges to new destination resource
//        Collection<ResourceEdge> ancestors = resourceEdgeDAO.findAncestorEdges(parent, relation);
//        for (ResourceEdge ancestorEdge : ancestors) {
//            int distance = ancestorEdge.getDistance() - 1;
//
//            resourceEdgeDAO.create(child, ancestorEdge.getTo(), distance, relation);
//            resourceEdgeDAO.create(ancestorEdge.getTo(), child, -distance, relation);
//        }
    }

    public void removeResourceEdges(AuthzSubject subject, Relationship<ResourceType> relation, Resource parent)
        throws PermissionException {

     

        // TODO perm check
        //final PermissionManager pm = PermissionManagerFactory.getInstance();

       
        //pm.check(subject.getId(), parent.getType(), parent.getInstanceId(),
          //  AuthzConstants.platformOpModifyPlatform);

        parent.removeRelationships(relation.getName());
    }

    /**
     * @param {@link Collection} of {@link Resource}s
     * 
     */
    @Transactional(readOnly = true)
    public void resourceHierarchyUpdated(AuthzSubject subj, Collection<Resource> resources) {
        if (resources.size() <= 0) {
            return;
        }
        final List<ResourceUpdatedZevent> events = new ArrayList<ResourceUpdatedZevent>();

        final Relationship<ResourceType> relation = getContainmentRelation();
        for (final Resource resource : resources) {
            events.add(new ResourceUpdatedZevent(subj, AppdefUtil.newAppdefEntityId(resource)));
            final Collection<Relationship<Resource>> descendants = resource.getRelationshipsFrom(relation.getName());
          //TODO this was findDescEdges before, so I guess it returned relationships all the way down.  Find out why
            for (Relationship<Resource> edge : descendants) {
                final Resource r = edge.getTo();
                events.add(new ResourceUpdatedZevent(subj, AppdefUtil.newAppdefEntityId(r)));
            }
        }
        zeventManager.enqueueEventsAfterCommit(events);
    }

    @Transactional(readOnly = true)
    public Relationship<ResourceType> getContainmentRelation() {
        //TODO how to create relationships applicable for any resource being modeled?
        return new Relationship<ResourceType>();
    }
    
    //TODO remove this method - ResourceManager (if kept) should have not knowledge of AppdefEntityIDs
    public Resource findResource(AppdefEntityID entityID) {
        return resourceDao.findById(entityID.getId());
    }
    
    @Transactional(readOnly=true)
    public ResourceType findResourceTypeById(Integer id) {
        return resourceTypeDao.findById(id);
    }

    @Transactional(readOnly=true)
    public ResourceType findRootResourceType() {
       return resourceTypeDao.findRoot();
    }

    @Transactional(readOnly=true)
    public Resource findResourceByName(String name) {
       return resourceDao.findByName(name);
    }
    
    @PostConstruct
    public void initializeResourceTypes() {
        //TODO Is this the place for this?
        if(resourceTypeDao.findRoot() == null) {
            ResourceType system=new ResourceType();
            system.setName("System");
            system.persist();
            Resource root = new Resource();
            root.setName("Root");
            root.persist();
            root.setType(system);
        }
        int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();
        for(int i=0;i< groupTypes.length;i++) {
            if(resourceTypeDao.findByName(AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[i])) == null) {
                ResourceType groupType = new ResourceType();
                groupType.setName(AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[i]));
                groupType.persist();
                if(groupTypes[i] == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP) {
                    PropertyType propType = new PropertyType();
                    propType.setName("applicationType");
                    propType.setDescription("applicationType");
                    propType.setHidden(true);
                    propType.persist();
                    groupType.addPropertyType(propType);
                }else {
                    PropertyType propType = new PropertyType();
                    propType.setName("groupEntType");
                    propType.setDescription("groupEntType");
                    propType.setHidden(true);
                    propType.persist();
                    groupType.addPropertyType(propType);
                    PropertyType propEntResType = new PropertyType();
                    propEntResType.setName("groupEntResType");
                    propEntResType.setDescription("groupEntResType");
                    propEntResType.setHidden(true);
                    propEntResType.persist();
                    groupType.addPropertyType(propEntResType);
                }
            }
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
