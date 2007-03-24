package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.util.pager.PageControl

import org.hyperic.hq.appdef.shared.AppdefResourceValue
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl

class ResourceHelper 
    extends BaseHelper
{
    private final NAME_FINDERS = [ 
        platform: [ 
            str: {name -> platMan.getPlatformByName(userVal, name)},
            int: {id -> platMan.getPlatformValueById(userVal, id)}],
        platformType: [
            str: {name -> platMan.findPlatformTypeByName(name)},
            int: {id -> platMan.findPlatformTypeValueById(id)}],
    ]

    ResourceHelper(user) {
        super(user)
    }

    private getPlatMan() { PlatformManagerEJBImpl.one }  

    /**
     * Get all the platforms.  The results are constrained by the authoraiztaion
     * of the current user
     */
    Collection getAllPlatforms() {
        platMan.getAllPlatforms(userVal, PageControl.PAGE_ALL)
    }
    
    /**
     * Generic method to find resources.  The results are constrained by the
     * authorization of the current user
     * 
     * args:  The arguments are a map of options.  Currently the only 
     *        options are to find a platform or platformType by name or id.
     *
     * Examples:
     * 
     *     To find a platform by name:
     *       > find platform:'My Platform'
     *
     *     To find a platform by id:
     *       > find platform:44123
     */
    AppdefResourceValue find(args) {
        def resourceType
        for (i in args) {
            if (NAME_FINDERS.containsKey(i.key)) {
                if (resourceType != null) {
                    throw new IllegalArgumentException("""Cannot specify more 
                                than one resource type [$resourceType] and 
                                [$i.key]""")
                }
                resourceType = i.key
             }
        }

        if (resourceType == null)
            throw new IllegalArgumentException(""""No resource type specified. 
                                               Must be one of  
                                               $NAME_FINDERS.keySet()""")

        def resourceVal = args[resourceType]
        def argType = (resourceVal instanceof String) ? 'str' : 'int'
        NAME_FINDERS[resourceType][argType].call(resourceVal)
    }
}
