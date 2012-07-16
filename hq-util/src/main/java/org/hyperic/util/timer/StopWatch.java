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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hyperic.util.StringUtil;

public class StopWatch {

    private long _start;
    private long _end;
    private Map<String, LinkedList<TimeSlice>> _markerMap;
    
    public StopWatch() {
        reset();
    }

    public void markTimeBegin (String marker) {
        LinkedList<TimeSlice> list;
        if (null == (list = _markerMap.get(marker))) {
            list = new LinkedList<TimeSlice>();
            _markerMap.put(marker, list);
        }
        list.add(new TimeSlice(marker));
    }

    public void markTimeEnd (String marker) {
        final LinkedList<TimeSlice> list = _markerMap.get(marker);
        if (list == null) {
            throw new IllegalArgumentException("Invalid marker");
        }
        final TimeSlice ts = list.getLast();
        ts.setFinished();
    }

    public StopWatch(long start) {
        _start = start;
        _markerMap = new HashMap<String, LinkedList<TimeSlice>>();
    }

    public long reset() {
        try {
            return this.getElapsed();
        } finally {
            _start = now();
            _markerMap = new HashMap<String, LinkedList<TimeSlice>>();
        }
    }
    
    private final long now() {
        return System.currentTimeMillis();
    }
    
    public long getElapsed() {
        _end = now();
        return _end - _start;
    }

    public String toString() {
        final long elap = getElapsed();
        final StringBuilder buf = new StringBuilder(64);
        buf.append(elap).append(" ms");
        if (_markerMap.size() > 0) {
            buf.append(" { Markers: ");
            for (Entry<String, LinkedList<TimeSlice>> entry : _markerMap.entrySet()) {
                final String marker = entry.getKey();
                final LinkedList<TimeSlice> list = entry.getValue();
                writeBuf(marker, list, buf);
            }
            buf.append(" } ");
        }
        return buf.toString();
    }

    private void writeBuf(String marker, List<TimeSlice> tsList, StringBuilder buf) {
        long total = -1;
        for (final TimeSlice ts : tsList) {
            final Long elapsed = ts.getElapsed();
            if (elapsed == null) {
                continue;
            }
            if (total == -1) {
                total = 0;
            }
            total += elapsed.longValue();
        }
        buf.append(" [").append(marker).append("=")
           .append((total == -1) ? "null" : total + " ms")
           .append("]");
    }

    class TimeSlice {
        String _marker;
        long _begin;
        long _end;
        public TimeSlice (String marker) {
            _marker = marker;
            _begin = now();
            _end = 0;
        }
        public void setFinished () {
            _end= now();
        }
        public Long getElapsed() {
            if (_end == 0) {
                return now();
            }
            return new Long(_end - _begin);
        }
    }
}
