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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.event.def.ReattachVisitor;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
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
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.ResourceAuditFactory;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.measurement.server.session.AgentScheduleSyncZevent;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.reference.RelationshipTypes;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.sigar.NetFlags;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Platform objects in appdef and their
 * relationships
 * 
 */
@org.springframework.stereotype.Service
@Transactional
public class PlatformManagerImpl implements PlatformManager {

    private static final String CPU_COUNT = "cpuCount";

    private static final String MAC_ADDRESS = "macAddress";

    private static final String IP_ADDRESS = "address";

    private static final String CERT_DN = "certDN";

    private static final String FQDN = "FQDN";

    private final Log log = LogFactory.getLog(PlatformManagerImpl.class.getName());

    private static final String VALUE_PROCESSOR = PagerProcessor_platform.class.getName();

    private Pager valuePager;

    private PermissionManager permissionManager;

    private AgentDAO agentDAO;

    private ServerManager serverManager;

    private CPropManager cpropManager;

    private ResourceManager resourceManager;

    private ResourceGroupManager resourceGroupManager;

    private AuthzSubjectManager authzSubjectManager;

    private ServiceManager serviceManager;

    private AuditManager auditManager;

    private AgentManager agentManager;

    private ZeventEnqueuer zeventManager;

    private ResourceAuditFactory resourceAuditFactory;

    @Autowired
    public PlatformManagerImpl(
                               PermissionManager permissionManager, AgentDAO agentDAO,
                               ServerManager serverManager, CPropManager cpropManager,
                               ResourceManager resourceManager,
                               ResourceGroupManager resourceGroupManager,
                               AuthzSubjectManager authzSubjectManager,
                               ServiceManager serviceManager,
                               AuditManager auditManager, AgentManager agentManager,
                               ZeventEnqueuer zeventManager,
                               ResourceAuditFactory resourceAuditFactory) {
        this.permissionManager = permissionManager;
        this.agentDAO = agentDAO;
        this.serverManager = serverManager;
        this.cpropManager = cpropManager;
        this.resourceManager = resourceManager;
        this.resourceGroupManager = resourceGroupManager;
        this.authzSubjectManager = authzSubjectManager;
        this.serviceManager = serviceManager;
        this.auditManager = auditManager;
        this.agentManager = agentManager;
        this.zeventManager = zeventManager;
        this.resourceAuditFactory = resourceAuditFactory;
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
    public ResourceType findPlatformType(Integer id) throws ObjectNotFoundException {
        return ResourceType.findResourceType(id);
    }

    /**
     * Find a platform type by name
     * 
     * @param type - name of the platform type
     * @return platformTypeValue
     * 
     */
    @Transactional(readOnly = true)
    public ResourceType findPlatformTypeByName(String type) throws PlatformNotFoundException {
        ResourceType ptype = ResourceType.findResourceTypeByName(type);
        if (ptype == null) {
            throw new PlatformNotFoundException(type);
        }
        return ptype;
    }

    /**
     * @return {@link ResourceType}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceType> findAllPlatformTypes() {
        return ResourceType.findRootResourceType().getResourceTypesFrom(RelationshipTypes.PLATFORM_TYPE);
    }

    /**
     * @return {@link ResourceType}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceType> findSupportedPlatformTypes() {
        Collection<ResourceType> platformTypes = findAllPlatformTypes();

        for (Iterator<ResourceType> it = platformTypes.iterator(); it.hasNext();) {
            ResourceType pType = it.next();
            if (!PlatformDetector.isSupportedPlatform(pType.getName())) {
                it.remove();
            }
        }
        return platformTypes;
    }

    /**
     * @return {@link PlatformType}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceType> findUnsupportedPlatformTypes() {
        Collection<ResourceType> platformTypes = findAllPlatformTypes();

        for (Iterator<ResourceType> it = platformTypes.iterator(); it.hasNext();) {
            ResourceType pType = it.next();
            if (PlatformDetector.isSupportedPlatform(pType.getName())) {
                it.remove();
            }
        }
        return platformTypes;
    }

   

    /**
     * Find all platform types
     * 
     * @return List of PlatformTypeValues
     * 
     */
    @Transactional(readOnly = true)
    public PageList<PlatformTypeValue> getAllPlatformTypes(AuthzSubject subject, PageControl pc) {
        Collection<ResourceType> platTypes = findAllPlatformTypes();
        //TODO above query was supposed to order by name
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(platTypes, pc);
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

        // build the platform types from the visible list of platforms
        Collection platforms;

        platforms = getViewablePlatforms(subject, pc);

        Collection<AppdefResourceType> platTypes = filterResourceTypes(platforms);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(platTypes, pc);
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
            Resource service = Resource.findResource(id.getId());

            if (service == null) {
                throw new ServiceNotFoundException(id);
            }
            p=service.getResourceTo(RelationshipTypes.SERVICE).getResourceTo(RelationshipTypes.SERVER);
          
            typeName = service.getType().getName();
        } else if (id.isServer()) {
            // look up the server
            Resource server = Resource.findResource(id.getId());

            if (server == null) {
                throw new ServerNotFoundException(id);
            }
            p = server.getResourceTo(RelationshipTypes.SERVER);
            typeName = server.getType().getName();
        } else if (id.isPlatform()) {
            p = findPlatformById(id.getId());
            typeName = p.getType().getName();
        } else if (id.isGroup()) {
            ResourceGroup g = resourceGroupManager.findResourceGroupById(id.getId());
            return g.getResourcePrototype().getName();
        } else {
            throw new IllegalArgumentException("Unsupported entity type: " + id);
        }

        if (id.isPlatform()) {
            return typeName;
        } else {
            return typeName + " " + p.getType().getName();
        }
    }

