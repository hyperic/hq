package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl

class AlertHelper extends BaseHelper {
    private alertMan = AlertManagerEJBImpl.one
    
    AlertHelper(AuthzSubject user) {
        super(user)
    }

    def findAlerts(int count, int priority, long timeRange, long endTime) {
        alertMan.findAlerts(userValue, count, priority, timeRange, endTime, 
                            null)
    }
}
