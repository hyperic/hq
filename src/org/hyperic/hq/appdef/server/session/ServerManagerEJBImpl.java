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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.AuditManagerEJBImpl;
import org.hyperic.hq.common.server.session.ResourceAudit;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.dao.DAOFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl;

/**
 * This class is responsible for managing Server objects in appdef
 * and their relationships
 * @ejb:bean name="ServerManager"
 *      jndi-name="ejb/appdef/ServerManager"
 *      local-jndi-name="LocalServerManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="Required"
 */
public class ServerManagerEJBImpl extends AppdefSessionEJB
    implements SessionBean {

    private Log log = LogFactory.getLog(ServerManagerEJBImpl.class);

    private final String VALUE_PROCESSOR
        = "org.hyperic.hq.appdef.server.session.PagerProcessor_server";
    private Pager valuePager = null;
    private final Integer APPDEF_RES_TYPE_UNDEFINED = new Integer(-1);

    /**
     * Validate a server value object which is to be created on this
     * platform. This method will check IP conflicts and any other
     * special constraint required to succesfully add a server instance
     * to a platform
     */
    private void validateNewServer(Platform p, Server server)
        throws ValidationException
    {
        // ensure the server value has a server type
        String msg = null;
        if(server.getServerType() == null) {
            msg = "Server has no ServiceType";
        } else if(server.getId() != null) {
            msg = "This server is not new, it has ID:" + server.getId();
        }
        if(msg == null) {
            Integer id = server.getServerType().getId();
            Collection stypes = p.getPlatformType().getServerTypes();
            for (Iterator i = stypes.iterator(); i.hasNext();) {
                ServerType sVal = (ServerType)i.next();
                if(sVal.getId().equals(id))
                    return;
            }
            msg = "Servers of type '" + server.getServerType().getName() +
                "' cannot be created on platforms of type '" +
                p.getPlatformType().getName() +"'";
        }
        if (msg != null) {
            throw new ValidationException(msg);
        }
    }

    /**
     * Validate a server value object which is to be created on this
     * platform. This method will check IP conflicts and any other
     * special constraint required to succesfully add a server instance
     * to a platform
     */
    private void validateNewServer(Platform p, ServerValue sv)
        throws ValidationException
    {
        // ensure the server value has a server type
        String msg = null;
        if(sv.getServerType() == null) {
            msg = "Server has no ServiceType";
        } else if(sv.idHasBeenSet()){
            msg = "This server is not new, it has ID:" + sv.getId();
        }
        if(msg == null) {
            Integer id = sv.getServerType().getId();
            Collection stypes = p.getPlatformType().getServerTypes();
            for (Iterator i = stypes.iterator(); i.hasNext();) {
                ServerType sVal = (ServerType)i.next();
                if(sVal.getId().equals(id))
                    return;
            }
            msg = "Servers of type '" + sv.getServerType().getName() +
                "' cannot be created on platforms of type '" +
                p.getPlatformType().getName() +"'";
        }
        if (msg != null) {
            throw new ValidationException(msg);
        }
    }

    /**
     * Construct the new name of the server
     * to be cloned to the target platform
     */
    private String getTargetServerName(Platform targetPlatform,
                                       Server serverToClone) {        
        
        String prefix = serverToClone.getPlatform().getName();
        String oldServerName = serverToClone.getName();
        String newServerName = StringUtil.removePrefix(oldServerName, prefix);
        
        if (newServerName.equals(oldServerName)) {
            // old server name may not contain the canonical host name
            // of the platform. try to get just the host name
            int dotIndex = prefix.indexOf(".");
            if (dotIndex > 0) {
                prefix = prefix.substring(0, dotIndex);
                newServerName = StringUtil.removePrefix(oldServerName, prefix);
            }
        }
        
        newServerName = targetPlatform.getName() + " " + newServerName;
        
        return newServerName;
    }
    
    /**
     * Clone a Server to a target Platform
     * @ejb:interface-method
     */
    public Server cloneServer(AuthzSubject subject, Platform targetPlatform,
                              Server serverToClone)
        throws ValidationException, PermissionException, RemoveException,
               VetoException, CreateException, FinderException
    {
        Server s = null;
        // See if we already have this server type
        for (Iterator it = targetPlatform.getServers().iterator(); it.hasNext();)
        {
            Server server = (Server) it.next();
            if (server.getServerType().equals(serverToClone.getServerType())) {
                // Do nothing if it's a Network server
                if (server.getServerType().getName().equals("NetworkServer")) {
                    return null;
                }
                // HQ-1657: virtual servers are not deleted. clone all other servers
                if (server.getServerType().isVirtual()) {
                    s = server;
                    break;
                }
            }
        }
        ConfigResponseDB cr = serverToClone.getConfigResponse();
        byte[] productResponse = cr.getProductResponse();
        byte[] measResponse = cr.getMeasurementResponse();
        byte[] controlResponse = cr.getControlResponse();
        byte[] rtResponse = cr.getResponseTimeResponse();
        ConfigManagerLocal cMan = ConfigManagerEJBImpl.getOne();
        ServiceManagerLocal svcMan = ServiceManagerEJBImpl.getOne();
        
        if (s == null) {
            ConfigResponseDB configResponse = cMan.createConfigResponse(
                productResponse, measResponse, controlResponse, rtResponse);
            s = new Server();
            s.setName(getTargetServerName(targetPlatform, serverToClone));
            s.setDescription(serverToClone.getDescription());
            s.setInstallPath(serverToClone.getInstallPath());
            String aiid = serverToClone.getAutoinventoryIdentifier();
            if (aiid != null) {
                s.setAutoinventoryIdentifier(serverToClone.getAutoinventoryIdentifier());
            } else {
                // Server was created by hand, use a generated AIID. (This matches
                // the behaviour in 2.7 and prior)
                aiid = serverToClone.getInstallPath() + "_" + System.currentTimeMillis() +
                "_" + serverToClone.getName();
                s.setAutoinventoryIdentifier(aiid);
            }
            s.setServicesAutomanaged(serverToClone.isServicesAutomanaged());
            s.setRuntimeAutodiscovery(serverToClone.isRuntimeAutodiscovery());
            s.setWasAutodiscovered(serverToClone.isWasAutodiscovered());
            s.setAutodiscoveryZombie(false);
            s.setLocation(serverToClone.getLocation());
            s.setModifiedBy(serverToClone.getModifiedBy());
            s.setConfigResponse(configResponse);
            s.setPlatform(targetPlatform);

            Integer stid = serverToClone.getServerType().getId();
            ServerTypeDAO dao = new ServerTypeDAO(DAOFactory.getDAOFactory());
            ServerType st = dao.findById(stid);
            s.setServerType(st);
            validateNewServer(targetPlatform, s);
            getServerDAO().create(s);
            // Add server to parent collection
            targetPlatform.getServersBag().add(s);
    
            createAuthzServer(subject, s);
    
            // Send resource create event
            ResourceCreatedZevent zevent =
                new ResourceCreatedZevent(subject, s.getEntityId());
            ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
        }
        else {
            boolean wasUpdated = cMan.configureResponse(subject, cr, s.getEntityId(),
                                                        productResponse, measResponse,
                                                        controlResponse, rtResponse,
                                                        null, true);
            if (wasUpdated) {
                ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
            	rMan.resourceHierarchyUpdated(subject, Collections.singletonList(s.getResource()));
            }
            
            // Scrub the services
            Service[] services =
                (Service[]) s.getServices().toArray(new Service[0]);
            for (int i = 0; i < services.length; i++) {
                Service svc = services[i];
                
                if (!svc.getServiceType().getName().equals("CPU")) {
                    svcMan.removeService(subject, svc);
                }
            }
        }

        return s;
    }

    /**
     * Move a Server to the given Platform
     *
     * @param subject The user initiating the move.
     * @param target The target {@link org.hyperic.hq.appdef.server.session.Server} to move.
     * @param destination The destination {@link Platform}.
     *
     * @throws org.hyperic.hq.authz.shared.PermissionException If the passed
     * user does not have permission to move the Server.
     * @throws org.hyperic.hq.common.VetoException If the operation canot be
     * performed due to incompatible types.
     *
     * @ejb:interface-method
     */
    public void moveServer(AuthzSubject subject, Server target,
                           Platform destination)
        throws VetoException, PermissionException
    {
        ResourceManagerLocal rMan = getResourceManager();

        try {
            // Permission checking on destination
            checkPermission(subject, getPlatformResourceType(),
                            destination.getId(),
                            AuthzConstants.platformOpAddServer);

            // Permission check on target
            checkPermission(subject, getServerResourceType(),
                            target.getId(), AuthzConstants.serverOpRemoveServer);
        } catch (FinderException e) {
            // TODO: FinderException needs to be expelled from this class.
            throw new VetoException("Caught FinderException checking permission: " +
                                    e.getMessage()); // notgonnahappen
        }

        // Ensure target can be moved to the destination
        if (!destination.getPlatformType().getServerTypes().contains(target.getServerType())) {
            throw new VetoException("Incompatible resources passed to move(), " +
                                    "cannot move server of type " +
                                    target.getServerType().getName() + " to " +
                                    destination.getPlatformType().getName());

        }

        // Unschedule measurements
        MeasurementManagerEJBImpl.getOne().disableMeasurements(subject,
                                                               target.getResource());

        // Reset Server parent id
        target.setPlatform(destination);

        // Add/Remove Server from Server collections
        target.getPlatform().getServersBag().remove(target);
        destination.getServersBag().add(target);

        // Move Authz resource.
        rMan.moveResource(subject, target.getResource(), destination.getResource());

        // Flush server move
        DAOFactory.getDAOFactory().getCurrentSession().flush();

        // Reschedule metrics
        ResourceUpdatedZevent zevent =
            new ResourceUpdatedZevent(subject, target.getEntityId());
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);            

        // Must also move all dependent services so that ancestor edges are
        // rebuilt and that service metrics are re-scheduled
        ArrayList services = new ArrayList(); // copy list since the move will modify the server collection.
        services.addAll(target.getServices());

        for (Iterator i = services.iterator(); i.hasNext(); ) {
            Service s = (Service)i.next();
            getServiceManager().moveService(subject, s, target);
        }
    }

    /**
     * Create a Server on the given platform.
     *
     * @return ServerValue - the saved value object
     * @exception CreateException - if it fails to add the server
     * @ejb:interface-method
     */
    public Server createServer(AuthzSubject subject, Integer platformId,
                               Integer serverTypeId, ServerValue sValue)
        throws CreateException, ValidationException, PermissionException,
               PlatformNotFoundException, AppdefDuplicateNameException 
    {
        try {
            trimStrings(sValue);

            Platform platform = getPlatformDAO().findById(platformId);
            ServerType serverType = getServerTypeDAO().findById(serverTypeId);

            sValue.setServerType(serverType.getServerTypeValue());
            sValue.setOwner(subject.getName());
            sValue.setModifiedBy(subject.getName());
            
            // validate the object
            validateNewServer(platform, sValue);
            
            // create it
            Server server = getServerDAO().create(sValue, platform);

            // Add server to parent collection
            platform.getServersBag().add(server);

            createAuthzServer(subject, server);

            // Send resource create event
            ResourceCreatedZevent zevent =
                new ResourceCreatedZevent(subject, server.getEntityId());
            ZeventManager.getInstance().enqueueEventAfterCommit(zevent);

            return server;
        } catch (CreateException e) {
            throw e;
        } catch (FinderException e) {
            throw new CreateException("Unable to find platform=" + platformId +
                                      " or server type=" + serverTypeId +
                                      ":" + e.getMessage());
        }
    }

    /**
     * Create a virtual server
     * @throws FinderException 
     * @throws CreateException 
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public Server createVirtualServer(AuthzSubject subject, Platform platform,
                                      ServerType st)
        throws PermissionException, CreateException, FinderException {
        // First of all, make sure this is a virtual type
        if (!st.isVirtual()) {
            throw new IllegalArgumentException(
                "createVirtualServer() called for non-virtual server type: " +
                st.getName());
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
        
        Server server = getServerDAO().create(sv, platform);
        
        // Add server to parent collection
        Collection servers = platform.getServersBag();
        if (!servers.contains(server)) {
            servers.add(server);
        }

        createAuthzServer(subject, server);
        return server;
    }
    
    /**
     * A removeServer method that takes a ServerLocal.  Used by
     * PlatformManager.removePlatform when cascading removal to servers.
     * @ejb:interface-method
     */
    public void removeServer(AuthzSubject subject, Server server)
        throws RemoveException, PermissionException, VetoException
    {
        final AppdefEntityID aeid = server.getEntityId();
        final Resource r = server.getResource();
        final Audit audit = ResourceAudit.deleteResource(r, subject, 0, 0);
        boolean pushed = false;
        
        try {
            AuditManagerEJBImpl.getOne().pushContainer(audit);
            pushed = true;
            if (!server.getServerType().isVirtual()) {
                checkRemovePermission(subject, server.getEntityId());
            }

            // Service manager will update the collection, so we need to copy
            final ServiceManagerLocal sMan = getServiceManager();
            Collection services = server.getServices();
            synchronized(services) {
                for (final Iterator i = services.iterator(); i.hasNext(); ) {
                    try {
                        // this looks funky but the idea is to pull the service
                        // obj into the session so that it is updated when flushed
                        final Service service =
                            sMan.findServiceById(((Service)i.next()).getId());
                        final String currAiid =
                            service.getAutoinventoryIdentifier();
                        final Integer id = service.getId();
                        // ensure aiid remains unique
                        service.setAutoinventoryIdentifier(id + currAiid);
                        service.setServer(null);
                        i.remove();
                    } catch (ServiceNotFoundException e) {
                        log.warn(e);
                    }
                }
            }

            final ServerDAO dao = getServerDAO();
            // this flush ensures that the service's server_id is set to null
            // before the server is deleted and the services cascaded
            dao.getSession().flush();

            // Remove server from parent Platform Server collection.
            Platform platform = server.getPlatform();
            if (platform != null) {
                platform.getServersBag().remove(server);
            }
            
            //Remove Server from ServerType.  If not done, results in an ObjectDeletedException 
            //when updating plugin types during plugin deployment
            server.getServerType().getServers().remove(server);
            
            // Keep config response ID so it can be deleted later.
            final ConfigResponseDB config = server.getConfigResponse();

            dao.remove(server);

            // Remove the config response
            if (config != null) {
                getConfigResponseDAO().remove(config);
            }

            deleteCustomProperties(aeid);

            // Remove authz resource
            removeAuthzResource(subject, aeid, r);

            dao.getSession().flush();
        } finally {
            if (pushed) {
                AuditManagerEJBImpl.getOne().popContainer(true);
            }
        }
    }

    /**
     * @ejb:interface-method
     */
    public void handleResourceDelete(Resource resource) {
        getServerDAO().clearResource(resource);
    }

    /**
     * Find all server types
     * @return list of serverTypeValues
     * @ejb:interface-method
     */
    public PageList getAllServerTypes(AuthzSubject subject, PageControl pc)
        throws FinderException {
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(getServerTypeDAO().findAllOrderByName(), pc);
    }

    /**
     * @ejb:interface-method
     */
    public Server getServerByName(Platform host, String name) {
        return getServerDAO().findByName(host, name);
    }
    
        
    /**
     * Find viewable server types
     * @return list of serverTypeValues
     * @ejb:interface-method
     */
    public PageList getViewableServerTypes(AuthzSubject subject,
                                           PageControl pc)
        throws FinderException, PermissionException {
        // build the server types from the visible list of servers
        final List authzPks = getViewableServers(subject);
        final Collection serverTypes =
            getServerDAO().getServerTypes(authzPks, true);
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverTypes, pc);
    }
    
    /**
     * Find viewable server non-virtual types for a platform
     * @return list of serverTypeValues
     * @ejb:interface-method
     */
    public PageList getServerTypesByPlatform(AuthzSubject subject,
                                             Integer platId,
                                             PageControl pc)
        throws PermissionException, PlatformNotFoundException, 
               ServerNotFoundException {    
        return getServerTypesByPlatform(subject, platId, true, pc);
    }

    /**
     * Find viewable server types for a platform
     * @return list of serverTypeValues
     * @ejb:interface-method
     */
    public PageList getServerTypesByPlatform(AuthzSubject subject,
                                             Integer platId,
                                             boolean excludeVirtual,
                                             PageControl pc)
        throws PermissionException, PlatformNotFoundException, 
               ServerNotFoundException {

        // build the server types from the visible list of servers
        Collection servers = getServersByPlatformImpl(subject,
                                                      platId,
                                                      APPDEF_RES_TYPE_UNDEFINED,
                                                      excludeVirtual,
                                                      pc);

        Collection serverTypes = filterResourceTypes(servers);

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
     * @ejb:interface-method
     */
    public PageList getServerTypesByPlatformType(AuthzSubject subject,
                                                 Integer platformTypeId,
                                                 PageControl pc)
        throws PlatformNotFoundException
    {
        PlatformType platType =
            getPlatformManager().findPlatformType(platformTypeId);

        Collection serverTypes = platType.getServerTypes();

        return valuePager.seek(serverTypes, pc);
    }

    /**
     * @ejb:interface-method
     */
    public Server findServerByAIID(AuthzSubject subject,
                                   Platform platform, String aiid)
        throws PermissionException {
        checkViewPermission(subject, platform.getEntityId());
        return getServerDAO().findServerByAIID(platform, aiid);
    }

    /**
     * Find a Server by Id.
     * @ejb:interface-method
     */
    public Server findServerById(Integer id) throws ServerNotFoundException {
        Server server = getServerById(id);
        
        if (server == null) {
            throw new ServerNotFoundException(id);
        }

        return server;
    }

    /**
     * Get a Server by Id.
     * @ejb:interface-method 
     * @return The Server with the given id, or null if not found.
     */
    public Server getServerById(Integer id) {
        return getServerDAO().get(id); 
    }

    /**
     * Find a ServerType by id
     * @ejb:interface-method
     */
    public ServerType findServerType(Integer id) {
        return getServerTypeDAO().findById(id); 
    }
    
    /**
     * Find a server type by name
     * @param name - the name of the server
     * @return ServerTypeValue
     * @ejb:interface-method
     */
    public ServerType findServerTypeByName(String name) throws FinderException {
        ServerType type = getServerTypeDAO().findByName(name);
        if (type == null) {
            throw new FinderException("name not found: " + name);
        }
        return type;
    }

    /**
     * @ejb:interface-method
     */
    public List findServersByType(Platform p, ServerType st) {
        return getServerDAO().findByPlatformAndType_orderName(p.getId(), 
                                                              st.getId());
    }

    /**
     * @ejb.interface-method
     */
    public Collection findDeletedServers() {
        return getServerDAO().findDeletedServers();
    }
    
    /** 
     * Get server lite value by id.  Does not check permission.
     * @ejb:interface-method
     */
    public Server getServerById(AuthzSubject subject, Integer id)
        throws ServerNotFoundException, PermissionException {
        Server server = findServerById(id);
        checkViewPermission(subject, server.getEntityId());
        return server;
    }

    /**
     * Get server IDs by server type.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @return An array of Server IDs.
     */
    public Integer[] getServerIds(AuthzSubject subject, Integer servTypeId)
        throws PermissionException {
        ServerDAO sLHome;
        try {
            sLHome = getServerDAO();
            Collection servers = sLHome.findByType(servTypeId);
            if (servers.size() == 0) {
                return new Integer[0];
            }
            List serverIds = new ArrayList(servers.size());
         
            // now get the list of PKs
            Collection viewable = super.getViewableServers(subject);
            // and iterate over the ejbList to remove any item not in the
            // viewable list
            int i = 0;
            for (Iterator it = servers.iterator(); it.hasNext(); i++) {
                Server aEJB = (Server) it.next();
                if (viewable.contains(aEJB.getId())) {
                    // add the item, user can see it
                    serverIds.add(aEJB.getId());
                }
            }
        
            return (Integer[]) serverIds.toArray(new Integer[0]);
        } catch (FinderException e) {
            // There are no viewable servers
            return new Integer[0];
        }
    }

    /**
     * Get server by service.
     * @ejb:interface-method
     */
    public ServerValue getServerByService(AuthzSubject subject, Integer sID) 
        throws ServerNotFoundException, ServiceNotFoundException, 
               PermissionException
    {
        Service svc = getServiceDAO().findById(sID);
        Server s = svc.getServer();
        checkViewPermission(subject, s.getEntityId());
        return s.getServerValue();
    }

    /**
     * Get server by service.  The virtual servers are not filtere out of
     * returned list.
     * @ejb:interface-method
     */
    public PageList getServersByServices(AuthzSubject subject, List sIDs) 
        throws PermissionException, ServerNotFoundException 
    {
        Set servers = new HashSet();
        for (Iterator i = sIDs.iterator(); i.hasNext(); ) {
            AppdefEntityID svcId = (AppdefEntityID) i.next();
            Service svc = getServiceDAO().findById(svcId.getId());

            servers.add(svc.getServer());
        }
        
        return valuePager.seek(filterViewableServers(servers, subject), null);
    }


    /**
     * Get all servers.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @return A List of ServerValue objects representing all of the
     * servers that the given subject is allowed to view.
     */
    public PageList getAllServers(AuthzSubject subject, PageControl pc)
        throws FinderException, PermissionException {
        Collection servers = getViewableServers(subject, pc);
        
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(servers, pc);
    }

    /**
     * Get the scope of viewable servers for a given user
     * @param subject - the user
     * @return List of ServerLocals for which subject has 
     * AuthzConstants.serverOpViewServer
     */
    private Collection getViewableServers(AuthzSubject subject,
                                          PageControl pc)
        throws PermissionException, FinderException {
        Collection servers;
        List authzPks = getViewableServers(subject);
        int attr = -1;
        if (pc != null) {
            attr = pc.getSortattribute();
        }
        switch(attr) {
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
     * @param serverIds {@link Collection} of {@link Server.getId}
     * @return {@link Collection} of {@link Server}
     */
    private Collection getServersFromIds(Collection serverIds, boolean asc) {
        final List rtn = new ArrayList(serverIds.size());
        for (Iterator it=serverIds.iterator(); it.hasNext(); ) {
            final Integer id = (Integer)it.next();
            try {
                final Server server = findServerById(id);
                final Resource r = server.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                rtn.add(server);
            } catch (ServerNotFoundException e) {
                log.debug(e.getMessage(), e);
            }
        }
        Collections.sort(rtn, new AppdefNameComparator(asc));
        return rtn;
    }
    
    /**
     * @ejb:interface-method
     */
    public Collection getViewableServers(AuthzSubject subject,
                                         Platform platform) {
        return filterViewableServers(platform.getServers(), subject);        
    }

    private Collection getServersByPlatformImpl( AuthzSubject subject,
                                                 Integer platId,
                                                 Integer servTypeId,
                                                 boolean excludeVirtual,
                                                 PageControl pc)
        throws PermissionException, ServerNotFoundException, 
               PlatformNotFoundException {
        List authzPks;
        try {
            authzPks = getViewableServers(subject);
        } catch(FinderException exc){
            throw new ServerNotFoundException(
                "No (viewable) servers associated with platform " + platId);
        }
        
        List servers;
        // first, if they specified a server type, then filter on it
        if(!servTypeId.equals(APPDEF_RES_TYPE_UNDEFINED)) {
            if(!excludeVirtual) {
                servers = getServerDAO()
                    .findByPlatformAndType_orderName(platId, servTypeId);
            } else {
                servers = getServerDAO()
                    .findByPlatformAndType_orderName(platId, servTypeId,
                                                     Boolean.FALSE);
            }
        }
        else {
            if(!excludeVirtual) {
                servers = getServerDAO().findByPlatform_orderName(platId);
            } else {
                servers = getServerDAO()
                    .findByPlatform_orderName(platId, Boolean.FALSE);
            }
        }
        for(Iterator i = servers.iterator(); i.hasNext();) {
            Server aServer = (Server) i.next();
            
            // Keep the virtual ones, we need them so that child services can be
            // added.  Otherwise, no one except the super user will have access
            // to the virtual services
            if (aServer.getServerType().isVirtual())
                continue;
            
            // Remove the server if its not viewable
            if (!authzPks.contains(aServer.getId())) {
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
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param platId platform id.
     * @param excludeVirtual true if you dont want virtual (fake container) servers
     * in the returned list
     * @param pc The page control.
     * @return A PageList of ServerValue objects representing servers on the
     * specified platform that the subject is allowed to view.
     */
    public PageList getServersByPlatform ( AuthzSubject subject,
                                           Integer platId,
                                           boolean excludeVirtual,
                                           PageControl pc) 
        throws ServerNotFoundException, PlatformNotFoundException,
               PermissionException 
    {
        return getServersByPlatform(subject, platId, APPDEF_RES_TYPE_UNDEFINED,
                                    excludeVirtual, pc);
    }

    /**
     * Get servers by server type and platform.
     * 
     * @ejb:interface-method
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
    public PageList getServersByPlatform ( AuthzSubject subject,
                                           Integer platId,
                                           Integer servTypeId,
                                           boolean excludeVirtual,
                                           PageControl pc) 
        throws ServerNotFoundException, PlatformNotFoundException, 
               PermissionException 
    {
        Collection servers = getServersByPlatformImpl(subject, platId,
                                                      servTypeId,
                                                      excludeVirtual, pc);
        
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(servers, pc);
    }

    /**
     * Get servers by server type and platform.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param platId platform id.
     * @return A PageList of ServerValue objects representing servers on the
     * specified platform that the subject is allowed to view.
     */
    public PageList getServersByPlatformServiceType( AuthzSubject subject,
                                                     Integer platId,
                                                     Integer svcTypeId)
        throws ServerNotFoundException, PlatformNotFoundException, 
               PermissionException {
        PageControl pc = PageControl.PAGE_ALL;
        Integer servTypeId;
        try {
            ServiceType typeV = getServiceTypeDAO().findById(svcTypeId);
            servTypeId = typeV.getServerType().getId();
        } catch (ObjectNotFoundException e) {
            throw new ServerNotFoundException("Service Type not found", e);
        }
        
        Collection servers =
            getServersByPlatformImpl(subject, platId, servTypeId, false, pc);
        
        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(servers, pc);
    }
    
    /**
     * Get servers by server type and platform.
     * @ejb:interface-method
     * @param subject The subject trying to list servers.
     * @param typeId server type id.
     *
     * @return A PageList of ServerValue objects representing servers on the
     * specified platform that the subject is allowed to view.
     */
    public List getServersByType( AuthzSubject subject, String name)
        throws PermissionException, InvalidAppdefTypeException {
        try {
            ServerType ejb = getServerTypeDAO().findByName(name);
            if (ejb == null) {
                return new PageList();
            }
            
            Collection servers = getServerDAO().findByType(ejb.getId());

            List authzPks = getViewableServers(subject);        
            for(Iterator i = servers.iterator(); i.hasNext();) {
                Integer sPK = ((Server) i.next()).getId();
                // remove server if its not viewable
                if(!authzPks.contains(sPK))
                    i.remove();
            }
            
            // valuePager converts local/remote interfaces to value objects
            // as it pages through them.
            return valuePager.seek(servers, PageControl.PAGE_ALL);
        } catch (FinderException e) {
            return new ArrayList(0);
        }
    }
    
    /**
     * Get non-virtual server IDs by server type and platform.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param platId platform id.
     * @return An array of Integer[] which represent the ServerIds
     * specified platform that the subject is allowed to view.
     */
    public Integer[] getServerIdsByPlatform(AuthzSubject subject,
                                            Integer platId)
        throws ServerNotFoundException, PlatformNotFoundException, 
               PermissionException 
    {
        return getServerIdsByPlatform(
            subject, platId, APPDEF_RES_TYPE_UNDEFINED, true);
    }
    
    /**
     * Get non-virtual server IDs by server type and platform.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @param platId platform id.
     * @return An array of Integer[] which represent the ServerIds
     */
    public Integer[] getServerIdsByPlatform(AuthzSubject subject,
                                            Integer platId, Integer servTypeId)
        throws ServerNotFoundException, PlatformNotFoundException, 
               PermissionException 
    {
        return getServerIdsByPlatform(subject, platId, servTypeId, true);
    }
    
    
    /**
     * Get server IDs by server type and platform.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @param platId platform id.
     * @return A PageList of ServerValue objects representing servers on the
     * specified platform that the subject is allowed to view.
     */
    public Integer[] getServerIdsByPlatform(AuthzSubject subject,
                                            Integer platId, 
                                            Integer servTypeId,
                                            boolean excludeVirtual)
        throws ServerNotFoundException, PlatformNotFoundException, 
               PermissionException 
    {
        Collection servers = getServersByPlatformImpl(subject, platId,
                                                      servTypeId,
                                                      excludeVirtual, null);
        
        Integer[] ids = new Integer[servers.size()];
        Iterator it = servers.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Server server = (Server) it.next();
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
     * the given application that the subject is allowed to view.
     */
    private Collection getServersByApplicationImpl(AuthzSubject subject,
                                                   Integer appId,
                                                   Integer servTypeId)
        throws ServerNotFoundException, ApplicationNotFoundException,
               PermissionException {
        Iterator it;
        List authzPks;
        Application appLocal;
        Collection appServiceCollection;
        HashMap serverCollection;
    
        ApplicationDAO appLocalHome = getApplicationDAO();
        
        try {
            appLocal = appLocalHome.findById(appId);
        } catch(ObjectNotFoundException exc){
            throw new ApplicationNotFoundException(appId, exc);
        }
        
        try {
            authzPks = getViewableServers(subject);
        } catch (FinderException e) {
            throw new ServerNotFoundException("No (viewable) servers " +
                                              "associated with " +
                                              "application " + appId, e);
        }
        
        serverCollection = new HashMap();
    
        // XXX - a better solution is to control the viewable set returned by
        //       ejbql finders. This will be forthcoming.
    
        appServiceCollection = appLocal.getAppServices();
        it = appServiceCollection.iterator();
    
        while (it.hasNext()) {
    
            AppService appService = (AppService) it.next();

            if ( appService.isIsGroup() ) {
                Collection services =
                    getServiceCluster(appService.getResourceGroup())
                        .getServices();
                
                Iterator serviceIterator = services.iterator();
                while ( serviceIterator.hasNext() ) {
                    Service service = (Service)serviceIterator.next();
                    Server server = service.getServer();
                    
                    // Don't bother with entire cluster if type is platform svc
                    if (server.getServerType().isVirtual())
                        break;

                    Integer serverId = server.getId();
                    
                    if (serverCollection.containsKey(serverId))
                        continue;
                    
                    serverCollection.put(serverId, server);
                }
            } else {
                Server server = appService.getService().getServer();
                if (!server.getServerType().isVirtual()) {
                    Integer serverId = server.getId();
                    
                    if (serverCollection.containsKey(serverId))
                        continue;
                    
                    serverCollection.put(serverId, server);
                }
            }
        }
    
        for(Iterator i = serverCollection.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            Server aServer = (Server) entry.getValue();
            
            // first, if they specified a server type, then filter on it
            if(servTypeId != APPDEF_RES_TYPE_UNDEFINED && 
               !(aServer.getServerType().getId().equals(servTypeId)) ) {
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
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @param pc The page control for this page list.
     * @return A List of ServerValue objects representing servers that support 
     * the given application that the subject is allowed to view.
     */
    public PageList getServersByApplication(AuthzSubject subject,
                                            Integer appId, PageControl pc)
        throws ServerNotFoundException, ApplicationNotFoundException,
               PermissionException {
        return getServersByApplication(subject, appId,
                                       APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    /**
     * Get servers by application and serverType.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @param pc The page control for this page list.
     * @return A List of ServerValue objects representing servers that support 
     * the given application that the subject is allowed to view.
     */
    public PageList getServersByApplication(AuthzSubject subject, Integer appId,
                                            Integer servTypeId, PageControl pc)
        throws ServerNotFoundException, ApplicationNotFoundException,
               PermissionException {
        Collection serverCollection =
            getServersByApplicationImpl(subject, appId, servTypeId);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverCollection, pc);
    }

    /**
     * Get server IDs by application and serverType.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @return A List of ServerValue objects representing servers that support
     * the given application that the subject is allowed to view.
     */
    public Integer[] getServerIdsByApplication(AuthzSubject subject,
                                               Integer appId,
                                               Integer servTypeId)
        throws ServerNotFoundException, ApplicationNotFoundException,
               PermissionException {
        Collection servers =
            getServersByApplicationImpl(subject, appId, servTypeId);
        
        Integer[] ids = new Integer[servers.size()];
        Iterator it = servers.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Server server = (Server) it.next();
            ids[i] = server.getId();
        }

        return ids;
    }

    /** 
     * Update a server
     * @param existing 
     * @ejb:interface-method
     */
    public Server updateServer(AuthzSubject subject, ServerValue existing)
        throws PermissionException, UpdateException,
               AppdefDuplicateNameException, ServerNotFoundException {
        try {
            Server server =
                getServerDAO().findById(existing.getId());
            checkModifyPermission(subject, server.getEntityId());
            existing.setModifiedBy(subject.getName());
            existing.setMTime(new Long(System.currentTimeMillis()));
            trimStrings(existing);
            
            if (server.matchesValueObject(existing)) {
                log.debug("No changes found between value object and entity");
            } else {
                if(!existing.getName().equals(server.getName())) {
                    Resource rv = server.getResource();
                    rv.setName(existing.getName());
                }

                server.updateServer(existing);
            }
            return server;
        } catch (ObjectNotFoundException e) {
            throw new ServerNotFoundException(existing.getId(), e);
        }
    }

    /**
     * Update server types
     * @ejb:interface-method
     */
    public void updateServerTypes(String plugin, ServerTypeInfo[] infos)
        throws CreateException, FinderException, RemoveException, VetoException 
    {
        // First, put all of the infos into a Hash
        HashMap infoMap = new HashMap();
        for (int i = 0; i < infos.length; i++) {
            String name = infos[i].getName();
            ServerTypeInfo sinfo =
                (ServerTypeInfo)infoMap.get(name);

            if (sinfo == null) {
                //first time we've seen this type
                //clone it incase we have to update the platforms
                infoMap.put(name, infos[i].clone());
            }
            else {
                //already seen this type; just update the platforms.
                //this allows server types of the same name to support
                //different families of platforms in the plugins.
                String[] platforms =
                    (String[])ArrayUtil.merge(sinfo.getValidPlatformTypes(),
                                              infos[i].getValidPlatformTypes(),
                                              new String[0]);
                sinfo.setValidPlatformTypes(platforms);
            }
        }

        ServerTypeDAO stDao = getServerTypeDAO();
        Collection curServers = stDao.findByPlugin(plugin);

        AuthzSubject overlord = 
            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        ResourceGroupManagerLocal resGroupMan = 
            ResourceGroupManagerEJBImpl.getOne();
        ResourceManagerLocal resMan = ResourceManagerEJBImpl.getOne();
        
        for (Iterator i = curServers.iterator(); i.hasNext();) {
            ServerType serverType = (ServerType) i.next();
            String serverName = serverType.getName();
            ServerTypeInfo sinfo =
                (ServerTypeInfo) infoMap.remove(serverName);

            if (sinfo == null) {
                deleteServerType(serverType, overlord, resGroupMan, resMan);
            } else {
                String curDesc = serverType.getDescription();
                Collection curPlats = serverType.getPlatformTypes();
                String newDesc = sinfo.getDescription();
                String[] newPlats = sinfo.getValidPlatformTypes();
                boolean  updatePlats;

                log.debug("Updating ServerType: " + serverName);
                        
                if (!newDesc.equals(curDesc))
                    serverType.setDescription(newDesc);

                // See if we need to update the supported platforms
                updatePlats = newPlats.length != curPlats.size();
                if(updatePlats == false){
                    // Ensure that the lists are the same
                    for(Iterator k = curPlats.iterator(); k.hasNext(); ){
                        PlatformType pLocal = (PlatformType)k.next();
                        int j;
                            
                        for(j=0; j<newPlats.length; j++){
                            if(newPlats[j].equals(pLocal.getName()))
                                break;
                        }
                        if(j == newPlats.length){
                            updatePlats = true;
                            break;
                        }
                    }
                }

                if(updatePlats == true){
                    findAndSetPlatformType(newPlats, serverType);
                }
            }
        }
            
        Resource prototype = 
            ResourceManagerEJBImpl.getOne().findRootResource();
        
        // Now create the left-overs
        for (Iterator i = infoMap.values().iterator(); i.hasNext(); ) {
            ServerTypeInfo sinfo = (ServerTypeInfo) i.next();
            ServerType stype = new ServerType();

            log.debug("Creating new ServerType: " + sinfo.getName());
            stype.setPlugin(plugin);
            stype.setName(sinfo.getName());
            stype.setDescription(sinfo.getDescription());
            stype.setVirtual(sinfo.isVirtual());
            String newPlats[] = sinfo.getValidPlatformTypes();
            findAndSetPlatformType(newPlats, stype);
            
            stype = stDao.create(stype);
            createAuthzResource(overlord, getServerPrototypeResourceType(),
                                prototype, stype.getId(), stype.getName(),
                                null);  // No parent
        }
    }

    /**
     * @ejb:interface-method
     */
    public void deleteServerType(ServerType serverType, AuthzSubject overlord,
                                 ResourceGroupManagerLocal resGroupMan,
                                 ResourceManagerLocal resMan)
        throws VetoException, RemoveException {
        // Need to remove all service types
        ServiceManagerLocal svcMan = ServiceManagerEJBImpl.getOne();
        ServiceType[] types = (ServiceType[])
            serverType.getServiceTypes().toArray(new ServiceType[0]);
        for (int i = 0; i < types.length; i++) {
            svcMan.deleteServiceType(types[i], overlord, resGroupMan, resMan);
        }

        log.debug("Removing ServerType: " + serverType.getName());
        Integer typeId = AuthzConstants.authzServerProto;
        Resource proto = 
            resMan.findResourceByInstanceId(typeId, serverType.getId());
        
        try {
            resGroupMan.removeGroupsCompatibleWith(proto);
        
            // Remove all servers
            Server[] servers = (Server[])
                serverType.getServers().toArray(new Server[0]);
            for (int i = 0; i < servers.length; i++) {
                removeServer(overlord, servers[i]);
            }
        } catch (PermissionException e) {
            assert false :
                "Overlord should not run into PermissionException";
        }
        
        ServerTypeDAO dao = new ServerTypeDAO(DAOFactory.getDAOFactory());
        dao.remove(serverType);
        
        resMan.removeResource(overlord, proto);
    }

    /**
     * @ejb:interface-method
     */
    public void setAutodiscoveryZombie(Server server, boolean zombie) {
        server.setAutodiscoveryZombie(zombie);
    }
    
    /**
     * Get a Set of PlatformTypeLocal objects which map to the names
     * as given by the argument.
     */
    private void findAndSetPlatformType(String[] platNames, ServerType stype)
        throws FinderException {
        PlatformTypeDAO platHome =
            new PlatformTypeDAO(DAOFactory.getDAOFactory());
            
        for (int i = 0; i < platNames.length; i++) {
            PlatformType pType = platHome.findByName(platNames[i]);
            if (pType == null) {
                throw new FinderException("Could not find platform type '" +
                                          platNames[i] + "'");
            }
            stype.addPlatformType(pType);
        }
    }

    /**
     * Create the Authz resource and verify that the user has 
     * correct permissions
     */
    private void createAuthzServer(AuthzSubject subject, Server server)
        throws CreateException, FinderException, PermissionException 
    {
        log.debug("Being Authz CreateServer");
        if(log.isDebugEnabled()) {
            log.debug("Checking for: " + AuthzConstants.platformOpAddServer + 
                " for subject: " + subject);
        }
        AppdefEntityID platId = server.getPlatform().getEntityId();
        checkPermission(subject, getPlatformResourceType(), platId.getId(),
                        AuthzConstants.platformOpAddServer);

        ResourceType serverProto = getServerPrototypeResourceType();
        ServerType serverType = server.getServerType();
        Resource proto = ResourceManagerEJBImpl.getOne()
            .findResourceByInstanceId(serverProto, serverType.getId());
        Resource parent = getResourceManager().findResource(platId);

        if (parent == null) {
            throw new SystemException("Unable to find parent platform [id=" +
                                      platId + "]");
        }
        Resource resource = createAuthzResource(subject,
                                                getServerResourceType(), proto,
                                                server.getId(),
                                                server.getName(),
                                                serverType.isVirtual(), parent);
        server.setResource(resource);
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
     * Returns a list of 2 element arrays.  The first element is the name of
     * the server type, the second element is the # of servers of that
     * type in the inventory.
     * 
     * @ejb:interface-method
     */
    public List getServerTypeCounts() {
        return getServerDAO().getServerTypeCounts(); 
    }
    
    /**
     * Get the # of servers within HQ inventory.  This method ingores virtual
     * server types.
     * @ejb:interface-method
     */
    public Number getServerCount() {
        return getServerDAO().getServerCount();
    }

    public static ServerManagerLocal getOne() {
        try {
            return ServerManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Create a server manager session bean.
     * @exception CreateException If an error occurs creating the pager
     * for the bean.
     */
    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }
    

    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
}
