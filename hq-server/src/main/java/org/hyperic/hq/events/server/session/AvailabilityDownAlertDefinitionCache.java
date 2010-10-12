/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

package org.hyperic.hq.events.server.session;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.springframework.stereotype.Repository;

/**
 * This class is an in-memory map of whether "availability down" alert
 * definitions exist for a resource
 */
@Repository
public class AvailabilityDownAlertDefinitionCache  {
    
    public static final String CACHENAME = "AvailabilityDownAlertDefinitionCache";
    private final Object _cacheLock = new Object();
    private final Cache _cache;
  
    public AvailabilityDownAlertDefinitionCache() {
        _cache = CacheManager.getInstance().getCache(CACHENAME);
    }

    public Boolean get(AppdefEntityID key) {
        Element el = _cache.get(key);
        if (el != null) {
            return (Boolean) el.getObjectValue();
        } 
        return null;
    }

    public void put(AppdefEntityID key, Boolean value) {
        Element el = new Element(key, value);

        synchronized (_cacheLock) {
            _cache.put(el);
        }
    }

    public void clear() {
		synchronized (_cacheLock) {
			_cache.removeAll();
		}
    }
    
    public int size() {
        return _cache.getSize();
    }

    private void remove(AppdefEntityID key) {
        synchronized (_cacheLock) {
            _cache.remove(key);
        }
    }
     
     public void removeFromCache(AlertDefinition def) {
         synchronized (_cacheLock) {
             if (isOkToRemove(def)) {
                 remove(def.getAppdefEntityId());
             }

             for (AlertDefinition childDef : def.getChildren()) {
                 if (isOkToRemove(childDef)) {
                     remove(childDef.getAppdefEntityId());
                 }
             }
         }
     }
     
     private boolean isOkToRemove(AlertDefinition def) {
         Resource r = def.getResource();
         return (r != null && !r.isInAsyncDeleteState());
     }

}
