package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as GroupMan

class ResourceGroupCategory {
    private static groupMan = GroupMan.one
    
    static String urlFor(ResourceGroup r, String context) {
        "/Resource.do?eid=${AppdefEntityConstants.APPDEF_TYPE_GROUP}:${r.id}"
    }
    
    static void setResources(ResourceGroup group, AuthzSubject user,
                             Collection resources)
    {
        groupMan.setResources(user, group, resources)
    }
}
