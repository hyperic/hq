/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.ApplicationType;
import org.hyperic.hq.appdef.server.session.CpropKey;
import org.hyperic.hq.appdef.server.session.DownResSortField;
import org.hyperic.hq.appdef.server.session.DownResource;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.server.session.ResourceContentChangedZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateFQDNException;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefInventorySummary;
import org.hyperic.hq.appdef.shared.AppdefManager;
import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.AppdefStatManager;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.DependencyTree;
import org.hyperic.hq.appdef.shared.GroupTypeValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ResourcesCleanupZevent;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.pager.AppdefPagerFilter;
import org.hyperic.hq.appdef.shared.pager.AppdefPagerFilterAssignSvc;
import org.hyperic.hq.appdef.shared.pager.AppdefPagerFilterExclude;
import org.hyperic.hq.appdef.shared.pager.AppdefPagerFilterGroupEntityResource;
import org.hyperic.hq.appdef.shared.pager.AppdefPagerFilterGroupMemExclude;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerImpl;
import org.hyperic.hq.authz.server.session.ResourceGroupSortField;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.GroupCreationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.hq.bizapp.shared.uibeans.SearchResult;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.measurement.ext.DownMetricValue;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.hq.util.Reference;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.IntegerTransformer;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@org.springframework.stereotype.Service
@Transactional
public class AppdefBossImpl implements AppdefBoss , ApplicationContextAware {

    private static final String APPDEF_PAGER_PROCESSOR = "org.hyperic.hq.appdef.shared.pager.AppdefPagerProc";

    private SessionManager sessionManager;

    private AgentManager agentManager;

    private AppdefStatManager appdefStatManager;

    private ApplicationManager applicationManager;

    private AuthzSubjectManager authzSubjectManager;

    private AutoinventoryManager autoinventoryManager;

    private AvailabilityManager availabilityManager;

    private ConfigManager configManager;

    private CPropManager cPropManager;

    private PermissionManager permissionManager;

    
    private PlatformManager platformManager;

    private AIBoss aiBoss;

    private ResourceGroupManager resourceGroupManager;

    private ResourceManager resourceManager;

    private ServerManager serverManager;

    private ServiceManager serviceManager;

    
    private AppdefManager appdefManager;

    private ZeventEnqueuer zEventManager;
    
    private ApplicationContext applicationContext;

    protected Log log = LogFactory.getLog(AppdefBossImpl.class.getName());
    protected final int APPDEF_TYPE_UNDEFINED = -1;
    protected final int APPDEF_RES_TYPE_UNDEFINED = -1;
    protected final int APPDEF_GROUP_TYPE_UNDEFINED = -1;

    @Autowired
    public AppdefBossImpl(SessionManager sessionManager, AgentManager agentManager,
                          AppdefStatManager appdefStatManager,
                          ApplicationManager applicationManager,
                          AuthzSubjectManager authzSubjectManager,
                          AutoinventoryManager autoinventoryManager,
                          AvailabilityManager availabilityManager, ConfigManager configManager,
                          CPropManager cPropManager, PermissionManager permissionManager,
                          PlatformManager platformManager,
                          AIBoss aiBoss, ResourceGroupManager resourceGroupManager,
                          ResourceManager resourceManager, ServerManager serverManager,
                          ServiceManager serviceManager, 
                          AppdefManager appdefManager, ZeventEnqueuer zEventManager) {
        this.sessionManager = sessionManager;
        this.agentManager = agentManager;
        this.appdefStatManager = appdefStatManager;
        this.applicationManager = applicationManager;
        this.authzSubjectManager = authzSubjectManager;
        this.autoinventoryManager = autoinventoryManager;
        this.availabilityManager = availabilityManager;
        this.configManager = configManager;
        this.cPropManager = cPropManager;
        this.permissionManager = permissionManager;        
        this.platformManager = platformManager;
        this.aiBoss = aiBoss;
        this.resourceGroupManager = resourceGroupManager;
        this.resourceManager = resourceManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.appdefManager = appdefManager;
        this.zEventManager = zEventManager;
    }

 

    /**
     * Find a common appdef resource type among the appdef entities
     * @param sessionID
     * @param aeids the array of appdef entity IDs
     * @return AppdefResourceTypeValue if they are of same type, null otherwise
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     * @throws SessionNotFoundException
     * @throws SessionTimeoutException
     * 
     */
    @Transactional(readOnly = true)
    public AppdefResourceType findCommonResourceType(int sessionID, String[] aeids)
        throws AppdefEntityNotFoundException, PermissionException, SessionNotFoundException,
        SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        if (aeids == null || aeids.length == 0) {
            return null;
        }

        // Take the resource type of the first entity
        AppdefEntityID aeid = new AppdefEntityID(aeids[0]);
        int resType = aeid.getType();

        AppdefResourceType retArt = null;
        // Now let's go through and make sure they're of the same type
        for (int i = 0; i < aeids.length; i++) {
            aeid = new AppdefEntityID(aeids[i]);
            // First check to make sure they are same resource type
            if (aeid.getType() != resType) {
                return null;
            }

            // Now get the appdef resource type value
            AppdefEntityValue arv = new AppdefEntityValue(aeid, subject);
            try {
                AppdefResourceType art = arv.getAppdefResourceType();

                if (retArt == null) {
                    retArt = art;
                } else if (!art.equals(retArt)) {
                    return null;
                }
            } catch (IllegalStateException e) {
                // Mixed group
                return null;
            }
        }

