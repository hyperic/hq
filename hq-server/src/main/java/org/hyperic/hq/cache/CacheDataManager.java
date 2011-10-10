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

import java.util.List;

import org.hyperic.hq.cache.statistics.CacheStatistics;

/**
 * Interface to underlying cache implementation. Can be used for general
 * access to cache layer without knowing what is the actual
 * cache implementation underneath.
 * <p>
 * Currently this interface provides only basic cache region
 * interaction is terms of handling simple cache items.
 */
public interface CacheDataManager {
   
    /** Constant for supported EhCache type */
    public static final String TYPE_EHCACHE = "ehcache"; 
    
    /** Constant for supported Gemfire type */
    public static final String TYPE_GEMFIRE = "gemfire"; 
    
    /**
     * Returns underlying cache objects. Returned types
     * depends on the cache implementation.
     * 
     * @return Raw cache region objects.
     */
    public List<Object> getCaches();
    
    /**
     * Returns underlying cache type.
     * 
     * @return Either {@code TYPE_EHCACHE} or {@code TYPE_GEMFIRE}
     */
    public String getCacheType();

    /**
     * Gets statistics for underlying cache.
     * 
     * @return Statistics for this cache.
     */
    public CacheStatistics getStatistics();

    /**
     * Get value from the cache.
     * 
     * @param cacheName Cache region name
     * @param key Cache key
     * @return Cache value or null if not found.
     */
    public Object get(String cacheName, Object key);

    /**
     * Puts value to the cache.
     * 
     * @param cacheName Cache region name
     * @param key Cache key
     * @param value Cache value
     */
    public void put(String cacheName, Object key, Object value);
    
    /**
     * Removes value from the cache.
     * 
     * @param cacheName Cache region name
     * @param key Cache key
     */
    public void remove(String cacheName, Object key);

}
