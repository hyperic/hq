/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.cache;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.cache.statistics.CacheStatistics;
import org.hyperic.hq.cache.statistics.LocalSingleEhCacheStatistics;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Cache data manager for interacting with EhCache
 */
public class EhCacheDataManager implements CacheDataManager {

    /**
     * Returning a list of {@link net.sf.ehcache.Cache} objects
     * known to EhCache cache manager.
     * 
     * @see org.hyperic.hq.cache.CacheDataManager#getCaches()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List getCaches() {
        CacheManager cacheManager = CacheManager.getInstance();
        String[] caches = cacheManager.getCacheNames();
        List<Cache> res = new ArrayList<Cache>(caches.length);
        for (int i=0; i<caches.length; i++) {
            res.add(cacheManager.getCache(caches[i]));
        }
        return res;
    }

    /**
     * Return supported type of {@code TYPE_EHCACHE}
     * 
     * @see org.hyperic.hq.cache.CacheDataManager#getCacheType()
     */
    public String getCacheType() {
        return TYPE_EHCACHE;
    }

    /*
     * (non-Javadoc)
     * @see org.hyperic.hq.cache.CacheDataManager#getStatistics()
     */
    public CacheStatistics getStatistics() {
        return new LocalSingleEhCacheStatistics();
    }

    /*
     * (non-Javadoc)
     * @see org.hyperic.hq.cache.CacheDataManager#get(java.lang.String, java.lang.Object)
     */
    public Object get(String cacheName, Object key) {
        Element element = getCache(cacheName).get(key);
        return element == null ? null : element.getObjectValue();
    }

    /*
     * (non-Javadoc)
     * @see org.hyperic.hq.cache.CacheDataManager#put(java.lang.String, java.lang.Object, java.lang.Object)
     */
    public void put(String cacheName, Object key, Object value) {
        Element element = new Element(key, value);
        getCache(cacheName).put(element);
    }

    /*
     * (non-Javadoc)
     * @see org.hyperic.hq.cache.CacheDataManager#remove(java.lang.String, java.lang.Object)
     */
    public void remove(String cacheName, Object key) {
        getCache(cacheName).remove(key);
    }

    /**
     * Get matching {@link net.sf.ehcache.Cache} from
     * EhCache cache manager.
     * 
     * @param cacheName Cache region name
     * @return Matching {@link net.sf.ehcache.Cache}
     */
    private Cache getCache(String cacheName) {
        CacheManager cacheManager = CacheManager.getInstance();
        return cacheManager.getCache(cacheName);
    }
    

}
