package org.hyperic.hq.scheduler;

public class QzCronTrigger extends org.hyperic.hq.scheduler.QzTrigger
    implements java.io.Serializable {

    // Fields
    private String _cronExpression;
    private String _timeZoneId;

    // Constructors
    public QzCronTrigger() {
    }

    // Property accessors
    public String getCronExpression() {
        return _cronExpression;
    }
    
    public void setCronExpression(String cronExpression) {
        _cronExpression = cronExpression;
    }

    public String getTimeZoneId() {
        return _timeZoneId;
    }
    
    public void setTimeZoneId(String timeZoneId) {
        _timeZoneId = timeZoneId;
    }
}
