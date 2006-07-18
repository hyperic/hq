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

package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.collection.IntHashMap;

import org.apache.commons.logging.Log;

/**
 *
 * Cache last data point so that we don't have to go into the database for it
 */

public class MetricDataCache {
    private IntHashMap data = new IntHashMap(50000);
    
    private static MetricDataCache singleton = new MetricDataCache();
    
    /**
     * Singleton accessor
     */
    public static MetricDataCache getInstance() {
        return singleton;
    }

    private MetricDataCache() {}
    
    public void add(Integer mid, MetricValue mval) {
        MetricValue oldVal = (MetricValue) data.get(mid.intValue());

        // Existing data is actually newer than value
        if (oldVal != null && oldVal.getTimestamp() > mval.getTimestamp())
            return;
        
        data.put(mid.intValue(), mval);
    }
    
    public MetricValue get(Integer mid, long timestamp) {
        MetricValue val = (MetricValue) data.get(mid.intValue());
            
        if (val != null && val.getTimestamp() >= timestamp)
            return val;

        return null;
    }
    
    public boolean hasChanged(Integer mid, MetricValue mval) {
        MetricValue oldVal = (MetricValue) data.get(mid.intValue());

        // Existing data is actually newer than value
        if (oldVal != null && oldVal.getTimestamp() < mval.getTimestamp())
            return oldVal.getValue() != mval.getValue();
        
        return true;
    }
    
    public int logSize(Log log) {
        int size = data.size();
        log.info("MetricDataCache size = " + size);
        return size;
    }
}
