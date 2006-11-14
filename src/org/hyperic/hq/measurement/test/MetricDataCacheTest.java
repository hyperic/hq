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

package org.hyperic.hq.measurement.test;

import junit.framework.TestCase;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.product.MetricValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Testsuite for the MetricDataCache.  This test should be changed to be
 * multi-threaded in the future.
 *
 * Assumes a cache capacity of 50,0000 items.
 */
public class MetricDataCacheTest extends TestCase {

    private static final int CACHE_CAPACITY = 50000;
    private static final Log _log =
        LogFactory.getLog(MetricDataCacheTest.class);

    public MetricDataCacheTest() {}

    public void testCacheFill() {

        MetricDataCache cache = MetricDataCache.getInstance();
        long ts = System.currentTimeMillis();

        for (int i = 0; i < CACHE_CAPACITY; i++) {
            Integer derivedMeasurementId = new Integer(i);
            MetricValue val = new MetricValue(0, ts++);
            cache.add(derivedMeasurementId, val);
        }

        cache.logSize(_log);

        Integer id = new Integer(CACHE_CAPACITY - 1);
        boolean wasAdded;

        // Update a metric value, assert it has changed in the cache.
        MetricValue newVal = new MetricValue(0, ts + 1);
        wasAdded = cache.add(id, newVal);
        assertTrue(wasAdded);

        // Lookup the cached value
        MetricValue cachedVal = cache.get(id, ts);
        assertNotNull(cachedVal);

        // Try to save old data
        MetricValue oldVal = new MetricValue(0, ts);
        wasAdded = cache.add(id, oldVal);
        assertFalse(wasAdded);
        
        // Lookup non-existant entry
        MetricValue nullVal = cache.get(new Integer(CACHE_CAPACITY), ts);
        assertNull(nullVal);
    }
}
