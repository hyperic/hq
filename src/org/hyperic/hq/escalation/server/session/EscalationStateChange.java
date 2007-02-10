package org.hyperic.hq.escalation.server.session;

import org.hyperic.util.HypericEnum;

public class EscalationStateChange 
    extends HypericEnum
{
    public static final EscalationStateChange CREATED =
        new EscalationStateChange(0, "created");
    public static final EscalationStateChange ACKNOWLEDGED =
        new EscalationStateChange(1, "acknowledged");
    public static final EscalationStateChange FIXED =
        new EscalationStateChange(2, "fixed");
    
    private EscalationStateChange(int code, String desc) {
        super(code, desc);
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
