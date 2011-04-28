/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

    private static resMan = Bootstrap.getBean(ResourceManager.class)

    static Resource getResource(AppdefResource r) {
		resMan.findResource(r.entityId)
    }

    static Resource getResource(AppdefResourceType r) {
		resMan.findResourceByInstanceId(r.authzType, r.id)
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

        def user = p.user
        def resource = r.resource

        if (user.id == 1) {
            // skip permission check for hqadmin
            return resource
        }

        // HHQ-3736 - null resource_id in EAM_PLATFORM,EAM_SERVER,EAM_SERVICE.
        if (resource == null) {
            throw new PermissionException()
        }  
        
        def instanceId = resource.instanceId
        assert instanceId == r.id

        def operation = r.getAuthzOp(p.operation)
        permMan.check(user.id, resource.resourceType, instanceId,
                      operation)
        resource
    }
}
