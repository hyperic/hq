package org.hyperic.hq.galerts.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.ResourceGroup;

public class GalertLog
    extends PersistedObject
{ 
    public static int MAX_SHORT_REASON = 256;
    public static int MAX_LONG_REASON  = 2048;
    
    private GalertDef          _def;
    private long               _timestamp;
    private ResourceGroup      _group;
    private String             _shortReason;
    private String             _longReason;
    private GalertDefPartition _partition;
    
    protected GalertLog() {}
    
    GalertLog(GalertDef def, ExecutionReason reason, long timestamp) {
        if (reason.getShortReason().length() > MAX_SHORT_REASON ||
            reason.getLongReason().length() > MAX_LONG_REASON)
        {
            throw new IllegalArgumentException("Reason is too long");
        }
        
        _def         = def;
        _timestamp   = timestamp;
        _group       = def.getGroup();
        _shortReason = reason.getShortReason();
        _longReason  = reason.getLongReason();
        _partition   = reason.getPartition();
    }

    public GalertDef getAlertDef() {
        return _def;
    }
    
    protected void setAlertDef(GalertDef def) {
        _def = def;
    }

    public long getTimestamp() {
        return _timestamp;
    }
    
    protected void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }
    
    public ResourceGroup getGroup() {
        return _group;
    }
    
    protected void setGroup(ResourceGroup group) {
        _group = group;
    }
    
    protected String getShortReason() {
        return _shortReason;
    }
    
    protected void setShortReason(String txt) {
        _shortReason = txt;
    }
    
    protected String getLongReason() {
        return _longReason;
    }
    
    protected void setLongReason(String txt) {
        _longReason = txt;
    }
    
    protected void setPartitionEnum(int code) {
        _partition = GalertDefPartition.findByCode(code);
    }
    
    protected int getPartitionEnum() {
        return _partition.getCode();
    }
    
    public ExecutionReason getExecutionReason() {
        return new ExecutionReason(getShortReason(), getLongReason(),
                                   _partition);
    }

    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + getGroup().hashCode();
        hash = hash * 31 + (int)getTimestamp();
        hash = hash * 31 + getShortReason().hashCode();
        return hash;
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        
        if (o == null || o instanceof GalertLog == false)
            return false;
        
        GalertLog oe = (GalertLog)o;

        return oe.getGroup().equals(getGroup()) &&
            oe.getTimestamp() == getTimestamp() &&
            oe.getShortReason().equals(getShortReason());
    }
}
