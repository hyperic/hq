package org.hyperic.hq.ui.rendit.metaclass

import org.hyperic.hq.appdef.shared.PlatformValue
import org.hyperic.hq.appdef.shared.AppdefResourceValue
import org.hyperic.hq.livedata.shared.LiveDataResult
import org.hyperic.hq.ui.rendit.helpers.LiveDataHelper
import org.hyperic.hq.appdef.shared.AppdefEntityID

import groovy.lang.DelegatingMetaClass

/**
 * This category adds utility methods to appdef types, to interface with 
 * LiveData.
 */
class AppdefLiveDataCategory {
    /**
     * AppdefEntityID.liveDataCommands
     * See also:  LiveDataHelper.getCommands
     */
    static String[] getLiveDataCommands(AppdefEntityID ent) {
        def ldh = new LiveDataHelper(CategoryInfo.user)
        ldh.getCommands(ent)        
    }
    
    static String[] getLiveDataCommands(AppdefResourceValue ent) {
        getLiveDataCommands(ent.entityId)
    }

    /**
     * AppdefEntityID.getLiveData(command, ?optionalConfigMap?)
     * See also:  LiveDataHelper.getData
     * 
     * The optional config map is .. optional, but if specified, must be a 
     * map of string keys and string values, specifying the configuration to
     * pass to the command
     */
    static LiveDataResult getLiveData(AppdefEntityID ent, String command,
                                      Object[] args) 
    {
        def ldh = new LiveDataHelper(CategoryInfo.user)
        def config
        
        if (args.length == 0) {
            config = [:]
        } else if (args.length == 1) {
            config = args[0]
        } else {
            throw new IllegalArgumentException("Too many arguments to " +
                                               "getLiveData()")
        }
        
        ldh.getData(ent, command, config)        
    }
    
    static LiveDataResult getLiveData(AppdefResourceValue ent, String command,
                                      Object[] args)
    {
        getLiveData(ent.entityId, command, args)
    }
}
