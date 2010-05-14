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

public abstract class ScheduleValue implements java.io.Serializable {

    // Properties common to all recurrance types
    private String description;  // Schedule description
    private Date   start;        // Time to start
    private Date   end;          // Time to end, can be null
    private int    interval;     // Every x days, weeks or months

    protected ScheduleValue(Date start, Date end, int interval)
    {
        this.start = start;
        this.end = end;
        this.interval = interval;
    }

    // Abstract functions
    public abstract String getScheduleString();

    // Attribute getters
    public Date getStart()
    {
        return this.start;
    }

    public Date getEnd()
    {
        return this.end;
    }

    public int getInterval()
    {
        return this.interval;
    }

    public String getDescription()
    {
        return this.description;
    }

    // Attribute setters
    public void setDescription(String description)
    {
        this.description = description;
    }

    // Private helpers
    protected static String getNumberWithSuffix(int number)
    {
        if ((number > 3 && number < 21) || (number > 23 && number < 31)) {
            return number + "th";
        } else if (number == 1 || number == 21 || number == 31) {
            return number + "st";
        } else if (number == 3 || number == 23) {
            return number + "rd";
        } else {
            return number + "nd";
        }
    }
}
