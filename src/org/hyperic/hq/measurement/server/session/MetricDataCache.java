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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.hyperic.hq.product.MetricValue;

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

public class MetricDataCache {
    // The cache name, must match what is in ehcache.xml
    private static final String CACHENAME = "MetricDataCache";

    private static final Object _cacheLock = new Object();

    private static Cache _cache;

    private static final MetricDataCache _singleton = new MetricDataCache();

    public static MetricDataCache getInstance() {
        return _singleton;
    }

    private MetricDataCache() {
        _cache = CacheManager.getInstance().getCache(CACHENAME);
    }
    
    /**
     * Add MetricValues to the cache. This method checks the timestamp of
     * each MetricValue to be added to ensure it's not an older data point than
     * what is already cached.
     *
     * @param data The list of DataPoint objects representing each MetricValue.
     * @return The list of DataPoint objects added to the cache. Any DataPoints 
     *         older than what is already cached will NOT be contained in this 
     *         list.
     */
    public Collection bulkAdd(List data) {
        HashMap cachedData = new HashMap(data.size());
        
        synchronized (_cacheLock) {
            for (Iterator iter = data.iterator(); iter.hasNext();) {
                DataPoint dp = (DataPoint) iter.next();
                
                if (add(dp.getMetricId(), dp.getMetricValue())) {
                    cachedData.put(dp.getMetricId(), dp);
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
    protected boolean add(Integer mid, MetricValue mval) {
        // Need to synchronize on cache update since the back filler  
        // may be updating the cache concurrently with the data inserter.
        // Without synchronization, a race condition could cause the latest 
        // metric to be overwritten in the cache by an older metric.
        synchronized (_cacheLock) {
            Element el = _cache.get(mid);

            if (el != null) {
                // Check if existing cached data is newer
                MetricValue val = (MetricValue)el.getObjectValue();
                if (val.getTimestamp() > mval.getTimestamp()) {
                    return false;
                }
            }

            _cache.put(new Element(mid, mval));
        }

        return true;            
    }

    /**
     * Get {@link MetricValue}s from the cache within the specified time range, from timestamp
     * to currentTimeMillis.
     *
     * @param mids {@link List} of {@link Integer}s representing MeasurementIds.
     * @param timestamp the start of the time range (inclusive) in millis.
     * @return {@link Map} of {@link Integer} of measurementIds to {@link MetricValue}
     * from the cache.  If the mid does not exist or the timestamp of value is out of the
     * specified window the returned Map will not include any representation of the mid.
     */
    public Map getAll(List mids, long timestamp) {
        final Map rtn = new HashMap(mids.size());
        synchronized (_cacheLock) {
            for (final Iterator it=mids.iterator(); it.hasNext(); ) {
                final Integer mid = (Integer) it.next();
                final Element elem = _cache.get(mid);
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

    /**
     * Get a MetricValue from the cache.
     *
     * @param mid The measurement id.
     * @param timestamp The beginning of the cache window.
     * @return The MetricValue from the cache, or null if the element is not
     * found, or the item in the cache is stale.
     */
    public MetricValue get(Integer mid, long timestamp) {
        Element el;
        synchronized (_cacheLock) {
            el = _cache.get(mid);
        }

        if (el != null) {
            MetricValue val = (MetricValue)el.getObjectValue();
            if (val.getTimestamp() >= timestamp) {
                return val;
            }
        }
        return null;
    }
    
    /**
     * Remove a MetricValue from cache
     * @param mid The measurement id to remove.
     */
    public void remove(Integer mid) {
        synchronized (_cacheLock) {
            _cache.remove(mid);
        }
    }
}
