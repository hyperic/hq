package org.hyperic.hq.hqu.rendit.metaclass

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl
import org.hyperic.hq.events.server.session.Alert
import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.galerts.server.session.GalertDef
import org.hyperic.hq.galerts.server.session.GalertLog

class AlertCategory {
    static String urlFor(Alert a, String context) {
        def d = a.alertDefinition
        "/alerts/Alerts.do?mode=viewAlert&eid=${d.appdefEntityId}&a=${a.id}"
    }
    
    static String urlFor(GalertLog a, String context) {
        def d = a.alertDef
        "/alerts/Alerts.do?mode=viewAlert&eid=${d.appdefID}&a=${a.id}"
    }

    static String urlFor(GalertDef d, String context) {
        def groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP
        if (context == 'listAlerts') {
            return "/alerts/Alerts.do?mode=list&rid=${d.group.id}&type=${groupType}"            
        }
        "/alerts/Config.do?mode=viewGroupDefinition&eid=${groupType}:${d.group.id}&ad=${d.id}"       
    }

    static AuthzSubject getAcknowledgedBy(Alert a) {
        _getAcknowledgedBy(a.ackedBy)
    }

    static AuthzSubject getAcknowledgedBy(GalertLog a) {
        _getAcknowledgedBy(a.ackedBy)
    }
    
    private static AuthzSubject _getAcknowledgedBy(id) {
        if (id == null)
            return null
            
        AuthzSubjectManagerEJBImpl.one.getSubjectById(id.toInteger())
    }
}
