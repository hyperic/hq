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

// -*- Mode: Java; indent-tabs-mode: nil; -*-

/*
 * MetricRange.java
 *
 */

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.Date;
import java.text.SimpleDateFormat;

public class MetricRange {

    public static final long SHIFT_RANGE = 60001;

    private Long begin;
    private Long end;

    public MetricRange() {
    }

    public MetricRange(Long b, Long e) {
        begin = b;
        end = e;
    }

    public Long getBegin() {
        return begin;
    }

    public void setBegin(Long l) {
        begin = l;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long l) {
        end = l;
    }

    /**
     * If the end date is after "now", reset the end to "now" and recalculate
     * the range.
     */
    public void shiftNow() {
        if (getBegin() == null || getEnd() == null) {
            return;
        }

        // 16.03.2014 - change logic according to bug HHQ-5890     

    }

    /**
     * 
     * Return a date-formatted representation of the range.
     */
    public String getFormattedRange() {
        if (getBegin() == null || getEnd() == null) {
            return "{}";
        }

        Date bd = new Date(getBegin().longValue());
        Date ed = new Date(getEnd().longValue());

        SimpleDateFormat df = new SimpleDateFormat();

        return df.format(bd) + " to " + df.format(ed);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("{");

        buf.append("begin=").append(begin);
        buf.append(" end=").append(end);

        return buf.append("}").toString();
    }
}
