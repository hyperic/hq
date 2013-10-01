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

import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Repository;

/**
 * Dynamic EhCache that is allowed to grow over time as needed.
 * AvailabilityCache should only be used in AvailabilityManager.addData()
 * OR Backfiller operations.  In big environments it will be very contentious
 * and due to its nature may not have the data being sought at any given time.
 * Use AvailabilityManager.getLastAvail().
 */
@Repository
public class AvailabilityCache {

	// Default configuration
	static final String CACHENAME          = "AvailabilityCache";
	static final int    CACHESIZE          = 20000;
	static final int    CACHESIZEINCREMENT = 1000;

	private final Object _cacheLock = new Object();
	private final Map _tranCacheState = new HashMap();
	private Thread _tranThread;
	private boolean _inTran = false;

	// The internal EhCache
	private Cache _cache;
	// Cache.getSize() is expensive so we keep out own cache size count.
	private int _cacheMaxSize;
	private int _cacheSize;

	private final Log _log = LogFactory.getLog(AvailabilityCache.class);



	public AvailabilityCache() {
		CacheManager cm = CacheManager.getInstance();
		// Allow configuration of this cache throgh ehcache.xml
		_cache = cm.getCache(CACHENAME);

		if (_cache == null) {
			if (_log.isDebugEnabled()) {
                _log.debug("No default cache specified in ehcache.xml, using default size of " + CACHESIZE + ".");
			}
			_cache = new Cache(CACHENAME, CACHESIZE, false, true, 0, 0);
			cm.addCache(_cache);
		}

		_cacheSize = 0;
		_cacheMaxSize = _cache.getCacheConfiguration().getMaxElementsInMemory();
	}



	private void captureCacheState(Integer metricId) {
		synchronized (_tranCacheState) {
			if (!_inTran || !Thread.currentThread().equals(_tranThread)) {
				return;
			}
			if (_tranCacheState.containsKey(metricId)) {
				return;
			}
			DataPoint pt;
			pt = (DataPoint)get(metricId);
			// doesn't matter if point is null
			_tranCacheState.put(metricId, pt);
		}
	}

	public void rollbackTran() {
		synchronized (_tranCacheState) {
			if (!_inTran || !Thread.currentThread().equals(_tranThread)) {
				return;
			}
			_inTran = false;
			// scottmf, when I use an iterator here, under certain circumstances
			// ConcurrentModificationException is thrown.  It is very hard
			// to reproduce and not straight forward
			Integer[] keys = (Integer[])_tranCacheState.keySet().toArray(new Integer[0]);
			for (int i=0; i<keys.length; i++) {
				DataPoint pt = (DataPoint)_tranCacheState.get(keys[i]);
				if (pt == null) {
					remove(keys[i]);
				} else {
					put(keys[i], pt);
				}
			}
			_tranThread = null;
			_tranCacheState.clear();
			_tranCacheState.notifyAll();
		}
	}

	public void commitTran() {
		synchronized (_tranCacheState) {
			if (!_inTran || !Thread.currentThread().equals(_tranThread)) {
				return;
			}
			_inTran = false;
			_tranThread = null;
			_tranCacheState.clear();
			_tranCacheState.notifyAll();
		}
	}

	/**
	 * @return true if a new cache transaction was started, false if the
	 * currentThread was already participating in the current transaction
	 */
	public boolean beginTran() {
		synchronized (_tranCacheState) {
			if (_inTran && Thread.currentThread().equals(_tranThread)) {
				return false;
			}
			while (_inTran) {
				try {
					_tranCacheState.wait();
				} catch (InterruptedException e) {
				}
			}
			_inTran = true;
			_tranThread = Thread.currentThread();
			_tranCacheState.clear();
			return true;
		}
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
	 * Remove id from cache
	 * @param id The Measurement id in question.
	 */
	public boolean remove(Integer id) {
		synchronized (_cacheLock) {
			return _cache.remove(id);
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
			boolean newTran = false;
			try {
				newTran = beginTran();
				captureCacheState(id);
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
			} finally {
				if (newTran) {
					commitTran();
				}
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
			boolean newTran = false;
			try {
				newTran = beginTran();
				_cache.removeAll();
				_cacheSize = 0;
			} finally {
				if (newTran) {
					commitTran();
				}
			}
		}
	}

	private boolean isFull() {
		synchronized (_cacheLock) {
			return _cacheSize == _cacheMaxSize;
		}
	}

	private void incrementCacheSize() {
        if (_log.isDebugEnabled()) {
            _log.debug("Asked to increment the " + CACHENAME + " cache.");
        }
        int curMax = -1;
		int newMax = -1;
		synchronized (_cacheLock) {
			curMax = _cache.getCacheConfiguration().getMaxElementsInMemory();
			newMax = curMax  + CACHESIZEINCREMENT;
			_cache.getCacheConfiguration().setMaxElementsInMemory(newMax);
			_cacheMaxSize = newMax;
		}
        if (_log.isDebugEnabled()) {
            _log.debug("Increased cache size from " + curMax + " to " + newMax);
        }
	}
}
