package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl
import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.escalation.server.session.Escalation

class AlertDefinitionCategory {

    static void setEscalation(AlertDefinition d, AuthzSubject s,
                              Escalation e) {
        AlertDefinitionManagerEJBImpl.one.setEscalation(s, d.getId(), e.getId())
    }
}
