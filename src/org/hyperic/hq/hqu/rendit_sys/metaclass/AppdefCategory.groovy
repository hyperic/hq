package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl as ResourceMan
import org.hyperic.hq.appdef.server.session.AppdefResource

class AppdefCategory {
    static Resource getResource(AppdefResource r) {
		ResourceMan.one.findResource(r.entityId)
    }
}
