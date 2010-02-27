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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StopWatch {

    private long _start;
    private long _end;
    private Map _markerMap;
    
    public StopWatch() {
        reset();
    }

    public void markTimeBegin (String marker) {
        List list;
        if (null == (list = (List)_markerMap.get(marker))) {
            list = new ArrayList();
            _markerMap.put(marker, list);
        }
        list.add(new TimeSlice(marker));
    }

    public void markTimeEnd (String marker) {
        if (!_markerMap.containsKey(marker)) {
            throw new IllegalArgumentException("Invalid marker");
        }
        List list = (List)_markerMap.get(marker);
        TimeSlice ts = (TimeSlice)list.get(list.size()-1);
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
            _start = now();
            _markerMap = new HashMap();
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
        long elap = this.getElapsed();

        StringBuffer buf = new StringBuffer();
        buf.append(StringUtil.formatDuration(elap, 2, true));

        if (_markerMap.size() > 0) {
            buf.append(" { Markers: ");
            for (Iterator i=_markerMap.entrySet().iterator();i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                final String marker = (String)entry.getKey();
                final List list = (List)entry.getValue();
                writeBuf(marker, list, buf);
            }
            buf.append(" } ");
        }
        return buf.toString();
    }

    private void writeBuf(String marker, List tsList, StringBuffer buf) {
        long total = -1;
        for (Iterator it=tsList.iterator();it.hasNext();) {
            TimeSlice ts = (TimeSlice)it.next();
            Long elapsed = ts.getElapsed();
            if (elapsed == null) {
                continue;
            }
            total += elapsed.longValue();
        }
        buf.append(" [").append(marker).append("=")
           .append((total < 0) ? "null" : StringUtil.formatDuration(total, 2, true))
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
                return null;
            }
            return new Long(_end - _begin);
        }
    }
}
