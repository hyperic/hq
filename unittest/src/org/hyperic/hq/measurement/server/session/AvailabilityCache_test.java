package org.hyperic.hq.measurement.server.session;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AvailabilityCache_test extends TestCase {

    private static Log _log = LogFactory.getLog(AvailabilityCache_test.class);

    private AvailabilityCache getClearedCache() {
        AvailabilityCache cache = AvailabilityCache.getInstance();
        cache.clear();
        return cache;
    }

    /**
     * Test a full load of the cache.
     * @throws Exception If any error occurs within the test.
     */
    public void testLoadFull() throws Exception {
        AvailabilityCache cache = getClearedCache();
        
        int i = 0;
        long start = System.currentTimeMillis();
        DataPoint dp = new DataPoint(0, 0, 0);
        for (; i < AvailabilityCache.CACHESIZE; i++) {
            cache.put(new Integer(i), dp);
        }
        _log.info("Filled " + i + " items in " +
                  (System.currentTimeMillis() - start) + " ms.");

        assertEquals(i, cache.getSize());
    }

    /**
     * Test a full load of the cache plus a single update to ensure the
     * cache does not grow.
     * @throws Exception If any error occurs within the test.
     */
    public void testLoadFullOneUpdate() throws Exception {
        AvailabilityCache cache = getClearedCache();

        int i = 0;
        DataPoint dp = new DataPoint(0, 0, 0);
        for (; i < AvailabilityCache.CACHESIZE; i++) {
            cache.put(new Integer(i), dp);
        }

        cache.put(new Integer(0), dp);
         
        assertEquals(i, cache.getSize());
    }

    /**
     * Test updates to the cache.
     * @throws Exception If any error occurs within the test.
     */
    public void testUpdates() throws Exception {
        AvailabilityCache cache = getClearedCache();

        DataPoint dp = new DataPoint(0, 0, 0);
        int i = 0;
        for (; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                cache.put(new Integer(i), dp);
            }
        }

        assertEquals(i, cache.getSize());
    }

    /**
     * Test getting Objects from the cache which may or may not exist.
     * @throws Exception If any error occurs within the test.
     */
    public void testGet() throws Exception {
        AvailabilityCache cache = getClearedCache();

        DataPoint dp = new DataPoint(0, 0, System.currentTimeMillis());
        cache.put(new Integer(0), dp);

        // Test null case
        assertNull(cache.get(new Integer(1)));

        // Test the object we just extracted from the cache.
        DataPoint cachedDp = cache.get(new Integer(0));
        assertEquals(dp.getTimestamp(), cachedDp.getTimestamp());
    }

    /**
     * Test getting an object from the cache which does not exist so the
     * default state is returned.
     * @throws Exception If any error occurs within the test.
     */
    public void testGetDefaultState() throws Exception {
        AvailabilityCache cache = getClearedCache();

        DataPoint defaultState = new DataPoint(0, 0, System.currentTimeMillis());

        // Test the default was returned
                DataPoint cachedDp = cache.get(new Integer(0), defaultState);
        assertEquals(cachedDp.getTimestamp(), defaultState.getTimestamp());

        // Test the point was actually added to the cache.
        DataPoint dp = cache.get(new Integer(0));
        assertEquals(dp.getTimestamp(), defaultState.getTimestamp());
    }

    /**
     * Test growing the maximum size of the cache.   This test should be
     * executed last since it modifies the size of the cache.
     * @throws Exception If any error occurs within the test.
     */
    public void testCacheIncrement() throws Exception {
        AvailabilityCache cache = getClearedCache();

        int i = 0;
        DataPoint dp = new DataPoint(0, 0, 0);
        for (; i < AvailabilityCache.CACHESIZE * 2; i++) {
            cache.put(new Integer(i), dp);
        }

        assertEquals(i, cache.getSize());
    }
}
