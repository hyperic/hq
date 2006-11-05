package org.hyperic.hq.scheduler;

public class QzJobListener  implements java.io.Serializable {

    // Fields
    private QzJobListenerId _id;
    private QzJobDetail _jobDetails;

    public QzJobListener() {
    }

    // Property accessors
    public QzJobListenerId getId() {
        return _id;
    }
    
    public void setId(QzJobListenerId id) {
        _id = id;
    }

    public QzJobDetail getJobDetails() {
        return _jobDetails;
    }
    
    public void setJobDetails(QzJobDetail jobDetails) {
        _jobDetails = jobDetails;
    }
}
