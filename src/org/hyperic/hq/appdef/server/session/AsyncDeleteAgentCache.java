/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * This class is used during the aynchronous delete process
 * to unschedule metrics. It is an in-memory map of resources
 * and its agent because that info no longer exists in the DB.
 */
public class AsyncDeleteAgentCache {
    private Log log = LogFactory.getLog(AsyncDeleteAgentCache.class);

    private final Map _cache;

    private static final AsyncDeleteAgentCache singleton = 
        new AsyncDeleteAgentCache();

    private AsyncDeleteAgentCache() {
        _cache = Collections.synchronizedMap(new HashMap());
    }

    /**
     * 
     * @param key The AppdefEntityID of the async deleted resource
     * @return Integer The agentId of the async deleted resource
     */
    public Integer get(AppdefEntityID key) {        
        return (Integer) _cache.get(key);
    }

    /**
     * 
     * @param key The AppdefEntityID of the async deleted resource
     * @param Integer The agentId of the async deleted resource
     */
    public void put(AppdefEntityID key, Integer value) {                                                          
        _cache.put(key, value);
    }
    
    public void remove(AppdefEntityID key) {
        _cache.remove(key);
    }

    public void clear() {
        _cache.clear();
    }
    
    public int getSize() {
        return _cache.size();
    }
    
    public String toString() {
        return _cache.toString();
    }

    public static AsyncDeleteAgentCache getInstance() {
        return singleton;
    }

}
