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
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
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
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing Server objects in appdef and their
 * relationships
 */
@org.springframework.stereotype.Service
@Transactional
public class ServerManagerImpl implements ServerManager {

    private final Log log = LogFactory.getLog(ServerManagerImpl.class);

    private static final String VALUE_PROCESSOR = "org.hyperic.hq.appdef.server.session.PagerProcessor_server";
    private Pager valuePager;
    private static final Integer APPDEF_RES_TYPE_UNDEFINED = new Integer(-1);

    private PermissionManager permissionManager;
    private ApplicationDAO applicationDAO;
    private ConfigResponseDAO configResponseDAO;
    private PlatformDAO platformDAO;
    private ServerDAO serverDAO;
    private PlatformTypeDAO platformTypeDAO;
    private ResourceManager resourceManager;
    private ServerTypeDAO serverTypeDAO;
    private ServiceDAO serviceDAO;
    private ServiceTypeDAO serviceTypeDAO;
    private ServiceManager serviceManager;
    private CPropManager cpropManager;
    private ConfigManager configManager;
    private MeasurementManager measurementManager;
    private AuditManager auditManager;
    private AuthzSubjectManager authzSubjectManager;
    private ResourceGroupManager resourceGroupManager;
    private ZeventEnqueuer zeventManager;
    private ResourceAuditFactory resourceAuditFactory;

    @Autowired
    public ServerManagerImpl(PermissionManager permissionManager, ApplicationDAO applicationDAO,
            ConfigResponseDAO configResponseDAO, PlatformDAO platformDAO, ServerDAO serverDAO,
            PlatformTypeDAO platformTypeDAO, ResourceManager resourceManager, ServerTypeDAO serverTypeDAO,
            ServiceDAO serviceDAO, ServiceTypeDAO serviceTypeDAO, ServiceManager serviceManager,
            CPropManager cpropManager, ConfigManager configManager, MeasurementManager measurementManager,
            AuditManager auditManager, AuthzSubjectManager authzSubjectManager,
            ResourceGroupManager resourceGroupManager, ZeventEnqueuer zeventManager,
            ResourceAuditFactory resourceAuditFactory) {

        this.permissionManager = permissionManager;
        this.applicationDAO = applicationDAO;
        this.configResponseDAO = configResponseDAO;
        this.platformDAO = platformDAO;
        this.serverDAO = serverDAO;
        this.platformTypeDAO = platformTypeDAO;
        this.resourceManager = resourceManager;
        this.serverTypeDAO = serverTypeDAO;
        this.serviceDAO = serviceDAO;
        this.serviceTypeDAO = serviceTypeDAO;
        this.serviceManager = serviceManager;
        this.cpropManager = cpropManager;
        this.configManager = configManager;
        this.measurementManager = measurementManager;
        this.auditManager = auditManager;
        this.authzSubjectManager = authzSubjectManager;
        this.resourceGroupManager = resourceGroupManager;
        this.zeventManager = zeventManager;
        this.resourceAuditFactory = resourceAuditFactory;
    }

    /**
     * Validate a server value object which is to be created on this platform.
     * This method will check IP conflicts and any other special constraint
     * required to succesfully add a server instance to a platform
     */
    private void validateNewServer(Platform p, Server server) throws ValidationException {
        // ensure the server value has a server type
        String msg = null;
        if(server.getServerType() == null) {
            msg = "Server has no ServiceType";
        }else if(server.getId() != null) {
            msg = "This server is not new, it has ID:" + server.getId();
        }
        if(msg == null) {
            Integer id = server.getServerType().getId();
            Collection<ServerType> stypes = p.getPlatformType().getServerTypes();
            for(ServerType sVal:stypes) {

                if(sVal.getId().equals(id)) return;
            }
            msg = "Servers of type '" + server.getServerType().getName() + "' cannot be created on platforms of type '"
                    + p.getPlatformType().getName() + "'";
        }
        if(msg != null) {
            throw new ValidationException(msg);
        }
    }

    /**
     * Filter a list of {@link Server}s by their viewability by the subject
     */
    protected Collection<Integer> filterViewableServers(Collection<Server> servers, AuthzSubject who) {
        try {
            final ResourceType type = resourceManager.findResourceTypeByName(AuthzConstants.serverResType);
            final Operation op = getOperationByName(type, AuthzConstants.serverOpViewServer);
            final Collection<Integer> set = permissionManager.findOperationScopeBySubject(who, op.getId());
            final Set<Integer> rtn = new HashSet<Integer>(servers.size());
            for(final Server server:servers) {
                final Integer id = server.getId();
                if(set.contains(id)) {
                    rtn.add(id);
                }
            }
            return rtn;
        }catch(PermissionException e) {
            log.error(e);
            log.debug(e, e);
        }catch(NotFoundException e) {
            log.error(e);
            log.debug(e, e);
        }
        return Collections.emptyList();
    }

    /**
     * Validate a server value object which is to be created on this platform.
     * This method will check IP conflicts and any other special constraint
     * required to succesfully add a server instance to a platform
     */
    private void validateNewServer(Platform p, ServerValue sv) throws ValidationException {
        // ensure the server value has a server type
        String msg = null;
        if(sv.getServerType() == null) {
            msg = "Server has no ServiceType";
        }else if(sv.idHasBeenSet()) {
            msg = "This server is not new, it has ID:" + sv.getId();
        }
        if(msg == null) {
            Integer id = sv.getServerType().getId();
            Collection<ServerType> stypes = p.getPlatformType().getServerTypes();
            for(ServerType sVal:stypes) {

                if(sVal.getId().equals(id)) {
                    return;
                }
            }
            msg = "Servers of type '" + sv.getServerType().getName() + "' cannot be created on platforms of type '"
                    + p.getPlatformType().getName() + "'";
        }
        if(msg != null) {
            throw new ValidationException(msg);
        }
    }

