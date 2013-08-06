/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.ApplicationDAO;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformDAO;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.PlatformTypeDAO;
import org.hyperic.hq.appdef.server.session.RemovedResourceEvent;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerDAO;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServerTypeDAO;
import org.hyperic.hq.appdef.server.session.ServiceDAO;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.server.session.ServiceTypeDAO;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ResourceTypeCleanupZevent;
import org.hyperic.hq.appdef.shared.ResourcesCleanupZevent;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceEdgeCreateException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.server.session.AppdefBossImpl;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ResourceAuditFactory;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MeasurementTemplateDAO;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.server.session.MonitorableTypeDAO;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.Classifier;
import org.hyperic.util.IntegerTransformer;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
public class ResourceManagerImpl implements ResourceManager {

    private final Log log = LogFactory.getLog(ResourceManagerImpl.class);
    private Pager resourceTypePager = null;
    private ResourceEdgeDAO resourceEdgeDAO;
    private PlatformDAO platformDAO;
    private AuthzSubjectManager authzSubjectManager;
    private AuthzSubjectDAO authzSubjectDAO;
    private ResourceDAO resourceDAO;
    private ResourceTypeDAO resourceTypeDAO;
    private ResourceRelationDAO resourceRelationDAO;
    private ZeventEnqueuer zeventManager;
    private ServerDAO serverDAO;
    private ServiceDAO serviceDAO;
    private PlatformTypeDAO platformTypeDAO;
    private ApplicationDAO applicationDAO;
    private PermissionManager permissionManager;
    private ResourceAuditFactory resourceAuditFactory;
    private MonitorableTypeDAO monitorableTypeDAO;
    private ServerTypeDAO serverTypeDAO;
    private ServiceTypeDAO serviceTypeDAO;
    private MeasurementTemplateDAO measurementTemplateDAO;
    private ResourceRemover resourceRemover;
    @Autowired
    private ResourceGroupManager resourceGroupManager;
    private ConfigManager configManager;

    @Autowired
    public ResourceManagerImpl(ResourceEdgeDAO resourceEdgeDAO, PlatformDAO platformDAO,
                               ServerDAO serverDAO, ServiceDAO serviceDAO,
                               AuthzSubjectManager authzSubjectManager,
                               AuthzSubjectDAO authzSubjectDAO, ResourceDAO resourceDAO,
                               ResourceTypeDAO resourceTypeDAO,
                               ResourceRelationDAO resourceRelationDAO,
                               ZeventEnqueuer zeventManager, PlatformTypeDAO platformTypeDAO,
                               ServerTypeDAO serverTypeDAO, ServiceTypeDAO serviceTypeDAO,
                               MeasurementTemplateDAO measurementTemplateDAO,
                               ApplicationDAO applicationDAO, PermissionManager permissionManager,
                               ResourceAuditFactory resourceAuditFactory, ResourceRemover resourceRemover,
                               MonitorableTypeDAO monitorableTypeDAO) {
        this.resourceEdgeDAO = resourceEdgeDAO;
        this.platformDAO = platformDAO;
        this.serverDAO = serverDAO;
        this.serviceDAO = serviceDAO;
        this.authzSubjectManager = authzSubjectManager;
        this.authzSubjectDAO = authzSubjectDAO;
        this.resourceDAO = resourceDAO;
        this.resourceTypeDAO = resourceTypeDAO;
        this.resourceRelationDAO = resourceRelationDAO;
        this.zeventManager = zeventManager;
        this.platformTypeDAO = platformTypeDAO;
        this.applicationDAO = applicationDAO;
        this.permissionManager = permissionManager;
        resourceTypePager = Pager.getDefaultPager();
        this.resourceAuditFactory = resourceAuditFactory;
        this.monitorableTypeDAO = monitorableTypeDAO;
        this.serverTypeDAO = serverTypeDAO;
        this.serviceTypeDAO = serviceTypeDAO;
        this.measurementTemplateDAO = measurementTemplateDAO;
        this.resourceRemover = resourceRemover;
    }
    
    @PostConstruct
    public void init() {
        this.configManager = (ConfigManager) Bootstrap.getBean("ConfigManager");
    }    

    /**
     * Find the type that has the given name.
     * @param name The name of the type you're looking for.
     * @return The value-object of the type of the given name.
     * @throws NotFoundException Unable to find a given or dependent entities.
     * 
     */
    @Transactional(readOnly = true)
    public ResourceType findResourceTypeByName(String name) throws NotFoundException {
        ResourceType rt = resourceTypeDAO.findByName(name);

        if (rt == null) {
            throw new NotFoundException("ResourceType " + name + " not found");
        }

        return rt;
    }

    /**
     * remove the authz resource entry
     */
    public void removeAuthzResource(AuthzSubject subject, AppdefEntityID aeid, Resource r)
        throws PermissionException, VetoException {
        if (log.isDebugEnabled())
            log.debug("Removing authz resource: " + aeid);

        Zevent zevent = new ResourceDeletedZevent(subject, aeid);
        RemovedResourceEvent event = new RemovedResourceEvent(r.getId());

        AuthzSubject s = authzSubjectManager.findSubjectById(subject.getId());
        
        removeResource(s, r);

        // Send resource delete event
        zeventManager.enqueueEventAfterCommit(zevent);
        zeventManager.enqueueEventAfterCommit(event);
    }

    /**
     * Find a resource, acting as a resource prototype.
     * 
     */
    @Transactional(readOnly = true)
    public Resource findResourcePrototypeByName(String name) {
        return resourceDAO.findResourcePrototypeByName(name);
    }

    /**
     * Check if there are any resources of a given type
     * 
     * 
     */
    @Transactional(readOnly = true)
    public boolean resourcesExistOfType(String typeName) {
        return resourceDAO.resourcesExistOfType(typeName);
    }

