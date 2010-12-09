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
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
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
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.ResourceAuditFactory;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.reference.RelationshipTypes;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Server objects in appdef and their
 * relationships
 */
@org.springframework.stereotype.Service
@Transactional
public class ServerManagerImpl implements ServerManager {

    private static final String AUTODISCOVERY_ZOMBIE = "autodiscoveryZombie";

    private static final String AUTO_INVENTORY_IDENTIFIER = "autoInventoryIdentifier";

    private static final String INSTALL_PATH = "installPath";

    private final Log log = LogFactory.getLog(ServerManagerImpl.class);

    private static final String VALUE_PROCESSOR = "org.hyperic.hq.appdef.server.session.PagerProcessor_server";
    private Pager valuePager;
    

    private PermissionManager permissionManager;
    private ResourceManager resourceManager;
    private AuditManager auditManager;
    private AuthzSubjectManager authzSubjectManager;
    private ResourceGroupManager resourceGroupManager;
    private ZeventEnqueuer zeventManager;
    private ResourceAuditFactory resourceAuditFactory;

    @Autowired
    public ServerManagerImpl(PermissionManager permissionManager,  ResourceManager resourceManager,
                              AuditManager auditManager,
                             AuthzSubjectManager authzSubjectManager, ResourceGroupManager resourceGroupManager,
                             ZeventEnqueuer zeventManager, ResourceAuditFactory resourceAuditFactory) {

        this.permissionManager = permissionManager;
        this.resourceManager = resourceManager;
        this.auditManager = auditManager;
        this.authzSubjectManager = authzSubjectManager;
        this.resourceGroupManager = resourceGroupManager;
        this.zeventManager = zeventManager;
        this.resourceAuditFactory = resourceAuditFactory;
    }

    /**
     * Filter a list of {@link Server}s by their viewability by the subject
     */
    protected List<Server> filterViewableServers(Collection<Resource> servers, AuthzSubject who) {
        List<Server> res = new ArrayList<Server>();
       

        try {
           
            //type = resourceManager.findResourceTypeByName(AuthzConstants.serverResType);
            //op = getOperationByName(type, AuthzConstants.serverOpViewServer);
        } catch (Exception e) {
            throw new SystemException("Internal error", e);
        }
        for (Resource s : servers) {

            //try {
                //TODO perm check
                //permissionManager.check(who.getId(), typeId, s.getId(), op.getId());
                res.add(toServer(s));
            //} catch (PermissionException e) {
                // Ok
            //}
        }
        return res;
    }

    /**
     * Validate a server value object which is to be created on this platform.
     * This method will check IP conflicts and any other special constraint
     * required to succesfully add a server instance to a platform
     */
    private void validateNewServer(Resource p, ServerValue sv) throws ValidationException {
        //TODO above validation already done when creating new Resource
        // ensure the server value has a server type
        String msg = null;
        if (sv.getServerType() == null) {
            msg = "Server has no ServiceType";
        } else if (sv.idHasBeenSet()) {
            msg = "This server is not new, it has ID:" + sv.getId();
        }
        if (msg == null) {
            Integer id = sv.getServerType().getId();
            Collection<ResourceType> stypes = p.getType().getResourceTypesFrom(RelationshipTypes.SERVER_TYPE);
            for (ResourceType sVal : stypes) {

                if (sVal.getId().equals(id)) {
                    return;
                }
            }
            msg = "Servers of type '" + sv.getServerType().getName() + "' cannot be created on platforms of type '" +
                  p.getType().getName() + "'";
        }
        if (msg != null) {
            throw new ValidationException(msg);
        }
    }

    /**
     * Get the scope of viewable servers for a given user
     * @param whoami - the user
     * @return List of ServerPK's for which subject has
     *         AuthzConstants.serverOpViewServer
     */
    protected List<Integer> getViewableServers(AuthzSubject whoami) throws PermissionException, NotFoundException {
        if (log.isDebugEnabled()) {
            log.debug("Checking viewable servers for subject: " + whoami.getName());
        }

        //OperationType op = getOperationByName(resourceManager.findResourceTypeByName(AuthzConstants.serverResType),
          //  AuthzConstants.serverOpViewServer);
       // List<Integer> idList = permissionManager.findOperationScopeBySubject(whoami, op.getId());
        
        Collection<Resource> servers = getAllServers();

        if (log.isDebugEnabled()) {
            log.debug("There are: " + servers.size() + " viewable servers");
        }
        List<Integer> keyList = new ArrayList<Integer>(servers.size());
        for (Resource server: servers) {
            keyList.add(server.getId());
        }
        return keyList;
    }
 
