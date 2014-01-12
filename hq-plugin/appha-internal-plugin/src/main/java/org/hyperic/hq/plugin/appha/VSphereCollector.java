/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.appha;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.collection.IntHashMap;

import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfProviderSummary;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.PerformanceManager;

public abstract class VSphereCollector extends Collector {

    private static final long CACHE_TIMEOUT = 60000;
    private static final Log _log = LogFactory.getLog(VSphereCollector.class.getName());
    private static ObjectCache<IntHashMap> cached;
    private static final Object CACHE_LOCK = new Object();

    public static final String PROP_URL = "url";
    public static final String PROP_UUID = "uuid";
    
    protected Properties _props;

    protected abstract void collect(VSphereUtil vim)
        throws Exception;

    protected abstract String getType();

    protected abstract String getName();
    
    protected abstract String getUuid();

    protected void init() throws PluginException {
        _props = getProperties();
        setSource(VSphereUtil.getURL(_props));
    }

    protected String getHostname() {
        return _props.getProperty(PROP_HOSTNAME);
    }

    protected String getCounterKey(PerfCounterInfo info) {
        String group = info.getGroupInfo().getKey();
        String name = info.getNameInfo().getKey();
        String rollup = info.getRollupType().toString();
        return group + "." + name + "." + rollup;
    }
    
    protected Integer getRefreshRate(PerformanceManager perfManager,
                                     ManagedEntity entity)
        throws Exception {
        
        PerfProviderSummary summary = perfManager.queryPerfProviderSummary(entity);
        
        return summary.getRefreshRate();        
    }

    //http://pubs.vmware.com/vi3/sdk/ReferenceGuide/vim.PerformanceManager.html
    //interval that summarizes statistics for five minute intervals, the ID is 300
    private static final Integer PERF_INTERVAL_ID = new Integer(300);

    protected PerfMetricId[] getPerfMetricIds(PerformanceManager perfManager,
                                              ManagedEntity entity, Integer interval)
    throws Exception {
        return perfManager.queryAvailablePerfMetric(entity, null, null, interval);
    }    

    protected PerfMetricId[] getPerfMetricIds(PerformanceManager perfManager, ManagedEntity entity)
    throws Exception {
        PerfMetricId[] ids =
            perfManager.queryAvailablePerfMetric(entity, null, null, PERF_INTERVAL_ID);
        return ids;
    }    
    
    protected ManagedEntity getManagedEntity(VSphereUtil mo) throws Exception {
        final String uuid = getUuid();
        final String name = getName();
        final String type = getType();
        if (uuid != null && name != null) {
            ManagedEntity entity = mo.findByUuid(type, uuid);
            if (entity == null) {
                entity = mo.find(type, name);
                return (entity == null || !VSphereUtil.getUuid(entity).equals(uuid)) ? null : entity;
            } else {
                return entity;
            }
        } else if (uuid == null) {
            return mo.find(type, name);
        } else {
            return mo.findByUuid(type, uuid);
        }
    }
    
    private IntHashMap getCounterCached() {
        synchronized (CACHE_LOCK) {
            return (cached == null || cached.isExpired()) ? null : cached.getEntity();
        }
    }

    protected IntHashMap getCounterInfo(PerformanceManager perfManager)
    throws Exception {
        IntHashMap info = getCounterCached();
        if (info != null) {
            return info;
        }
        PerfCounterInfo[] counters = perfManager.getPerfCounter();
        info = new IntHashMap(counters.length);
        for (int i=0; i<counters.length; i++) {
            info.put(counters[i].getKey(), counters[i]);
        }
        synchronized (CACHE_LOCK) {
            cached = new ObjectCache<IntHashMap>(info, CACHE_TIMEOUT);
        }
        return info;
    }

    //collect() only allows 1 connection per-host
    //support isPoolable for concurrent connections to multiple hosts 
    public boolean isPoolable() {
        return true;
    }

    public void collect() {
        VSphereConnection conn = null;
        try {
            setAvailability(false);
            conn = VSphereConnection.getPooledInstance(getProperties());
            synchronized (conn.LOCK) {
                collect(conn.vim);
            }
        } catch (Exception e) {
            setErrorMessage(e.getMessage(), e);
            _log.error(e.getMessage(), e);
        } finally {
            if (conn != null) conn.release();
        }
    }
}
