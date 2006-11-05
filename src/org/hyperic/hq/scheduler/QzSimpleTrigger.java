package org.hyperic.hq.scheduler;

public class QzSimpleTrigger extends org.hyperic.hq.scheduler.QzTrigger
    implements java.io.Serializable {

    // Fields
    private long _repeatCount;
    private long _repeatInterval;
    private long _timesTriggered;

    // Constructors
    public QzSimpleTrigger() {
    }

    // Property accessors
    public long getRepeatCount() {
        return _repeatCount;
    }
    
    public void setRepeatCount(long repeatCount) {
        _repeatCount = repeatCount;
    }

    public long getRepeatInterval() {
        return _repeatInterval;
    }
    
    public void setRepeatInterval(long repeatInterval) {
        _repeatInterval = repeatInterval;
    }

    public long getTimesTriggered() {
        return _timesTriggered;
    }
    
    public void setTimesTriggered(long timesTriggered) {
        _timesTriggered = timesTriggered;
    }
}