    private Collection<Resource> getAllServers() {
        Set<Resource> servers = new HashSet<Resource>();
        Collection<Resource> platforms = Resource.findRootResource().getResourcesFrom(RelationshipTypes.PLATFORM);
        for(Resource platform: platforms) {
            servers.addAll(platform.getResourcesFrom(RelationshipTypes.SERVER));
        }
        return servers;
    }
    
    private Resource create(ServerValue sv, Resource p) {
        Resource s = new Resource();
        s.setName(sv.getName());
        s.setDescription(sv.getDescription());
        s.setProperty(INSTALL_PATH,sv.getInstallPath());
        String aiid = sv.getAutoinventoryIdentifier();
        if (aiid != null) {
            s.setProperty(AUTO_INVENTORY_IDENTIFIER,sv.getAutoinventoryIdentifier());
        } else {
            // Server was created by hand, use a generated AIID. (This matches
            // the behaviour in 2.7 and prior)
            aiid = sv.getInstallPath() + "_" + System.currentTimeMillis() + "_" + sv.getName();
            s.setProperty(AUTO_INVENTORY_IDENTIFIER,aiid);
        }
      
        s.setProperty("servicesAutoManaged",sv.getServicesAutomanaged());
        s.setProperty("runtimeAutodiscovery",sv.getRuntimeAutodiscovery());
        s.setProperty("wasAutodiscovered",sv.getWasAutodiscovered());
        s.setProperty(AUTODISCOVERY_ZOMBIE,false);
        s.setLocation(sv.getLocation());
        s.setModifiedBy(sv.getModifiedBy());
        s.persist();
        p.relateTo(s,RelationshipTypes.SERVER);
       
        Integer stid = sv.getServerType().getId();
        ResourceType st = ResourceType.findResourceType(stid);
        s.setType(st);
        return s;
   }
    
    private Server toServer(Resource server) {
        //TODO
        return new Server(server.getId());
    }
    
    private ServerType toServerType(ResourceType serverType) {
        //TODO
        return new ServerType();
    }

    /**
     * Create a Server on the given platform.
     * 
     * @return ServerValue - the saved value object
     * 
     * 
     */
    public Server createServer(AuthzSubject subject, Integer platformId, Integer serverTypeId, ServerValue sValue)
        throws ValidationException, PermissionException, PlatformNotFoundException, AppdefDuplicateNameException,
        NotFoundException {
        //try {
            trimStrings(sValue);
            
            //TODO perm checking
            //permissionManager
            //.checkPermission(subject, resourceManager.findResourceTypeByName(AuthzConstants.platformResType), platformId, 
              //  AuthzConstants.platformOpAddServer);

            Resource platform = Resource.findResource(platformId);
            ResourceType serverType = ResourceType.findResourceType(serverTypeId);

            sValue.setServerType(toServerType(serverType).getServerTypeValue());
            sValue.setOwner(subject.getName());
            sValue.setModifiedBy(subject.getName());

            // validate the object
            validateNewServer(platform, sValue);

            // create it
            Resource server = create(sValue, platform);

            //TODO abstract to ResourceManager when we can send events w/out AppdefEntityIDs
            Server serv = toServer(server);
            ResourceCreatedZevent zevent = new ResourceCreatedZevent(subject, serv.getEntityId());
            zeventManager.enqueueEventAfterCommit(zevent);

            return serv;
            // } catch (CreateException e) {
            // throw e;
        //} catch (NotFoundException e) {
        //    throw new NotFoundException("Unable to find platform=" + platformId + " or server type=" + serverTypeId +
         //                               ":" + e.getMessage());
        //}
    }

  
    /**
     * A removeServer method that takes a ServerLocal. Used by
     * PlatformManager.removePlatform when cascading removal to servers.
     * 
     */
    public void removeServer(AuthzSubject subject, Server server) throws PermissionException, VetoException {
        final Audit audit = resourceAuditFactory.deleteResource(resourceManager
            .findResourceById(AuthzConstants.authzHQSystem), subject, 0, 0);
        boolean pushed = false;

        try {
            auditManager.pushContainer(audit);
            pushed = true;
            //TODO virtual?
            //if (!server.getType().isVirtual()) {
              //  permissionManager.checkRemovePermission(subject, server.getEntityId());
            //}

            //TODO remove services         
            //config, cprops, and relationships will get cleaned up by removal here
            resourceManager.removeResource(subject, Resource.findResource(server.getId()));
           
        } finally {
            if (pushed) {
                auditManager.popContainer(true);
            }
        }
    }

   

