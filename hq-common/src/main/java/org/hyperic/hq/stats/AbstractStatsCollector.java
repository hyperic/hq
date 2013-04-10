	/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMWare, Inc.
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.hyperic.util.stats.StatsObject;

public abstract class AbstractStatsCollector {
	private final Log log = LogFactory.getLog(AbstractStatsCollector.class);

    // using tree due to ordering capabilities
    private final Map<String, StatCollector> statKeys = new TreeMap<String, StatCollector>();
	private final ConcurrentLinkedQueue<StatsObject> queue = new ConcurrentLinkedQueue<StatsObject>();
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private Long pid;
    protected final Sigar sigar = new Sigar();
    protected final MBeanServer mBeanServer;

    public static final String JVM_TOTAL_MEMORY = "JVM_TOTAL_MEMORY", 
    						   JVM_FREE_MEMORY  = "JVM_FREE_MEMORY",
    						   JVM_MAX_MEMORY   = "JVM_MAX_MEMORY", 
    						   SIGAR_1MLOAD = "SIGAR_1MLOAD",
    						   SIGAR_CPU    = "SIGAR_CPU", 
    						   SIGAR_PROC_RES_MEM = "SIGAR_PROC_RES_MEM",
    						   SIGAR_TCP_INERRS   = "SIGAR_TCP_INERRS", 
    						   SIGAR_TCP_RETRANS  = "SIGAR_TCP_RETRANS",
    						   SIGAR_PAGEOUT = "SIGAR_PAGEOUT", 
    						   SIGAR_PAGEIN  = "SIGAR_PAGEIN",
                               EDEN_MEMORY_USED     = "EDEN_MEMORY_USED",
                               SURVIVOR_MEMORY_USED = "SURVIVOR_MEMORY_USED",
                               TENURED_MEMORY_USED  = "TENURED_MEMORY_USED",
                               HEAP_MEMORY_USED = "HEAP_MEMORY_USED",
                               JVM_MARKSWEEP_GC = "JVM_MARKSWEEP_GC",
                               JVM_COPY_GC      = "JVM_COPY_GC",
    						   STATS_COLLECTOR  = "STATS_COLLECTOR";


    public AbstractStatsCollector(MBeanServer mBeanServer) {
    	this.mBeanServer = mBeanServer;
        registerInternalStats();
    }
    
