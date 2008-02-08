package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceType
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as rgmi
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.appdef.server.session.Platform
import org.hyperic.hq.appdef.server.session.Server
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl as PlatMan
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl as ServerMan
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl as ServiceMan
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl
import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl
import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.hq.livedata.shared.LiveDataResult
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.hqu.rendit.util.ResourceConfig


class ResourceCategory {
    private static platMan = PlatMan.one
    private static svcMan  = ServiceMan.one
    private static svrMan  = ServerMan.one 
    private static dman    = DerivedMeasurementManagerEJBImpl.one
    
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
        } else if (typeId == AuthzConstants.authzGroup) {
            return AppdefEntityID.newGroupID(r.instanceId)
        } else {
            throw new RuntimeException("Resource [${r}] is not an appdef object")
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
        
        rgmi.one.findResourceGroupById(user.authzSubjectValue,
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
    
    private static boolean isServer(Resource r) {
        r.resourceType.id == AuthzConstants.authzServer
    }

    static Platform toPlatform(Resource r) {
        assert isPlatform(r)
        platMan.findPlatformById(r.instanceId)
    }
    
    private static Server toServer(Resource r) {
        assert isServer(r)
        svrMan.findServerById(r.instanceId)
    }

    static boolean isVirtual(Resource t) {
        if (t.resourceType.id == AuthzConstants.authzServerProto) {
            return svrMan.findServerType(t.instanceId)?.isVirtual() == true
        }
        false
    }
    
    static ResourceConfig getConfig(Resource r) {
        new ResourceConfig(r)
    }
    
    /**
     * Get all the children of a resource, viewable by the passed user.
     */
    static Collection viewableChildren(Resource r, AuthzSubject user) {
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
}