    /**
     * Find all server types
     * @return list of serverTypeValues
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ServerTypeValue> getAllServerTypes(AuthzSubject subject, PageControl pc) {
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        //TODO order by name
        return valuePager.seek(getAllServerTypes(), pc);
    }
    
    private Set<ServerType> getAllServerTypes() {
        Set<ServerType> serverTypes = new HashSet<ServerType>();
        Set<ResourceType> resourceTypes = getAllServerResourceTypes();
        for(ResourceType serverType: resourceTypes) {
            serverTypes.add(toServerType(serverType));
        }
        return serverTypes;
    }
    private Set<ResourceType> getAllServerResourceTypes() {
        Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
        Collection<ResourceType> platformTypes = ResourceType.findRootResourceType().getResourceTypesFrom(RelationshipTypes.PLATFORM_TYPE);
        for(ResourceType platformType:platformTypes) {
            resourceTypes.addAll(platformType.getResourceTypesFrom(RelationshipTypes.SERVER_TYPE));
        }
        return resourceTypes;
    }
    
    private Collection<ServerType> getServerTypes(final List<Integer> serverIds, final boolean asc) {
        //TODO from ServerDAO
        return null;
    }

    /**
     * Find viewable server types
     * @return list of serverTypeValues
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ServerTypeValue> getViewableServerTypes(AuthzSubject subject, PageControl pc)
        throws PermissionException, NotFoundException {
        // build the server types from the visible list of servers
        final List<Integer> authzPks = getViewableServers(subject);
        final Collection<ServerType> serverTypes =getServerTypes(authzPks, true);
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverTypes, pc);
    }

    /**
     * Find viewable server types for a platform
     * @return list of serverTypeValues
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ServerTypeValue> getServerTypesByPlatform(AuthzSubject subject, Integer platId,
                                                              PageControl pc)
        throws PermissionException, PlatformNotFoundException, ServerNotFoundException {

        // build the server types from the visible list of servers
        Collection<Server> servers = getServersByPlatformImpl(subject, platId, null,
             pc);

        Collection<ServerType> serverTypes = filterResourceTypes(servers);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverTypes, pc);
    }

    /**
     * Find all ServerTypes for a givent PlatformType id.
     * 
     * This can go once we begin passing POJOs to the UI layer.
     * 
     * @return A list of ServerTypeValue objects for thie PlatformType.
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ServerTypeValue> getServerTypesByPlatformType(AuthzSubject subject, Integer platformTypeId,
                                                                  PageControl pc) throws PlatformNotFoundException {
        ResourceType platType = ResourceType.findResourceType(platformTypeId);

        Collection<ResourceType> resourceTypes = platType.getResourceTypesFrom(RelationshipTypes.SERVER_TYPE);
        Set<ServerType> serverTypes = new HashSet<ServerType>();
        for(ResourceType resourceType: resourceTypes) {
            serverTypes.add(toServerType(resourceType));
        }

        return valuePager.seek(serverTypes, pc);
    }
    
    private Resource findServerByAIID(Resource platform, String aiid) {
        Collection<Resource> servers = platform.getResourcesFrom(RelationshipTypes.SERVER);
        for(Resource server: servers) {
            if(server.getProperty(AUTO_INVENTORY_IDENTIFIER).equals(aiid)) {
                return server;
            }
        }
        return null;
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public Server findServerByAIID(AuthzSubject subject, Platform platform, String aiid) throws PermissionException {
        //TODO perm check
        //permissionManager.checkViewPermission(subject, platform.getId());
        return toServer(findServerByAIID(Resource.findResource(platform.getId()), aiid));
    }

    /**
     * Find a Server by Id.
     * 
     */
    @Transactional(readOnly=true)
    public Server findServerById(Integer id) throws ServerNotFoundException {
        Server server = getServerById(id);

        if (server == null) {
            throw new ServerNotFoundException(id);
        }

        return server;
    }

