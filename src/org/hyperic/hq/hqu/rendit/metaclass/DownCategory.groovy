package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.appdef.server.session.DownResource

class DownCategory {
    static String urlFor(DownResource d, String context) {
        "/alerts/Alerts.do?mode=list&eid=${d.resource.entityId}"        
    }
}
