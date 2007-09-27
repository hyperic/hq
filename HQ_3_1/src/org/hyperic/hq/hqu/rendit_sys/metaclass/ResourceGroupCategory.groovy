package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.appdef.shared.AppdefEntityConstants

class ResourceGroupCategory {
    static String urlFor(ResourceGroup r, String context) {
        "/Resource.do?eid=${AppdefEntityConstants.APPDEF_TYPE_GROUP}:${r.id}"
    }
}
