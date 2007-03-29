package org.hyperic.hq.ui.rendit.metaclass

import org.hyperic.util.pager.PageControl
import org.hyperic.hq.appdef.shared.AppdefEntityValue
import org.hyperic.hq.appdef.server.session.AppdefResource
import org.hyperic.hq.appdef.shared.PlatformValue
import org.hyperic.hq.appdef.shared.AppdefResourceValue
import org.hyperic.hq.livedata.shared.LiveDataResult
import org.hyperic.hq.ui.rendit.helpers.ResourceHelper
import org.hyperic.hq.appdef.shared.AppdefEntityID

/**
 * This category enhances the general appdef types by giving them additional
 * methods
 */
class AppdefAppdefCategory {
    /**
     * Get a resource from an appdef entity ID.
     */
    static AppdefResourceValue getResource(AppdefEntityID entId) {
		(new ResourceHelper(categoryInfo.user)).find(resource:entId)
    }
    
    /**
     * Get the children of a resource.  
     * 
     * Returns:  A collection of AppdefResourceValues
     */
    static Collection getChildren(AppdefEntityID entId) {
        def ent = new AppdefEntityValue(entId, CategoryInfo.user.valueObject) 
        
        if (entId.isPlatform())
            return ent.getAssociatedServers(PageControl.PAGE_ALL)
        else if (entId.isServer())
            return ent.getAssociatedServices(PageControl.PAGE_ALL)
        else
            throw new IllegalArgumentException("Unknown resource type")
    }
		
    static Collection getChildren(AppdefResourceValue ent) {
        getChildren(ent.entityId)
    }
		
}
