package org.hyperic.hq.scheduler;

public class QzTriggerListener  implements java.io.Serializable {

    // Fields    
    private QzTriggerListenerId _id;
    private QzTrigger _trigger;

    // Constructors
    public QzTriggerListener() {
    }

    // Property accessors
    public QzTriggerListenerId getId() {
        return _id;
    }
    
    public void setId(QzTriggerListenerId id) {
        _id = id;
    }

    public QzTrigger getTrigger() {
        return _trigger;
    }
    
    public void setTrigger(QzTrigger trigger) {
        _trigger = trigger;
    }
}
