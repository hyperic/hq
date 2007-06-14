package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl

class AlertHelper extends BaseHelper {
    private alertMan = AlertManagerEJBImpl.one
    
    AlertHelper(AuthzSubject user) {
        super(user)
    }

    def findAlerts(int priority, long timeRange, long endTime, PageInfo pInfo) {
        alertMan.findAlerts(user.id, priority, timeRange, endTime, pInfo);
    }
}
