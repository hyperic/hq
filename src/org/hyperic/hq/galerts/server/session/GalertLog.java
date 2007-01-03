package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;

public class GalertLog
    extends PersistedObject 
    implements AlertInterface
{ 
    public static int MAX_SHORT_REASON = 256;
    public static int MAX_LONG_REASON  = 2048;

    private boolean            _fixed;
    private GalertDef          _def;
    private long               _timestamp;
    private String             _shortReason;
    private String             _longReason;
    private GalertDefPartition _partition;
    private Collection         _actionLog = new ArrayList();
    
    protected GalertLog() {}
    
    GalertLog(GalertDef def, ExecutionReason reason, long timestamp) {
        if (reason.getShortReason().length() > MAX_SHORT_REASON ||
            reason.getLongReason().length() > MAX_LONG_REASON)
        {
            throw new IllegalArgumentException("Reason is too long");
        }
        
        _def         = def;
        _timestamp   = timestamp;
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

    public AlertDefinitionInterface getAlertDefinitionInterface() {
        return getAlertDef();
    }

    public long getTimestamp() {
        return _timestamp;
    }
    
    protected void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }
    
    public String getShortReason() {
        return _shortReason;
    }
    
    protected void setShortReason(String txt) {
        _shortReason = txt;
    }
    
    public String getLongReason() {
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
    
    protected void setActionLogBag(Collection actionLog) {
        _actionLog = actionLog;
    }

    protected Collection getActionLogBag() {
        return _actionLog;
    }

    public Collection getActionLog() {
        return Collections.unmodifiableCollection(_actionLog);
    }

    public ExecutionReason getExecutionReason() {
        return new ExecutionReason(getShortReason(), getLongReason(),
                                   _partition);
    }

    public boolean isFixed() {
        return _fixed;
    }

    public void setFixed(boolean fixed) {
        _fixed = fixed;
    }

    public int hashCode() {
        int hash = 1;

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

        return oe.getTimestamp() == getTimestamp() && 
               oe.getShortReason().equals(getShortReason());
    }
}
