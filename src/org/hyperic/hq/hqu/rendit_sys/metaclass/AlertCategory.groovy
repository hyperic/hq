package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.events.server.session.Alert

class AlertCategory {
    static String urlFor(Alert a) {
        def d = a.alertDefinition
        "/alerts/Alerts.do?mode=viewAlert&eid=${d.appdefEntityId}&a=${a.id}"
    }
}
