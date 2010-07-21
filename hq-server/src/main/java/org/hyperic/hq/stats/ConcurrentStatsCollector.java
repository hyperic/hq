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

package org.hyperic.hq.stats;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.hyperic.util.stats.StatsObject;
import org.springframework.stereotype.Component;

@Component
public final class ConcurrentStatsCollector {
	private final Log _log = LogFactory.getLog(ConcurrentStatsCollector.class);

    // using tree due to ordering capabilities
    private final Map<String, StatCollector> _statKeys = new TreeMap<String, StatCollector>();
    private final MBeanServer _mbeanServer;
	private final ConcurrentLinkedQueue<StatsObject> _queue = new ConcurrentLinkedQueue<StatsObject>();
    private final Sigar _sigar = new Sigar();
    private static boolean enabled = true;
    private AtomicBoolean _hasStarted = new AtomicBoolean(false);
    private Long _pid;

    public static final int WRITE_PERIOD = 15;
    public static final String JVM_TOTAL_MEMORY = "JVM_TOTAL_MEMORY", 
    						   JVM_FREE_MEMORY = "JVM_FREE_MEMORY",
    						   JVM_MAX_MEMORY = "JVM_MAX_MEMORY", 
    						   FIRE_ALERT_TIME = "FIRE_ALERT_TIME",
    						   EVENT_PROCESSING_TIME = "EVENT_PROCESSING_TIME", 
    						   EHCACHE_TOTAL_OBJECTS = "EHCACHE_TOTAL_OBJECTS",
    						   CONCURRENT_STATS_COLLECTOR = "CONCURRENT_STATS_COLLECTOR",
    						   LATHER_NUMBER_OF_CONNECTIONS = "LATHER_NUMBER_OF_CONNECTIONS",
    						   RUNTIME_PLATFORM_AND_SERVER_MERGER = "RUNTIME_PLATFORM_AND_SERVER_MERGER", 
    						   SIGAR_1MLOAD = "SIGAR_1MLOAD",
    						   SIGAR_CPU = "SIGAR_CPU", 
    						   AVAIL_MANAGER_METRICS_INSERTED = "AVAIL_MANAGER_METRICS_INSERTED",
    						   DATA_MANAGER_INSERT_TIME = "DATA_MANAGER_INSERT_TIME", 
    						   SIGAR_PROC_RES_MEM = "SIGAR_PROC_RES_MEM",
    						   SIGAR_TCP_INERRS = "SIGAR_TCP_INERRS", 
    						   SIGAR_TCP_RETRANS = "SIGAR_TCP_RETRANS",
    						   SIGAR_PAGEOUT = "SIGAR_PAGEOUT", 
    						   SIGAR_PAGEIN = "SIGAR_PAGEIN",
    						   JMS_TOPIC_PUBLISH_TIME = "JMS_TOPIC_PUBLISH_TIME", 
    						   METRIC_DATA_COMPRESS_TIME = "METRIC_DATA_COMPRESS_TIME",
    						   DB_ANALYZE_TIME = "DB_ANALYZE_TIME", 
    						   PURGE_EVENT_LOGS_TIME = "PURGE_EVENT_LOGS_TIME",
    						   PURGE_MEASUREMENTS_TIME = "PURGE_MEASUREMENTS_TIME", 
    						   MEASUREMENT_SCHEDULE_TIME = "MEASUREMENT_SCHEDULE_TIME",
    						   SEND_ALERT_TIME = "SEND_ALERT_TIME", 
    						   ZEVENT_QUEUE_SIZE = "ZEVENT_QUEUE_SIZE",
    						   TRIGGER_INIT_TIME = "TRIGGER_INIT_TIME", 
    						   FIRED_ALERT_TIME = "FIRED_ALERT_TIME",
    						   SCHEDULE_QUEUE_SIZE = "SCHEDULE_QUEUE_SIZE",
    						   UNSCHEDULE_QUEUE_SIZE = "UNSCHEDULE_QUEUE_SIZE",
    						   ESCALATION_EXECUTE_STATE_TIME = "ESCALATION_EXECUTE_STATE_TIME",
    						   JDBC_HQ_DS_MAX_ACTIVE = "JDBC_HQ_DS_MAX_ACTIVE", 
    						   JDBC_HQ_DS_IN_USE = "JDBC_HQ_DS_IN_USE";