        return retArt;
    }

    /**
     * Find all the platform types defined in the system.
     * 
     * @return A list of PlatformTypeValue objects.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<PlatformTypeValue> findAllPlatformTypes(int sessionID, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException {

        AuthzSubject subject = sessionManager.getSubject(sessionID);
        PageList<PlatformTypeValue> platTypeList = platformManager.getAllPlatformTypes(subject, pc);

        return platTypeList;
    }

    /**
     * Find all the viewable platform types defined in the system.
     * 
     * @return A list of PlatformTypeValue objects.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<PlatformTypeValue> findViewablePlatformTypes(int sessionID, PageControl pc)
    throws SessionTimeoutException, SessionNotFoundException, PermissionException, NotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        PageList<PlatformTypeValue> platTypeList = platformManager.getViewablePlatformTypes( subject, pc);
        return platTypeList;
    }

    /**
     * Find all the server types defined in the system.
     * 
     * @return A list of ServerTypeValue objects.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> findAllServerTypes(int sessionID, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException {

        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serverManager.getAllServerTypes(subject, pc);
    }

    /**
     * Find all viewable server types defined in the system.
     * 
     * @return A list of ServerTypeValue objects.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> findViewableServerTypes(int sessionID, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        NotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serverManager.getViewableServerTypes(subject, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<AppdefResourceTypeValue> findAllApplicationTypes(int sessionID)
        throws ApplicationException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);

        return applicationManager.getAllApplicationTypes(subject);

    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ApplicationType findApplicationTypeById(int sessionId, Integer id)
        throws ApplicationException {
        sessionManager.authenticate(sessionId);
        return applicationManager.findApplicationType(id);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> findAllServiceTypes(int sessionID, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serviceManager.getAllServiceTypes(subject, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> findViewableServiceTypes(int sessionID, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException,
        NotFoundException {

        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serviceManager.getViewableServiceTypes(subject, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> findViewablePlatformServiceTypes(int sessionID, Integer platId)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serviceManager.findVirtualServiceTypesByPlatform(subject, platId);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ApplicationValue findApplicationById(int sessionID, Integer id)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException,
        SessionNotFoundException {

        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return applicationManager.findApplicationById(subject, id).getApplicationValue();
    }

    /**
     * <p>
     * Get first-level child resources of a given resource based on the child
     * resource type.
     * </p>
     * 
     * <p>
     * For example:
     * <ul>
     * <li><b>platform -</b> list of servers</li>
     * <li><b>server -</b> list of services</li>
     * <li><b>service -</b> <i>not supported</i></li>
     * <li><b>application -</b> list of services</li>
     * <li><b>group -</b> <i>list of members if the group is compatible</i></li>
     * </ul>
     * </p>
     * 
     * @param parent the resource whose children we want
     * @param childResourceType the type of child resource
     * 
     * @return list of <code>{@link
     * org.hyperic.hq.appdef.shared.AppdefResourceValue}</code>
     *         objects
     * 
     * 
     */
    @Transactional(readOnly = true)
    public PageList<? extends AppdefResourceValue> findChildResources(
                                                                      int sessionID,
                                                                      AppdefEntityID parent,
                                                                      AppdefEntityTypeID childResourceType,
                                                                      PageControl pc)
        throws SessionException, PermissionException, AppdefEntityNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        AppdefEntityValue adev = new AppdefEntityValue(parent, subject);

        switch (childResourceType.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return adev.getAssociatedServers(childResourceType.getId(), pc);
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return adev.getAssociatedServices(childResourceType.getId(), pc);
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                AppdefGroupValue grp = findGroup(sessionID, parent.getId());
                if (grp.getGroupEntResType() != childResourceType.getId().intValue()) {
                    throw new IllegalArgumentException("childResourceType " + childResourceType +
                                                       " does not match group resource type" +
                                                       grp.getGroupEntResType());
                }
                AppdefEntityID[] ids = new AppdefEntityID[grp.getSize()];
                int idx = 0;
                for (AppdefEntityID id : grp.getAppdefGroupEntries()) {
                    ids[idx++] = id;
                }
                return findByIds(sessionID, ids, pc);
            default:
                throw new IllegalArgumentException("Unsupported appdef type " + parent.getType());
        }
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ApplicationValue> findApplications(int sessionID, AppdefEntityID id,
                                                       PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException,
        SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return applicationManager.getApplicationsByResource(subject, id, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> findPlatformServices(int sessionID, Integer platformId,
                                                       PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException,
        SessionNotFoundException {
        // Get the AuthzSubject for the user's session
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serviceManager.getPlatformServices(subject, platformId, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServiceValue> findPlatformServices(int sessionID, Integer platformId,
                                                       Integer typeId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException,
        SessionNotFoundException {
        // Get the AuthzSubject for the user's session
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serviceManager.getPlatformServices(subject, platformId, typeId, pc);
    }

    /**
     * Find service inventory by application - including services and clusters
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefResourceValue> findServiceInventoryByApplication(int sessionID,
                                                                           Integer appId,
                                                                           PageControl pc)
        throws AppdefEntityNotFoundException, SessionException, PermissionException {
        AppdefEntityID aeid = AppdefEntityID.newAppID(appId);

        return findServices(sessionID, aeid, true, pc);
    }

    /**
     * Find all services on a server
     * 
     * @return A list of ServiceValue objects.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefResourceValue> findServicesByServer(int sessionID, Integer serverId,
                                                              PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionException {
        AppdefEntityID aeid = AppdefEntityID.newServerID(serverId);
        return findServices(sessionID, aeid, false, pc);
    }

    private PageList<AppdefResourceValue> findServices(int sessionID, AppdefEntityID aeid,
                                                       boolean allServiceInventory, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionException {
        PageList<AppdefResourceValue> res = null;

        if (pc == null) {
            pc = PageControl.PAGE_ALL;
        }

        // Get the AuthzSubject for the user's session
        AuthzSubject subject = sessionManager.getSubject(sessionID);

        AppdefEntityValue aeval = new AppdefEntityValue(aeid, subject);
        switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                // TODO G
                res = aeval.getAssociatedServices(pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                // fetch all service inventory including clusters.
                if (allServiceInventory) {
                    res = serviceManager
                        .getServiceInventoryByApplication(subject, aeid.getId(), pc);
                    // app services will include service clusters which need
                    // to be converted to their service group counterpart.
                    for (int i = 0; i < res.size(); i++) {
                        Object o = res.get(i);
                        if (o instanceof ServiceClusterValue) {
                            res
                                .set(i,
                                    findGroup(sessionID, ((ServiceClusterValue) o).getGroupId()));
                        }
                    }
                } else {
                    res = serviceManager.getServicesByApplication(subject, aeid.getId(), pc);
                }
                break;
            default:
                log.error("Invalid type given to find services.");
        }
        return res;
    }

    /**
     * Find the platform by service.
     * 
     */
    @Transactional(readOnly = true)
    public PlatformValue findPlatformByDependentID(int sessionID, AppdefEntityID entityId)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException,
        PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        Integer id = entityId.getId();
        switch (entityId.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return findPlatformById(sessionID, id);
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return platformManager.getPlatformByServer(subject, id);
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return platformManager.getPlatformByService(subject, id);
            default:
                throw new IllegalArgumentException("Invalid entity type: " + entityId.getType());
        }
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ServerValue findServerByService(int sessionID, Integer serviceID)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException,
        PermissionException {
        return (ServerValue) findServers(sessionID, AppdefEntityID.newServiceID(serviceID), null)
            .get(0);
    }

    /**
     * 
     */
    public PageList<ServerValue> findServersByTypeAndPlatform(int sessionId, Integer platformId,
                                                              int adResTypeId, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException,
        SessionNotFoundException {
        return findServers(sessionId, AppdefEntityID.newPlatformID(platformId), adResTypeId, pc);
    }

    /**
     * Get the virtual server for a given platform and service type
     * 
     * 
     */
    public ServerValue findVirtualServerByPlatformServiceType(int sessionID, Integer platId,
                                                              Integer svcTypeId)
        throws ServerNotFoundException, PlatformNotFoundException, PermissionException,
        SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        List<ServerValue> servers = serverManager.getServersByPlatformServiceType(subject, platId,
            svcTypeId);

        // There should only be one
        return servers.get(0);
    }

    /**
     * Find all servers on a given platform
     * 
     * @return A list of ServerValue objects
     * 
     */
    public PageList<ServerValue> findServersByPlatform(int sessionID, Integer platformId,
                                                       PageControl pc)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException,
        PermissionException {
        return findServers(sessionID, AppdefEntityID.newPlatformID(platformId), pc);
    }

    /**
     * Get the virtual servers for a given platform
     * 
     * 
     */
    public PageList<ServerValue> findViewableServersByPlatform(int sessionID, Integer platformId,
                                                               PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException,
        SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serverManager.getServersByPlatform(subject, platformId, true, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> findServerTypesByPlatform(int sessionID, Integer platformId,
                                                               PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException,
        SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serverManager.getServerTypesByPlatform(subject, platformId, true, pc);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServerTypeValue> findServerTypesByPlatformType(int sessionID,
                                                                   Integer platformId,
                                                                   PageControl pc)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serverManager.getServerTypesByPlatformType(subject, platformId, pc);
    }

    private PageList<ServerValue> findServers(int sessionID, AppdefEntityID aeid, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException,
        SessionNotFoundException {
        return findServers(sessionID, aeid, APPDEF_RES_TYPE_UNDEFINED, pc);
    }

    private PageList<ServerValue> findServers(int sessionID, AppdefEntityID aeid, int servTypeId,
                                              PageControl pc) throws AppdefEntityNotFoundException,
        PermissionException, SessionTimeoutException, SessionNotFoundException {

        PageList<ServerValue> res;

        // Get the AuthzSubject for the user's session
        AuthzSubject subject = sessionManager.getSubject(sessionID);

        switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                if (servTypeId == APPDEF_RES_TYPE_UNDEFINED) {
                    res = serverManager.getServersByPlatform(subject, aeid.getId(), false, pc);
                } else {
                    // exclude virtual servers
                    res = serverManager.getServersByPlatform(subject, aeid.getId(), new Integer(
                        servTypeId), true, pc);
                }
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                res = serverManager.getServersByApplication(subject, aeid.getId(), pc);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                ServerValue val;
                val = serverManager.getServerByService(subject, aeid.getId());
                res = new PageList<ServerValue>();
                res.add(val);
                break;
            default:
                log.error("Invalid type given to find server.");
                res = null;
        }
        return res;
    }

    /**
     * Get all platforms in the inventory.
     * 
     * 
     * @param sessionID The current session token.
     * @param pc a PageControl object which determines the size of the page and
     *        the sorting, if any.
     * @return A List of PlatformValue objects representing all of the platforms
     *         that the given subject is allowed to view.
     */
    @Transactional(readOnly = true)
    public PageList<PlatformValue> findAllPlatforms(int sessionID, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException,
        NotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return platformManager.getAllPlatforms(subject, pc);
    }

    /**
     * Get recently created platforms in the inventory.
     * 
     * 
     * @param sessionID The current session token.
     * @return A List of PlatformValue objects representing all of the platforms
     *         that the given subject is allowed to view that was created in the
     *         past time range specified.
     */
    @Transactional(readOnly = true)
    public PageList<PlatformValue> findRecentPlatforms(int sessionID, long range, int size)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException,
        NotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return platformManager.getRecentPlatforms(subject, range, size);
    }

    /**
     * Looks up and returns a list of value objects corresponding to the list of
     * appdef entity represented by the instance ids passed in. The method does
     * not require the caller to know the instance-id's corresponding type.
     * Similarly, the return value is upcasted.
     * @return list of appdefResourceValue
     * 
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public PageList<AppdefResourceValue> findByIds(int sessionId, AppdefEntityID[] entities, PageControl pc)
    throws PermissionException, SessionTimeoutException, SessionNotFoundException {
        final AuthzSubject subject = sessionManager.getSubject(sessionId);
        final Collection<ResourceType> types = new HashSet<ResourceType>();
        final Map<Integer, Resource> resources = new HashMap<Integer, Resource>(entities.length);
        for (final AppdefEntityID aeid : entities) {
            if (aeid == null) {
                continue;
            }
            final Resource res = resourceManager.findResource(aeid);
            if (res == null || res.isInAsyncDeleteState()) {
                continue;
            }
            resources.put(res.getId(), res);
            types.add(res.getResourceType());
        }
        final Collection<AppdefResourceValue> rtn =
            permissionManager.findViewableResources(subject, types, new IntegerTransformer<AppdefResourceValue>() {
                public AppdefResourceValue transform(Integer id) {
                    final Resource r = resources.get(id);
                    if (null != r) {
                        return AppdefResourceValue.convertToAppdefResourceValue(r);
                    }
                    return null;
                }
            });
        if (pc != null) {
        	return Pager.getDefaultPager().seek(rtn, pc);
        } else {
            return new PageList<AppdefResourceValue>(rtn, rtn.size());
        }
    }

    /**
     * Looks up and returns a value object corresponding to the appdef entity
     * represented by the instance id passed in. The method does not require the
     * caller to know the instance-id's corresponding type. Similarly, the
     * return value is upcasted.
     * 
     */
    @Transactional(readOnly = true)
    public AppdefResourceValue findById(int sessionId, AppdefEntityID entityId)
    throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return findById(subject, entityId);
    }

    /**
     * TODO: this needs to be a batch query operation at the DAO layer TODO:
     * requires object model change at the db level to do it properly TODO:
     * AppdefResourceType includes all but the APPDEF_TYPE_GROUP.
     * 
     * Looks up and returns a value object corresponding to the appdef entity
     * represented by the instance id passed in. The method does not require the
     * caller to know the instance-id's corresponding type. Similarly, the
     * return value is upcasted.
     */
    @Transactional(readOnly = true)
    public AppdefResourceValue findById(AuthzSubject subject, AppdefEntityID entityId)
    throws AppdefEntityNotFoundException, PermissionException, SessionTimeoutException, SessionNotFoundException {
        AppdefEntityValue aeval = new AppdefEntityValue(entityId, subject);
        AppdefResourceValue retVal = aeval.getResourceValue();
        if (retVal == null) {
            throw new IllegalArgumentException(entityId.getType() + " is not a valid appdef entity type");
        }
        if (entityId.isServer()) {
            ServerValue server = (ServerValue) retVal;
            Platform platform = server.getPlatform();
            if (platform == null) {
                return null;
            }
            retVal.setHostName(platform.getName());
        } else if (entityId.isService()) {
            ServiceValue service = (ServiceValue) retVal;
            ServerLightValue server = service.getServer();
            if (server == null) {
                return null;
            }
            retVal.setHostName(server.getName());
        }
        return retVal;
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PlatformValue findPlatformById(int sessionID, Integer id)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException,
        PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return (PlatformValue) findById(subject, AppdefEntityID.newPlatformID(id));
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Agent findResourceAgent(AppdefEntityID entityId) throws AppdefEntityNotFoundException,
        SessionTimeoutException, SessionNotFoundException, PermissionException,
        AgentNotFoundException {
        return appdefManager.findResourceAgent(entityId);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ServerValue findServerById(int sessionID, Integer id)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException,
        PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return (ServerValue) findById(subject, AppdefEntityID.newServerID(id));
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ServiceValue findServiceById(int sessionID, Integer id)
        throws AppdefEntityNotFoundException, SessionTimeoutException, SessionNotFoundException,
        PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return (ServiceValue) findById(subject, AppdefEntityID.newServiceID(id));
    }

    /**
     * @return A PageList of all registered appdef resource types as well as the
     *         three group specific resource types.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefResourceTypeValue> findAllResourceTypes(int sessionId, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException {
        return findAllResourceTypes(sessionId, APPDEF_TYPE_UNDEFINED, pc);
    }

    /**
     * @return A PageList of all registered appdef resource types of a
     *         particular entity type.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefResourceTypeValue> findAllResourceTypes(int sessionId, int entType,
                                                                  PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        List<AppdefResourceTypeValue> toBePaged = new ArrayList<AppdefResourceTypeValue>(); // at
        // very
        // least,
        // return
        // empty
        // list.
        Pager defaultPager = Pager.getDefaultPager();

        try {
            boolean allFlag = false;
            PageControl lpc = PageControl.PAGE_ALL;

            PageControl.initDefaults(lpc, SortAttribute.RESTYPE_NAME);

            if (entType == APPDEF_TYPE_UNDEFINED) {
                allFlag = true;
            }

            if (allFlag || entType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
                toBePaged.addAll(platformManager.getViewablePlatformTypes(subject, lpc));
            }

            if (allFlag || entType == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
                toBePaged.addAll(serverManager.getViewableServerTypes(subject, lpc));
            }
            if (allFlag || entType == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
                toBePaged.addAll(serviceManager.getViewableServiceTypes(subject, lpc));
            }
            if (allFlag || entType == AppdefEntityConstants.APPDEF_TYPE_APPLICATION) {
                toBePaged.addAll(applicationManager.getAllApplicationTypes(subject));
            }
            if (allFlag || entType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {

                // For groups we have "psuedo" AppdefResourceTypes.
                int groupTypes[] = AppdefEntityConstants.getAppdefGroupTypesNormalized();

                for (int i = 0; i < groupTypes.length; i++) {
                    AppdefResourceTypeValue tvo = new GroupTypeValue();
                    tvo.setId(new Integer(groupTypes[i]));
                    tvo.setName(AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[i]));
                    toBePaged.add(tvo);
                }
            }
        } catch (NotFoundException e) {
            log.debug("Caught harmless NotFoundException no resource " + "types defined.");
        }
        // TODO: G
        return defaultPager.seek(toBePaged, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * @param platTypePK - the type of platform
     * @return PlatformValue - the saved Value object
     * 
     */
    public Platform createPlatform(int sessionID, PlatformValue platformVal, Integer platTypePK,
                                   Integer agent) throws ValidationException,
        SessionTimeoutException, SessionNotFoundException, PermissionException,
        AppdefDuplicateNameException, AppdefDuplicateFQDNException, ApplicationException {
        try {
            // Get the AuthzSubject for the user's session
            AuthzSubject subject = sessionManager.getSubject(sessionID);
            Platform platform = platformManager.createPlatform(subject, platTypePK, platformVal,
                agent);
            return platform;
        } catch (AppdefDuplicateNameException e) {
            log.error("Unable to create platform. Rolling back", e);
            throw e;
        } catch (AppdefDuplicateFQDNException e) {
            log.error("Unable to create platform. Rolling back", e);
            throw e;
        } catch (PlatformNotFoundException e) {
            log.error("Unable to create platform. Rolling back", e);
            throw new SystemException("Error occurred creating platform:" + e.getMessage());
        } catch (ApplicationException e) {
            log.error("Unable to create platform. Rolling back", e);
            throw e;
        }
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public AppdefResourceTypeValue findResourceTypeById(int sessionID, AppdefEntityTypeID id)
        throws SessionTimeoutException, SessionNotFoundException {
        try {
            AppdefResourceType type = null;
            switch (id.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    type = findPlatformTypeById(sessionID, id.getId());
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    type = findServerTypeById(sessionID, id.getId());
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    type = findServiceTypeById(sessionID, id.getId());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown appdef type: " + id);
            }
            return type.getAppdefResourceTypeValue();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Transactional(readOnly = true)
    public AppdefResourceTypeValue findResourceTypeByResId(int sessionID, Integer resourceId)
    throws SessionTimeoutException, SessionNotFoundException {
        if (resourceId == null) {
            return null;
        }
        try {
            final Resource resType = resourceManager.getResourceById(resourceId);
            AppdefResourceType type = null;
            if (resType.getResourceType().getId().equals(AuthzConstants.authzPlatformProto)) {
                type = findPlatformTypeById(sessionID, resType.getInstanceId());
            } else if (resType.getResourceType().getId().equals(AuthzConstants.authzServerProto)) {
                type = findServerTypeById(sessionID, resType.getInstanceId());
            } else if (resType.getResourceType().getId().equals(AuthzConstants.authzServiceProto)) {
                type = findServiceTypeById(sessionID, resType.getInstanceId());
            } else {
                throw new IllegalArgumentException("Unknown appdef type: " + resType.getInstanceId());
            }
            return type.getAppdefResourceTypeValue();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PlatformType findPlatformTypeById(int sessionID, Integer id)
        throws PlatformNotFoundException, SessionTimeoutException, SessionNotFoundException {
        sessionManager.authenticate(sessionID);
        return platformManager.findPlatformType(id);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PlatformType findPlatformTypeByName(int sessionID, String name)
        throws PlatformNotFoundException, SessionTimeoutException, SessionNotFoundException {
        sessionManager.authenticate(sessionID);
        return platformManager.findPlatformTypeByName(name);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ServiceType findServiceTypeById(int sessionID, Integer id)
        throws SessionTimeoutException, SessionNotFoundException {
        sessionManager.authenticate(sessionID);
        return serviceManager.findServiceType(id);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public PageList<ServiceTypeValue> findServiceTypesByServerType(int sessionID, int serverTypeId)
        throws SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        return serviceManager.getServiceTypesByServerType(subject, serverTypeId);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ServerType findServerTypeById(int sessionID, Integer id) throws SessionTimeoutException,
        SessionNotFoundException {
        sessionManager.authenticate(sessionID);
        return serverManager.findServerType(id);
    }

    /**
     * Private method to call the setCPropValue from a Map
     * @param subject Subject setting the values
     * @param cProps A map of String key/value pairs to set
     */
    private void setCPropValues(AuthzSubject subject, AppdefEntityID entityId,
                                Map<String, String> cProps) throws SessionNotFoundException,
        SessionTimeoutException, CPropKeyNotFoundException, AppdefEntityNotFoundException,
        PermissionException {
        AppdefEntityValue aVal = new AppdefEntityValue(entityId, subject);
        int typeId = aVal.getAppdefResourceType().getId().intValue();
        for (Map.Entry<String, String> entry : cProps.entrySet()) {
            cPropManager.setValue(entityId, typeId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Create a server with CProps
     * @param platformPK - the pk of the host platform
     * @param serverTypePK - the type of server
     * @param cProps - the map with Custom Properties for the server
     * @return ServerValue - the saved server
     * 
     */
    public ServerValue createServer(int sessionID, ServerValue serverVal, Integer platformPK,
                                    Integer serverTypePK, Map<String, String> cProps)
        throws ValidationException, SessionTimeoutException, SessionNotFoundException,
        PermissionException, AppdefDuplicateNameException, CPropKeyNotFoundException,
        NotFoundException {
        try {
            // Get the AuthzSubject for the user's session
            AuthzSubject subject = sessionManager.getSubject(sessionID);

            // Call into appdef to create the platform.

            Server server = serverManager
                .createServer(subject, platformPK, serverTypePK, serverVal);
            if (cProps != null) {
                AppdefEntityID entityId = server.getEntityId();
                setCPropValues(subject, entityId, cProps);
            }

            return server.getServerValue();
        } catch (AppdefEntityNotFoundException e) {
            log.error("Unable to create server.", e);
            throw new SystemException("Unable to find new server");
        }
    }

    /**
     * Create an application
     * @return ApplicationValue - the saved application
     * 
     */
    public ApplicationValue createApplication(int sessionID, ApplicationValue appVal,
                                              ConfigResponse protoProps)
        throws ValidationException, SessionTimeoutException, SessionNotFoundException,
        PermissionException, AppdefDuplicateNameException, NotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);

        Application pk = applicationManager.createApplication(subject, appVal);
        return pk.getApplicationValue();
    }

    /**
     * @param serviceTypePK - the type of service
     * @param aeid - the appdef entity ID
     * @return ServiceValue - the saved ServiceValue
     * 
     */
    public ServiceValue createService(int sessionID, ServiceValue serviceVal,
                                      Integer serviceTypePK, AppdefEntityID aeid)
        throws SessionNotFoundException, SessionTimeoutException, ServerNotFoundException,
        PlatformNotFoundException, PermissionException, AppdefDuplicateNameException,
        ValidationException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        try {
            Integer serverPK;
            if (aeid.isPlatform()) {
                // Look up the platform's virtual server
                List<ServerValue> servers = serverManager.getServersByPlatformServiceType(subject,
                    aeid.getId(), serviceTypePK);

                // There should only be 1 virtual server of this type
                ServerValue server = servers.get(0);
                serverPK = server.getId();
            } else {
                serverPK = aeid.getId();
            }
            Service newSvc = createService(subject, serviceVal, serviceTypePK, serverPK, null);
            return newSvc.getServiceValue();
        } catch (CPropKeyNotFoundException exc) {
            log.error("Error setting no properties for new service");
            throw new SystemException("Error setting no properties.", exc);
        }
    }

    /**
     * Create a service with CProps
     * 
     * @param serviceTypePK - the type of service
     * @param serverPK - the server host
     * @param cProps - the map with Custom Properties for the service
     * @return Service - the saved Service
     * 
     */
    public Service createService(AuthzSubject subject, ServiceValue serviceVal,
                                 Integer serviceTypePK, Integer serverPK, Map<String, String> cProps)
        throws SessionNotFoundException, SessionTimeoutException, AppdefDuplicateNameException,
        ValidationException, PermissionException, CPropKeyNotFoundException {
        try {

            Service savedService = serviceManager.createService(subject, serverPK, serviceTypePK,
                serviceVal.getName(), serviceVal.getDescription(), serviceVal.getLocation());
            if (cProps != null) {
                AppdefEntityID entityId = savedService.getEntityId();
                setCPropValues(subject, entityId, cProps);
            }

            return savedService;
        } catch (AppdefEntityNotFoundException e) {
            log.error("Unable to create service.", e);
            throw new SystemException("Unable to find new service");
        }
    }

    /**
     * Removes an appdef entity by nulling out any reference from its children
     * and then deleting it synchronously. The children are then cleaned up in
     * the zevent queue by issuing a {@link ResourcesCleanupZevent}
     * @param aeid {@link AppdefEntityID} resource to be removed.
     * @return AppdefEntityID[] - an array of the resources (including children)
     *         deleted
     * 
     */
    public AppdefEntityID[] removeAppdefEntity(int sessionId, AppdefEntityID aeid)
    	throws SessionNotFoundException, SessionTimeoutException, ApplicationException,
    	VetoException {
    	return appdefManager.removeAppdefEntity(sessionId, aeid, true);
    }
    
  
    /**
     * Removes an appdef entity by nulling out any reference from its children
     * and then deleting it synchronously. The children are then cleaned up in
     * the zevent queue by issuing a {@link ResourcesCleanupZevent}
     * @param aeid {@link AppdefEntityID} resource to be removed.
     * @param removeAllVirtual tells the method to remove all resources, including
     *        associated platforms, under the virtual resource hierarchy
     * @return AppdefEntityID[] - an array of the resources (including children)
     *         deleted
     * 
     */
    public AppdefEntityID[] removeAppdefEntity(int sessionId, AppdefEntityID aeid,
    										   boolean removeAllVirtual)
        throws SessionNotFoundException, SessionTimeoutException, ApplicationException,
        VetoException {       
        return appdefManager.removeAppdefEntity(sessionId, aeid, removeAllVirtual);
    }


   
    /**
     * 
     */
    public void removePlatform(AuthzSubject subject, Integer platformId)
        throws ApplicationException, VetoException {
        appdefManager.removePlatform(subject, platformId);
    }

    
  
    public void removeService(AuthzSubject subject, Integer serviceId)
        throws VetoException, PermissionException, ServiceNotFoundException {
        appdefManager.removeService(subject, serviceId);
    }

    /**
     * 
     */
    public ServerValue updateServer(int sessionId, ServerValue aServer) throws PermissionException,
        ValidationException, SessionTimeoutException, SessionNotFoundException, UpdateException,
        AppdefDuplicateNameException {
        try {
            return updateServer(sessionId, aServer, null);
        } catch (CPropKeyNotFoundException exc) {
            log.error("Error updating no properties for server");
            throw new SystemException("Error updating no properties.", exc);
        }
    }
    
    public ServerValue updateServer(int sessionId, ServerValue aServer, Map<String, String> cProps)
            throws ValidationException, SessionTimeoutException, SessionNotFoundException,
            PermissionException, UpdateException, AppdefDuplicateNameException,
            CPropKeyNotFoundException {
        return this.updateServer(sessionManager.getSubject(sessionId), aServer, cProps) ;
    }//EOM 

    /**
     * Update a server with cprops.
     * @param cProps - the map with Custom Properties for the server
     * 
     */
    public ServerValue updateServer(final AuthzSubject subject, ServerValue aServer, Map<String, String> cProps)
        throws ValidationException, SessionTimeoutException, SessionNotFoundException,
        PermissionException, UpdateException, AppdefDuplicateNameException,
        CPropKeyNotFoundException {
        try {
            try {
                Server updated = serverManager.updateServer(subject, aServer);

                if (cProps != null) {
                    AppdefEntityID entityId = aServer.getEntityId();
                    setCPropValues(subject, entityId, cProps);
                }
                return updated.getServerValue();
            } catch (Exception e) {
                log.error("Error updating server: " + aServer.getId());
                throw e;
            }
            // } catch (CreateException e) {
            // change to a update exception as this only occurs
            // if there was a failure instantiating the session
            // bean
            // throw new UpdateException("Error creating manager session bean: "
            // + e.getMessage());
        } catch (PermissionException e) {
            throw (PermissionException) e;
        } catch (AppdefDuplicateNameException e) {
            throw (AppdefDuplicateNameException) e;
        } catch (CPropKeyNotFoundException e) {
            throw (CPropKeyNotFoundException) e;
        } catch (AppdefEntityNotFoundException e) {
            throw new SystemException("Unable to find updated server");
        } catch (Exception e) {
            throw new UpdateException("Unknown error updating server: " + aServer.getId(), e);
        }
    }

    /**
     * 
     */
    public ServiceValue updateService(int sessionId, ServiceValue aService)
        throws PermissionException, ValidationException, SessionTimeoutException,
        SessionNotFoundException, UpdateException, AppdefDuplicateNameException, NotFoundException {
        try {
            return updateService(sessionId, aService, null);
        } catch (CPropKeyNotFoundException exc) {
            log.error("Error updating no properties for service");
            throw new SystemException("Error updating no properties.", exc);
        }
    }

    /**
     * Update a service with cProps.
     * @param cProps - the map with Custom Properties for the service
     * 
     */
    public ServiceValue updateService(int sessionId, ServiceValue aService,
                                      Map<String, String> cProps) throws ValidationException,
        SessionTimeoutException, SessionNotFoundException, PermissionException, UpdateException,
        AppdefDuplicateNameException, CPropKeyNotFoundException, NotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return updateService(subject, aService, cProps);
    }

    /**
     * Update a service with cProps.
     * @param cProps - the map with Custom Properties for the service
     * 
     */
    public ServiceValue updateService(AuthzSubject subject, ServiceValue aService,
                                      Map<String, String> cProps) throws ValidationException,
        SessionTimeoutException, SessionNotFoundException, PermissionException, UpdateException,
        AppdefDuplicateNameException, CPropKeyNotFoundException, NotFoundException {
        try {
            Service updated = serviceManager.updateService(subject, aService);

            if (cProps != null) {
                AppdefEntityID entityId = aService.getEntityId();
                setCPropValues(subject, entityId, cProps);
            }
            return updated.getServiceValue();
        } catch (Exception e) {
            log.error("Error updating service: " + aService.getId());

            if (e instanceof PermissionException) {
                throw (PermissionException) e;
            } else if (e instanceof NotFoundException) {
                throw (NotFoundException) e;
            } else if (e instanceof AppdefDuplicateNameException) {
                throw (AppdefDuplicateNameException) e;
            } else if (e instanceof CPropKeyNotFoundException) {
                throw (CPropKeyNotFoundException) e;
            } else if (e instanceof AppdefEntityNotFoundException) {
                throw new SystemException("Unable to find updated service");
            } else {
                throw new UpdateException("Unknown error updating service: " + aService.getId(), e);
            }
        }
    }

    /**
     * 
     */
    public PlatformValue updatePlatform(int sessionId, PlatformValue aPlatform)
        throws ValidationException, PermissionException, SessionTimeoutException,
        SessionNotFoundException, UpdateException, ApplicationException,
        AppdefDuplicateNameException, AppdefDuplicateFQDNException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return updatePlatform(subject, aPlatform);
    }

    /**
     * 
     */
    public PlatformValue updatePlatform(AuthzSubject subject, PlatformValue aPlatform)
        throws ValidationException, PermissionException, SessionTimeoutException,
        SessionNotFoundException, UpdateException, ApplicationException,
        AppdefDuplicateNameException, AppdefDuplicateFQDNException {
        try {
            return platformManager.updatePlatform(subject, aPlatform).getPlatformValue();
        } catch (Exception e) {
            log.error("Error updating platform: " + aPlatform.getId());
            // rollback();
            if (e instanceof PermissionException) {
                throw (PermissionException) e;
            } else if (e instanceof AppdefDuplicateNameException) {
                throw (AppdefDuplicateNameException) e;
            } else if (e instanceof AppdefDuplicateFQDNException) {
                throw (AppdefDuplicateFQDNException) e;
            } else if (e instanceof ApplicationException) {
                throw (ApplicationException) e;
            } else {
                throw new UpdateException("Unknown error updating platform: " + aPlatform.getId(),
                    e);
            }
        }
    }

    /**
     * 
     */
    public ApplicationValue updateApplication(int sessionId, ApplicationValue app)
        throws ApplicationException, PermissionException {
        try {
            AuthzSubject caller = sessionManager.getSubject(sessionId);
            return applicationManager.updateApplication(caller, app);
        } catch (Exception e) {

            throw new SystemException(e);
        }
    }

    /**
     * Set the services used by an application indicate whether the service is
     * an entry point
     * 
     */
    public void setApplicationServices(int sessionId, Integer appId, List<AppdefEntityID> entityIds)
        throws ApplicationException, PermissionException {
        try {
            AuthzSubject caller = sessionManager.getSubject(sessionId);
            applicationManager.setApplicationServices(caller, appId, entityIds);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Get the dependency tree for a given application
     * 
     */
    @Transactional(readOnly = true)
    public DependencyTree getAppDependencyTree(int sessionId, Integer appId)
        throws ApplicationException, PermissionException {
        try {
            AuthzSubject caller = sessionManager.getSubject(sessionId);
            return applicationManager.getServiceDepsForApp(caller, appId);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * 
     */
    public void setAppDependencyTree(int sessionId, DependencyTree depTree)
        throws ApplicationException, PermissionException {
        try {
            AuthzSubject caller = sessionManager.getSubject(sessionId);
            applicationManager.setServiceDepsForApp(caller, depTree);
        } catch (Exception e) {

            throw new SystemException(e);
        }
    }

   
    public void removeServer(AuthzSubject subj, Integer serverId)
    throws ServerNotFoundException, SessionNotFoundException, SessionTimeoutException, PermissionException,
           SessionException, VetoException {
       appdefManager.removeServer(subj, serverId);
    }

    /**
     * Remove an application service.
     * @param appId - The application identifier.
     * 
     */
    public void removeAppService(int sessionId, Integer appId, Integer serviceId)
        throws ApplicationException, ApplicationNotFoundException, PermissionException,
        SessionTimeoutException, SessionNotFoundException {

        AuthzSubject caller = sessionManager.getSubject(sessionId);
        applicationManager.removeAppService(caller, appId, serviceId);

    }

    /**
     * @return The updated Resource
     * 
     */
    public AppdefResourceValue changeResourceOwner(int sessionId, AppdefEntityID eid,
                                                   Integer newOwnerId) throws ApplicationException,
        PermissionException {
        try {
            AuthzSubject caller = sessionManager.getSubject(sessionId);
            AuthzSubject newOwner = authzSubjectManager.findSubjectById(newOwnerId);

            if (eid.isGroup()) {
                ResourceGroup g = resourceGroupManager.findResourceGroupById(caller, eid.getId());
                if (g.getGroupType() == AppdefEntityConstants.APPDEF_TYPE_GROUP_DYNAMIC) {
                    throw new GroupException("Can't change owner of a dynamic group");
                }
                resourceGroupManager.changeGroupOwner(caller, g, newOwner);
//                zEventManager.enqueueEventAfterCommit(new ResourceOwnerChangedZevent(newOwnerId, eid, aevResource.getId()));
                return findGroup(sessionId, eid.getId());
            }

            AppdefEntityValue aev = new AppdefEntityValue(eid, caller);
            Resource aevResource = aev.getResourcePOJO().getResource();
            final AuthzSubject oldOwner = aevResource.getOwner();
            appdefManager.changeOwner(caller, aev.getResourcePOJO(), newOwner);
            
            zEventManager.enqueueEventAfterCommit(new ResourceOwnerChangedZevent(oldOwner.getId(), newOwnerId, eid, aevResource.getId()));
            return aev.getResourceValue();
        } catch (PermissionException e) {
            throw e;
        } catch (Exception e) {
            // everything else is a system error
            throw new SystemException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Resource> getResources(String[] resources) {
        if (resources == null)
            return Collections.EMPTY_LIST;

        List<Resource> ret = new ArrayList<Resource>(resources.length);

        for (int i = 0; i < resources.length; i++) {
            AppdefEntityID aeid = new AppdefEntityID(resources[i]);
            ret.add(resourceManager.findResource(aeid));
        }
        return ret;
    }

    /**
     * Create and return a new mixed group value object. This group can contain
     * mixed resources of any entity/resource type combination including
     * platform, server and service.
     * @param name - The name of the group.
     * @param description - A description of the group contents. (optional)
     * @param location - Location of group (optional)
     * @return AppdefGroupValue object
     * 
     */
    @SuppressWarnings("unchecked")
    public ResourceGroup createGroup(int sessionId, String name, String description,
                                     String location, String[] resources, boolean privGrp)
        throws GroupCreationException, GroupDuplicateNameException, SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        ResourceGroupCreateInfo cInfo = new ResourceGroupCreateInfo(name, description,
            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS, null, // prototype
            location, 0, // clusterId
            false, // system?
            privGrp);

        // No roles or resources
        return resourceGroupManager.createResourceGroup(subject, cInfo, Collections.EMPTY_LIST,
            getResources(resources));
    }

    /**
     * Create and return a new strict mixed group value object. This type of
     * group can contain either applications or other groups. However, the
     * choice between between the two is mutually exclusive because all group
     * members must be of the same entity type. Additionally, groups that
     * contain groups are limited to containing either "application groups" or
     * "platform,server&service groups".
     * @param adType - The appdef entity type (groups or applications)
     * @param name - The name of the group.
     * @param description - A description of the group contents. (optional)
     * @param location - Location of group (optional)
     * @return AppdefGroupValue object
     * 
     */
    @SuppressWarnings("unchecked")
    public ResourceGroup createGroup(int sessionId, int adType, String name, String description,
                                     String location, String[] resources, boolean privGrp)
        throws GroupCreationException, SessionException, GroupDuplicateNameException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        int groupType;

        if (adType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP;
        } else if (adType == AppdefEntityConstants.APPDEF_TYPE_APPLICATION) {
            groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
        } else {
            throw new IllegalArgumentException("Invalid group type. Strict "
                                               + "mixed group types can be "
                                               + "group or application");
        }

        ResourceGroupCreateInfo cInfo = new ResourceGroupCreateInfo(name, description, groupType,
            null, // prototype
            location, 0, // clusterId
            false, // system?
            privGrp);

        // No roles or resources
        return resourceGroupManager.createResourceGroup(subject, cInfo, Collections.EMPTY_LIST,
            getResources(resources));
    }

    /**
     * Create and return a new compatible group type object. This group type can
     * contain any type of platform, server or service. Compatible groups are
     * strict which means that all members must be of the same type. Compatible
     * group members must also be compatible which means that all group members
     * must have the same resource type. Compatible groups of services have an
     * additional designation of being of type "Cluster".
     * @param adType - The type of entity this group is compatible with.
     * @param adResType - The resource type this group is compatible with.
     * @param name - The name of the group.
     * @param description - A description of the group contents. (optional)
     * @param location - Location of group (optional)
     * 
     */
    @SuppressWarnings("unchecked")
    public ResourceGroup createGroup(int sessionId, int adType, int adResType, String name,
                                     String description, String location, String[] resources,
                                     boolean privGrp) throws GroupCreationException,
        GroupDuplicateNameException, SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        int groupType;

        switch (adType) {
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
                break;
            default:
                throw new IllegalArgumentException("Invalid group compatibility "
                                                   + "type specified");
        }

        Resource prototype = resourceManager.findResourcePrototype(new AppdefEntityTypeID(adType,
            adResType));

        ResourceGroupCreateInfo cInfo = new ResourceGroupCreateInfo(name, description, groupType,
            prototype, location, 0, // clusterId
            false, // system?
            privGrp);

        // No roles or resources
        return resourceGroupManager.createResourceGroup(subject, cInfo, Collections.EMPTY_LIST,
            getResources(resources));
    }

    /**
     * Remove resources from the group's contents.
     * 
     * 
     */
    public void removeResourcesFromGroup(int sessionId, ResourceGroup group,
                                         Collection<Resource> resources) throws SessionException,
        PermissionException, VetoException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        resourceGroupManager.removeResources(subject, group, resources);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public ResourceGroup findGroupById(int sessionId, Integer groupId) throws PermissionException,
        SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return resourceGroupManager.findResourceGroupById(subject, groupId);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Map<String, Number> getResourceTypeCountMap(int sessionId, Integer groupId)
        throws PermissionException, SessionException {
        ResourceGroup g = findGroupById(sessionId, groupId);
        return resourceGroupManager.getMemberTypes(g);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public AppdefGroupValue findGroup(int sessionId, Integer id) throws PermissionException,
        SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        ResourceGroup group = resourceGroupManager.findResourceGroupById(subject, id);
        return resourceGroupManager.getGroupConvert(subject, group);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceGroup> getGroupsForResource(int sessionId, Resource r)
        throws SessionNotFoundException, SessionTimeoutException {
        sessionManager.authenticate(sessionId);
        return resourceGroupManager.getGroups(r);
    }

    /**
     * Lookup and return a list of group value objects by their identifiers.
     * @return PageList of AppdefGroupValue objects
     * @throws AppdefGroupNotFoundException when group cannot be found.
     * @throws InvalidAppdefTypeException if group is compat and the appdef type
     *         id is incorrect.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefGroupValue> findGroups(int sessionId, Integer[] groupIds, PageControl pc)
        throws PermissionException, SessionException {
        List<AppdefGroupValue> toBePaged = new ArrayList<AppdefGroupValue>(groupIds.length);
        for (int i = 0; i < groupIds.length; i++) {
            toBePaged.add(findGroup(sessionId, groupIds[i]));
        }
        return getPageList(toBePaged, pc);
    }

    /**
     * Produce list of all groups where caller is authorized to modify. Include
     * just those groups that contain the specified appdef entity.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefGroupValue> findAllGroupsMemberInclusive(int sessionId, PageControl pc,
                                                                   AppdefEntityID entity)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException,
        ApplicationException {
        return findAllGroupsMemberInclusive(sessionId, pc, entity, new Integer[0]);
    }

    /**
     * Produce list of all groups where caller is authorized to modify. Include
     * just those groups that contain the specified appdef entity. Apply group
     * filter to remove unwanted groups.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     */
    @Transactional(readOnly = true)
    private PageList<AppdefGroupValue> findAllGroupsMemberInclusive(int sessionId, PageControl pc,
                                                                    AppdefEntityID entity,
                                                                    Integer[] excludeIds)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException,
        ApplicationException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        List<ResourceGroup> excludeGroups = new ArrayList<ResourceGroup>(excludeIds.length);
        for (int i = 0; i < excludeIds.length; i++) {
            excludeGroups.add(resourceGroupManager.findResourceGroupById(excludeIds[i]));
        }

        Resource r = resourceManager.findResource(entity);
        PageInfo pInfo = PageInfo.create(pc, ResourceGroupSortField.NAME);
        PageList<ResourceGroup> res = resourceGroupManager.findGroupsContaining(subject, r,
            excludeGroups, pInfo);
        List<AppdefGroupValue> appVals = new ArrayList<AppdefGroupValue>(res.size());
        for (ResourceGroup g : res) {
            appVals.add(resourceGroupManager.getGroupConvert(subject, g));
        }
        // TODO: G
        return new PageList(appVals, res.getTotalSize());
    }

    /**
     * Produce list of all groups where caller is authorized to modify. Exclude
     * any groups that contain the appdef entity id.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefGroupValue> findAllGroupsMemberExclusive(int sessionId, PageControl pc,
                                                                   AppdefEntityID entity)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException {
        return findAllGroupsMemberExclusive(sessionId, pc, entity, null, null);
    }

    /**
     * Produce list of all groups where caller is authorized to modify. Exclude
     * any groups that contain the appdef entity id.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefGroupValue> findAllGroupsMemberExclusive(int sessionId, PageControl pc,
                                                                   AppdefEntityID entity,
                                                                   Integer[] removeIds)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException {
        return findAllGroupsMemberExclusive(sessionId, pc, entity, removeIds, null);
    }

    /**
     * Produce list of all groups where caller is authorized to modify. Exclude
     * any groups that contain the appdef entity id. Filter out any unwanted
     * groups specified by groupId array.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefGroupValue> findAllGroupsMemberExclusive(int sessionId, PageControl pc,
                                                                   AppdefEntityID entity,
                                                                   Integer[] removeIds,
                                                                   Resource resourceType)
        throws PermissionException, SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        List<ResourceGroup> excludeGroups = new ArrayList<ResourceGroup>(removeIds.length);

        for (int i = 0; i < removeIds.length; i++) {
            excludeGroups.add(resourceGroupManager.findResourceGroupById(removeIds[i]));
        }
        Resource r = resourceManager.findResource(entity);
        PageInfo pInfo = PageInfo.create(pc, ResourceGroupSortField.NAME);
        PageList<ResourceGroup> res = resourceGroupManager.findGroupsNotContaining(subject, r,
            resourceType, excludeGroups, pInfo);

        // Now convert those ResourceGroups into AppdefResourceGroupValues
        List<AppdefGroupValue> appVals = new ArrayList<AppdefGroupValue>(res.size());
        for (ResourceGroup g : res) {
            appVals.add(resourceGroupManager.getGroupConvert(subject, g));
        }

        return new PageList<AppdefGroupValue>(appVals, res.getTotalSize());
    }

    /**
     * Produce list of all groups where caller is authorized to modify. Exclude
     * any groups that contain the appdef entity id. Filter out any unwanted
     * groups specified by groupId array.
     * @param entity for use in group member filtering.
     * @return List containing AppdefGroupValue.
     * 
     * XXX needs to be optimized
     */
    @Transactional(readOnly = true)
    public PageList<AppdefGroupValue> findAllGroupsMemberExclusive(int sessionId, PageControl pc,
                                                                   AppdefEntityID[] entities)
        throws PermissionException, SessionException {
        List<AppdefGroupValue> commonList = new ArrayList<AppdefGroupValue>();

        for (int i = 0; i < entities.length; i++) {
            AppdefEntityID eid = entities[i];
            Resource resource = resourceManager.findResource(eid);
            List<AppdefGroupValue> result = findAllGroupsMemberExclusive(sessionId, pc, eid,
                new Integer[] {}, resource.getPrototype());

            if (i == 0) {
                commonList.addAll(result);
            } else {
                commonList.retainAll(result);
            }

            if (commonList.isEmpty()) {
                // no groups in common, so exit
                break;
            }
        }

        return new PageList<AppdefGroupValue>(commonList, commonList.size());
    }

    /**
     * Produce list of all group pojos where caller is authorized
     * @return List containing AppdefGroup.
     * 
     */
    @Transactional(readOnly = true)
    public Collection<ResourceGroup> findAllGroupPojos(int sessionId) throws PermissionException,
        SessionTimeoutException, SessionNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        Collection<ResourceGroup> resGrps = resourceGroupManager
            .getAllResourceGroups(subject, true);

        // We only want the appdef resource groups
        for (Iterator<ResourceGroup> it = resGrps.iterator(); it.hasNext();) {
            ResourceGroup resGrp = it.next();
            if (resGrp.isSystem()) {
                it.remove();
            }
        }
        return resGrps;
    }

    /**
     * Add entities to a resource group
     * 
     */
    public void addResourcesToGroup(int sessionID, ResourceGroup group, List<AppdefEntityID> aeids)
        throws SessionException, PermissionException, VetoException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);

        for (AppdefEntityID aeid : aeids) {
            Resource resource = resourceManager.findResource(aeid);
            resourceGroupManager.addResource(subject, group, resource);
        }
    }

    /**
     * Update properties of a group.
     * 
     * @see ResourceGroupManagerImpl.updateGroup
     * 
     */
    public void updateGroup(int sessionId, ResourceGroup group, String name, String description,
                            String location) throws SessionException, PermissionException,
        GroupDuplicateNameException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        resourceGroupManager.updateGroup(subject, group, name, description, location);
    }

    // Return a PageList of authz resources.
    private List<AppdefEntityID> findViewableEntityIds(AuthzSubject subject, int appdefTypeId,
                                                       String rName, Integer protoFilterId,
                                                       PageControl pc) {
        List<AppdefEntityID> appentResources = new ArrayList<AppdefEntityID>();

        if (appdefTypeId != APPDEF_TYPE_UNDEFINED) {
            Integer protoType = null;
            Collection<ResourceType> resourceTypes = new ArrayList<ResourceType>();
            switch (appdefTypeId) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    protoType = AuthzConstants.authzPlatformProto;
                    resourceTypes.add(resourceManager.findResourceTypeById(AuthzConstants.authzPlatform));
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    protoType = AuthzConstants.authzServerProto;
                    resourceTypes.add(resourceManager.findResourceTypeById(AuthzConstants.authzServer));
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    protoType = AuthzConstants.authzServiceProto;
                    resourceTypes.add(resourceManager.findResourceTypeById(AuthzConstants.authzService));
                    break;
                default:
                    break;
            }
            final Collection<Integer> resourceIds =
                resourceManager.findAllViewableResourceIds(subject, resourceTypes);
            for (final Integer resourceId : resourceIds) {
                final Resource r = resourceManager.findResourceById(resourceId);
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                if (rName != null && !r.getName().toLowerCase().contains(rName.toLowerCase())) {
                    continue;
                }
                try {
                    if (protoFilterId != null && protoType != null) {
                        final Resource proto = r.getPrototype();
                        if (proto.getInstanceId().equals(protoFilterId) &&
                                proto.getResourceType().getId().equals(protoType)) {
                            appentResources.add(AppdefUtil.newAppdefEntityId(r));
                        }
                    } else {
                        appentResources.add(AppdefUtil.newAppdefEntityId(r));
                    }
                } catch (IllegalArgumentException e) {
                }
            }
        } else {
            final Collection<Integer> resourceIds =
                resourceManager.findAllViewableResourceIds(subject, null);
            for (final Integer resourceId : resourceIds) {
                final Resource r = resourceManager.findResourceById(resourceId);
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                try {
                    appentResources.add(AppdefUtil.newAppdefEntityId(r));
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return appentResources;
    }

    /**
     * Produce list of compatible, viewable inventory items. The returned list
     * of value objects will consist only of group inventory compatible with the
     * the specified group type.
     * 
     * NOTE: This method returns an empty page list when no compatible inventory
     * is found.
     * @param groupType - the optional group type
     * @param appdefTypeId - the id correponding to the type of entity. example:
     *        group, platform, server, service NOTE: A valid entity type id is
     *        now MANDATORY!
     * @param appdefResTypeId - the id corresponding to the type of resource
     *        example: linux, jboss, vhost
     * @param resourceName - resource name (or name substring) to search for.
     * @return page list of value objects that extend AppdefResourceValue
     * 
     */
    @Transactional(readOnly = true)
    public PageList findCompatInventory(int sessionId, int groupType, int appdefTypeId,
                                        int groupEntTypeId, int appdefResTypeId,
                                        String resourceName, AppdefEntityID[] pendingEntities,
                                        PageControl pc) throws AppdefEntityNotFoundException,
        PermissionException, SessionException {
        if (groupType != APPDEF_GROUP_TYPE_UNDEFINED &&
            !AppdefEntityConstants.groupTypeIsValid(groupType)) {
            throw new IllegalArgumentException("Invalid group type: " + groupType);
        }

        return findCompatInventory(sessionId, appdefTypeId, appdefResTypeId, groupEntTypeId, null,
            false, pendingEntities, resourceName, null, groupType, pc);
    }

    /**
     * Produce list of compatible, viewable inventory items. The returned list
     * of value objects will be filtered on AppdefGroupValue -- if the group
     * contains the entity, then then the entity will not be included in the
     * returned set.
     * 
     * NOTE: This method returns an empty page list when no compatible inventory
     * is found.
     * @param appdefTypeId - the id correponding to the type of entity example:
     *        platform, server, service NOTE: A valid entity type id is now
     *        MANDATORY!
     * @param appdefResTypeId - the id corresponding to the type of resource
     *        example: linux, jboss, vhost
     * @param groupEntity - the appdef entity of a group value who's members are
     *        to be filtered out of result set.
     * @param resourceName - resource name (or name substring) to search for.
     * @return page list of value objects that extend AppdefResourceValue
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefResourceValue> findCompatInventory(int sessionId, int appdefTypeId,
                                                             int appdefResTypeId,
                                                             AppdefEntityID groupEntity,
                                                             AppdefEntityID[] pendingEntities,
                                                             String resourceName, PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionException {
        PageList<AppdefResourceValue> ret = findCompatInventory(sessionId, appdefTypeId,
            appdefResTypeId, APPDEF_GROUP_TYPE_UNDEFINED, groupEntity, false, pendingEntities,
            resourceName, null, APPDEF_GROUP_TYPE_UNDEFINED, pc);

        if (appdefTypeId == AppdefEntityConstants.APPDEF_TYPE_SERVER ||
            appdefTypeId == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            for (AppdefResourceValue res : ret) {

                if (appdefTypeId == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
                    Server server = serverManager.findServerById(res.getId());
                    Platform platform = server.getPlatform();
                    if (platform != null) {
                        res.setHostName(platform.getName());
                    }

                } else {
                    Service service = serviceManager.findServiceById(res.getId());
                    Server server = service.getServer();
                    if (server != null) {
                        res.setHostName(server.getName());
                    }
                }
            }
        }
        return ret;
    }

    private PageList<AppdefResourceValue> findCompatInventory(int sessionId, int appdefTypeId,
                                                              int appdefResTypeId, int grpEntId,
                                                              AppdefEntityID groupEntity,
                                                              boolean members,
                                                              AppdefEntityID[] pendingEntities,
                                                              String resourceName,
                                                              List<AppdefPagerFilter> filterList,
                                                              int groupType, PageControl pc)
        throws PermissionException, SessionException {
        AuthzSubject subj = sessionManager.getSubject(sessionId);
        AppdefPagerFilterGroupEntityResource erFilter;
        AppdefPagerFilterAssignSvc assignedSvcFilter;
        AppdefPagerFilterGroupMemExclude groupMemberFilter;
        boolean groupEntContext = groupType != APPDEF_GROUP_TYPE_UNDEFINED ||
                                  grpEntId != APPDEF_GROUP_TYPE_UNDEFINED;

        StopWatch watch = new StopWatch();
        watch.markTimeBegin("findCompatInventory");

        // init our (never-null) page and filter lists
        if (filterList == null) {
            filterList = new ArrayList<AppdefPagerFilter>();
        }
        assignedSvcFilter = null;
        groupMemberFilter = null;

        // This list can contain items having different appdef types
        // we need to screen the list and only include items matching the
        // value of appdefTypeId. Otherwise, the paging logic below
        // will be thrown off (it assumes that list of pendingEntities is always
        // a subset of the available, which is not always the case) [HHQ-3026]
        List<AppdefEntityID> pendingEntitiesFiltered = new ArrayList<AppdefEntityID>();

        // add a pager filter for removing pending appdef entities
        if (pendingEntities != null) {

            for (int x = 0; x < pendingEntities.length; x++) {
                if (pendingEntities[x].getType() == appdefTypeId) {
                    pendingEntitiesFiltered.add(pendingEntities[x]);
                }
            }

            filterList.add(new AppdefPagerFilterExclude((AppdefEntityID[]) pendingEntitiesFiltered
                .toArray(new AppdefEntityID[pendingEntitiesFiltered.size()])));
        }

        // If the caller supplied a group entity for filtering, this will be
        // used for (i) removing inventory already in the group and (ii)
        // filtering out incompatible inventory. Otherwise, we assume a context
        // where groupType is explicitly passed in. (i.e. the
        // resource hub)
        if (groupEntity == null) {
            if (groupType == APPDEF_GROUP_TYPE_UNDEFINED) {
                if (appdefTypeId == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                    groupType = appdefResTypeId;
                    appdefResTypeId = APPDEF_RES_TYPE_UNDEFINED;
                }
            }
        } else {
            try {
                AppdefGroupValue gValue = findGroup(sessionId, groupEntity.getId());
                groupType = gValue.getGroupType();
                groupMemberFilter = new AppdefPagerFilterGroupMemExclude(gValue, members);
                filterList.add(groupMemberFilter);
            } catch (PermissionException e) {
                // Should never happen, finder accounts for permissions;
                log.error("Caught permission exc filtering on group", e);
            }
        }

        // Install a filter that uses group type, entity type and
        // resource type to filter the inventory set. This facilitates
        // the HTML selectors that appear all over the product.
        if (groupEntContext) {
            erFilter = new AppdefPagerFilterGroupEntityResource(subj, groupType, grpEntId,
                appdefResTypeId, true);
            filterList.add(erFilter);
        } else if (groupEntity != null) {
            erFilter = new AppdefPagerFilterGroupEntityResource(subj, groupType, appdefTypeId,
                appdefResTypeId, true);
            erFilter.setGroupSelected(true);
            filterList.add(erFilter);
        } else {
            erFilter = null;
        }

        // find ALL viewable resources by entity (type or name) and
        // translate to appdef entities.
        // We have to create a new page control because we are no
        // longer limiting the size of the record set in authz.
        watch.markTimeBegin("findViewableEntityIds");
        Integer filterType = appdefResTypeId != -1 ? new Integer(appdefResTypeId) : null;

        List<AppdefEntityID> toBePaged = findViewableEntityIds(subj, appdefTypeId, resourceName,
            filterType, pc);
        watch.markTimeEnd("findViewableEntityIds");

        // Page it, then convert to AppdefResourceValue
        List<AppdefResourceValue> finalList = new ArrayList<AppdefResourceValue>();
        watch.markTimeBegin("getPageList");
        // TODO: G
        PageList<AppdefEntityID> pl = getPageList(toBePaged, pc, filterList);
        watch.markTimeEnd("getPageList");

        for (AppdefEntityID ent : pl) {
            AppdefEntityValue aev = new AppdefEntityValue(ent, subj);

            try {
                if (ent.isGroup()) {
                    finalList.add(aev.getAppdefGroupValue());
                } else {
                    AppdefResource resource = aev.getResourcePOJO();
                    finalList.add(resource.getAppdefResourceValue());
                }
            } catch (AppdefEntityNotFoundException e) {
                // XXX - hack to ignore the error. This must have occurred
                // when we created the resource, and rolled back the
                // AppdefEntity but not the Resource
                log.error("Invalid entity still in resource table: " + ent);
                continue;
            }
        }

        // Use pendingEntitiesFiltered as it will contain the correct number of
        // items based on the selected appdeftype [HHQ-3026]
        int pendingSize = 0;
        if (pendingEntitiesFiltered != null)
            pendingSize = pendingEntitiesFiltered.size();

        int erFilterSize = 0;
        if (erFilter != null)
            erFilterSize = erFilter.getFilterCount();

        int assignedSvcFilterSize = 0;
        if (assignedSvcFilter != null)
            assignedSvcFilterSize = assignedSvcFilter.getFilterCount();

        int groupMemberFilterSize = 0;
        if (groupMemberFilter != null)
            groupMemberFilterSize = groupMemberFilter.getFilterCount();

        int adjustedSize = toBePaged.size() - erFilterSize - pendingSize - assignedSvcFilterSize -
                           groupMemberFilterSize;
        watch.markTimeEnd("findCompatInventory");
        log.debug("findCompatInventory(): " + watch);
        return new PageList<AppdefResourceValue>(finalList, adjustedSize);
    }

    @Transactional(readOnly = true)
    public PageList<AppdefResourceValue> search(int sessionId, int appdefTypeId, String searchFor,
                                                AppdefEntityTypeID appdefResType, Integer groupId,
                                                int[] groupSubType, boolean matchAny, boolean matchOwn,
                                                boolean matchUnavail, PageControl pc)
    throws PermissionException, SessionException, PatternSyntaxException {
        final AuthzSubject subj = sessionManager.getSubject(sessionId);
        final Set<Integer> groupSubTypeSet = getSet(groupSubType);
        final Collection<ResourceType> types = Collections.singletonList(getResourceType(groupSubTypeSet, appdefTypeId));
        final Reference<Integer> totalSetSize = new Reference<Integer>(0);
        final Resource proto = (appdefResType != null) ? resourceManager.findResourcePrototype(appdefResType) : null;
        final Resource protoToExclude =
            resourceManager.findResourcePrototypeByName(AuthzConstants.platformPrototypeVmwareVsphereVm);
        Integer excludeId = (protoToExclude == null) ? null : protoToExclude.getId();
        if (proto != null && excludeId != null && excludeId.equals(proto.getId())) {
            excludeId = null;
        }
        final IntegerTransformer<AppdefResourceValue> transformer =
            getIntegerTransformer(subj, appdefTypeId, searchFor, appdefResType, groupId, groupSubTypeSet, matchAny,
                                matchOwn, matchUnavail, pc, totalSetSize, excludeId);
        final Comparator<AppdefResourceValue> comparator = getNameComparator(pc.getSortorder());
        final Set<AppdefResourceValue> resources =
            permissionManager.findViewableResources(subj, types, pc.getSortorder(), transformer, comparator);
        final PageList<AppdefResourceValue> rtn = new PageList<AppdefResourceValue>(resources, totalSetSize.get());
        return rtn;
    }

    private IntegerTransformer<AppdefResourceValue> getIntegerTransformer(final AuthzSubject subj, 
                                                                      final int appdefTypeId, String searchFor,
                                                                      final AppdefEntityTypeID appdefResType,
                                                                      final Integer groupId,
                                                                      final Set<Integer> groupSubType,
                                                                      final boolean matchAny, final boolean matchOwn,
                                                                      final boolean matchUnavail,
                                                                      final PageControl pc,
                                                                      final Reference<Integer> totalSetSize,
                                                                      final Integer excludeId) {
        final Map<Integer, DownMetricValue> unavails = matchUnavail ? availabilityManager.getUnavailResMap() : null;
        final Pattern pattern = (searchFor != null) ? Pattern.compile(searchFor, Pattern.CASE_INSENSITIVE) : null;
        final int pagesize = (pc != null) ? pc.getPagesize() : Integer.MAX_VALUE;
        final int startIndex = (pc != null) ? pc.getPagenum() * pc.getPagesize() : 0;
        final Integer subjectId = subj.getId();
        final Integer protoId =
            (appdefResType != null) ? resourceManager.findResourcePrototype(appdefResType).getId() : null;
        final boolean noSelections = pattern == null && appdefResType == null && groupId == null &&
                                     groupSubType == null && !matchOwn && !matchUnavail;
        final boolean isGroup = (appdefTypeId == AppdefEntityConstants.APPDEF_TYPE_GROUP) ||
                                (groupSubType != null && !groupSubType.isEmpty());
        final Set<Resource> groupMembers = getGroupMembers(groupId);
        final IntegerTransformer<AppdefResourceValue> rtn = new IntegerTransformer<AppdefResourceValue>() {
            int returned = 0;
            int index = 0;
            int resProtoId;
            @SuppressWarnings("deprecation")
            public AppdefResourceValue transform(Integer id) {
                // don't do any work if there are no selections and if the item is not within the page
                if (noSelections && !(index >= startIndex && returned < pagesize)) {
                    index++;
                    totalSetSize.set(totalSetSize.get()+1);
                    return null;
                }
                final Resource resource = resourceManager.getResourceById(id);
                if (resource == null || resource.isInAsyncDeleteState()) {
                    return null;
                }
                if (isGroup) {
                    final ResourceGroup group = resourceGroupManager.getGroupById(resource.getInstanceId());
                    Integer groupType = group.getGroupType();
                    if (group == null) {
                        return null;
                    } else if (matchAny && isMixedGroup(groupType)) {
                        // do nothing
                    } else if (!groupSubType.contains(groupType)) {
                        return null;
                    }
                    final Resource grpPrototype = group.getResourcePrototype();
                    // mixed groups don't have a prototype
                    resProtoId = (grpPrototype == null) ? Integer.MIN_VALUE : grpPrototype.getId();
                } else {
                    resProtoId = resource.getPrototype().getId();
                }
                if (noSelections || matchAny && matchedAny(resource) || matchedAll(resource)) {
                    totalSetSize.set(totalSetSize.get()+1);
                    if (pc == null || (index++ >= startIndex && returned < pagesize)) {
                        final AppdefResourceValue rtn = AppdefResourceValue.convertToAppdefResourceValue(resource);
                        if (rtn != null) {
                            returned++;
                        }
                        return rtn;
                    }
                }
                return null;
            }
            private boolean isMixedGroup(Integer groupType) {
                return groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS ||
                       groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP ||
                       groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
            }
            private boolean matchedAll(Resource resource) {
                if (excludeId != null && excludeId.equals(resProtoId)) {
                    return false;
                }
                return (pattern == null || pattern.matcher(resource.getName()).find()) &&
                       (!matchUnavail   || unavails.containsKey(resource.getId()))     &&
                       (groupId == null || groupMembers.contains(resource))            &&
                       (protoId == null || protoId.equals(resProtoId))                 &&
                       (!matchOwn       || resource.getOwner().getId().equals(subjectId));
            }
            private boolean matchedAny(Resource resource) {
                if (excludeId != null && excludeId.equals(resProtoId)) {
                    return false;
                }
                return (pattern != null && pattern.matcher(resource.getName()).find()) ||
                       (matchUnavail    && unavails.containsKey(resource.getId()))     ||
                       (groupId != null && groupMembers.contains(resource))            ||
                       (protoId != null && protoId.equals(resProtoId))                 ||
                       (matchOwn        && resource.getOwner().getId().equals(subjectId));
            }
        };
        return rtn;
    }

    private Set<Resource> getGroupMembers(Integer groupId) {
        Set<Resource> rtn = Collections.emptySet();
        if (groupId != null) {
            final ResourceGroup group = resourceGroupManager.getGroupById(groupId);
            final List<Resource> members = resourceGroupManager.getMembers(group);
            rtn = new HashSet<Resource>(members);
        }
        return rtn;
    }

    private Comparator<AppdefResourceValue> getNameComparator(final int sortOrder) {
        return new Comparator<AppdefResourceValue>() {
            public int compare(AppdefResourceValue o1, AppdefResourceValue o2) {
                if (o1 == o2) {
                    return 0;
                }
                int rtn;
                if (sortOrder == PageControl.SORT_ASC) {
                    rtn = o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                } else {
                    rtn = o2.getName().toLowerCase().compareTo(o1.getName().toLowerCase());
                }
                if (rtn != 0) {
                    return rtn;
                } else {
                    return o1.getId().compareTo(o2.getId());
                }
            }
        };
    }

    private Set<Integer> getSet(int[] groupSubType) {
        Set<Integer> rtn = Collections.emptySet();
        if (groupSubType != null) {
            rtn = new HashSet<Integer>();
            for (int type : groupSubType) {
                rtn.add(type);
            }
        }
        return rtn;
    }

    private ResourceType getResourceType(Set<Integer> groupSubTypeSet, int appdefTypeId) {
        if (groupSubTypeSet != null && !groupSubTypeSet.isEmpty()) {
            return resourceManager.getResourceTypeById(AuthzConstants.authzGroup);
        }
        switch (appdefTypeId) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return resourceManager.getResourceTypeById(AuthzConstants.authzPlatform);
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return resourceManager.getResourceTypeById(AuthzConstants.authzServer);
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return resourceManager.getResourceTypeById(AuthzConstants.authzService);
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return resourceManager.getResourceTypeById(AuthzConstants.authzApplication);
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                return resourceManager.getResourceTypeById(AuthzConstants.authzGroup);
        }
        throw new SystemException("appdefTypeId=" + appdefTypeId + " is invalid");
    }

    /**
     * Perform a search for resources
     * 
     */
    @Transactional(readOnly = true)
    public PageList<SearchResult> search(int sessionId, String searchFor, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        PageList<Resource> resources = resourceManager.findViewables(subject, searchFor, pc);

        List<SearchResult> searchResults = new ArrayList<SearchResult>(resources.size());
        for (Resource res : resources) {
            AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(res);
            searchResults.add(new SearchResult(res.getName(), AppdefEntityConstants
                .typeToString(aeid.getType()), aeid.getAppdefKey()));
        }

        return new PageList<SearchResult>(searchResults, resources.getTotalSize());
    }

    /**
     * Find SERVICE compatible inventory. Specifically, find all viewable
     * services and service clusters. Services that are assigned to clusters are
     * not returned by this method. Value objects returned by this method
     * include ServiceValue and/or AppdefGroupValue. An array of pending
     * AppdefEntityID can also be specified for filtering.
     * 
     * NOTE: This method returns an empty page list when no compatible inventory
     * is found.
     * 
     * @param sessionId - valid auth token
     * @return page list of value objects that extend AppdefResourceValue
     * 
     */
    @Transactional(readOnly = true)
    public PageList<AppdefResourceValue> findAvailableServicesForApplication(
                                                                             int sessionId,
                                                                             Integer appId,
                                                                             AppdefEntityID[] pendingEntities,
                                                                             String nameFilter,
                                                                             PageControl pc)
        throws AppdefEntityNotFoundException, PermissionException, SessionException {

        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        // init our (never-null) page and filter lists
        List<AppdefPagerFilter> filterList = new ArrayList<AppdefPagerFilter>();

        // add a pager filter for removing pending appdef entities
        if (pendingEntities != null) {
            filterList.add(new AppdefPagerFilterExclude(pendingEntities));
        }

        int oriPageSize = pc.getPagesize();
        pc.setPagesize(PageControl.SIZE_UNLIMITED);
        if (debug)
            watch.markTimeBegin("findViewableSvcResources");
        Set<Resource> authzResources = new TreeSet<Resource>(resourceManager
            .findViewableSvcResources(subject, nameFilter, pc));
        if (debug)
            watch.markTimeEnd("findViewableSvcResources");

        int authzResourcesSize = authzResources.size();

        pc.setPagesize(oriPageSize);

        // Remove existing application assigned inventory
        if (debug)
            watch.markTimeBegin("findServiceInventoryByApplication");
        List<AppdefResourceValue> assigned = findServiceInventoryByApplication(sessionId, appId,
            PageControl.PAGE_ALL);
        if (debug)
            watch.markTimeEnd("findServiceInventoryByApplication");
        if (debug)
            watch.markTimeBegin("loop1");

        for (AppdefResourceValue val : assigned) {
            authzResources.remove(resourceManager.findResource(val.getEntityId()));
        }
        if (debug)
            watch.markTimeEnd("loop1");

        List<AppdefEntityID> toBePaged = new ArrayList<AppdefEntityID>(authzResources.size());
        for (Resource r : authzResources) {
            toBePaged.add(AppdefUtil.newAppdefEntityId(r));
        }

        // Page it, then convert to AppdefResourceValue
        List<AppdefResourceValue> finalList = new ArrayList<AppdefResourceValue>(authzResources
            .size());
        // TODO: G
        PageList<AppdefEntityID> pl = getPageList(toBePaged, pc, filterList);
        for (AppdefEntityID ent : pl) {
            try {
                finalList.add(findById(subject, ent));
            } catch (AppdefEntityNotFoundException e) {
                // XXX - hack to ignore the error. This must have occurred when
                // we created the resource, and rolled back the AppdefEntity
                // but not the Resource
                log.error("Invalid entity still in resource table: " + ent, e);
            }
        }

        int pendingSize = (pendingEntities != null) ? pendingEntities.length : 0;
        int adjustedSize = authzResourcesSize - pendingSize;
        PageList<AppdefResourceValue> rtn = new PageList<AppdefResourceValue>(finalList,
            adjustedSize);
        if (debug)
            log.debug(watch);
        return rtn;
    }

    private <T> PageList<T> getPageList(Collection<T> coll, PageControl pc) {
        // TODO: G
        return Pager.getDefaultPager().seek(coll, pc);
    }

    // Page out the collection, applying any filters in the process.
    // TODO: G
    private PageList getPageList(Collection coll, PageControl pc, List filterList) {
        Pager pager;
        AppdefPagerFilter[] filterArr;

        pc = PageControl.initDefaults(pc, SortAttribute.RESTYPE_NAME);

        filterArr = (AppdefPagerFilter[]) filterList.toArray(new AppdefPagerFilter[0]);

        try {
            pager = Pager.getPager(APPDEF_PAGER_PROCESSOR);
        } catch (Exception e) {
            throw new SystemException("Unable to get a pager", e);
        }
        return pager.seekAll(coll, pc.getPagenum(), pc.getPagesize(), filterArr);
    }

    /**
     * Add an appdef entity to a batch of groups.
     * 
     * @param sessionId representing session identifier
     * @param entityId object to be added.
     * @param groupIds identifier array
     * 
     */
    public void batchGroupAdd(int sessionId, AppdefEntityID entityId, Integer[] groupIds)
        throws SessionException, PermissionException, VetoException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        Resource resource = resourceManager.findResource(entityId);
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>(groupIds.length);
        for (int i = 0; i < groupIds.length; i++) {
            ResourceGroup group = resourceGroupManager.findResourceGroupById(subject, groupIds[i]);
            groups.add(group);
        }
        resourceGroupManager.addResource(subject, resource, groups);
    }

    /**
     * Update all the appdef resources owned by this user to be owned by the
     * root user. This is done to prevent resources from being orphaned in the
     * UI due to its display restrictions. This method should only get called
     * before a user is about to be deleted
     * 
     */
    public void resetResourceOwnership(int sessionId, AuthzSubject currentOwner)
        throws UpdateException, PermissionException, AppdefEntityNotFoundException {

        // first look up the appdef resources by owner

        Collection<Resource> resources = resourceManager.findResourceByOwner(currentOwner);
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        for (Resource aRes : resources) {
            AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(aRes);

            if (aeid.isGroup()) {
                ResourceGroup g = resourceGroupManager.findResourceGroupById(overlord, aRes
                    .getInstanceId());
                resourceGroupManager.changeGroupOwner(overlord, g, overlord);
            } else {
                resourceManager.setResourceOwner(overlord, aRes, overlord);
                AppdefEntityValue aev = new AppdefEntityValue(aeid, overlord);
                AppdefResource appdef = aev.getResourcePOJO();
                appdef.setModifiedBy(overlord.getName());
            }
        }
    }

    /**
     * Remove an appdef entity from a batch of groups.
     * @param entityId object to be removed
     * @param groupIds identifier array
     * 
     */
    public void batchGroupRemove(int sessionId, AppdefEntityID entityId, Integer[] groupIds)
        throws PermissionException, SessionException, VetoException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        Resource resource = resourceManager.findResource(entityId);
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>(groupIds.length);

        for (int i = 0; i < groupIds.length; i++) {
            ResourceGroup group = resourceGroupManager.findResourceGroupById(subject, groupIds[i]);
            groups.add(group);
        }
        resourceGroupManager.removeResource(subject, resource, groups);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public AppdefResourcePermissions getResourcePermissions(int sessionId, AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException {
        AuthzSubject who = sessionManager.getSubject(sessionId);
        return permissionManager.getResourcePermissions(who, id);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public int getAgentCount(int sessionId) throws SessionNotFoundException,
        SessionTimeoutException {
        sessionManager.authenticate(sessionId);
        return agentManager.getAgentCount();
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<Agent> findAllAgents(int sessionId) throws SessionNotFoundException,
        SessionTimeoutException {
        sessionManager.authenticate(sessionId);
        return agentManager.getAgents();
    }

    /**
     * Get the value of one agent based on the IP and Port on which the agent is
     * listening
     * 
     */
    @Transactional(readOnly = true)
    public Agent findAgentByIpAndPort(int sessionId, String ip, int port)
        throws SessionNotFoundException, SessionTimeoutException, AgentNotFoundException {
        sessionManager.authenticate(sessionId);
        return agentManager.getAgent(ip, port);
    }

    /**
     * Set (or delete) a custom property for a resource. If the property already
     * exists, it will be overwritten.
     * @param id Appdef entity to set the value for
     * @param key Key to associate the value with
     * @param val Value to assicate with the key. If the value is null, then the
     *        value will simply be removed.
     * 
     */
    public void setCPropValue(int sessionId, AppdefEntityID id, String key, String val)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException, CPropKeyNotFoundException {
        AuthzSubject who = sessionManager.getSubject(sessionId);
        AppdefEntityValue aVal = new AppdefEntityValue(id, who);
        int typeId = aVal.getAppdefResourceType().getId().intValue();
        cPropManager.setValue(id, typeId, key, val);
    }

    /**
     * Get a map which holds the descriptions & their associated values for an
     * appdef entity.
     * @param id Appdef entity to get the custom entities for
     * @return The properties stored for a specific entity ID
     * 
     */
    @Transactional(readOnly = true)
    public Properties getCPropDescEntries(int sessionId, AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        AppdefEntityNotFoundException {
        sessionManager.authenticate(sessionId);
        return cPropManager.getDescEntries(id);
    }

    /**
     * Get all the keys associated with an appdef resource type.
     * @param appdefType One of AppdefEntityConstants.APPDEF_TYPE_*
     * @param appdefTypeId The ID of the appdef resource type
     * @return a List of CPropKeyValue objects
     * 
     */
    @Transactional(readOnly = true)
    public List<CpropKey> getCPropKeys(int sessionId, int appdefType, int appdefTypeId)
        throws SessionNotFoundException, SessionTimeoutException {
        sessionManager.authenticate(sessionId);
        return cPropManager.getKeys(appdefType, appdefTypeId);
    }

    /**
     * Get all the keys associated with an appdef type of a resource.
     * @param aeid The ID of the appdef resource
     * @return a List of CPropKeyValue objects
     * @throws PermissionException
     * @throws AppdefEntityNotFoundException
     * 
     */
    @Transactional(readOnly = true)
    public List<CpropKey> getCPropKeys(int sessionId, AppdefEntityID aeid)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        PermissionException {
        AuthzSubject who = sessionManager.getSubject(sessionId);

        AppdefEntityValue av = new AppdefEntityValue(aeid, who);
        int typeId = av.getAppdefResourceType().getId().intValue();

        return cPropManager.getKeys(aeid.getType(), typeId);
    }

    /**
     * Get the appdef inventory summary visible to a user
     * 
     */
    @Transactional(readOnly = true)
    public AppdefInventorySummary getInventorySummary(int sessionId, boolean countTypes)
        throws SessionNotFoundException, SessionTimeoutException {
        AuthzSubject who = sessionManager.getSubject(sessionId);
        return new AppdefInventorySummary(who, countTypes, permissionManager);
    }

    /**
     * Returns a 2x2 array mapping "appdef type id" to its corresponding label.
     * Suitable for populating an HTML selector.
     * 
     */
    @Transactional(readOnly = true)
    public String[][] getAppdefTypeStrArrMap() {
        int[] validTypes = AppdefEntityConstants.getAppdefTypes();
        String[][] retVal = new String[validTypes.length][2];
        for (int i = 0; i < validTypes.length; i++) {
            retVal[i][0] = Integer.toString(validTypes[i]);
            retVal[i][1] = AppdefEntityConstants.typeToString(validTypes[i]);
        }
        return retVal;
    }

    /**
     * A method to set ALL the configs of a resource. This includes the
     * resourceConfig, metricConfig, rtConfig and controlConfig.This also
     * includes the enabling/disabling of rtMetrics for both service and
     * enduser. NOTE: This method should ONLY be called when a user manually
     * configures a resource.
     * @param allConfigs The full configuation information.
     * @param allConfigsRollback The configuation to rollback to if an error
     *        occurs.
     * 
     */
    public void setAllConfigResponses(int sessionInt, AllConfigResponses allConfigs,
                                      AllConfigResponses allConfigsRollback)
    throws PermissionException, EncodingException, PluginException, ApplicationException,
           AutoinventoryException, ScheduleWillNeverFireException, AgentConnectionException {
        AuthzSubject subject = sessionManager.getSubject(sessionInt);
        setAllConfigResponses(subject, allConfigs, allConfigsRollback);
    }

    public void setAllConfigResponses(AuthzSubject subject, AllConfigResponses allConfigs,
            						  AllConfigResponses allConfigsRollback)
    throws PermissionException, EncodingException, PluginException, ApplicationException,
    	   AutoinventoryException, ScheduleWillNeverFireException, AgentConnectionException {
    	setAllConfigResponses(subject, allConfigs, allConfigsRollback, Boolean.TRUE);
    }

    public void setAllConfigResponses(AuthzSubject subject, AllConfigResponses allConfigs,
                                      AllConfigResponses allConfigsRollback,
                                      Boolean isUserManaged)
    throws PermissionException, EncodingException, PluginException, ApplicationException,
           AutoinventoryException, ScheduleWillNeverFireException, AgentConnectionException {
        boolean doRollback = true;
        boolean doValidation = (allConfigsRollback != null);
        AppdefEntityID id = allConfigs.getResource();

        try {
            doSetAll(subject, allConfigs, doValidation, false, isUserManaged);

            if (doValidation) {
                configManager.clearValidationError(subject, id);
            }

            doRollback = false;

            // run an auto-scan for platforms
            if (id.isPlatform()) {
                // HQ-1259: Use hqadmin as the subject to propagate platform
                // configuration changes to platform services if the user
                // as insufficient permissions
                AuthzSubject aiSubject = subject;
                try {
                    permissionManager.checkAIScanPermission(subject, id);
                } catch (PermissionException pe) {
                    aiSubject = authzSubjectManager.getSubjectById(AuthzConstants.rootSubjectId);
                }
                autoinventoryManager.startScan(aiSubject, id, new ScanConfigurationCore(), null, null, null);
            }
        } catch (InvalidConfigException e) {
            // setValidationError for InventoryHelper.isResourceConfigured
            // so this error will be displayed in the UI
            // configManager.setValidationError(subject, id, e.getMessage());
            throw e;
        } finally {
            if (doRollback && doValidation) {
                doSetAll(subject, allConfigsRollback, false, true, isUserManaged);
            }
        }
    }

    private void doSetAll(AuthzSubject subject, AllConfigResponses allConfigs,
                          boolean doValidation, boolean force,
                          Boolean isUserManaged) throws EncodingException,
        PermissionException, ConfigFetchException, PluginException, ApplicationException {
        AppdefEntityID entityId = allConfigs.getResource();
        Set<AppdefEntityID> ids = new HashSet<AppdefEntityID>();
        ConfigResponseDB existingConfig;
        Service svc = null;
        try {
            existingConfig = configManager.getConfigResponse(entityId);
            ConfigManager.ConfigDiff diff = configManager.configureResponseDiff(subject, existingConfig, entityId,
                ConfigResponse.safeEncode(allConfigs.getProductConfig()), ConfigResponse
                    .safeEncode(allConfigs.getMetricConfig()), ConfigResponse.safeEncode(allConfigs
                    .getControlConfig()), ConfigResponse.safeEncode(allConfigs.getRtConfig()),
                    isUserManaged, force);
            if (diff.isWasUpdated()) {
                ids.add(entityId);
                Resource r = this.resourceManager.findResource(entityId);
                Integer rid = r.getId();
                ResourceContentChangedZevent contentChangedEvent = new ResourceContentChangedZevent(
                        rid,null,diff.getAllConfigDiff(),null, null);
                List<ResourceContentChangedZevent> updateEvents = new ArrayList<ResourceContentChangedZevent>();
                updateEvents.add(contentChangedEvent);
                zEventManager.enqueueEventsAfterCommit(updateEvents);
            }
            if (diff.isWasUpdated() && !doValidation) {
                Resource r = resourceManager.findResource(entityId);
                resourceManager.resourceHierarchyUpdated(subject, Collections.singletonList(r));
            }

            if (doValidation) {
                Set<String> validationTypes = new HashSet<String>();

                if (allConfigs.shouldConfigProduct()) {
                    validationTypes.add(ProductPlugin.TYPE_CONTROL);
                    validationTypes.add(ProductPlugin.TYPE_RESPONSE_TIME);
                    validationTypes.add(ProductPlugin.TYPE_MEASUREMENT);
                }

                if (allConfigs.shouldConfigMetric()) {
                    validationTypes.add(ProductPlugin.TYPE_MEASUREMENT);
                }

                // Need to set the flags on the service so that they
                // can be looked up immediately and RtEnabler to work
                if (svc != null) {
                    // These flags
                    if (allConfigs.getEnableServiceRT() != svc.isServiceRt() ||
                        allConfigs.getEnableEuRT() != svc.isEndUserRt()) {
                        allConfigs.setShouldConfig(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME, true);
                        svc.setServiceRt(allConfigs.getEnableServiceRT());
                        svc.setEndUserRt(allConfigs.getEnableEuRT());
                    }
                }

                if (allConfigs.shouldConfigRt()) {
                    validationTypes.add(ProductPlugin.TYPE_RESPONSE_TIME);
                }

                if (allConfigs.shouldConfigControl()) {
                    validationTypes.add(ProductPlugin.TYPE_CONTROL);
                }

                ConfigValidator configValidator = (ConfigValidator) ProductProperties
                    .getPropertyInstance(ConfigValidator.PDT_PROP);

                // See if we can validate
                if (configValidator != null) {
                    Iterator<String> validations = validationTypes.iterator();
                    AppdefEntityID[] idArr = (AppdefEntityID[]) ids.toArray(new AppdefEntityID[0]);

                    while (validations.hasNext()) {
                        configValidator.validate(subject, validations.next(), idArr);
                    }
                }
            }

            if (allConfigs.shouldConfigProduct() || allConfigs.shouldConfigMetric()) {
                List<Server> servers = new ArrayList<Server>();
                if (entityId.isServer()) {
                    servers.add(serverManager.findServerById(entityId.getId()));
                } else if (entityId.isPlatform()) {
                    // Get the virtual servers
                    Platform plat = platformManager.findPlatformById(entityId.getId());
                    for (Server server : plat.getServers()) {
                        if (server.getServerType().isVirtual()) {
                            servers.add(server);
                        }
                    }
                }

                for (Server server : servers) {
                    // Look up the server's services
                    for (Service service : server.getServices()) {
                        ids.add(service.getEntityId());
                    }
                }
            }

            // if should configure RT
            if (allConfigs.shouldConfigRt())
                ids.add(entityId);

            if (ids.size() > 0) { // Actually updated
                List<ResourceUpdatedZevent> events = new ArrayList<ResourceUpdatedZevent>(ids
                    .size());
                AuthzSubject hqadmin = authzSubjectManager
                    .getSubjectById(AuthzConstants.rootSubjectId);

                for (AppdefEntityID ade : ids) {
                    AuthzSubject eventSubject = subject;

                    // HQ-1259: Use hqadmin as the subject to propagate platform
                    // configuration changes to platform services if the user
                    // as insufficient permissions
                    if (entityId.isPlatform() && !ade.isPlatform()) {
                        try {
                            permissionManager.checkModifyPermission(subject, ade);
                        } catch (PermissionException pe) {
                            eventSubject = hqadmin;
                        }
                    }
                    events.add(new ResourceUpdatedZevent(eventSubject, ade, allConfigs));
                }
                zEventManager.enqueueEventsAfterCommit(events);
            }
            
            

            if (entityId.isServer() || entityId.isService()) {
                aiBoss.toggleRuntimeScan(subject, entityId, allConfigs.getEnableRuntimeAIScan());
            }
        } catch (UpdateException e) {
            log.error("Error while updating resource " + allConfigs.getResource());
            throw new ApplicationException(e);
        }
    }

    /**
     * Get the navigation map data for a given Appdef entity.
     * @return all navigable resources for the given appdef entity
     * 
     */
    @Transactional(readOnly=true)
    public ResourceTreeNode[] getNavMapData(int sessionId, AppdefEntityID adeId)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException,
        AppdefEntityNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        switch (adeId.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                return appdefStatManager.getNavMapDataForPlatform(subject, new Integer(adeId
                    .getID()));
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return appdefStatManager
                    .getNavMapDataForServer(subject, new Integer(adeId.getID()));
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                return appdefStatManager.getNavMapDataForService(subject,
                    new Integer(adeId.getID()));
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                return appdefStatManager.getNavMapDataForApplication(subject, new Integer(adeId
                    .getID()));
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                return appdefStatManager.getNavMapDataForGroup(subject, new Integer(adeId.getID()));
        }
        return new ResourceTreeNode[0];
    }

    /**
     * Get the navigation map data for a an auto-group.
     * @param adeIds the appdef entity ids of the "parents" of the groupd
     *        children
     * @param ctype the child resource type
     * @return all navigable resources for the given appdef entities and child
     *         resource type
     * 
     */
    @Transactional(readOnly=true)
    public ResourceTreeNode[] getNavMapData(int sessionId, AppdefEntityID[] adeIds, int ctype)
    throws SessionNotFoundException, SessionTimeoutException, PermissionException, AppdefEntityNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return appdefStatManager.getNavMapDataForAutoGroup(subject, adeIds, new Integer(ctype));
    }

    /**
     * Get the list of resources that are unavailable
     * 
     */
    @Transactional(readOnly=true)
    public Collection<DownResource> getUnavailableResources(AuthzSubject user, String typeId, PageInfo info)
    throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException, PermissionException {
        List<DownMetricValue> unavailEnts = availabilityManager.getUnavailEntities(null);

        if (unavailEnts.size() == 0) {
            return new ArrayList<DownResource>(0);
        }

        DownResSortField sortField = (DownResSortField) info.getSort();
        Set<DownResource> ret = new TreeSet<DownResource>(sortField.getComparator(!info.isAscending()));

        int appdefType = -1;
        int appdefTypeId = -1;

        if (typeId != null && typeId.length() > 0) {
            try {
                appdefType = Integer.parseInt(typeId);
            } catch (NumberFormatException e) {
                AppdefEntityTypeID aetid = new AppdefEntityTypeID(typeId);
                appdefType = aetid.getType();
                appdefTypeId = aetid.getID();
            }
        }

        Set<AppdefEntityID> viewables = new HashSet<AppdefEntityID>(findViewableEntityIds(user,
            APPDEF_TYPE_UNDEFINED, null, null, null));
        for (DownMetricValue dmv : unavailEnts) {
            AppdefEntityID entityId = dmv.getEntityId();
            if (!viewables.contains(entityId)) {
                continue;
            }

            AppdefEntityValue res = new AppdefEntityValue(entityId, user);

            // Look up the resource type
            if (appdefType != -1) {
                if (entityId.getType() != appdefType) {
                    continue;
                }
                if (appdefTypeId != -1) {
                    AppdefResourceType type = res.getAppdefResourceType();
                    if (type.getId().intValue() != appdefTypeId) {
                        continue;
                    }
                }
            }

            if (log.isDebugEnabled()) {
                log.debug(res.getName() + " down for " + (dmv.getDuration() / 60000) + "min");
            }

            ret.add(new DownResource(res.getResourcePOJO(), dmv));
        }

        if (!info.isAll() && ret.size() > info.getPageSize()) {
            // Have to reduce the size
            List<DownResource> reduced = new ArrayList<DownResource>(ret);
            return reduced.subList(0, info.getPageSize() - 1);
        }
        return ret;
    }

    /**
     * Get the map of unavailable resource counts by type
     * 
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly=true)
    public Map<String, List<AppdefResourceType>> getUnavailableResourcesCount(AuthzSubject user)
        throws AppdefEntityNotFoundException, PermissionException {
        // Keys for the Map table, UI should localize instead of showing key
        // values directly
        final String PLATFORMS = "Platforms";
        final String SERVERS = "Servers";
        final String SERVICES = "Services";

        List<DownMetricValue> unavailEnts = availabilityManager.getUnavailEntities(null);
        Map<String, List<AppdefResourceType>> ret = new LinkedHashMap<String, List<AppdefResourceType>>();
        ret.put(PLATFORMS, new ArrayList<AppdefResourceType>());
        ret.put(SERVERS, new ArrayList<AppdefResourceType>());
        ret.put(SERVICES, new ArrayList<AppdefResourceType>());

        if (unavailEnts.size() == 0) {
            return ret;
        }

        Set<AppdefEntityID> viewables = new HashSet<AppdefEntityID>(findViewableEntityIds(user,
            APPDEF_TYPE_UNDEFINED, null, null, null));
        for (DownMetricValue dmv : unavailEnts) {

            AppdefEntityID aeid = dmv.getEntityId();

            if (!viewables.contains(aeid)) {
                continue;
            }

            List<AppdefResourceType> list;

            if (aeid.isPlatform()) {
                list = ret.get(PLATFORMS);
            } else if (aeid.isServer()) {
                list = ret.get(SERVERS);
            } else if (aeid.isService()) {
                list = ret.get(SERVICES);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Can't handle appdef type: " + aeid.getType());
                }
                continue;
            }

            AppdefEntityValue aev = new AppdefEntityValue(aeid, user);
            list.add(aev.getAppdefResourceType());
        }

        // Now sort each of the lists
        for (List<AppdefResourceType> list : ret.values()) {
            Collections.sort(list);
        }
        return ret;
    }

    /**
     * Check whether or not a given resource exists in the virtual hierarchy
     * 
     */
    @Transactional(readOnly=true)
    public boolean hasVirtualResourceRelation(Resource resource) {
        return resourceManager.hasResourceRelation(resource, resourceManager.getVirtualRelation());
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
