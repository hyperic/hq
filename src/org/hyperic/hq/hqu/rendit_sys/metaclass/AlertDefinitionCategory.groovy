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
        AdefMan.one.setEscalation(s, d.getId(), e.getId())
    }

    static void unsetEscalation(AlertDefinition d, AuthzSubject s) {
        EscMan.one.setEscalation(ClassicEscalationAlertType.CLASSIC, d.getId(), null)
    }
}