    /**
     * Get a Server by Id.
     * 
     * @return The Server with the given id, or null if not found.
     */
    @Transactional(readOnly=true)
    public Server getServerById(Integer id) {
        return toServer(Resource.findResource(id));
    }

    /**
     * Find a ServerType by id
     * 
     */
    @Transactional(readOnly=true)
    public ServerType findServerType(Integer id) {
        return toServerType(ResourceType.findResourceType(id));
    }

    /**
     * Find a server type by name
     * @param name - the name of the server
     * @return ServerTypeValue
     * 
     */
    @Transactional(readOnly=true)
    public ServerType findServerTypeByName(String name) throws NotFoundException {
        ResourceType type = ResourceType.findResourceTypeByName(name);
        if (type == null) {
            throw new NotFoundException("name not found: " + name);
        }
        return toServerType(type);
    }
    
    @Transactional(readOnly=true)
    public List<Server> findServersByType(Platform p, ServerType st) {
        List<Server> servers = new ArrayList<Server>();
        Resource platResource = Resource.findResource(p.getId());
        Collection<Resource> relatedServers = platResource.getResourcesFrom(RelationshipTypes.SERVER);
        for(Resource server: relatedServers) {
            if(st.equals(server.getType())) {
                servers.add(toServer(server));
            }
        }
        return servers;
    }
    
    private List<Server> findServersByType(Resource platform, ResourceType serverType) {
        List<Server> servers = new ArrayList<Server>();
        Collection<Resource> relatedServers = platform.getResourcesFrom(RelationshipTypes.SERVER);
        for(Resource server: relatedServers) {
            if(serverType.equals(server.getType())) {
                servers.add(toServer(server));
            }
        }
        return servers;
    }
    
    private List<Server> findByPlatformOrderName(Resource platform) {
        List<Server> servers = new ArrayList<Server>();
        Collection<Resource> relatedServers = platform.getResourcesFrom(RelationshipTypes.SERVER);
        for(Resource server: relatedServers) {
                servers.add(toServer(server));
        }
        //TODO order
        return servers;
    }
    

    /**
     * Get server lite value by id. Does not check permission.
     * 
     */
    @Transactional(readOnly=true)
    public Server getServerById(AuthzSubject subject, Integer id) throws ServerNotFoundException, PermissionException {
        Server server = findServerById(id);
        //TODO
        //permissionManager.checkViewPermission(subject, server.getId());
        return server;
    }

    /**
     * /** Get server IDs by server type.
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @return An array of Server IDs.
     */
    @Transactional(readOnly=true)
    public Integer[] getServerIds(AuthzSubject subject, Integer servTypeId) throws PermissionException {

        try {

            Collection<Resource> servers = ResourceType.findResourceType(servTypeId).getResources();
            if (servers.size() == 0) {
                return new Integer[0];
            }
            List<Integer> serverIds = new ArrayList<Integer>(servers.size());

            // now get the list of PKs
            Collection<Integer> viewable = getViewableServers(subject);
            // and iterate over the List to remove any item not in the
            // viewable list
            int i = 0;
            for (Iterator<Resource> it = servers.iterator(); it.hasNext(); i++) {
                Resource server = it.next();
                if (viewable.contains(server.getId())) {
                    // add the item, user can see it
                    serverIds.add(server.getId());
                }
            }

            return (Integer[]) serverIds.toArray(new Integer[0]);
        } catch (NotFoundException e) {
            // There are no viewable servers
            return new Integer[0];
        }
    }
 
    /**
     * Get server by service.
     * 
     */
    @Transactional(readOnly=true)
    public ServerValue getServerByService(AuthzSubject subject, Integer sID) throws ServerNotFoundException,
        ServiceNotFoundException, PermissionException {
        Resource svc = Resource.findResource(sID);
        Resource s = svc.getResourceTo(RelationshipTypes.SERVICE);
        //TODO
        //permissionManager.checkViewPermission(subject, s.getId());
        return toServer(s).getServerValue();
    }

