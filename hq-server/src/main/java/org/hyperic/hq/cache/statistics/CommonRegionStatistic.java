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
 * Placeholder for common cache region metrics.
 * <p>
 * User must ensure that all statistic fields are set or initialized.
 */
public class CommonRegionStatistic implements CacheRegionStatistic {

    private String name;
    private Long limit;
    private Long memoryUsage;
    private Long size;
    private Long hits;
    private Long misses;
    
    public CommonRegionStatistic(String name, Long limit, Long memoryUsage,
            Long size, Long hits, Long misses) {
        this.name = name;
        this.limit = limit;
        this.memoryUsage = memoryUsage;
        this.size = size;
        this.hits = hits;
        this.misses = misses;
    }
    
    public CommonRegionStatistic() {
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getLimit() {
        return limit;
    }
    public void setLimit(Long limit) {
        this.limit = limit;
    }
    public Long getMemoryUsage() {
        return memoryUsage;
    }
    public void setMemoryUsage(Long memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
    public Long getSize() {
        return size;
    }
    public void setSize(Long size) {
        this.size = size;
    }
    public Long getHits() {
        return hits;
    }
    public void setHits(Long hits) {
        this.hits = hits;
    }
    public Long getMisses() {
        return misses;
    }
    public void setMisses(Long misses) {
        this.misses = misses;
    }

    
}
