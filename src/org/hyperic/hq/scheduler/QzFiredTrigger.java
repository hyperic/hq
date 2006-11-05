package org.hyperic.hq.scheduler;

public class QzFiredTrigger  implements java.io.Serializable {

    // Fields
    private String _entryId;
    private String _triggerName;
    private String _triggerGroup;
    private String _instanceName;
    private long _firedTime;
    private String _state;
    private boolean _isVolatile;
    private String _jobName;
    private String _jobGroup;
    private boolean _isStateful;
    private boolean _requestsRecovery;

    // Constructors
    public QzFiredTrigger() {
    }

    // Property accessors
    public String getEntryId() {
        return _entryId;
    }
    
    public void setEntryId(String entryId) {
        _entryId = entryId;
    }

    public String getTriggerName() {
        return _triggerName;
    }
    
    public void setTriggerName(String triggerName) {
        _triggerName = triggerName;
    }

    public String getTriggerGroup() {
        return _triggerGroup;
    }
    
    public void setTriggerGroup(String triggerGroup) {
        _triggerGroup = triggerGroup;
    }

    public String getInstanceName() {
        return _instanceName;
    }
    
    public void setInstanceName(String instanceName) {
        _instanceName = instanceName;
    }

    public long getFiredTime() {
        return _firedTime;
    }
    
    public void setFiredTime(long firedTime) {
        _firedTime = firedTime;
    }

    public String getState() {
        return _state;
    }
    
    public void setState(String state) {
        _state = state;
    }

    public boolean isIsVolatile() {
        return _isVolatile;
    }
    
    public void setIsVolatile(boolean isVolatile) {
        _isVolatile = isVolatile;
    }

    public String getJobName() {
        return _jobName;
    }
    
    public void setJobName(String jobName) {
        _jobName = jobName;
    }

    public String getJobGroup() {
        return _jobGroup;
    }
    
    public void setJobGroup(String jobGroup) {
        _jobGroup = jobGroup;
    }

    public boolean isIsStateful() {
        return _isStateful;
    }
    
    public void setIsStateful(boolean isStateful) {
        _isStateful = isStateful;
    }

    public boolean isRequestsRecovery() {
        return _requestsRecovery;
    }
    
    public void setRequestsRecovery(boolean requestsRecovery) {
        _requestsRecovery = requestsRecovery;
    }
}
