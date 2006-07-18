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

package org.hyperic.util.collection;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/**
 * This class is meant to serve as a cached list of objects which will
 * be expired on a set interval. Its Key is expected to be the object
 * you want to cache, the value will always be the long value of the
 * time in which it was inserted into the cache. This is necessary so
 * the removeEldestEntry function can reliably remove the oldest 
 * item on the queue.
 * Note, this implementation is NOT synchronized. In addition, since
 * it extends LinkedHashMap, you should note that reads also modify
 * the list, thus, if you need synchronized access, use the helper
 * method.
 */
public class TimeBasedCache extends LinkedHashMap {
    private static final int DEF_MAX_ENTRIES = 250;
    private static final int DEF_EXPIRE_TIME_SECS = 15;

    private long expireTime;

    public TimeBasedCache() {
        super(DEF_MAX_ENTRIES);
        this.expireTime = DEF_EXPIRE_TIME_SECS * 1000;
    }

    /**
     * Construct a cache list of user specified size
     * and expiration time
     * @param size - number of objects in the cache
     * @param expireSecs - seconds to keep objects in cache
     */
    public TimeBasedCache(int size, int expireSecs) {
        super(size);
        this.expireTime = expireSecs * 1000;
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
        long createTime = ((Long)eldest.getValue())
                                .longValue();
        long diff = System.currentTimeMillis() - createTime;                        
        return diff > expireTime;
    }

    public boolean containsKey(Object key) {
        Long createTime = (Long) super.remove(key);
        
        if (createTime != null &&
            System.currentTimeMillis() - createTime.longValue() < expireTime) {
            super.put(key, createTime);
            return true;
        }

        return false;
    }
    
    /**
     * This is a convenient way of entering items into the cache.
     * it takes care of assigning the value to be the current time
     */
    public Object put(Object key) {
        return this.put(key, new Long(System.currentTimeMillis()));
    }

    /**
     * Overriding the generic put method to guarantee type safety 
     * at insertion time
     */
    public Object put(Object key, Object val) {
        if(!(val instanceof java.lang.Long)) {
            throw new IllegalArgumentException(
                "TimeBasedCache only supports values of Long objects");
        }
        return super.put(key, val);
    }

    /**
     * Get an instance of TimeBasedCached which is backed by a 
     * synchronized map
     */
    public static Map getSynchronizedCache(int size, 
                                                      int expireSecs) {
        return Collections.synchronizedMap(
                    new TimeBasedCache(size, expireSecs));
    }
}
