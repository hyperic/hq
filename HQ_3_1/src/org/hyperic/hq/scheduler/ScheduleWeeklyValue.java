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

import java.util.Calendar;
import java.util.Date;
import java.text.DateFormat;

public class ScheduleWeeklyValue extends ScheduleValue       
{
    // Bitmask that holds which days of the week to recur on
    private int days;

    public ScheduleWeeklyValue(Date start, Date end, int interval)
    {
        super(start, end, interval);
        this.days = 0;
    }

    // Schedule value interface methods
    public String getScheduleString() 
    {
        StringBuffer sb = new StringBuffer();

        if (this.getInterval() > 1) {
            sb.append("Every " +
                      this.getNumberWithSuffix(this.getInterval()) +
                      " week on ");
        } else {
            sb.append("Weekly on ");
        }

        // At least one of these will be set
        if (this.isDaySet(Calendar.MONDAY))
            sb.append("Mon,");
        if (this.isDaySet(Calendar.TUESDAY))
            sb.append("Tue,");
        if (this.isDaySet(Calendar.WEDNESDAY))
            sb.append("Wed,");
        if (this.isDaySet(Calendar.THURSDAY))
            sb.append("Thu,");
        if (this.isDaySet(Calendar.FRIDAY))
            sb.append("Fri,");
        if (this.isDaySet(Calendar.SATURDAY))
            sb.append("Sat,");
        if (this.isDaySet(Calendar.SUNDAY))
            sb.append("Sun,");

        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append(" @ ");

        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        sb.append(df.format(this.getStart()));

        return sb.toString();
    }

    public void setDay(int day)
    {
        this.days |= 1 << day;
    }

    public boolean isDaySet(int day) 
    {
        return (((1 << day) & this.days) != 0);
    }
}
