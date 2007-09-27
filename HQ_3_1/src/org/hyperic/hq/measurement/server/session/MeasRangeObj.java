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

package org.hyperic.hq.measurement.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.TimeUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MeasRangeObj
{
    private static final String logCtx = MeasRangeObj.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);

    private static MeasRangeObj _onlyInst = new MeasRangeObj();
    private List _ranges = new ArrayList();

    private MeasRangeObj()
    {
        setRanges();
    }

    public static MeasRangeObj getInstance()
    {
        return _onlyInst;
    }

    private void setRanges()
    {
        Calendar cal = Calendar.getInstance();
        long currTime = System.currentTimeMillis();
        String currTable = MeasTabManagerUtil.getMeasTabname(cal, currTime);
        currTime = MeasTabManagerUtil.getMeasTabStartTime(cal, currTime);
        String table = currTable;
        long max = MeasTabManagerUtil.getMeasTabEndTime(cal, currTime);
        MeasRange range = new MeasRange(currTable, currTime, max);
        synchronized(_ranges)
        {
            do
            {
                _log.debug("loading measurement range -> "+range);
                _ranges.add(range);
                max = MeasTabManagerUtil.getMeasTabEndTime(cal, currTime);
                currTime = MeasTabManagerUtil.getPrevMeasTabTime(cal, currTime);
                currTime = MeasTabManagerUtil.getMeasTabStartTime(cal, currTime);
                table = MeasTabManagerUtil.getMeasTabname(cal, currTime);
                range = new MeasRange(table, currTime, max);
            }
            while (!currTable.equals(table));
        }
    }

    Map bucketData(List data)
    {
        HashMap buckets = new HashMap();
        List ranges = getRanges();
        for (Iterator it = data.iterator(); it.hasNext(); )
        {
            DataPoint pt = (DataPoint) it.next();
            String table = getTable(ranges, pt.getMetricValue().getTimestamp());
            List dpts;
            if (null == (dpts = (List)buckets.get(table))) {
                dpts = new ArrayList();
                buckets.put(table, dpts);
            }
            dpts.add(pt);
        }
        return buckets;
    }

    public List getRanges()
    {
        synchronized(_ranges)
        {
            MeasRange latestRange = (MeasRange)_ranges.get(0);
            long now = System.currentTimeMillis();
            if (now > latestRange.getMaxTimestamp())
            {
                _ranges.remove(_ranges.size()-1);
                MeasRange latest = getCurrentRange();
                _log.debug("loading measurement range -> "+latest);
                _ranges.add(0, latest);
            }
            return new ArrayList(_ranges);
        }
    }

    String getTable(List ranges, long timestamp)
    {
        for (Iterator i=ranges.iterator(); i.hasNext(); )
        {
            MeasRange range = (MeasRange)i.next();
            if (timestamp <= range.getMaxTimestamp() &&
                timestamp >= range.getMinTimestamp()) {
                return range.getTable();
            }
        }
        _log.debug("Could not find an appropriate range for "+TimeUtil.toString(timestamp));
        return MeasTabManagerUtil.getMeasTabname(timestamp);
    }

    private MeasRange getCurrentRange()
    {
        Calendar cal = Calendar.getInstance();
        long currTime = System.currentTimeMillis();
        String table = MeasTabManagerUtil.getMeasTabname(cal, currTime);
        long start   = MeasTabManagerUtil.getMeasTabStartTime(cal, currTime);
        long end     = MeasTabManagerUtil.getMeasTabEndTime(cal, currTime);
        return new MeasRange(table, start, end);
    }
}
