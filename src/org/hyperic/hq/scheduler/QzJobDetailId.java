package org.hyperic.hq.scheduler;

public class QzJobDetailId  implements java.io.Serializable {

    // Fields
    private String _jobName;
    private String _jobGroup;

    // Constructors
    public QzJobDetailId() {
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
}
