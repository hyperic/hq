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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.inventory.domain.OperationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing appdef objects in EE
 * 
 */
@org.springframework.stereotype.Service
@Transactional
public class AppdefManagerImpl implements AppdefManager {

    private PlatformDAO platformDAO;

    private PlatformTypeDAO platformTypeDAO;

    private ServerDAO serverDAO;

    private ServerTypeDAO serverTypeDAO;

    private ServiceDAO serviceDAO;

    private ServiceTypeDAO serviceTypeDAO;

    private PermissionManager permissionManager;

    private ResourceManager resourceManager;

    @Autowired
    public AppdefManagerImpl(PlatformDAO platformDAO, PlatformTypeDAO platformTypeDAO, ServerDAO serverDAO,
                             ServerTypeDAO serverTypeDAO, ServiceDAO serviceDAO, ServiceTypeDAO serviceTypeDAO,
                             PermissionManager permissionManager, ResourceManager resourceManager) {

        this.platformDAO = platformDAO;
        this.platformTypeDAO = platformTypeDAO;
        this.serverDAO = serverDAO;
        this.serverTypeDAO = serverTypeDAO;
        this.serviceDAO = serviceDAO;
        this.serviceTypeDAO = serviceTypeDAO;
        this.permissionManager = permissionManager;
        this.resourceManager = resourceManager;
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
        List<Integer> typeIds = OperationType.findOperableResourceIds(subject, "EAM_PLATFORM", "platform_type_id",
            AuthzConstants.platformResType, AuthzConstants.platformOpControlPlatform, null);

        TreeMap<String, AppdefEntityID> platformTypes = new TreeMap<String, AppdefEntityID>();
        for (Integer typeId : typeIds) {
            try {
                PlatformType pt = platformTypeDAO.findById(typeId);
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
        List<Integer> ids = OperationType.findOperableResourceIds(subject, "EAM_PLATFORM", "id", AuthzConstants.platformResType,
            AuthzConstants.platformOpControlPlatform, "platform_type_id=" + tid);

        TreeMap<String, AppdefEntityID> platformNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Platform plat = platformDAO.findById(id);
                platformNames.put(plat.getName(), AppdefEntityID.newPlatformID(id));
            } catch (ObjectNotFoundException e) {
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
        List<Integer> typeIds = OperationType.findOperableResourceIds(subject, "EAM_SERVER", "server_type_id",
            AuthzConstants.serverResType, AuthzConstants.serverOpControlServer, null);

        TreeMap<String, AppdefEntityTypeID> serverTypes = new TreeMap<String, AppdefEntityTypeID>();
        for (Integer typeId : typeIds) {

            try {
                ServerType st = serverTypeDAO.findById(typeId);
                if (!st.isVirtual())
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
        List<Integer> ids = OperationType.findOperableResourceIds(subject, "EAM_SERVER", "id", AuthzConstants.serverResType,
            AuthzConstants.serverOpControlServer, "server_type_id=" + tid);

        TreeMap<String, AppdefEntityID> serverNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Server svr = serverDAO.findById(id);
                serverNames.put(svr.getName(), AppdefEntityID.newServerID(id));
            } catch (ObjectNotFoundException e) {
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
        List<Integer> typeIds = OperationType.findOperableResourceIds(subject, "EAM_SERVICE", "service_type_id",
            AuthzConstants.serviceResType, AuthzConstants.serviceOpControlService, null);

        TreeMap<String, AppdefEntityTypeID> serviceTypes = new TreeMap<String, AppdefEntityTypeID>();
        for (Integer typeId : typeIds) {

            try {
                ServiceType st = serviceTypeDAO.findById(typeId);
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
        List<Integer> ids = OperationType.findOperableResourceIds(subject, "EAM_SERVICE", "id", AuthzConstants.serviceResType,
            AuthzConstants.serviceOpControlService, "service_type_id=" + tid);

        TreeMap<String, AppdefEntityID> serviceNames = new TreeMap<String, AppdefEntityID>();
        for (Integer id : ids) {

            try {
                Service svc = serviceDAO.findById(id);
                serviceNames.put(svc.getName(), AppdefEntityID.newServiceID(id));
            } catch (ObjectNotFoundException e) {
                continue;
            }
        }

        return serviceNames;
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
        // update the modified field in the appdef table -- YUCK
        res.setModifiedBy(who.getName());
    }
}
