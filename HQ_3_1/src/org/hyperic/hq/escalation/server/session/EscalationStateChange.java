package org.hyperic.hq.escalation.server.session;

import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

public class EscalationStateChange 
    extends HypericEnum
{
    private static final String BUNDLE = "org.hyperic.hq.escalation.Resources";
    
    public static final EscalationStateChange CREATED =
        new EscalationStateChange(0, "created",
                                  "escalation.state.created");
    public static final EscalationStateChange ACKNOWLEDGED =
        new EscalationStateChange(1, "acknowledged",
                                  "escalation.state.acked");
    public static final EscalationStateChange FIXED =
        new EscalationStateChange(2, "fixed",
                                  "escalation.state.fixed");
    
    private EscalationStateChange(int code, String desc, String localeProp) {
        super(code, desc, localeProp, ResourceBundle.getBundle(BUNDLE)); 
    }
    
    public boolean isCreated() {
        return equals(CREATED);
    }

    public boolean isAcknowledged() {
        return equals(ACKNOWLEDGED);
    }

    public boolean isFixed() {
        return equals(FIXED);
    }
}
