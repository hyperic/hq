package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.util.pager.PageControl

import org.hyperic.hq.appdef.shared.AppdefEntityValue
import org.hyperic.hq.appdef.shared.AppdefResourceValue
import org.hyperic.hq.appdef.server.session.AppdefGroupManagerEJBImpl
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
        group: [ 
            str: {name -> groupMan.findGroupByName(userVal, name)},
            int: {id -> groupMan.findGroup(userVal, id)}],
    ]
    
    private final ALL_FINDERS = [
        platforms: {platMan.getAllPlatforms(userVal, PageControl.PAGE_ALL)},
        groups: {groupMan.findAllGroups(userVal, PageControl.PAGE_ALL)},                               
    ]

    ResourceHelper(user) {
        super(user)
    }

    private getPlatMan() { PlatformManagerEJBImpl.one }  
    private getGroupMan() { AppdefGroupManagerEJBImpl.one }  

    /**
     * Generic method to find resources.  The results are constrained by the
     * authorization of the current user.  The result objects are subclasses  
     * of AppdefResourceValue.
     * 
     * Examples:
     * 
     *     To find a platform by name:
     *       > find platform:'My Platform'
     *
     *     To find a platform by id:
     *       > find platform:44123
     *
     *     To find all the platforms:
     *       > find all:'platforms'
     *
     *     To find a resource (by appdef entityID)
     *       > find resource:entId
     */
    def find(args) {
        if (args.containsKey('resource'))
            return findByEntId(args) 
            
        if (args.containsKey('all')) 
            return findAll(args)

        findSingle(args)
    }
    
    private def findByEntId(args) {
        new AppdefEntityValue(args['resource'], userVal).resourceValue
    }
    
    private def findAll(args) {
        def type = args['all']
        if (!ALL_FINDERS.containsKey(type)) {
            throw new IllegalArgumentException("Unknown resource type [$type]" + 
                                               ".  Must be one of " +
                                               ALL_FINDERS.keySet());
        }
        ALL_FINDERS[type]()
    }
     
    private def findSingle(args) {
        def resourceType
        for (i in args) {
            if (NAME_FINDERS.containsKey(i.key)) {
                if (resourceType != null) {
                    throw new IllegalArgumentException("Cannot specify more " + 
                                "than one resource type [$resourceType] and " + 
                                "[$i.key]")
                }
                resourceType = i.key
             }
        }

        if (resourceType == null)
            throw new IllegalArgumentException("No resource type specified. "+ 
                                               "Must be one of " +  
                                               NAME_FINDERS.keySet())

        def resourceVal = args[resourceType]
        def argType = (resourceVal instanceof String) ? 'str' : 'int'

        if (argType == 'str') {
            try { // If it's a string with an integer inside, try that
                resourceVal = Integer.valueOf(resourceVal)
                argType = 'int'
            } catch(NumberFormatException e) {
            }
        }
        NAME_FINDERS[resourceType][argType](resourceVal)
    }
}
