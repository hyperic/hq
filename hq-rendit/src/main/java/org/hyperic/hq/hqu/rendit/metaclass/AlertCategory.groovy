package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerImpl
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.server.session.Alert
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
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
            
       Bootstrap.getBean(AuthzSubjectManager.class).getSubjectById(id.toInteger())
    }
    
    static void fix(Alert a, AuthzSubject subject, String reason) {
      Bootstrap.getBean(EscalationManager.class).fixAlert(subject, ClassicEscalationAlertType.CLASSIC,a.id,reason)
    }
}
