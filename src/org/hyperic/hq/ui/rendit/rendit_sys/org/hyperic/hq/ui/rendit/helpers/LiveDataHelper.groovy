package org.hyperic.hq.ui.rendit.helpers

import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl
import org.json.JSONArray

class LiveDataHelper 
    extends BaseHelper
{
    LiveDataHelper(user) {
        super(user)
    }

    private getDataMan() { LiveDataManagerEJBImpl.one }
    
    String[] getCommands(resource) {
        dataMan.getCommands(resource.entityId)
    }

    JSONArray getData(resource, command) {
        new JSONArray(dataMan.getData(resource.entityId, command)) 
    }
}

