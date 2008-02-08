package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.hibernate.SessionManager
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl as ResourceMan
import org.hyperic.hq.appdef.server.session.AppdefResource
import org.hyperic.hq.authz.shared.PermissionManagerFactory as PermManFactory
import org.hyperic.hq.authz.shared.PermissionManager

class AppdefCategory {
    static Resource getResource(AppdefResource r) {
		ResourceMan.one.findResource(r.entityId)
    }

    /**
     * Assert permissions on a resource.  If the permission fails, a 
     * PermissionException will be thrown.
     *
     * This method is currently only useful on resources of type AppdefResource
     *
     * checkPerms(resource:myPlatform, operation:'view')
     *   -- Throws PermissionException if platform cannot be viewed
     *
     * checkPerms(resource:myPlatform, operation:'view', permCheck:false)
     *   -- no-op, the permCheck flag immediately returns
     *
     * @return the {@link Resource} represented by the passed AppdefResource
     *
     * @throws PermissionException if denied
     *
     * XXX:  For 'create' operations, this doesn't work, since we use
     *       the root resource type for create ops
     */
    static Resource checkPerms(AppdefResource r, Map p) {
        if (p.permCheck == false)
            return r.resource

        def result = []
        def runner = [
            getName:{'HQU Perm Check'},
            run:{
                PermissionManager permMan = PermManFactory.instance
                
                ['operation', 'user'].each {p.get(it, null)}
             
                def operation  = r.getAuthzOp(p.operation)
                def user       = p.user
                def resource   = r.resource
                def instanceId = resource.instanceId
                assert instanceId == r.id
                
                permMan.check(user.id, resource.resourceType, instanceId, 
                              operation)
                result << r.resource
            }
        ] as SessionManager.SessionRunner
        
        SessionManager.runInSession(runner)
        return result[0]
    }
}