    /**
     * Get server by service. The virtual servers are not filtere out of
     * returned list.
     * 
     */
    @Transactional(readOnly=true)
    public PageList<ServerValue> getServersByServices(AuthzSubject subject, List<AppdefEntityID> sIDs)
        throws PermissionException, ServerNotFoundException {
        Set<Resource> servers = new HashSet<Resource>();
        for (AppdefEntityID svcId : sIDs) {

            Resource svc = Resource.findResource(svcId.getId());

            servers.add(svc.getResourceTo(RelationshipTypes.SERVICE));
        }

        return valuePager.seek(filterViewableServers(servers, subject), null);
    }

    /**
     * 
     */
    @Transactional(readOnly=true)
    public Collection<Server> getViewableServers(AuthzSubject subject, Platform platform) {
        Resource platformRes = Resource.findResource(platform.getId());
        return filterViewableServers(platformRes.getResourcesFrom(RelationshipTypes.SERVER), subject);
    }

    private Collection<Server> getServersByPlatformImpl(AuthzSubject subject, Integer platId, Integer servTypeId,
                                                        PageControl pc)
        throws PermissionException, ServerNotFoundException, PlatformNotFoundException {
        List<Integer> authzPks;
        try {
            authzPks = getViewableServers(subject);
        } catch (NotFoundException exc) {
            throw new ServerNotFoundException("No (viewable) servers associated with platform " + platId);
        }

        List<Server> servers;
        // first, if they specified a server type, then filter on it
        if (servTypeId != null) {
            servers = findServersByType(Resource.findResource(platId), ResourceType.findResourceType(servTypeId));
            
        } else {
            servers = findByPlatformOrderName(Resource.findResource(platId));
            
        }
        for (Iterator<Server> i = servers.iterator(); i.hasNext();) {
            Server aServer = i.next();

            // Remove the server if its not viewable
            if (!authzPks.contains(aServer.getId())) {
                i.remove();
            }
        }

        // If sort descending, then reverse the list
        if (pc != null && pc.isDescending()) {
            Collections.reverse(servers);
        }

        return servers;
    }

