/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.mgmt.data.AgentRepository;
import org.hyperic.hq.agent.mgmt.data.ManagedResourceRepository;
import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.hyperic.hq.agent.mgmt.domain.ManagedResource;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.EntityNotFoundException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ResourceAuditFactory;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.inventory.data.ResourceDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.RelationshipTypes;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.measurement.server.session.AgentScheduleSyncZevent;
import org.hyperic.hq.plugin.mgmt.data.PluginResourceTypeRepository;
import org.hyperic.hq.plugin.mgmt.domain.Plugin;
import org.hyperic.hq.plugin.mgmt.domain.PluginResourceType;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.sigar.NetFlags;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Platform objects in appdef and their
 * relationships
 * 
 */
@org.springframework.stereotype.Service
@Transactional
public class PlatformManagerImpl implements PlatformManager {
    
    public static final String IP_RESOURCE_TYPE_NAME = "IP";
    
    private final Log log = LogFactory.getLog(PlatformManagerImpl.class.getName());

    private static final String VALUE_PROCESSOR = PagerProcessor_platform.class.getName();

    private Pager valuePager;
    
    private PermissionManager permissionManager;

    private AgentRepository agentDAO;

    private ResourceManager resourceManager;

    private ResourceGroupManager resourceGroupManager;

    private AuditManager auditManager;

    private AgentManager agentManager;

    private ZeventEnqueuer zeventManager;
    
    private PlatformFactory platformFactory;

    private ResourceAuditFactory resourceAuditFactory;
    
    private ManagedResourceRepository managedResourceRepository;
    
    private PluginResourceTypeRepository pluginResourceTypeRepository;
    
    private ServerManager serverManager;
    
    private ServiceManager serviceManager;
    
    private ServerFactory serverFactory;
    
    private ResourceDao resourceDao;
    
    private ResourceTypeDao resourceTypeDao;
    
    private ServiceFactory serviceFactory;
   

    @Autowired
    public PlatformManagerImpl(
                               PermissionManager permissionManager, AgentRepository agentDAO,
                               ResourceManager resourceManager,
                               ResourceGroupManager resourceGroupManager,
                               AuditManager auditManager, AgentManager agentManager,
                               ZeventEnqueuer zeventManager,
                               ResourceAuditFactory resourceAuditFactory, PluginResourceTypeRepository pluginResourceTypeRepository,
                               ServerManager serverManager, PlatformFactory platformFactory,
                               ServiceManager serviceManager, ServerFactory serverFactory,
                               ResourceDao resourceDao, ResourceTypeDao resourceTypeDao, 
                               ManagedResourceRepository managedResourceRepository, ServiceFactory serviceFactory) {
        this.permissionManager = permissionManager;
        this.agentDAO = agentDAO;
        this.resourceManager = resourceManager;
        this.resourceGroupManager = resourceGroupManager;
        this.auditManager = auditManager;
        this.agentManager = agentManager;
        this.zeventManager = zeventManager;
        this.resourceAuditFactory = resourceAuditFactory;
        this.pluginResourceTypeRepository = pluginResourceTypeRepository;
        this.serverManager = serverManager;
        this.platformFactory = platformFactory;
        this.serviceManager = serviceManager;
        this.serverFactory = serverFactory;
        this.resourceDao = resourceDao;
        this.resourceTypeDao  = resourceTypeDao;
        this.managedResourceRepository = managedResourceRepository;
        this.serviceFactory = serviceFactory;
    }
    
    private Platform toPlatform(Resource resource) {
        Platform platform = platformFactory.createPlatform(resource);
        Set<Resource> servers = resource.getResourcesFrom(RelationshipTypes.SERVER);
        for(Resource server: servers) {
            platform.addServer(serverFactory.createServer(server));
        }
        Set<Resource> services = resource.getResourcesFrom(RelationshipTypes.SERVICE);
        for(Resource service: services) {
            platform.addService(serviceFactory.createService(service));
        }
        return platform;
    }

    // TODO resolve circular dependency
    private AIQueueManager getAIQueueManager() {
        return Bootstrap.getBean(AIQueueManager.class);
    }

    // TODO remove after HE-54 allows injection
    private PlatformCounter getCounter() {
        PlatformCounter counter = (PlatformCounter) ProductProperties
            .getPropertyInstance("hyperic.hq.platform.counter");

        if (counter == null) {
            counter = new DefaultPlatformCounter();
        }
        return counter;
    }
    
    /**
     * Find a PlatformType by id
     * 
     * 
     */
    @Transactional(readOnly = true)
    public PlatformType findPlatformType(Integer id)  {
        ResourceType platType = resourceManager.findResourceTypeById(id);
        if(platType ==  null) {
            throw new EntityNotFoundException("Resource Type with ID: " + id + 
                " was not found");
        }
        return platformFactory.createPlatformType(platType);
    }

    /**
     * Find a platform type by name
     * 
     * @param type - name of the platform type
     * @return platformTypeValue
     * 
     */
    @Transactional(readOnly = true)
    public PlatformType findPlatformTypeByName(String type) throws PlatformNotFoundException {
        ResourceType ptype = resourceManager.findResourceTypeByName(type);
        if (ptype == null) {
            throw new PlatformNotFoundException(type);
        }
        return platformFactory.createPlatformType(ptype);
    }
    
    private Collection<ResourceType> findAllPlatformResourceTypes() {
        return resourceManager.findRootResourceType().getResourceTypesFrom(RelationshipTypes.PLATFORM);
    }
 
    /**
     * Find all platform types
     * 
     * @return List of PlatformTypeValues
     * 
     */
    @Transactional(readOnly = true)
    public PageList<PlatformTypeValue> getAllPlatformTypes(AuthzSubject subject, PageControl pc) {
        List<PlatformType> platformTypes = new ArrayList<PlatformType>();
        Collection<ResourceType> resourceTypes = findAllPlatformResourceTypes();
        for(ResourceType resourceType: resourceTypes) {
            platformTypes.add(platformFactory.createPlatformType(resourceType));
        }
        Collections.sort(platformTypes, new Comparator<PlatformType>() {
            public int compare(PlatformType o1, PlatformType o2) {
                return o1.getSortName().compareTo(o2.getSortName());
            }
        });
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(platformTypes, pc);
    }

