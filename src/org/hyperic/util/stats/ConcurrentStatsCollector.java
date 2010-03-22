/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.util.stats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public final class ConcurrentStatsCollector {
    private final Log _log = LogFactory.getLog(ConcurrentStatsCollector.class);
    private static final String BASE_FILENAME = "hqstats";
    private String _currFilename;
    private FileWriter _file;
    private final String _baseDir;
    private final ConcurrentLinkedQueue _queue = new ConcurrentLinkedQueue();
    private final ScheduledThreadPoolExecutor _executor =
        new ScheduledThreadPoolExecutor(1);
    private static final ConcurrentStatsCollector _instance =
        new ConcurrentStatsCollector();
    public static final int WRITE_PERIOD = 15;
    private final Sigar _sigar = new Sigar();
    private Long _pid;
    public static final String JVM_TOTAL_MEMORY = "JVM_TOTAL_MEMORY",
        JVM_FREE_MEMORY              = "JVM_FREE_MEMORY",
        JVM_MAX_MEMORY               = "JVM_MAX_MEMORY",
        FIRE_ALERT_TIME              = "FIRE_ALERT_TIME",
        EVENT_PROCESSING_TIME        = "EVENT_PROCESSING_TIME",
        GALERT_FIRED_EVENT           = "GALERT_FIRED_EVENT",
        EHCACHE_TOTAL_OBJECTS        = "EHCACHE_TOTAL_OBJECTS",
        CONCURRENT_STATS_COLLECTOR   = "CONCURRENT_STATS_COLLECTOR",
        LATHER_NUMBER_OF_CONNECTIONS = "LATHER_NUMBER_OF_CONNECTIONS",
        RUNTIME_PLATFORM_AND_SERVER_MERGER = "RUNTIME_PLATFORM_AND_SERVER_MERGER",
        SIGAR_1MLOAD                 = "SIGAR_1MLOAD",
        SIGAR_CPU                    = "SIGAR_CPU",
        AVAIL_MANAGER_METRICS_INSERTED = "AVAIL_MANAGER_METRICS_INSERTED",
        DATA_MANAGER_INSERT_TIME     = "DATA_MANAGER_INSERT_TIME",
        SIGAR_PROC_RES_MEM           = "SIGAR_PROC_RES_MEM",
        SIGAR_TCP_INERRS             = "SIGAR_TCP_INERRS",
        SIGAR_TCP_RETRANS            = "SIGAR_TCP_RETRANS",
        SIGAR_PAGEOUT                = "SIGAR_PAGEOUT",
        SIGAR_PAGEIN                 = "SIGAR_PAGEIN",
        JMS_QUEUE_PUBLISH_TIME       = "JMS_QUEUE_PUBLISH_TIME",
        JMS_TOPIC_PUBLISH_TIME       = "JMS_TOPIC_PUBLISH_TIME",
        METRIC_DATA_COMPRESS_TIME    = "METRIC_DATA_COMPRESS_TIME",
        DB_ANALYZE_TIME              = "DB_ANALYZE_TIME",
        PURGE_EVENT_LOGS_TIME        = "PURGE_EVENT_LOGS_TIME",
        PURGE_MEASUREMENTS_TIME      = "PURGE_MEASUREMENTS_TIME",
        MEASUREMENT_SCHEDULE_TIME    = "MEASUREMENT_SCHEDULE_TIME",
        SEND_ALERT_TIME              = "SEND_ALERT_TIME",
        ZEVENT_QUEUE_SIZE            = "ZEVENT_QUEUE_SIZE",
        TRIGGER_INIT_TIME            = "TRIGGER_INIT_TIME",
        FIRED_ALERT_TIME             = "FIRED_ALERT_TIME";
    // using tree due to ordering capabilities
    private final Map _statKeys = new TreeMap();
    private AtomicBoolean _hasStarted = new AtomicBoolean(false);
    private final MBeanServer _mbeanServer;

    private ConcurrentStatsCollector() {
    	final char fs = File.separatorChar;
    	String unittestPropStringVal =  System.getProperty("hq.unittest.run");
    	boolean inUnittestEnv =
    		unittestPropStringVal == null ? false :
    			(new Boolean(unittestPropStringVal)).booleanValue();
    	if (!inUnittestEnv) {
        	final String d =
        		HQApp.getInstance().getRestartStorageDir().getAbsolutePath();
        	final String jbossLogSuffix =
        		"server" + fs + "default" + fs + "log" + fs + "hqstats" + fs;
        	_baseDir = d + fs + jbossLogSuffix;
        	_log.info("using hqstats baseDir " + _baseDir);
        	final File dir = new File(_baseDir);
        	if (!dir.exists()) {
        		dir.mkdir();
        	}
    		_mbeanServer = MBeanUtil.getMBeanServer();
    		registerInternalStats();
    	} else {
    		_mbeanServer = null;
    		_baseDir = null;
    	}
    }

    public final void register(final String statId) {
        // can't register any stats after the collector has been initially
        // started due to consistent ordering in the csv output file
        if (_hasStarted.get()) {
            return;
        }
        if (_statKeys.containsKey(statId)) {
            return;
        }
        _statKeys.put(statId, null);
    }

    public final void register(final StatCollector stat) {
        // can't register any stats after the collector has been initially
        // started due to the need for consistent ordering in the csv output file
        if (_hasStarted.get()) {
            _log.warn(stat.getId() +
                " attempted to register in stat collector although it has " +
                " already been started, not allowing");
            return;
        }
        if (_statKeys.containsKey(stat.getId())) {
            _log.warn(stat.getId() +
                " attempted to register in stat collector although it has " +
                " already been registered, not allowing");
            return;
        }
        _statKeys.put(stat.getId(), stat);
    }

    public final void startCollector() {
        if (_hasStarted.get()) {
            return;
        }
        _currFilename = getFilename(false);
        cleanupFilename(_currFilename);
        File file = new File(_currFilename);
        try {
            if (file.exists()) {
                String mvFilename = getFilename(true);
                file.renameTo(new File(mvFilename));
                gzipFile(mvFilename);
            }
            if (_file != null) {
                _file.close();
            }
            _file = new FileWriter(_currFilename, true);
        } catch (IOException e) {
            _log.error(e.getMessage(), e);
        }
        printHeader();
        _executor.scheduleWithFixedDelay(
            new StatsWriter(), WRITE_PERIOD, WRITE_PERIOD, TimeUnit.SECONDS);
        _hasStarted.set(true);
        _log.info("ConcurrentStatsCollector has started");
    }

    private final void cleanupFilename(String filename) {
        final File file = new File(filename);
        final File path = file.getParentFile();
        final String name = file.getName();
        final FilenameFilter filter = new FilenameFilter() {
            final String _filename = name;
            public boolean accept(File dir, String name) {
                if (dir.equals(path) && name.startsWith(_filename)) {
                    return true;
                }
                return false;
            }
        };
        final File[] files = path.listFiles(filter);
        final long oneWeekAgo =
            System.currentTimeMillis() - (7*MeasurementConstants.DAY);
        for (int i=0; i<files.length; i++) {
            if (files[i].lastModified() < oneWeekAgo) {
                files[i].delete();
            }
        }
    }

    private final void printHeader() {
        final StringBuilder buf = new StringBuilder("timestamp,");
        final String countAppend = "_COUNT";
        for (Iterator it=_statKeys.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            final String key = (String)entry.getKey();
            final StatCollector value = (StatCollector)entry.getValue();
            buf.append(key).append(',');
            // Only print the COUNT column if the StatCollector object doesn't
            // exist.
            // This means that the stat counts come in asynchronously rather
            // than begin calculated every interval.
            if (value == null) {
                buf.append(key).append(countAppend).append(',');
            }
        }
        try {
            _file.append(buf.append("\n").toString());
            _file.flush();
        } catch (IOException e) {
            _log.warn(e.getMessage(), e);
        }
    }

    private final String getFilename(boolean withTimestamp) {
        final Calendar cal = Calendar.getInstance();
        final int month = 1 + cal.get(Calendar.MONTH);
        final String monthStr = (month < 10) ? "0"+month : String.valueOf(month);
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        final String dayStr = (day < 10) ? "0"+day : String.valueOf(day);
        String rtn = BASE_FILENAME+"-"+monthStr+"-"+dayStr;
        if (withTimestamp) {
            final int hour = cal.get(Calendar.HOUR_OF_DAY);
            final String hourStr = (hour < 10) ? "0"+hour : String.valueOf(hour);
            final int min = cal.get(Calendar.MINUTE);
            final String minStr = (min < 10) ? "0"+min : String.valueOf(min);
            final int sec = cal.get(Calendar.SECOND);
            final String secStr = (sec < 10) ? "0"+sec : String.valueOf(sec);
            rtn = rtn+"-"+hourStr+"."+minStr+"."+secStr;
        }
        return _baseDir + rtn + ".csv";
    }

    public static final ConcurrentStatsCollector getInstance() {
        return _instance;
    }

    public final void addStat(final long value, final String id) {
        if (!_statKeys.containsKey(id)) {
            return;
        }
        final long now = System.currentTimeMillis();
        // may want to look at pooling these objects to avoid creation overhead
        final StatsObject stat = new StatsObject(value, id);
        _queue.add(stat);
        final long total = System.currentTimeMillis() - now;
        _queue.add(new StatsObject(total, CONCURRENT_STATS_COLLECTOR));
    }

    private class StatsWriter implements Runnable {
        public synchronized void run() {
            try {
                Map stats = getStatsByKey();
                StringBuilder buf = getCSVBuf(stats);
                final FileWriter fw = getFileWriter();
                fw.append(buf.append("\n").toString());
                fw.flush();
            } catch (Throwable e) {
                _log.warn(e.getMessage(), e);
            }
        }
        private FileWriter getFileWriter() throws IOException {
            final String filename = getFilename(false);
            if (!_currFilename.equals(filename)) {
                _file.close();
                gzipFile(_currFilename);
                _file = new FileWriter(filename, true);
                printHeader();
                _currFilename = filename;
                cleanupFilename(_currFilename);
            }
            return _file;
        }
        private final StringBuilder getCSVBuf(Map stats) {
            final StringBuilder rtn = new StringBuilder();
            rtn.append(System.currentTimeMillis()).append(',');
            for (Iterator it=_statKeys.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                String key = (String)entry.getKey();
                StatCollector stat = (StatCollector)entry.getValue();
                if (stat != null) {
                    long value;
                    try {
                        value = stat.getVal();
                        rtn.append(value).append(',');
                    } catch (StatUnreachableException e) {
                        if (_log.isDebugEnabled()) {
                            _log.debug(e.getMessage(), e);
                        }
                        rtn.append(',');
                        continue;
                    }
                } else {
                    List list = (List)stats.get(key);
                    long total = 0l;
                    if (list != null) {
                        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                            Number val = (Number)iter.next();
                            total += val.longValue();
                        }
                        rtn.append(total).append(',')
                           .append(list.size()).append(",");
                    } else {
                        rtn.append(',').append(',');
                    }
                }
            }
            return rtn;
        }
        private Map getStatsByKey() {
            final Map rtn = new HashMap();
            final Object marker = new Object();
            _queue.add(marker);
            List tmp;
            Object obj;
            while (marker != (obj = _queue.poll())) {
                final StatsObject stat = (StatsObject)obj;
                final String id = stat.getId();
                final long val = stat.getVal();
                if (null == (tmp = (List)rtn.get(id))) {
                    tmp = new ArrayList();
                    rtn.put(id, tmp);
                }
                tmp.add(new Long(val));
            }
            return rtn;
        }
    }
    private void gzipFile(final String filename) {
        new Thread() {
            public void run() {
                FileOutputStream gfile = null;
                GZIPOutputStream gstream = null;
                PrintStream pstream = null;
                boolean succeed = false;
                try {
                    gfile = new FileOutputStream(filename+".gz");
                    gstream = new GZIPOutputStream(gfile);
                    pstream = new PrintStream(gstream);
                    BufferedReader reader =
                        new BufferedReader(new FileReader(filename));
                    String tmp;
                    while (null != (tmp = reader.readLine())) {
                        pstream.append(tmp).append("\n");
                    }
                    gstream.finish();
                    succeed = true;
                } catch (IOException e) {
                    _log.warn(e.getMessage(), e);
                } finally {
                    close(gfile);
                    close(gstream);
                    close(pstream);
                    if (succeed) {
                        new File(filename).delete();
                    } else {
                        new File(filename+".gz").delete();
                    }
                }
            }
            private void close(OutputStream s) {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        _log.warn(e.getMessage(), e);
                    }
                }
            }
        }.start();
    }

    private final void registerInternalStats() {
        register(new StatCollector() {
            private final CacheManager _cMan = CacheManager.getInstance();
            public String getId() {
                return EHCACHE_TOTAL_OBJECTS;
            }
            public long getVal() {
                long rtn = 0l;
                String[] caches = _cMan.getCacheNames();
                for (int i=0; i<caches.length; i++) {
                    rtn += _cMan.getCache(caches[i]).getStatistics().getObjectCount();
                }
                return rtn;
            }
        });
        register(new StatCollector() {
            public String getId() {
                return JVM_TOTAL_MEMORY;
            }
            public long getVal() {
                return Runtime.getRuntime().totalMemory();
            }
        });
        register(new StatCollector() {
            public String getId() {
                return JVM_FREE_MEMORY;
            }
            public long getVal() {
                return Runtime.getRuntime().freeMemory();
            }
        });
        register(new StatCollector() {
            public String getId() {
                return JVM_MAX_MEMORY;
            }
            public long getVal() {
                return Runtime.getRuntime().maxMemory();
            }
        });
        register(new StatCollector() {
            public String getId() {
                return SIGAR_1MLOAD;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    return (long)_sigar.getLoadAverage()[0];
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new StatCollector() {
            public String getId() {
                return SIGAR_CPU;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    return (long)_sigar.getProcCpu(getProcPid()).getPercent();
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new StatCollector() {
            public String getId() {
                return SIGAR_PROC_RES_MEM;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    return (long)_sigar.getProcMem(getProcPid()).getResident();
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new StatCollector() {
            private long last = 0;
            public String getId() {
                return SIGAR_TCP_INERRS;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    long curr = _sigar.getTcp().getInErrs();
                    long rtn =  curr - last;
                    last = curr;
                    return rtn;
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new StatCollector() {
            private long last = 0;
            public String getId() {
                return SIGAR_TCP_RETRANS;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    long curr = _sigar.getTcp().getRetransSegs();
                    long rtn =  curr - last;
                    last = curr;
                    return rtn;
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new StatCollector() {
            private long last = 0;
            public String getId() {
                return SIGAR_PAGEOUT;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    long curr = _sigar.getSwap().getPageOut();
                    long rtn =  curr - last;
                    last = curr;
                    return rtn;
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new StatCollector() {
            private long last = 0;
            public String getId() {
                return SIGAR_PAGEIN;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    long curr = _sigar.getSwap().getPageIn();
                    long rtn =  curr - last;
                    last = curr;
                    return rtn;
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });

        register(new MBeanCollector("HIBERNATE_2ND_LEVEL_CACHE_HITS",
            "Hibernate:type=statistics,application=hq",
            "SecondLevelCacheHitCount", true));

        register(new MBeanCollector("HIBERNATE_2ND_LEVEL_CACHE_MISSES",
            "Hibernate:type=statistics,application=hq",
            "SecondLevelCacheMissCount", true));

        register(new MBeanCollector("HIBERNATE_QUERY_CACHE_MISSES",
            "Hibernate:type=statistics,application=hq",
            "QueryCacheMissCount", true));

        register(new MBeanCollector("HIBERNATE_QUERY_CACHE_HITS",
            "Hibernate:type=statistics,application=hq",
            "QueryCacheHitCount", true));

        register(new MBeanCollector("ZEVENTS_PROCESSED",
            "hyperic.jmx:name=HQInternal", "ZeventsProcessed", true));

        register(new MBeanCollector("ZEVENT_QUEUE_SIZE",
            "hyperic.jmx:name=HQInternal", "ZeventQueueSize", false));

        register(new MBeanCollector("PLATFORM_COUNT",
            "hyperic.jmx:name=HQInternal", "PlatformCount", false));

        register(new MBeanCollector("JDBC_HQ_DS_AVAIL_CONNS",
            "jboss.jca:service=ManagedConnectionPool,name=HypericDS",
            "AvailableConnectionCount", false));

        register(new MBeanCollector("JDBC_HQ_DS_IN_USE",
            "jboss.jca:service=ManagedConnectionPool,name=HypericDS",
            "ConnectionCount", false));

        register(new MBeanCollector("JMS_EVENT_TOPIC",
            "jboss.mq.destination:service=Topic,name=eventsTopic",
            "AllMessageCount", false));

        register(new MBeanCollector(
            "EDEN_MEMORY_USED", "java.lang:type=MemoryPool,name=",
            new String[] {"Par Eden Space", "PS Eden Space", "Eden Space"},
            "Usage", "used"));

        register(new MBeanCollector(
            "SURVIVOR_MEMORY_USED", "java.lang:type=MemoryPool,name=",
            new String[] {"Par Survivor Space", "PS Survivor Space",
                          "Survivor Space"},
            "Usage", "used"));

        register(new MBeanCollector(
            "TENURED_MEMORY_USED", "java.lang:type=MemoryPool,name=",
            new String[] {"CMS Old Gen", "PS Old Gen", "Tenured Gen"},
            "Usage", "used"));

        register(new MBeanCollector(
            "HEAP_MEMORY_USED", "java.lang:type=Memory",
            new String[] {""}, "HeapMemoryUsage", "used"));

        register(new MBeanCollector(
            "JVM_MARKSWEEP_GC", "java.lang:type=GarbageCollector,name=",
            new String[] {"ConcurrentMarkSweep", "PS MarkSweep"},
            "CollectionTime", true));

        register(new MBeanCollector(
            "JVM_COPY_GC", "java.lang:type=GarbageCollector,name=",
            new String[] {"Copy", "ParNew", "PS Scavenge"}, "CollectionTime",
            true));

        register(LATHER_NUMBER_OF_CONNECTIONS);
    	register(GALERT_FIRED_EVENT);
        register(CONCURRENT_STATS_COLLECTOR);
    }

    private long getProcPid() {
        if (_pid != null) {
            return _pid.longValue();
        }
        _pid = new Long(_sigar.getPid());
        return _pid.longValue();
    }

    private class MBeanCollector implements StatCollector {
        private final String _id,
                             _objectName,
                             _attrName,
                             _valProp;
        private final String [] _names;
        private StatUnreachableException _ex = null;
        private int failures = 0;
        private final boolean _isComposite,
                              _isTrend;
        private long _last = 0l;

        MBeanCollector(String id, String objectName, String[] names,
                       String attrName, boolean trend) {
            _id = id;
            _objectName = objectName;
            _attrName = attrName;
            _names = names;
            _valProp = null;
            _isComposite = false;
            _isTrend = trend;
        }

        MBeanCollector(String id, String objectName, String attrName,
                       boolean trend) {
            _id = id;
            _objectName = objectName;
            _attrName = attrName;
            _names = new String[] {""};
            _valProp = null;
            _isComposite = false;
            _isTrend = trend;
        }

        MBeanCollector(String id, String objectName, String[] names,
                       String attrName, String valProp) {
            _names = names;
            _id = id;
            _objectName = objectName;
            _attrName = attrName;
            _valProp = valProp;
            _isComposite = true;
            _isTrend = false;
        }

        public final String getId() {
            return _id;
        }

        public final long getVal() throws StatUnreachableException {
            // no need to keep generating a new exception.  If it fails
            // 10 times, assume that the mbean server is not on.
            if (_ex != null && failures >= 10) {
                throw _ex;
            }
            Exception exception = null;
            for (int i=0; i<_names.length; i++) {
                try {
                    if (_isComposite) {
                        long val = getComposite(_names[i]);
                        return (_isTrend) ? (_last = val - _last) : val;
                    } else {
                        long val = getValue(_names[i]);
                        return (_isTrend) ? (_last = val - _last) : val;
                    }
                } catch (Exception e) {
                    exception = e;
                }
            }
            failures++;
            final String msg = _objectName + " query failed";
            if (exception == null) {
                _ex = new StatUnreachableException(msg);
                throw _ex;
            }
            _ex = new StatUnreachableException(msg, exception);
            throw _ex;
        }

        private final long getComposite(String name)
            throws StatUnreachableException,
                   AttributeNotFoundException,
                   InstanceNotFoundException,
                   MBeanException,
                   ReflectionException,
                   MalformedObjectNameException,
                   NullPointerException {
            final ObjectName objName = new ObjectName(_objectName + name);
            final CompositeDataSupport cds =
                (CompositeDataSupport)_mbeanServer.getAttribute(
                    objName, _attrName);
            final long val = ((Number)cds.get(_valProp)).longValue();
            failures = 0;
            return val;
        }

        private final long getValue(String name)
            throws MalformedObjectNameException,
                   NullPointerException,
                   AttributeNotFoundException,
                   InstanceNotFoundException,
                   MBeanException,
                   ReflectionException {
            final ObjectName _objName = new ObjectName(_objectName + name);
            long rtn = ((Number)_mbeanServer.getAttribute(
                _objName, _attrName)).longValue();
            failures = 0;
            return rtn;
        }
    }
}
