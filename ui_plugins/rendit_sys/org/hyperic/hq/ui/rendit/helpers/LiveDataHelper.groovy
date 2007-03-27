package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.hq.product.PluginNotFoundException
import org.hyperic.hq.appdef.shared.AppdefEntityID
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
    
    String[] getCommands(AppdefEntityID id) {
        dataMan.getCommands(userVal, id)
    }
    
    LiveDataResult[] getData(LiveDataCommand[] commands) {
        dataMan.getData(userVal, commands)
    }

    LiveDataResult getData(AppdefEntityID ent, String command, config) { 
        def cmd = [ent, command, config as ConfigResponse] as LiveDataCommand 
        dataMan.getData(userVal, cmd)
    }

    LiveDataResult getData(LiveDataCommand command) {
        dataMan.getData(userVal, command)
    }
    
    /** 
     * Check if a resource supports a command.  
     */
    boolean resourceSupports(AppdefEntityID id, String command) {
        try {
            return (id.liveDataCommands as List).contains(command) 
        } catch(PluginNotFoundException e) {
            return false
        }
    }
}

