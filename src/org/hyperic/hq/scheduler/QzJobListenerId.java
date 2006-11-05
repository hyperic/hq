package org.hyperic.hq.scheduler;

public class QzJobListenerId  implements java.io.Serializable {

    // Fields
    private String _jobName;
    private String _jobGroup;
    private String _jobListener;

    // Constructors
    public QzJobListenerId() {
    }

    // Property accessors
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

    public String getJobListener() {
        return _jobListener;
    }
    
    public void setJobListener(String jobListener) {
        _jobListener = jobListener;
    }
}
