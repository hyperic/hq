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

import org.hyperic.hq.product.MetricValue;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

    /**
 * The MetricDataCache caches the last measurement keyed on the derived
 * measurement id.  The purpose of this cache is to avoid needing to go to
 * the database when looking up the last value for a metric.
 */

public class MetricDataCache {
    // The cache name, must match what is in ehcache.xml
    private static final String CACHENAME = "MetricDataCache";

    private static Cache _cache;

    private static MetricDataCache _singleton = new MetricDataCache();
    
    /**
     * Singleton accessor
     */
    public static MetricDataCache getInstance() {
        return _singleton;
    }

    private MetricDataCache() {
        _cache = CacheManager.getInstance().getCache(CACHENAME);
    }

    /**
     * Add a MetricValue to the cache.  This method checks the timestamp of
     * the MetricValue to be added to ensure it's not an older datapoint than
     * what is already cached.
     *
     * @param mid The measurement id.
     * @param mval The MetricValue to store.
     * @return true if the MetricValue was added to the cache, false otherwise.
     */
    public boolean add(Integer mid, MetricValue mval) {
        Element el = _cache.get(mid);

        if (el != null) {
            // Check if existing cached data is newer
            MetricValue val = (MetricValue)el.getObjectValue();
            if (val.getTimestamp() > mval.getTimestamp()) {
                return false;
            }
        }

        el = new Element(mid, mval);
        _cache.put(el);

        return true;
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
        Element el = _cache.get(mid);

        if (el != null) {
            MetricValue val = (MetricValue)el.getObjectValue();
            if (val.getTimestamp() >= timestamp) {
                return val;
            }
        }
        return null;
    }
}
