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
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.ConfigResponseDB;
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
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
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
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.ResourceAuditFactory;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.vm.VmMapping;
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
@org.springframework.stereotype.Service("PlatformManager")
@Transactional
public class PlatformManagerImpl implements PlatformManager {

    private final Log log = LogFactory.getLog(PlatformManagerImpl.class.getName());

    private static final String VALUE_PROCESSOR = PagerProcessor_platform.class.getName();

    private Pager valuePager;

    private final PlatformTypeDAO platformTypeDAO;

    private final PermissionManager permissionManager;

    private final AgentDAO agentDAO;

    private final ServerManager serverManager;

    private final CPropManager cpropManager;

    private final ResourceManager resourceManager;

    private final ResourceGroupManager resourceGroupManager;

    private final AuthzSubjectManager authzSubjectManager;

    private final ServiceManager serviceManager;

    private final ApplicationDAO applicationDAO;

    private final ConfigResponseDAO configResponseDAO;

    private final PlatformDAO platformDAO;

    private final ServerDAO serverDAO;

    private final ServiceDAO serviceDAO;

    private final AuditManager auditManager;

    private final AgentManager agentManager;

    private final ZeventEnqueuer zeventManager;

    private final ResourceAuditFactory resourceAuditFactory;

    private final SRNManager srnManager;

    @Autowired
    public PlatformManagerImpl(PlatformTypeDAO platformTypeDAO,
                               PermissionManager permissionManager, AgentDAO agentDAO,
                               ServerManager serverManager, CPropManager cpropManager,
                               ResourceManager resourceManager,
                               ResourceGroupManager resourceGroupManager,
                               AuthzSubjectManager authzSubjectManager,
                               ServiceManager serviceManager, ApplicationDAO applicationDAO,
                               ConfigResponseDAO configResponseDAO, PlatformDAO platformDAO,
                               ServerDAO serverDAO, ServiceDAO serviceDAO,
                               AuditManager auditManager, AgentManager agentManager,
                               ZeventEnqueuer zeventManager, SRNManager srnManager,
            ResourceAuditFactory resourceAuditFactory) {
        this.platformTypeDAO = platformTypeDAO;
        this.permissionManager = permissionManager;
        this.agentDAO = agentDAO;
        this.serverManager = serverManager;
        this.cpropManager = cpropManager;
        this.resourceManager = resourceManager;
        this.resourceGroupManager = resourceGroupManager;
        this.authzSubjectManager = authzSubjectManager;
        this.serviceManager = serviceManager;
        this.applicationDAO = applicationDAO;
        this.configResponseDAO = configResponseDAO;
        this.platformDAO = platformDAO;
        this.serverDAO = serverDAO;
        this.serviceDAO = serviceDAO;
        this.auditManager = auditManager;
        this.agentManager = agentManager;
        this.zeventManager = zeventManager;
        this.resourceAuditFactory = resourceAuditFactory;
        this.srnManager = srnManager;
    }

    // TODO resolve circular dependency
    private AIQueueManager getAIQueueManager() {
        return Bootstrap.getBean(AIQueueManager.class);
    }

    /**
     * Find a PlatformType by id
     * 
     * 
     */
    @Transactional(readOnly = true)
    public PlatformType findPlatformType(Integer id) throws ObjectNotFoundException {
        return platformTypeDAO.findById(id);
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
        PlatformType ptype = platformTypeDAO.findByName(type);
        if (ptype == null) {
            throw new PlatformNotFoundException(type);
        }
        return ptype;
    }

    /**
     * @return {@link PlatformType}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<PlatformType> findAllPlatformTypes() {
        return platformTypeDAO.findAll();
    }

    /**
     * @return {@link PlatformType}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<PlatformType> findSupportedPlatformTypes() {
        Collection<PlatformType> platformTypes = findAllPlatformTypes();

        for (Iterator<PlatformType> it = platformTypes.iterator(); it.hasNext();) {
            PlatformType pType = it.next();
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
    public Collection<PlatformType> findUnsupportedPlatformTypes() {
        Collection<PlatformType> platformTypes = findAllPlatformTypes();

        for (Iterator<PlatformType> it = platformTypes.iterator(); it.hasNext();) {
            PlatformType pType = it.next();
            if (PlatformDetector.isSupportedPlatform(pType.getName())) {
                it.remove();
            }
        }
        return platformTypes;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Resource findResource(PlatformType pt) {

        ResourceType rType;

        String typeName = AuthzConstants.platformPrototypeTypeName;
        try {
            rType = resourceManager.findResourceTypeByName(typeName);
        } catch (NotFoundException e) {
            throw new SystemException(e);
        }
        return resourceManager.findResourceByInstanceId(rType, pt.getId());
    }

    /**
     * Find all platform types
     * 
     * @return List of PlatformTypeValues
     * 
     */
    @Transactional(readOnly = true)
    public PageList<PlatformTypeValue> getAllPlatformTypes(AuthzSubject subject, PageControl pc) {
        Collection<PlatformType> platTypes = platformTypeDAO.findAllOrderByName();
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
        Platform p;
        String typeName;

        if (id.isService()) {
            // look up the service
            Service service = serviceDAO.get(id.getId());

            if (service == null) {
                throw new ServiceNotFoundException(id);
            }

            p = service.getServer().getPlatform();
            typeName = service.getServiceType().getName();
        } else if (id.isServer()) {
            // look up the server
            Server server = serverDAO.get(id.getId());

            if (server == null) {
                throw new ServerNotFoundException(id);
            }
            p = server.getPlatform();
            typeName = server.getServerType().getName();
        } else if (id.isPlatform()) {
            p = findPlatformById(id.getId());
            typeName = p.getPlatformType().getName();
        } else if (id.isGroup()) {
            ResourceGroup g = resourceGroupManager.findResourceGroupById(id.getId());
            return g.getResourcePrototype().getName();
        } else {
            throw new IllegalArgumentException("Unsupported entity type: " + id);
        }

        if (id.isPlatform()) {
            return typeName;
        } else {
            return typeName + " " + p.getPlatformType().getName();
        }
    }

