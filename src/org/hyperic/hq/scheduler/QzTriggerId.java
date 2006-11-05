package org.hyperic.hq.scheduler;

public class QzTriggerId  implements java.io.Serializable {

    // Fields
    private String _triggerName;
    private String _triggerGroup;

    // Constructors
    public QzTriggerId() {
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
}