    /**
     * Construct the new name of the server to be cloned to the target platform
     */
    private String getTargetServerName(Platform targetPlatform, Server serverToClone) {

        String prefix = serverToClone.getPlatform().getName();
        String oldServerName = serverToClone.getName();
        String newServerName = StringUtil.removePrefix(oldServerName, prefix);

        if(newServerName.equals(oldServerName)) {
            // old server name may not contain the canonical host name
            // of the platform. try to get just the host name
            int dotIndex = prefix.indexOf(".");
            if(dotIndex > 0) {
                prefix = prefix.substring(0, dotIndex);
                newServerName = StringUtil.removePrefix(oldServerName, prefix);
            }
        }

        newServerName = targetPlatform.getName() + " " + newServerName;

        return newServerName;
    }

    /**
     * Get the scope of viewable servers for a given user
     * 
     * @param whoami
     *            - the user
     * @return List of ServerPK's for which subject has
     *         AuthzConstants.serverOpViewServer
     */
    protected Collection<Integer> getViewableServers(AuthzSubject whoami) throws PermissionException, NotFoundException {
        if(log.isDebugEnabled()) {
            log.debug("Checking viewable servers for subject: " + whoami.getName());
        }

        Operation op = getOperationByName(resourceManager.findResourceTypeByName(AuthzConstants.serverResType),
                AuthzConstants.serverOpViewServer);
        Collection<Integer> idList = permissionManager.findOperationScopeBySubject(whoami, op.getId());

        if(log.isDebugEnabled()) {
            log.debug("There are: " + idList.size() + " viewable servers");
        }
        return idList;
    }

    /**
     * Move a Server to the given Platform
     * 
     * @param subject
     *            The user initiating the move.
     * @param target
     *            The target {@link org.hyperic.hq.appdef.server.session.Server}
     *            to move.
     * @param destination
     *            The destination {@link Platform}.
     * 
     * @throws org.hyperic.hq.authz.shared.PermissionException
     *             If the passed user does not have permission to move the
     *             Server.
     * @throws org.hyperic.hq.common.VetoException
     *             If the operation canot be performed due to incompatible
     *             types.
     * 
     * 
     */
    public void moveServer(AuthzSubject subject, Server target, Platform destination) throws VetoException,
            PermissionException {

        try {
            // Permission checking on destination

            permissionManager.checkPermission(subject,
                    resourceManager.findResourceTypeByName(AuthzConstants.platformResType), destination.getId(),
                    AuthzConstants.platformOpAddServer);

            // Permission check on target
            permissionManager.checkPermission(subject,
                    resourceManager.findResourceTypeByName(AuthzConstants.serverResType), target.getId(),
                    AuthzConstants.serverOpRemoveServer);
        }catch(NotFoundException e) {
            throw new VetoException("Caught NotFoundException checking permission: " + e.getMessage()); // notgonnahappen
        }

        // Ensure target can be moved to the destination
        if(!destination.getPlatformType().getServerTypes().contains(target.getServerType())) {
            throw new VetoException("Incompatible resources passed to move(), " + "cannot move server of type "
                    + target.getServerType().getName() + " to " + destination.getPlatformType().getName());

        }

        // Unschedule measurements
        measurementManager.disableMeasurements(subject, target.getResource());

        // Reset Server parent id
        target.setPlatform(destination);

        // Add/Remove Server from Server collections
        target.getPlatform().getServersBag().remove(target);
        destination.getServersBag().add(target);

        // Move Authz resource.
        resourceManager.moveResource(subject, target.getResource(), destination.getResource());

        // Flush server move
        serverDAO.getSession().flush();

        // Reschedule metrics
        ResourceUpdatedZevent zevent = new ResourceUpdatedZevent(subject, target.getEntityId());
        zeventManager.enqueueEventAfterCommit(zevent);

        // Must also move all dependent services so that ancestor edges are
        // rebuilt and that service metrics are re-scheduled
        ArrayList<Service> services = new ArrayList<Service>(); // copy list
        // since the
        // move will
        // modify the
        // server
        // collection.
        services.addAll(target.getServices());

        for(Service s:services) {

            serviceManager.moveService(subject, s, target);
        }
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
        try {
            trimStrings(sValue);

            Platform platform = platformDAO.findById(platformId);
            ServerType serverType = serverTypeDAO.findById(serverTypeId);

            sValue.setServerType(serverType.getServerTypeValue());
            sValue.setOwner(subject.getName());
            sValue.setModifiedBy(subject.getName());

            // validate the object
            validateNewServer(platform, sValue);

            // create it
            Server server = serverDAO.create(sValue, platform);

            // Add server to parent collection
            platform.getServersBag().add(server);

            createAuthzServer(subject, server);

            // Send resource create event
            ResourceCreatedZevent zevent = new ResourceCreatedZevent(subject, server.getEntityId());
            NewResourceEvent event = new NewResourceEvent(platform.getResource().getId(), server.getResource());
            zeventManager.enqueueEventAfterCommit(event,2);
            zeventManager.enqueueEventAfterCommit(zevent);

            return server;
            // } catch (CreateException e) {
            // throw e;
        }catch(NotFoundException e) {
            throw new NotFoundException("Unable to find platform=" + platformId + " or server type=" + serverTypeId
                    + ":" + e.getMessage());
        }
    }