    private ConcurrentStatsCollector() {
        if (ConcurrentStatsCollector.enabled) {
            _mbeanServer = Bootstrap.getBean(MBeanServer.class);
            registerInternalStats();
        } else {
            _mbeanServer = null;
        }
    }

    public Map<String, StatCollector> getStatKeys() {
		return _statKeys;
	}

	public final void register(final String statId) {
        // can't register any stats after the collector has been initially
        // started due to consistent ordering in the csv output file
        if (_hasStarted.get()) {
        	_log.info("Cannot register " + statId + " because the collector has already been started.");
        	
            return;
        }

        if (_statKeys.containsKey(statId)) {
            return;
        }
        
        _statKeys.put(statId, null);
    }

    public final void register(final StatCollector stat) {
        // can't register any stats after the collector has been initially
        // started due to the need for consistent ordering in the csv output
        // file
        if (_hasStarted.get()) {
            _log.warn(stat.getId() + " attempted to register in stat collector although it has " +
                      " already been started, not allowing");
            return;
        }
        
        if (_statKeys.containsKey(stat.getId())) {
            _log.warn(stat.getId() + " attempted to register in stat collector although it has " +
                      " already been registered, not allowing");
            return;
        }
        
        _statKeys.put(stat.getId(), stat);
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

    private final void registerInternalStats() {
        register(new StatCollector() {
            private final CacheManager _cMan = CacheManager.getInstance();

            public String getId() {
                return EHCACHE_TOTAL_OBJECTS;
            }

            public long getVal() {
                long rtn = 0l;
                String[] caches = _cMan.getCacheNames();
                for (int i = 0; i < caches.length; i++) {
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
                    return (long) _sigar.getLoadAverage()[0];
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
                    return (long) _sigar.getProcCpu(getProcPid()).getPercent();
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
                    return (long) _sigar.getProcMem(getProcPid()).getResident();
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
                    long rtn = curr - last;
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
                    long rtn = curr - last;
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
                    long rtn = curr - last;
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
                    long rtn = curr - last;
                    last = curr;
                    return rtn;
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });

        register(new MBeanCollector("HIBERNATE_2ND_LEVEL_CACHE_HITS", "Hibernate:type=statistics,application=hq",
            "SecondLevelCacheHitCount", true));

        register(new MBeanCollector("HIBERNATE_2ND_LEVEL_CACHE_MISSES", "Hibernate:type=statistics,application=hq",
            "SecondLevelCacheMissCount", true));

        register(new MBeanCollector("HIBERNATE_QUERY_CACHE_MISSES", "Hibernate:type=statistics,application=hq",
            "QueryCacheMissCount", true));

        register(new MBeanCollector("HIBERNATE_QUERY_CACHE_HITS", "Hibernate:type=statistics,application=hq",
            "QueryCacheHitCount", true));

        register(new MBeanCollector("ZEVENTS_PROCESSED", "hyperic.jmx:name=HQInternal", "ZeventsProcessed", true));

        register(new MBeanCollector("ZEVENT_QUEUE_SIZE", "hyperic.jmx:name=HQInternal", "ZeventQueueSize", false));

        register(new MBeanCollector("PLATFORM_COUNT", "hyperic.jmx:name=HQInternal", "PlatformCount", false));

        register(new MBeanCollector("JMS_EVENT_TOPIC",
            "org.apache.activemq:BrokerName=localhost,Type=Topic,Destination=topic/eventsTopic", "QueueSize", false));

        register(new MBeanCollector("EDEN_MEMORY_USED", "java.lang:type=MemoryPool,name=",
            new String[] { "Par Eden Space", "PS Eden Space", "Eden Space" }, "Usage", "used"));

        register(new MBeanCollector("SURVIVOR_MEMORY_USED", "java.lang:type=MemoryPool,name=",
            new String[] { "Par Survivor Space", "PS Survivor Space", "Survivor Space" }, "Usage", "used"));

        register(new MBeanCollector("TENURED_MEMORY_USED", "java.lang:type=MemoryPool,name=",
            new String[] { "CMS Old Gen", "PS Old Gen", "Tenured Gen" }, "Usage", "used"));

        register(new MBeanCollector("HEAP_MEMORY_USED", "java.lang:type=Memory", new String[] { "" },
            "HeapMemoryUsage", "used"));

        register(new MBeanCollector("JVM_MARKSWEEP_GC", "java.lang:type=GarbageCollector,name=",
            new String[] { "ConcurrentMarkSweep", "PS MarkSweep" }, "CollectionTime", true));

        register(new MBeanCollector("JVM_COPY_GC", "java.lang:type=GarbageCollector,name=",
            new String[] { "Copy", "ParNew", "PS Scavenge" }, "CollectionTime", true));

        
        register(new MBeanCollector(JDBC_HQ_DS_MAX_ACTIVE, "hyperic.jmx:type=DataSource,name=tomcat.jdbc",
            "MaxActive", false));

        register(new MBeanCollector(JDBC_HQ_DS_IN_USE, "hyperic.jmx:type=DataSource,name=tomcat.jdbc",
            "Active", false));

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
        private final String _id, _objectName, _attrName, _valProp;
        private final String[] _names;
        private StatUnreachableException _ex = null;
        private int failures = 0;
        private final boolean _isComposite, _isTrend;
        private long _last = 0l;

        MBeanCollector(String id, String objectName, String[] names, String attrName, boolean trend) {
            _id = id;
            _objectName = objectName;
            _attrName = attrName;
            _names = names;
            _valProp = null;
            _isComposite = false;
            _isTrend = trend;
        }

        MBeanCollector(String id, String objectName, String attrName, boolean trend) {
            _id = id;
            _objectName = objectName;
            _attrName = attrName;
            _names = new String[] { "" };
            _valProp = null;
            _isComposite = false;
            _isTrend = trend;
        }

        MBeanCollector(String id, String objectName, String[] names, String attrName, String valProp) {
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
            // no need to keep generating a new exception. If it fails
            // 10 times, assume that the mbean server is not on.
            if (_ex != null && failures >= 10) {
                throw _ex;
            }
            Exception exception = null;
            for (int i = 0; i < _names.length; i++) {
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

        private final long getComposite(String name) throws StatUnreachableException, AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException, MalformedObjectNameException,
            NullPointerException {
            final ObjectName objName = new ObjectName(_objectName + name);
            final CompositeDataSupport cds = (CompositeDataSupport) _mbeanServer.getAttribute(objName, _attrName);
            final long val = ((Number) cds.get(_valProp)).longValue();
            failures = 0;
            return val;
        }

        private final long getValue(String name) throws MalformedObjectNameException, NullPointerException,
            AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
            final ObjectName _objName = new ObjectName(_objectName + name);
            long rtn = ((Number) _mbeanServer.getAttribute(_objName, _attrName)).longValue();
            failures = 0;
            return rtn;
        }
    }

    public static void setEnabled(boolean enabled) {
        ConcurrentStatsCollector.enabled = enabled;
    }
    
    public StatsObject generateMarker() {
    	final StatsObject marker = new StatsObject(-1, null);
    	
    	_queue.add(marker);
    	
    	return marker;
    }
    
    public StatsObject pollQueue() {
    	return _queue.poll();
    }
}
