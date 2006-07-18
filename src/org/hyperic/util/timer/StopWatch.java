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

package org.hyperic.util.timer;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class StopWatch {

    private long start;
    private long end;
    private Map markerMap;
    
    public StopWatch() {
        reset();
    }

    public void markTimeBegin (String marker) {
        if (markerMap.containsKey(marker)) {
            TimeSlice ts = (TimeSlice)markerMap.get(marker);
            ts.cont();
        } else {
            markerMap.put(marker, new TimeSlice(marker));
        }
    }

    public void markTimeEnd (String marker) {
        if (!markerMap.containsKey(marker)) {
            throw new IllegalArgumentException("Invalid marker");
        }
        TimeSlice ts = (TimeSlice)markerMap.get(marker);
        ts.setFinished();
    }

    public StopWatch(long start) {
        this.start = start;
        markerMap = new HashMap();
    }

    public long reset() {
        try {
            return this.getElapsed();
        } finally {
            start = System.currentTimeMillis();
            markerMap = new HashMap();
        }
    }
    
    public long getElapsed() {
        end = System.currentTimeMillis();
        return end - start;
    }

    public String toString() {
        long elap = this.getElapsed();

        String fraction = (elap % 1000) + "";
        int pad = 3 - fraction.length();

        StringBuffer buf = new StringBuffer()
            .append(elap / 1000).append('.');

        //for example, 15 millseconds formatted as ".015" rather than ".15"
        while (pad-- > 0) {
            buf.append("0");
        }
        buf.append(fraction);

        if (markerMap.size() > 0) {
            buf.append(" { Markers: ");
            for (Iterator i=markerMap.values().iterator();i.hasNext();) {
                TimeSlice ts = (TimeSlice)i.next();
                ts.writeBuf(buf);
            }
            buf.append(" } ");
        }
        return buf.toString();
    }

    class TimeSlice {
        String marker;
        long begin;
        long end;

        public TimeSlice (String marker) {
            this.marker = marker;
            this.begin=this.end=System.currentTimeMillis();
        }

        public void setFinished () {
            this.end= System.currentTimeMillis();
        }

        public void cont () {
            begin -= (end-begin);
        }

        public void writeBuf(StringBuffer buf) {
            long elap = end-begin;
            buf.append(" [").append(marker).append("=")
               .append(elap/1000).append('.').append(elap%1000).append("] ");
        }
    }
}
