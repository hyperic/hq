package org.hyperic.hq.control.server.session;

import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.util.pager.PagerProcessor;

public class PagerProcessor_control_history implements PagerProcessor {
 
    public PagerProcessor_control_history () {}

    public Object processElement(Object o) {
        if (o == null) return null;
        try {
            if (o instanceof ControlHistory) {
                return ((ControlHistory)o).getControlHistoryValue();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error converting to " +
                                            "ControlHistoryValue: " + e);
        }
        return o;
    }
}