    /**
     * Delete a platform
     * 
     * @param subject The user performing the delete operation.
     * @param id - The id of the Platform
     * 
     */
    public void removePlatform(AuthzSubject subject, Resource platform)
        throws PlatformNotFoundException, PermissionException, VetoException {
        
        final Audit audit = resourceAuditFactory.deleteResource(resourceManager
            .findResourceById(AuthzConstants.authzHQSystem), subject, 0, 0);
        boolean pushed = false;
        try {
            auditManager.pushContainer(audit);
            pushed = true;
            permissionManager.checkRemovePermission(subject, platform.getId());
            // keep the configresponseId so we can remove it later
           
            removeServerReferences(platform);

            
            getAIQueueManager().removeAssociatedAIPlatform(platform);
            cleanupAgent(platform);
           
            //TODO
            //if (config != null) {
              //  configResponseDAO.remove(config);
            //}
            cpropManager.deleteValues(aeid.getType(), aeid.getID());
            resourceManager.removeAuthzResource(subject, aeid, platform);
          

        } catch (PermissionException e) {
            log.debug("Error while removing Platform");

            throw e;
        } finally {
            if (pushed)
                auditManager.popContainer(false);
        }
    }

    private void cleanupAgent(Resource platform) {
        //TODO?
//        final Agent agent = platform.getAgent();
//        if (agent == null) {
//            return;
//        }
//        final Collection<Platform> platforms = agent.getPlatforms();
//        
//        for (final Iterator<Platform> it = platforms.iterator(); it.hasNext();) {
//            final Platform p = it.next();
//            if (p == null) {
//                continue;
//            }
//         
//            if (p.getId().equals(platform.getId())) {
//                it.remove();
//            }
//        }
    }

   private void removeServerReferences(Resource platform) {
//
//        final Collection<Server> servers = platform.getServersBag();
//        // since we are using the hibernate collection
//        // we need to synchronize
//        synchronized (servers) {
//            for (final Iterator<Server> i = servers.iterator(); i.hasNext();) {
//                try {
//                    // this looks funky but the idea is to pull the server
//                    // obj into the session so that it is updated when flushed
//                    final Server server = serverManager.findServerById(i.next().getId());
//                    // there are instances where we may have a duplicate
//                    // autoinventory identifier btwn platforms
//                    // (sendmail, ntpd, CAM Agent Server, etc...)
//                    final String uniqAiid = server.getPlatform().getId() +
//                                            server.getAutoinventoryIdentifier();
//                    server.setAutoinventoryIdentifier(uniqAiid);
//                    server.setPlatform(null);
//                    i.remove();
//                } catch (ServerNotFoundException e) {
//                    log.warn(e.getMessage());
//                }
//            }
//        }
   }

  

    /**
     * Create a Platform of a specified type
     * 
     * 
     */
    public Resource createPlatform(AuthzSubject subject, Integer platformTypeId,
                                   PlatformValue pValue, Integer agentPK)
        throws ValidationException, PermissionException, AppdefDuplicateNameException,
        AppdefDuplicateFQDNException, ApplicationException {
        // check if the object already exists

        if (Resource.findResourceByName(pValue.getName()) != null) {
            // duplicate found, throw a duplicate object exception
            throw new AppdefDuplicateNameException();
        }
        
        if (findByFQDN(pValue.getFqdn()) != null) {
            // duplicate found, throw a duplicate object exception
            throw new AppdefDuplicateFQDNException();
        }

        try {
            // AUTHZ CHECK
            // in order to succeed subject has to be in a role
            // which allows creating of authz resources
            permissionManager.checkCreatePlatformPermission(subject);
            Resource platform;
            Agent agent = null;

            if (agentPK != null) {
                agent = agentDAO.findById(agentPK);
            }
            //TODO
            //ConfigResponseDB config;
           // if (pValue.getConfigResponseId() == null) {
             //   config = configResponseDAO.createPlatform();
            //} else {
            //    config = configResponseDAO.findById(pValue.getConfigResponseId());
            //}

            trimStrings(pValue);
            getCounter().addCPUs(pValue.getCpuCount().intValue());
            validateNewPlatform(pValue);
            ResourceType pType = findPlatformType(platformTypeId);

            pValue.setOwner(subject.getName());
            pValue.setModifiedBy(subject.getName());

            Resource platformRes = create(pValue, agent,pType);
            platformRes.persist(); // To setup its ID
     

            // Create the virtual server types
            //TODo what the hell are virtual servers?
//            for (ServerType st : pType.getServerTypes()) {
//
//                if (st.isVirtual()) {
//                    serverManager.createVirtualServer(subject, platform, st);
//                }
//            }

          

            // Send resource create event
            ResourceCreatedZevent zevent = new ResourceCreatedZevent(subject, platform
                .getId());
            zeventManager.enqueueEventAfterCommit(zevent);

            return platform;
        } catch (NotFoundException e) {
            throw new ApplicationException("Unable to find PlatformType: " + platformTypeId +
                                           " : " + e.getMessage());
        }
    }
    
    private Resource create(PlatformValue pv, Agent agent, ResourceType type) 
    {
        Resource p = new Resource();
        
        p.setName(pv.getName());
        p.setDescription(pv.getDescription());
        p.setProperty(CERT_DN,pv.getCertdn());
        p.setCommentText(pv.getCommentText());
        p.setProperty(CPU_COUNT,pv.getCpuCount());
        p.setProperty(FQDN,pv.getFqdn());
        p.setLocation(pv.getLocation());
        p.setModifiedBy(pv.getModifiedBy());
        p.setType(type);
        //TODO?
        //p.setAgent(agent);
        //TODO setConfig
        //p.setConfigResponse(config);
        
        for (Iterator i=pv.getAddedIpValues().iterator(); i.hasNext();) {
            IpValue ipv = (IpValue)i.next();
            Resource ip = new Resource();
            ip.setProperty(IP_ADDRESS, ipv.getAddress());
            ip.setProperty("netmask",ipv.getNetmask());
            ip.setProperty(MAC_ADDRESS, ipv.getMACAddress());
            ip.persist();
            p.relateTo(ip, RelationshipTypes.IP);
        }
       
        return p;
    }
    