    /**
     * Delete a platform
     * 
     * @param subject The user performing the delete operation.
     * @param platform - The Platform
     * 
     */
    public void removePlatform(AuthzSubject subject, Platform platform)
        throws PlatformNotFoundException, PermissionException, VetoException {
        final AppdefEntityID aeid = platform.getEntityId();
        final Resource r = platform.getResource();
        final int resourceId = r.getId();
        final Audit audit = resourceAuditFactory.deleteResource(resourceManager
            .findResourceById(AuthzConstants.authzHQSystem), subject, 0, 0);
        boolean pushed = false;
        final Agent agent = platform.getAgent();
        try {
            auditManager.pushContainer(audit);
            pushed = true;
            permissionManager.checkRemovePermission(subject, platform.getEntityId());
            // keep the configresponseId so we can remove it later
            ConfigResponseDB config = platform.getConfigResponse();
            // don't want a proxy obj, since it will never be null
            config = (config.getId() == null) ? null : configResponseDAO.get(config.getId());
            removeServerReferences(platform);

            // this flush ensures that the server's platform_id is set to null
            // before the platform is deleted and the servers cascaded
            platformDAO.getSession().flush();
            getAIQueueManager().removeAssociatedAIPlatform(platform);
            cleanupAgent(platform);
            platform.getIps().clear();
            platformDAO.remove(platform);
            if (config != null) {
                configResponseDAO.remove(config);
            }
            cpropManager.deleteValues(aeid.getType(), aeid.getID());
            resourceManager.removeAuthzResource(subject, aeid, r);
            platformDAO.getSession().flush();
            if ( agent.getPlatforms().size() == 0) {
            	if(log.isDebugEnabled()){
                    log.debug("Removing agent " + agent.getAddress() + ":" + agent.getPort() +
                            " as there are no more platforms left for agent to service.");
            	}

                agentManager.removeAgent(agent);
            }
            zeventManager.enqueueEventAfterCommit(new ResourceDeletedZevent(subject, aeid, resourceId));

        } catch (PermissionException e) {
            log.debug("Error while removing Platform");

            throw e;
        } finally {
            if (pushed) {
                auditManager.popContainer(false);
            }
        }
    }

    private void cleanupAgent(Platform platform) {
        final Agent agent = platform.getAgent();
        if (agent == null) {
            return;
        }
        final Collection<Platform> platforms = agent.getPlatforms();
        
        for (final Iterator<Platform> it = platforms.iterator(); it.hasNext();) {
            final Platform p = it.next();
            if (p == null) {
                continue;
            }
         
            if (p.getId().equals(platform.getId())) {
                it.remove();
            }
        }
    }

