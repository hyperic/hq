package org.hyperic.hq.events.server.session;

import org.hyperic.hq.escalation.server.session.EscalatableBase;
import org.hyperic.hq.events.AlertInterface;

public class ClassicEscalatable
    extends EscalatableBase
{
    private Alert _alert;
    
    public ClassicEscalatable(Alert a, String shortReason, String longReason) {
        super(a.getAlertDefinition(), a.getId(), shortReason, longReason);
        
        _alert = a;
    }

    public AlertInterface getAlertInfo() {
        return _alert;
    }
}
