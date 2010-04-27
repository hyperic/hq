package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceManagerImpl
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.appdef.server.session.AppdefResource
import org.hyperic.hq.appdef.server.session.AppdefResourceType
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory as PermManFactory
import org.hyperic.hq.authz.shared.PermissionManager
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.PermissionException;

class AppdefCategory {
    static Resource getResource(AppdefResource r) {
		Bootstrap.getBean(ResourceManager.class).findResource(r.entityId) 
    }

    static Resource getResource(AppdefResourceType r) {
		Bootstrap.getBean(ResourceManager.class).findResourceByInstanceId(r.authzType, r.id)
    }
    
    /**
     * Assert permissions on a resource.  If the permission fails, a 
     * PermissionException will be thrown.
     *
     * This method is currently only useful on resources of type AppdefResource
     *
     * myPlatform.checkPerms(operation:'view', user:myUser)
     *   -- Throws PermissionException if platform cannot be viewed
     *
     * myPlatform.checkPerms(operation:'view', permCheck:false, user:someUser)
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

        PermissionManager permMan = PermManFactory.instance

        ['operation', 'user'].each {p.get(it, null)}

        def operation = r.getAuthzOp(p.operation)
        def user = p.user
        def resource = r.resource
        
        // HHQ-3736 - null resource_id in EAM_PLATFORM,EAM_SERVER,EAM_SERVICE.
        if (resource == null) {
            throw new PermissionException()
        }  
        
        def instanceId = resource.instanceId
        assert instanceId == r.id

        permMan.check(user.id, resource.resourceType, instanceId,
                      operation)
        resource
    }
}