    private Resource create(AIPlatformValue aip, String initialOwner,Agent agent, ResourceType type) 
    {
        Resource p = copyAIPlatformValue(aip);
        p.setType(type);
        //TODO set config
        //p.setConfigResponse(config);
        p.setModifiedBy(initialOwner);
        p.setAgent(agent);
        return p;
    }
    
    private Resource copyAIPlatformValue(AIPlatformValue aip) {
            Resource p = new Resource();
       
            p.setProperty(CERT_DN,aip.getCertdn());
            p.setProperty(FQDN,aip.getFqdn());
            p.setName(aip.getName());
            p.setDescription(aip.getDescription());
            p.setCommentText("");
            p.setLocation("");
            p.setProperty(CPU_COUNT,aip.getCpuCount());
            fixName(p);
            return p;
    }
    
    private void fixName(Resource p) {
            // if name is not set then set it to fqdn (assuming it is set of course)
            String name = p.getName();
            if (name == null || "".equals(name.trim())) {
                p.setName((String)p.getProperty(FQDN));
            }
    }
  

    private void throwDupPlatform(Serializable id, String platName) {
        throw new NonUniqueObjectException(id, "Duplicate platform found " + "with name: " +
                                               platName);
    }

    /**
     * Create a Platform from an AIPlatform
     * 
     * @param aipValue the AIPlatform to create as a regular appdef platform.
     * 
     */
    public Resource createPlatform(AuthzSubject subject, AIPlatformValue aipValue)
        throws ApplicationException {
        getCounter().addCPUs(aipValue.getCpuCount().intValue());

        ResourceType platType = ResourceType.findResourceTypeByName(aipValue.getPlatformTypeName());

        if (platType == null) {
            throw new SystemException("Unable to find PlatformType [" +
                                      aipValue.getPlatformTypeName() + "]");
        }

        Resource checkP = Resource.findResourceByName(aipValue.getName());
        if (checkP != null) {
            throwDupPlatform(checkP.getId(), aipValue.getName());
        }

        Agent agent = agentDAO.findByAgentToken(aipValue.getAgentToken());

        if (agent == null) {
            throw new ApplicationException("Unable to find agent: " + aipValue.getAgentToken());
        }
        //TODO
        //ConfigResponseDB config = configResponseDAO.createPlatform();
        
        // AUTHZ CHECK
        try {
            permissionManager.checkCreatePlatformPermission(subject);
        } catch (Exception e) {
            throw new SystemException(e);
        }

        Resource platform = create(aipValue, subject.getName(),agent,platType);
        
        platform.persist();

       

        // Send resource create event
        ResourceCreatedZevent zevent = new ResourceCreatedZevent(subject, platform.getId());
        zeventManager.enqueueEventAfterCommit(zevent);

        return platform;
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

        Collection<Resource> platforms;

        platforms = getViewablePlatforms(subject, pc);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(platforms, pc);
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

        Collection<Resource> platforms = Resource
            .findByCTime(System.currentTimeMillis() - range);

        // now get the list of PKs
        List<Integer> viewable = getViewablePlatformPKs(subject);
        // and iterate over the list to remove any item not viewable
        for (Iterator<Resource> i = platforms.iterator(); i.hasNext();) {
            Resource platform = i.next();
            if (!viewable.contains(platform.getId())) {
                // remove the item, user cant see it
                i.remove();
            }
        }

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(platforms, pc);
    }

