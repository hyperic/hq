package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl as RsrcMan
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl as GroupMan
import org.hyperic.hq.authz.shared.AuthzConstants

class ResourceGroupCategory {
    private static groupMan = GroupMan.one
    private static rsrcMan  = RsrcMan.one
    
    static String urlFor(ResourceGroup r, String context) {
        "/Resource.do?eid=${AppdefEntityConstants.APPDEF_TYPE_GROUP}:${r.id}"
    }
    
    static void setResources(ResourceGroup group, AuthzSubject user,
                             Collection resources)
    {
        groupMan.setResources(user, group, resources)
    }

    static Collection getResources(ResourceGroup group) {
        groupMan.getMembers(group)
    }
    
    /**
     * Returns true if the ResourceGroup is compatible (i.e. only contains
     * resources of a homogenous type)
     */
    static boolean isCompatible(ResourceGroup g) {
        g.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS ||
        g.groupType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC
    }
    
    /**
     * Gets the Resource prototype that the group is compatible wth.
     */
    static Resource getCompatibleType(ResourceGroup g) {
        if (!isCompatible(g)) {
            throw new IllegalArgumentException("Attempted to get compatible " +
                                               "type on non-compatible group")
        }
        
        def resourceTypeId
        if (g.groupEntType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            resourceTypeId = AuthzConstants.authzPlatformProto
        } else if (g.groupEntType == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            resourceTypeId = AuthzConstants.authzServerProto
        } else if (g.groupEntType == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            resourceTypeId = AuthzConstants.authzServiceProto
        } else {
            assert "Unknown group ent type ${g.groupEntType} for compat groups"
        }
            
        rsrcMan.findResourcePojoByInstanceId(resourceTypeId, g.groupEntResType)
    }
}
