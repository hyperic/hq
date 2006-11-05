package org.hyperic.hq.scheduler;

public class QzSchedulerState  implements java.io.Serializable {

    // Fields
    private String _instanceName;
    private long _lastCheckinTime;
    private long _checkinInterval;
    private String _recoverer;

    // Constructors
    public QzSchedulerState() {
    }

    // Property accessors
    public String getInstanceName() {
        return _instanceName;
    }
    
    public void setInstanceName(String instanceName) {
        _instanceName = instanceName;
    }

    public long getLastCheckinTime() {
        return _lastCheckinTime;
    }
    
    public void setLastCheckinTime(long lastCheckinTime) {
        _lastCheckinTime = lastCheckinTime;
    }

    public long getCheckinInterval() {
        return _checkinInterval;
    }
    
    public void setCheckinInterval(long checkinInterval) {
        _checkinInterval = checkinInterval;
    }

    public String getRecoverer() {
        return _recoverer;
    }
    
    public void setRecoverer(String recoverer) {
        _recoverer = recoverer;
    }
}
