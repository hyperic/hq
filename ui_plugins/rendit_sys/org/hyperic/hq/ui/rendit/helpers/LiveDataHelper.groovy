package org.hyperic.hq.ui.rendit.helpers

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

    LiveDataResult getData(AppdefResourceValue resource, String command, 
                           config) 
    {
        LiveDataCommand cmd = new LiveDataCommand(resource.entityId, command,
                                                  config as ConfigResponse)
        dataMan.getData(userVal, cmd)
    }
}

