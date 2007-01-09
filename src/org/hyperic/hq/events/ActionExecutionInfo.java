package org.hyperic.hq.events;

import org.hyperic.hq.events.server.session.Action;

/**
 * These are value objects passed into 
 * {@link Action#executeAction(AlertInterface, ActionExecutionInfo)
 * which contain the reasons that the action is being executed.
 */
public class ActionExecutionInfo {
    private String _shortReason;
    private String _longReason;
    
    public ActionExecutionInfo(String shortReason, String longReason) {
        _shortReason = shortReason;
        _longReason  = longReason;
    }
    
    public String getShortReason() {
        return _shortReason;
    }
    
    public String getLongReason() {
        return _longReason;
    }
}