    /**
     * Create a virtual server
     * 
     * @throws NotFoundException
     * 
     * @throws PermissionException
     * 
     */
    public Server createVirtualServer(AuthzSubject subject, Platform platform, ServerType st)
            throws PermissionException, NotFoundException {
        // First of all, make sure this is a virtual type
        if(!st.isVirtual()) {
            throw new IllegalArgumentException("createVirtualServer() called for non-virtual server type: "
                    + st.getName());
        }

        // Create a new ServerValue to fill in
        ServerValue sv = new ServerValue();
        sv.setServerType(st.getServerTypeValue());
        sv.setName(platform.getName() + " " + st.getName());
        sv.setInstallPath("/");
        sv.setServicesAutomanaged(false);
        sv.setRuntimeAutodiscovery(true);
        sv.setWasAutodiscovered(false);
        sv.setOwner(subject.getName());
        sv.setModifiedBy(subject.getName());

        Server server = serverDAO.create(sv, platform);

        // Add server to parent collection
        Collection<Server> servers = platform.getServersBag();
        if(!servers.contains(server)) {
            servers.add(server);
        }

        createAuthzServer(subject, server);
        return server;
    }

    /**
     * A removeServer method that takes a ServerLocal. Used by
     * PlatformManager.removePlatform when cascading removal to servers.
     * 
     */
    public void removeServer(AuthzSubject subject, Server server) throws PermissionException, VetoException {
        final AppdefEntityID aeid = server.getEntityId();
        final Resource r = server.getResource();
        final Audit audit = resourceAuditFactory.deleteResource(
                resourceManager.findResourceById(AuthzConstants.authzHQSystem), subject, 0, 0);
        boolean pushed = false;

        try {
            auditManager.pushContainer(audit);
            pushed = true;
            if(!server.getServerType().isVirtual()) {
                permissionManager.checkRemovePermission(subject, server.getEntityId());
            }

            // Service manager will update the collection, so we need to copy

            Collection<Service> services = server.getServices();
            synchronized (services) {
                for(final Iterator<Service> i = services.iterator();i.hasNext();) {
                    try {
                        // this looks funky but the idea is to pull the service
                        // obj into the session so that it is updated when
                        // flushed
                        final Service service = serviceManager.findServiceById(i.next().getId());
                        final String currAiid = service.getAutoinventoryIdentifier();
                        final Integer id = service.getId();
                        // ensure aiid remains unique
                        service.setAutoinventoryIdentifier(id + currAiid);
                        service.setServer(null);
                        i.remove();
                    }catch(ServiceNotFoundException e) {
                        log.warn(e);
                    }
                }
            }

            // this flush ensures that the service's server_id is set to null
            // before the server is deleted and the services cascaded
            serverDAO.getSession().flush();

            // Remove server from parent Platform Server collection.
            Platform platform = server.getPlatform();
            if(platform != null) {
                platform.getServersBag().remove(server);
            }

            // Remove Server from ServerType. If not done, results in an
            // ObjectDeletedException
            // when updating plugin types during plugin deployment
            server.getServerType().getServers().remove(server);

            // Keep config response ID so it can be deleted later.
            final ConfigResponseDB config = server.getConfigResponse();

            serverDAO.remove(server);

            // Remove the config response
            if(config != null) {
                configResponseDAO.remove(config);
            }
            cpropManager.deleteValues(aeid.getType(), aeid.getID());

            // Remove authz resource
            resourceManager.removeAuthzResource(subject, aeid, r);

            serverDAO.getSession().flush();
        }finally {
            if(pushed) {
                auditManager.popContainer(true);
            }
        }
    }

    /**
     * 
     */
    public void handleResourceDelete(Resource resource) {
        resource.setResourceType(null);
    }

    /**
     * Find all server types
     * 
     * @return list of serverTypeValues
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> getAllServerTypes(AuthzSubject subject, PageControl pc) {
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverTypeDAO.findAllOrderByName(), pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Server getServerByName(Platform host, String name) {
        return serverDAO.findByName(host, name);
    }

    /**
     * Find viewable server types
     * 
     * @return list of serverTypeValues
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> getViewableServerTypes(AuthzSubject subject, PageControl pc)
            throws PermissionException, NotFoundException {
        // build the server types from the visible list of servers
        final Collection<Integer> authzPks = getViewableServers(subject);
        final Collection<ServerType> serverTypes = serverDAO.getServerTypes(new ArrayList<Integer>(authzPks), true);
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverTypes, pc);
    }

    /**
     * Find viewable server non-virtual types for a platform
     * 
     * @return list of serverTypeValues
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> getServerTypesByPlatform(AuthzSubject subject, Integer platId, PageControl pc)
            throws PermissionException, PlatformNotFoundException, ServerNotFoundException {
        return getServerTypesByPlatform(subject, platId, true, pc);
    }

    /**
     * Find viewable server types for a platform
     * 
     * @return list of serverTypeValues
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> getServerTypesByPlatform(AuthzSubject subject, Integer platId,
            boolean excludeVirtual, PageControl pc) throws PermissionException, PlatformNotFoundException,
            ServerNotFoundException {

        // build the server types from the visible list of servers
        Collection<Server> servers = getServersByPlatformImpl(subject, platId, APPDEF_RES_TYPE_UNDEFINED,
                excludeVirtual, pc);

        Collection<AppdefResourceType> serverTypes = filterResourceTypes(servers);

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
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> getServerTypesByPlatformType(AuthzSubject subject, Integer platformTypeId,
            PageControl pc) throws PlatformNotFoundException {
        PlatformType platType = platformTypeDAO.findById(platformTypeId);

        Collection<ServerType> serverTypes = platType.getServerTypes();

        return valuePager.seek(serverTypes, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Server findServerByAIID(AuthzSubject subject, Platform platform, String aiid) throws PermissionException {
        permissionManager.checkViewPermission(subject, platform.getEntityId());
        return serverDAO.findServerByAIID(platform, aiid);
    }

    /**
     * Find a Server by Id.
     * 
     */
    @Transactional(readOnly = true)
    public Server findServerById(Integer id) throws ServerNotFoundException {
        Server server = getServerById(id);

        if(server == null) {
            throw new ServerNotFoundException(id);
        }

        return server;
    }