    protected abstract ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long seconds);

    public Map<String, StatCollector> getStatKeys() {
		return statKeys;
	}
    
    void setStarted(boolean started) {
        hasStarted.set(started);
    }

	public void register(final String statId) {
        // can't register any stats after the collector has been initially
        // started due to consistent ordering in the csv output file
        if (hasStarted.get()) {
        	log.warn("Cannot register " + statId +
        	         " because the collector has already been started.");
            return;
        }
        if (statKeys.containsKey(statId)) {
            return;
        }
        statKeys.put(statId, null);
    }

    public void register(final StatCollector stat) {
        // can't register any stats after the collector has been initially
        // started due to the need for consistent ordering in the csv output
        // file
        if (hasStarted.get()) {
            log.warn(stat.getId() + " attempted to register in stat collector although it has " +
                      " already been started, not allowing");
            return;
        }
        if (statKeys.containsKey(stat.getId())) {
            log.warn(stat.getId() + " attempted to register in stat collector although it has " +
                      " already been registered, not allowing");
            return;
        }
        statKeys.put(stat.getId(), stat);
    }

    public void addStat(final long value, final String id) {
        if (!statKeys.containsKey(id)) {
            return;
        }
        final long now = System.currentTimeMillis();
        // may want to look at pooling these objects to avoid creation overhead
        final StatsObject stat = new StatsObject(value, id);
        queue.add(stat);
        final long total = System.currentTimeMillis() - now;
        queue.add(new StatsObject(total, STATS_COLLECTOR));
    }

    private final void registerInternalStats() {
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
                    return (long) sigar.getLoadAverage()[0];
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
                    return (long) (sigar.getProcCpu(getProcPid()).getPercent() * 100);
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
                    return (long) sigar.getProcMem(getProcPid()).getResident();
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
                    long curr = sigar.getTcp().getInErrs();
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
            private boolean isFirst = true;
            public String getId() {
                return SIGAR_TCP_RETRANS;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    long curr = sigar.getTcp().getRetransSegs();
                    long rtn = curr - last;
                    last = curr;
                    if (isFirst) {
                        isFirst = false;
                        throw new StatUnreachableException("don't return first value");
                    }
                    return rtn;
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new StatCollector() {
            private long last = 0;
            private boolean isFirst = true;
            public String getId() {
                return SIGAR_PAGEOUT;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    long curr = sigar.getSwap().getPageOut();
                    long rtn = curr - last;
                    last = curr;
                    if (isFirst) {
                        isFirst = false;
                        throw new StatUnreachableException("don't return first value");
                    }
                    return rtn;
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new StatCollector() {
            private long last = 0;
            private boolean isFirst = true;
            public String getId() {
                return SIGAR_PAGEIN;
            }
            public long getVal() throws StatUnreachableException {
                try {
                    long curr = sigar.getSwap().getPageIn();
                    long rtn = curr - last;
                    last = curr;
                    if (isFirst) {
                        isFirst = false;
                        throw new StatUnreachableException("don't return first value");
                    }
                    return rtn;
                } catch (SigarException e) {
                    throw new StatUnreachableException(e.getMessage(), e);
                }
            }
        });
        register(new MBeanCollector(EDEN_MEMORY_USED, "java.lang:type=MemoryPool,name=",
            new String[] { "Par Eden Space", "PS Eden Space", "Eden Space" }, "Usage", "used"));
        register(new MBeanCollector(SURVIVOR_MEMORY_USED, "java.lang:type=MemoryPool,name=",
            new String[] {"Par Survivor Space", "PS Survivor Space", "Survivor Space" }, "Usage",
                          "used"));
        register(new MBeanCollector(TENURED_MEMORY_USED, "java.lang:type=MemoryPool,name=",
            new String[] { "CMS Old Gen", "PS Old Gen", "Tenured Gen" }, "Usage", "used"));
        register(new MBeanCollector(HEAP_MEMORY_USED, "java.lang:type=Memory", new String[] {""},
            "HeapMemoryUsage", "used"));
        register(new MBeanCollector(JVM_MARKSWEEP_GC, "java.lang:type=GarbageCollector,name=",
            new String[] { "ConcurrentMarkSweep", "PS MarkSweep" }, "CollectionTime", true));
        register(new MBeanCollector(JVM_COPY_GC, "java.lang:type=GarbageCollector,name=",
            new String[] { "Copy", "ParNew", "PS Scavenge" }, "CollectionTime", true));
        register(STATS_COLLECTOR);
    }

    protected long getProcPid() {
        if (pid != null) {
            return pid.longValue();
        }
        pid = new Long(sigar.getPid());
        return pid.longValue();
    }
    
    public StatsObject generateMarker() {
    	final StatsObject marker = new StatsObject(-1, null);
    	queue.add(marker);
    	return marker;
    }

    public StatsObject pollQueue() {
    	return queue.poll();
    }

    protected class StatSampler implements StatCollector {
        private final StatCollector stat;
        private final String id;
        private final AtomicLong value = new AtomicLong();
        private final AtomicReference<StatUnreachableException> exceptionRef =
            new AtomicReference<StatUnreachableException>();
        protected StatSampler(StatCollector s) {
            this.id = s.getId() + "_MAX";
            this.stat = s;
            final Runnable runnable = new Runnable() {
                public void run() {
                    if (!hasStarted.get()) {
                        return;
                    }
                    try {
                        final long newVal = stat.getVal();
                        synchronized (value) {
                            final long val = value.get();
                            if (Math.abs(newVal) > Math.abs(val)) {
                                value.set(newVal);
                            }
                            exceptionRef.set(null);
                        }
                    } catch (StatUnreachableException e) {
                        exceptionRef.set(e);
                    } catch (Throwable t) {
                        log.error(t,t);
                    }
                }
            };
            scheduleAtFixedRate(runnable, 1000);
        }
        public String getId() {
            return id;
        }
        public long getVal() throws StatUnreachableException {
            StatUnreachableException ex;
            if ((ex = exceptionRef.get()) != null) {
                throw ex;
            }
            synchronized (value) {
                return value.getAndSet(0);
            }
        }
    }

    protected class MBeanCollector implements StatCollector {
        private final String _id, _objectName, _attrName, _valProp;
        private final String[] _names;
        private StatUnreachableException _ex = null;
        private int failures = 0;
        private final boolean _isComposite, _isTrend;
        private long _last = 0l;
        protected MBeanCollector(String id, String objectName, String[] names, String attrName, boolean trend) {
            _id = id;
            _objectName = objectName;
            _attrName = attrName;
            _names = names;
            _valProp = null;
            _isComposite = false;
            _isTrend = trend;
        }
        protected MBeanCollector(String id, String objectName, String attrName, boolean trend) {
            _id = id;
            _objectName = objectName;
            _attrName = attrName;
            _names = new String[] { "" };
            _valProp = null;
            _isComposite = false;
            _isTrend = trend;
        }
        protected MBeanCollector(String id, String objectName, String[] names, String attrName, String valProp) {
            _names = names;
            _id = id;
            _objectName = objectName;
            _attrName = attrName;
            _valProp = valProp;
            _isComposite = true;
            _isTrend = false;
        }
        public String getId() {
            return _id;
        }
        public long getVal() throws StatUnreachableException {
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
                        long rtn = (_isTrend) ? (val - _last) : val;
                        _last = val;
                        return rtn;
                    } else {
                        long val = getValue(_names[i]);
                        long rtn = (_isTrend) ? (val - _last) : val;
                        _last = val;
                        return rtn;
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
            final CompositeDataSupport cds = (CompositeDataSupport) mBeanServer.getAttribute(objName, _attrName);
            final long val = ((Number) cds.get(_valProp)).longValue();
            failures = 0;
            return val;
        }
        private final long getValue(String name) throws MalformedObjectNameException, NullPointerException,
            AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
            final ObjectName _objName = new ObjectName(_objectName + name);
            long rtn = ((Number) mBeanServer.getAttribute(_objName, _attrName)).longValue();
            failures = 0;
            return rtn;
        }
    }
    
    public void destory() { 
        this.queue.clear() ;
        this.statKeys.clear() ; 
    }//EOM 

}
