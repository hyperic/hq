package org.hyperic.hq.measurement.test;

import junit.framework.TestCase;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.timer.StopWatch;
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

        // Try to save the already cached value
        wasAdded = cache.add(id, cachedVal);
        assertFalse(wasAdded);
        
        // Lookup non-existant entry
        MetricValue nullVal = cache.get(new Integer(CACHE_CAPACITY), ts);
        assertNull(nullVal);
    }
}
