/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.appdef.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for ServerManager.
 */
public interface ServerManager {
  
    /**
     * Create a Server on the given platform.
     * @return ServerValue - the saved value object
     * 
     */
    public Server createServer(AuthzSubject subject, Integer platformId, Integer serverTypeId, ServerValue sValue)
        throws ValidationException, PermissionException, PlatformNotFoundException,
        org.hyperic.hq.appdef.shared.AppdefDuplicateNameException, NotFoundException;
    
    /**
     * The same as a regular server, but association to Platform is through a "virtual" relation. 
     *  We keep this purely to handle legacy AI runtime reports that report platform services under a "virtual" server
     *  that has a unique ID but is invisible to the user
     *  We also don't set the AppdefType so these won't get picked up on queries for all servers.
     * A virtual server should only exist in the system if queried specifically by ID or name
     * @param subject
     * @param platformId
     * @param serverTypeId
     * @param sValue
     * @return
     * @throws ValidationException
     * @throws PermissionException
     * @throws PlatformNotFoundException
     * @throws AppdefDuplicateNameException
     * @throws NotFoundException
     */
    
    public Server createVirtualServer(AuthzSubject subject, Integer platformId, Integer serverTypeId, ServerValue sValue) 
        throws ValidationException, PermissionException, PlatformNotFoundException, AppdefDuplicateNameException,
        NotFoundException;

    /**
     * A removeServer method that takes a ServerLocal. Used by
     * PlatformManager.removePlatform when cascading removal to servers.
     */
    public void removeServer(AuthzSubject subject, Server server) throws  PermissionException,
        VetoException;
    
    
    public void removeServer(AuthzSubject subject, Integer serverId) throws  PermissionException,
    VetoException;

    
    ServerType createServerType(ServerTypeInfo sinfo, String plugin) throws NotFoundException;
    
    ServerType createVirtualServerType(ServerTypeInfo sinfo, String plugin) throws NotFoundException;

    /**
     * Find all server types
     * @return list of serverTypeValues
     */
    public PageList<ServerTypeValue> getAllServerTypes(AuthzSubject subject, PageControl pc);
    
    PageList<Resource> getAllServerResources(AuthzSubject subject, PageControl pc);

    /**
     * Find viewable server types
     * @return list of serverTypeValues
     */
    public PageList<ServerTypeValue> getViewableServerTypes(AuthzSubject subject, PageControl pc)
        throws  PermissionException, NotFoundException;

    /**
     * Find viewable server types for a platform
     * @return list of serverTypeValues
     */
    public PageList<ServerTypeValue> getServerTypesByPlatform(AuthzSubject subject, Integer platId,PageControl pc)
        throws PermissionException, PlatformNotFoundException, ServerNotFoundException;

    /**
     * Find all ServerTypes for a givent PlatformType id. This can go once we
     * begin passing POJOs to the UI layer.
     * @return A list of ServerTypeValue objects for thie PlatformType.
     */
    public PageList<ServerTypeValue> getServerTypesByPlatformType(AuthzSubject subject, Integer platformTypeId,
                                                                  PageControl pc) throws PlatformNotFoundException;

    public Server findServerByAIID(AuthzSubject subject, Platform platform, String aiid) throws PermissionException;

    /**
     * Find a Server by Id.
     */
    public Server findServerById(Integer id) throws ServerNotFoundException;

    /**
     * Get a Server by Id.
     * @return The Server with the given id, or null if not found.
     */
    public Server getServerById(Integer id);

    /**
     * Find a ServerType by id
     */
    public ServerType findServerType(Integer id);

    /**
     * Find a server type by name
     * @param name - the name of the server
     * @return ServerTypeValue
     */
    public ServerType findServerTypeByName(String name) throws NotFoundException;
    
    public List<Server> findServersByType(Platform p, ServerType st);

    /**
     * Get server lite value by id. Does not check permission.
     */
    public Server getServerById(AuthzSubject subject, Integer id) throws ServerNotFoundException, PermissionException;

