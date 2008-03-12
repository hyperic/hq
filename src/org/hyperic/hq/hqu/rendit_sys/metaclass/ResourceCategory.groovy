package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as AuthzMan
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceType
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as GroupMan
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.appdef.Agent
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.appdef.server.session.Platform
import org.hyperic.hq.appdef.shared.PlatformValue
import org.hyperic.hq.appdef.server.session.PlatformType
import org.hyperic.hq.appdef.server.session.Server
import org.hyperic.hq.appdef.server.session.Service
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PlatMan
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl as ServerMan
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl as ServiceMan
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl as DMan
import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl
import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.hq.livedata.shared.LiveDataResult
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.hqu.rendit.util.ResourceConfig
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper

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
    private static platMan  = PlatMan.one
    private static svcMan   = ServiceMan.one
    private static svrMan   = ServerMan.one 
    private static dman     = DMan.one
    private static authzMan = AuthzMan.one
    private static groupMan = GroupMan.one
    
    /**
     * Creates a URL for the resource.  This should typically only be called
     * via HtmlUtil.linkTo (or from a controller).  
     *
     * Resources have the following contexts:
     *  'alert' : Returns a link to the resource's alert definitions
     *  - Otherwise, simply returns a link to the resource's default page.
     */
    static String urlFor(Resource r, String context) {
        if (context == 'alert') {
            return "/alerts/Config.do?mode=list&eid=${r.entityID}"
        } 
        return "/Resource.do?eid=${r.entityID}"
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
    
    static AppdefEntityID getEntityID(Resource r) {
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
        def ent = getEntityID(r)
        ent.isPlatform() || ent.isServer() || ent.isService()
    }

    static Collection getDesignatedMetrics(Resource r) {
		dman.findDesignatedMeasurements(r.entityID)
    }

    static Collection getEnabledMetrics(Resource r) {
        dman.findEnabledMeasurements(null, r.entityID, null)
    }

    static boolean isGroup(Resource r) {
        return r.resourceType.id == AuthzConstants.authzGroup
    }
    
    static Collection getGroupMembers(Resource r, AuthzSubject user) {
        if (!r.isGroup()) {
            return Collections.EMPTY_LIST
        }
        
        groupMan.findResourceGroupById(user.authzSubjectValue,
                                       r.instanceId).resources
    }
    
    /**
     * Get a collection of {@link String}s, depicting the LiveData
     * commands available to the specified resource for the specified user
     */
    static Collection getLiveDataCommands(Resource r, AuthzSubject user) {
        LiveDataManagerEJBImpl.one.getCommands(user, r.entityID) as List
    }

    static LiveDataResult getLiveData(Resource r, AuthzSubject user, 
                                      String cmd, ConfigResponse cfg)  
    {
        def lcmd = new LiveDataCommand(r.entityID, cmd, cfg)
        LiveDataManagerEJBImpl.one.getData(user, lcmd)
    }
    
    static List getLiveData(Collection resources, AuthzSubject user,
                            String cmd, ConfigResponse cfg)
    {
        def cmds = []
        for (r in resources) {
            cmds << new LiveDataCommand(r.entityID, cmd, cfg)
        }
        LiveDataManagerEJBImpl.one.getData(user, cmds as LiveDataCommand[]) as List
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

    static boolean isVirtual(Resource t) {
        if (t.resourceType.id == AuthzConstants.authzServerProto) {
            return svrMan.findServerType(t.instanceId)?.isVirtual() == true
        }
        false
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
     * Get all the children of a resource, viewable by the passed user.
     *
     * @return a list of {@link Resource}s
     */
    static Collection getViewableChildren(Resource r, AuthzSubject user) {
        def res = []
        if (isPlatform(r)) {
            def plat    = toPlatform(r)
            def servers = plat.servers.grep { 
                it.checkPerms(operation: 'view', user:user)
            }
                                                            
            res.addAll(servers*.resource)
            res.addAll(svcMan.getPlatformServices(user.valueObject, 
                                                  r.instanceId)*.resource)
        } else if (isServer(r)) {
            def svr = toServer(r)
            def services = svr.services.grep { 
                it.checkPerms(operation: 'view', user:user)
            }
            
            res.addAll(services*.resource)
        }
        res
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
        }
        
        
        []
    }

    static createInstance(Resource proto, Resource parent, String name,
                          AuthzSubject subject, Map cfg, Agent agent, List ips)
    {
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
        platVal.fqdn     = cfg.fqdn
        platVal.cpuCount = 1  // XXX:  How can we better gauge?

        def plat  = platMan.createPlatform(subject.valueObject, proto.instanceId,
                                           platVal, agent.id)
        for (ip in ips) {
            platMan.addIp(plat, ip.address, ip.netmask, ip.mac)
        }
              
        def res = plat.resource
        setConfig(res, cfg, subject)
        return res
    }
     

    /**
     * Create a new instance of this prototype:
     */
    static createInstance(Resource proto, Resource parent, 
                          String name, AuthzSubject subject, Map cfg)
    {
        cfg = cfg + [:]  // Clone to avoid modifying someone else's cfg
        def subjectVal = subject.valueObject
        
        if (proto.isServicePrototype()) {
            def serviceType = svcMan.findServiceType(proto.instanceId)
            def serverType  = serviceType.serverType
            
            if (serverType.isVirtual()) {
                // Parent points at the 'resource' version of the Platform, so
                // we use the instanceId here, not the Resource.id
                def servers = svrMan.getServersByPlatformServiceType(subjectVal,
                                                                     parent.instanceId,
                                                                     proto.instanceId)
                assert servers.size() == 1, "All virtual servers should be created for Platform ${parent.name}" 
                
                def server = svrMan.findServerById(servers[0].id) // value -> pojo
                def res = svcMan.createService(subjectVal, server, 
                                               serviceType, name, "desc: ${name}",
                                               "loc: ${name}", null).resource
                	setConfig(res, cfg, subject)
                return res
            }
            return 'non-virtual server'
        }
        return 'falout'
    }

    private static getOverlord() {
        return authzMan.overlordPojo
    }
}
