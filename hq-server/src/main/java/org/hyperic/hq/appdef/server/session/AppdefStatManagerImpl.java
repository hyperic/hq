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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.util.timer.StopWatch;
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
    
    private ResourceManager resourceManager;

    //TODO all the AppdefStatDAO queries are going to fail
    private AppdefStatDAO appdefStatDAO;

    @Autowired
    public AppdefStatManagerImpl(ApplicationManager applicationManager,
                                 PlatformManager platformManager, ServerManager serverManager,
                                 ServiceManager serviceManager,
                                 ResourceGroupManager resourceGroupManager,
                                 AppdefStatDAO appdefStatDAO, ResourceManager resourceManager) {
        this.applicationManager = applicationManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.resourceGroupManager = resourceGroupManager;
        this.resourceManager = resourceManager;
        this.appdefStatDAO = appdefStatDAO;
    }

    /**
     * <p>
     * Return map of platform counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Integer> getPlatformCountsByTypeMap(AuthzSubject subject) {
        try {
            return platformManager.getPlatformTypeCounts();
        } catch (Exception e) {
            log.error("Caught Exception finding Platforms by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return platforms count.
     * </p>
     * 
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public int getPlatformsCount(AuthzSubject subject) {
        try {
            return platformManager.getPlatformCount().intValue();
        } catch (Exception e) {
            log.error("Caught Exception counting Platforms: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return map of server counts.
     * </p>
     * 
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Integer> getServerCountsByTypeMap(AuthzSubject subject) {
        try {
            return serverManager.getServerTypeCounts();
        } catch (Exception e) {
            log.error("Caught Exception finding Servers by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return servers count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public int getServersCount(AuthzSubject subject) {
        try {
            return serverManager.getServerCount().intValue();
        } catch (Exception e) {
            log.error("Caught Exception finding Servers by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return map of service counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Integer> getServiceCountsByTypeMap(AuthzSubject subject) {
        try {
            return serviceManager.getServiceTypeCounts();
        } catch (Exception e) {
            log.error("Caught Exception finding Services by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return services count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public int getServicesCount(AuthzSubject subject) {
        try {
            return serviceManager.getServiceCount().intValue();
        } catch (Exception e) {
            log.error("Caught Exception finding Services by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return map of app counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Integer> getApplicationCountsByTypeMap(AuthzSubject subject) {
        try {
            return appdefStatDAO.getApplicationCountsByTypeMap(subject);
        } catch (Exception e) {
            log.error("Caught Exception finding applications by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return apps count.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public int getApplicationsCount(AuthzSubject subject) {
        try {
            return applicationManager.getApplicationCount().intValue();
        } catch (Exception e) {
            log.error("Caught Exception finding applications by type: " + e, e);
            throw new SystemException(e);
        }
    }

    /**
     * <p>
     * Return map of grp counts.
     * </p>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<Integer, Integer> getGroupCountsMap(AuthzSubject subject) {
        try {
            Map<Integer,Integer> groupCounts = new HashMap<Integer,Integer>();
            int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();
            for (int x = 0; x < groupTypes.length; x++) {
                ResourceType groupType =  resourceManager.findResourceTypeByName(AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[x]));
                groupCounts.put(groupTypes[x], resourceGroupManager.getGroupCountOfType(groupType).intValue());
            }
            return groupCounts;
        } catch (Exception e) {
            log.error("Caught Exception finding groups by type: " + e, e);
            throw new SystemException(e);
        }
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
            final ResourceTreeNode aPlatformNode = new ResourceTreeNode(plat.getName(),
                getAppdefTypeLabel(APPDEF_TYPE_PLATFORM, plat.getAppdefResourceType().getName()), plat
                    .getEntityId(), ResourceTreeNode.RESOURCE);
            
            final Set<ResourceTreeNode> servers = new HashSet<ResourceTreeNode>();
            for(Server server: plat.getServers()) {
                servers.add(new ResourceTreeNode(server.getName(), getAppdefTypeLabel(
                    APPDEF_TYPE_SERVER, server.getServerType().getName()), AppdefEntityID
                    .newServerID(new Integer(server.getId())), plat.getEntityId(),
                    server.getServerType().getId()));
            }
            
            aPlatformNode.setSelected(true);
            ResourceTreeNode[] svrNodes = (ResourceTreeNode[]) servers
                .toArray(new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(svrNodes, true);
            aPlatformNode.addUpChildren(svrNodes);
            ResourceTreeNode[] retVal = new ResourceTreeNode[] { aPlatformNode };
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
            StopWatch timer = new StopWatch();
            final Map<Integer, ResourceTreeNode> serviceMap = new HashMap<Integer, ResourceTreeNode>();
            Platform platform = server.getPlatform();
            ResourceTreeNode aPlatformNode = new ResourceTreeNode(platform.getName(),
                getAppdefTypeLabel(APPDEF_TYPE_PLATFORM, platform.getPlatformType().getName()),
                AppdefEntityID.newPlatformID(new Integer(platform.getId())),
                (AppdefEntityID) null, platform.getPlatformType().getId());
            
            final ResourceTreeNode aServerNode = new ResourceTreeNode(server.getName(),
                getAppdefTypeLabel(server.getEntityId().getType(), server.getAppdefResourceType()
                    .getName()), server.getEntityId(), ResourceTreeNode.RESOURCE);
            
            for(Service service: server.getServices()) {
                serviceMap.put(new Integer(service.getId()), new ResourceTreeNode(
                    service.getName(), getAppdefTypeLabel(APPDEF_TYPE_SERVICE,
                        service.getServiceType().getName()), AppdefEntityID.newServiceID(new Integer(
                        service.getId())), server.getEntityId(), service.getServiceType().getId()));
            }
            
            aServerNode.setSelected(true);
            ResourceTreeNode[] services = (ResourceTreeNode[]) serviceMap.values().toArray(
                new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(services, true);
            aServerNode.addUpChildren(services);
            // TODO aPlatformNode can be null if user is unauthz
            //if (aPlatformNode != null) {
                aServerNode.addDownChild(aPlatformNode);
            //}
            ResourceTreeNode[] serverNode = new ResourceTreeNode[] { aServerNode };

            if (log.isDebugEnabled()) {
                log.debug("getNavMapDataForServer() executed in: " + timer);
            }

            return serverNode;
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
            StopWatch timer = new StopWatch();
            
            ResourceTreeNode aServiceNode = new ResourceTreeNode(service.getName(),
                getAppdefTypeLabel(service.getEntityId().getType(), service
                    .getAppdefResourceType().getName()), service.getEntityId(),
                ResourceTreeNode.RESOURCE);
            
            Map<Integer, ResourceTreeNode> appMap = new HashMap<Integer, ResourceTreeNode>();
            
            AppdefResource parent = service.getParent();
            ResourceTreeNode aPlatformNode;
            ResourceTreeNode aServerNode=null;
            if(parent instanceof Platform) {
                Platform parentPlatform = (Platform)parent;
                aPlatformNode = new ResourceTreeNode(parentPlatform.getName(),
                    getAppdefTypeLabel(APPDEF_TYPE_PLATFORM, parentPlatform.getPlatformType().getName()),
                    AppdefEntityID.newPlatformID(new Integer(parentPlatform.getId())),
                    ResourceTreeNode.RESOURCE);
            }else {
                Server parentServer = (Server)parent;
                aServerNode = new ResourceTreeNode(parentServer.getName(), getAppdefTypeLabel(
                    APPDEF_TYPE_SERVER, parentServer.getServerType().getName()), AppdefEntityID
                    .newServerID(new Integer(parentServer.getId())), ResourceTreeNode.RESOURCE);
                aPlatformNode = new ResourceTreeNode(parentServer.getPlatform().getName(),
                    getAppdefTypeLabel(APPDEF_TYPE_PLATFORM, parentServer.getPlatform().getPlatformType().getName()),
                    AppdefEntityID.newPlatformID(new Integer(parentServer.getPlatform().getId())),
                    ResourceTreeNode.RESOURCE);
            }
            
            // TODO server nodes and platform nodes can be null if user is
            // unauthz
            if (aServerNode != null) {
                //if (aPlatformNode != null) {
                    aServerNode.addDownChild(aPlatformNode);
                //}
                aServiceNode.addDownChild(aServerNode);
            } else if (aPlatformNode != null) {
                aServiceNode.addDownChild(aPlatformNode);
            }
            
            aServiceNode.setSelected(true);
            
            Collection<Application> applications = applicationManager.getApplicationsByResource(subject, service.getEntityId());
            for(Application application: applications) {
                appMap.put(new Integer(application.getId()), new ResourceTreeNode(
                    application.getName(), getAppdefTypeLabel(
                    AppdefEntityConstants.APPDEF_TYPE_APPLICATION,
                    application.getDescription()), AppdefEntityID.newAppID(new Integer(
                    application.getId())), ResourceTreeNode.RESOURCE));
            }
            ResourceTreeNode[] appNodes = (ResourceTreeNode[]) appMap.values().toArray(
                new ResourceTreeNode[0]);
            ResourceTreeNode.alphaSortNodes(appNodes, true);
            aServiceNode.addUpChildren(appNodes);
            
            ResourceTreeNode[] serviceNode = new ResourceTreeNode[] { aServiceNode };

            if (log.isDebugEnabled()) {
                log.debug("getNavMapDataForService() executed in: " + timer);
            }
            return serviceNode;
        } catch (Exception e) {
            log.error("Unable to get NavMap data: " + e, e);
            throw new SystemException(e);
        }
    }
    
    private String getAppdefTypeLabel(int typeId, String desc) {
        String typeLabel = AppdefEntityConstants.typeToString(typeId);
        if (desc == null) {
            desc = typeLabel;
        } else if (desc.toLowerCase().indexOf(typeLabel.toLowerCase()) == -1) {
            desc += " " + typeLabel;
        }
        return desc;
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
            return appdefStatDAO.getNavMapDataForGroup(subject, group, groupVal);
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
