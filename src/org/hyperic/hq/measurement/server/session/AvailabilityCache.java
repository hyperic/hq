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

public class AvailabilityCache {

    // The cache name, must match what is in ehcache.xml
    private static final String CACHENAME = "AvailabilityCache";

    private static final Object _cacheLock = new Object();

    private static Cache _cache;

    private static final AvailabilityCache _instance = new AvailabilityCache();

    private AvailabilityCache() {
        _cache = CacheManager.getInstance().getCache(CACHENAME);
    }
    
    public static AvailabilityCache getInstance() {
        return _instance;
    }

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

    public DataPoint get(Integer id) {
        synchronized (_cacheLock) {
            Element e = _cache.get(id);

            if (e != null) {
                return (DataPoint)e.getObjectValue();
            }

            return null;
        }
    }
    
    public void clear() {
        synchronized (_cacheLock) {
            _cache.removeAll();
        }
    }
    
    public void put(Integer id, DataPoint state) {
        synchronized (_cacheLock) {
            _cache.put(new Element(id, state));
        }
    }
}
