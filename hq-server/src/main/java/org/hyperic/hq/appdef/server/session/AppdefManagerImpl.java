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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefManager;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ResourcesCleanupZevent;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.server.session.AppdefBossImpl;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.events.shared.MaintenanceEventManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TrackerManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.timer.StopWatch;
import org.quartz.SchedulerException;
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

    private OperationDAO operationDAO;
    
    private SessionManager sessionManager;
    
    private ServerManager serverManager;
    
    private MeasurementManager measurementManager;
    
    private PlatformManager platformManager;
    
    private AIQueueManager aiQueueManager;
    
    private TrackerManager trackerManager;
    
    private ConfigManager configManager;
    
    private ServiceManager serviceManager;
    
    private AgentManager agentManager;
    
    private ApplicationManager applicationManager;
    
    private ResourceGroupManager resourceGroupManager;

    private ZeventEnqueuer zEventManager;
    
    protected Log log = LogFactory.getLog(AppdefManagerImpl.class.getName());
    
    private static final String BUNDLE = "org.hyperic.hq.bizapp.Resources";
    
    
    @Autowired
    public AppdefManagerImpl(PlatformDAO platformDAO, PlatformTypeDAO platformTypeDAO, ServerDAO serverDAO,
                             ServerTypeDAO serverTypeDAO, ServiceDAO serviceDAO, ServiceTypeDAO serviceTypeDAO,
                             PermissionManager permissionManager, ResourceManager resourceManager, OperationDAO operationDAO,  SessionManager sessionManager,
                             ServerManager serverManager,   MeasurementManager measurementManager,  PlatformManager platformManager,
                             AIQueueManager aiQueueManager, TrackerManager trackerManager, ConfigManager configManager,
                             ServiceManager serviceManager, AgentManager agentManager, ApplicationManager applicationManager,
                             ResourceGroupManager resourceGroupManager, ZeventEnqueuer zEventManager) {

        this.platformDAO = platformDAO;
        this.platformTypeDAO = platformTypeDAO;
        this.serverDAO = serverDAO;
        this.serverTypeDAO = serverTypeDAO;
        this.serviceDAO = serviceDAO;
        this.serviceTypeDAO = serviceTypeDAO;
        this.permissionManager = permissionManager;
        this.resourceManager = resourceManager;
        this.operationDAO = operationDAO;
        this.sessionManager = sessionManager;
        this.serverManager = serverManager;
        this.platformManager = platformManager;
        this.measurementManager = measurementManager;
        this.aiQueueManager = aiQueueManager;
        this.trackerManager = trackerManager;
        this.configManager = configManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.agentManager = agentManager;
        this.applicationManager = applicationManager;
        this.resourceGroupManager = resourceGroupManager;
        this.zEventManager = zEventManager;
        
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
        List<Integer> typeIds = operationDAO.findOperableResourceIds(subject, "EAM_PLATFORM", "platform_type_id",
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
        List<Integer> ids = operationDAO.findOperableResourceIds(subject, "EAM_PLATFORM", "id", AuthzConstants.platformResType,
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
        List<Integer> typeIds = operationDAO.findOperableResourceIds(subject, "EAM_SERVER", "server_type_id",
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
        List<Integer> ids = operationDAO.findOperableResourceIds(subject, "EAM_SERVER", "id", AuthzConstants.serverResType,
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
        List<Integer> typeIds = operationDAO.findOperableResourceIds(subject, "EAM_SERVICE", "service_type_id",
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
        List<Integer> ids = operationDAO.findOperableResourceIds(subject, "EAM_SERVICE", "id", AuthzConstants.serviceResType,
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



    public AppdefEntityID[] removeAppdefEntity(int sessionId, AppdefEntityID aeid) throws SessionNotFoundException,
            SessionTimeoutException, ApplicationException, VetoException {
        return removeAppdefEntity(sessionId, aeid, true);
    }

    /**
     * TODO possibly find another way to return the correct impl of interface if
     * in HQ or HQ EE. Previously this had to be lazy b/c using SPEL
     * permissionManager.maintenanceEventManager causes the
     * Bootstrap.getBean(MaintenanceEventManager) to be invoked during creation
     * of ejb-context.xml (which doesn't work b/c Bootstrap.context is still set
     * to dao-context.xml)
     * @return
     */
    private MaintenanceEventManager getMaintenanceEventManager() {
        return permissionManager.getMaintenanceEventManager();
    }


    public AppdefEntityID[] removeAppdefEntity(int sessionId, AppdefEntityID aeid, boolean removeAllVirtual)
            throws SessionNotFoundException, SessionTimeoutException, ApplicationException, VetoException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        final Resource res = resourceManager.findResource(aeid);

        if (aeid.isGroup()) {
            // HQ-1577: Do not delete group if downtime schedule exists
            try {
                MaintenanceEvent event = getMaintenanceEventManager().getMaintenanceEvent(subject,
                    aeid.getId());

                if (event != null && event.getStartTime() != 0) {
                    String msg = ResourceBundle.getBundle(BUNDLE).getString(
                        "resource.groups.remove.error.downtime.exists");
                    throw new VetoException(MessageFormat.format(msg,
                        new String[] { res.getName() }));
                }
            } catch (SchedulerException se) {
                // HHQ-3772: This should not happen. However, if it does,
                // log the exception as a warning and do not allow to delete
                // until the scheduler issue is resolved
                log.warn("Scheduler error getting the downtime schedule for group[" + aeid + "]: " +
                         se.getMessage(), se);

                String msg = ResourceBundle.getBundle(BUNDLE).getString(
                    "resource.groups.remove.error.downtime.scheduler.failure");

                throw new VetoException(MessageFormat.format(msg, new String[] { res.getName() }));
            }
        }
        return removeAppdefEntity(res, aeid.getId(), aeid.getType(),subject, removeAllVirtual);
    }



    public AppdefEntityID[] removeAppdefEntity(Resource res, Integer id, int type, AuthzSubject subject,
            boolean removeAllVirtual) throws VetoException, ApplicationException {
        final StopWatch timer = new StopWatch();
        if (res == null) {
            log.warn("AppdefEntityId=" + id + " is not associated with a Resource");
            return new AppdefEntityID[0];
        }
        AppdefEntityID[] removed = resourceManager.removeResourceAndRelatedResources(subject, res, false, removeAllVirtual);
        Map<Integer, List<AppdefEntityID>> agentCache = null;
        
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                Server server = serverManager.findServerById(id);
                agentCache = buildAsyncDeleteAgentCache(server);
                removeServer(subject, server.getId());
                break;
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                Platform platform = platformManager.findPlatformById(id);
                agentCache = buildAsyncDeleteAgentCache(platform);
                removePlatform(subject, platform.getId());
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                removeService(subject, id);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                resourceGroupManager.removeResourceGroup(subject, resourceGroupManager
                    .findResourceGroupById(id));
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                applicationManager.removeApplication(subject, id);
                break;
            default:
                break;
        }

        if (log.isDebugEnabled()) {
            log.debug("removeAppdefEntity() for " + id + " executed in " + timer.getElapsed());
        }

        zEventManager.enqueueEventAfterCommit(new ResourcesCleanupZevent(agentCache));

        return removed;

    }
    
    /**
     * Disable all measurements for a resource
     * @param id the resource's ID
     */
    private void disableMeasurements(AuthzSubject subject, Resource res) throws PermissionException {
        measurementManager.disableMeasurements(subject, res);
    }
    
    public void removeServer(AuthzSubject subj, Integer serverId)
    throws ServerNotFoundException, SessionNotFoundException, SessionTimeoutException, PermissionException,
           SessionException, VetoException {
        Server server = serverManager.findServerById(serverId);
        try {
            disableMeasurements(subj, server.getResource());
            serverManager.removeServer(subj, server);
        } catch (PermissionException e) {
            log.error("Caught permission exception: [server:" + server.getId() + "]");
            log.debug(e,e);
            throw e;
        }
    }
    
    /**
     * 
     */
    public void removePlatform(AuthzSubject subject, Integer platformId)
        throws ApplicationException, VetoException {
        try {
            Platform platform = platformManager.findPlatformById(platformId);
            // Disable all measurements for this platform. We don't actually
            // remove the measurements here to avoid delays in deleting
            // resources.
            disableMeasurements(subject, platform.getResource());

            // Remove from AI queue
            try {
                List<Integer> aiplatformList = new ArrayList<Integer>();
                Integer platformID = platform.getId();
                AIPlatformValue aiPlatform = aiQueueManager.getAIPlatformByPlatformID(subject,
                    platformID);

                if (aiPlatform != null) {
                    aiplatformList.add(aiPlatform.getId());
                    log.info("Removing from AIqueue: " + aiPlatform.getId());
                    aiQueueManager.processQueue(subject, aiplatformList, null, null,
                        AIQueueConstants.Q_DECISION_PURGE);
                }
            } catch (AIQApprovalException e) {
                log.error("Error removing from AI queue", e);
            }

            // now, remove the platform.
            platformManager.removePlatform(subject, platform);
        } catch (PermissionException e) {
            log.error("Caught PermissionException while removing platform: " + platformId, e);
            throw e;
        }
    }
    
    /**
     * Remove config and track plugins for a given resource
     */
    private void removeTrackers(AuthzSubject subject, AppdefEntityID id) throws PermissionException {

        ConfigResponse response;

        try {
            response = configManager.getMergedConfigResponse(subject,
                ProductPlugin.TYPE_MEASUREMENT, id, true);
        } catch (Throwable t) {
            log.debug("Unable to get config response: " + t.getMessage(), t);
            // If anything goes wrong getting the config, just move
            // along. The plugins will be removed on the next agent
            // restart.
            return;
        }

        try {
            trackerManager.disableTrackers(subject, id, response);
        } catch (PluginException e) {
            // Not much we can do.. plugins will be removed on next
            // agent restart.
            log.error("Unable to remove track plugins", e);
        }
    }

    
    public void removeService(AuthzSubject subject, Integer serviceId)
            throws VetoException, PermissionException, ServiceNotFoundException {
            try {
                Service service = serviceManager.findServiceById(serviceId);
                // now remove any measurements associated with the service
                disableMeasurements(subject, service.getResource());
                removeTrackers(subject, service.getEntityId());
                serviceManager.removeService(subject, service);
            } catch (PermissionException e) {

                throw (PermissionException) e;
            }
        }


    /**
     * @param server The server being deleted
     * 
     * @return {@link Map} of {@link Integer} of agentIds to {@link List} of
     *         {@link AppdefEntityID}s
     */
    private Map<Integer, List<AppdefEntityID>> buildAsyncDeleteAgentCache(Server server) {
        Map<Integer, List<AppdefEntityID>> cache = new HashMap<Integer, List<AppdefEntityID>>();

        try {
            Agent agent = findResourceAgent(server.getEntityId());
            List<AppdefEntityID> resources = new ArrayList<AppdefEntityID>();

            for (Service s : server.getServices()) {
                resources.add(s.getEntityId());
            }
            cache.put(agent.getId(), resources);
        } catch (Exception e) {
            log.warn("Unable to build AsyncDeleteAgentCache for server[id=" + server.getId() +
                     ", name=" + server.getName() + "]: " + e.getMessage());
        }

        return cache;
    }

    /**
     * @param platform The platform being deleted
     * 
     * @return {@link Map} of {@link Integer} of agentIds to {@link List} of
     *         {@link AppdefEntityID}s
     */
    private Map<Integer, List<AppdefEntityID>> buildAsyncDeleteAgentCache(Platform platform) {
        Map<Integer, List<AppdefEntityID>> cache = new HashMap<Integer, List<AppdefEntityID>>();

        try {
            Agent agent = platform.getAgent();
            List<AppdefEntityID> resources = new ArrayList<AppdefEntityID>();

            for (Server s : platform.getServers()) {
                if (!s.getServerType().isVirtual()) {
                    resources.add(s.getEntityId());
                }
                List<AppdefEntityID> services = buildAsyncDeleteAgentCache(s).get(agent.getId());
                resources.addAll(services);
            }
            cache.put(agent.getId(), resources);
        } catch (Exception e) {
            log.warn("Unable to build AsyncDeleteAgentCache for platform[id=" + platform.getId() +
                     ", name=" + platform.getName() + "]: " + e.getMessage());
        }

        return cache;
    }
    
    /**
     * 
     */
    @Transactional(readOnly = true)
    public Agent findResourceAgent(AppdefEntityID entityId) throws AppdefEntityNotFoundException,
        SessionTimeoutException, SessionNotFoundException, PermissionException,
        AgentNotFoundException {
        return agentManager.getAgent(entityId);
    }


    
}