    /**
     * Get servers by platform.
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param platId platform id.
     * @param excludeVirtual true if you dont want virtual (fake container)
     *        servers in the returned list
     * @param pc The page control.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly=true)
    public PageList<ServerValue> getServersByPlatform(AuthzSubject subject, Integer platId, 
                                                      PageControl pc) throws ServerNotFoundException,
        PlatformNotFoundException, PermissionException {
        return getServersByPlatform(subject, platId, null,  pc);
    }

    /**
     * Get servers by server type and platform.
     * 
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @param platId platform id.
     * @param pc The page control.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly=true)
    public PageList<ServerValue> getServersByPlatform(AuthzSubject subject, Integer platId, Integer servTypeId,
                                                       PageControl pc)
        throws ServerNotFoundException, PlatformNotFoundException, PermissionException {
        Collection<Server> servers = getServersByPlatformImpl(subject, platId, servTypeId,  pc);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(servers, pc);
    }

    /**
     * Get servers by server type and platform.
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param platId platform id.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly=true)
    public PageList<ServerValue> getServersByPlatformServiceType(AuthzSubject subject, Integer platId, Integer svcTypeId)
        throws ServerNotFoundException, PlatformNotFoundException, PermissionException {
        PageControl pc = PageControl.PAGE_ALL;
        Integer servTypeId;
        try {
            ResourceType typeV = ResourceType.findResourceType(svcTypeId);
            servTypeId = typeV.getResourceTypeTo(RelationshipTypes.SERVICE_TYPE).getId();
        } catch (ObjectNotFoundException e) {
            throw new ServerNotFoundException("Service Type not found", e);
        }

        Collection<Server> servers = getServersByPlatformImpl(subject, platId, servTypeId,  pc);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(servers, pc);
    }
    
    
    /**
     * Get server IDs by server type and platform.
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @param platId platform id.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly=true)
    public Integer[] getServerIdsByPlatform(AuthzSubject subject, Integer platId, Integer servTypeId) throws ServerNotFoundException,
        PlatformNotFoundException, PermissionException {
        Collection<Server> servers = getServersByPlatformImpl(subject, platId, servTypeId, null);

        Integer[] ids = new Integer[servers.size()];
        Iterator<Server> it = servers.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Server server = it.next();
            ids[i] = server.getId();
        }

        return ids;
    }

    /**
     * Get servers by application and serverType.
     * 
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    @Transactional(readOnly=true)
    private Collection<Server> getServersByApplicationImpl(AuthzSubject subject, Integer appId, Integer servTypeId)
        throws ServerNotFoundException, ApplicationNotFoundException, PermissionException {

        List<Integer> authzPks;
        ResourceGroup appLocal;

        try {
            appLocal = ResourceGroup.findResourceGroup(appId);
        } catch (ObjectNotFoundException exc) {
            throw new ApplicationNotFoundException(appId, exc);
        }

        try {
            authzPks = getViewableServers(subject);
        } catch (NotFoundException e) {
            throw new ServerNotFoundException("No (viewable) servers " + "associated with " + "application " + appId, e);
        }

        HashMap<Integer, Server> serverCollection = new HashMap<Integer, Server>();

        // XXX - a better solution is to control the viewable set returned by
        // ql finders. This will be forthcoming.

        Collection<Resource> appServiceCollection = appLocal.getMembers();
        Iterator<Resource> it = appServiceCollection.iterator();

        while (it.hasNext()) {

            Resource appService = it.next();
            //TODO

//            if (appService.isIsGroup()) {
//                Collection<Service> services = serviceManager.getServiceCluster(appService.getResourceGroup())
//                    .getServices();
//
//                Iterator<Service> serviceIterator = services.iterator();
//                while (serviceIterator.hasNext()) {
//                    Service service = serviceIterator.next();
//                    Server server = service.getServer();
//
//                    // Don't bother with entire cluster if type is platform svc
//                    if (server.getServerType().isVirtual()) {
//                        break;
//                    }
//
//                    Integer serverId = server.getId();
//
//                    if (serverCollection.containsKey(serverId)) {
//                        continue;
//                    }
//
//                    serverCollection.put(serverId, server);
//                }
//            } else {
            //TODO making assumption that all group members are services here
                Server server = toServer(appService.getResourceTo(RelationshipTypes.SERVICE));
                
                    Integer serverId = server.getId();

                    if (serverCollection.containsKey(serverId))
                        continue;

                    serverCollection.put(serverId, server);
                
            //}
        }

        for (Iterator<Map.Entry<Integer, Server>> i = serverCollection.entrySet().iterator(); i.hasNext();) {
            Map.Entry<Integer, Server> entry = i.next();
            Server aServer = entry.getValue();

            // first, if they specified a server type, then filter on it
            if (servTypeId != null && !(aServer.getServerType().getId().equals(servTypeId))) {
                i.remove();
            }
            // otherwise, remove the server if its not viewable
            else if (!authzPks.contains(aServer.getId())) {
                i.remove();
            }
        }

        return serverCollection.values();
    }

    /**
     * Get servers by application.
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @param pc The page control for this page list.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    @Transactional(readOnly=true)
    public PageList<ServerValue> getServersByApplication(AuthzSubject subject, Integer appId, PageControl pc)
        throws ServerNotFoundException, ApplicationNotFoundException, PermissionException {
        return getServersByApplication(subject, appId, null, pc);
    }

    /**
     * Get servers by application and serverType.
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @param pc The page control for this page list.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    @Transactional(readOnly=true)
    public PageList<ServerValue> getServersByApplication(AuthzSubject subject, Integer appId, Integer servTypeId,
                                                         PageControl pc) throws ServerNotFoundException,
        ApplicationNotFoundException, PermissionException {
        Collection<Server> serverCollection = getServersByApplicationImpl(subject, appId, servTypeId);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverCollection, pc);
    }

    /**
     * Get server IDs by application and serverType.
     * 
     * 
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    @Transactional(readOnly=true)
    public Integer[] getServerIdsByApplication(AuthzSubject subject, Integer appId, Integer servTypeId)
        throws ServerNotFoundException, ApplicationNotFoundException, PermissionException {
        Collection<Server> servers = getServersByApplicationImpl(subject, appId, servTypeId);

        Integer[] ids = new Integer[servers.size()];
        Iterator<Server> it = servers.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Server server = it.next();
            ids[i] = server.getId();
        }

        return ids;
    }
    
    private boolean matchesValueObject(ServerValue existing, Resource server) {
        //TODO from Server
        return true;
    }
    
    private void updateServer(ServerValue existing, Resource server) {
        //TODO from Server
    }

    /**
     * Update a server
     * @param existing
     * 
     */
    public Server updateServer(AuthzSubject subject, ServerValue existing) throws PermissionException, UpdateException,
        AppdefDuplicateNameException, ServerNotFoundException {
        try {
            Resource server = Resource.findResource(existing.getId());
            //TODO perm check
            //permissionManager.checkModifyPermission(subject, server.getId());
            existing.setModifiedBy(subject.getName());
            existing.setMTime(new Long(System.currentTimeMillis()));
            trimStrings(existing);

            if (matchesValueObject(existing,server)) {
                log.debug("No changes found between value object and entity");
            } else {
                if (!existing.getName().equals(server.getName())) {
                   
                    server.setName(existing.getName());
                }

                updateServer(existing,server);
            }
            return toServer(server);
        } catch (ObjectNotFoundException e) {
            throw new ServerNotFoundException(existing.getId(), e);
        }
    }

