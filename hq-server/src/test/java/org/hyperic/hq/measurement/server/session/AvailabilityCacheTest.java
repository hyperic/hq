/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.measurement.server.session;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AvailabilityCacheTest extends TestCase {

    private static Log _log = LogFactory.getLog(AvailabilityCacheTest.class);
    private AvailabilityCache cache;
    
    public void setUp() throws Exception {
        super.setUp();
        this.cache = new AvailabilityCache();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        cache.clear();
    }
  
    public void testCacheTransactionThreads() throws Exception {
        Thread thread = new Thread() {
            public void run() {
                int id = 0;
                cache.beginTran();
                DataPoint dp = new DataPoint(id, 1.0, 1);
                cache.put(new Integer(id), dp);
                dp = new DataPoint(id, 1.0, 2);
                cache.put(new Integer(id), dp);
                cache.commitTran();
            }
        };
        thread.start();

        int id = 0;
        cache.beginTran();
        DataPoint dp = new DataPoint(id, 1.0, 3);
        cache.put(new Integer(id), dp);
        dp = new DataPoint(id, 1.0, 4);
        cache.put(new Integer(id), dp);
        cache.rollbackTran();

        // don't want to hang the build
        thread.join(5000);
        if (thread.isAlive()) {
            thread.interrupt();
            assertTrue(false);
            return;
        }

        DataPoint curr = (DataPoint)cache.get(new Integer(id));
        assertTrue(2 == curr.getTimestamp());
    }

    public void testCacheTransaction2() throws Exception {
        int id = 0;
        DataPoint first = new DataPoint(id, 0.0, 0);
        cache.put(new Integer(id), first);

        cache.beginTran();
        DataPoint dp = new DataPoint(id, 1.0, 1);
        cache.put(new Integer(id), dp);
        dp = new DataPoint(id, 1.0, 2);
        cache.put(new Integer(id), dp);
        cache.commitTran();

        DataPoint curr = (DataPoint)cache.get(new Integer(id));
        assertTrue(dp.getTimestamp() == curr.getTimestamp());
    }

    public void testCacheTransaction1() throws Exception {
        int id = 0;
        DataPoint first = new DataPoint(id, 0.0, 0);
        cache.put(new Integer(id), first);

        cache.beginTran();
        DataPoint dp = new DataPoint(id, 1.0, 1);
        cache.put(new Integer(id), dp);
        dp = new DataPoint(id, 1.0, 2);
        cache.put(new Integer(id), dp);
        cache.rollbackTran();

        DataPoint curr = (DataPoint)cache.get(new Integer(id));
        assertTrue(first.getValue() == curr.getValue());
    }
    
    public void testCacheTransaction0() throws Exception {
        cache.beginTran();
        int id = 0;
        DataPoint dp = new DataPoint(id, 0, 0);
        cache.put(new Integer(id), dp);
        cache.rollbackTran();

        assertTrue(cache.get(new Integer(id)) == null);
    }

    /**
     * Test a full load of the cache.
     * @throws Exception If any error occurs within the test.
     */
    public void testLoadFull() throws Exception { 
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
        int i = 0;
        DataPoint dp = new DataPoint(0, 0, 0);
        for (; i < AvailabilityCache.CACHESIZE * 2; i++) {
            cache.put(new Integer(i), dp);
        }

        assertEquals(i, cache.getSize());
    }
}