    /**
     * Get platform light value by id. Does not check permission.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Resource getPlatformById(AuthzSubject subject, Integer id)
        throws PlatformNotFoundException, PermissionException {
        Resource platform = findPlatformById(id);
        permissionManager.checkViewPermission(subject, platform.getId());
        // Make sure that resource is loaded as to not get
        // LazyInitializationException
        platform.getName();
        return platform;
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
    public Resource findPlatformById(Integer id) throws PlatformNotFoundException {
        Resource platform = Resource.findResource(id);

        if (platform == null) {
            throw new PlatformNotFoundException(id);
        }

        // Make sure that resource is loaded as to not get
        // LazyInitializationException
        platform.getName();

        return platform;
    }
    
    private Collection<Resource> getAllPlatforms() {
        return Resource.findRootResource().getResourcesFrom(RelationshipTypes.PLATFORM);
        
    }
    
    private Resource findByFQDN(String fqdn) {
        Collection<Resource> platforms = getAllPlatforms();
        for(Resource platform: platforms) {
            if(fqdn.equals(platform.getProperty(FQDN))) {
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
                if(address.equals(ip.getProperty(IP_ADDRESS))) {
                    platforms.add(platform);
                }
            }
        }
        return platforms;
    }
    
    private Collection<Resource> findByMacAddr(String address) {
        Set<Resource> platforms = new HashSet<Resource>();
        for(Resource platform: getAllPlatforms()) {
            Set<Resource> ips = platform.getResourcesFrom(RelationshipTypes.IP);
            for(Resource ip:ips) {
                if(address.equals(ip.getProperty(MAC_ADDRESS))) {
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
    public Resource findPlatformByAIPlatform(AuthzSubject subject, AIPlatformValue aiPlatform) 
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
        if (p != null) {
            permissionManager.checkViewPermission(subject, p.getId());
        }
        if(p == null) {
            throw new PlatformNotFoundException("platform not found for ai " + "platform: " +
                aiPlatform.getId());
        }
        return p;
    }

    /**
     * Get the Platform object based on an AIPlatformValue. Checks against FQDN,
     * CertDN, then checks to see if all IP addresses match. If all of these
     * checks fail null is returned.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Resource getPlatformByAIPlatform(AuthzSubject subject, AIPlatformValue aiPlatform)
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

                        if (plat.getProperty(FQDN).equals(fqdn)) { // Perfect
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
            permissionManager.checkViewPermission(subject, p.getId());
            if (porker && // Let agent porker
                // create new platforms
                !(p.getProperty(FQDN).equals(fqdn) || p.getProperty(CERT_DN).equals(certdn) || p.getAgent().getAgentToken().equals(agentToken))) {
                p = null;
            }
        }

        return p;
    }

    /**
     * @return non-virtual, physical, {@link Platform} associated with the
     *         agentToken or null if one does not exist.
     * 
     */
    @Transactional(readOnly = true)
    public Resource getPhysPlatformByAgentToken(String agentToken) {
        try {
            Agent agent = agentManager.getAgent(agentToken);
            Collection<Resource> platforms = getAllPlatforms();
            for (Resource platform : platforms) {
                if(agent.equals(platform.getAgent())) {
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
            if (!ipSet.contains(ip.getProperty(IP_ADDRESS))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Find a platform by name
     * 
     * 
     * @param subject - who is trying this
     * @param name - the name of the platform
     */
    @Transactional(readOnly = true)
    public PlatformValue getPlatformByName(AuthzSubject subject, String name)
        throws PlatformNotFoundException, PermissionException {
        Resource p = Resource.findResourceByName(name);
        if (p == null) {
            throw new PlatformNotFoundException("platform " + name + " not found");
        }
        // now check if the user can see this at all
        permissionManager.checkViewPermission(subject, p.getId());
        return getPlatformValue(p);
    }
    
    private PlatformValue getPlatformValue(Resource platform) {
        PlatformValue _platformValue = new PlatformValue();
        _platformValue.setSortName(getSortName());
        _platformValue.setCommentText(getCommentText());
        _platformValue.setModifiedBy(getModifiedBy());
        _platformValue.setOwner(getOwner());
        _platformValue.setCertdn(getCertdn());
        _platformValue.setFqdn(getFqdn());
        _platformValue.setName(platform.getName());
        _platformValue.setLocation(getLocation());
        _platformValue.setDescription(getDescription());
        _platformValue.setCpuCount(getCpuCount());
        _platformValue.setId(platform.getId());
        _platformValue.setMTime(getMTime());
        _platformValue.setCTime(getCTime());
        _platformValue.removeAllIpValues();
        Iterator iIpValue = getIps(platform).iterator();
        while (iIpValue.hasNext()){
            _platformValue.addIpValue( ((Ip)iIpValue.next()).getIpValue() );
        }
        _platformValue.cleanIpValue();
        if (platform.getType() != null)
            _platformValue.setPlatformType(
                platform.getType().getPlatformTypeValue());
        else
            _platformValue.setPlatformType( null );
        if (platform.getAgent() != null) {
            // Make sure that the agent is loaded
            _platformValue.setAgent(platform.getAgent());
        }
        else
            _platformValue.setAgent(null);
        return _platformValue;
    }
    
    private PlatformTypeValue getPlatformTypeValue(ResourceType platformType) {
        PlatformTypeValue _platformTypeValue =  new PlatformTypeValue();
        _platformTypeValue.setSortName(getSortName());
        _platformTypeValue.setName(platformType.getName());
        _platformTypeValue.setDescription(getDescription());
        _platformTypeValue.setPlugin(getPlugin());
        _platformTypeValue.setId(platformType.getId());
        _platformTypeValue.setMTime(getMTime());
        _platformTypeValue.setCTime(getCTime());
        return _platformTypeValue;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Resource getPlatformByName(String name) {
        //TODO SortName?
        return Resource.findResourceByName(name);
    }

    /**
     * Get the Platform that has the specified Fqdn
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Resource findPlatformByFqdn(AuthzSubject subject, String fqdn)
        throws PlatformNotFoundException, PermissionException {
        Resource p;
        try {
            p = findByFQDN(fqdn);
        } catch (NonUniqueResultException e) {
            //TODO this isn't going to get thrown
            p = null;
        }
        if (p == null) {
            throw new PlatformNotFoundException("Platform with fqdn " + fqdn + " not found");
        }
        // now check if the user can see this at all
        permissionManager.checkViewPermission(subject, p.getId());
        return p;
    }

    /**
     * Get the Collection of platforms that have the specified Ip address
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Resource> getPlatformByIpAddr(AuthzSubject subject, String address)
        throws PermissionException {
        return findByIpAddr(address);
    }

    @Transactional(readOnly = true)
    public Collection<Resource> getPlatformByMacAddr(AuthzSubject subject, String address)
        throws PermissionException {

        // TODO: Add permission check

        return findByMacAddr(address);
    }

    @Transactional(readOnly = true)
    public Resource getAssociatedPlatformByMacAddress(AuthzSubject subject, Resource r)
        throws PermissionException, PlatformNotFoundException {
        // TODO: Add permission check

        ResourceType rt = r.getType();
        if (rt != null && !rt.getId().equals(AuthzConstants.authzPlatform)) {
            throw new PlatformNotFoundException("Invalid resource type = " + rt.getName());
        }

        Resource platform = findPlatformById(r.getInstanceId());
        String macAddr = getPlatformMacAddress(platform);
        Collection<Resource> platforms = getPlatformByMacAddr(subject, macAddr);
        Resource associatedPlatform = null;

        for (Resource p : platforms) {
            if (!p.getId().equals(platform.getId())) {
                // TODO: Add additional logic if there are more than 2 platforms
                associatedPlatform = p;
                break;
            }
        }

        if (associatedPlatform == null) {
            if (log.isDebugEnabled()) {
                log.debug("No matching platform found from " + platforms.size() + " platforms" +
                           " for resource[id=" + r.getId() + ", macAddress=" + macAddr + "].");
            }
        }

        return associatedPlatform;
    }

    private String getPlatformMacAddress(Resource platform) {
        // TODO: Should this method be part of the Platform object?

        String macAddress = null;

        for (Resource ip : getIps(platform)) {
            if (!"00:00:00:00:00:00".equals(ip.getProperty(MAC_ADDRESS))) {
                macAddress = (String)ip.getProperty(MAC_ADDRESS);
                break;
            }
        }

        return macAddress;
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

        List<Integer> pks = new ArrayList<Integer>();
        for (Resource plat : platforms) {
            pks.add(plat.getId());
        }
        return pks;
    }
    
    private Collection<Resource> findByAgentToken(String agentToken) {
        Set<Resource> agentPlatforms = new HashSet<Resource>();
        Collection<Resource> platforms = getAllPlatforms();
        for (Resource platform : platforms) {
            if(agentToken.equals(platform.getAgent().getAgentToken())) {
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
        // now check if the user can see this at all
        permissionManager.checkViewPermission(subject, p.getId());
        return getPlatformValue(p);
    }
    
    private Resource findByServiceId(Integer serviceId) {
        Set<Resource> servers = Resource.findRootResource().getResourcesFrom(RelationshipTypes.SERVER);
        for(Resource server: servers) {
            Set<Resource> services = server.getResourcesFrom(RelationshipTypes.SERVICE);
            for(Resource service : services) {
                if(serviceId.equals(service.getId())) {
                    return service;
                }
            }
        }
        return null;
    }
    
    private List<Resource> findByServers(Integer[] ids) {
        List<Resource> platforms = new ArrayList<Resource>();
        for(Integer id: ids) {
            Resource server = Resource.findResource(id);
            platforms.add(server.getResourceTo(RelationshipTypes.PLATFORM));
        }
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
        Resource server = Resource.findResource(serverId);

        if (server == null || server.getResourceTo(RelationshipTypes.PLATFORM) == null) {
            // This should throw server not found. Servers always have
            // platforms..
            throw new PlatformNotFoundException("platform for server " + serverId + " not found");
        }

        Resource p = server.getResourceTo(RelationshipTypes.PLATFORM);
        permissionManager.checkViewPermission(subject, p.getId());
        return getPlatformValue(p);
    }

    /**
     * Get the platform ID for a server.
     * 
     * 
     * @param serverId Server ID.
     */
    @Transactional(readOnly = true)
    public Integer getPlatformIdByServer(Integer serverId) throws PlatformNotFoundException {
        Resource server = Resource.findResource(serverId);

        if (server == null)
            throw new PlatformNotFoundException("platform for server " + serverId + " not found");

        return server.getResourceTo(RelationshipTypes.PLATFORM).getId();
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
        Set<Integer> authzPks;
        try {
            authzPks = new HashSet<Integer>(getViewablePlatformPKs(subject));
        } catch (NotFoundException exc) {
            return new PageList<PlatformValue>();
        }

        Integer[] ids = new Integer[sIDs.size()];
        int i = 0;
        for (Iterator<AppdefEntityID> it = sIDs.iterator(); it.hasNext(); i++) {
            AppdefEntityID svrId = it.next();
            ids[i] = svrId.getId();
        }

        List<Resource> foundPlats = findByServers(ids);

        ArrayList<Resource> platforms = new ArrayList<Resource>();
        for (Resource platform : foundPlats) {
            if (authzPks.contains(platform.getId())) {
                platforms.add(platform);
            }
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

        ResourceGroup appLocal = ResourceGroup.findResourceGroup(appId);
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

            if (appService.isIsGroup()) {
                Collection<Service> services = serviceManager.getServiceCluster(
                    appService.getResourceGroup()).getServices();

                for (Service service : services) {

                    PlatformValue pValue = getPlatformByService(subject, service.getId());
                    if (!platCollection.contains(pValue)) {
                        platCollection.add(pValue);
                    }
                }
            } else {
                Integer serviceId = appService.getId();
                PlatformValue pValue = getPlatformByService(subject, serviceId);
                // Fold duplicate platforms
                if (!platCollection.contains(pValue)) {
                    platCollection.add(pValue);
                }
            }
        }

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(platCollection, pc);
    }

    /**
     * builds a list of resource types from the list of resources
     * @param resources - {@link Collection} of {@link AppdefResource}
     * @param {@link Collection} of {@link AppdefResourceType}
     */
    private Collection<AppdefResourceType> filterResourceTypes(Collection<AppdefResource> resources) {
        final Set<AppdefResourceType> resTypes = new HashSet<AppdefResourceType>();
        for (final AppdefResource o : resources) {

            if (o == null) {
                continue;
            }
            final AppdefResourceType rt = o.getAppdefResourceType();
            if (rt != null) {
                resTypes.add(rt);
            }
        }
        final List<AppdefResourceType> rtn = new ArrayList<AppdefResourceType>(resTypes);
        Collections.sort(rtn, new Comparator<AppdefResourceType>() {
            private String getName(AppdefResourceType obj) {

                return ((AppdefResourceType) obj).getSortName();

            }

            public int compare(AppdefResourceType o1, AppdefResourceType o2) {
                return getName(o1).compareTo(getName(o2));
            }
        });
        return rtn;
    }

    protected List<Integer> getViewablePlatformPKs(AuthzSubject who) throws PermissionException,
        NotFoundException {
        // now get a list of all the viewable items

        OperationType op = getOperationByName(resourceManager
            .findResourceTypeByName(AuthzConstants.platformResType),
            AuthzConstants.platformOpViewPlatform);
        return permissionManager.findOperationScopeBySubject(who, op.getId());
    }

    /**
     * Find an operation by name inside a ResourcetypeValue object
     */
    protected OperationType getOperationByName(org.hyperic.hq.inventory.domain.ResourceType rtV, String opName)
        throws PermissionException {
        OperationType op = rtV.getOperationType(opName);
        if(op == null) {
            throw new PermissionException("Operation: " + opName + " not valid for ResourceType: " +
                                      rtV.getName());
        }
        return op;
    }

    /**
     * Get server IDs by server type and platform.
     * 
     * 
     * 
     * @param subject The subject trying to list servers.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public Integer[] getPlatformIds(AuthzSubject subject, Integer platTypeId)
        throws PermissionException {

        try {

            Collection<Resource> platforms = ResourceType.findResourceType(platTypeId).getResources();
            Collection<Integer> platIds = new ArrayList<Integer>();

            // now get the list of PKs
            Collection<Integer> viewable = getViewablePlatformPKs(subject);
            // and iterate over the list to remove any item not in the
            // viewable list
            for (Resource platform : platforms) {

                if (viewable.contains(platform.getId())) {
                    // remove the item, user cant see it
                    platIds.add(platform.getId());
                }
            }

            return (Integer[]) platIds.toArray(new Integer[0]);
        } catch (NotFoundException e) {
            // There are no viewable platforms
            return new Integer[0];
        }
    }

    /**
     * Get server IDs by server type and platform.
     * 
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param pc The page control.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public List<Resource> getPlatformsByType(AuthzSubject subject, String type)
        throws PermissionException, InvalidAppdefTypeException {
        try {
            ResourceType ptype = ResourceType.findResourceTypeByName(type);
            if (ptype == null) {
                return new PageList<Resource>();
            }

            List<Resource> platforms = new ArrayList<Resource>(ResourceType.findResourceType(ptype.getId()).getResources());
            if (platforms.size() == 0) {
                // There are no viewable platforms
                return platforms;
            }
            // now get the list of PKs
            Collection<Integer> viewable = getViewablePlatformPKs(subject);
            // and iterate over the List to remove any item not in the
            // viewable list
            for (Iterator<Resource> it = platforms.iterator(); it.hasNext();) {
                Resource platform = it.next();
                if (!viewable.contains(platform.getId())) {
                    // remove the item, user can't see it
                    it.remove();
                }
            }

            return platforms;
        } catch (NotFoundException e) {
            // There are no viewable platforms
            return new PageList<Resource>();
        }
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
    protected Collection<Resource> getViewablePlatforms(AuthzSubject whoami, PageControl pc)
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
                    platforms = findAll_orderName(pc.isAscending());
                    break;
                case SortAttribute.CTIME:
                    platforms = findAll_orderCTime(pc.isAscending());
                    break;
                default:
                    throw new NotFoundException("Invalid sort attribute: " + attr);
            }
        }
        // now get the list of PKs
        Set<Integer> viewable = new HashSet<Integer>(getViewablePlatformPKs(whoami));
        // and iterate over the List to remove any item not in the
        // viewable list
        for (Iterator<Resource> i = platforms.iterator(); i.hasNext();) {
            Resource platform = i.next();
            if (!viewable.contains(platform.getId())) {
                // remove the item, user cant see it
                i.remove();
            }
        }
        return platforms;
    }

    /**
     * Get the platforms that have an IP with the specified address. If no
     * matches are found, this method DOES NOT throw a
     * PlatformNotFoundException, rather it returns an empty PageList.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public PageList<PlatformValue> findPlatformsByIpAddr(AuthzSubject subject, String addr,
                                                         PageControl pc) throws PermissionException {
        Collection<Resource> platforms = findByIpAddr(addr);
        if (platforms.size() == 0) {
            return new PageList<PlatformValue>();
        }
        return valuePager.seek(platforms, pc);
    }

    /**
     * @param subj
     * @param pType platform type
     * @param nameRegEx regex which matches either the platform fqdn or the
     *        resource sortname XXX scottmf need to add permission checking
     * 
     */
    @Transactional(readOnly = true)
    public List<Resource> findPlatformPojosByTypeAndName(AuthzSubject subj, Integer pType,
                                                         String regEx) {
        //TODO regex for name or FQDN
        return findByTypeAndRegEx(pType, regEx);
    }

    /**
     * @param subj
     * @param platformTypeIds List<Integer> of platform type ids
     * @param hasChildren indicates whether the platform is the parent of a
     *        network hierarchy
     * @return a list of {@link Platform}s
     * 
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Resource> findParentPlatformPojosByNetworkRelation(AuthzSubject subj,
                                                                   List<Integer> platformTypeIds,
                                                                   String platformName,
                                                                   Boolean hasChildren) {
        List<ResourceType> unsupportedPlatformTypes = new ArrayList<ResourceType>(
            findUnsupportedPlatformTypes());
        List<Integer> pTypeIds = new ArrayList<Integer>();

        if (platformTypeIds != null && !platformTypeIds.isEmpty()) {
            for (Integer pTypeId : platformTypeIds) {

                ResourceType pType = findPlatformType(pTypeId);
                if (unsupportedPlatformTypes.contains(pType)) {
                    pTypeIds.add(pTypeId);
                }
            }
            if (pTypeIds.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
        } else {
            // default values
            for (ResourceType pType : unsupportedPlatformTypes) {

                pTypeIds.add(pType.getId());
            }
        }
        //TODO network relation parent/child relationship?
        //return platformDAO.findParentByNetworkRelation(pTypeIds, platformName, hasChildren);
    }

    /**
     * @param subj
     * @param platformTypeIds List<Integer> of platform type ids
     * @return a list of {@link Platform}s
     * 
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Resource> findPlatformPojosByNoNetworkRelation(AuthzSubject subj,
                                                               List<Integer> platformTypeIds,
                                                               String platformName) {
        List<ResourceType> supportedPlatformTypes = new ArrayList<ResourceType>(
            findSupportedPlatformTypes());
        List<Integer> pTypeIds = new ArrayList<Integer>();

        if (platformTypeIds != null && !platformTypeIds.isEmpty()) {
            for (Integer pTypeId : platformTypeIds) {

                ResourceType pType = findPlatformType(pTypeId);
                if (supportedPlatformTypes.contains(pType)) {
                    pTypeIds.add(pTypeId);
                }
            }
            if (pTypeIds.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
        } else {
            // default values
            for (ResourceType pType : supportedPlatformTypes) {

                pTypeIds.add(pType.getId());
            }
        }
        //TODO network relation parent/child relationship?
        //return platformDAO.findByNoNetworkRelation(pTypeIds, platformName);
    }

    /**
     * Get the platforms that have an IP with the specified address.
     * 
     * @return a list of {@link Platform}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Resource> findPlatformPojosByIpAddr(String addr) {
        return findByIpAddr(addr);
    }
    
    public boolean matchesValueObject(PlatformValue obj, Resource platform) {
        boolean matches;
        if (obj.getId() != null) {
            matches = (obj.getId().intValue() == platform.getId().intValue());
        } else {
            matches = (platform.getId() == null);
        }
        if (obj.getCTime() != null) {
            matches = (obj.getCTime().floatValue() == platform.getCTime().floatValue());
        } else {
            matches = (platform.getCreationTime() == 0);
        }
        matches &=
            (platform.getName() != null ? platform.getName().equals(obj.getName())
                : (obj.getName() == null)) ;
        matches &=
            (platform.getDescription() != null ?
                platform.getDescription().equals(obj.getDescription())
                : (obj.getDescription() == null)) ;
        matches &=
            (platform.getProperty(CERT_DN) != null ? platform.getProperty(CERT_DN).equals(obj.getCertdn())
                : (obj.getCertdn() == null)) ;
        matches &=
            (platform.getCommentText() != null ?
                platform.getCommentText().equals(obj.getCommentText())
                : (obj.getCommentText() == null)) ;
        matches &=
            (platform.getProperty(CPU_COUNT) != null ?
                platform.getProperty(CPU_COUNT).equals(obj.getCpuCount())
                : (obj.getCpuCount() == null)) ;
        matches &=
            (platform.getProperty(FQDN) != null ? platform.getProperty(FQDN).equals(obj.getFqdn())
                : (obj.getFqdn() == null)) ;
        matches &=
            (platform.getLocation() != null ?
                platform.getLocation().equals(obj.getLocation())
                : (obj.getLocation() == null)) ;
        // now for the IP's
        // if there's any in the addedIp's collection, it was messed with
        // which means the match fails
        matches &=
            (obj.getAddedIpValues().size() == 0) ;
        matches &=
            (obj.getRemovedIpValues().size() == 0) ;
        // check to see if we have changed the agent
        matches &=
            (platform.getAgent() != null ? platform.getAgent().equals(obj.getAgent())
                : (obj.getAgent() == null));
        return matches;
        }


    /**
     * Update an existing Platform. Requires all Ip's to have been re-added via
     * the platformValue.addIpValue(IpValue) method due to bug 4924
     * 
     * @param existing - the value object for the platform you want to save
     * 
     */
    public Resource updatePlatformImpl(AuthzSubject subject, PlatformValue existing)
        throws UpdateException, PermissionException, AppdefDuplicateNameException,
        PlatformNotFoundException, AppdefDuplicateFQDNException, ApplicationException {
        permissionManager.checkPermission(subject, existing.getEntityId(),
            AuthzConstants.platformOpModifyPlatform);
        existing.setModifiedBy(subject.getName());
        existing.setMTime(new Long(System.currentTimeMillis()));
        trimStrings(existing);

        Resource plat = Resource.findResource(existing.getId());

        if (existing.getCpuCount() == null) {
            // cpu count is no longer an option in the UI
            existing.setCpuCount((Integer)plat.getProperty(CPU_COUNT));
        }

        if (matchesValueObject(existing,plat)) {
            log.debug("No changes found between value object and entity");
            return plat;
        } else {
            int newCount = existing.getCpuCount().intValue();
            int prevCpuCount = ((Integer)plat.getProperty(CPU_COUNT)).intValue();
            if (newCount > prevCpuCount) {
                getCounter().addCPUs(newCount - prevCpuCount);
            }

            if (!(existing.getName().equals(plat.getName()))) {
                if (Resource.findResourceByName(existing.getName()) != null)
                    // duplicate found, throw a duplicate object exception
                    throw new AppdefDuplicateNameException();
            }

            if (!(existing.getFqdn().equals(plat.getProperty(FQDN)))) {
                if (findByFQDN(existing.getFqdn()) != null)
                    // duplicate found, throw a duplicate object exception
                    throw new AppdefDuplicateFQDNException();
            }

            // See if we need to create an AIPlatform
            if (existing.getAgent() != null) {
                if (plat.getAgent() == null) {
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
                } else if (!plat.getAgent().equals(existing.getAgent())) {
                    // Need to enqueue the ResourceUpdatedZevent if the
                    // agent changed to get the metrics scheduled
                    List<ResourceUpdatedZevent> events = new ArrayList<ResourceUpdatedZevent>();
                    events.add(new ResourceUpdatedZevent(subject, plat.getId()));
                    for (Resource svr : plat.getResourcesFrom(RelationshipTypes.SERVER)) {

                        events.add(new ResourceUpdatedZevent(subject, svr.getId()));

                        for (Resource svc : svr.getResourcesFrom(RelationshipTypes.SERVICE)) {

                            events.add(new ResourceUpdatedZevent(subject, svc.getId()));
                        }
                    }

                    zeventManager.enqueueEventsAfterCommit(events);
                }
            }
            updatePlatform(plat, existing);
            return plat;
        }
    }
    
    private void updatePlatform(Resource platform, PlatformValue existing) {
        // retrieve current list of ips
        Collection curips = getIps(platform);
        if (curips == null) {
            curips = new ArrayList();
            setIps(platform,curips);
        }
      
        // first remove any which were in the removedIp collection
        for (Iterator i = existing.getRemovedIpValues().iterator(); i.hasNext();) {
            IpValue aIp = (IpValue) i.next();
            if (aIp.idHasBeenSet()) {
                removeAIp(curips, aIp);
            }
        }
        Collection ips = existing.getAddedIpValues();
        // now get any ips which were in the ipValues array
        for (int i = 0; i < existing.getIpValues().length; i++) {
            IpValue aIp = existing.getIpValues()[i];
            if (!(ips.contains(aIp))) {
                ips.add(aIp);
            }
        }
        for (Iterator i = ips.iterator(); i.hasNext();) {
            IpValue aIp = (IpValue) i.next();
            if (aIp.idHasBeenSet()) {
      
                updateAIp(curips, aIp);
            } else {
                // looks like its a new one
                Ip nip = new Ip();
                nip.setIpValue(aIp);
                nip.setPlatform(platform);
                curips.add(nip);
            }
        }
        // finally update the platform
        platform.setPlatformValue(existing);
       
        // if there is a agent
        if (existing.getAgent() != null) {
            // get the agent token and set the agent to the platform
            platform.setAgent(existing.getAgent());
        }
        platform.persist();
        // it is a good idea to
        // flush the Session here
    }

    /**
     * Update an existing Platform. Requires all Ip's to have been re-added via
     * the platformValue.addIpValue(IpValue) method due to bug 4924
     * 
     * @param existing - the value object for the platform you want to save
     * 
     */
    public Resource updatePlatform(AuthzSubject subject, PlatformValue existing)
        throws UpdateException, PermissionException, AppdefDuplicateNameException,
        PlatformNotFoundException, AppdefDuplicateFQDNException, ApplicationException {
        return updatePlatformImpl(subject, existing);
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

    /**
     * DevNote: This method was refactored out of updatePlatformTypes. It does
     * not work.
     * 
     * 
     */
    public void deletePlatformType(ResourceType pt) throws VetoException {

        Resource proto = resourceManager.findResourceByInstanceId(
            AuthzConstants.authzPlatformProto, pt.getId());
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        try {
            log.debug("Removing PlatformType: " + pt.getName());

            resourceGroupManager.removeGroupsCompatibleWith(proto);

            // Remove all platforms
            for (Resource platform : pt.getResources()) {

                try {
                    removePlatform(overlord, platform);
                } catch (PlatformNotFoundException e) {
                    assert false : "Delete based on a platform should not "
                                   + "result in PlatformNotFoundException";
                }
            }
        } catch (PermissionException e) {
            assert false : "Overlord should not run into PermissionException";
        }

        // Need to remove all server types, too

        for (ServerType st : pt.getServerTypes()) {
            serverManager.deleteServerType(st, overlord, resourceGroupManager, resourceManager);
        }

        
        pt.remove();
    }

    /**
     * Update platform types
     * 
     * 
     */
    public void updatePlatformTypes(String plugin, PlatformTypeInfo[] infos) throws VetoException,
        NotFoundException {
        // First, put all of the infos into a Hash
        HashMap<String, PlatformTypeInfo> infoMap = new HashMap<String, PlatformTypeInfo>();
        for (int i = 0; i < infos.length; i++) {
            infoMap.put(infos[i].getName(), infos[i]);
        }

        Collection<ResourceType> curPlatforms = findByPlugin(plugin);

        for (ResourceType ptlocal : curPlatforms) {

            String localName = ptlocal.getName();
            PlatformTypeInfo pinfo = (PlatformTypeInfo) infoMap.remove(localName);

            // See if this exists
            if (pinfo == null) {
                deletePlatformType(ptlocal);
            } else {
                String curName = ptlocal.getName();
                String newName = pinfo.getName();

                // Just update it
                log.debug("Updating PlatformType: " + localName);

                if (!newName.equals(curName))
                    ptlocal.setName(newName);
            }
        }

        // Now create the left-overs
        for (PlatformTypeInfo pinfo : infoMap.values()) {
            createPlatformType(pinfo.getName(), plugin);
        }
    }

    public ResourceType createPlatformType(String name, String plugin) throws NotFoundException {
        log.debug("Creating new PlatformType: " + name);
        Resource prototype = resourceManager.findRootResource();
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        ResourceType pt = new ResourceType();
        pt.setName(name);
        pt.persist();
        //TODO relate ResourceType to plugin
        return pt;
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

        Resource platform = this.getPlatformByAIPlatform(subj, aiplatform);
        if (platform == null) {
            throw new PlatformNotFoundException("Platform not found with either FQDN: " + fqdn +
                                                " nor CertDN: " + certdn);
        }
        int prevCpuCount = ((Integer)platform.getProperty(CPU_COUNT)).intValue();
        Integer count = aiplatform.getCpuCount();
        if ((count != null) && (count.intValue() > prevCpuCount)) {
            getCounter().addCPUs(aiplatform.getCpuCount().intValue() - prevCpuCount);
        }

        // Get the FQDN before we update
        String prevFqdn = (String)platform.getProperty(FQDN);

        platform.updateWithAI(aiplatform, subj.getName(), platform.getResource());

        // If FQDN has changed, we need to update servers' auto-inventory tokens
        if (!prevFqdn.equals(platform.getProperty(FQDN))) {
            for (Server server : platform.getServers()) {

                if (server.getAutoinventoryIdentifier().startsWith(prevFqdn)) {
                    String newAID = server.getAutoinventoryIdentifier().replace(prevFqdn, fqdn);
                    server.setAutoinventoryIdentifier(newAID);
                }
            }
        }

        // need to check if IPs have changed, if so update Agent
        updateAgentIps(subj, aiplatform, platform);

    }

    private void updateAgentIps(AuthzSubject subj, AIPlatformValue aiplatform, Resource platform) {
        List<AIIpValue> ips = Arrays.asList(aiplatform.getAIIpValues());

        Agent agent;
        try {
            // make sure we have the current agent that exists on the platform
            // and associate it
            agent = agentManager.getAgent(aiplatform.getAgentToken());
            platform.setAgent(agent);
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
                    platform.setAgent(agent);
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
            if (platform.getAgent() == null) {
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
    public void addIp(Resource platform, String address, String netmask, String macAddress) {
        return platform.addIp(address, netmask, macAddress);
    }

    /**
     * Update an IP on a platform
     * 
     * 
     */
    public void updateIp(Resource platform, String address, String netmask, String macAddress) {
        return platform.updateIp(address, netmask, macAddress);
    }

    /**
     * Remove an IP on a platform
     * 
     * 
     */
    public void removeIp(Resource platform, String address, String netmask, String macAddress) {
        Ip ip = platform.removeIp(address, netmask, macAddress);
        if (ip != null) {
            platformDAO.remove(ip);
        }
    }

    /**
     * Returns a list of 2 element arrays. The first element is the name of the
     * platform type, the second element is the # of platforms of that type in
     * the inventory.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<Object[]> getPlatformTypeCounts() {
        return platformDAO.getPlatformTypeCounts();
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Number getPlatformCount() {
        return platformDAO.getPlatformCount();
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Number getCpuCount() {
        return platformDAO.getCpuCount();
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        valuePager = Pager.getPager(VALUE_PROCESSOR);
    }

}
