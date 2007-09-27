/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.hq.dao.PlatformTypeDAO;
import org.hyperic.hq.dao.ServerDAO;
import org.hyperic.hq.dao.ConfigResponseDAO;
import org.hyperic.hq.dao.ApplicationDAO;
import org.hyperic.dao.DAOFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.zevents.ZeventManager;

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
     * Create a Server on the given platform.
     *
     * @return ServerValue - the saved value object
     * @exception CreateException - if it fails to add the server
     * @ejb:interface-method
     */
    public Server createServer(AuthzSubjectValue subject, Integer platformId,
                               Integer serverTypeId, ServerValue sValue)
        throws CreateException, ValidationException, PermissionException,
               PlatformNotFoundException, AppdefDuplicateNameException {
        try {
            trimStrings(sValue);

            Platform platform =
                getPlatformMgrLocal().findPlatformById(platformId);
            ServerType serverType = getServerTypeDAO().findById(serverTypeId);

            sValue.setServerType(serverType.getServerTypeValue());
            sValue.setOwner(subject.getName());
            sValue.setModifiedBy(subject.getName());
            
            // validate the object
            validateNewServer(platform, sValue);
            
            // create it
            Server server = getServerDAO().create(sValue, platform);

            // Add server to parent collection
            Collection servers = platform.getServers();
            if (!servers.contains(server)) {
                servers.add(server);
            }

            createAuthzServer(sValue.getName(),
                              server.getId(),
                              platformId,
                              serverType.getVirtual(), 
                              subject);

            // Send resource create event
            ResourceCreatedZevent zevent =
                new ResourceCreatedZevent(subject, server.getEntityId());
            ZeventManager.getInstance().enqueueEventAfterCommit(zevent);

            return server;
        } catch (CreateException e) {
            throw e;
        } catch (PermissionException e) {
            // rollback the transaction if no permission to create
            // a server; otherwise, a server record gets created without its
            // corresponding resource record.
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
    public Server createVirtualServer(AuthzSubjectValue subject,
                                      Platform platform, ServerType st)
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
        Collection servers = platform.getServers();
        if (!servers.contains(server)) {
            servers.add(server);
        }

        createAuthzServer(server.getName(),
                          server.getId(),
                          platform.getId(),
                          st.isVirtual(), 
                          subject);
        return server;
    }
    
    /**
     * Remove a server
     * @param subject The user issuing the delete operation.
     * @param id  The id of the Server.
     * @ejb:interface-method
     */
    public void removeServer(AuthzSubjectValue subject, Integer id)
        throws ServerNotFoundException, RemoveException, PermissionException {

        Server server;
        try {
            server = getServerDAO().findById(id);
        } catch (ObjectNotFoundException e) {
            throw new ServerNotFoundException(id);
        }
        removeServer(subject, server);
    }

    /**
     * A removeServer method that takes a ServerLocal.  Used by
     * PlatformManager.removePlatform when cascading removal to servers.
     * @ejb:interface-method
     */
    public void removeServer(AuthzSubjectValue subject, Server server)
        throws ServerNotFoundException, RemoveException, PermissionException
    {
        AppdefEntityID aeid = server.getEntityId();
        try {
            checkRemovePermission(subject, server.getEntityId());

            // Service manager will update the collection, so we need to copy
            Collection services = new ArrayList(server.getServices());
            for (Iterator i = services.iterator(); i.hasNext(); ) {
                Service service = (Service)i.next();
                getServiceMgrLocal().removeService(subject, service);
            }

            // Keep config response ID so it can be deleted later.
            Integer cid = server.getConfigResponseId();

            // Remove authz resource
            removeAuthzResource(subject, aeid);

            // Remove server from parent Platform Server collection.
            Platform platform = server.getPlatform();
            Collection servers = platform.getServers();
            for (Iterator i = servers.iterator(); i.hasNext(); ) {
                Server s = (Server)i.next();
                if (s.equals(server)) {
                    i.remove();
                    break;
                }
            }
            
            getServerDAO().remove(server);

            // Remove the config response
            if (cid != null) {
                try {
                    ConfigResponseDAO cdao =
                        DAOFactory.getDAOFactory().getConfigResponseDAO();
                    cdao.remove(cdao.findById(cid));
                } catch (ObjectNotFoundException e) {
                    // OK, no config response, just log it
                    log.warn("Invalid config ID " + cid);
                }
            }

            deleteCustomProperties(aeid);
        } catch (FinderException e) {
            throw new ServerNotFoundException(aeid.getId(), e);
        }
    }

    /**
     * Find all server types
     * @return list of serverTypeValues
     * @ejb:interface-method
     */
    public PageList getAllServerTypes(AuthzSubjectValue subject,
                                      PageControl pc)
        throws FinderException {

        Collection serverTypes = getServerTypeDAO().findAllOrderByName();

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverTypes, pc);
    }
        
    /**
     * Find viewable server types
     * @return list of serverTypeValues
     * @ejb:interface-method
     */
    public PageList getViewableServerTypes(AuthzSubjectValue subject,
                                      PageControl pc)
        throws FinderException, PermissionException {

        // build the server types from the visible list of servers
        Collection servers;
        servers = getViewableServers(subject, pc);

        Collection serverTypes = filterResourceTypes(servers);

        // valuePager converts local/remote interfaces to value objects
        // as it pages through them.
        return valuePager.seek(serverTypes, pc);
    }
    
    /**
     * Find viewable server non-virtual types for a platform
     * @return list of serverTypeValues
     * @ejb:interface-method
     */
    public PageList getServerTypesByPlatform(AuthzSubjectValue subject,
                                             Integer platId,
                                             PageControl pc)
        throws PermissionException, PlatformNotFoundException, 
               ServerNotFoundException {    
        return this.getServerTypesByPlatform(subject, platId, true, pc);
    }
    /**
     * Find viewable server types for a platform
     * @return list of serverTypeValues
     * @ejb:interface-method
     */
    public PageList getServerTypesByPlatform(AuthzSubjectValue subject,
                                             Integer platId,
                                             boolean excludeVirtual,
                                             PageControl pc)
        throws PermissionException, PlatformNotFoundException, 
               ServerNotFoundException {

        // build the server types from the visible list of servers
        Collection servers;
        servers = getServersByPlatformImpl(subject, 
                                           platId, 
                                           this.APPDEF_RES_TYPE_UNDEFINED,
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
    public PageList getServerTypesByPlatformType(AuthzSubjectValue subject,
                                                 Integer platformTypeId,
                                                 PageControl pc)
        throws PlatformNotFoundException
    {
        PlatformManagerLocal platformManager = getPlatformMgrLocal();
        PlatformType platType =
            platformManager.findPlatformTypeById(platformTypeId);

        Collection serverTypes = platType.getServerTypes();

        return valuePager.seek(serverTypes, pc);
    }

    /**
     * Find a ServerValue by Id.
     * @ejb:interface-method
     * @deprecated Use findServerById instead.
     */
    public ServerValue findServerValueById(AuthzSubjectValue subject,
                                           Integer id)
        throws ServerNotFoundException {

        Server server = findServerById(id);
        return server.getServerValue();
    }

    /**
     * Find a Server by Id.
     * @ejb:interface-method
     */
    public Server findServerById(Integer id)
        throws ServerNotFoundException
    {
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
     * Find servers by name
     * @param subject - who
     * @param name - name of server
     * @ejb:interface-method
     */
    public ServerValue[] findServersByName(AuthzSubjectValue subject,
                                           String name)
        throws ServerNotFoundException
    {
        List serverLocals = getServerDAO().findByName(name);

        int numServers = serverLocals.size();
        if (numServers == 0) {
            throw new ServerNotFoundException("Server '" +
                                              name + "' not found");
        }

        List servers = new ArrayList();
        for (int i = 0; i < numServers; i++) {
            Server sLocal = (Server)serverLocals.get(i);
            ServerValue sValue = sLocal.getServerValue();
            try {
                checkViewPermission(subject, sValue.getEntityId());
                servers.add(sValue);
            } catch (PermissionException e) {
                //Ok, won't be added to the list
            }
        }
        return (ServerValue[])servers.toArray(new ServerValue[0]);
    }

    /**
     * Find a server type by id
     * @param id - The ID of the server
     * @return ServerTypeValue
     * @ejb:interface-method
     */
    public ServerTypeValue findServerTypeById(Integer id)
        throws FinderException {

        ServerType st = getServerTypeDAO().findById(id);
        return st.getServerTypeValue();
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
    public ServerTypeValue findServerTypeByName(String name)
        throws FinderException {

        ServerType ejb = getServerTypeDAO().findByName(name);
        if (ejb == null) {
            throw new FinderException("name not found: " + name);
        }
        return ejb.getServerTypeValue();
    }

    /** 
     * Get server lite value by id.  Does not check permission.
     * @ejb:interface-method
     */
    public ServerLightValue getServerLightValue(AuthzSubjectValue subject,
                                                Integer id)
        throws ServerNotFoundException, PermissionException {
        Server server = findServerById(id);
        checkViewPermission(subject, server.getEntityId());
        return server.getServerLightValue();
    }

    /**
     * Get server IDs by server type.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @return An array of Server IDs.
     */
    public Integer[] getServerIds(AuthzSubjectValue subject,
                                  Integer servTypeId)
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
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (FinderException e) {
            // There are no viewable servers
            return new Integer[0];
        }
    }

    /** 
     * Get server by id.
     * @ejb:interface-method
     */
    public ServerValue getServerById(AuthzSubjectValue subject, 
                                     Integer id)
        throws ServerNotFoundException, PermissionException {

        Server s = findServerById(id);
        checkViewPermission(subject, s.getEntityId());
        return s.getServerValue();
    }

    /**
     * Get servers by name.
     * @ejb:interface-method
     * @param name - name of server
     */
    public ServerValue[] getServersByName(AuthzSubjectValue subject, String name)
        throws ServerNotFoundException {

        return findServersByName(subject, name);
    }

    /**
     * Get server by service.
     * @ejb:interface-method
     */
    public ServerValue getServerByService(AuthzSubjectValue subject,
                                          Integer sID) 
        throws ServerNotFoundException, ServiceNotFoundException, 
               PermissionException
    {
        Service svc;
        Server s;
        ServerValue serverValue;

        svc = getServiceDAO().findById(sID);
        s = svc.getServer();
        serverValue = s.getServerValue();

        checkViewPermission(subject, serverValue.getEntityId());

        return serverValue;
    }

    /**
     * Get server by service.  The virtual servers are not filtere out of
     * returned list.
     * @ejb:interface-method
     */
    public PageList getServersByServices(AuthzSubjectValue subject, 
                                         List sIDs) 
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
    public PageList getAllServers ( AuthzSubjectValue subject,
                                    PageControl pc) 
        throws FinderException, PermissionException {
        Collection servers = null;
        servers = getViewableServers(subject, pc);
        
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
    private Collection getViewableServers(AuthzSubjectValue subject,
                                          PageControl pc)
        throws PermissionException, FinderException {
        Collection servers;
        try {
            List authzPks = getViewableServers(subject);
            int attr = -1;
            if (pc != null) {
                attr = pc.getSortattribute();
            }
            switch(attr) {
                case SortAttribute.RESOURCE_NAME:
                    servers = getServerDAO().findAll_orderName(
                        pc != null ? !pc.isDescending() : true);
                    break;
                default:
                    servers = getServerDAO().findAll_orderName(true);
                    break;
            }
            for(Iterator i = servers.iterator(); i.hasNext();) {
                Integer sPK = ((Server)i.next()).getId();
                // remove server if its not viewable
                if(!authzPks.contains(sPK)) {
                    i.remove();
                }
            }
            return servers;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    private Collection getServersByPlatformImpl( AuthzSubjectValue subject,
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
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        List servers;
        // first, if they specified a server type, then filter on it
        if(servTypeId != APPDEF_RES_TYPE_UNDEFINED) {
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
                servers = getServerDAO()
                    .findByPlatform_orderName(platId);
            } else {
                servers = getServerDAO()
                    .findByPlatform_orderName(platId, Boolean.FALSE);
            }
        }
        for(Iterator i = servers.iterator(); i.hasNext();) {
            Server aServer = (Server)i.next();
            
            // Keep the virtual ones, we need them so that child services can be
            // added.  Otherwise, no one except the super user will have access
            // to the virtual services
            if (aServer.getServerType().getVirtual())
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
    public PageList getServersByPlatform ( AuthzSubjectValue subject,
                                           Integer platId,
                                           boolean excludeVirtual,
                                           PageControl pc) 
        throws ServerNotFoundException, PlatformNotFoundException,
               PermissionException 
    {
        return this.getServersByPlatform(subject, 
                                         platId,
                                         this.APPDEF_RES_TYPE_UNDEFINED, 
                                         excludeVirtual,
                                         pc);
    }

    /**
     * Get servers by server type and platform.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @param platId platform id.
     * @param pc The page control.
     * @return A PageList of ServerValue objects representing servers on the
     * specified platform that the subject is allowed to view.
     */
    public PageList getServersByPlatform ( AuthzSubjectValue subject,
                                           Integer platId,
                                           Integer servTypeId,
                                           boolean excludeVirtual,
                                           PageControl pc) 
        throws ServerNotFoundException, PlatformNotFoundException, 
               PermissionException 
    {
        Collection servers =
            getServersByPlatformImpl(subject, platId, servTypeId, excludeVirtual, pc);
        
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
    public PageList getServersByPlatformServiceType( AuthzSubjectValue subject,
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
    public List getServersByType( AuthzSubjectValue subject, Integer typeId)
        throws PermissionException {
        try {
            Collection servers = getServerDAO().findByType(typeId);

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
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * Get non-virtual server IDs by server type and platform.
     * @ejb:interface-method
     *
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @param platId platform id.
     * @return A PageList of ServerValue objects representing servers on the
     * specified platform that the subject is allowed to view.
     */
    public Integer[] getServerIdsByPlatform(AuthzSubjectValue subject,
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
    public Integer[] getServerIdsByPlatform(AuthzSubjectValue subject,
                                            Integer platId, 
                                            Integer servTypeId,
                                            boolean excludeVirtual)
        throws ServerNotFoundException, PlatformNotFoundException, 
               PermissionException 
    {
        Collection servers =
            getServersByPlatformImpl(subject, platId, servTypeId, excludeVirtual, null);
        
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
    private Collection getServersByApplicationImpl(AuthzSubjectValue subject,
                                                   Integer appId,
                                                   Integer servTypeId)
        throws ServerNotFoundException, ApplicationNotFoundException,
               PermissionException {
        Iterator it;
        List authzPks;
        Application appLocal;
        Collection appServiceCollection;
        HashMap serverCollection;
    
        try {
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
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        serverCollection = new HashMap();
    
        // XXX - a better solution is to control the viewable set returned by
        //       ejbql finders. This will be forthcoming.
    
        appServiceCollection = appLocal.getAppServices();
        it = appServiceCollection.iterator();
    
        while (it.hasNext()) {
    
            AppService appService = (AppService) it.next();

            if ( appService.getIsCluster() ) {
                Collection services = appService.getServiceCluster().getServices();
                Iterator serviceIterator = services.iterator();
                while ( serviceIterator.hasNext() ) {
                    Service service = (Service)serviceIterator.next();
                    Server server = service.getServer();
                    
                    // Don't bother with entire cluster if type is platform svc
                    if (server.getServerType().getVirtual())
                        break;

                    Integer serverId = server.getId();
                    
                    if (serverCollection.containsKey(serverId))
                        continue;
                    
                    serverCollection.put(serverId, server);
                }
            } else {
                Server server = appService.getService().getServer();
                if (!server.getServerType().getVirtual()) {
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
    public PageList getServersByApplication(AuthzSubjectValue subject,
                                            Integer appId, PageControl pc)
        throws ServerNotFoundException, ApplicationNotFoundException,
               PermissionException {
        return this.getServersByApplication(subject, appId,
                                            this.APPDEF_RES_TYPE_UNDEFINED, pc);
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
    public PageList getServersByApplication(AuthzSubjectValue subject,
                                            Integer appId, Integer servTypeId,
                                            PageControl pc)
        throws ServerNotFoundException, ApplicationNotFoundException,
               PermissionException {
        Collection serverCollection =
            this.getServersByApplicationImpl(subject, appId, servTypeId);

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
    public Integer[] getServerIdsByApplication(AuthzSubjectValue subject,
                                               Integer appId,
                                               Integer servTypeId)
        throws ServerNotFoundException, ApplicationNotFoundException,
               PermissionException {
        Collection servers =
            this.getServersByApplicationImpl(subject, appId, servTypeId);
        
        Integer[] ids = new Integer[servers.size()];
        Iterator it = servers.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Server server = (Server) it.next();
            ids[i] = server.getId();
        }

        return ids;
    }

    /**
     * Validate a new Server Type
     * @throws ValidationException
     */  
    public void validateNewServerType(ServerTypeValue stv, List suppPlatTypes) 
        throws ValidationException {
            // Currently no validation rules for new ServerTypes
    } 

    /** 
     * Update a server
     * @param existing 
     * @ejb:interface-method
     */
    public Server updateServer(AuthzSubjectValue subject,
                                    ServerValue existing)
        throws PermissionException, UpdateException,
               AppdefDuplicateNameException, ServerNotFoundException {
        try {
            Server server =
                getServerDAO().findById(existing.getId());
            checkModifyPermission(subject, server.getEntityId());
            existing.setModifiedBy(subject.getName());
            existing.setMTime(new Long(System.currentTimeMillis()));
            trimStrings(existing);
            if(!existing.getName().equals(server.getName())) {
                ResourceValue rv = getAuthzResource(getServerResourceType(),
                    existing.getId());
                rv.setName(existing.getName());
                updateAuthzResource(rv);
            }
            if(server.matchesValueObject(existing)) {
                log.debug("No changes found between value object and entity");
            } else {
                server.updateServer(existing);
            }
            return server;
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (FinderException e) {
            throw new ServerNotFoundException(existing.getId(), e);
        } catch (ObjectNotFoundException e) {
            throw new ServerNotFoundException(existing.getId(), e);
        }
    }

    /**
     * Change Server owner
     *
     * @ejb:interface-method
     */
    public void changeServerOwner(AuthzSubjectValue who,
                                         Integer serverId,
                                         AuthzSubjectValue newOwner)
        throws PermissionException, ServerNotFoundException {
        Server server;
        try {
            // first lookup the server
            server = getServerDAO().findById(serverId);
            // check if the caller can modify this server
            checkModifyPermission(who, server.getEntityId());
            // now get its authz resource
            ResourceValue authzRes = getServerResourceValue(serverId);
            // change the authz owner
            getResourceManager().setResourceOwner(who, authzRes, newOwner);
            // update the owner field in the appdef table -- YUCK
            server.setOwner(newOwner.getName());
            server.setModifiedBy(who.getName());
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (FinderException e) {
            throw new ServerNotFoundException(serverId, e);
        } catch (ObjectNotFoundException e) {
            throw new ServerNotFoundException(serverId, e);
        }
    }

    /**
     * Update server types
     * @ejb:interface-method
     * @ejb:transaction type="RequiresNew"
     */
    public void updateServerTypes(String plugin, ServerTypeInfo[] infos)
        throws CreateException, FinderException, RemoveException {

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

        ServerTypeDAO stLHome = getServerTypeDAO();
        Collection curServers = stLHome.findByPlugin(plugin);
            
        for (Iterator i = curServers.iterator(); i.hasNext();) {
            ServerType stlocal = (ServerType) i.next();
            String serverName = stlocal.getName();
            ServerTypeInfo sinfo =
                (ServerTypeInfo) infoMap.remove(serverName);

            // See if this exists
            if (sinfo == null) {
                log.debug("Removing ServerType: " + serverName);
                stLHome.remove(stlocal);
            } else {
                String   curDesc    = stlocal.getDescription();
                Collection      curPlats   = stlocal.getPlatformTypes();
                String   newDesc    = sinfo.getDescription();
                String[] newPlats   = sinfo.getValidPlatformTypes();
                boolean  updatePlats;

                log.debug("Updating ServerType: " + serverName);
                        
                if (!newDesc.equals(curDesc))
                    stlocal.setDescription(newDesc);

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
                    findAndSetPlatformType(newPlats, stlocal);
                }
            }
        }
            
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
            // Now create the server type
            stLHome.create(stype);
        }
    }

    /**
     * Get a Set of PlatformTypeLocal objects which map to the names
     * as given by the argument.
     */
    private void findAndSetPlatformType(String[] platNames, ServerType stype)
        throws FinderException {
        PlatformTypeDAO platHome =
            DAOFactory.getDAOFactory().getPlatformTypeDAO();
            
        for(int i=0; i<platNames.length; i++){
            PlatformType pType;
            
            pType = platHome.findByName(platNames[i]);
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
     * @param serverName TODO
     * @param serverId TODO
     * @param subject - the user creating
     */
    private void createAuthzServer(String serverName, 
                                   Integer serverId,
                                   Integer platformId,
                                   boolean isVirtual, 
                                   AuthzSubjectValue subject)
        throws CreateException, FinderException, PermissionException {
        log.debug("Being Authz CreateServer");
        if(log.isDebugEnabled()) {
            log.debug("Checking for: " + AuthzConstants.platformOpAddServer + 
                " for subject: " + subject);
        }
        checkPermission(subject, getPlatformResourceType(), 
                        platformId, 
                        AuthzConstants.platformOpAddServer);
        createAuthzResource(subject, 
                            getServerResourceType(), 
                            serverId,
                            serverName,
                            isVirtual);
        
    }

    /**
     * Get the AUTHZ ResourceValue for a Server
     * @return ResourceValue
     * @ejb:interface-method
     */
    public ResourceValue getServerResourceValue(Integer pk)
        throws FinderException, NamingException {
        return this.getAuthzResource(getServerResourceType(),
                                     pk);
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