    /**
     * Get server IDs by server type.
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @return An array of Server IDs.
     */
    public Integer[] getServerIds(AuthzSubject subject, Integer servTypeId) throws PermissionException;

    /**
     * Get server by service.
     */
    public ServerValue getServerByService(AuthzSubject subject, Integer sID) throws ServerNotFoundException,
        org.hyperic.hq.appdef.shared.ServiceNotFoundException, PermissionException;

    /**
     * Get server by service
     */
    public PageList<ServerValue> getServersByServices(AuthzSubject subject, List<AppdefEntityID> sIDs)
        throws PermissionException, ServerNotFoundException;

    public Collection<Server> getViewableServers(AuthzSubject subject, Platform platform);

    /**
     * Get servers by platform.
     * @param subject The subject trying to list servers.
     * @param platId platform id.
     * @param pc The page control.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    public PageList<ServerValue> getServersByPlatform(AuthzSubject subject, Integer platId, 
                                                      PageControl pc) throws ServerNotFoundException,
        PlatformNotFoundException, PermissionException;

    /**
     * Get servers by server type and platform.
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @param platId platform id.
     * @param pc The page control.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    public PageList<ServerValue> getServersByPlatform(AuthzSubject subject, Integer platId, Integer servTypeId, PageControl pc)
        throws ServerNotFoundException, PlatformNotFoundException, PermissionException;

    /**
     * Get servers by server type and platform.
     * @param subject The subject trying to list servers.
     * @param platId platform id.
     * @return A PageList of ServerValue objects representing servers on the
     *         specified platform that the subject is allowed to view.
     */
    public PageList<ServerValue> getServersByPlatformServiceType(AuthzSubject subject, Integer platId, Integer svcTypeId)
        throws ServerNotFoundException, PlatformNotFoundException, PermissionException;

  

    /**
     * Get server IDs by server type and platform.
     * @param subject The subject trying to list servers.
     * @param servTypeId server type id.
     * @param platId platform id.
     * @return An array of Integer[] which represent the ServerIds
     */
    public Integer[] getServerIdsByPlatform(AuthzSubject subject, Integer platId, Integer servTypeId)
        throws ServerNotFoundException, PlatformNotFoundException, PermissionException;


    /**
     * Get servers by application.
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @param pc The page control for this page list.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    public PageList<ServerValue> getServersByApplication(AuthzSubject subject, Integer appId, PageControl pc)
        throws ServerNotFoundException, ApplicationNotFoundException, PermissionException;

    /**
     * Get servers by application and serverType.
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @param pc The page control for this page list.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    public PageList<ServerValue> getServersByApplication(AuthzSubject subject, Integer appId, Integer servTypeId,
                                                         PageControl pc) throws ServerNotFoundException,
        ApplicationNotFoundException, PermissionException;

    /**
     * Get server IDs by application and serverType.
     * @param subject The subject trying to list servers.
     * @param appId Application id.
     * @return A List of ServerValue objects representing servers that support
     *         the given application that the subject is allowed to view.
     */
    public Integer[] getServerIdsByApplication(AuthzSubject subject, Integer appId, Integer servTypeId)
        throws ServerNotFoundException, ApplicationNotFoundException, PermissionException;

    /**
     * Update a server
     * @param existing
     */
    public Server updateServer(AuthzSubject subject, ServerValue existing) throws PermissionException, UpdateException,
        AppdefDuplicateNameException, ServerNotFoundException;

    /**
     * Update server types
     */
    public void updateServerTypes(String plugin, org.hyperic.hq.product.ServerTypeInfo[] infos) throws 
          VetoException, NotFoundException;


    public void setAutodiscoveryZombie(Server server, boolean zombie);

    /**
     * @return A Map of server type to the # of servers of that type in the
     * inventory.
     */
    public Map<String,Integer> getServerTypeCounts();
    
    Number getServerCount() ;
}