    private void removeServerReferences(Platform platform) {

        final Collection<Server> servers = platform.getServersBag();
        // since we are using the hibernate collection
        // we need to synchronize
        synchronized (servers) {
            for (final Iterator<Server> i = servers.iterator(); i.hasNext();) {
                try {
                    // this looks funky but the idea is to pull the server
                    // obj into the session so that it is updated when flushed
                    final Server server = serverManager.findServerById(i.next().getId());
                    // there are instances where we may have a duplicate
                    // autoinventory identifier btwn platforms
                    // (sendmail, ntpd, CAM Agent Server, etc...)
                    final String uniqAiid = server.getPlatform().getId() +
                                            server.getAutoinventoryIdentifier();
                    server.setAutoinventoryIdentifier(uniqAiid);
                    server.setPlatform(null);
                    i.remove();
                } catch (ServerNotFoundException e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }

    public void handleResourceDelete(Resource resource) {
        resource.setResourceType(null);
    }

    /**
     * Create a Platform of a specified type
     */
    public Platform createPlatform(AuthzSubject subject, Integer platformTypeId,
                                   PlatformValue pValue, Integer agentPK)
        throws ValidationException, PermissionException, AppdefDuplicateNameException,
        AppdefDuplicateFQDNException, ApplicationException {
        // check if the object already exists

        if (platformDAO.findByName(pValue.getName()) != null) {
            // duplicate found, throw a duplicate object exception
            throw new AppdefDuplicateNameException();
        }
        if (platformDAO.findByFQDN(pValue.getFqdn()) != null) {
            // duplicate found, throw a duplicate object exception
            throw new AppdefDuplicateFQDNException();
        }

        try {

            ConfigResponseDB config;
            Platform platform;
            Agent agent = null;

            if (agentPK != null) {
                agent = agentDAO.findById(agentPK);
            }
            if (pValue.getConfigResponseId() == null) {
                config = configResponseDAO.createPlatform();
            } else {
                config = configResponseDAO.findById(pValue.getConfigResponseId());
            }

            trimStrings(pValue);
            validateNewPlatform(pValue);
            PlatformType pType = findPlatformType(platformTypeId);

            pValue.setOwner(subject.getName());
            pValue.setModifiedBy(subject.getName());

            platform = pType.create(pValue, agent, config);
            platformDAO.save(platform); // To setup its ID
            // AUTHZ CHECK
            // in order to succeed subject has to be in a role
            // which allows creating of authz resources
            createAuthzPlatform(subject, platform);

            // Create the virtual server types
            for (ServerType st : pType.getServerTypes()) {

                if (st.isVirtual()) {
                    serverManager.createVirtualServer(subject, platform, st);
                }
            }

            platformDAO.getSession().flush();

            NewResourceEvent event = new NewResourceEvent(null,platform.getResource());
            zeventManager.enqueueEventAfterCommit(event,2);
            // Send resource create event
            // Send resource create & increment platform count events
            zeventManager.enqueueEventAfterCommit(new ResourceCreatedZevent(subject, platform.getEntityId()));

            return platform;
        } catch (NotFoundException e) {
            throw new ApplicationException("Unable to find PlatformType: " + platformTypeId +
                                           " : " + e.getMessage());
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
    public Platform createPlatform(AuthzSubject subject, AIPlatformValue aipValue) throws ApplicationException {
        PlatformType platType = platformTypeDAO.findByName(aipValue.getPlatformTypeName());

        if (platType == null) {
            throw new SystemException("Unable to find PlatformType [" +
                    aipValue.getPlatformTypeName() + "]");
        }
        Platform checkP = platformDAO.findByName(aipValue.getName());
        if (checkP != null) {
            throwDupPlatform(checkP.getId(), aipValue.getName());
        }

        Agent agent = agentDAO.findByAgentToken(aipValue.getAgentToken());

        if (agent == null) {
            throw new ApplicationException("Unable to find agent: " + aipValue.getAgentToken());
        }

        ConfigResponseDB config = configResponseDAO.createPlatform();

        Platform platform = platType.create(aipValue, subject.getName(), config, agent);
        agent.getPlatforms().add(platform);
        platformDAO.save(platform);

        // AUTHZ CHECK
        try {
            createAuthzPlatform(subject, platform);
        } catch (Exception e) {
            throw new SystemException(e);
        }

        NewResourceEvent event = new NewResourceEvent(null,platform.getResource());
        zeventManager.enqueueEventAfterCommit(event,2);
        // Send resource create & increment platform count events
        zeventManager.enqueueEventAfterCommit(new ResourceCreatedZevent(subject, platform.getEntityId()));
        
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

        Collection<Platform> platforms;

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
        Collection<Platform> platforms = platformDAO.findByCTime(System.currentTimeMillis() - range);
        // now get the list of PKs
        Collection<Integer> viewable = new HashSet<Integer>(getViewablePlatformPKs(subject));
        // and iterate over the list to remove any item not viewable
        for (Iterator<Platform> i = platforms.iterator(); i.hasNext();) {
            Platform platform = i.next();
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
     */
    @Transactional(readOnly = true)
    public Platform getPlatformById(Integer id) {
        if (id == null) {
            return null;
        }
        return platformDAO.get(id);
    }

    /**
     * Get platform light value by id.
     */
    @Transactional(readOnly = true)
    public Platform getPlatformById(AuthzSubject subject, Integer id)
        throws PlatformNotFoundException, PermissionException {
        Platform platform = findPlatformById(id);
        permissionManager.checkViewPermission(subject, platform.getEntityId());
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
    public Platform findPlatformById(Integer id) throws PlatformNotFoundException {
        Platform platform = platformDAO.get(id);

        if (platform == null) {
            throw new PlatformNotFoundException(id);
        }

        // Make sure that resource is loaded as to not get
        // LazyInitializationException
        platform.getName();

        return platform;
    }
    
    @Transactional(readOnly = true)
    public Platform findPlatformByAIPlatform(AuthzSubject subject, AIPlatformValue aiPlatform) 
        throws PermissionException, PlatformNotFoundException {
        Platform p =  platformDAO.findByFQDN(aiPlatform.getFqdn());
        if(p == null) {
            final AIIpValue[] ipvals = aiPlatform.getAIIpValues();
            // Find by IP address. For now, if we get one IP address
            // match (and it isn't localhost), we assume that it is
            // the same platform. In the future, we are probably going
            // to need to do better.
            for (AIIpValue qip : ipvals) {
                String address = qip.getAddress();
                // XXX This is a hack that we need to get rid of
                // at some point. The idea is simple. Every platform
                // has the localhost address. So, if we are looking
                // for a platform based on IP address, searching for
                // localhost doesn't give us any information. Long
                // term, when we are trying to match all addresses,
                // this can go away.
                if (address.equals(NetFlags.LOOPBACK_ADDRESS) && (ipvals.length > 1)) {
                    continue;
                }
    
                Collection<Platform> platforms = platformDAO.findByIpAddr(address);
    
                Set<Platform> platformsMatchingIp = new HashSet<Platform>();
                if (!platforms.isEmpty()) {               
                    for (Platform plat : platforms) {
                        // Make sure the types match
                        if (!plat.getPlatformType().getName().equals(
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
            permissionManager.checkViewPermission(subject, p.getEntityId());
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
    public Platform getPlatformByAIPlatform(AuthzSubject subject, AIPlatformValue aiPlatform)
        throws PermissionException {
        Platform p = null;

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
                if (address.equals(NetFlags.LOOPBACK_ADDRESS) && (i < (ipvals.length - 1))) {
                    continue;
                }

                Collection<Platform> platforms = platformDAO.findByIpAddr(address);

                if (!platforms.isEmpty()) {
                    Platform ipMatch = null;

                    for (Platform plat : platforms) {

                        // Make sure the types match
                        if (!plat.getPlatformType().getName().equals(aiPlatform.getPlatformTypeName())) {
                            continue;
                        }

                        // If we got any platforms that match this IP address, then
                        // we just take it and see if we can match up more criteria.
                        // We can assume that is a candidate for the platform we are
                        // looking for. This should only fall apart if we have
                        // multiple platforms defined for the same IP address, which
                        // should be a rarity.

                        if (plat.getFqdn().equals(fqdn)) { // Perfect
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
            p = platformDAO.findByFQDN(fqdn);
        }

        String agentToken = aiPlatform.getAgentToken();
        if (p == null) {
            p = getPhysPlatformByAgentToken(agentToken);
        }

        if (p != null) {
            permissionManager.checkViewPermission(subject, p.getEntityId());
            if (porker && // Let agent porker
                // create new platforms
                !(p.getFqdn().equals(fqdn) || p.getCertdn().equals(certdn) || p.getAgent()
                    .getAgentToken().equals(agentToken))) {
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
    public Platform getPhysPlatformByAgentToken(String agentToken) {
        try {

            Agent agent = agentManager.getAgent(agentToken);
            Collection<Platform> platforms = agent.getPlatforms();
            for (Platform platform : platforms) {

                String platType = platform.getPlatformType().getName();
                // need to check if the platform is not a platform device
                if (PlatformDetector.isSupportedPlatform(platType)) {
                    return platform;
                }
            }
        } catch (AgentNotFoundException e) {
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

    private boolean platformMatchesAllIps(Platform p, List<AIIpValue> ips) {
        Collection<Ip> platIps = p.getIps();
        if (platIps.size() != ips.size()) {
            return false;
        }
        Set<String> ipSet = new HashSet<String>();
        for (AIIpValue ip : ips) {

            ipSet.add(ip.getAddress());
        }
        for (Ip ip : platIps) {
            if (!ipSet.contains(ip.getAddress())) {
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
        Platform p = platformDAO.findByName(name);
        if (p == null) {
            throw new PlatformNotFoundException("platform " + name + " not found");
        }
        // now check if the user can see this at all
        permissionManager.checkViewPermission(subject, p.getEntityId());
        return p.getPlatformValue();
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Platform getPlatformByName(String name) {
        return platformDAO.findBySortName(name);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Platform getPlatformByResourceId(int id) {
        return platformDAO.findByResourceId(id);
    }

    /**
     * Get the Platform that has the specified Fqdn
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Platform findPlatformByFqdn(AuthzSubject subject, String fqdn)
        throws PlatformNotFoundException, PermissionException {
        Platform p;
        try {
            p = platformDAO.findByFQDN(fqdn);
        } catch (NonUniqueResultException e) {
            p = null;
        }
        if (p == null) {
            throw new PlatformNotFoundException("Platform with fqdn " + fqdn + " not found");
        }
        // now check if the user can see this at all
        permissionManager.checkViewPermission(subject, p.getEntityId());
        return p;
    }

    /**
     * Get the Collection of platforms that have the specified Ip address
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Platform> getPlatformByIpAddr(AuthzSubject subject, String address)
        throws PermissionException {
        return platformDAO.findByIpAddr(address);
    }

    @Transactional(readOnly = true)
    public Collection<Platform> getPlatformByMacAddr(AuthzSubject subject, String address)
        throws PermissionException {

        // TODO: Add permission check

        return platformDAO.findByMacAddr(address);
    }

    @Transactional(readOnly = true)
    public Platform getAssociatedPlatformByMacAddress(AuthzSubject subject, Resource r)
        throws PermissionException, PlatformNotFoundException {
        // TODO: Add permission check

        ResourceType rt = r.getResourceType();
        if ((rt != null) && !rt.getId().equals(AuthzConstants.authzPlatform)) {
            throw new PlatformNotFoundException("Invalid resource type = " + rt.getName());
        }

        Platform platform = findPlatformById(r.getInstanceId());
        String macAddr = getPlatformMacAddress(platform);
        Collection<Platform> platforms = getPlatformByMacAddr(subject, macAddr);
        Platform associatedPlatform = null;

        for (Platform p : platforms) {
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

    private String getPlatformMacAddress(Platform platform) {
        // TODO: Should this method be part of the Platform object?

        String macAddress = null;

        for (Ip ip : platform.getIps()) {
            if (!"00:00:00:00:00:00".equals(ip.getMacAddress())) {
                macAddress = ip.getMacAddress();
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
        Collection<Platform> platforms = platformDAO.findByAgentToken(agentToken);
        if ((platforms == null) || (platforms.size() == 0)) {
            throw new PlatformNotFoundException("Platform with agent token " + agentToken +
                                                " not found");
        }

        List<Integer> pks = new ArrayList<Integer>();
        for (Platform plat : platforms) {
            pks.add(plat.getId());
        }
        return pks;
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
        Platform p = platformDAO.findByServiceId(serviceId);
        if (p == null) {
            throw new PlatformNotFoundException("platform for service " + serviceId + " not found");
        }
        // now check if the user can see this at all
        permissionManager.checkViewPermission(subject, p.getEntityId());
        return p.getPlatformValue();
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
        Platform p = platformDAO.findByServiceId(serviceId);
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
        Server server = serverDAO.get(serverId);

        if ((server == null) || (server.getPlatform() == null)) {
            // This should throw server not found. Servers always have
            // platforms..
            throw new PlatformNotFoundException("platform for server " + serverId + " not found");
        }

        Platform p = server.getPlatform();
        permissionManager.checkViewPermission(subject, p.getEntityId());
        return p.getPlatformValue();
    }

    /**
     * Get the platform ID for a server.
     * 
     * 
     * @param serverId Server ID.
     */
    @Transactional(readOnly = true)
    public Integer getPlatformIdByServer(Integer serverId) throws PlatformNotFoundException {
        Server server = serverDAO.get(serverId);

        if (server == null) {
            throw new PlatformNotFoundException("platform for server " + serverId + " not found");
        }

        return server.getPlatform().getId();
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

        List<Platform> foundPlats = platformDAO.findByServers(ids);

        ArrayList<Platform> platforms = new ArrayList<Platform>();
        for (Platform platform : foundPlats) {
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

        Application appLocal = applicationDAO.get(appId);
        if (appLocal == null) {
            throw new ApplicationNotFoundException(appId);
        }

        Collection<PlatformValue> platCollection = new ArrayList<PlatformValue>();
        // XXX Call to authz, get the collection of all services
        // that we are allowed to see.
        // OR, alternatively, find everything, and then call out
        // to authz in batches to find out which ones we are
        // allowed to return.

        Collection<AppService> serviceCollection = appLocal.getAppServices();
        Iterator<AppService> it = serviceCollection.iterator();
        while ((it != null) && it.hasNext()) {
            AppService appService = it.next();

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
                Integer serviceId = appService.getService().getId();
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

                return obj.getSortName();

            }

            public int compare(AppdefResourceType o1, AppdefResourceType o2) {
                return getName(o1).compareTo(getName(o2));
            }
        });
        return rtn;
    }

    protected Collection<Integer> getViewablePlatformPKs(AuthzSubject who)
    throws PermissionException, NotFoundException {
        // now get a list of all the viewable items
        Operation op = getOperationByName(resourceManager
            .findResourceTypeByName(AuthzConstants.platformResType),
            AuthzConstants.platformOpViewPlatform);
        return permissionManager.findOperationScopeBySubject(who, op.getId());
    }

    /**
     * Find an operation by name inside a ResourcetypeValue object
     */
    protected Operation getOperationByName(ResourceType rtV, String opName)
        throws PermissionException {
        Collection<Operation> ops = rtV.getOperations();
        for (Operation op : ops) {
            if (op.getName().equals(opName)) {
                return op;
            }
        }
        throw new PermissionException("Operation: " + opName + " not valid for ResourceType: " +
                                      rtV.getName());
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

            Collection<Platform> platforms = platformDAO.findByType(platTypeId);
            Collection<Integer> platIds = new ArrayList<Integer>();

            // now get the list of PKs
            Collection<Integer> viewable = getViewablePlatformPKs(subject);
            // and iterate over the list to remove any item not in the
            // viewable list
            for (Platform platform : platforms) {

                if (viewable.contains(platform.getId())) {
                    // remove the item, user cant see it
                    platIds.add(platform.getId());
                }
            }

            return platIds.toArray(new Integer[0]);
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
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public List<Platform> getPlatformsByType(AuthzSubject subject, String type)
        throws PermissionException, InvalidAppdefTypeException {
        try {
            PlatformType ptype = platformTypeDAO.findByName(type);
            if (ptype == null) {
                return new PageList<Platform>();
            }

            List<Platform> platforms = platformDAO.findByType(ptype.getId());
            if (platforms.size() == 0) {
                // There are no viewable platforms
                return platforms;
            }
            // now get the list of PKs
            Collection<Integer> viewable = getViewablePlatformPKs(subject);
            // and iterate over the List to remove any item not in the
            // viewable list
            for (Iterator<Platform> it = platforms.iterator(); it.hasNext();) {
                Platform platform = it.next();
                if (!viewable.contains(platform.getId())) {
                    // remove the item, user can't see it
                    it.remove();
                }
            }

            return platforms;
        } catch (NotFoundException e) {
            // There are no viewable platforms
            return new PageList<Platform>();
        }
    }
    
    @Transactional(readOnly=true)
    public Collection<Platform> findAll(AuthzSubject superUser) throws PermissionException {
        permissionManager.checkIsSuperUser(superUser);
        return platformDAO.findAll();
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
    protected Collection<Platform> getViewablePlatforms(AuthzSubject whoami, PageControl pc)
        throws PermissionException, NotFoundException {
        // first find all, based on the sorting attribute passed in, or
        // with no sorting if the page control is null
        Collection<Platform> platforms;
        // if page control is null, find all platforms
        if (pc == null) {
            platforms = platformDAO.findAll();
        } else {
            pc = PageControl.initDefaults(pc, SortAttribute.RESOURCE_NAME);
            int attr = pc.getSortattribute();
            switch (attr) {
                case SortAttribute.RESOURCE_NAME:
                    platforms = platformDAO.findAll_orderName(pc.isAscending());
                    break;
                case SortAttribute.CTIME:
                    platforms = platformDAO.findAll_orderCTime(pc.isAscending());
                    break;
                default:
                    throw new NotFoundException("Invalid sort attribute: " + attr);
            }
        }
        // now get the list of PKs
        Set<Integer> viewable = new HashSet<Integer>(getViewablePlatformPKs(whoami));
        // and iterate over the List to remove any item not in the
        // viewable list
        for (Iterator<Platform> i = platforms.iterator(); i.hasNext();) {
            Platform platform = i.next();
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
        Collection<Platform> platforms = platformDAO.findByIpAddr(addr);
        if (platforms.size() == 0) {
            return new PageList<PlatformValue>();
        }
        return valuePager.seek(platforms, pc);
    }

    /**
     * @param subj
     * @param pType platform type
     * @param regEx regex which matches either the platform fqdn or the
     *        resource sortname XXX scottmf need to add permission checking
     * 
     */
    @Transactional(readOnly = true)
    public List<Platform> findPlatformPojosByTypeAndName(AuthzSubject subj, Integer pType,
                                                         String regEx) {
        return platformDAO.findByTypeAndRegEx(pType, regEx);
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
    public List<Platform> findParentPlatformPojosByNetworkRelation(AuthzSubject subj,
                                                                   List<Integer> platformTypeIds,
                                                                   String platformName,
                                                                   Boolean hasChildren) {
        List<PlatformType> unsupportedPlatformTypes = new ArrayList<PlatformType>(
            findUnsupportedPlatformTypes());
        List<Integer> pTypeIds = new ArrayList<Integer>();

        if ((platformTypeIds != null) && !platformTypeIds.isEmpty()) {
            for (Integer pTypeId : platformTypeIds) {

                PlatformType pType = findPlatformType(pTypeId);
                if (unsupportedPlatformTypes.contains(pType)) {
                    pTypeIds.add(pTypeId);
                }
            }
            if (pTypeIds.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
        } else {
            // default values
            for (PlatformType pType : unsupportedPlatformTypes) {

                pTypeIds.add(pType.getId());
            }
        }

        return platformDAO.findParentByNetworkRelation(pTypeIds, platformName, hasChildren);
    }

    /**
     * @param subj
     * @param platformTypeIds List<Integer> of platform type ids
     * @return a list of {@link Platform}s
     * 
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Platform> findPlatformPojosByNoNetworkRelation(AuthzSubject subj,
                                                               List<Integer> platformTypeIds,
                                                               String platformName) {
        List<PlatformType> supportedPlatformTypes = new ArrayList<PlatformType>(
            findSupportedPlatformTypes());
        List<Integer> pTypeIds = new ArrayList<Integer>();

        if ((platformTypeIds != null) && !platformTypeIds.isEmpty()) {
            for (Integer pTypeId : platformTypeIds) {

                PlatformType pType = findPlatformType(pTypeId);
                if (supportedPlatformTypes.contains(pType)) {
                    pTypeIds.add(pTypeId);
                }
            }
            if (pTypeIds.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
        } else {
            // default values
            for (PlatformType pType : supportedPlatformTypes) {

                pTypeIds.add(pType.getId());
            }
        }

        return platformDAO.findByNoNetworkRelation(pTypeIds, platformName);
    }

    /**
     * Get the platforms that have an IP with the specified address.
     * 
     * @return a list of {@link Platform}s
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Platform> findPlatformPojosByIpAddr(String addr) {
        return platformDAO.findByIpAddr(addr);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Platform> findDeletedPlatforms() {
        return platformDAO.findDeletedPlatforms();
    }

    /**
     * Update an existing Platform. Requires all Ip's to have been re-added via
     * the platformValue.addIpValue(IpValue) method due to bug 4924
     * 
     * @param existing - the value object for the platform you want to save
     * 
     */
    public Platform updatePlatformImpl(AuthzSubject subject, PlatformValue existing)
        throws UpdateException, PermissionException, AppdefDuplicateNameException,
        PlatformNotFoundException, AppdefDuplicateFQDNException, ApplicationException {
        permissionManager.checkPermission(subject, existing.getEntityId(),
            AuthzConstants.platformOpModifyPlatform);
        existing.setModifiedBy(subject.getName());
        existing.setMTime(new Long(System.currentTimeMillis()));
        trimStrings(existing);

        Platform plat = platformDAO.findById(existing.getId());
        String oldName = plat.getName();

        if (existing.getCpuCount() == null) {
            // cpu count is no longer an option in the UI
            existing.setCpuCount(plat.getCpuCount());
        }

        Map<String, String> changedProps = plat.changedProperties(existing);
        if (changedProps.isEmpty()) {
            log.debug("No changes found between value object and entity");
            return plat;
        } else {
            if (!(existing.getName().equals(plat.getName()))) {
                if (platformDAO.findByName(existing.getName()) != null) {
                    // duplicate found, throw a duplicate object exception
                    throw new AppdefDuplicateNameException();
                }
            }

            if (!(existing.getFqdn().equals(plat.getFqdn()))) {
                if (platformDAO.findByFQDN(existing.getFqdn()) != null) {
                    // duplicate found, throw a duplicate object exception
                    throw new AppdefDuplicateFQDNException();
                }
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
                    for (IpValue ipVal : ipVals) {
                        AIIpValue aiIpVal = new AIIpValue();
                        aiIpVal.setAddress(ipVal.getAddress());
                        aiIpVal.setNetmask(ipVal.getNetmask());
                        aiIpVal.setMACAddress(ipVal.getMACAddress());
                        aiPlatform.addAIIpValue(aiIpVal);
                    }

                    getAIQueueManager().queue(subject, aiPlatform, false, false, true);

                } else if (!plat.getAgent().equals(existing.getAgent())) {
                    // Need to enqueue the ResourceUpdatedZevent if the
                    // agent changed to get the metrics scheduled
                    resourceManager.resourceHierarchyUpdated(subject, Collections.singletonList(plat.getResource()));
                }
            }
            Resource r = plat.getResource();
            this.zeventManager.enqueueEventAfterCommit(new ResourceContentChangedZevent(r.getId(),existing.getName(),
                    null, changedProps, oldName));
            platformDAO.updatePlatform(plat, existing);
            return plat;
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
     * Create the Authz resource and verify that the subject has the
     * createPlatform permission.
     * 
     * @param subject - the user creating
     */
    private void createAuthzPlatform(AuthzSubject subject, Platform platform)
        throws PermissionException, NotFoundException {
        log.debug("Begin Authz CreatePlatform");
        // check to make sure the user has createPlatform permission
        // on the root resource type
        permissionManager.checkCreatePlatformPermission(subject);

        ResourceType platProtoType = resourceManager
            .findResourceTypeByName(AuthzConstants.platformPrototypeTypeName);
        Resource proto = resourceManager.findResourceByInstanceId(platProtoType, platform
            .getPlatformType().getId());
        log.debug("User has permission to create platform. " + "Adding AuthzResource");
        Resource resource = resourceManager.createResource(subject, resourceManager
            .findResourceTypeByName(AuthzConstants.platformResType), proto, platform.getId(),
            platform.getName(), false, null);
        platform.setResource(resource);
    }

    /**
     * DevNote: This method was refactored out of updatePlatformTypes. It does
     * not work.
     * 
     * 
     */
    public void deletePlatformType(PlatformType pt) throws VetoException {

        Resource proto = resourceManager.findResourceByInstanceId(
            AuthzConstants.authzPlatformProto, pt.getId());
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        try {
            log.debug("Removing PlatformType: " + pt.getName());

            resourceGroupManager.removeGroupsCompatibleWith(proto);

            // Remove all platforms
            for (Platform platform : pt.getPlatforms()) {

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

        // TODO: Need to remove the Resource prototype associated with this
        // platform.
        platformTypeDAO.remove(pt);

        resourceManager.removeResource(overlord, proto);
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
        for (PlatformTypeInfo info : infos) {
            infoMap.put(info.getName(), info);
        }

        Collection<PlatformType> curPlatforms = platformTypeDAO.findByPlugin(plugin);

        for (PlatformType ptlocal : curPlatforms) {

            String localName = ptlocal.getName();
            PlatformTypeInfo pinfo = infoMap.remove(localName);

            // See if this exists
            if (pinfo == null) {
                deletePlatformType(ptlocal);
            } else {
                String curName = ptlocal.getName();
                String newName = pinfo.getName();

                // Just update it
                log.debug("Updating PlatformType: " + localName);

                if (!newName.equals(curName)) {
                    ptlocal.setName(newName);
                }
            }
        }

        // Now create the left-overs
        for (PlatformTypeInfo pinfo : infoMap.values()) {
            createPlatformType(pinfo.getName(), plugin);
        }
    }

    public PlatformType createPlatformType(String name, String plugin) throws NotFoundException {
        log.debug("Creating new PlatformType: " + name);
        Resource prototype = resourceManager.findRootResource();
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        PlatformType pt = platformTypeDAO.create(name, plugin);
        resourceManager.createResource(overlord, resourceManager
            .findResourceTypeByName(AuthzConstants.platformPrototypeTypeName), prototype, pt
            .getId(), pt.getName(), false, null);
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

        Platform platform = this.getPlatformByAIPlatform(subj, aiplatform);
        if (platform == null) {
            throw new PlatformNotFoundException("Platform not found with either FQDN: " + fqdn +
                                                " nor CertDN: " + certdn);
        }

        // Get the FQDN before we update
        String prevFqdn = platform.getFqdn();
        String oldName = platform.getName();
        platform.updateWithAI(aiplatform, subj.getName(), platform.getResource());
        Map<String, String> changedProperties = platform.changedProperties(aiplatform);
        if (!changedProperties.isEmpty()) {
            this.zeventManager.enqueueEventAfterCommit(new ResourceContentChangedZevent(aiplatform.getId(),
                    aiplatform.getName(), null, changedProperties, oldName));
        }
        // If FQDN has changed, we need to update servers' auto-inventory tokens
        if (!prevFqdn.equals(platform.getFqdn())) {
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

    private void updateAgentIps(AuthzSubject subj, AIPlatformValue aiplatform, Platform platform) {
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
      
        //[HHQ-5955]- indicate whether agent IP exist in ipList 
        boolean isIpInList = isIpInList(ips, agent.getAddress());
        
        for (AIIpValue ip : ips) {

            if ((ip.getQueueStatus() == AIQueueConstants.Q_STATUS_REMOVED) &&
                agent.getAddress().equals(ip.getAddress())) {
                changeAgentIp = true;
                break;
            }
            
            /*[HHQ-5955]- When IP is changed frequently we can experience a situation where the agent IP has been removed from AIQ ipList.
              Although the agent IP does not exist in ipList this is an IP changed scenario. 
              So we need to check whether the current IP queue status is Removed and the agent IP does not exist in IP list.*/
            if ((ip.getQueueStatus() == AIQueueConstants.Q_STATUS_REMOVED) && !isIpInList){
                log.debug("changeAgentIp(): Agent IP doesn't exist in IP list but find IP with status Removed [" + ip.getAddress() + "]. change agent IP = true");
                changeAgentIp = true;
                break;
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
                if ((ip.getQueueStatus() != AIQueueConstants.Q_STATUS_ADDED) &&
                (// Q_STATUS_PLACEHOLDER in this context simply means that
                // the ip is already associated with the platform and there
                // is no change. Therefore it needs to be part of our checks
                ip.getQueueStatus() != AIQueueConstants.Q_STATUS_PLACEHOLDER)) {
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
    
    /**
     * Check whether IP exists in IP list. Fix [HHQ-5955]. 
     * @param ipList IP list
     * @param address IP
     * @return true if IP exists in IP list, otherwise false
     * @author tgoldman
     */
    private boolean isIpInList (List<AIIpValue> ipList, String address){
        boolean isIpInList = false;
        
        /* Check for null */
        if (address == null){
            return isIpInList;
        }
        for (AIIpValue ipValue : ipList){
            if (ipValue != null && ipValue.getAddress().equals(address)){
                isIpInList = true;
                break;
            }
        }
        
        if (log.isDebugEnabled()){
            log.debug("isIpInList(): isIpInList = [" + isIpInList + "]");
        }
        return isIpInList;
    }
    
    private void enableMeasurements(AuthzSubject subj, Platform platform) {
        List<AppdefEntityID> eids = new ArrayList<AppdefEntityID>();
        eids.add(platform.getEntityId());
        Collection<Server> servers = platform.getServers();
        for (Server server : servers) {

            eids.add(server.getEntityId());
            Collection<Service> services = server.getServices();
            for (Service service : services) {

                eids.add(service.getEntityId());
            }
        }
        srnManager.scheduleInBackground(eids, true, true);
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
        return platform.addIp(address, netmask, macAddress);
    }

    /**
     * Update an IP on a platform
     * 
     * 
     */
    public Ip updateIp(Platform platform, String address, String netmask, String macAddress) {
        return platform.updateIp(address, netmask, macAddress);
    }

    /**
     * Remove an IP on a platform
     * 
     * 
     */
    public void removeIp(Platform platform, String address, String netmask, String macAddress) {
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

    @Transactional(readOnly = true)
    public Platform getPlatformByAgentId(Integer agentId) {
        final Agent agent = agentDAO.get(agentId);
        if (agent == null) {
            return null;
        }
        final Collection<Platform> platforms = agent.getPlatforms();
        for (final Platform platform : platforms) {
            final Resource resource = platform.getResource();
            if (PlatformDetector.isSupportedPlatform(resource.getPrototype().getName())) {
                return platform;
            }
        }
        return null;
    }
    
    public void removePlatformVmMapping(AuthzSubject subject, List<String> macAddresses) throws PermissionException {
        for (String mac : macAddresses) {
            Collection<Platform> platforms = this.getPlatformByMacAddr(subject, mac);
            if ((platforms==null) || platforms.isEmpty()) {
                if (log.isDebugEnabled()) { log.debug("no platform in the system is assosiated to the " + mac + " mac address"); }
                continue;
            }
            List<ResourceContentChangedZevent> events = new ArrayList<ResourceContentChangedZevent>(platforms.size());
          
            for (Platform platform : platforms) {
                try {
                    AppdefEntityID id = platform.getEntityId();
                    int typeId = platform.getAppdefResourceType().getId().intValue();                 
                    this.cpropManager.setValue(id, typeId, HQConstants.MOID, "");
                    this.cpropManager.setValue(id, typeId, HQConstants.VCUUID, "");
                    Map<String,String> changedProps = new HashMap<String,String>();
                    changedProps.put(HQConstants.MOID, null);
                    changedProps.put(HQConstants.VCUUID, null);
                    ResourceContentChangedZevent contentChangedEvent = new ResourceContentChangedZevent(
                            platform.getId(),null,null,changedProps, null);
                    events.add(contentChangedEvent);

                }catch(Throwable t) {
                  log.warn(t,t);
                }
                this.zeventManager.enqueueEventsAfterCommit(events);
            }
        }
    }

    public void mapUUIDToPlatforms(AuthzSubject subject, List<VmMapping> mapping) throws PermissionException, CPropKeyNotFoundException {
        for(VmMapping vmMap : mapping) {
            for (String mac : vmMap.getMacs().split(";")) {
                if ("00:00:00:00:00:00".equals(mac)) { continue; }
                Collection<Platform> platforms = this.getPlatformByMacAddr(subject, mac);
                if ((platforms==null) || platforms.isEmpty()) {
                    if (log.isDebugEnabled()) { log.debug("no platform in the system is assosiated to the " + mac + " mac address"); }
                    continue;
                }

                // there should only be 2 platforms
                boolean platformUUIDUpdated = false;
                List<ResourceContentChangedZevent> events = new ArrayList<ResourceContentChangedZevent>(platforms.size());
                for(Platform platform:platforms) {
                    try {
                        // only map the UUID for actual platforms, not for virtual ones discovered by the vc plugin
                        if (AuthzConstants.platformPrototypeVmwareVsphereVm.equals(platform.getResource().getPrototype().getName())) { continue; }
                        AppdefEntityID id = platform.getEntityId();
                        int typeId = platform.getAppdefResourceType().getId().intValue();
                        String moref = vmMap.getMoId();
                        String vcUUID = vmMap.getVcUUID();
                        this.cpropManager.setValue(id, typeId, HQConstants.MOID, moref);
                        this.cpropManager.setValue(id, typeId, HQConstants.VCUUID, vcUUID);
                        Map<String,String> changedProps = new HashMap<String,String>();
                        changedProps.put(HQConstants.MOID,moref);
                        changedProps.put(HQConstants.VCUUID,vcUUID);
                        ResourceContentChangedZevent contentChangedEvent = new 
                                ResourceContentChangedZevent(platform.getId(),null,null,changedProps, null);
                        events.add(contentChangedEvent);
                        platformUUIDUpdated=true;
                    } catch (AppdefEntityNotFoundException e) { log.error(e); }
                }
                this.zeventManager.enqueueEventsAfterCommit(events);

                // assume one mac address is sufficient for VM-platform mapping
                if (platformUUIDUpdated) { break;}
            }
        }
        //TODO~ check if updates DB by the end of the transaction
        //TODO~ make sure the uuid is extracted in the resource mapper for platforms
    }

    public Collection<Platform> getOrphanedPlatforms() {
        return platformDAO.getOrphanedPlatforms();
    }
    
    
}
