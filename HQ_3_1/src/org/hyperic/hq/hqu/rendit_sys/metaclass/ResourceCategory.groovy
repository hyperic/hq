package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.shared.AuthzConstants
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl

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
    
    static boolean getSupportsMonitoring(Resource r) {
        def ent = getEntityID(r)
        ent.isPlatform() || ent.isServer() || ent.isService()
    }
    
    static Collection getDesignatedMetrics(Resource r) {
        def aeid = getEntityID(r)
		DerivedMeasurementManagerEJBImpl.one.findDesignatedMeasurements(aeid)
    }
}
