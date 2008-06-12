package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl as AdefMan
import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.escalation.server.session.Escalation
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl as EscMan
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.appdef.server.session.AppdefResourceType
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl

class AlertDefinitionCategory {

    static void setEscalation(AlertDefinition d, AuthzSubject s,
                              Escalation e) {
        AdefMan.one.setEscalation(s.authzSubjectValue, d.getId(), e.getId())
    }

    static void unsetEscalation(AlertDefinition d, AuthzSubject s) {
        EscMan.one.setEscalation(ClassicEscalationAlertType.CLASSIC, d.getId(),
                                 null)
    }

    /**
     * Enable/Disable an alert definition
     *
     * @param d           The alert definition {@link AlertDefinition}
     * @param s           The caller {@link AuthzSubject}
     * @param enabled     Specifies whether the alert definition 
     *                    should be enabled or disabled.
     */
    static void updateAlertDefinitionActiveStatus(AlertDefinition d,
                                                  AuthzSubject s,
                                                  boolean enable) {
        AdefMan.one.updateAlertDefinitionActiveStatus(s.authzSubjectValue,
                                                      d, enable)
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
        def appdefId
        if (d.typeBased)
            appdefId = d.appdefId
        else
            appdefId = d.resource.prototype.instanceId
            
        if (d.appdefType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            return PlatformManagerEJBImpl.one.findPlatformType(appdefId)
        } else if (d.appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVER) {
            return ServerManagerEJBImpl.one.findServerType(appdefId)
        } else if (d.appdefType == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            return ServiceManagerEJBImpl.one.findServiceType(appdefId)
        } else {
            throw new RuntimeException("Unhandled appdef type: ${appdefId}")
        }
    }
    
    static boolean getTypeBased(AlertDefinition d) {
        return d.parent != null && d.parent.id == 0;
    }
}
