package org.hyperic.hq.galerts.server.session;

public class ExecutionReason {
    private final String             _shortReason;
    private final String             _longReason;
    private final GalertDefPartition _partition;
    
    public ExecutionReason(String shortReason, String longReason,
                           GalertDefPartition partition) 
    {
        _shortReason = shortReason;
        _longReason  = longReason;
        _partition   = partition;
    }
    
    public String getShortReason() {
        return _shortReason;
    }
    
    public String getLongReason() {
        return _longReason;
    }
    
    public GalertDefPartition getPartition() {
        return _partition;
    }
    
    public String toString() {
        return "Execution Reason (" + _partition + "):\n" + 
            "Short = [" + _shortReason + "]\n" + 
            "Long = \n" + 
            "[" + _longReason + "]" + "\n";
    }
}
