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

import java.util.Date;
import java.text.DateFormat;

public class ScheduleDailyValue extends ScheduleValue {

    // Flag that tells the scheduler to recur only on weekdays
    private boolean everyWeekday;

    public ScheduleDailyValue(Date start, Date end, int interval)
    {
        super(start, end, interval);
        this.everyWeekday = false;
    }

    // Schedule value interface methods
    public String getScheduleString()
    {
        StringBuffer sb = new StringBuffer();

        if (this.everyWeekday) {
            sb.append("Every Weekday @ ");
        } else if (this.getInterval() > 1) {
            sb.append("Every " + 
                      this.getNumberWithSuffix(this.getInterval()) +
                      " day @ ");
        } else {
            sb.append("Daily @ ");
        }

        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        sb.append(df.format(this.getStart()));

        return sb.toString();
    }

    // Attribute setters
    public void setEveryWeekDay(boolean arg)
    {
        this.everyWeekday = arg;
    }

    // Attribute getters
    public boolean getEveryWeekDay()
    {
        return this.everyWeekday;
    }
    
    public String toString() 
    {
        StringBuffer sb  = new StringBuffer("(");
        sb.append(" everyWeekDay = " + this.getEveryWeekDay() );
        sb.append(" getScheduleString = " + this.getScheduleString());
        sb.append(" )");
        return sb.toString();
    }
}

        
