package org.hyperic.hq.hqu.rendit.metaclass

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.appdef.server.session.AppdefResourceType
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl
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

    static String urlFor(AlertDefinition d, String context) {
        if (context == 'listAlerts') {
        	def cal = new GregorianCalendar()
        	cal.setTime(new Date(d.lastFired))
        	
            return "/alerts/Alerts.do?mode=list&eid=${d.appdefType}:${d.appdefId}&year=" +
            	cal.get(Calendar.YEAR) + "&month=" + cal.get(Calendar.MONTH) +
            	"&day=" + cal.get(Calendar.DATE)
        }
        
        if (d.typeBased) {
            // Type-based definition
            return "/alerts/Config.do?mode=viewDefinition&aetid=${d.appdefEntityId}&ad=${d.id}"            
        }
        "/alerts/Config.do?mode=viewDefinition&eid=${d.appdefEntityId}&ad=${d.id}"        
    }

    /**
     * @deprecated  This method should be deprecated, since we don't want
     *              people to rely on Appdef type entities instead of Resources
     */
    static AppdefResourceType getResourceType(AlertDefinition d) {
        if (d.appdefType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            return PlatformManagerEJBImpl.one.findPlatformTypeById(d.appdefId)
        } else if (d.appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            return ServerManagerEJBImpl.one.findServerType(d.appdefId)
        } else if (d.appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            return ServiceManagerEJBImpl.one.findServiceType(d.appdefId)
        } else {
            throw new RuntimeException("Unhandled appdef type: ${d.appdefType}")
        }
    }
    
    static AuthzSubject getAcknowledgedBy(Alert a) {
        _getAcknowledgedBy(a.ackedBy)
    }

    static AuthzSubject getAcknowledgedBy(GalertLog a) {
        _getAcknowledgedBy(a.ackedBy)
    }
    
    static boolean getTypeBased(AlertDefinition d) {
        return d.parent != null && d.parent.id == 0;
    }
    
    private static AuthzSubject _getAcknowledgedBy(id) {
        if (id == null)
            return null
            
        AuthzSubjectManagerEJBImpl.one.getSubjectById(id.toInteger())
    }
}
