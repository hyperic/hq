package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl as AdefMan
import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.escalation.server.session.Escalation
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl as EscMan
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType

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
}