    /**
     * Get a Server by Id.
     * 
     * @return The Server with the given id, or null if not found.
     */
    @Transactional(readOnly = true)
    public Server getServerById(Integer id) {
        return serverDAO.get(id);
    }

    /**
     * Find a ServerType by id
     * 
     */
    @Transactional(readOnly = true)
    public ServerType findServerType(Integer id) {
        return serverTypeDAO.findById(id);
    }

    /**
     * Find a server type by name
     * 
     * @param name
     *            - the name of the server
     * @return ServerTypeValue
     * 
     */
    @Transactional(readOnly = true)
    public ServerType findServerTypeByName(String name) throws NotFoundException {
        ServerType type = serverTypeDAO.findByName(name);
        if(type == null) {
            throw new NotFoundException("name not found: " + name);
        }
        return type;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<Server> findServersByType(Platform p, ServerType st) {
        return serverDAO.findByPlatformAndType_orderName(p.getId(), st.getId());
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Server> findDeletedServers() {
        return serverDAO.findDeletedServers();
    }

    /**
     * Get server lite value by id. Does not check permission.
     * 
     */
    @Transactional(readOnly = true)
    public Server getServerById(AuthzSubject subject, Integer id) throws ServerNotFoundException, PermissionException {
        Server server = findServerById(id);
        permissionManager.checkViewPermission(subject, server.getEntityId());
        return server;
    }

    /**
     * /** Get server IDs by server type.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param servTypeId
     *            server type id.
     * @return An array of Server IDs.
     */
    @Transactional(readOnly = true)
    public Integer[] getServerIds(AuthzSubject subject, Integer servTypeId) throws PermissionException {

        try {

            Collection<Server> servers = serverDAO.findByType(servTypeId);
            if(servers.size() == 0) {
                return new Integer[0];
            }
            List<Integer> serverIds = new ArrayList<Integer>(servers.size());

            // now get the list of PKs
            Collection<Integer> viewable = getViewableServers(subject);
            // and iterate over the List to remove any item not in the
            // viewable list
            int i = 0;
            for(Iterator<Server> it = servers.iterator();it.hasNext();i++) {
                Server server = it.next();
                if(viewable.contains(server.getId())) {
                    // add the item, user can see it
                    serverIds.add(server.getId());
                }
            }

            return (Integer[]) serverIds.toArray(new Integer[0]);
        }catch(NotFoundException e) {
            // There are no viewable servers
            return new Integer[0];
        }
    }

    /**
     * Get server by service.
     * 
     */
    @Transactional(readOnly = true)
    public ServerValue getServerByService(AuthzSubject subject, Integer sID) throws ServerNotFoundException,
            ServiceNotFoundException, PermissionException {
        Service svc = serviceDAO.findById(sID);
        Server s = svc.getServer();
        permissionManager.checkViewPermission(subject, s.getEntityId());
        return s.getServerValue();
    }

    /**
     * Get server by service. The virtual servers are not filtere out of
     * returned list.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<Server> getServersByServices(AuthzSubject subject, List<AppdefEntityID> sIDs)
            throws PermissionException, ServerNotFoundException {
        Set<Server> servers = new HashSet<Server>();
        for(AppdefEntityID svcId:sIDs) {
            Service svc = serviceDAO.findById(svcId.getId());
            servers.add(svc.getServer());
        }
        final Collection<Integer> viewableIds = filterViewableServers(servers, subject);
        final Collection<Server> viewable = new ArrayList<Server>();
        for(final Integer id:viewableIds) {
            viewable.add(serverDAO.get(id));
        }
        return new PageList<Server>(viewable, viewable.size());
    }

    /**
     * Get all servers.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @return A List of ServerValue objects representing all of the servers
     *         that the given subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServerValue> getAllServers(AuthzSubject subject, PageControl pc) throws PermissionException,
            NotFoundException {
        Collection<Server> servers = getViewableServers(subject, pc);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(servers, pc);
    }

    /**
     * Get the scope of viewable servers for a given user
     * 
     * @param subject
     *            - the user
     * @return List of ServerLocals for which subject has
     *         AuthzConstants.serverOpViewServer
     */
    @Transactional(readOnly = true)
    private Collection<Server> getViewableServers(AuthzSubject subject, PageControl pc) throws PermissionException,
            NotFoundException {
        Collection<Server> servers;
        Collection<Integer> authzPks = getViewableServers(subject);
        int attr = -1;
        if(pc != null) {
            attr = pc.getSortattribute();
        }
        switch (attr) {
        case SortAttribute.RESOURCE_NAME:
            servers = getServersFromIds(authzPks, pc.isAscending());
            break;
        default:
            servers = getServersFromIds(authzPks, true);
            break;
        }
        return servers;
    }

    /**
     * @param serverIds
     *            {@link Collection} of {@link Server.getId}
     * @return {@link Collection} of {@link Server}
     */
    @Transactional(readOnly = true)
    private Collection<Server> getServersFromIds(Collection<Integer> serverIds, boolean asc) {
        final List<Server> rtn = new ArrayList<Server>(serverIds.size());
        for(Integer id:serverIds) {

            try {
                final Server server = findServerById(id);
                final Resource r = server.getResource();
                if(r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                rtn.add(server);
            }catch(ServerNotFoundException e) {
                log.debug(e.getMessage(), e);
            }
        }
        Collections.sort(rtn, new AppdefNameComparator(asc));
        return rtn;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Collection<Server> getViewableServers(AuthzSubject subject, Platform platform) {
        Collection<Integer> viewable = filterViewableServers(platform.getServers(), subject);
        Collection<Server> rtn = new ArrayList<Server>(viewable.size());
        for(Integer id:viewable) {
            rtn.add(serverDAO.get(id));
        }
        return rtn;
    }

    private Collection<Server> getServersByPlatformImpl(AuthzSubject subject, Integer platId, Integer servTypeId,
            boolean excludeVirtual, PageControl pc) throws PermissionException, ServerNotFoundException,
            PlatformNotFoundException {
        Collection<Integer> authzPks;
        try {
            authzPks = getViewableServers(subject);
        }catch(NotFoundException exc) {
            throw new ServerNotFoundException("No (viewable) servers associated with platform " + platId);
        }

        List<Server> servers;
        // first, if they specified a server type, then filter on it
        if(!servTypeId.equals(APPDEF_RES_TYPE_UNDEFINED)) {
            if(!excludeVirtual) {
                servers = serverDAO.findByPlatformAndType_orderName(platId, servTypeId);
            }else {
                servers = serverDAO.findByPlatformAndType_orderName(platId, servTypeId, Boolean.FALSE);
            }
        }else {
            if(!excludeVirtual) {
                servers = serverDAO.findByPlatform_orderName(platId);
            }else {
                servers = serverDAO.findByPlatform_orderName(platId, Boolean.FALSE);
            }
        }
        for(Iterator<Server> i = servers.iterator();i.hasNext();) {
            Server aServer = i.next();

            // Keep the virtual ones, we need them so that child services can be
            // added. Otherwise, no one except the super user will have access
            // to the virtual services
            if(aServer.getServerType().isVirtual()) continue;

            // Remove the server if its not viewable
            if(!authzPks.contains(aServer.getId())) {
                i.remove();
            }
        }

        // If sort descending, then reverse the list
        if(pc != null && pc.isDescending()) {
            Collections.reverse(servers);
        }

        return servers;
    }

    /**
     * Get servers by platform.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param platId
     *            platform id.
     * @param excludeVirtual
     *            true if you dont want virtual (fake container) servers in the
     *            returned list
     * @param pc
     *            The page control.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServerValue> getServersByPlatform(AuthzSubject subject, Integer platId, boolean excludeVirtual,
            PageControl pc) throws ServerNotFoundException, PlatformNotFoundException, PermissionException {
        return getServersByPlatform(subject, platId, APPDEF_RES_TYPE_UNDEFINED, excludeVirtual, pc);
    }

    /**
     * Get servers by server type and platform.
     * 
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param servTypeId
     *            server type id.
     * @param platId
     *            platform id.
     * @param pc
     *            The page control.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServerValue> getServersByPlatform(AuthzSubject subject, Integer platId, Integer servTypeId,
            boolean excludeVirtual, PageControl pc) throws ServerNotFoundException, PlatformNotFoundException,
            PermissionException {
        Collection<Server> servers = getServersByPlatformImpl(subject, platId, servTypeId, excludeVirtual, pc);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(servers, pc);
    }

    /**
     * Get servers by server type and platform.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param platId
     *            platform id.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServerValue> getServersByPlatformServiceType(AuthzSubject subject, Integer platId, Integer svcTypeId)
            throws ServerNotFoundException, PlatformNotFoundException, PermissionException {
        PageControl pc = PageControl.PAGE_ALL;
        Integer servTypeId;
        try {
            ServiceType typeV = serviceTypeDAO.findById(svcTypeId);
            servTypeId = typeV.getServerType().getId();
        }catch(ObjectNotFoundException e) {
            throw new ServerNotFoundException("Service Type not found", e);
        }

        Collection<Server> servers = getServersByPlatformImpl(subject, platId, servTypeId, false, pc);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(servers, pc);
    }

    /**
     * Get servers by server type and platform.
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param typeId
     *            server type id.
     * 
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public List<ServerValue> getServersByType(AuthzSubject subject, String name) throws PermissionException,
            InvalidAppdefTypeException {
        try {
            ServerType serverType = serverTypeDAO.findByName(name);
            if(serverType == null) {
                return new PageList<ServerValue>();
            }

            Collection<Server> servers = serverDAO.findByType(serverType.getId());

            Collection<Integer> authzPks = getViewableServers(subject);
            for(Iterator<Server> i = servers.iterator();i.hasNext();) {
                Integer sPK = i.next().getId();
                // remove server if its not viewable
                if(!authzPks.contains(sPK)) i.remove();
            }

            // valuePager converts local/remote interfaces to value objects
            // as it pages through them.
            return valuePager.seek(servers, PageControl.PAGE_ALL);
        }catch(NotFoundException e) {
            return new ArrayList<ServerValue>(0);
        }
    }

    /**
     * Get non-virtual server IDs by server type and platform.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param platId
     *            platform id.
     * @return An array of Integer[] which represent the ServerIds specified
     *         platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public Integer[] getServerIdsByPlatform(AuthzSubject subject, Integer platId) throws ServerNotFoundException,
            PlatformNotFoundException, PermissionException {
        return getServerIdsByPlatform(subject, platId, APPDEF_RES_TYPE_UNDEFINED, true);
    }

    /**
     * Get non-virtual server IDs by server type and platform.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param servTypeId
     *            server type id.
     * @param platId
     *            platform id.
     * @return An array of Integer[] which represent the ServerIds
     */
    @Transactional(readOnly = true)
    public Integer[] getServerIdsByPlatform(AuthzSubject subject, Integer platId, Integer servTypeId)
            throws ServerNotFoundException, PlatformNotFoundException, PermissionException {
        return getServerIdsByPlatform(subject, platId, servTypeId, true);
    }

    /**
     * Get server IDs by server type and platform.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param servTypeId
     *            server type id.
     * @param platId
     *            platform id.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public Integer[] getServerIdsByPlatform(AuthzSubject subject, Integer platId, Integer servTypeId,
            boolean excludeVirtual) throws ServerNotFoundException, PlatformNotFoundException, PermissionException {
        Collection<Server> servers = getServersByPlatformImpl(subject, platId, servTypeId, excludeVirtual, null);

        Integer[] ids = new Integer[servers.size()];
        Iterator<Server> it = servers.iterator();
        for(int i = 0;it.hasNext();i++) {
            Server server = it.next();
            ids[i] = server.getId();
        }

        return ids;
    }

    /**
     * Get servers by application and serverType.
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param appId
     *            Application id.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    private Collection<Server> getServersByApplicationImpl(AuthzSubject subject, Integer appId, Integer servTypeId)
            throws ServerNotFoundException, ApplicationNotFoundException, PermissionException {

        Collection<Integer> authzPks;
        Application appLocal;

        try {
            appLocal = applicationDAO.findById(appId);
        }catch(ObjectNotFoundException exc) {
            throw new ApplicationNotFoundException(appId, exc);
        }

        try {
            authzPks = getViewableServers(subject);
        }catch(NotFoundException e) {
            throw new ServerNotFoundException("No (viewable) servers " + "associated with " + "application " + appId, e);
        }

        HashMap<Integer, Server> serverCollection = new HashMap<Integer, Server>();

        // XXX - a better solution is to control the viewable set returned by
        // ql finders. This will be forthcoming.

        Collection<AppService> appServiceCollection = appLocal.getAppServices();
        Iterator<AppService> it = appServiceCollection.iterator();

        while (it.hasNext()) {

            AppService appService = it.next();

            if(appService.isIsGroup()) {
                Collection<Service> services = serviceManager.getServiceCluster(appService.getResourceGroup())
                        .getServices();

                Iterator<Service> serviceIterator = services.iterator();
                while (serviceIterator.hasNext()) {
                    Service service = serviceIterator.next();
                    Server server = service.getServer();

                    // Don't bother with entire cluster if type is platform svc
                    if(server.getServerType().isVirtual()) {
                        break;
                    }

                    Integer serverId = server.getId();

                    if(serverCollection.containsKey(serverId)) {
                        continue;
                    }

                    serverCollection.put(serverId, server);
                }
            }else {
                Server server = appService.getService().getServer();
                if(!server.getServerType().isVirtual()) {
                    Integer serverId = server.getId();

                    if(serverCollection.containsKey(serverId)) continue;

                    serverCollection.put(serverId, server);
                }
            }
        }

        for(Iterator<Map.Entry<Integer, Server>> i = serverCollection.entrySet().iterator();i.hasNext();) {
            Map.Entry<Integer, Server> entry = i.next();
            Server aServer = entry.getValue();

            // first, if they specified a server type, then filter on it
            if(servTypeId != APPDEF_RES_TYPE_UNDEFINED && !(aServer.getServerType().getId().equals(servTypeId))) {
                i.remove();
            }
            // otherwise, remove the server if its not viewable
            else if(!authzPks.contains(aServer.getId())) {
                i.remove();
            }
        }

        return serverCollection.values();
    }

    /**
     * Get servers by application.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param appId
     *            Application id.
     * @param pc
     *            The page control for this page list.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServerValue> getServersByApplication(AuthzSubject subject, Integer appId, PageControl pc)
            throws ServerNotFoundException, ApplicationNotFoundException, PermissionException {
        return getServersByApplication(subject, appId, APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * Get servers by application and serverType.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param appId
     *            Application id.
     * @param pc
     *            The page control for this page list.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<ServerValue> getServersByApplication(AuthzSubject subject, Integer appId, Integer servTypeId,
            PageControl pc) throws ServerNotFoundException, ApplicationNotFoundException, PermissionException {
        Collection<Server> serverCollection = getServersByApplicationImpl(subject, appId, servTypeId);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverCollection, pc);
    }

    /**
     * Get server IDs by application and serverType.
     * 
     * 
     * @param subject
     *            The subject trying to list servers.
     * @param appId
     *            Application id.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public Integer[] getServerIdsByApplication(AuthzSubject subject, Integer appId, Integer servTypeId)
            throws ServerNotFoundException, ApplicationNotFoundException, PermissionException {
        Collection<Server> servers = getServersByApplicationImpl(subject, appId, servTypeId);

        Integer[] ids = new Integer[servers.size()];
        Iterator<Server> it = servers.iterator();
        for(int i = 0;it.hasNext();i++) {
            Server server = it.next();
            ids[i] = server.getId();
        }

        return ids;
    }

    /**
     * Update a server
     * 
     * @param existing
     * 
     */
    public Server updateServer(AuthzSubject subject, ServerValue existing) throws PermissionException, UpdateException,
            AppdefDuplicateNameException, ServerNotFoundException {
        try {
            Server server = serverDAO.findById(existing.getId());
            permissionManager.checkModifyPermission(subject, server.getEntityId());
            existing.setModifiedBy(subject.getName());
            existing.setMTime(new Long(System.currentTimeMillis()));
            trimStrings(existing);
            Map<String, String> changedProps = server.changedProperties(existing);
            if(changedProps.isEmpty()) {
                log.debug("No changes found between value object and entity");
            }else {
                Resource rv = server.getResource();
                String oldName = null;
                if(!existing.getName().equals(server.getName())) {
                    oldName = server.getName();
                    rv.setName(existing.getName());
                }
                this.zeventManager.enqueueEventAfterCommit(new ResourceContentChangedZevent(rv.getId(), rv.getName(),
                        null, changedProps, oldName));
                server.updateServer(existing);
            }
            return server;
        }catch(ObjectNotFoundException e) {
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
        for(int i = 0;i < infos.length;i++) {
            String name = infos[i].getName();
            ServerTypeInfo sinfo = infoMap.get(name);

            if(sinfo == null) {
                // first time we've seen this type
                // clone it incase we have to update the platforms
                infoMap.put(name, (ServerTypeInfo) infos[i].clone());
            }else {
                // already seen this type; just update the platforms.
                // this allows server types of the same name to support
                // different families of platforms in the plugins.
                String[] platforms = (String[]) ArrayUtil.merge(sinfo.getValidPlatformTypes(),
                        infos[i].getValidPlatformTypes(), new String[0]);
                sinfo.setValidPlatformTypes(platforms);
            }
        }

        Collection<ServerType> curServers = serverTypeDAO.findByPlugin(plugin);

        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        for(ServerType serverType:curServers) {

            String serverName = serverType.getName();
            ServerTypeInfo sinfo = (ServerTypeInfo) infoMap.remove(serverName);

            if(sinfo == null) {
                deleteServerType(serverType, overlord, resourceGroupManager, resourceManager);
            }else {
                String curDesc = serverType.getDescription();
                Collection<PlatformType> curPlats = serverType.getPlatformTypes();
                String newDesc = sinfo.getDescription();
                String[] newPlats = sinfo.getValidPlatformTypes();
                boolean updatePlats;

                log.debug("Updating ServerType: " + serverName);

                if(!newDesc.equals(curDesc)) {
                    serverType.setDescription(newDesc);
                }

                // See if we need to update the supported platforms
                updatePlats = newPlats.length != curPlats.size();
                if(updatePlats == false) {
                    // Ensure that the lists are the same
                    for(PlatformType pLocal:curPlats) {

                        int j;

                        for(j = 0;j < newPlats.length;j++) {
                            if(newPlats[j].equals(pLocal.getName())) break;
                        }
                        if(j == newPlats.length) {
                            updatePlats = true;
                            break;
                        }
                    }
                }

                if(updatePlats == true) {
                    findAndSetPlatformType(newPlats, serverType);
                }
            }
        }

        // Now create the left-overs
        for(ServerTypeInfo sinfo:infoMap.values()) {
            createServerType(sinfo, plugin);
        }
    }

    public ServerType createServerType(ServerTypeInfo sinfo, String plugin) throws NotFoundException {
        Resource prototype = resourceManager.findRootResource();
        ServerType stype = new ServerType();

        log.debug("Creating new ServerType: " + sinfo.getName());
        stype.setPlugin(plugin);
        stype.setName(sinfo.getName());
        stype.setDescription(sinfo.getDescription());
        stype.setVirtual(sinfo.isVirtual());
        String newPlats[] = sinfo.getValidPlatformTypes();
        findAndSetPlatformType(newPlats, stype);

        stype = serverTypeDAO.create(stype);
        resourceManager.createResource(authzSubjectManager.getOverlordPojo(),
                resourceManager.findResourceTypeByName(AuthzConstants.serverPrototypeTypeName), prototype,
                stype.getId(), stype.getName(), false, null); // No parent
        return stype;
    }

    /**
     * Find an operation by name inside a ResourcetypeValue object
     */
    protected Operation getOperationByName(ResourceType rtV, String opName) throws PermissionException {
        Collection<Operation> ops = rtV.getOperations();
        for(Operation op:ops) {

            if(op.getName().equals(opName)) {
                return op;
            }
        }
        throw new PermissionException("Operation: " + opName + " not valid for ResourceType: " + rtV.getName());
    }

    /**
     * builds a list of resource types from the list of resources
     * 
     * @param resources
     *            - {@link Collection} of {@link AppdefResource}
     * @param {@link Collection} of {@link AppdefResourceType}
     */
    protected Collection<AppdefResourceType> filterResourceTypes(Collection<? extends AppdefResource> resources) {
        final Set<AppdefResourceType> resTypes = new HashSet<AppdefResourceType>();
        for(AppdefResource o:resources) {

            if(o == null) {
                continue;
            }
            final AppdefResourceType rt = o.getAppdefResourceType();
            if(rt != null) {
                resTypes.add(rt);
            }
        }
        final List<AppdefResourceType> rtn = new ArrayList<AppdefResourceType>(resTypes);
        Collections.sort(rtn, new Comparator<AppdefResourceType>() {
            private String getName(Object obj) {
                if(obj instanceof AppdefResourceType) {
                    return ((AppdefResourceType) obj).getSortName();
                }
                return "";
            }

            public int compare(AppdefResourceType o1, AppdefResourceType o2) {
                return getName(o1).compareTo(getName(o2));
            }
        });
        return rtn;
    }

    /**
     * 
     */
    public void deleteServerType(ServerType serverType, AuthzSubject overlord, ResourceGroupManager resGroupMan,
            ResourceManager resMan) throws VetoException {
        // Need to remove all service types

        ServiceType[] types = (ServiceType[]) serverType.getServiceTypes().toArray(new ServiceType[0]);
        for(int i = 0;i < types.length;i++) {
            serviceManager.deleteServiceType(types[i], overlord, resGroupMan, resMan);
        }

        log.debug("Removing ServerType: " + serverType.getName());
        Integer typeId = AuthzConstants.authzServerProto;
        Resource proto = resMan.findResourceByInstanceId(typeId, serverType.getId());

        try {
            resGroupMan.removeGroupsCompatibleWith(proto);

            // Remove all servers
            Server[] servers = (Server[]) serverType.getServers().toArray(new Server[0]);
            for(int i = 0;i < servers.length;i++) {
                removeServer(overlord, servers[i]);
            }
        }catch(PermissionException e) {
            assert false : "Overlord should not run into PermissionException";
        }

        serverTypeDAO.remove(serverType);

        resMan.removeResource(overlord, proto);
    }

    /**
     * 
     */
    public void setAutodiscoveryZombie(Server server, boolean zombie) {
        server.setAutodiscoveryZombie(zombie);
    }

    /**
     * Get a Set of PlatformTypeLocal objects which map to the names as given by
     * the argument.
     */
    private void findAndSetPlatformType(String[] platNames, ServerType stype) throws NotFoundException {

        for(int i = 0;i < platNames.length;i++) {
            PlatformType pType = platformTypeDAO.findByName(platNames[i]);
            if(pType == null) {
                throw new NotFoundException("Could not find platform type '" + platNames[i] + "'");
            }
            stype.addPlatformType(pType);
        }
    }

    /**
     * Create the Authz resource and verify that the user has correct
     * permissions
     */
    private void createAuthzServer(AuthzSubject subject, Server server) throws PermissionException, NotFoundException {
        log.debug("Being Authz CreateServer");
        if(log.isDebugEnabled()) {
            log.debug("Checking for: " + AuthzConstants.platformOpAddServer + " for subject: " + subject);
        }
        AppdefEntityID platId = server.getPlatform().getEntityId();
        permissionManager.checkPermission(subject,
                resourceManager.findResourceTypeByName(AuthzConstants.platformResType), platId.getId(),
                AuthzConstants.platformOpAddServer);

        ResourceType serverProto = resourceManager.findResourceTypeByName(AuthzConstants.serverPrototypeTypeName);
        ServerType serverType = server.getServerType();
        Resource proto = resourceManager.findResourceByInstanceId(serverProto, serverType.getId());
        Resource parent = resourceManager.findResource(platId);

        if(parent == null) {
            throw new SystemException("Unable to find parent platform [id=" + platId + "]");
        }
        Resource resource = resourceManager.createResource(subject,
                resourceManager.findResourceTypeByName(AuthzConstants.serverResType), proto, server.getId(),
                server.getName(), serverType.isVirtual(), parent);
        server.setResource(resource);
    }

    /**
     * Trim all string attributes
     */
    private void trimStrings(ServerValue server) {
        if(server.getDescription() != null) server.setDescription(server.getDescription().trim());
        if(server.getInstallPath() != null) server.setInstallPath(server.getInstallPath().trim());
        if(server.getAutoinventoryIdentifier() != null)
            server.setAutoinventoryIdentifier(server.getAutoinventoryIdentifier().trim());
        if(server.getLocation() != null) server.setLocation(server.getLocation().trim());
        if(server.getName() != null) server.setName(server.getName().trim());
    }

    /**
     * Returns a list of 2 element arrays. The first element is the name of the
     * server type, the second element is the # of servers of that type in the
     * inventory.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public List<Object[]> getServerTypeCounts() {
        return serverDAO.getServerTypeCounts();
    }

    /**
     * Get the # of servers within HQ inventory. This method ingores virtual
     * server types.
     * 
     */
    @Transactional(readOnly = true)
    public Number getServerCount() {
        return serverDAO.getServerCount();
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {

        valuePager = Pager.getPager(VALUE_PROCESSOR);

    }

    public Collection<Server> getOrphanedServers() {
        return serverDAO.getOrphanedServers();
    }
    
    public Collection<Server> getRemovableChildren(AuthzSubject subject, Resource parent) {
        Collection<Resource> removableChildren = this.resourceManager.getRemovableChildren(subject, parent);
        return serverDAO.getServers(removableChildren);
    }
}
