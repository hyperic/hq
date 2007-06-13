package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl

class AlertHelper extends BaseHelper {
    private alertMan = AlertManagerEJBImpl.one
    
    AlertHelper(AuthzSubject user) {
        super(user)
    }

    def findAlerts(int priority, long timeRange, long endTime,
                   int page, int pageSize) 
    {
        alertMan.findAlerts(user.id, priority, timeRange, endTime, page,
                            pageSize)
    }
}
