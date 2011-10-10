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

/**
 * Interface to known and supported cache region statistics.
 */
public interface CacheRegionStatistic {

    /**
     * Gets configured name of the cache region.
     * 
     * @return Name of the cache region
     */
    public String getName();

    /**
     * Gets max number of items in the region.
     * 
     * @return Max number of items in the region.
     */
    public Long getLimit();
    
    /**
     * Gets current memory usage of the regions.
     * 
     * @return Current memory usage as raw bytes.
     */
    public Long getMemoryUsage();
    
    /**
     * Gets current size of the region.
     * 
     * @return Size of the region.
     */
    public Long getSize();
    
    /**
     * Gets current cache region hits.
     * 
     * @return Cache hits.
     */
    public Long getHits();
    
    /**
     * Gets current cache misses.
     * 
     * @return Cache misses.
     */
    public Long getMisses();

}
