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
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.auth.data.AuthzSubjectRepository;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ResourceAuditFactory;
import org.hyperic.hq.inventory.data.ResourceDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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
    private ZeventEnqueuer zeventManager;
   
    private PermissionManager permissionManager;
    private ResourceAuditFactory resourceAuditFactory;
    private ApplicationContext applicationContext;
    private ResourceDao resourceDao;
    private ResourceTypeDao resourceTypeDao;
    private AuthzSubjectRepository authzSubjectRepository;

    @Autowired
    public ResourceManagerImpl(AuthzSubjectManager authzSubjectManager,
                               
                               ZeventEnqueuer zeventManager, PermissionManager permissionManager,
                               ResourceAuditFactory resourceAuditFactory, ResourceDao resourceDao,
                               ResourceTypeDao resourceTypeDao, AuthzSubjectRepository authzSubjectRepository) {
        this.authzSubjectManager = authzSubjectManager;
        this.zeventManager = zeventManager;
        this.permissionManager = permissionManager;
        resourceTypePager = Pager.getDefaultPager();
        this.resourceAuditFactory = resourceAuditFactory;
        this.resourceDao = resourceDao;
        this.resourceTypeDao = resourceTypeDao;
        this.authzSubjectRepository = authzSubjectRepository;
    }
    
   
    @Transactional(readOnly = true)
    public boolean resourcesExistOfType(String typeName) {
        return resourceTypeDao.findByName(typeName).hasResources();
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
        AppdefEntityID resourceId = AppdefUtil.newAppdefEntityId(r);
        r.remove();
        // Send resource delete event
        ResourceDeletedZevent zevent = new ResourceDeletedZevent(subject, resourceId);
        zeventManager.enqueueEventAfterCommit(zevent);

    }

    /**
     * 
     */
    public void setResourceOwner(AuthzSubject whoami, Resource resource, AuthzSubject newOwner)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        if (pm.hasAdminPermission(whoami.getId()) || whoami.getOwnedResources().contains(resource)) {
            newOwner.addOwnedResource(resource);
            whoami.removeOwnedResource(resource);
            authzSubjectRepository.save(whoami);
            resource.setModifiedBy(whoami.getName());
            resourceDao.merge(resource);
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
     * Gets all the Resources owned by the given Subject.
     * @param subject The owner.
     * @return Array of resources owned by the given subject.
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Resource> findResourceByOwner(AuthzSubject owner) {
        return owner.getOwnedResources();
    }

    @Transactional(readOnly = true)
    public boolean isResourceChildOf(Resource parent, Resource child) {
        return parent.hasChild(child,true);
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

       
        for (final Resource resource : resources) {
            events.add(new ResourceUpdatedZevent(subj, AppdefUtil.newAppdefEntityId(resource)));
            final Set<Resource> children = resource.getChildren(true);
            for (Resource child: children) {
                events.add(new ResourceUpdatedZevent(subj, AppdefUtil.newAppdefEntityId(child)));
            }
        }
        zeventManager.enqueueEventsAfterCommit(events);
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
            ResourceType system= new ResourceType("System");
            resourceTypeDao.persist(system);
            Resource root = new Resource("Root", system);
            resourceDao.persist(root);
        }     
    }
    
    
    public Set<ResourceType> findResourceTypesWithResources() {
        final Set<ResourceType> typesWithResources = new HashSet<ResourceType>();
        Collection<ResourceType> resTypes = resourceTypeDao.findAll();
        for(ResourceType resType: resTypes) {
            if(!(resType.getResources().isEmpty())) {
                typesWithResources.add(resType);
            }
        }
        return typesWithResources;
    }
    
    public PageList<Resource> getResourcesOfType(ResourceType resourceType, PageControl pc) {
        PageRequest pageInfo = new PageRequest(pc.getPagenum(),pc.getPagesize(),
            new Sort(pc.getSortorder() == PageControl.SORT_ASC ? Direction.ASC: Direction.DESC,"name"));
        Page<Resource> resources = resourceDao.findByIndexedProperty("type", resourceType.getId(),pageInfo,String.class);
        return new PageList<Resource>(resources.getContent(),(int)resources.getTotalElements());
    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
