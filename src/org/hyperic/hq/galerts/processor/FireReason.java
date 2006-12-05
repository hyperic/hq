package org.hyperic.hq.galerts.processor;

public class FireReason {
    private String _shortReason;
    private String _longReason;
        
    public FireReason(String shortReason, String longReason) {
        _shortReason = shortReason;
        _longReason  = longReason;
    }
        
    public String getShortReason() {
        return _shortReason;
    }

    public String getLongReason() {
        return _longReason;
    }
        
    public String toString() {
        return "FireReason['" + _shortReason + "']";
    }
}
