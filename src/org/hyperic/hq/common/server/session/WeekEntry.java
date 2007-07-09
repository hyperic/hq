/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.common.server.session;

import java.util.Calendar;

/**
 * Represents time during a single week .. any week of the year.
 * 
 * WeekDay:              0->6  (0 being Sunday)
 * startTime & endTime:  minutes since 00:00 (midnight)
 */
public class WeekEntry
    extends CalendarEntry
{
    private static final int MINUTES_IN_DAY = 60 * 24;
    
    private int _weekDay;
    private int _startTime;
    private int _endTime;
    
    protected WeekEntry() {}

    WeekEntry(org.hyperic.hq.common.server.session.Calendar c, int weekDay, 
              int startTime, int endTime) 
    {
        super(c);
        
        if (weekDay < 0 || weekDay > 6)
            throw new IllegalArgumentException("Weekday must be 0 <= x <= 6");

        if (startTime < 0 || startTime >= MINUTES_IN_DAY) 
            throw new IllegalArgumentException("startTime must be 0 <= x <=  + "
                                               + MINUTES_IN_DAY);
        
        if (endTime < 0 || endTime >= MINUTES_IN_DAY) 
            throw new IllegalArgumentException("endTime must be 0 <= x <=  + "
                                               + MINUTES_IN_DAY);
        
        if (endTime <= startTime) 
            throw new IllegalArgumentException("endTime must be > startTime");

        _weekDay   = weekDay;
        _startTime = startTime;
        _endTime   = endTime;
    }

    public int getWeekDay() {
        return _weekDay;
    }
    
    protected void setWeekDay(int weekDay) {
        _weekDay = weekDay;
    }
    
    public int getStartTime() {
        return _startTime;
    }
    
    protected void setStartTime(int startTime) {
        _startTime = startTime;
    }
    
    public int getEndTime() {
        return _endTime;
    }
    
    protected void setEndTime(int endTime) {
        _endTime = endTime;
    }
    
    public boolean containsTime(long time) {
        Calendar c = Calendar.getInstance();
        
        c.setTimeInMillis(time);
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + _weekDay);
        c.set(Calendar.HOUR_OF_DAY, _startTime / 60);
        c.set(Calendar.MINUTE, _startTime % 60);
        c.set(Calendar.SECOND, 0);
              
        if (c.getTimeInMillis() > time)
            return false;

        c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + _weekDay);
        c.set(Calendar.HOUR_OF_DAY, _endTime / 60);
        c.set(Calendar.MINUTE, _endTime % 60);
        c.set(Calendar.SECOND, 0);
        
        return c.getTimeInMillis() >= time;
    }

    public String toString() {
        return "WeekEntry[weekday=" + _weekDay + ", startTime=" + _startTime + 
               ", endTime=" + _endTime + "]";
    }
}
