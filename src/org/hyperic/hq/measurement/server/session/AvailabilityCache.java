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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Dynamic EhCache that is allowed to grow over time as needed.  At some point
 * this logic should be pushed into a base class so other Cache's managed
 * directly by HQ make use of this functionallity. (MetricDataCache, SRNCache)
 */
public class AvailabilityCache {

    // Default configuration
    static final String CACHENAME          = "AvailabilityCache";
    static final int    CACHESIZE          = 20000;
    static final int    CACHESIZEINCREMENT = 1000;

    private static final Object _cacheLock = new Object();

    // The internal EhCache
    private static Cache _cache;
    // Cache.getSize() is expensive so we keep out own cache size count.
    private static int _cacheMaxSize;
    private static int _cacheSize;

    private static Log _log = LogFactory.getLog(AvailabilityCache.class);

    private static final AvailabilityCache _instance = new AvailabilityCache();

    private AvailabilityCache() {
        CacheManager cm = CacheManager.getInstance();
        // Allow configuration of this cache throgh ehcache.xml
        _cache = cm.getCache(CACHENAME);

        if (_cache == null) {
            _log.info("No default cache specified in ehcache.xml, using " +
                      "default size of " + CACHESIZE + ".");
            _cache = new Cache(CACHENAME, CACHESIZE, false, true, 0, 0);
            cm.addCache(_cache);
        }
        
        _cacheSize = 0;
        _cacheMaxSize = _cache.getCacheConfiguration().getMaxElementsInMemory();
    }

    /**
     * @return The singleton instance of the AvailabilityCache.
     */
    public static AvailabilityCache getInstance() {
        return _instance;
    }

    /**
     * Get a DataPoint from the cache based on Measurement id
     * @param id The Measurement id in question.
     * @return The DataPoint or the defaultState if the Measurement id is not
     * located in the cache. 
     */
    public DataPoint get(Integer id, DataPoint defaultState) {
        synchronized (_cacheLock) {
            Element e = _cache.get(id);

            if (e == null) {
                _cache.put(new Element(id, defaultState));
                return defaultState;
            }

            return (DataPoint)e.getObjectValue();
        }
    }

    /**
     * Get a DataPoint from the cache based on the Measurement id.
     * @param id The Measurement id in question.
     * @return The DataPoint or null if it does not exist in the cache.
     */
    public DataPoint get(Integer id) {
        synchronized (_cacheLock) {
            Element e = _cache.get(id);

            if (e != null) {
                 return (DataPoint)e.getObjectValue();
            }

            return null;
        }
    }

    /**
     * Put an item into the cache.
     * @param id The Measurement id representing the availability data point.
     * @param state The DataPoint to store for the given id.
     */
    public void put(Integer id, DataPoint state) {
        synchronized (_cacheLock) {
            if (!_cache.isKeyInCache(id)) {
                if (isFull()) {
                    incrementCacheSize();
                }

                _cache.put(new Element(id, state));
                _cacheSize++;
            } else {
                // Update only, don't increment counter.
                _cache.put(new Element(id, state));
            }
        }
    }

    /**
     * Get the total cache size
     * @return The total cache size, as determined by EhCache.
     */
    int getSize() {
        synchronized(_cacheLock) {
            return _cache.getSize();
        }
    }

    /**
     * Remove all elements from the AvailabilityCache.
     */
    void clear() {
        synchronized (_cacheLock) {
            _cache.removeAll();
            _cacheSize = 0;
        }
    }

    private boolean isFull() {
        synchronized (_cacheLock) {
            return _cacheSize == _cacheMaxSize;
        }
    }

    private void incrementCacheSize() {
        _log.info("Asked to increment the " + CACHENAME + " cache.");

        synchronized (_cacheLock) {
            int curMax = _cache.getCacheConfiguration().getMaxElementsInMemory();
            int newMax = curMax  + CACHESIZEINCREMENT;

            _cache.getCacheConfiguration().setMaxElementsInMemory(newMax);
            _cacheMaxSize = newMax;
            _log.info("Increased cache size from " + curMax + " to " + newMax);
        }
    }
}
