package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.hq.product.PluginNotFoundException
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefResourceValue
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl
import org.hyperic.hq.livedata.shared.LiveDataResult
import org.hyperic.hq.livedata.shared.LiveDataCommand

class LiveDataHelper 
    extends BaseHelper
{
    LiveDataHelper(user) {
        super(user)
    }

    private getDataMan() { LiveDataManagerEJBImpl.one }
    
    String[] getCommands(AppdefResourceValue resource) {
        dataMan.getCommands(userVal, resource.entityId)
    }

    String[] getCommands(AppdefEntityID id) {
        dataMan.getCommands(userVal, id)
    }

    LiveDataResult getData(AppdefResourceValue resource, String command, 
                           config) 
    {
        def cmd = [resource.entityId, command, 
                   config as ConfigResponse] as LiveDataCommand
        dataMan.getData(userVal, cmd)
    }
    
    /** 
     * Check if a resource supports a command.  
     * XXX:  Would be cool if we could just attach this functionality directly
     *       to the entity ID or the resource via categories or metaclasses!
     */
    boolean resourceSupports(AppdefEntityID id, String command) {
        try {
            return (dataMan.getCommands(userVal, id) as List).contains(command)
        } catch(PluginNotFoundException e) {
            return false
        }
    }
}

