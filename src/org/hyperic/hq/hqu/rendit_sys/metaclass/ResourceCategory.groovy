package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl
import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl
import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.hq.livedata.shared.LiveDataResult
import org.hyperic.util.config.ConfigResponse


class ResourceCategory {
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
            throw RuntimeException("Resource [${r}] is not an appdef object")
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
        def aeid = getEntityID(r)
		DerivedMeasurementManagerEJBImpl.one.findDesignatedMeasurements(aeid)
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
}
