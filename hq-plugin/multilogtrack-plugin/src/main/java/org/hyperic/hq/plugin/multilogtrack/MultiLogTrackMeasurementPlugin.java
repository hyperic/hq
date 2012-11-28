/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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

package org.hyperic.hq.plugin.multilogtrack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.TimeUtil;

public class MultiLogTrackMeasurementPlugin extends MeasurementPlugin {
    
    public static final Map<String, Map<String, TimeValuePair>> numLinesPerPattern =
        new HashMap<String, Map<String, TimeValuePair>>();
    public static final Map<String, Map<String, TimeValuePair>> lastLinesPerPattern =
        new HashMap<String, Map<String, TimeValuePair>>();

    @Override
    public MetricValue getValue(Metric metric) throws MetricUnreachableException {
        String pattern = metric.getObjectProperty("logfilepattern");
        String alias = metric.getAttributeName();
        String logfilePattern = (pattern == null) ? "" : pattern;
        String basedir = metric.getObjectProperty("basedir");
        String includepattern = metric.getObjectProperty("includepattern");
        boolean overrideChecks = Boolean.parseBoolean(metric.getObjectProperty(MultiLogTrackServerDetector.OVERRIDE_CHECKS));
        basedir = MultiLogTrackServerDetector.getBasedir(basedir);
        List<String> files = MultiLogTrackServerDetector.getFilesCached(logfilePattern, basedir, includepattern, false);
        if (metric.isAvail()) {
            if (!overrideChecks && (files == null || files.isEmpty())) {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
            return new MetricValue(Metric.AVAIL_UP);
        } else if (alias.equals("NumCapturedLogs")) {
            TimeValuePair num = getNumLinesAndClear(MultiLogTrackPlugin.INCLUDE_PATTERN, basedir, logfilePattern, includepattern);
            MetricValue rtn = num == null ? new MetricValue(0) : new MetricValue(num.getVal());
            return rtn;
        } else if (alias.equals("SecondaryNumCapturedLogs")) {
            TimeValuePair num = getNumLinesAndClear(MultiLogTrackPlugin.INCLUDE_PATTERN_2, basedir, logfilePattern, includepattern);
            MetricValue rtn = num == null ? new MetricValue(0) : new MetricValue(num.getVal());
            return rtn;
        } else if (alias.equals("DiffNumCapturedLogs")) {
            TimeValuePair tl = getNumLinesAndClear(MultiLogTrackPlugin.INCLUDE_PATTERN, basedir, logfilePattern, includepattern);
            TimeValuePair tl2 = getNumLinesAndClear(MultiLogTrackPlugin.INCLUDE_PATTERN_2, basedir, logfilePattern, includepattern);
            MetricValue diff = getDiff(tl, tl2);
            return diff;
        }
        if (basedir != null) {
            final File f = new File(basedir);
            try {
                if (!f.canRead() && f.exists()) {
                    throw new MetricUnreachableException("basedir=" + basedir + " exists but is not readable by the agent user");
                } else if (overrideChecks) {
                    // do nothing
                } else if (!f.isDirectory()) {
                    throw new MetricUnreachableException("basedir=" + basedir + " exists but is not a directory");
                } else if (!f.exists()) {
                    throw new MetricUnreachableException("basedir=" + basedir + " does not exist");
                }
            } catch (Exception e) {
                throw new MetricUnreachableException("unexpected error while attempting to read the basedir: " + e, e);
            }
        }
        if (!overrideChecks && (files == null || files.isEmpty())) {
            throw new MetricUnreachableException("no files were matched from the logfilepattern");
        }
        return new MetricValue(files.size());
    }

    private MetricValue getDiff(TimeValuePair tl1, TimeValuePair tl2) {
        Long time1 = (tl1 == null) ? 0l: tl1.getTime();
        Long time2 = (tl2 == null) ? 0l : tl2.getTime();
        int val = time1.compareTo(time2);
        return (val <= 0) ? new MetricValue(0) : new MetricValue(1);
    }

    private static TimeValuePair getNumLinesAndClear(String property, String basedir, String logpattern, String includePattern) {
        String key = basedir + "," + logpattern + "," + includePattern;
        TimeValuePair last = getLastCachedValue(property, key);
        if (last != null) {
            return last;
        }
        Map<String, TimeValuePair> numLines = null;
        synchronized (numLinesPerPattern) {
            numLines = numLinesPerPattern.get(property);
            if (numLines == null) {
                numLines = new HashMap<String, TimeValuePair>();
                numLinesPerPattern.put(property, numLines);
            }
        }
        TimeValuePair rtn = null;
        try {
            synchronized (numLines) {
                rtn = numLines.remove(key);
                return rtn;
            }
        } finally {
            synchronized (lastLinesPerPattern) {
                Map<String, TimeValuePair> map = lastLinesPerPattern.get(property);
                if (map == null) {
                    map = new HashMap<String, TimeValuePair>();
                    lastLinesPerPattern.put(property, map);
                }
                if (rtn != null) {
                    rtn.setLastUsed(now());
                }
                map.put(key, rtn);
            }
        }
    }
    
    private static TimeValuePair getLastCachedValue(String property, String key) {
        synchronized (lastLinesPerPattern) {
            Map<String, TimeValuePair> map = lastLinesPerPattern.get(property);
            if (map == null) {
                map = new HashMap<String, TimeValuePair>();
                lastLinesPerPattern.put(property, map);
            }
            TimeValuePair rtn = map.get(key);
            if (rtn == null) {
                return null;
            }
            long lastCacheUsed = rtn.getLastUsed();
            lastCacheUsed = roundDownTime(lastCacheUsed, 60000);
            long now = now();
            long nowRounded = roundDownTime(now, 60000);
            if (lastCacheUsed > 0 && lastCacheUsed + 60000 <= nowRounded) {
                map.remove(key);
                return null;
            }
            rtn.setLastUsed(now);
            return rtn;
        }
    }

    private static long roundDownTime(long approxTime, long interval) {
        return approxTime - (approxTime % interval);
    }

    private static Long now() {
        return System.currentTimeMillis();
    }

    static void incrementNumLines(String property, String basedir, String logfilepattern, String includepattern, int offset) {
        String key = basedir + "," + logfilepattern + "," + includepattern;
        Map<String, TimeValuePair> numLines = null;
        synchronized (numLinesPerPattern) {
            numLines = numLinesPerPattern.get(property);
            if (numLines == null) {
                numLines = new HashMap<String, TimeValuePair>();
                numLinesPerPattern.put(property, numLines);
            }
        }
        synchronized (numLines) {
            TimeValuePair num = numLines.get(key);
            if (num == null) {
                num = new TimeValuePair(now()+offset, 1);
                numLines.put(key, num);
            } else {
                num.increment();
	            num.setTime(now());
            }
        }
    }

    private static class TimeValuePair {
        private Long time;
        private AtomicInteger val;
        private long lastUsed = -1;
        private TimeValuePair(Long time, Integer val) {
            this.time = time;
            this.val = new AtomicInteger(1);
        }
        public long getLastUsed() {
            return lastUsed;
        }
        public void setLastUsed(long lastUsed) {
            this.lastUsed = lastUsed;
        }
        public void setTime(Long time) {
            this.time = time;
        }
        public void increment() {
            val.incrementAndGet();
        }
        private Integer getVal() {
            return val.get();
        }
        private Long getTime() {
            return time;
        }
        public String toString() {
            return "time=" + TimeUtil.toString(time) + ", val=" + val.get();
        }
    }

}