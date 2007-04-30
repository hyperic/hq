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

import org.hyperic.util.StringUtil;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class StopWatch {

    private long _start;
    private long _end;
    private Map _markerMap;
    
    public StopWatch() {
        reset();
    }

    public void markTimeBegin (String marker) {
        if (_markerMap.containsKey(marker)) {
            TimeSlice ts = (TimeSlice)_markerMap.get(marker);
            ts.cont();
        } else {
            _markerMap.put(marker, new TimeSlice(marker));
        }
    }

    public void markTimeEnd (String marker) {
        if (!_markerMap.containsKey(marker)) {
            throw new IllegalArgumentException("Invalid marker");
        }
        TimeSlice ts = (TimeSlice)_markerMap.get(marker);
        ts.setFinished();
    }

    public StopWatch(long start) {
        _start = start;
        _markerMap = new HashMap();
    }

    public long reset() {
        try {
            return this.getElapsed();
        } finally {
            _start = System.currentTimeMillis();
            _markerMap = new HashMap();
        }
    }
    
    public long getElapsed() {
        _end = System.currentTimeMillis();
        return _end - _start;
    }

    public String toString() {
        long elap = this.getElapsed();

        StringBuffer buf = new StringBuffer();
        buf.append(StringUtil.formatDuration(elap, 2, true));

        if (_markerMap.size() > 0) {
            buf.append(" { Markers: ");
            for (Iterator i=_markerMap.values().iterator();i.hasNext();) {
                TimeSlice ts = (TimeSlice)i.next();
                ts.writeBuf(buf);
            }
            buf.append(" } ");
        }
        return buf.toString();
    }

    class TimeSlice {
        String _marker;
        long _begin;
        long _end;

        public TimeSlice (String marker) {
            _marker = marker;
            _begin = _end=System.currentTimeMillis();
        }

        public void setFinished () {
            _end= System.currentTimeMillis();
        }

        public void cont () {
            _begin -= (_end - _begin);
        }

        public void writeBuf(StringBuffer buf) {
            long elap = _end - _begin;
            buf.append(" [").append(_marker).append("=")
               .append(StringUtil.formatDuration(elap, 2, true))
               .append("]");
        }
    }
}
