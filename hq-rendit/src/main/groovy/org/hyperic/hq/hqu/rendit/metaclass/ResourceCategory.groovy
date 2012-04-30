/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
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

package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.authz.server.session.ResourceGroupSortField;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.appdef.Agent
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.AppdefEntityValue
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.IpValue
import org.hyperic.hq.appdef.shared.PlatformValue
import org.hyperic.hq.appdef.server.session.Platform
import org.hyperic.hq.appdef.server.session.Server
import org.hyperic.hq.appdef.server.session.Service
import org.hyperic.hq.appdef.shared.ServerValue
import org.hyperic.hq.common.VetoException
import org.hyperic.hq.events.MaintenanceEvent
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.events.shared.MaintenanceEventManager
import org.hyperic.hq.product.PluginNotFoundException

import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.livedata.shared.LiveDataManager;
import org.hyperic.hq.livedata.shared.LiveDataResult
import org.hyperic.util.config.ConfigResponse
import org.hyperic.util.pager.PageControl
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.hqu.rendit.util.ResourceConfig
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException

/**
 * This class provides tonnes of abstractions over the Appdef layer.
 *
 * It makes Appdef Platform/Server/Services:
 *   -- work in a hierarchy (getChildren())
 *   
 * It also takes advantage of the 'Resource' objects which act as prototypes
 * (XXX:  Would be good to make a real subclass of Resource into ResourcePrototype)
 */

class ResourceCategory {
    private static liveDataMan = Bootstrap.getBean(LiveDataManager.class)
    private static platMan  = Bootstrap.getBean(PlatformManager.class)
    private static svcMan   = Bootstrap.getBean(ServiceManager.class)
    private static svrMan   = Bootstrap.getBean(ServerManager.class)
    private static dman     = Bootstrap.getBean(MeasurementManager.class)
    private static authzMan = Bootstrap.getBean(AuthzSubjectManager.class)
    private static groupMan = Bootstrap.getBean(ResourceGroupManager.class)
    private static defMan   = Bootstrap.getBean(AlertDefinitionManager.class)
    private static alertMan = Bootstrap.getBean(AlertManager.class)
    private static eventMan = Bootstrap.getBean(EventLogManager.class)
    private static cMan     = Bootstrap.getBean(ControlManager.class)
    private static csMan    = Bootstrap.getBean(ControlScheduleManager.class);
    private static resMan   = Bootstrap.getBean(ResourceManager.class)

    private static MaintenanceEventManager maintMan =
        PermissionManagerFactory.getInstance().getMaintenanceEventManager();

    /**
     * Creates a URL for the resource.  This should typically only be called
     * via HtmlUtil.linkTo (or from a controller).  
     *
     * Resources have the following contexts:
     *  'alert' : Returns a link to the resource's alert definitions
     *  'currentHealth' : Returns a link to the resource's default page with pre-set metric settings
     *  - Otherwise, simply returns a link to the resource's default page.
     */
    static String urlFor(Resource r, String context) {
        if (context == 'alert') {
            return "/alerts/Config.do?mode=list&eid=${r.entityId}"
        } else if (context == 'currentHealth') {
            return "/ResourceCurrentHealth.do?eid=${r.entityId}"
        }
        return "/Resource.do?eid=${r.entityId}"
    }
    
