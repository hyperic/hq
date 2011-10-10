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
package org.hyperic.hq.cache.statistics;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;

/**
 * Implementation for {@link CacheStatistics} which gets statistics
 * from the local EhCache {@link net.sf.ehcache.CacheManager}.
 */
public class LocalSingleEhCacheStatistics implements CacheStatistics {

    /*
     * (non-Javadoc)
     * @see org.hyperic.hq.cache.statistics.CacheStatistics#getRegionStatistics()
     */
    public List<CacheRegionStatistic> getRegionStatistics() {
        List<CacheRegionStatistic> ret = new ArrayList<CacheRegionStatistic>();
        
        CacheManager cacheManager = CacheManager.getInstance();
        String[] cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            CommonRegionStatistic stat = new CommonRegionStatistic();
            stat.setName(cache.getName());
            CacheConfiguration config = cache.getCacheConfiguration();
            stat.setLimit(new Long(config.getMaxElementsInMemory()));
            stat.setMemoryUsage(cache.calculateInMemorySize());
            stat.setSize(new Long(cache.getSize()));
            stat.setHits(new Long(cache.getStatistics().getCacheHits()));
            stat.setMisses(new Long(cache.getStatistics().getCacheMisses()));
            ret.add(stat);
        }
        return ret;
    }

}
