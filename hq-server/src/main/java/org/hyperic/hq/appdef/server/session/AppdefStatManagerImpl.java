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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefStatManager;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.hq.common.SystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AppdefStatManagerImpl provides summary and aggregate statistical information
 * for appdef related entities.
 * <p>
 * 
 * </p>
 * 
 */
@org.springframework.stereotype.Service
public class AppdefStatManagerImpl implements AppdefStatManager {

    private static final String LOG_CTX = AppdefStatManagerImpl.class.getName();

    private final Log log = LogFactory.getLog(LOG_CTX);

    private static final int APPDEF_TYPE_PLATFORM = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
    private static final int APPDEF_TYPE_SERVER = AppdefEntityConstants.APPDEF_TYPE_SERVER;
    private static final int APPDEF_TYPE_SERVICE = AppdefEntityConstants.APPDEF_TYPE_SERVICE;

    private ApplicationManager applicationManager;

    private PlatformManager platformManager;

    private ServerManager serverManager;

    private ServiceManager serviceManager;

    private ResourceGroupManager resourceGroupManager;

    private AppdefStatDAO appdefStatDAO;

    @Autowired
    public AppdefStatManagerImpl(ApplicationManager applicationManager,
                                 PlatformManager platformManager, ServerManager serverManager,
                                 ServiceManager serviceManager,
                                 ResourceGroupManager resourceGroupManager,
                                 AppdefStatDAO appdefStatDAO) {
        this.applicationManager = applicationManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.resourceGroupManager = resourceGroupManager;
        this.appdefStatDAO = appdefStatDAO;
    }

    /**
     * <p>
     * Return directly connected resource tree for node level platform
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForPlatform(AuthzSubject subject, Integer platformId)
        throws PlatformNotFoundException, PermissionException {
        try {
            Platform plat = platformManager.findPlatformById(platformId);
            ResourceTreeNode[] retVal = appdefStatDAO.getNavMapDataForPlatform(subject, plat);
            if (log.isDebugEnabled()) {
                log.debug(mapToString(retVal));
            }
            return retVal;
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return directly connected resource tree for node level server
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForServer(AuthzSubject subject, Integer serverId)
        throws ServerNotFoundException, PermissionException {
        Server server = serverManager.findServerById(serverId);

        try {
            return appdefStatDAO.getNavMapDataForServer(subject, server);
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return directly connected resource tree for node level service
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForService(AuthzSubject subject, Integer serviceId)
        throws ServiceNotFoundException, PermissionException {
        Service service = serviceManager.findServiceById(serviceId);
        try {
            return appdefStatDAO.getNavMapDataForService(subject, service);
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private String mapToString(ResourceTreeNode[] node) {
        StringBuffer sb = new StringBuffer();
        if (node == null) {
            sb.append("MAP IS NULL!\n");
            return sb.toString();
        }
        int height = node.length;
        for (int x = 0; x < height; x++) {
            if (node[x] == null) {
                sb.append("MAP[" + x + "] IS NULL!\n");
            } else {
                sb.append("MAP[" + x + "] NOT NULL \n");
            }
        }
        return sb.toString();
    }

    /**
     * <p>
     * Return directly connected resource tree for node level service
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForApplication(AuthzSubject subject, Integer appId)
        throws ApplicationNotFoundException, PermissionException {
        Application app = applicationManager.findApplicationById(subject, appId);
        try {
            return appdefStatDAO.getNavMapDataForApplication(subject, app);
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return resources for autogroups
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForAutoGroup(AuthzSubject subject,
                                                        AppdefEntityID[] parents, Integer resType)
        throws AppdefEntityNotFoundException, PermissionException {
        try {
            final int appdefTypeUndefined = -1;
            // platform auto-groups do not have parent resource types
            int entType = (parents != null) ? getChildEntityType(parents[0].getType())
                                           : APPDEF_TYPE_PLATFORM;

            AppdefResourceType type = getResourceTypeValue(entType, resType);
            // derive parent and child entity types
            final int pEntityType = (parents != null) ? parents[0].getType() : appdefTypeUndefined;
            final int cEntityType = (pEntityType != appdefTypeUndefined) ? getChildEntityType(pEntityType)
                                                                        : APPDEF_TYPE_PLATFORM;
            return appdefStatDAO.getNavMapDataForAutoGroup(subject, parents, type, pEntityType,
                cEntityType);
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private AppdefResourceType getResourceTypeValue(int entityType, Integer resType)
        throws AppdefEntityNotFoundException {
        switch (entityType) {
            case APPDEF_TYPE_PLATFORM:
                return platformManager.findPlatformType(resType);
            case APPDEF_TYPE_SERVER:
                return serverManager.findServerType(resType);
            case APPDEF_TYPE_SERVICE:
                return serviceManager.findServiceType(resType);
            default:
                return null;
        }
    }

    /**
     * <p>
     * Return resources for groups (not autogroups)
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public ResourceTreeNode[] getNavMapDataForGroup(AuthzSubject subject, Integer groupId)
        throws PermissionException {

        ResourceGroup group = resourceGroupManager.findResourceGroupById(subject, groupId);
        AppdefGroupValue groupVal = resourceGroupManager.getGroupConvert(subject, group);
        try {
            return appdefStatDAO.getNavMapDataForGroup(subject, groupVal);
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }

    private int getChildEntityType(int type) {
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return APPDEF_TYPE_SERVER;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return APPDEF_TYPE_SERVICE;
            default:
                return type;
        }
    }

}