    /**
     * Find viewable platform types
     * 
     * @return List of PlatformTypeValues
     * 
     */
    @Transactional(readOnly = true)
    public PageList<PlatformTypeValue> getViewablePlatformTypes(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {

        // TODO build the platform types from the platforms the user is allowed to view
        //Collection<PlatformType> platTypes = filterResourceTypes(platforms);
        return getAllPlatformTypes(subject, pc);
    }

    /**
     * Get PlatformPluginName for an entity id. There is no authz in this method
     * because it is not needed.
     * 
     * @return name of the plugin for the entity's platform such as
     *         "Apache 2.0 Linux". It is used as to look up plugins via a
     *         generic plugin manager.
     * 
     */
    @Transactional(readOnly = true)
    public String getPlatformPluginName(AppdefEntityID id) throws AppdefEntityNotFoundException {
        Resource p;
        String typeName;

        if (id.isService()) {
            // look up the service
            Resource service = resourceManager.findResourceById(id.getId());

            if (service == null) {
                throw new ServiceNotFoundException(id);
            }
            Resource parent = service.getResourceTo(RelationshipTypes.SERVICE);
            if(parent.getProperty(AppdefResourceType.APPDEF_TYPE_ID).equals(AppdefEntityConstants.APPDEF_TYPE_SERVER)) {
                p = parent.getResourceTo(RelationshipTypes.SERVER);
            }else {
                p = parent;
            }
            typeName = service.getType().getName();
        } else if (id.isServer()) {
            // look up the server
            Resource server = resourceManager.findResourceById(id.getId());

            if (server == null) {
                throw new ServerNotFoundException(id);
            }
            p = server.getResourceTo(RelationshipTypes.SERVER);
            typeName = server.getType().getName();
        } else if (id.isPlatform()) {
            Platform platform = findPlatformById(id.getId());
            return platform.getPlatformType().getName();
        } else if (id.isGroup()) {
            ResourceGroup g = resourceGroupManager.findResourceGroupById(id.getId());
            return g.getType().getName();
        } else {
            throw new IllegalArgumentException("Unsupported entity type: " + id);
        }

        return typeName + " " + p.getType().getName();
        
    }

    /**
     * Delete a platform
     * 
     * @param subject The user performing the delete operation.
     * @param id - The id of the Platform
     * 
     */
    public void removePlatform(AuthzSubject subject, Platform platform)
        throws PlatformNotFoundException, PermissionException, VetoException {
        //TODO audit
        //final Audit audit = resourceAuditFactory.deleteResource(resourceManager
          //  .findResourceById(AuthzConstants.authzHQSystem), subject, 0, 0);
        //boolean pushed = false;
        try {
            //auditManager.pushContainer(audit);
            //pushed = true;
            //TODO perm check
            //permissionManager.checkRemovePermission(subject, platform.getId());
            
            //remove server
            removeServers(subject,platform);
            removeServices(subject,platform);
            removeIPs(platform);
            
            getAIQueueManager().removeAssociatedAIPlatform(platform);
           
           
             //config, cprops, and relationships will get cleaned up by removal here
            resourceManager.removeResource(subject, resourceManager.findResourceById(platform.getId()));

        //} catch (PermissionException e) {
          //  log.debug("Error while removing Platform");
            //throw e;
        } finally {
           // if (pushed)
             //   auditManager.popContainer(false);
        }
    }

  
   private void removeServers(AuthzSubject subject,Platform platform) throws PermissionException, VetoException {
       Set<Resource> servers=resourceManager.findResourceById(platform.getId()).getResourcesFrom(RelationshipTypes.SERVER);
       for(Resource server: servers) {
           serverManager.removeServer(subject, server.getId());
       }
   }
   
   private void removeServices(AuthzSubject subject,Platform platform) throws PermissionException, VetoException {
       Set<Resource> services=resourceManager.findResourceById(platform.getId()).getResourcesFrom(RelationshipTypes.SERVICE);
       for(Resource service: services) {
           serviceManager.removeService(subject, service.getId());
       }
   }
   
   private void removeIPs(Platform platform) {
       Set<Resource> ips=resourceManager.findResourceById(platform.getId()).getResourcesFrom(RelationshipTypes.IP);
       for(Resource ip: ips) {
           ip.remove();
       }
   }
     
   private Resource create(AuthzSubject subject,AIPlatformValue aip, String initialOwner,Agent agent, ResourceType type) 
   {
        String name = aip.getName();
        if (name == null || "".equals(name.trim())) {
           name = aip.getFqdn();
        }
        Resource p = new Resource(name, type);
        resourceDao.persist(p);
        p.setDescription(aip.getDescription());
        p.setLocation("");
        
        //TODO abstract creationTime, modifiedTime?
        p.setProperty(PlatformFactory.CREATION_TIME, System.currentTimeMillis());
        p.setProperty(PlatformFactory.MODIFIED_TIME,System.currentTimeMillis());
        p.setProperty(PlatformFactory.CERT_DN,aip.getCertdn());
        p.setProperty(PlatformFactory.FQDN,aip.getFqdn());
        p.setProperty(PlatformFactory.COMMENT_TEXT,"");
        p.setProperty(PlatformFactory.CPU_COUNT,aip.getCpuCount());
        p.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
        p.setModifiedBy(initialOwner);
        ManagedResource managedResource =  new ManagedResource(p.getId(), agent);
        managedResourceRepository.save(managedResource);
        p.setOwner(subject.getName());
        resourceManager.findRootResource().relateTo(p, RelationshipTypes.PLATFORM);
        resourceManager.findRootResource().relateTo(p, RelationshipTypes.CONTAINS);
        return p;
    }
    
    private Resource create(AuthzSubject owner, PlatformValue pv, Agent agent, 
                              ResourceType type) 
    {
        Resource p = new Resource(pv.getName(), type);
        p.setDescription(pv.getDescription());
        p.setLocation(pv.getLocation());
        p.setModifiedBy(pv.getModifiedBy());
        p.setOwner(owner.getName());
        resourceDao.persist(p);
        p.setProperty(PlatformFactory.CERT_DN,pv.getCertdn());
        p.setProperty(PlatformFactory.COMMENT_TEXT,pv.getCommentText());
        p.setProperty(PlatformFactory.CPU_COUNT,pv.getCpuCount());
        p.setProperty(PlatformFactory.FQDN,pv.getFqdn());
        //TODO abstract creationTime, modifiedTime
        p.setProperty(PlatformFactory.CREATION_TIME, System.currentTimeMillis());
        p.setProperty(PlatformFactory.MODIFIED_TIME,System.currentTimeMillis());
        ManagedResource managedResource =  new ManagedResource(p.getId(), agent);
        managedResourceRepository.save(managedResource);
        for (IpValue ipv : pv.getAddedIpValues()) {
            addIp(toPlatform(p), ipv.getAddress(), ipv.getNetmask(), ipv.getMACAddress());
        }
        resourceManager.findRootResource().relateTo(p, RelationshipTypes.PLATFORM);
        resourceManager.findRootResource().relateTo(p, RelationshipTypes.CONTAINS);
        return p;
    }
  
    
    /**
     * Private method to validate a new PlatformValue object
     * 
     * @throws ValidationException
     */
    private void validateNewPlatform(PlatformValue pv) throws ValidationException {
        String msg = null;
        // first check if its new
        if (pv.idHasBeenSet()) {
            msg = "This platform is not new. It has id: " + pv.getId();
        }
        // else if(someotherthing) ...

        // Now check if there's a msg set and throw accordingly
        if (msg != null) {
            throw new ValidationException(msg);
        }
    }

    
    public Platform createPlatform(AuthzSubject subject, Integer platformTypeId,
                                   PlatformValue pValue, Integer agentPK)
        throws ValidationException, PermissionException, AppdefDuplicateNameException,
        AppdefDuplicateFQDNException, ApplicationException {
        // check if the object already exists

        if (resourceManager.findResourceByName(pValue.getName()) != null) {
            // duplicate found, throw a duplicate object exception
            throw new AppdefDuplicateNameException();
        }
        if (findByFQDN(pValue.getFqdn()) != null) {
            // duplicate found, throw a duplicate object exception
            throw new AppdefDuplicateFQDNException();
        }
        Agent agent = null;

            if (agentPK != null) {
                agent = agentDAO.findOne(agentPK);
                if(agent == null) {
                    throw new EntityNotFoundException("Agent with ID: " + 
                        agentPK + " was not found");
                }
            }

            trimStrings(pValue);
            getCounter().addCPUs(pValue.getCpuCount().intValue());
            validateNewPlatform(pValue);
            
            pValue.setOwner(subject.getName());
            pValue.setModifiedBy(subject.getName());

            // AUTHZ CHECK
            try {
                permissionManager.checkCreatePlatformPermission(subject);
            } catch (Exception e) {
                throw new SystemException(e);
            }
            ResourceType platType = resourceManager.findResourceTypeById(platformTypeId);
            Resource platform = create(subject,pValue, agent, platType);
            Platform plat = toPlatform(platform);

            // Send resource create event
            ResourceCreatedZevent zevent = new ResourceCreatedZevent(subject, plat.getEntityId());
            zeventManager.enqueueEventAfterCommit(zevent);

            return plat;
    }


    /**
     * Create a Platform from an AIPlatform
     * 
     * @param aipValue the AIPlatform to create as a regular appdef platform.
     * 
     */
    public Platform createPlatform(AuthzSubject subject, AIPlatformValue aipValue)
        throws ApplicationException, DuplicateObjectException {
        getCounter().addCPUs(aipValue.getCpuCount().intValue());

        ResourceType platType = resourceManager.findResourceTypeByName(aipValue.getPlatformTypeName());

        if (platType == null) {
            throw new SystemException("Unable to find PlatformType [" +
                                      aipValue.getPlatformTypeName() + "]");
        }

        Resource checkP = resourceManager.findResourceByName(aipValue.getName());
        if (checkP != null) {
            throw new DuplicateObjectException("Duplicate platform found " + "with name: " +
                aipValue.getName());
        }

        Agent agent = agentDAO.findByAgentToken(aipValue.getAgentToken());

        if (agent == null) {
            throw new ApplicationException("Unable to find agent: " + aipValue.getAgentToken());
        }
     
        
        // AUTHZ CHECK
        try {
            permissionManager.checkCreatePlatformPermission(subject);
        } catch (Exception e) {
            throw new SystemException(e);
        }

        Resource platform = create(subject,aipValue, subject.getName(),agent,platType);     
        

        // Send resource create event.  TODO abstract to ResourceManager when we don't need to use entity ID
        Platform plat = toPlatform(platform);
        ResourceCreatedZevent zevent = new ResourceCreatedZevent(subject, plat.getEntityId());
        zeventManager.enqueueEventAfterCommit(zevent);

        return plat;
    }

    /**
     * Get all platforms.
     * 
     * 
     * @param subject The subject trying to list platforms.
     * @param pc a PageControl object which determines the size of the page and
     *        the sorting, if any.
     * @return A List of PlatformValue objects representing all of the platforms
     *         that the given subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<PlatformValue> getAllPlatforms(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {

        Collection<Platform> platforms = getViewablePlatforms(subject, pc);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(platforms, pc);
    }
    
    public PageList<Resource> getAllPlatformResources(AuthzSubject subject, PageControl pc) {
        PageRequest pageInfo = new PageRequest(pc.getPagenum(),pc.getPagesize(),
            new Sort(pc.getSortorder() == PageControl.SORT_ASC? Direction.ASC : Direction.DESC,"sortName"));
        Page<Resource> resources = resourceDao.findByIndexedProperty(AppdefResourceType.APPDEF_TYPE_ID, 
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM,pageInfo,String.class);
        return new PageList<Resource>(resources.getContent(),(int)resources.getTotalElements());
    }
    
    public Set<Integer> getAllPlatformIds() {
        Set<Integer> platformIds = new HashSet<Integer>();
        Collection<Resource> platforms = getAllPlatforms();
        for(Resource platform:platforms) {
            platformIds.add(platform.getId());
        }
        return platformIds;
    }
    
    private Set<Resource> findByCreationTime(long creationTime) {
        //TODO this would be more performant with a JPA or Graph query,
        //but not sure yet if we need creationTime as a concept for all Resources
        Collection<Resource> platforms = getAllPlatforms();
        Set<Resource> platformsSince = new HashSet<Resource>();
        for(Resource platform: platforms) {
            if((Long)platform.getProperty(PlatformFactory.CREATION_TIME) > creationTime) {
                platformsSince.add(platform);
            }
        }
        return platformsSince;
    }

    /**
     * Get platforms created within a given time range.
     * 
     * 
     * @param subject The subject trying to list platforms.
     * @param range The range in milliseconds.
     * @param size The number of platforms to return.
     * @return A List of PlatformValue objects representing all of the platforms
     *         that the given subject is allowed to view that were created
     *         within the given range.
     */
    @Transactional(readOnly = true)
    public PageList<PlatformValue> getRecentPlatforms(AuthzSubject subject, long range, int size)
        throws PermissionException, NotFoundException {
        PageControl pc = new PageControl(0, size);

        Collection<Resource> platforms = findByCreationTime(System.currentTimeMillis() - range);
        Set<Platform> plats = new HashSet<Platform>();
       
        //TODO filter viewable
        for (Iterator<Resource> i = platforms.iterator(); i.hasNext();) {
            Resource platform = i.next();
            plats.add(toPlatform(platform));
        }

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(plats, pc);
    }

    /**
     * Get platform light value by id. Does not check permission.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Platform getPlatformById(AuthzSubject subject, Integer id)
        throws PlatformNotFoundException, PermissionException {
        Platform platform = findPlatformById(id);
        //TODO perm check
        //permissionManager.checkViewPermission(subject, platform.getId());
        return platform;
    }
    
    public Set<Platform> getPlatformsByAgent(Agent agent) {
        Set<Platform> platforms = new HashSet<Platform>();
        List<ManagedResource> managedResources = managedResourceRepository.findByAgent(agent);
        for(ManagedResource managedResource: managedResources) {
            Resource resource = resourceManager.findResourceById(managedResource.getResourceId());
            platforms.add(toPlatform(resource));
        }
        return platforms;
    }

    /**
     * Find a Platform by Id.
     * 
     * @param id The id to look up.
     * @return A Platform object representing this Platform.
     * @throws PlatformNotFoundException If the given Platform is not found.
     * 
     */
    @Transactional(readOnly = true)
    public Platform findPlatformById(Integer id) throws PlatformNotFoundException {
        Resource platform = resourceManager.findResourceById(id);
        if (platform == null) {
            throw new PlatformNotFoundException(id);
        }

        return toPlatform(platform);
    }
    
    private Collection<Resource> getAllPlatforms() {
        return resourceManager.findRootResource().getResourcesFrom(RelationshipTypes.PLATFORM);
        
    }
    
    private Resource findByFQDN(String fqdn) {
        Collection<Resource> platforms = getAllPlatforms();
        for(Resource platform: platforms) {
            if(fqdn.equals(platform.getProperty(PlatformFactory.FQDN))) {
                return platform;
            }
        }
        return null;
    }
    
    private Collection<Resource> findByIpAddr(String address) {
        Set<Resource> platforms = new HashSet<Resource>();
        for(Resource platform: getAllPlatforms()) {
            Set<Resource> ips = platform.getResourcesFrom(RelationshipTypes.IP);
            for(Resource ip:ips) {
                if(address.equals(ip.getProperty(PlatformFactory.IP_ADDRESS))) {
                    platforms.add(platform);
                }
            }
        }
        return platforms;
    }
    
    private Collection<Resource> getIps(Resource platform) {
        return platform.getResourcesFrom(RelationshipTypes.IP);
    }
    
    @Transactional(readOnly = true)
    public Platform findPlatformByAIPlatform(AuthzSubject subject, AIPlatformValue aiPlatform) 
        throws PermissionException, PlatformNotFoundException {
        Resource p =  findByFQDN(aiPlatform.getFqdn());
        if(p == null) {
            final AIIpValue[] ipvals = aiPlatform.getAIIpValues();
            // Find by IP address. For now, if we get one IP address
            // match (and it isn't localhost), we assume that it is
            // the same platform. In the future, we are probably going
            // to need to do better.
            for (int i = 0; i < ipvals.length; i++) {
                AIIpValue qip = ipvals[i];
    
                String address = qip.getAddress();
                // XXX This is a hack that we need to get rid of
                // at some point. The idea is simple. Every platform
                // has the localhost address. So, if we are looking
                // for a platform based on IP address, searching for
                // localhost doesn't give us any information. Long
                // term, when we are trying to match all addresses,
                // this can go away.
                if (address.equals(NetFlags.LOOPBACK_ADDRESS) && ipvals.length > 1) {
                    continue;
                }
    
                
                Collection<Resource> platforms = findByIpAddr(address);
    
                Set<Resource> platformsMatchingIp = new HashSet<Resource>();
                if (!platforms.isEmpty()) {               
                    for (Resource plat : platforms) {
                        // Make sure the types match
                        if (!plat.getType().getName().equals(
                            aiPlatform.getPlatformTypeName())) {
                            continue;
                        }
                        if (platformMatchesAllIps(plat, Arrays.asList(ipvals))) {
                            platformsMatchingIp.add(plat);
                        }
                    }
                    if(platformsMatchingIp.size() > 1) {
                        //This could happen in an agent porker situation, but shouldn't
                        log.warn("Found multiple existing platforms with IP address " + address + 
                            " matching platform in AI Queue, but no FQDN match.  Change to platform with FQDN: " + 
                                aiPlatform.getFqdn() + " may not be processed");
                    }else if(platformsMatchingIp.size() == 1) {
                        // If FQDN was not matched, but all IPs are
                        p = platformsMatchingIp.iterator().next();
                    } 
                }
            }
        }
        if (p == null) {
            p = getPhysPlatformByAgentToken(aiPlatform.getAgentToken());
        }
        //TODO perm check
        //if (p != null) {
          //  permissionManager.checkViewPermission(subject, p.getId());
        //}
        if(p == null) {
            throw new PlatformNotFoundException("platform not found for ai " + "platform: " +
                aiPlatform.getId());
        }
        return toPlatform(p);
    }

    /**
     * Get the Platform object based on an AIPlatformValue. Checks against FQDN,
     * CertDN, then checks to see if all IP addresses match. If all of these
     * checks fail null is returned.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Platform getPlatformByAIPlatform(AuthzSubject subject, AIPlatformValue aiPlatform)
        throws PermissionException {
        Resource p = null;

        String fqdn = aiPlatform.getFqdn();
        String certdn = aiPlatform.getCertdn();

        final AIIpValue[] ipvals = aiPlatform.getAIIpValues();
        boolean porker = isAgentPorker(Arrays.asList(ipvals));
        if (! porker) {
            // We can't use the FQDN to find a platform, because
            // the FQDN can change too easily. Instead we use the
            // IP address now. For now, if we get one IP address
            // match (and it isn't localhost), we assume that it is
            // the same platform. In the future, we are probably going
            // to need to do better.
            for (int i = 0; i < ipvals.length; i++) {
                AIIpValue qip = ipvals[i];

                String address = qip.getAddress();
                // XXX This is a hack that we need to get rid of
                // at some point. The idea is simple. Every platform
                // has the localhost address. So, if we are looking
                // for a platform based on IP address, searching for
                // localhost doesn't give us any information. Long
                // term, when we are trying to match all addresses,
                // this can go away.
                if (address.equals(NetFlags.LOOPBACK_ADDRESS) && i < (ipvals.length - 1)) {
                    continue;
                }

                Collection<Resource> platforms = findByIpAddr(address);

                if (!platforms.isEmpty()) {
                    Resource ipMatch = null;

                    for (Resource plat : platforms) {

                        // Make sure the types match
                        if (!plat.getType().getName().equals(
                            aiPlatform.getPlatformTypeName())) {
                            continue;
                        }

                        // If we got any platforms that match this IP address,
                        // then
                        // we just take it and see if we can match up more
                        // criteria.
                        // We can assume that is a candidate for the platform we
                        // are
                        // looking for. This should only fall apart if we have
                        // multiple platforms defined for the same IP address,
                        // which
                        // should be a rarity.

                        if (plat.getProperty(PlatformFactory.FQDN).equals(fqdn)) { // Perfect
                            p = plat;
                            break;
                        }

                        // FQDN changed
                        if (platformMatchesAllIps(plat, Arrays.asList(ipvals))) {
                            ipMatch = plat;
                        }
                    }

                    // If FQDN was not matched, but all IPs are
                    p = (p == null) ? ipMatch : p;
                }

                // Found a match
                if (p != null) {
                    break;
                }
            }
        }

        // One more try
        if (p == null) {
            p = findByFQDN(fqdn);
        }

        String agentToken = aiPlatform.getAgentToken();
        if (p == null) {
            p = getPhysPlatformByAgentToken(agentToken);
        }

        if (p != null) {
            //TODO perm check
            //permissionManager.checkViewPermission(subject, p.getId());
            if (porker && // Let agent porker
                // create new platforms
                !(p.getProperty(PlatformFactory.FQDN).equals(fqdn) || p.getProperty(PlatformFactory.CERT_DN).equals(certdn) || 
                    managedResourceRepository.findAgentByResource(p.getId()).getAgentToken().equals(agentToken))) {
                p = null;
            }
        }
        if(p == null) {
            return null;
        }

        return toPlatform(p);
    }

    /**
     * @return the {@link Platform} associated with the
     *         agentToken or null if one does not exist.
     * 
     */
    private Resource getPhysPlatformByAgentToken(String agentToken) {
        try {
            Agent agent = agentManager.getAgent(agentToken);
            Collection<Resource> platforms = getAllPlatforms();
            for (Resource platform : platforms) {
                if(agent.equals(managedResourceRepository.findAgentByResource(platform.getId()))) {
                    String platType = platform.getType().getName();
                    // need to check if the platform is not a platform device
                    if (PlatformDetector.isSupportedPlatform(platType)) {
                        return platform;
                    }
                }
            }
        }catch(AgentNotFoundException e) {
            return null;
        }
        return null;
    }

    private boolean isAgentPorker(List<AIIpValue> ips) {
        // anytime a new agent comes in (ip / port being unique) it creates a
        // new object mapping in the db. Therefore if the agent is found but
        // with no associated platform, we need to check the ips in the
        // agent table. If there are more than one IPs match,
        // then assume this is the Agent Porker

        for (AIIpValue ip : ips) {

            if (ip.getAddress().equals(NetFlags.LOOPBACK_ADDRESS)) {
                continue;
            }
            List<Agent> agents = agentManager.findAgentsByIP(ip.getAddress());
            if (agents.size() > 1) {
                return true;
            }
        }
        return false;
        
    }

    private boolean platformMatchesAllIps(Resource p, List<AIIpValue> ips) {
        Collection<Resource> platIps = getIps(p);
        if (platIps.size() != ips.size()) {
            return false;
        }
        Set<String> ipSet = new HashSet<String>();
        for (AIIpValue ip : ips) {

            ipSet.add(ip.getAddress());
        }
        for (Resource ip : platIps) {
            if (!ipSet.contains(ip.getProperty(PlatformFactory.IP_ADDRESS))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the Platform that has the specified Fqdn
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Platform findPlatformByFqdn(AuthzSubject subject, String fqdn)
        throws PlatformNotFoundException, PermissionException {
        Resource p = findByFQDN(fqdn);
        if (p == null) {
            throw new PlatformNotFoundException("Platform with fqdn " + fqdn + " not found");
        }
        // TODO perm check
        //permissionManager.checkViewPermission(subject, p.getId());
        return toPlatform(p);
    }

    /**
     * Get the platform by agent token
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Integer> getPlatformPksByAgentToken(AuthzSubject subject, String agentToken)
        throws PlatformNotFoundException {
        Collection<Resource> platforms = findByAgentToken(agentToken);
        if (platforms == null || platforms.size() == 0) {
            throw new PlatformNotFoundException("Platform with agent token " + agentToken +
                                                " not found");
        }

        Set<Integer> pks = new HashSet<Integer>();
        for (Resource plat : platforms) {
            pks.add(plat.getId());
        }
        return pks;
    }
    
    private Collection<Resource> findByAgentToken(String agentToken) {
        Set<Resource> agentPlatforms = new HashSet<Resource>();
        Collection<Resource> platforms = getAllPlatforms();
        for (Resource platform : platforms) {
            if(agentToken.equals(managedResourceRepository.findAgentByResource(platform.getId()).getAgentToken())) {
                agentPlatforms.add(platform);
            }
        }
        return agentPlatforms;
    }


    /**
     * Get the platform that hosts the server that provides the specified
     * service.
     * 
     * 
     * @param subject The subject trying to list services.
     * @param serviceId service ID.
     * @return the Platform
     */
    @Transactional(readOnly = true)
    public PlatformValue getPlatformByService(AuthzSubject subject, Integer serviceId)
        throws PlatformNotFoundException, PermissionException {
        Resource p = findByServiceId(serviceId);
        if (p == null) {
            throw new PlatformNotFoundException("platform for service " + serviceId + " not found");
        }
        // TODO perm check
        //permissionManager.checkViewPermission(subject, p.getId());
        return toPlatform(p).getPlatformValue();
    }
    
    private Resource findByServiceId(Integer serviceId) {
        Resource service = resourceManager.findResourceById(serviceId);
        if(service == null) {
            return null;
        }
        Resource parent = service.getResourceTo(RelationshipTypes.SERVICE);
        Resource grandparent = parent.getResourceTo(RelationshipTypes.SERVER);
        if( grandparent != null) {
            return grandparent;
        }
        return parent;
    }
    
    private List<Resource> findByServers(Integer[] ids) {
        List<Resource> platforms = new ArrayList<Resource>();
        for(Integer id: ids) {
            Resource server = resourceManager.findResourceById(id);
            platforms.add(server.getResourceTo(RelationshipTypes.SERVER));
        }
        Collections.sort(platforms, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                return o1.getSortName().compareTo(o2.getSortName());
            }
        });
        return platforms;
    }

    /**
     * Get the platform ID that hosts the server that provides the specified
     * service.
     * 
     * 
     * @param serviceId service ID.
     * @return the Platform
     */
    @Transactional(readOnly = true)
    public Integer getPlatformIdByService(Integer serviceId) throws PlatformNotFoundException {
        Resource p = findByServiceId(serviceId);
        if (p == null) {
            throw new PlatformNotFoundException("platform for service " + serviceId + " not found");
        }
        return p.getId();
    }

    /**
     * Get the platform for a server.
     * 
     * 
     * @param subject The subject trying to list services.
     * @param serverId Server ID.
     */
    @Transactional(readOnly = true)
    public PlatformValue getPlatformByServer(AuthzSubject subject, Integer serverId)
        throws PlatformNotFoundException, PermissionException {
        Resource server = resourceManager.findResourceById(serverId);

        if (server == null || server.getResourceTo(RelationshipTypes.SERVER) == null) {
            // This should throw server not found. Servers always have
            // platforms..
            throw new PlatformNotFoundException("platform for server " + serverId + " not found");
        }

        Resource p = server.getResourceTo(RelationshipTypes.SERVER);
        //TODO perm check
        //permissionManager.checkViewPermission(subject, p.getId());
        return toPlatform(p).getPlatformValue();
    }

    /**
     * Get the platform ID for a server.
     * 
     * 
     * @param serverId Server ID.
     */
    @Transactional(readOnly = true)
    public Integer getPlatformIdByServer(Integer serverId) throws PlatformNotFoundException {
        Resource server = resourceManager.findResourceById(serverId);

        if (server == null)
            throw new PlatformNotFoundException("platform for server " + serverId + " not found");

        return server.getResourceTo(RelationshipTypes.SERVER).getId();
    }

    /**
     * Get the platforms for a list of servers.
     * 
     * 
     * @param subject The subject trying to list services.
     */
    @Transactional(readOnly = true)
    public PageList<PlatformValue> getPlatformsByServers(AuthzSubject subject,
                                                         List<AppdefEntityID> sIDs)
        throws PlatformNotFoundException, PermissionException {
       

        Integer[] ids = new Integer[sIDs.size()];
        int i = 0;
        for (Iterator<AppdefEntityID> it = sIDs.iterator(); it.hasNext(); i++) {
            AppdefEntityID svrId = it.next();
            ids[i] = svrId.getId();
        }

        List<Resource> foundPlats = findByServers(ids);
        //TODO filter platforms by viewable
        ArrayList<Platform> platforms = new ArrayList<Platform>();
        for (Resource platform : foundPlats) {
            platforms.add(toPlatform(platform));
        }
        return valuePager.seek(platforms, null);
    }

    /**
     * Get all platforms by application.
     * 
     * 
     * 
     * @param subject The subject trying to list services.
     * @param appId Application ID. but when they are, they should live
     *        somewhere in appdef/shared so that clients can use them too.
     * @return A List of ApplicationValue objects representing all of the
     *         services that the given subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<PlatformValue> getPlatformsByApplication(AuthzSubject subject, Integer appId,
                                                             PageControl pc)
        throws ApplicationNotFoundException, PlatformNotFoundException, PermissionException {

        ResourceGroup appLocal = resourceGroupManager.findResourceGroupById(appId);
        if (appLocal == null) {
            throw new ApplicationNotFoundException(appId);
        }

        Collection<PlatformValue> platCollection = new ArrayList<PlatformValue>();
        // XXX Call to authz, get the collection of all services
        // that we are allowed to see.
        // OR, alternatively, find everything, and then call out
        // to authz in batches to find out which ones we are
        // allowed to return.

        Collection<Resource> serviceCollection = appLocal.getMembers();
        Iterator<Resource> it = serviceCollection.iterator();
        while (it != null && it.hasNext()) {
            Resource appService = it.next();
            Integer serviceId = appService.getId();
            PlatformValue pValue = getPlatformByService(subject, serviceId);
            // Fold duplicate platforms
            if (!platCollection.contains(pValue)) {
                platCollection.add(pValue);
            }
        }

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(platCollection, pc);
    }
    
    @Transactional(readOnly = true)
    public Integer[] getPlatformIds(AuthzSubject subject, Integer platTypeId)
        throws PermissionException {
        //TODO this was never guaranteed to be ordered, should return a Collection
        Collection<Resource> platforms = resourceManager.findResourceTypeById(platTypeId).getResources();
        Collection<Integer> platIds = new ArrayList<Integer>();

        // TODO filter viewable platforms
        for (Resource platform : platforms) {
            platIds.add(platform.getId());
        }

        return platIds.toArray(new Integer[0]);
    }
    
    private Collection<Resource> findAllOrderName(boolean asc) {
        List<Resource> platforms = new ArrayList<Resource>(getAllPlatforms());
        if(asc) {
            Collections.sort(platforms, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        }else {
            Collections.sort(platforms, new Comparator<Resource>() {
                public int compare(Resource o1, Resource o2) {
                    return o2.getName().compareTo(o1.getName());
                } 
        });}
        return platforms;
    }
    
    private Collection<Resource> findAllOrderCTime(boolean asc) {
        List<Resource> platforms = new ArrayList<Resource>(getAllPlatforms());
        if(asc) {
            Collections.sort(platforms, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                return ((Long)o1.getProperty(PlatformFactory.CREATION_TIME)).compareTo((Long)o2.getProperty(PlatformFactory.CREATION_TIME));
            }
        });
        }else {
            Collections.sort(platforms, new Comparator<Resource>() {
                public int compare(Resource o1, Resource o2) {
                    return ((Long)o2.getProperty(PlatformFactory.CREATION_TIME)).compareTo((Long)o1.getProperty(PlatformFactory.CREATION_TIME));
                } 
        });}
        return platforms;
    }

   
    /**
     * Get the scope of viewable platforms for a given user
     * @param whoami - the user
     * @return List of PlatformLocals for which subject has
     *         AuthzConstants.platformOpViewPlatform XXX scottmf, this needs to
     *         be completely rewritten. It should not query all the platforms
     *         and mash that list together with the viewable resources. This
     *         will potentially bloat the session with useless pojos, not to
     *         mention the poor performance implications. Instead it should get
     *         the viewable resources then select those platform where id in
     *         (:pids) OR look them up from cache.
     */
    private Collection<Platform> getViewablePlatforms(AuthzSubject whoami, PageControl pc)
        throws PermissionException, NotFoundException {
        // first find all, based on the sorting attribute passed in, or
        // with no sorting if the page control is null
        Collection<Resource> platforms;
        // if page control is null, find all platforms
        if (pc == null) {
            platforms = getAllPlatforms();
        } else {
            pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
            int attr = pc.getSortattribute();
            switch (attr) {
                case SortAttribute.RESOURCE_NAME:
                    platforms = findAllOrderName(pc.isAscending());
                    break;
                case SortAttribute.CTIME:
                    platforms = findAllOrderCTime(pc.isAscending());
                    break;
                default:
                    throw new NotFoundException("Invalid sort attribute: " + attr);
            }
        }
        // TODO filter by viewable
        Set<Platform> viewablePlatforms = new HashSet<Platform>();
        for (Resource platform : platforms) {
            viewablePlatforms.add(toPlatform(platform));
        }
        return viewablePlatforms;
    }
    
   


    /**
     * Update an existing Platform. Requires all Ip's to have been re-added via
     * the platformValue.addIpValue(IpValue) method due to bug 4924
     * 
     * @param existing - the value object for the platform you want to save
     * 
     */
    private Platform updatePlatformImpl(AuthzSubject subject, PlatformValue existing)
        throws UpdateException, PermissionException, AppdefDuplicateNameException,
        PlatformNotFoundException, AppdefDuplicateFQDNException, ApplicationException {
        //TODO perm check
        //permissionManager.checkPermission(subject, existing.getEntityId(),
          //  AuthzConstants.platformOpModifyPlatform);
        existing.setModifiedBy(subject.getName());
        existing.setMTime(new Long(System.currentTimeMillis()));
        trimStrings(existing);

        Resource plat = resourceManager.findResourceById(existing.getId());
        Platform platform = toPlatform(plat);
        if (existing.getCpuCount() == null) {
            // cpu count is no longer an option in the UI
            existing.setCpuCount((Integer)plat.getProperty(PlatformFactory.CPU_COUNT));
        }

        if (platform.matchesValueObject(existing)) {
            log.debug("No changes found between value object and entity");
            return platform;
        } else {
            int newCount = existing.getCpuCount().intValue();
            int prevCpuCount = ((Integer)plat.getProperty(PlatformFactory.CPU_COUNT)).intValue();
            if (newCount > prevCpuCount) {
                getCounter().addCPUs(newCount - prevCpuCount);
            }

            if (!(existing.getName().equals(plat.getName()))) {
                if (resourceManager.findResourceByName(existing.getName()) != null)
                    // duplicate found, throw a duplicate object exception
                    throw new AppdefDuplicateNameException();
            }

            if (!(existing.getFqdn().equals(plat.getProperty(PlatformFactory.FQDN)))) {
                if (findByFQDN(existing.getFqdn()) != null)
                    // duplicate found, throw a duplicate object exception
                    throw new AppdefDuplicateFQDNException();
            }

            // See if we need to create an AIPlatform
            if (existing.getAgent() != null) {
                if (managedResourceRepository.findAgentByResource(plat.getId()) == null) {
                    // Create AIPlatform for manually created platform

                    AIPlatformValue aiPlatform = new AIPlatformValue();
                    aiPlatform.setFqdn(existing.getFqdn());
                    aiPlatform.setName(existing.getName());
                    aiPlatform.setDescription(existing.getDescription());
                    aiPlatform.setPlatformTypeName(existing.getPlatformType().getName());
                    aiPlatform.setCertdn(existing.getCertdn());
                    aiPlatform.setAgentToken(existing.getAgent().getAgentToken());

                    IpValue[] ipVals = existing.getIpValues();
                    for (int i = 0; i < ipVals.length; i++) {
                        AIIpValue aiIpVal = new AIIpValue();
                        aiIpVal.setAddress(ipVals[i].getAddress());
                        aiIpVal.setNetmask(ipVals[i].getNetmask());
                        aiIpVal.setMACAddress(ipVals[i].getMACAddress());
                        aiPlatform.addAIIpValue(aiIpVal);
                    }

                    getAIQueueManager().queue(subject, aiPlatform, false, false, true);
                } else if (!managedResourceRepository.findAgentByResource(plat.getId()).equals(existing.getAgent())) {
                    // Need to enqueue the ResourceUpdatedZevent if the
                    // agent changed to get the metrics scheduled
                    List<ResourceUpdatedZevent> events = new ArrayList<ResourceUpdatedZevent>();
                    events.add(new ResourceUpdatedZevent(subject, platform.getEntityId()));
                    for (Resource svr : plat.getResourcesFrom(RelationshipTypes.SERVER)) {

                        events.add(new ResourceUpdatedZevent(subject, AppdefEntityID.newServerID(svr.getId())));

                        for (Resource svc : svr.getResourcesFrom(RelationshipTypes.SERVICE)) {

                            events.add(new ResourceUpdatedZevent(subject, AppdefEntityID.newServiceID(svc.getId())));
                        }
                    }

                    zeventManager.enqueueEventsAfterCommit(events);
                }
            }
            updatePlatform(plat, existing);
            return platform;
        }
    }
    
    private void removeAIp(Collection<Resource> coll, IpValue ipv) {
        for (Iterator<Resource> i = coll.iterator(); i.hasNext();) {
            Resource ip = i.next();
            if (ip.getId().equals(ipv.getId())) {
                i.remove();
                ip.remove();
                return;
            }
        }
    }
    
    private void updateAIp(Collection<Resource> coll, IpValue ipv) {
        for (Iterator<Resource> i = coll.iterator(); i.hasNext();) {
            Resource ip = i.next();
            if (ip.getId().equals(ipv.getId())) {
                updateIp(ip,ipv.getNetmask(),ipv.getMACAddress());
                return;
            }
        }
    }
    
    private void updatePlatform(Resource platform, PlatformValue existing) {
        // retrieve current list of ips
        Collection<Resource> curips = getIps(platform);
      
        // first remove any which were in the removedIp collection
        for ( IpValue aIp  : existing.getRemovedIpValues()) {
            if (aIp.idHasBeenSet()) {
                removeAIp(curips, aIp);
            }
        }
        Collection<IpValue> ips = existing.getAddedIpValues();
        // now get any ips which were in the ipValues array
        for (int i = 0; i < existing.getIpValues().length; i++) {
            IpValue aIp = existing.getIpValues()[i];
            if (!(ips.contains(aIp))) {
                ips.add(aIp);
            }
        }
        for (IpValue aIp : ips) {
            if (aIp.idHasBeenSet()) {
                updateAIp(curips, aIp);
            } else {
                // looks like its a new one
                addIp(toPlatform(platform), aIp.getAddress(), aIp.getNetmask(), aIp.getMACAddress());
            }
        }
        // finally update the platform
        platform.setDescription(existing.getDescription());
        platform.setProperty(PlatformFactory.COMMENT_TEXT,existing.getCommentText());
        platform.setModifiedBy(existing.getModifiedBy());
        platform.setLocation(existing.getLocation());
        platform.setProperty(PlatformFactory.CPU_COUNT,existing.getCpuCount());
        platform.setProperty(PlatformFactory.CERT_DN,existing.getCertdn());
        platform.setProperty(PlatformFactory.FQDN,existing.getFqdn());
        platform.setName(existing.getName());
        
        // if there is a agent
        if (existing.getAgent() != null) {
            // get the agent token and set the agent to the platform
            if(managedResourceRepository.findAgentByResource(platform.getId()) == null) {
                ManagedResource managedResource =  new ManagedResource(platform.getId(), existing.getAgent());
                managedResourceRepository.save(managedResource);
            }
        }
    }

    /**
     * Update an existing Platform. Requires all Ip's to have been re-added via
     * the platformValue.addIpValue(IpValue) method due to bug 4924
     * 
     * @param existing - the value object for the platform you want to save
     * 
     */
    public Platform updatePlatform(AuthzSubject subject, PlatformValue existing)
        throws UpdateException, PermissionException, AppdefDuplicateNameException,
        PlatformNotFoundException, AppdefDuplicateFQDNException, ApplicationException {
        return updatePlatformImpl(subject, existing);
    }

   
    private void deletePlatformType(ResourceType pt) throws VetoException {
        // Need to remove all server types, too
        for (ResourceType st : pt.getResourceTypesFrom(RelationshipTypes.SERVER)) {
            st.remove();
        }
        pt.remove();
    }
    

    /**
     * Update platform types
     * 
     * 
     */
    public void updatePlatformTypes(Plugin plugin, PlatformTypeInfo[] infos) throws VetoException,
        NotFoundException {
        // First, put all of the infos into a Hash
        HashMap<String, PlatformTypeInfo> infoMap = new HashMap<String, PlatformTypeInfo>();
        for (int i = 0; i < infos.length; i++) {
            infoMap.put(infos[i].getName(), infos[i]);
        }
        
        Collection<ResourceType> platformTypes = findAllPlatformResourceTypes();
        Set<ResourceType> curPlatforms = new HashSet<ResourceType>();
        for(ResourceType curResourceType: platformTypes) {
            if(pluginResourceTypeRepository.findNameByResourceType(curResourceType.getId()).equals(plugin)) {
                curPlatforms.add(curResourceType);
            }
        }
        
        for (ResourceType ptlocal : curPlatforms) {

            String localName = ptlocal.getName();
            PlatformTypeInfo pinfo = (PlatformTypeInfo) infoMap.remove(localName);

            // See if this exists
            if (pinfo == null) {
                deletePlatformType(ptlocal);
            } 
        }

        // Now create the left-overs
        for (PlatformTypeInfo pinfo : infoMap.values()) {
            createPlatformType(pinfo.getName(), plugin);
        }
    }

    public PlatformType createPlatformType(String name, Plugin plugin) throws NotFoundException {
        log.debug("Creating new PlatformType: " + name);
        ResourceType pt = new ResourceType(name);
        resourceTypeDao.persist(pt);
        PluginResourceType pluginResourceType = new PluginResourceType(plugin.getName(),pt.getId());
        pluginResourceTypeRepository.save(pluginResourceType);
        Set<PropertyType> propertyTypes = new HashSet<PropertyType>();
        propertyTypes.add(createPropertyType(PlatformFactory.CERT_DN,String.class));
        propertyTypes.add(createPropertyType(PlatformFactory.FQDN,String.class));
        propertyTypes.add(createPropertyType(PlatformFactory.COMMENT_TEXT,String.class));
        propertyTypes.add(createPropertyType(PlatformFactory.CPU_COUNT,Integer.class));
        propertyTypes.add(createPropertyType(PlatformFactory.CREATION_TIME,Long.class));
        propertyTypes.add(createPropertyType(PlatformFactory.MODIFIED_TIME,Long.class));
        PropertyType appdefType = createPropertyType(AppdefResourceType.APPDEF_TYPE_ID, Integer.class);
        appdefType.setIndexed(true);
        propertyTypes.add(appdefType);
        pt.addPropertyTypes(propertyTypes);
        resourceManager.findRootResourceType().relateTo(pt, RelationshipTypes.PLATFORM);
        pt.relateTo(resourceManager.findResourceTypeByName(IP_RESOURCE_TYPE_NAME),RelationshipTypes.IP);
        return platformFactory.createPlatformType(pt);
    }
    
    private PropertyType createPropertyType(String propName, Class<?> type) {
        PropertyType propType = new PropertyType(propName,type);
        propType.setDescription(propName);
        propType.setHidden(true);
        return propType;
    }
    
    private void updateWithAI(Platform platform, AIPlatformValue aiplatform,String owner) {
        Resource resource = resourceManager.findResourceById(platform.getId());
        if (aiplatform.getName() != null
            && !aiplatform.getName().equals(platform.getName())) {
            resource.setName(aiplatform.getName());
                 // if the fqdn and the name are currently equal 
                    // but only fqdn is changing,
                  // then the name should change as well
         } else if (!platform.getFqdn().equals(aiplatform.getFqdn())
                   && platform.getName().equals(platform.getFqdn())) {
             resource.setName(aiplatform.getFqdn());
         }
         resource.setProperty(PlatformFactory.CERT_DN,aiplatform.getCertdn());
         resource.setProperty(PlatformFactory.FQDN,aiplatform.getFqdn());
         resource.setModifiedBy(owner);
         resource.setProperty(PlatformFactory.CPU_COUNT,aiplatform.getCpuCount());
         resource.setDescription(aiplatform.getDescription());
    }

    /**
     * Update an existing appdef platform with data from an AI platform.
     * 
     * @param aiplatform the AI platform object to use for data
     * 
     */
    public void updateWithAI(AIPlatformValue aiplatform, AuthzSubject subj)
        throws PlatformNotFoundException, ApplicationException {

        String certdn = aiplatform.getCertdn();
        String fqdn = aiplatform.getFqdn();

        Platform platform = this.getPlatformByAIPlatform(subj, aiplatform);
        if (platform == null) {
            throw new PlatformNotFoundException("Platform not found with either FQDN: " + fqdn +
                                                " nor CertDN: " + certdn);
        }
        int prevCpuCount = platform.getCpuCount();
        Integer count = aiplatform.getCpuCount();
        if ((count != null) && (count.intValue() > prevCpuCount)) {
            getCounter().addCPUs(aiplatform.getCpuCount().intValue() - prevCpuCount);
        }

        // Get the FQDN before we update
        String prevFqdn = platform.getFqdn();

        updateWithAI(platform,aiplatform, subj.getName());

        // If FQDN has changed, we need to update servers' auto-inventory tokens
        if (!prevFqdn.equals(platform.getFqdn())) {
            for (Server server : platform.getServers()) {

                if (server.getAutoinventoryIdentifier().startsWith(prevFqdn)) {
                    String newAID = server.getAutoinventoryIdentifier().replace(prevFqdn, fqdn);
                    server.setAutoinventoryIdentifier(newAID);
                    serverManager.updateServer(subj, server.getServerValue());
                }
            }
        }

        // need to check if IPs have changed, if so update Agent
        updateAgentIps(subj, aiplatform, resourceManager.findResourceById(platform.getId()));

    }

    private void updateAgentIps(AuthzSubject subj, AIPlatformValue aiplatform, Resource platform) {
        List<AIIpValue> ips = Arrays.asList(aiplatform.getAIIpValues());

        Agent agent;
        try {
            // make sure we have the current agent that exists on the platform
            // and associate it
            agent = agentManager.getAgent(aiplatform.getAgentToken());
            if(managedResourceRepository.findAgentByResource(platform.getId()) == null) {
                ManagedResource managedResource =  new ManagedResource(platform.getId(), agent);
                managedResourceRepository.save(managedResource);
            }
        } catch (AgentNotFoundException e) {
            // the agent should exist at this point even if it is a new agent.
            // something failed at another stage of this process
            throw new SystemException(e);
        }
        boolean changeAgentIp = false;

        for (AIIpValue ip : ips) {

            if (ip.getQueueStatus() == AIQueueConstants.Q_STATUS_REMOVED &&
                agent.getAddress().equals(ip.getAddress())) {
                changeAgentIp = true;
            }
        }
        // Keep in mind that a unidirectional agent address
        // doesn't matter since the communication is always
        // from agent to server
        if (changeAgentIp && !agent.isUnidirectional()) {
            String origIp = agent.getAddress();
            // In a perfect world this loop would key on platform.getIps() but
            // then there are other issues with verifying that the agent is up
            // before approving since platform IPs are updated later in the
            // code flow. Since ips contains new and current IP addresses
            // of the platform it works.
            for (AIIpValue ip : ips) {
                if (ip.getQueueStatus() != AIQueueConstants.Q_STATUS_ADDED &&
                // Q_STATUS_PLACEHOLDER in this context simply means that
                    // the ip is already associated with the platform and there
                    // is no change. Therefore it needs to be part of our checks
                    ip.getQueueStatus() != AIQueueConstants.Q_STATUS_PLACEHOLDER) {
                    continue;
                }
                try {
                    // if ping succeeds then this address is ok to use
                    // for the agent ip address.
                    // This logic is based on a conversation that
                    // scottmf had with chip 9/17/2009.
                    agent.setAddress(ip.getAddress());
                    agentManager.pingAgent(subj, agent);
                    log.info("updating ip for agentId=" + agent.getId() + " and platformid=" +
                             platform.getId() + " from ip=" + origIp + " to ip=" + ip.getAddress());
                    if(managedResourceRepository.findAgentByResource(platform.getId()) == null) {
                        ManagedResource managedResource =  new ManagedResource(platform.getId(), agent);
                        managedResourceRepository.save(managedResource);
                    }
                    enableMeasurements(subj, platform);
                    break;
                } catch (AgentConnectionException e) {
                    // if the agent connection fails then continue
                } catch (Exception e) {
                    // something is wrong internally, log the error but continue
                    log.error(e, e);
                }
                // make sure address does not change if/when last ping fails
                agent.setAddress(origIp);
            }
            if (managedResourceRepository.findAgentByResource(platform.getId()) == null) {
                log.warn("Removing agent reference from platformid=" + platform.getId() +
                         ".  Server cannot ping the agent from any IP " +
                         "associated with the platform");
            }
        }

    }
    
    private Collection<Resource> getServers(Resource platform) {
        return platform.getResourcesFrom(RelationshipTypes.SERVER);
    }
    
    private Collection<Resource> getServices(Resource server) {
        return server.getResourcesFrom(RelationshipTypes.SERVICE);
    }


    private void enableMeasurements(AuthzSubject subj, Resource platform) {
        List<Integer> eids = new ArrayList<Integer>();
        eids.add(platform.getId());
        Collection<Resource> servers = getServers(platform);
        for (Resource server : servers) {

            eids.add(server.getId());
            Collection<Resource> services = getServices(server);
            for (Resource service : services) {

                eids.add(service.getId());
            }
        }
        AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(eids);
        zeventManager.enqueueEventAfterCommit(event);
    }

    /**
     * Used to trim all string based attributes present in a platform value
     * object
     */
    private void trimStrings(PlatformValue plat) {
        if (plat.getName() != null) {
            plat.setName(plat.getName().trim());
        }
        if (plat.getCertdn() != null) {
            plat.setCertdn(plat.getCertdn().trim());
        }
        if (plat.getCommentText() != null) {
            plat.setCommentText(plat.getCommentText().trim());
        }
        if (plat.getDescription() != null) {
            plat.setDescription(plat.getDescription().trim());
        }
        if (plat.getFqdn() != null) {
            plat.setFqdn(plat.getFqdn().trim());
        }
        // now the Ips
        for (IpValue ip : plat.getAddedIpValues()) {

            if (ip.getAddress() != null) {
                ip.setAddress(ip.getAddress().trim());
            }
            if (ip.getMACAddress() != null) {
                ip.setMACAddress(ip.getMACAddress().trim());
            }
            if (ip.getNetmask() != null) {
                ip.setNetmask(ip.getNetmask().trim());
            }
        }
        // and the saved ones in case this is an update
        for (int i = 0; i < plat.getIpValues().length; i++) {
            IpValue ip = plat.getIpValues()[i];
            if (ip.getAddress() != null) {
                ip.setAddress(ip.getAddress().trim());
            }
            if (ip.getMACAddress() != null) {
                ip.setMACAddress(ip.getMACAddress().trim());
            }
            if (ip.getNetmask() != null) {
                ip.setNetmask(ip.getNetmask().trim());
            }
        }
    }
    
   

    /**
     * Add an IP to a platform
     * 
     * 
     */
    public Ip addIp(Platform platform, String address, String netmask, String macAddress) {
        //IP unique by Platform and address
        Resource ip = new Resource(platform.getName() + '.'+ address, 
            resourceManager.findResourceTypeByName(IP_RESOURCE_TYPE_NAME));
        resourceDao.persist(ip);
        ip.setProperty(PlatformFactory.IP_ADDRESS,address);
        ip.setProperty(PlatformFactory.NETMASK,netmask);
        ip.setProperty(PlatformFactory.MAC_ADDRESS,macAddress);
        ip.setProperty(PlatformFactory.CREATION_TIME, System.currentTimeMillis());
        ip.setProperty(PlatformFactory.MODIFIED_TIME,System.currentTimeMillis());
        resourceManager.findResourceById(platform.getId()).relateTo(ip, RelationshipTypes.IP);
        return platformFactory.createIp(ip);  
    }

    /**
     * Update an IP on a platform
     * 
     * 
     */
    public Ip updateIp(Platform platform, String address, String netmask, String macAddress) {
        Resource resource = resourceManager.findResourceById(platform.getId());
        Collection<Resource> ips = resource.getResourcesFrom(RelationshipTypes.IP);
        for(Resource ip: ips) {
            if(ip.getProperty(PlatformFactory.IP_ADDRESS).equals(address)) {
                updateIp(ip,netmask,macAddress);
                return platformFactory.createIp(ip);
            }
        }
        return null;
    }
    
    private void updateIp(Resource ip, String netmask, String macAddress) {
        ip.setProperty(PlatformFactory.NETMASK,netmask);
        ip.setProperty(PlatformFactory.MAC_ADDRESS,macAddress);
    }

    /**
     * Remove an IP on a platform
     * 
     * 
     */
    public void removeIp(Platform platform, String address, String netmask, String macAddress) {
        Resource resource = resourceManager.findResourceById(platform.getId());
        Collection<Resource> ips = resource.getResourcesFrom(RelationshipTypes.IP);
        for(Resource ip: ips) {
            if(ip.getProperty(PlatformFactory.IP_ADDRESS).equals(address) && ip.getProperty(PlatformFactory.NETMASK).equals(netmask) && 
                ip.getProperty(PlatformFactory.MAC_ADDRESS).equals(macAddress)) {
                ip.remove();
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String,Integer> getPlatformTypeCounts() {
        Collection<ResourceType> platformTypes = findAllPlatformResourceTypes();
        List<ResourceType> orderedPlatformTypes =  new ArrayList<ResourceType>(platformTypes);
        Collections.sort(orderedPlatformTypes, new Comparator<ResourceType>() {
            public int compare(ResourceType o1, ResourceType o2) {
                return (o1.getName().compareTo(o2.getName()));
            }
        });
        //TODO do we really need to order the Map?
        Map<String,Integer> counts = new LinkedHashMap<String,Integer>();
        for(ResourceType platformType: orderedPlatformTypes) {
            counts.put(platformType.getName(),platformType.getResources().size());
        }
        return counts;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Number getPlatformCount() {
        return getAllPlatforms().size();
    }
    
    public Collection<Platform> getPlatformsByType(AuthzSubject subject, String platformTypeName) throws PermissionException, InvalidAppdefTypeException {
        ResourceType ptype = resourceManager.findResourceTypeByName(platformTypeName);
        if (ptype == null) {
            return new HashSet<Platform>(0);
        }
           
        Collection<Resource> resources = ptype.getResources();
        Set<Platform> platforms = new HashSet<Platform>(resources.size());
           
        if (resources.size() == 0) {
            // There are no viewable platforms
            return platforms;
        }
        // TODO filter viewable
        for(Resource resource: resources) {
            platforms.add(toPlatform(resource));
        }
        return platforms;
    }

    
    
    /**
     * Get a list of all the entities which can be serviced by an Agent.
     */
    @Transactional(readOnly = true)
    public ResourceTree getEntitiesForAgent(AuthzSubject subject, Agent agt)
        throws AgentNotFoundException, PermissionException {

        Collection<Platform> plats  = new HashSet<Platform>();
        Collection<Resource> resources = getAllPlatforms();
        for(Resource resource: resources) {
            Agent resourceAgent = managedResourceRepository.findAgentByResource(resource.getId());
            if(resourceAgent.equals(agt)) {
                plats.add(toPlatform(resource));
            }
        }
        if (plats.size() == 0) {
            return new ResourceTree();
        }
        AppdefEntityID[] platIds = new AppdefEntityID[plats.size()];
        int i = 0;
        for (Platform plat : plats) {
            platIds[i] = AppdefEntityID.newPlatformID(plat.getId());
            i++;
        }

        ResourceTreeGenerator generator = Bootstrap.getBean(ResourceTreeGenerator.class);
        generator.setSubject(subject);
        try {
            return generator.generate(platIds, ResourceTreeGenerator.TRAVERSE_UP);
        } catch (AppdefEntityNotFoundException exc) {
            throw new SystemException("Internal inconsistancy finding " + "resources for agent");
        }
    }
    
    

    public Platform getPlatformByName(String name) {
        //TODO this used to find by sortName = toUpper(name)
        Resource resource = resourceDao.findByName(name);
        if(resource == null) {
            return null;
        }
        return toPlatform(resource);
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        valuePager = Pager.getPager(VALUE_PROCESSOR);
    }

}
