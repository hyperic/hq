package org.hyperic.hq.scheduler;

public class QzCalendar  implements java.io.Serializable {

    // Fields
    private String _calendarName;
    private byte[] _calendar;

    // Constructors
    public QzCalendar() {
    }

    // Property accessors
    public String getCalendarName() {
        return _calendarName;
    }
    
    public void setCalendarName(String calendarName) {
        _calendarName = calendarName;
    }

    public byte[] getCalendar() {
        return _calendar;
    }
    
    public void setCalendar(byte[] calendar) {
        _calendar = calendar;
    }
}


