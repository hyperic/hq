package org.hyperic.hq.events;

import java.util.List;

import org.hyperic.util.HypericEnum;

public class AlertSeverity 
    extends HypericEnum
{
    public static final AlertSeverity LOW = 
        new AlertSeverity(EventConstants.PRIORITY_LOW,
                    EventConstants.getPriority(EventConstants.PRIORITY_LOW));
    public static final AlertSeverity MEDIUM = 
        new AlertSeverity(EventConstants.PRIORITY_MEDIUM,
                    EventConstants.getPriority(EventConstants.PRIORITY_MEDIUM));
    public static final AlertSeverity HIGH =  
        new AlertSeverity(EventConstants.PRIORITY_HIGH,
                    EventConstants.getPriority(EventConstants.PRIORITY_HIGH));

    private AlertSeverity(int code, String desc) {
        super(code, desc);
    }
    
    public static List getAll() {
        return HypericEnum.getAll(HypericEnum.class);
    }
}