    /**
     * Update server types
     * 
     */
    public void updateServerTypes(String plugin, ServerTypeInfo[] infos) throws VetoException, NotFoundException {
        // First, put all of the infos into a Hash
        HashMap<String, ServerTypeInfo> infoMap = new HashMap<String, ServerTypeInfo>();
        for (int i = 0; i < infos.length; i++) {
            String name = infos[i].getName();
            ServerTypeInfo sinfo = infoMap.get(name);

            if (sinfo == null) {
                // first time we've seen this type
                // clone it incase we have to update the platforms
                infoMap.put(name, (ServerTypeInfo) infos[i].clone());
            } else {
                // already seen this type; just update the platforms.
                // this allows server types of the same name to support
                // different families of platforms in the plugins.
                String[] platforms = (String[]) ArrayUtil.merge(sinfo.getValidPlatformTypes(), infos[i]
                    .getValidPlatformTypes(), new String[0]);
                sinfo.setValidPlatformTypes(platforms);
            }
        }

        Collection<ResourceType> curServers = ResourceType.findByPlugin(plugin);

        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        for (ResourceType serverType : curServers) {

            String serverName = serverType.getName();
            ServerTypeInfo sinfo = (ServerTypeInfo) infoMap.remove(serverName);

            if (sinfo == null) {
                deleteServerType(serverType, overlord, resourceGroupManager, resourceManager);
            } else {
                String curDesc = serverType.getDescription();
                Collection<ResourceType> curPlats = serverType.getResourceTypesTo(RelationshipTypes.SERVER_TYPE);
                String newDesc = sinfo.getDescription();
                String[] newPlats = sinfo.getValidPlatformTypes();
                boolean updatePlats;

                log.debug("Updating ServerType: " + serverName);

                if (!newDesc.equals(curDesc)) {
                    serverType.setDescription(newDesc);
                }

                // See if we need to update the supported platforms
                updatePlats = newPlats.length != curPlats.size();
                if (updatePlats == false) {
                    // Ensure that the lists are the same
                    for (ResourceType pLocal : curPlats) {

                        int j;

                        for (j = 0; j < newPlats.length; j++) {
                            if (newPlats[j].equals(pLocal.getName()))
                                break;
                        }
                        if (j == newPlats.length) {
                            updatePlats = true;
                            break;
                        }
                    }
                }

                if (updatePlats == true) {
                    findAndSetPlatformType(newPlats, serverType);
                }
            }
        }

       

        // Now create the left-overs
        for (ServerTypeInfo sinfo : infoMap.values()) {
            createServerType(sinfo,plugin);
        }
    }
    
