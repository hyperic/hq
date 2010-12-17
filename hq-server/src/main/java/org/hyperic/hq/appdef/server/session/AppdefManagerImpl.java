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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing appdef objects in EE
 * 
 */
@org.springframework.stereotype.Service
@Transactional
public class AppdefManagerImpl implements AppdefManager {

    

    private PermissionManager permissionManager;

    private ResourceManager resourceManager;
    
    private PlatformManager platformManager;
    
    private ServerManager serverManager;
    
    private ServiceManager serviceManager;

    @Autowired
    public AppdefManagerImpl(
                             PermissionManager permissionManager, ResourceManager resourceManager, PlatformManager platformManager,
                             ServerManager serverManager, ServiceManager serviceManager) {

        this.permissionManager = permissionManager;
        this.resourceManager = resourceManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
    }

    

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> getControllablePlatformTypes(AuthzSubject subject) throws PermissionException {
        List<Integer> typeIds = findOperableResourceIds(subject, "EAM_PLATFORM", "platform_type_id",
            AuthzConstants.platformResType, AuthzConstants.platformOpControlPlatform, null);

        TreeMap<String, AppdefEntityID> platformTypes = new TreeMap<String, AppdefEntityID>();
        for (Integer typeId : typeIds) {
            try {
                PlatformType pt = platformManager.findPlatformType(typeId);
                platformTypes.put(pt.getName(), AppdefEntityTypeID.newPlatformID(typeId));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }

        return platformTypes;
    }

    /**
     * Get controllable platform types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> getControllablePlatformNames(AuthzSubject subject, int tid)
        throws PermissionException {
        List<Integer> ids = findOperableResourceIds(subject, "EAM_PLATFORM", "id", AuthzConstants.platformResType,
            AuthzConstants.platformOpControlPlatform, "platform_type_id=" + tid);

        TreeMap<String, AppdefEntityID> platformNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Platform plat = platformManager.findPlatformById(id);
                platformNames.put(plat.getName(), AppdefEntityID.newPlatformID(id));
            } catch (PlatformNotFoundException e) {
                continue;
            }
        }

        return platformNames;
    }

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityTypeID> getControllableServerTypes(AuthzSubject subject) throws PermissionException {
        List<Integer> typeIds = findOperableResourceIds(subject, "EAM_SERVER", "server_type_id",
            AuthzConstants.serverResType, AuthzConstants.serverOpControlServer, null);

        TreeMap<String, AppdefEntityTypeID> serverTypes = new TreeMap<String, AppdefEntityTypeID>();
        for (Integer typeId : typeIds) {

            try {
                ServerType st = serverManager.findServerType(typeId);
                
                    serverTypes.put(st.getName(), new AppdefEntityTypeID(AppdefEntityConstants.APPDEF_TYPE_SERVER,
                        typeId));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }

        return serverTypes;
    }

    /**
     * Get controllable server types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> getControllableServerNames(AuthzSubject subject, int tid)
        throws PermissionException {
        List<Integer> ids = findOperableResourceIds(subject, "EAM_SERVER", "id", AuthzConstants.serverResType,
            AuthzConstants.serverOpControlServer, "server_type_id=" + tid);

        TreeMap<String, AppdefEntityID> serverNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Server svr = serverManager.findServerById(id);
                serverNames.put(svr.getName(), AppdefEntityID.newServerID(id));
            } catch (ServerNotFoundException e) {
                continue;
            }
        }

        return serverNames;
    }

    /**
     * Get controllable service types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityTypeID> getControllableServiceTypes(AuthzSubject subject) throws PermissionException {
        List<Integer> typeIds = findOperableResourceIds(subject, "EAM_SERVICE", "service_type_id",
            AuthzConstants.serviceResType, AuthzConstants.serviceOpControlService, null);

        TreeMap<String, AppdefEntityTypeID> serviceTypes = new TreeMap<String, AppdefEntityTypeID>();
        for (Integer typeId : typeIds) {

            try {
                ServiceType st = serviceManager.findServiceType(typeId);
                serviceTypes.put(st.getName(),
                    new AppdefEntityTypeID(AppdefEntityConstants.APPDEF_TYPE_SERVICE, typeId));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }

        return serviceTypes;
    }

    /**
     * Get controllable service types
     * @param subject
     * @return a list of ServerTypeValue objects
     * @throws PermissionException
     * 
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> getControllableServiceNames(AuthzSubject subject, int tid)
        throws PermissionException {
        List<Integer> ids = findOperableResourceIds(subject, "EAM_SERVICE", "id", AuthzConstants.serviceResType,
            AuthzConstants.serviceOpControlService, "service_type_id=" + tid);

        TreeMap<String, AppdefEntityID> serviceNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Service svc = serviceManager.findServiceById(id);
                serviceNames.put(svc.getName(), AppdefEntityID.newServiceID(id));
            } catch (ServiceNotFoundException e) {
                continue;
            }
        }

        return serviceNames;
    }
    
    private List<Integer> findOperableResourceIds(AuthzSubject subject, String tableName, 
        String fieldName, String resType, String opType, String sumthn) {
        //TODO from OperationDAO
        return new ArrayList<Integer>();
    }

    /**
     * Change appdef entity owner
     * 
     * 
     */
    public void changeOwner(AuthzSubject who, AppdefResource res, AuthzSubject newOwner) throws PermissionException,
        ServerNotFoundException {
        // check if the caller can modify this server
        permissionManager.checkModifyPermission(who, res.getEntityId());
        // now get its authz resource
        Resource authzRes = res.getResource();
        // change the authz owner
        resourceManager.setResourceOwner(who, authzRes, newOwner);
    }
}
