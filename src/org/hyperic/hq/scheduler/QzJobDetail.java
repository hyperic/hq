package org.hyperic.hq.scheduler;

import java.util.Collection;

public class QzJobDetail  implements java.io.Serializable {

    // Fields    
    private QzJobDetailId _id;
    private String _description;
    private String _jobClassName;
    private boolean _isDurable;
    private boolean _isVolatile;
    private boolean _isStateful;
    private boolean _requestsRecovery;
    private byte[] _jobData;
    private Collection _jobListeners;
    private Collection _triggers;

    // Constructors
    public QzJobDetail() {
    }

    // Property accessors
    public QzJobDetailId getId() {
        return _id;
    }
    
    public void setId(QzJobDetailId id) {
        _id = id;
    }

    public String getDescription() {
        return _description;
    }
    
    public void setDescription(String description) {
        _description = description;
    }
    public String getJobClassName() {
        return _jobClassName;
    }
    
    public void setJobClassName(String jobClassName) {
        _jobClassName = jobClassName;
    }

    public boolean isIsDurable() {
        return _isDurable;
    }
    
    public void setIsDurable(boolean isDurable) {
        _isDurable = isDurable;
    }

    public boolean isIsVolatile() {
        return _isVolatile;
    }
    
    public void setIsVolatile(boolean isVolatile) {
        _isVolatile = isVolatile;
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

    public byte[] getJobData() {
        return _jobData;
    }
    
    public void setJobData(byte[] jobData) {
        _jobData = jobData;
    }

    public Collection getJobListeners() {
        return _jobListeners;
    }
    
    public void setJobListeners(Collection jobListeners) {
        _jobListeners = jobListeners;
    }

    public Collection getTriggers() {
        return _triggers;
    }
    
    public void setTriggers(Collection triggers) {
        _triggers = triggers;
    }

}
