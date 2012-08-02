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

package org.hyperic.hq.measurement.server.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.hyperic.hq.product.MetricValue;
import org.springframework.stereotype.Repository;

/**
 * The MetricDataCache caches the last measurement keyed on the derived
 * measurement id.  The purpose of this cache is to avoid needing to go to
 * the database when looking up the last value for a metric.
 * 
 * If this area of the code becomes a bottleneck, we may want to consider
 * ditching ehcache and just using a straight HashMap -- the code is 
 * simpler, and locking straightforward.  However, it is currently nice to
 * keep ehcache, as it allows us to configure sizes and get stats.  
 */
@Repository
public class EhCacheMetricDataCache implements MetricDataCache {
    // The cache name, must match what is in ehcache.xml
    private static final String CACHENAME = "MetricDataCache";

    private final Object cacheLock = new Object();

    private  Cache cache;

    public EhCacheMetricDataCache() {
        cache = CacheManager.getInstance().getCache(CACHENAME);
    }
    
   
    public Collection<DataPoint> bulkAdd(List<DataPoint> data) {
        HashMap<Integer,DataPoint> cachedData = new HashMap<Integer,DataPoint>(data.size());
        
        synchronized (cacheLock) {
            for (DataPoint dp : data) {
              
                if (add(dp.getMeasurementId(), dp.getMetricValue())) {
                    cachedData.put(dp.getMeasurementId(), dp);
                }
            }
        }
        
        return cachedData.values();
    }

    /**
     * Add a MetricValue to the cache. This method checks the timestamp of
     * the MetricValue to be added to ensure it's not an older data point than
     * what is already cached.
     * 
     * Each invocation of this method is synchronized internally, so consider 
     * using the {@link #bulkAdd(List) bulk add} for batch updates to the cache.
     *
     * @param mid The measurement id.
     * @param mval The MetricValue to store.
     * @return true if the MetricValue was added to the cache, false otherwise.
     */
    public boolean add(Integer mid, MetricValue mval) {
        // Need to synchronize on cache update since the back filler  
        // may be updating the cache concurrently with the data inserter.
        // Without synchronization, a race condition could cause the latest 
        // metric to be overwritten in the cache by an older metric.
        synchronized (cacheLock) {
            Element el = cache.get(mid);

            if (el != null) {
                // Check if existing cached data is newer
                MetricValue val = (MetricValue)el.getObjectValue();
                if (val.getTimestamp() > mval.getTimestamp()) {
                    return false;
                }
            }

            cache.put(new Element(mid, mval));
        }

        return true;            
    }
    
    public Map<Integer,MetricValue> getAll(List<Integer> mids, long timestamp) {
        final Map<Integer,MetricValue> rtn = new HashMap<Integer,MetricValue>(mids.size());
        synchronized (cacheLock) {
            for (final Integer mid : mids ) {
                final Element elem = cache.get(mid);
                if (elem == null) {
                    continue;
                }
                final MetricValue val = (MetricValue) elem.getObjectValue();
                if (val != null && val.getTimestamp() >= timestamp) {
                    rtn.put(mid, val);
                }
            }
        }
        return rtn;
    }


   
    public MetricValue get(Integer mid, long timestamp) {
        Element el;
        synchronized (cacheLock) {
            el = cache.get(mid);
        }

        if (el != null) {
            MetricValue val = (MetricValue)el.getObjectValue();
            if (val.getTimestamp() >= timestamp) {
                return val;
            }
        }
        return null;
    }
    
   
    public void remove(Integer mid) {
        synchronized (cacheLock) {
            cache.remove(mid);
        }
    }
}
