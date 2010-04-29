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

package org.hyperic.hq.scheduler;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

// XXX: This does not yet support monthly recurrance of the format
//      2nd day of the 3rd week

public class ScheduleMonthlyValue extends ScheduleValue
{

    // Which day of the month to recur on
    private int day;

    // Offset dates
    private boolean offset = false;
    private int dayOfWeek;
    private int weekOfMonth;

    public ScheduleMonthlyValue(Date start, Date end, int interval)
    {
        super(start, end, interval);
        setDay(1);
        setDayOfWeek(1);
        setWeekOfMonth(1);
    }

    // Schedule value interface methods
    public String getScheduleString()
    {
        StringBuffer sb = new StringBuffer();

        if (getInterval() > 1) {
            sb.append("Every " +
                      getNumberWithSuffix(getInterval()) +
                      " month on the ");
        } else {
                sb.append("Monthly, on the ");
        }
        
        if (isOffset()) {
            sb.append(getNumberWithSuffix(getWeekOfMonth()));
            
            switch(getDayOfWeek()) {
              case Calendar.MONDAY:
                sb.append(" Monday @ ");
                break;
              case Calendar.TUESDAY:
                sb.append(" Tuesday @ ");
                break;
              case Calendar.WEDNESDAY:
                sb.append(" Wednesday @ ");
                break;
              case Calendar.THURSDAY:
                sb.append(" Thursday @ ");
                break;
              case Calendar.FRIDAY:
                sb.append(" Friday @ ");
                break;
              case Calendar.SATURDAY:
                sb.append(" Saturday @ ");
                break;
              case Calendar.SUNDAY:
                sb.append(" Sunday @ ");
              default:
                // shouldn't happen
            }
        } else {
            sb.append(getNumberWithSuffix(getDay()) + " @ ");
        }
         
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        sb.append(df.format(this.getStart()));
        
        return sb.toString();
    }

    // Attribute Setters
    public void setDay(int day)
    {
        this.offset = false;
        this.day = day;
    }

    public void setDayOfWeek(int dayOfWeek)
    {
        this.offset = true;
        this.dayOfWeek = dayOfWeek;
    }

    public void setWeekOfMonth(int weekOfMonth)
    {
        this.offset = true;
        this.weekOfMonth = weekOfMonth;
    }

    // Attribute Getters
    public int getDay()
    {
        return this.day;
    }

    public int getDayOfWeek()
    {
        return this.dayOfWeek;
    }

    public int getWeekOfMonth()
    {
        return this.weekOfMonth;
    }

    public boolean isOffset()
    {   
        return this.offset;
    }
}
