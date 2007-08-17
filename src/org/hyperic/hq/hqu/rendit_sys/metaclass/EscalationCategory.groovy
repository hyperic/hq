package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.escalation.server.session.Escalation

class EscalationCategory {
    static String urlFor(Escalation e, String context) {
        "/admin/config/Config.do?mode=escalate&escId=${e.id}"        
    }
}
