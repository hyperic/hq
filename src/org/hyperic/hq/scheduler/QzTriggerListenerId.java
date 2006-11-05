package org.hyperic.hq.scheduler;

public class QzTriggerListenerId  implements java.io.Serializable {

    // Fields    
    private String _triggerName;
    private String _triggerGroup;
    private String _triggerListener;

    // Constructors
    public QzTriggerListenerId() {
    }

    // Property accessors
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

    public String getTriggerListener() {
        return _triggerListener;
    }
    
    public void setTriggerListener(String triggerListener) {
        _triggerListener = triggerListener;
    }
}
