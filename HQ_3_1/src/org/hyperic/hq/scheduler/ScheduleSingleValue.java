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
import java.util.Date;

public class ScheduleSingleValue extends ScheduleValue {

    public ScheduleSingleValue(Date start)
    {
        super(start, null, 1);
    }

    // Schedule value abstract methods
    public String getScheduleString()
    {
        StringBuffer sb = new StringBuffer();

        DateFormat dfDate = DateFormat.getDateInstance(DateFormat.SHORT);
        DateFormat dfTime = DateFormat.getTimeInstance(DateFormat.SHORT);
        
        sb.append("Once, on ");
        sb.append(dfDate.format(this.getStart()));
        sb.append(" @ ");
        sb.append(dfTime.format(this.getStart()));

        return sb.toString();
    }
}