    public ServerType createServerType(ServerTypeInfo sinfo, String plugin) throws NotFoundException {
        ResourceType stype = new ResourceType();
        log.debug("Creating new ServerType: " + sinfo.getName());
        //TODO get plugin
        //stype.setPlugin(plugin);
        stype.setName(sinfo.getName());
        stype.setDescription(sinfo.getDescription());
        String newPlats[] = sinfo.getValidPlatformTypes();
        findAndSetPlatformType(newPlats, stype);

        stype.persist();
        return toServerType(stype);
    }

    /**
     * builds a list of resource types from the list of resources
     * @param resources - {@link Collection} of {@link AppdefResource}
     * @param {@link Collection} of {@link AppdefResourceType}
     */
    private Collection<ServerType> filterResourceTypes(Collection<Server> resources) {
        final Set<ServerType> resTypes = new HashSet<ServerType>();
        for (Server o : resources) {
            resTypes.add(o.getServerType());
        }
        final List<ServerType> rtn = new ArrayList<ServerType>(resTypes);
        Collections.sort(rtn, new Comparator<ServerType>() {
            private String getName(Object obj) {
                if (obj instanceof ServerType) {
                    //TODO was sortName
                    return ((ServerType) obj).getName();
                }
                return "";
            }

            public int compare(ServerType o1, ServerType o2) {
                return getName(o1).compareTo(getName(o2));
            }
        });
        return rtn;
    }

    /**
     * 
     */
    public void deleteServerType(ResourceType serverType, AuthzSubject overlord, ResourceGroupManager resGroupMan,
                                 ResourceManager resMan) throws VetoException {
        // Need to remove all service types

        ResourceType[] serviceTypes = (ResourceType[]) serverType.getResourceTypesFrom(RelationshipTypes.SERVICE_TYPE).toArray(new ResourceType[0]);
        for (ResourceType serviceType : serviceTypes) {
            serviceType.remove();
        }

        log.debug("Removing ServerType: " + serverType.getName());
        //Integer typeId = AuthzConstants.authzServerProto;
        //Resource proto = resMan.findResourceByInstanceId(typeId, serverType.getId());

        //try {
            //TODO remove compat groups?
            //resGroupMan.removeGroupsCompatibleWith(proto);

            // Remove all servers done by removing server type
            
       // } catch (PermissionException e) {
         //    assert false : "Overlord should not run into PermissionException";
        //}

        serverType.remove();
    }

    /**
     * 
     */
    public void setAutodiscoveryZombie(Server server, boolean zombie) {
        Resource.findResource(server.getId()).setProperty(AUTODISCOVERY_ZOMBIE,zombie);
    }

    /**
     * Get a Set of PlatformTypeLocal objects which map to the names as given by
     * the argument.
     */
    private void findAndSetPlatformType(String[] platNames, ResourceType stype) throws NotFoundException {

        for (int i = 0; i < platNames.length; i++) {
            ResourceType pType = ResourceType.findResourceTypeByName(platNames[i]);
            if (pType == null) {
                throw new NotFoundException("Could not find platform type '" + platNames[i] + "'");
            }
           pType.relateTo(stype, RelationshipTypes.SERVER_TYPE);
        }
    }

   

    /**
     * Trim all string attributes
     */
    private void trimStrings(ServerValue server) {
        if (server.getDescription() != null)
            server.setDescription(server.getDescription().trim());
        if (server.getInstallPath() != null)
            server.setInstallPath(server.getInstallPath().trim());
        if (server.getAutoinventoryIdentifier() != null)
            server.setAutoinventoryIdentifier(server.getAutoinventoryIdentifier().trim());
        if (server.getLocation() != null)
            server.setLocation(server.getLocation().trim());
        if (server.getName() != null)
            server.setName(server.getName().trim());
    }

    /**
     * Returns a list of 2 element arrays. The first element is the name of the
     * server type, the second element is the # of servers of that type in the
     * inventory.
     * 
     * 
     */
    @Transactional(readOnly=true)
    public List<Object[]> getServerTypeCounts() {
        Collection<ResourceType> serverTypes = getAllServerResourceTypes();
        List<Object[]> counts = new ArrayList<Object[]>();
        for(ResourceType serverType: serverTypes) {
            counts.add(new Object[]{serverType.getName(),serverType.getResources().size()});
        }
        return counts;
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {

        valuePager = Pager.getPager(VALUE_PROCESSOR);

    }
}