    /**
     * Get the appdef type (1 = platform, 2=server, etc.) of a resource
     * which is a prototype.
     */
    static getAppdefType(Resource r) {
        def typeId = r.resourceType.id
        
        if (typeId == AuthzConstants.authzPlatformProto) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM
        } else if (typeId == AuthzConstants.authzServerProto) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER
        } else if (typeId == AuthzConstants.authzServiceProto) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE
        } else {
            throw new RuntimeException("Resource [${r}] is not an appdef " + 
            "resource type")
        }
    }
    
    static AppdefEntityID getEntityId(Resource r) {
        if (r.resourceType == null) { // Possible that resource has been deleted
            // Assume it's a platform
            return AppdefEntityID.newPlatformID(r.instanceId)
        }
        
        def typeId = r.resourceType.id
        
        if (typeId == AuthzConstants.authzPlatform) {
            return AppdefEntityID.newPlatformID(r.instanceId)
        } else if (typeId == AuthzConstants.authzServer) {
            return AppdefEntityID.newServerID(r.instanceId)
        } else if (typeId == AuthzConstants.authzService) {
            return AppdefEntityID.newServiceID(r.instanceId)
        } else if (typeId == AuthzConstants.authzApplication) {
            return AppdefEntityID.newAppID(r.instanceId)
        } else if (typeId == AuthzConstants.authzGroup) {
            return AppdefEntityID.newGroupID(r.instanceId)
        } else {
            throw new RuntimeException("Resource [${r}] is not an appdef object.  " + 
            "typeId=${typeId}")
        }
    }
    
    static String urlFor(AppdefEntityID aeid, String context) {
        return "/Resource.do?eid=${aeid}"
    }
    
    static boolean getSupportsMonitoring(Resource r) {
        def ent = r.entityId
        ent.isPlatform() || ent.isServer() || ent.isService()
    }
    
    static Collection getDesignatedMetrics(Resource r) {
        dman.findDesignatedMeasurements(r.entityId)
    }
    
    static Collection getEnabledMetrics(Resource r) {
        dman.findEnabledMeasurements(null, r.entityId, null)
    }
    
    /**
     * Get all metrics for a resource
     */
    static Collection getMetrics(Resource r) {
        dman.findMeasurements(null, r)
    }
    
    /**
     * Get the metrics summary for a resource
     */
    static Map getMetricsSummary(Resource r, AuthzSubject user, long begin, long end) {
       def mgr = SessionManager.instance
       def sessionId = mgr.put(user)
       def aeids = [AppdefUtil.newAppdefEntityId(r)] as AppdefEntityID[]
       return Bootstrap.getBean(MeasurementBoss.class).findMetrics(sessionId,
           aeids,
           MeasurementConstants.FILTER_NONE,
           null,
           begin,
           end,
           false)          
   }
    
    /**
     * Get the availability Measurement for a Resource.
     */
    static getAvailabilityMeasurement(Resource r) {
        dman.getAvailabilityMeasurement(r)
    }
    
    static List getAlertDefinitions(Resource r, AuthzSubject user) {
        def alertDefs
        if (r.isPlatform() || r.isServer() || r.isService()) {
            // Individual alert definition
            alertDefs = defMan.findAlertDefinitions(user, r.entityId)
        } else {
            // Resource type alert definition
            alertDefs = defMan.findAlertDefinitions(user, r)
        }
        alertDefs.findAll { !it.deleted }
    }
    
    /**
     * Get the alerts for a Resource
     */    
    static List getAlerts(Resource r, AuthzSubject user, long begin, long end,
    int count, AlertSeverity priority) {
        def includes = [r.entityId]
        alertMan.findAlerts(user, count, priority.code,
                end - begin, end, includes)
    }
    
    /**
     * Get the unfixed alert count for a Resouce
     */
    static int getUnfixedAlertCount(Resource r, AuthzSubject user, long timeRange, long end) {
        //def includes = [r.entityId]
        //alertMan.findAlerts(user, false, 0, end - timeRange, end, includes)
       alertMan.getUnfixedCount(user.id, timeRange, end, r) ;
    }
    
    /**
     * Get the event logs for a Resource
     */
    static List getLogs(Resource r, AuthzSubject user, long begin, long end) {
        eventMan.findLogs(r.entityId, user, (String[])null, begin, end)
    }
    
    /**
     * Get the control actions for a Resource
     * @throws PermissionException If the user does not have sufficient permissions
     */
    static List getControlActions(Resource r, AuthzSubject user) {
        try {
            return cMan.getActions(user, r.entityId)
        } catch (PluginNotFoundException e) {
            return []
        }
    }
    
    /**
     * Get the control history for a Resource
     * @throws PermissionException If the user does not have sufficient permissions
     */
    static List getControlHistory(Resource r, AuthzSubject user) {
        return csMan.findJobHistory(user, r.entityId, PageControl.PAGE_ALL)
    }
    
    /**
     * Run a control action on a Resource
     * @throws PermissionException If the user does not have permission to
     *         execute the action
     * @throws PluginException If the resource does not support control.
     */
    static void runAction(Resource r, AuthzSubject user, String action,
    String arguments) {
        if (r.entityId.isGroup()) {
            cMan.doGroupAction(user, r.entityId, action, arguments, null)
        } else {
            cMan.doAction(user, r.entityId, action, arguments)
        }
    }
    
    static boolean isGroup(Resource r) {
        return r.resourceType.id == AuthzConstants.authzGroup
    }
    
    static Collection getGroupMembers(Resource r, AuthzSubject user) {
        if (!r.isGroup()) {
            return Collections.EMPTY_LIST
        }
        
        groupMan.findResourceGroupById(user, r.instanceId).resources
    }
    
    /**
     * Get a collection of {@link String}s, depicting the LiveData
     * commands available to the specified resource for the specified user
     */
    static Collection getLiveDataCommands(Resource r, AuthzSubject user) {
        try {
            return liveDataMan.getCommands(user, r.entityId) as List
        } catch (PluginNotFoundException e) {
            return []
        }
    }
    
    static LiveDataResult getLiveData(Resource r, AuthzSubject user, 
    String cmd, ConfigResponse cfg) {
        def lcmd = new LiveDataCommand(r.entityId, cmd, cfg)
        liveDataMan.getData(user, lcmd)
    }
    
    static List getLiveData(Collection resources, AuthzSubject user,
    String cmd, ConfigResponse cfg) {
        def cmds = []
        for (r in resources) {
            cmds << new LiveDataCommand(r.entityId, cmd, cfg)
        }
        liveDataMan.getData(user, cmds as LiveDataCommand[]) as List
    }
    
    static boolean isPlatform(Resource r) {
        r.resourceType.id == AuthzConstants.authzPlatform
    }
    
    static boolean isServer(Resource r) {
        r.resourceType.id == AuthzConstants.authzServer
    }
    
    static boolean isService(Resource r) {
        r.resourceType.id == AuthzConstants.authzService
    }

    static Platform toPlatform(Resource r) {
        assert isPlatform(r)
        platMan.findPlatformById(r.instanceId)
    }

    static Server toServer(Resource r) {
        assert isServer(r)
        svrMan.findServerById(r.instanceId)
    }

    static Service toService(Resource r) {
        assert isService(r)
        svcMan.findServiceById(r.instanceId)
    }
    
    static ResourceGroup toGroup(Resource r) {
        assert isGroup(r)
        groupMan.findResourceGroupById(authzMan.overlordPojo, r.instanceId)
    }
    
    static boolean isVirtual(Resource t) {
        if (t.resourceType.id == AuthzConstants.authzServerProto) {
            return svrMan.findServerType(t.instanceId)?.isVirtual() == true
        }
        false
    }
    
    /**
     * Get the description for this Resource.  If no description exists (i.e.
     * this is not a Platform, Server, or Service) an empty string is returned.
     */
    static String getDescription(Resource r) {
        def description = null;
        if (isPlatform(r)) {
            description = toPlatform(r).getDescription()
        } else if (isServer(r)) {
            description = toServer(r).getDescription()
        } else if (isService(r)) {
            description = toService(r).getDescription()
        }
        return description == null ? "" : description
    }

    /**
     * @see documentation for ResourceConfig.  We don't return it directly
     * here, as we'd like to abstract the thing doing the persisting
     */
    static Map getConfig(Resource r) {
        def cfg = new ResourceConfig(r)
        cfg.populate()
        cfg.entries
    }
    
    /**
     * @see ResourceConfig
     */
    static void setConfig(Resource r, Map m, AuthzSubject subject) {
        (new ResourceConfig(r)).setProperties(m, subject)
    }

    /**
     * @see ResourceConfig
     */
    static void setConfig(Resource r, Map m, AuthzSubject subject, Boolean isUserManaged) {
        (new ResourceConfig(r)).setProperties(m, subject, isUserManaged)
    }
        
    /**
     * Get all the children of a resource, viewable by the passed user.
     *
     * @return a list of {@link Resource}s
     */
    static Collection getViewableChildren(Resource r, AuthzSubject user) {
        resMan.findChildren(user, r)
    }
    
    /**
     * Convoluted way to get the children from the root resource.
     *
     * @deprecated
     */
    static List getChildren(Resource r, Map args) {
        if (r.isRoot()) {
            // Need subsystem argument
            assert args.inSubsystem, "Must specify an 'inSubsystem' argument " +
            "[like 'appdef']"
            if (args.inSubsystem == 'appdef') {
                def overlord = authzMan.overlordPojo
                def rhelp = new ResourceHelper(overlord)
                return rhelp.findAllPlatforms()
            } else { 
                throw new IllegalArgumentException("Unknown subsystem, " + 
                "[${args.inSubsystem}]")
            }
        }
        []
    }
    
    static boolean isPlatformPrototype(Resource r) {
        return r.resourceType.id == AuthzConstants.authzPlatformProto
    }
    
    static boolean isServerPrototype(Resource r) {
        return r.resourceType.id == AuthzConstants.authzServerProto
    }
    
    static boolean isServicePrototype(Resource r) {
        return r.resourceType.id == AuthzConstants.authzServiceProto
    }
    
    static boolean isVSpherePlatformPrototype(Resource r) {
          return (r.name == AuthzConstants.platformPrototypeVmwareVsphereVm
               || r.name == AuthzConstants.platformPrototypeVmwareVsphereHost)
    }
    
    static Resource getPlatform(Resource r) {
        def aeid = AppdefUtil.newAppdefEntityId(r)
        def aeval = new AppdefEntityValue(aeid, authzMan.overlordPojo)
        def plats = aeval.getAssociatedPlatforms(PageControl.PAGE_ALL);
        def plat = plats[0]
        return resMan.findResource(plat.entityId)
    }
    
    /**
     * Get the children of a resource, of a specific type.  Pass in the 
     * root resource to get root-level appdef objects.
     *
     * "Give me the 'FileServer File' children of my 'Travistation.local' "
     * 
     * @return a list of {@link Resource}s
     */
    static List getChildrenByPrototype(Resource r, Resource proto) {
        if (r.isRoot()) {
            if (!proto.isPlatformPrototype()) {
                // The only supported children of root is the platform, 
                // but this could be expanded to also return things like 
                // users, alert defs or any other resource types
                return []
            }
            def typeRsrc = platMan.findPlatformType(proto.instanceId).resource
            def rhelp    = new ResourceHelper(getOverlord())
            return rhelp.find(byPrototype:typeRsrc)
        }
        
        if (isPlatform(r)) {
            Platform p = toPlatform(r)
            if (proto.isServicePrototype()) {
                def svcType = svcMan.findServiceType(proto.instanceId)
                return svcMan.findPlatformServicesByType(p, svcType).resource
            } else if (proto.isServerPrototype()) {
                def svrType = svrMan.findServerType(proto.instanceId)
                return svrMan.findServersByType(p, svrType).resource
            } else {
                // Else prototype is not a valid proto for the resource 
                return []
            }
        } else if (isServer(r)) {
            Server s = toServer(r)
            def svcType = svcMan.findServiceType(proto.instanceId)
            return svcMan.findServicesByType(s, svcType).resource
        }
        
        []
    }
    
    static List getGroupsContaining(Resource r, AuthzSubject user) {
        PageInfo pInfo = PageInfo.create(PageControl.PAGE_ALL,ResourceGroupSortField.NAME)
        return groupMan.findGroupsContaining(user, r,Collections.EMPTY_LIST,pInfo)
    }
                 
    static List getGroupsNotContaining(Resource r, AuthzSubject user) {         
        PageInfo pInfo = PageInfo.create(PageControl.PAGE_ALL,ResourceGroupSortField.NAME)
        return groupMan.findGroupsNotContaining(user, r, r.getPrototype(),Collections.EMPTY_LIST, pInfo)
    }
    
    static createInstance(Resource proto, Resource parent, String name,
    AuthzSubject subject, Map cfg, Agent agent, List ips) {
        if (!proto.isPlatformPrototype()) {
            throw new RuntimeException("createInstance called for non-platform " +
            "prototype, when platproto was " + 
            "expected")
        }
        
        if (!parent.isRoot())
            throw new RuntimeException("Platforms can only be created as " + 
            "children of root")
        
        cfg = cfg + [:]
        
        def typeRsrc = platMan.findPlatformType(proto.instanceId)
        def platVal = new PlatformValue()
        ['fqdn'].each { 
            if (!cfg[it]) 
                throw new Exception("Must specify [${it}] when creating a " +
                "platform")
        }
        
        platVal.name     = name
        platVal.description = cfg.description
        platVal.fqdn     = cfg.fqdn
        platVal.cpuCount = 1  // XXX:  How can we better gauge?
        platVal.location = cfg.location

        for (ip in ips) {
            def ipVal = new IpValue()
            ipVal.setAddress(ip.address)
            ipVal.setNetmask(ip.netmask)
            ipVal.setMACAddress(ip.mac)
            platVal.addIpValue(ipVal)
        }
        
        if (platVal.getIpValues().length == 0 && cfg.ip != null) {
            def ipVal = new IpValue()
            ipVal.setAddress(cfg.ip)            
            platVal.addIpValue(ipVal)
        }
                
        def plat  = platMan.createPlatform(subject, proto.instanceId,
                platVal, agent.id)
        
        def res = plat.resource
        setConfig(res, cfg, subject)
        return res
    }
    
    
    /**
     * Create a new instance of this prototype:
     */
    static createInstance(Resource proto, Resource parent, 
    String name, AuthzSubject subject, Map cfg) {
        cfg = cfg + [:]  // Clone to avoid modifying someone else's cfg
        
        if (proto.isServicePrototype()) {
            def serviceType = svcMan.findServiceType(proto.instanceId)
            def serverType  = serviceType.serverType
            
            Server server
            if (serverType.isVirtual() && parent.isPlatform()) {
                // Parent points at the 'resource' version of the Platform, so
                // we use the instanceId here, not the Resource.id
                def servers = svrMan.getServersByPlatformServiceType(subject,
                        parent.instanceId,
                        proto.instanceId)
                assert servers.size() == 1, "Cannot create any platform services of " + proto.name + " for " + parent.name + 
                                                          ".  This is because the virtual server which relates " +
                                                          parent.name + " to the service does not exist in the database." +
                                                          "  To find out which virtual server is missing, find the " +
                                                          "plugin where the platform service exists and get the server " +
                                                          "that it should belong to.  From there either remove the agent's datadir " +
                                                          "and re-initialize it or contact support for further options." +
                                                          "  (instanceId=" + parent.instanceId + ")"
                
                server = svrMan.findServerById(servers[0].id) // value -> pojo
            } else if (parent.isServer()) {
                // Normal case, create service on a server
                server = toServer(parent)
            } else {
                // Invalid parameters
                throw new IllegalArgumentException("Invalid prototypes passed to " +
                "createInstance, cannot create " +
                proto.name + " on " + parent.name)
            }
            
            if (!serverType.equals(server.getServerType())) {
                throw new IllegalArgumentException("Cannot create resources of" +
                " type " + serviceType.name +
                " on " + server.getServerType().name)
            }
            
            def res = svcMan.createService(subject, server,  serviceType, name,
                    "", "", null).resource
            setConfig(res, cfg, subject)
            return res
        } else if (proto.isServerPrototype() && parent.isPlatform()) {
            Platform platform = toPlatform(parent)
            def serverType = svrMan.findServerType(proto.instanceId)
            def platformTypes = serverType.getPlatformTypes()
            ServerValue sv = new ServerValue()
            sv.name        = name
            
            if (!platformTypes.contains(platform.getPlatformType())) {
                throw new IllegalArgumentException("Cannot create resources of " +
                "type " + serverType.name +
                " on " + platform.getPlatformType().name)
            }
            
            if (cfg['installPath']) {
                sv.installPath = cfg['installPath']
            } else {
                sv.installPath = ""
            }
            
            if (cfg['description']) {
                sv.description = cfg['description']
            } else {
                sv.description = ""
            }
            
            if (cfg['autoIdentifier']) {
                sv.autoinventoryIdentifier = cfg['autoIdentifier']
            }
            
            def res = svrMan.createServer(subject, platform.id,
                    proto.instanceId, sv).resource
            setConfig(res, cfg, subject)
            return res
        } else {
            throw new IllegalArgumentException("Cannot create resources of type " +
            proto.name + " on resource " +
            parent.name)
        }
    }
    
    /**
     * @deprecated Use ResourceHelper.createGroup
     */
    static ResourceGroup createGroup(Resource r, AuthzSubject user, String name,
    String description, String location) {
        def groupType
        
        if (r.isGroup()) {
            if (r.name == AuthzConstants.rootResourceGroupName) {
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS
            }
            else if (r.name == AuthzConstants.groupResourceTypeName) {
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP
            }
            r = null
        }
        else if (r.id == AuthzConstants.authzApplicationProto) {
            groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP
            r = null
        }
        else {
            switch (r.appdefType) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC
                    break
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
                    break
                default:
                    throw new IllegalArgumentException("Invalid group compatibility"
                    + " type specified")
            }
        }
        
        ResourceGroupCreateInfo cInfo = 
                new ResourceGroupCreateInfo(name, description,
                groupType,
                r,      
                location,
                0,         // clusterId 
                false,     // sytstem
                false)     // private
        
        // No roles or resources
        groupMan.createResourceGroup(user, cInfo, [], [])
    }
    
    static void remove(Resource r, AuthzSubject user) {
        def boss = Bootstrap.getBean(AppdefBoss.class)
        def mgr = SessionManager.instance
        def sessionId = mgr.put(user)
        boss.removeAppdefEntity(sessionId, r.entityId)
    }
    
    /**
     * Move a Resource.
     */
    static void moveTo(Resource target, AuthzSubject user, Resource destination) {
        
        if (target.isService() && destination.isServer()) {
            // Normal service move
            svcMan.moveService(user, target.toService(), destination.toServer())
        } else if (target.isService() && destination.isPlatform()) {
            // Platform service move
            svcMan.moveService(user, target.toService(), destination.toPlatform())
        } else if (target.isServer() && destination.isPlatform()) {
            // Server move
            svrMan.moveServer(user, target.toServer(), destination.toPlatform())
        } else {
            // TODO: This matches incompatible type exception thrown from
            // the manager layer.  Should investigate what is thrown here, since
            // it is important to handle this exception gracefully from the
            // client.
            throw new VetoException("Not implemented: " +
            " target=" + target.getResourceType().getName() +
            " dest=" + destination.getResourceType().getName())
        }
    }

    static MaintenanceEvent scheduleMaintenance(Resource r, AuthzSubject subject,
                                                long start, long end) {
        MaintenanceEvent e = new MaintenanceEvent(r.entityId);
        e.setStartTime(start)
        e.setEndTime(end)
        maintMan.schedule(subject, e)
    }

    static void unscheduleMaintenance(Resource r, AuthzSubject subject) {
        MaintenanceEvent e = new MaintenanceEvent(r.entityId);
        maintMan.unschedule(subject, e)
    }

    static MaintenanceEvent getMaintenanceEvent(Resource r,
                                                AuthzSubject subject) {
        maintMan.getMaintenanceEvent(subject, r.entityId)
    }
        
    private static getOverlord() {
        return authzMan.overlordPojo
    }
}