    /**
     * Create a resource.
     * 
     * 
     */
    public Resource createResource(AuthzSubject owner, ResourceType rt, Resource prototype,
                                   Integer instanceId, String name, boolean system, Resource parent) {
        long start = System.currentTimeMillis();

        Resource res = resourceDAO.create(rt, prototype, name, owner, instanceId, system);

        ResourceRelation relation = getContainmentRelation();

        resourceEdgeDAO.create(res, res, 0, relation); // Self-edge
        if (parent != null) {
            createResourceEdges(parent, res, relation, false);

            // TODO: Explore calling this when ResourceCreatedZevent
            // is processed instead
            createVirtualResourceEdges(owner, parent, res, system);
            
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

        ResourceRelation relation = getContainmentRelation();

        // Clean out edges for the current target
        resourceEdgeDAO.deleteEdges(target);

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
        return new Integer(resourceDAO.size());
    }

    /**
     * Get the # of resource types within HQ inventory
     * 
     */
    @Transactional(readOnly = true)
    public Number getResourceTypeCount() {
        return new Integer(resourceTypeDAO.size());
    }

    /**
     * Get the Resource entity associated with this ResourceType.
     * @param type This ResourceType.
     * 
     */
    @Transactional(readOnly = true)
    public Resource getResourceTypeResource(Integer typeId) {
        ResourceType resourceType = resourceTypeDAO.findById(typeId);
        return resourceType.getResource();
    }

    /**
     * Find the Resource that has the given instance ID and ResourceType.
     * @param type The ResourceType of the Resource you're looking for.
     * @param instanceId Your ID for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     * 
     */
    @Transactional(readOnly = true)
    public Resource findResourceByInstanceId(ResourceType type, Integer instanceId) {
        Resource resource = findResourceByInstanceId(type.getId(), instanceId);

        if (resource == null) {
            throw new RuntimeException("Unable to find resourceType=" + type.getId() +
                                       " instanceId=" + instanceId);
        }
        return resource;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Resource findResourceByInstanceId(Integer typeId, Integer instanceId) {
        return resourceDAO.findByInstanceId(typeId, instanceId);
    }
    
    @Transactional(readOnly = true)
    public Collection<Resource> getUnconfiguredResources() {
        return resourceDAO.getUnconfiguredResources();
    }

    /**
     * Find's the root (id=0) resource
     * 
     */
    @Transactional(readOnly = true)
    public Resource findRootResource() {
        return resourceDAO.findRootResource();
    }

    @Transactional(readOnly = true)
    public Resource getResourceById(Integer id) {
        return resourceDAO.get(id);
    }

    @Transactional(readOnly = true)
    public Resource findResourceById(Integer id) {
        return resourceDAO.findById(id);
    }

    /**
     * Find the Resource that has the given instance ID and ResourceType name.
     * @param type The ResourceType of the Resource you're looking for.
     * @param instanceId Your ID for the resource you're looking for.
     * @return The value-object of the Resource of the given ID.
     * 
     */
    @Transactional(readOnly = true)
    public Resource findResourceByTypeAndInstanceId(String type, Integer instanceId) {
        ResourceType resType = resourceTypeDAO.findByName(type);
        return resourceDAO.findByInstanceId(resType.getId(), instanceId);
    }

    private Platform findPlatformById(Integer id) throws PlatformNotFoundException {
        Platform platform = platformDAO.get(id);

        if (platform == null)
            throw new PlatformNotFoundException(id);

        // Make sure that resource is loaded as to not get
        // LazyInitializationException
        platform.getName();

        return platform;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Resource findResource(AppdefEntityID aeid) {
        Resource rtn = null;
        try {
            final Integer id = aeid.getId();
            switch (aeid.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    Server server = serverDAO.get(id);
                    if (server == null) {
                        return null;
                    }
                    rtn = server.getResource();
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    rtn = findPlatformById(id).getResource();
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    org.hyperic.hq.appdef.server.session.Service service = serviceDAO.get(id);
                    if (service == null) {
                        return null;
                    }
                    rtn = service.getResource();
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                    // XXX not sure about appdef group mapping since 4.0
                    rtn = resourceDAO.findByInstanceId(aeid.getAuthzTypeId(), id);
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
                    rtn = findApplicationById(overlord, id).getResource();
                    break;
                default:
                    rtn = resourceDAO.findByInstanceId(aeid.getAuthzTypeId(), id);
            }
            // return real object instead of a proxy
            return (rtn == null) ? null : getResourceById(rtn.getId());
        } catch (ApplicationNotFoundException e) {
            log.debug(e,e);
        } catch (ObjectNotFoundException e) {
            log.debug(e,e);
        } catch (PermissionException e) {
            log.debug(e,e);
        } catch (PlatformNotFoundException e) {
            log.debug(e,e);
        }
        return null;
    }

    private Application findApplicationById(AuthzSubject subject, Integer id)
        throws ApplicationNotFoundException, PermissionException {
        try {
            Application app = applicationDAO.findById(id);
            permissionManager.checkViewPermission(subject, app.getEntityId());
            return app;
        } catch (ObjectNotFoundException e) {
            throw new ApplicationNotFoundException(id, e);
        }
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Resource findResourcePrototype(AppdefEntityTypeID id) {
        return findPrototype(id);
    }

    private Resource findPrototype(AppdefEntityTypeID id) {
        Integer authzType;

        switch (id.getType()) {
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
                throw new IllegalArgumentException("Unsupported prototype type: " + id.getType());
        }
        return resourceDAO.findByInstanceId(authzType, id.getId());
    }

    /**
     * Removes the specified resource by nulling out its resourceType. Will not
     * null the resourceType of the resource which is passed in. These resources
     * need to be cleaned up eventually by
     * {@link AppdefBossImpl.removeDeletedResources}. This may be done in the
     * background via zevent by issuing a {@link ResourcesCleanupZevent}.
     * @see {@link AppdefBossImpl.removeDeletedResources}
     * @see {@link ResourcesCleanupZevent}
     * @param r {@link Resource} resource to be removed.
     * @param nullResourceType tells the method to null out the resourceType
     * @param removeAllVirtual tells the method to remove all resources, including
     *        associated platforms, under the virtual resource hierarchy
     * @return AppdefEntityID[] - an array of the resources (including children)
     *         deleted
     * TODO suspending transaction was a performance enhancement from previous releases (i.e. 4.3)
     * It makes integration testing difficult, but we need to live w/it for now  until we can
     * re-visit the whole asynch deletion concept.  Makes a HUGE difference in performance.
     */
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public AppdefEntityID[] removeResourceAndRelatedResources(AuthzSubject subj, Resource r,
                                                boolean nullResourceType,
                                                boolean removeAllVirtual) throws VetoException,
        PermissionException {
        final ResourceType resourceType = r.getResourceType();

        // Possible this resource has already been marked for deletion
        if (resourceType == null) {
            return new AppdefEntityID[0];
        }

        // Make sure user has permission to remove this resource
        final PermissionManager pm = PermissionManagerFactory.getInstance();
        String opName = null;

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
        if (debug)
            watch.markTimeBegin("removeResourceAndRelatedResources.pmCheck");
        pm.check(subj.getId(), resourceType, r.getInstanceId(), opName);
        if (debug) {
            watch.markTimeEnd("removeResourceAndRelatedResources.pmCheck");
        }

        ResourceEdgeDAO edgeDao = resourceEdgeDAO;
        if (debug) {
            watch.markTimeBegin("removeResourceAndRelatedResources.findEdges");
        }
        Collection<ResourceEdge> edges = edgeDao.findDescendantEdges(r, getContainmentRelation());
        Collection<ResourceEdge> virtEdges = edgeDao.findDescendantEdges(r, getVirtualRelation());
        if (debug) {
            watch.markTimeEnd("removeResourceAndRelatedResources.findEdges");
        }
        Set<AppdefEntityID> removed = new HashSet<AppdefEntityID>();
        for (ResourceEdge edge : edges) {
            // Remove descendants' permissions
            removed.addAll(Arrays.asList(removeResourceAndRelatedResources(subj, edge.getTo(), true, removeAllVirtual)));
        }
		
		for (ResourceEdge edge : virtEdges) {
			Resource prototype = edge.getTo().getPrototype();

			if (!removeAllVirtual
					&& (prototype == null || !AuthzConstants.VMWARE_PROTOTYPES
							.contains(prototype.getName()))) {
				// do not remove the associated resources,
				// but remove the virtual resource edges
			    resourceRemover.removeEdges(edge.getTo(), getVirtualRelation());
			} else {
				resourceRemover.removeResource(subj, edge.getTo(), true);
			}
		}
		
        removed.add(AppdefUtil.newAppdefEntityId(r));
        if (debug) {
            watch.markTimeBegin("removeResource");
        }
        resourceRemover.removeResource(subj, r, nullResourceType);
        if (debug) {
            watch.markTimeEnd("removeResource");
            log.debug(watch);
        }
        return removed.toArray(new AppdefEntityID[0]);
    }

  

    /**
     * 
     */
    public void removeResource(AuthzSubject subject, Resource r) throws VetoException {
        resourceRemover.removeResource(subject, r);
    }

    /**
     * 
     */
    public void setResourceOwner(AuthzSubject whoami, Resource resource, AuthzSubject newOwner)
        throws PermissionException {
        PermissionManager pm = PermissionManagerFactory.getInstance();

        if (pm.hasAdminPermission(whoami.getId()) || resourceDAO.isOwner(resource, whoami.getId())) {
            resource.setOwner(newOwner);
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
    // TODO: G
    @Transactional(readOnly = true)
    public List<ResourceType> getAllResourceTypes(AuthzSubject subject, PageControl pc) {
        Collection<ResourceType> resTypes = resourceTypeDAO.findAll();
        pc = PageControl.initDefaults(pc, SortAttribute.RESTYPE_NAME);
        return resourceTypePager.seek(resTypes, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get viewable resources by "type" OR "resource name"
     * 
     * @param subject
     * @return Map of resource values
     * 
     */
    @Transactional(readOnly = true)
    public PageList<Resource> findViewables(AuthzSubject subject, String searchFor, final PageControl pc) {
        final StopWatch watch = new StopWatch();
        final PermissionManager pm = PermissionManagerFactory.getInstance();
        final Collection<ResourceType> types = getPSSAGResourceTypes();
        searchFor = (searchFor.startsWith("*")) ? "." + searchFor : searchFor;
        final Pattern pattern = Pattern.compile(searchFor, Pattern.CASE_INSENSITIVE);
        final int pagesize = pc.getPagesize();
        final int startIndex = pc.getPagenum() * pc.getPagesize();
        final Set<Resource> resources = pm.findViewableResources(subject, types, pc.getSortorder(),
            new IntegerTransformer<Resource>() {
                int returned = 0;
                int index = 0;
                public Resource transform(Integer id) {
                    if (pc != null && returned > pagesize) {
                        return null;
                    }
                    final Resource resource = resourceDAO.get(id);
                    if (resource == null || resource.isInAsyncDeleteState()) {
                        return null;
                    }
                    if (pattern.matcher(resource.getName()).find()) {
                        if (index++ >= startIndex) {
                            returned++;
                            return resource;
                        }
                    }
                    return null;
                }
            }
        );
        final Pager pager = Pager.getDefaultPager();
        final List<Resource> paged = pager.seek(resources, pc);
        PageList<Resource> rtn = new PageList<Resource>();
        for (Resource r : paged) {
            rtn.add(r);
        }
        rtn.setTotalSize(resources.size());
        if (log.isDebugEnabled()) log.debug("time to search for " + searchFor + ": " + watch);
        return rtn;
    }
    
    /**
     * Get Platform, Server, Service, Application and Group ResourceTypes
     */
    public Collection<ResourceType> getPSSTypes() {
        final Collection<ResourceType> types = new ArrayList<ResourceType>(5);
        types.add(findResourceTypeById(AuthzConstants.authzPlatform));
        types.add(findResourceTypeById(AuthzConstants.authzServer));
        types.add(findResourceTypeById(AuthzConstants.authzService));
        return types;
    }
    
    /**
     * Get Platform, Server, Service, Application and Group ResourceTypes
     */
    private Collection<ResourceType> getPSSAGResourceTypes() {
        final Collection<ResourceType> types = new ArrayList<ResourceType>(5);
        types.add(findResourceTypeById(AuthzConstants.authzPlatform));
        types.add(findResourceTypeById(AuthzConstants.authzServer));
        types.add(findResourceTypeById(AuthzConstants.authzService));
        types.add(findResourceTypeById(AuthzConstants.authzGroup));
        types.add(findResourceTypeById(AuthzConstants.authzApplication));
        return types;
    }
    

    @Transactional(readOnly = true)
    public Map<Resource, Collection<Resource>> findChildResources(List<Resource> resources,
                                                                  final Set<Integer> viewableResourceIds,
                                                                  final boolean includeSystemResources) {
        if (viewableResourceIds == null || viewableResourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Collection<ResourceEdge> edges = resourceEdgeDAO.findChildEdges(resources, getContainmentRelation());
        final Classifier<ResourceEdge, Resource, Resource> classifier =
            new Classifier<ResourceEdge, Resource, Resource>() {
            @Override
            public NameValue<Resource, Resource> classify(ResourceEdge edge) {
                final Resource parent = edge.getFrom();
                final Resource child = edge.getTo();
                if (viewableResourceIds.contains(child.getId()) || (includeSystemResources && child.isSystem())) {
                    return new NameValue<Resource, Resource>(parent, child);
                }
                return null;
            }
        };
        return classifier.classify(edges);
    }

    @Transactional(readOnly = true)
    public Collection<Integer> findAllViewableResourceIds(AuthzSubject subject,
                                                          Collection<ResourceType> resourceTypes) {
        final PermissionManager pm = PermissionManagerFactory.getInstance();
        final Set<Integer> resources = (resourceTypes == null || resourceTypes.size() == 0) ?
            pm.findViewableResources(subject, resourceTypeDAO.findAll()) :
            pm.findViewableResources(subject, resourceTypes);
        return resources;
    }
    
    @Transactional(readOnly = true)
    public ResourceType getResourceTypeById(Integer resourceTypeId) {
        return resourceTypeDAO.get(resourceTypeId);
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
    public Map<String, Collection<Integer>> findAllViewableInstances(AuthzSubject subject,
                                                                     Collection<ResourceType> types) {
        final Map<String, Collection<Integer>> rtn = new HashMap<String, Collection<Integer>>();
        final PermissionManager pm = PermissionManagerFactory.getInstance();
        final Set<Integer> resources = (types == null) ?
            pm.findViewableResources(subject, resourceTypeDAO.findAll()) :
            pm.findViewableResources(subject, types);
        for (final Integer resId : resources) {
            final Resource res = findResourceById(resId);
            if (res == null || res.isInAsyncDeleteState() || res.isSystem()) {
                continue;
            }
            final String type = res.getResourceType().getName();
            Collection<Integer> tmp;
            if (null == (tmp = rtn.get(type))) {
                tmp = new ArrayList<Integer>();
                rtn.put(type, tmp);
            }
            tmp.add(res.getInstanceId());
        }
        return rtn;
    }

    /**
     * Find all the resources which are descendants of the given resource
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> findResourcesByParent(AuthzSubject subject, Resource res) {
        return resourceDAO.findByResource(subject, res);
    }

    /**
     * Find all the resources which are direct descendants of the given resource.
     * In resource edge terminology, distance = 1.
     */
    @Transactional(readOnly = true)
    public List<Resource> findChildren(AuthzSubject subject, Resource res) {
        return resourceDAO.findChildren(subject, res);
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
        return resourceDAO.findResourcesOfType(resourceType, pInfo);
    }

    /**
     * Find all the resources which have the specified prototype
     * @return a list of {@link Resource}s
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> findResourcesOfPrototype(Resource proto, PageInfo pInfo) {
        return resourceDAO.findResourcesOfPrototype(proto, pInfo);
    }

    @Transactional(readOnly = true)
    public Collection<Resource> getResourcesByPrototypeAndRegex(Resource proto, String regex) {
        return resourceDAO.getResourcesByPrototype(proto, regex);
    }

    /**
     * Get all resources which are prototypes of platforms, servers, and
     * services and have a resource of that type in the inventory.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> findAppdefPrototypes() {
        return resourceDAO.findAppdefPrototypes();
    }

    /**
     * Get all resources which are prototypes of platforms, servers, and
     * services.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> findAllAppdefPrototypes() {
        return resourceDAO.findAllAppdefPrototypes();
    }

    /**
     * Find all the resources by plugin
     * @return a list of {@link Resource}s
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> findResourcesByPlugin(String pluginName) {
        return resourceDAO.findByPlugin(pluginName);
    }
    
    /**
     * Get viewable service resources. Service resources include individual
     * cluster unassigned services as well as service clusters.
     * 
     * @param subject
     * @param pc control
     * @return PageList of resource values
     * 
     */
    @Transactional(readOnly = true)
    public PageList<Resource> findViewableSvcResources(AuthzSubject subject, String nameFilter,
                                                       PageControl pc) {

        AuthzSubject subj = authzSubjectDAO.findById(subject.getId());

        pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);

        PermissionManager pm = PermissionManagerFactory.getInstance();

        // returns a sorted Collection by resourceName
        Collection<Resource> resources = pm.findServiceResources(subj, Boolean.FALSE);

        if (nameFilter != null) {
            for (Iterator<Resource> it = resources.iterator(); it.hasNext();) {
                Resource r = it.next();
                if (r == null || r.isInAsyncDeleteState() ||
                    !r.getName().toLowerCase().contains(nameFilter.toLowerCase())) {
                    it.remove();
                }
            }
        }

        Collection<Resource> ordResources = resources;
        if (pc.isDescending()) {
            ordResources = new ArrayList<Resource>(resources);
            Collections.reverse((List<Resource>) ordResources);
        }

        return new PageList<Resource>(ordResources, ordResources.size());
    }

    /**
     * Gets all the Resources owned by the given Subject.
     * @param subject The owner.
     * @return Array of resources owned by the given subject.
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Resource> findResourceByOwner(AuthzSubject owner) {
        return resourceDAO.findByOwner(owner, PageControl.SORT_UNSORTED);
    }

    /**
     * 
     * @param parentList {@link List} of {@link Resource}s
     * @return {@link Collection} of {@link ResourceEdge}s
     */
    @Transactional(readOnly = true)
    public Collection<ResourceEdge> findResourceEdges(ResourceRelation relation,
                                                      List<Resource> parentList) {
        return resourceEdgeDAO.findDescendantEdges(parentList, relation);
    }

    /**
     * 
     * @return {@link Collection} of {@link ResourceEdge}s
     */
    @Transactional(readOnly = true)
    public Collection<ResourceEdge> findResourceEdges(ResourceRelation relation, Resource parent) {
        return resourceEdgeDAO.findDescendantEdges(parent, relation);
    }

    /**
     * 
     * 
     */
    @Transactional(readOnly = true)
    public boolean isResourceChildOf(Resource parent, Resource child) {
        return resourceEdgeDAO.isResourceChildOf(parent, child);
    }

    @Transactional(readOnly = true)
    public boolean hasChildResourceEdges(Resource resource, ResourceRelation relation) {
        return resourceEdgeDAO.hasChildren(resource, relation);
    }

    @Transactional(readOnly = true)
    public int getDescendantResourceEdgeCount(Resource resource, ResourceRelation relation) {
        return resourceEdgeDAO.getDescendantCount(resource, relation);
    }

    @Transactional(readOnly = true)
    public Collection<ResourceEdge> findChildResourceEdges(Resource resource,
                                                           ResourceRelation relation) {
        return resourceEdgeDAO.findChildEdges(resource, relation);
    }

    @Transactional(readOnly = true)
    public Collection<ResourceEdge> findDescendantResourceEdges(Resource resource,
                                                                ResourceRelation relation) {
        return resourceEdgeDAO.findDescendantEdges(resource, relation);
    }

    @Transactional(readOnly = true)
    public Collection<ResourceEdge> findAncestorResourceEdges(Resource resource,
                                                              ResourceRelation relation) {
        return resourceEdgeDAO.findAncestorEdges(resource, relation);
    }

    @Transactional(readOnly = true)
    public Collection<ResourceEdge> findResourceEdgesByName(String name, ResourceRelation relation) {
        return resourceEdgeDAO.findByName(name, relation);
    }

    @Transactional(readOnly = true)
    public Collection<Resource> getParentResources(AuthzSubject subj, ResourceGroup group,
                                                   ResourceRelation relation, int distance,
                                                   int maxdistance) {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        final Resource proto = group.getResourcePrototype();
        final Collection<ResourceType> types = (proto == null) ?
            resourceTypeDAO.findAll() : getTypeFromProto(proto.getResourceType());

        if (debug) watch.markTimeBegin("permissionManager.findViewableResources");
        final Set<Integer> viewable = permissionManager.findViewableResources(subj, types);
        if (debug) watch.markTimeEnd("permissionManager.findViewableResources");

        if (debug) watch.markTimeBegin("resourceGroupManager.getMembers");
        final List<Resource> resources = resourceGroupManager.getMembers(group);
        if (debug) watch.markTimeEnd("resourceGroupManager.getMembers");
        if (resources.isEmpty()) {
            return Collections.emptyList();
        }

        if (debug) watch.markTimeBegin("resourceDAO.getParentResourceIds");
        final Collection<Integer> parentIds =
            resourceDAO.getParentResourceIds(resources, relation, distance, maxdistance);
        if (debug) watch.markTimeEnd("resourceDAO.getParentResourceIds");

        final Set<Resource> rtn = new HashSet<Resource>(parentIds.size());
        if (debug) watch.markTimeBegin("resourceDAO.get");
        for (final Integer parentId : parentIds) {
            final Resource parent = resourceDAO.get(parentId);
            if (parent == null || parent.isInAsyncDeleteState()) {
                continue;
            }
            if (viewable.contains(parent.getId())) {
                rtn.add(parent);
            }
        }
        if (debug) watch.markTimeEnd("resourceDAO.get");

        if (debug) log.debug(watch);
        return rtn;
    }

    private Collection<ResourceType> getTypeFromProto(ResourceType resourceType) {
        Integer id = resourceType.getId();
        Collection<ResourceType> rtn = new ArrayList<ResourceType>(3);
        if (id.equals(AuthzConstants.authzPlatformProto)) {
            rtn.add(resourceTypeDAO.get(AuthzConstants.authzPlatform));
            return rtn;
        } else if (id.equals(AuthzConstants.authzServerProto)) {
            rtn.add(resourceTypeDAO.get(AuthzConstants.authzPlatform));
            rtn.add(resourceTypeDAO.get(AuthzConstants.authzServer));
            return rtn;
        } else if (id.equals(AuthzConstants.authzServiceProto)) {
            rtn.add(resourceTypeDAO.get(AuthzConstants.authzPlatform));
            rtn.add(resourceTypeDAO.get(AuthzConstants.authzServer));
            rtn.add(resourceTypeDAO.get(AuthzConstants.authzService));
            return rtn;
        }
        return resourceTypeDAO.findAll();
    }

    @Transactional(readOnly = true)
    public Collection<Resource> getDescendantResources(AuthzSubject subj, Collection<Resource> resources,
                                                       ResourceRelation relation, Resource proto, boolean children) {
        final Collection<ResourceEdge> parentEdges =
            resourceEdgeDAO.getDescendantEdgesOfType(resources, proto, relation, children ? 1 : -1);
        final Map<Integer, Resource> rtn = new HashMap<Integer, Resource>(parentEdges.size());
        final Set<ResourceType> types = new HashSet<ResourceType>();
        for (final ResourceEdge edge : parentEdges) {
            if (edge == null) {
                continue;
            }
            final Resource res = edge.getTo();
            if (res == null || res.isInAsyncDeleteState()) {
                continue;
            }
            types.add(res.getResourceType());
            rtn.put(res.getId(), res);
        }
        return permissionManager.findViewableResources(subj, types, new IntegerTransformer<Resource>() {
            public Resource transform(Integer id) {
                return rtn.get(id);
            }
        });
    }

    @Transactional(readOnly = true)
    public Collection<Resource> getParentResources(AuthzSubject subj, Resource resource, ResourceRelation relation) {
        if (resource.getResourceType().getId().equals(AuthzConstants.authzGroup)) {
            final ResourceGroup group = resourceGroupManager.findResourceGroupById(resource.getInstanceId());
            return getParentResources(subj, group, relation, -1, -1);
        }
        return getDescendantResources(subj, Collections.singletonList(resource), relation, null, false);
    }

    @Transactional(readOnly = true)
    public ResourceEdge getParentResourceEdge(Resource resource, ResourceRelation relation) {
        return resourceEdgeDAO.getParentEdge(resource, relation);
    }

    @Transactional(readOnly = true)
    public boolean hasResourceRelation(Resource resource, ResourceRelation relation) {
        return resourceEdgeDAO.hasResourceRelation(resource, relation);
    }

    /**
     * 
     * @return {@link Collection} of {@link ResourceEdge}s
     */
    @Transactional(readOnly = true)
    public List<ResourceEdge> findResourceEdges(ResourceRelation relation, Integer resourceId,
                                                List<Integer> platformTypeIds, String platformName) {
        if (relation == null || !relation.getId().equals(AuthzConstants.RELATION_NETWORK_ID)) {
            throw new IllegalArgumentException("Only " +
                                               AuthzConstants.ResourceEdgeNetworkRelation +
                                               " resource relationships are supported.");
        }

        return resourceEdgeDAO.findDescendantEdgesByNetworkRelation(resourceId, platformTypeIds,
            platformName);
    }

    private void createResourceEdges(Resource parent, Resource child, ResourceRelation relation,
                                     boolean createSelfEdge) {

        // Self-edge
        if (createSelfEdge) {
            resourceEdgeDAO.create(child, child, 0, relation);
        }

        // Direct edges
        resourceEdgeDAO.create(child, parent, -1, relation);
        resourceEdgeDAO.create(parent, child, 1, relation);

        // Ancestor edges to new destination resource
        Collection<ResourceEdge> ancestors = resourceEdgeDAO.findAncestorEdges(parent, relation);
        for (ResourceEdge ancestorEdge : ancestors) {
            int distance = ancestorEdge.getDistance() - 1;

            resourceEdgeDAO.create(child, ancestorEdge.getTo(), distance, relation);
            resourceEdgeDAO.create(ancestorEdge.getTo(), child, -distance, relation);
        }
    }

    /**
     *
     * 
     */
    public void createResourceEdges(AuthzSubject subject, ResourceRelation relation,
                                    AppdefEntityID parent, AppdefEntityID[] children)
        throws PermissionException, ResourceEdgeCreateException {
        createResourceEdges(subject, relation, parent, children, false);
    }

    private ConfigResponseDB getConfigResponse(AppdefEntityID id) {
        ConfigResponseDB config;

        switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                config = platformDAO.findById(id.getId()).getConfigResponse();
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                config = serverDAO.findById(id.getId()).getConfigResponse();
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                config = serviceDAO.findById(id.getId()).getConfigResponse();
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            default:
                throw new IllegalArgumentException("The passed entity type "
                                                   + "does not support config " + "responses");
        }

        return config;
    }
    
    /**
     *
     * 
     */
    public void createResourceEdges(AuthzSubject subject, ResourceRelation relation,
                                    AppdefEntityID parent, AppdefEntityID[] children,
                                    boolean deleteExisting) throws PermissionException,
        ResourceEdgeCreateException {

        if (relation == null) {
            throw new ResourceEdgeCreateException("Resource relation is null");
        }
        if (relation.getId().equals(AuthzConstants.RELATION_NETWORK_ID)) {
            createNetworkResourceEdges(subject, relation, parent, children, deleteExisting);
        } else if (relation.getId().equals(AuthzConstants.RELATION_VIRTUAL_ID)) {
            createVirtualResourceEdges(subject, relation, parent, children);
        } else {
            throw new ResourceEdgeCreateException("Unsupported resource relation: " +
                                                  relation.getName());
        }

    }

    private void createVirtualResourceEdges(AuthzSubject subject, ResourceRelation relation,
                                            AppdefEntityID parent, AppdefEntityID[] children)
        throws PermissionException, ResourceEdgeCreateException {

        Resource parentResource = findResource(parent);

        if (parentResource != null && !parentResource.isInAsyncDeleteState() && children != null &&
            children.length > 0) {

            try {

                if (!hasResourceRelation(parentResource, relation)) {
                    // create self-edge for parent of virtual hierarchy
                    resourceEdgeDAO.create(parentResource, parentResource, 0, relation);
                }
                for (int i = 0; i < children.length; i++) {
                    Resource childResource = findResource(children[i]);

                    // Check if child resource already exists in VM hierarchy
                    ResourceEdge existing = getParentResourceEdge(childResource, relation);

                    if (existing != null) {
                        Resource existingParent = existing.getTo();
                        if (existingParent.getId().equals(parentResource.getId())) {
                            createVirtualResourceEdgesByMacAddress(subject, childResource);

                            // already exists with same parent, so skip
                            if (log.isDebugEnabled()) {
                                log
                                    .debug("Skipping. Virtual resource edge already exists: from id=" +
                                           parentResource.getId() +
                                           ", to id=" +
                                           childResource.getId());
                            }
                            continue;
                        } else {
                            // already exists with different parent, assume
                            // vMotion occurred
                            if (log.isDebugEnabled()) {
                                log
                                    .debug("Virtual resource edge exists with another resource: fromId=" +
                                           existingParent.getId() +
                                           ", toId=" +
                                           childResource.getId() +
                                           ". Moving to target fromId=" +
                                           parentResource.getId());
                            }

                            // Clean out edges for the current target
                            Collection<ResourceEdge> edges = findDescendantResourceEdges(
                                childResource, relation);
                            for (ResourceEdge re : edges) {
                                resourceEdgeDAO.deleteEdges(re.getTo(), relation);
                            }
                            resourceEdgeDAO.deleteEdges(childResource, relation);
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
    private void createVirtualResourceEdges(AuthzSubject owner, Resource parent, Resource res,
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

    private boolean createVirtualResourceEdgesByMacAddress(AuthzSubject subject, Resource resource) {
        boolean isEdgesCreated = false;

        try {
            // TODO resolve circular dependency
            Platform associatedPlatform = Bootstrap.getBean(PlatformManager.class)
                .getAssociatedPlatformByMacAddress(subject, resource);

            if (associatedPlatform != null) {
                String vmPrototype = AuthzConstants.platformPrototypeVmwareVsphereVm;
                if (vmPrototype.equals(resource.getPrototype().getName())) {
                    isEdgesCreated = createVirtualResourceEdgesByMacAddress(resource,
                        associatedPlatform.getResource());
                } else {
                    isEdgesCreated = createVirtualResourceEdgesByMacAddress(associatedPlatform
                        .getResource(), resource, false);
                }
            }
        } catch (Exception e) {
            log.error("Could not create virtual resource edge by MAC address" +
                      " for resource[id=" + resource.getId() + "]: " + e.getMessage(), e);
        }

        return isEdgesCreated;
    }

    private boolean createVirtualResourceEdgesByMacAddress(Resource vmResource, Resource hqResource)
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
                //Possible (but not likely) for 2 physical platforms to have same MAC address (particularly in test scenarios)
                return false;
            } else if (vmPrototype.equals(hqResource.getPrototype().getName())) {
                throw new ResourceEdgeCreateException("Resource[id=" + hqResource.getId() +
                                                      "] cannot be a " + vmPrototype);
            }

            ResourceRelation relation = getVirtualRelation();

            if (getParentResourceEdge(hqResource, relation) == null) {
                createResourceEdges(vmResource, hqResource, relation, true);

                if (createServerEdges) {
                    // create virtual resource edges for the servers for the
                    // platform.
                    // data is redundant with the containtment resource edges,
                    // but is needed to improve search speed
                    try {
                        // TODO resolve circular dependency
                        Platform hqPlatform = Bootstrap.getBean(PlatformManager.class)
                            .findPlatformById(hqResource.getInstanceId());

                        for (Server s : hqPlatform.getServers()) {
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
                log.debug("createVirtualResourceEdgesByMacAddress: vmResourceId=" +
                          vmResource.getId() + ", hqResourceId=" + hqResource.getId() +
                          ", isEdgesCreated=" + isEdgesCreated);
            }
        }

        return isEdgesCreated;
    }

    private void createNetworkResourceEdges(AuthzSubject subject, ResourceRelation relation,
                                            AppdefEntityID parent, AppdefEntityID[] children,
                                            boolean deleteExisting) throws PermissionException,
        ResourceEdgeCreateException {
        if (parent == null || !parent.isPlatform()) {
            throw new ResourceEdgeCreateException("Only platforms are supported.");
        }

        Platform parentPlatform = null;
        //TODO resolve circular dependency
        PlatformManager platMan = Bootstrap.getBean(PlatformManager.class);
        try {
            parentPlatform = platMan.findPlatformById(parent.getId());
        } catch (PlatformNotFoundException pe) {
            throw new ResourceEdgeCreateException("Platform id " + parent.getId() + " not found.");
        }
        List<PlatformType> supportedPlatformTypes = new ArrayList<PlatformType>(platMan.findSupportedPlatformTypes());
        if (supportedPlatformTypes.contains(parentPlatform.getPlatformType())) {
            throw new ResourceEdgeCreateException(parentPlatform.getPlatformType().getName() +
                                                  " not supported as a top-level platform type.");
        }
        Resource parentResource = parentPlatform.getResource();
        // Make sure user has permission to modify resource edges
        permissionManager.check(subject.getId(), parentResource.getResourceType(), parentResource
            .getInstanceId(), AuthzConstants.platformOpModifyPlatform);

        // HQ-1670: Should not be able to add a parent resource to
        // a network hierarchy if it has not been configured yet
        ConfigResponseDB config = getConfigResponse(parent);
        if (config != null) {
            String validationError = config.getValidationError();
            if (validationError != null) {
                throw new ResourceEdgeCreateException("Resource id " + parentResource.getId() +
                                                      ": " + validationError);
            }
        }
        if (parentResource != null && !parentResource.isInAsyncDeleteState() && children != null &&
            children.length > 0) {

            try {
                if (deleteExisting) {
                    removeResourceEdges(subject, relation, parentResource);
                }

                
                Collection<ResourceEdge> edges = findResourceEdges(relation, parentResource);
                List<ResourceEdge> existing = null;
                Platform childPlatform = null;
                Resource childResource = null;

                if (edges.isEmpty()) {
                    // create self-edge for parent of network hierarchy
                    resourceEdgeDAO.create(parentResource, parentResource, 0, relation);
                }
                for (int i = 0; i < children.length; i++) {
                    if (!children[i].isPlatform()) {
                        throw new ResourceEdgeCreateException("Only platforms are supported.");
                    }
                    try {
                        childPlatform = platMan.findPlatformById(children[i].getId());
                        childResource = childPlatform.getResource();

                        if (!supportedPlatformTypes.contains(childPlatform.getPlatformType())) {
                            throw new ResourceEdgeCreateException(childPlatform.getPlatformType()
                                .getName() +
                                                                  " not supported as a dependent platform type.");
                        }
                    } catch (PlatformNotFoundException pe) {
                        throw new ResourceEdgeCreateException("Platform id " + children[i].getId() +
                                                              " not found.");
                    }

                    // Check if child resource already exists in a network
                    // hierarchy
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
                            throw new ResourceEdgeCreateException("Resource id " +
                                                                  childResource.getId() +
                                                                  " already exists in another network hierarchy.");
                        }
                    } else if (existing.size() > 1) {
                        // a resource can only belong to one network hierarchy
                        // this is a data integrity issue if it happens
                        throw new ResourceEdgeCreateException("Resource id " +
                                                              childResource.getId() +
                                                              " exists in " + existing.size() +
                                                              " network hierarchies.");
                    }
                    if (childResource != null && !childResource.isInAsyncDeleteState()) {
                        resourceEdgeDAO.create(parentResource, childResource, 1, relation);
                        resourceEdgeDAO.create(childResource, parentResource, -1, relation);
                    }
                }
            } catch (Throwable t) {
                throw new ResourceEdgeCreateException(t);
            }
        }
    }

    /**
     *
     * 
     */
    public void removeResourceEdges(AuthzSubject subject, ResourceRelation relation,
                                    AppdefEntityID parent, AppdefEntityID[] children)
        throws PermissionException {

        if (relation == null || !relation.getId().equals(AuthzConstants.RELATION_NETWORK_ID)) {
            throw new IllegalArgumentException("Only " +
                                               AuthzConstants.ResourceEdgeNetworkRelation +
                                               " resource relationships are supported.");
        }

        Resource parentResource = findResource(parent);
        Resource childResource = null;

        // Make sure user has permission to modify resource edges
        final PermissionManager pm = PermissionManagerFactory.getInstance();

        pm.check(subject.getId(), parentResource.getResourceType(), parentResource.getInstanceId(),
            AuthzConstants.platformOpModifyPlatform);

        if (parentResource != null && !parentResource.isInAsyncDeleteState()) {
            ResourceEdgeDAO eDAO = resourceEdgeDAO;

            for (int i = 0; i < children.length; i++) {
                childResource = findResource(children[i]);

                if (childResource != null && !childResource.isInAsyncDeleteState()) {
                    eDAO.deleteEdge(parentResource, childResource, relation);
                    eDAO.deleteEdge(childResource, parentResource, relation);
                }
            }
            Collection<ResourceEdge> edges = findResourceEdges(relation, parentResource);
            if (edges.isEmpty()) {
                // remove self-edge for parent of network hierarchy
                eDAO.deleteEdges(parentResource, relation);
            }
        }
    }

    /**
     *
     * 
     */
    public void removeResourceEdges(AuthzSubject subject, ResourceRelation relation, Resource parent)
        throws PermissionException {

        if (relation == null || !relation.getId().equals(AuthzConstants.RELATION_NETWORK_ID)) {
            throw new IllegalArgumentException("Only " +
                                               AuthzConstants.ResourceEdgeNetworkRelation +
                                               " resource relationships are supported.");
        }

        // Make sure user has permission to modify resource edges
        final PermissionManager pm = PermissionManagerFactory.getInstance();

        pm.check(subject.getId(), parent.getResourceType(), parent.getInstanceId(),
            AuthzConstants.platformOpModifyPlatform);

        resourceEdgeDAO.deleteEdges(parent, relation);
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

        final ResourceRelation relation = getContainmentRelation();
        for (final Resource resource : resources) {
            events.add(new ResourceUpdatedZevent(subj, AppdefUtil.newAppdefEntityId(resource)));
            final Collection<ResourceEdge> descendants = resourceEdgeDAO.findDescendantEdges(
                resource, relation);
            for (ResourceEdge edge : descendants) {
                final Resource r = edge.getTo();
                events.add(new ResourceUpdatedZevent(subj, AppdefUtil.newAppdefEntityId(r)));
            }
        }
        zeventManager.enqueueEventsAfterCommit(events);
    }

    @Transactional(readOnly = true)
    public ResourceRelation getContainmentRelation() {
        return resourceRelationDAO.findById(AuthzConstants.RELATION_CONTAINMENT_ID);
    }

    @Transactional(readOnly = true)
    public ResourceRelation getNetworkRelation() {
        return resourceRelationDAO.findById(AuthzConstants.RELATION_NETWORK_ID);
    }

    @Transactional(readOnly = true)
    public String getAppdefEntityName(AppdefEntityID appEnt) {
        Resource res = findResource(appEnt);
        if (res != null) {
            return res.getName();
        }
        return appEnt.getAppdefKey();
    }
    
    public ResourceRelation getVirtualRelation() {
        return resourceRelationDAO.findById(AuthzConstants.RELATION_VIRTUAL_ID);
    }

    @Transactional(readOnly = true)
    public int getPlatformCountMinusVsphereVmPlatforms() {
        return resourceDAO.getPlatformCountMinusVsphereVmPlatforms();
    }

    public ResourceType findResourceTypeById(Integer id) {
        return resourceTypeDAO.findById(id);
    }
    
    public Map<String, Long> getResourceCountByPlugin(Collection<Plugin> plugins) {
        final Collection<MonitorableType> types = new ArrayList<MonitorableType>();
        for (final Plugin plugin : plugins) {
            types.addAll(monitorableTypeDAO.findByPluginName(plugin.getName()).values());
        }
        return resourceDAO.getResourceCountByProtoTypeName(types);
    }

    public void removeResourcesAndTypes(AuthzSubject subj, Collection<MonitorableType> types) {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        if (debug) log.debug("Removing Resources By Types " + types);
        final Collection<String> typeNames = new ArrayList<String>(types.size());
        for (final MonitorableType type : types) {
            typeNames.add(type.getName());
        }
        if (debug) watch.markTimeBegin("getResourcesByProtoTypeName");
        final Collection<Resource> resources = resourceDAO.getResourcesByProtoTypeName(typeNames);
        if (debug) watch.markTimeEnd("getResourcesByProtoTypeName");
        for (final Resource r : resources) {
            try {
                if (debug) watch.markTimeBegin("removeResourcePerms");
                final AppdefEntityID[] aeids = removeResourceAndRelatedResources(subj, r, true, false);
                if (debug) watch.markTimeEnd("removeResourcePerms");
                if (debug) log.debug("removed aeids=[" + aeids + "]");
            } catch (PermissionException e) {
                log.error(e,e);
            } catch (VetoException e) {
                log.error(e,e);
            }
        }
        if (debug) log.debug("Done Removing Resources By Types " + watch);
        zeventManager.enqueueEventAfterCommit(new ResourceTypeCleanupZevent(typeNames));
    }

    public void removeResourceTypes(Collection<String> typeNames) {
        for (final String typeName : typeNames) {
            removeMeasurementTempls(typeName);
            final MonitorableType mt = monitorableTypeDAO.findByName(typeName);
            // mt should not be null at this point, but you never know
            if (mt != null) {
                monitorableTypeDAO.remove(mt);
            }
            final Resource proto = resourceDAO.findResourcePrototypeByName(typeName);
            // proto = null here means that no resources are configured with that particular type
            if (proto != null) {
                removeAppdefType(proto);
                AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
                try {
                    removeResource(overlord, proto);
                } catch (VetoException e) {
                    throw new SystemException(e);
                }
            }
        }
    }

    private void removeMeasurementTempls(String typeName) {
        final List<MeasurementTemplate> templs =
            measurementTemplateDAO.findDerivedByMonitorableType(typeName);
        for (final MeasurementTemplate t : templs) {
            measurementTemplateDAO.remove(t);
        }
    }

    private void removeAppdefType(Resource proto) {
        final Integer typeId = proto.getResourceType().getId();
        if (typeId.equals(AuthzConstants.authzPlatformProto)) {
            final PlatformType platformType = platformTypeDAO.get(proto.getInstanceId());
            if (platformType != null) {
                platformTypeDAO.remove(platformType);
            }
        } else if (typeId.equals(AuthzConstants.authzServerProto)) {
            final ServerType serverType = serverTypeDAO.get(proto.getInstanceId());
            if (serverType != null) {
                serverTypeDAO.remove(serverType);
            }
        } else if (typeId.equals(AuthzConstants.authzServiceProto)) {
            final ServiceType serviceType = serviceTypeDAO.get(proto.getInstanceId());
            if (serviceType != null) {
                serviceTypeDAO.remove(serviceType);
            }
        }
    }

    public Collection<Resource> getOrphanedResources() {
        return resourceDAO.getOrphanedResources();
    }

    public Collection<Resource> getRemovableChildren(AuthzSubject subject, Resource parent) {
        Collection<Resource> children = this.findChildren(subject,parent);
        Map<Resource, ConfigResponse> rscConf = this.configManager.getConfigResponses(new HashSet<Resource>(children),false);
        Collection<Resource> removableResources = new ArrayList<Resource>();
        for(Map.Entry<Resource, ConfigResponse> rscConfE:rscConf.entrySet()) {
            ConfigResponse conf = rscConfE.getValue();
            String removable = conf.getValue(Collector.REMOVABLE);
            if (removable!=null&&"true".equals(removable)) {
                removableResources.add(rscConfE.getKey());
            }
        }
        return removableResources;
    }
}
