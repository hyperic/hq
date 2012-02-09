package org.hyperic.lather.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.hyperic.hq.common.SystemException;
import org.junit.Assert;
import org.junit.Test;

public class ConnManagerTest {
    
    private static final String METHOD0 = "method0";
    private static final String METHOD1 = "method1";
    
    @Test
    public void testIntialization() {
        Map<String, Semaphore> maxConnMap = new HashMap<String, Semaphore>();
        maxConnMap.put(METHOD0, new Semaphore(1));
        maxConnMap.put(METHOD1, new Semaphore(1));
        boolean caught = false;
        try {
            new ConnManager(maxConnMap);
        } catch (SystemException e) {
            caught = true;
        }
        Assert.assertTrue("ConnManager should throw a SystemException if prop=" +
            ConnManager.PROP_MAXCONNS + " is not set", caught);
        caught = false;
        maxConnMap.put(ConnManager.PROP_MAXCONNS, new Semaphore(1));
        try {
            new ConnManager(maxConnMap);
        } catch (SystemException e) {
            caught = true;
        }
        Assert.assertFalse("ConnManager not should throw a SystemException if prop=" +
            ConnManager.PROP_MAXCONNS + " is set", caught);
    }

    @Test
    public void testGrabConnection() {
        Map<String, Semaphore> maxConnMap = new HashMap<String, Semaphore>();
        maxConnMap.put(METHOD0, new Semaphore(10));
        maxConnMap.put(ConnManager.PROP_MAXCONNS, new Semaphore(100));
        ConnManager connManager = new ConnManager(maxConnMap);

        boolean gotConn = connManager.grabConn(METHOD0);
        Assert.assertTrue("should have been able to grab conn for method0", gotConn);
        int permits = connManager.getAvailablePermits(METHOD0);
        Assert.assertEquals("num permits for method0 should = 9", 9, permits);

        // method1 does not have its own pool, therefore it should grab from the maxConns pool
        gotConn = connManager.grabConn(METHOD1);
        Assert.assertTrue("should have been able to grab conn for method1", gotConn);
        permits = connManager.getAvailablePermits(METHOD1);
        Assert.assertEquals("num permits for method0 should = 99", 99, permits);
    }

    @Test
    public void testReleaseConnection() {
        Map<String, Semaphore> maxConnMap = new HashMap<String, Semaphore>();
        maxConnMap.put(METHOD0, new Semaphore(10));
        maxConnMap.put(ConnManager.PROP_MAXCONNS, new Semaphore(100));
        ConnManager connManager = new ConnManager(maxConnMap);

        // method1 does not have its own pool, therefore it should grab from the maxConns pool
        connManager.grabConn(METHOD1);
        connManager.grabConn(METHOD0);
        connManager.releaseConn(METHOD1);
        int permits = connManager.getAvailablePermits(METHOD1);
        Assert.assertEquals("num permits for method0 should = 100", 100, permits);
        connManager.releaseConn(METHOD0);
        permits = connManager.getAvailablePermits(METHOD0);
        Assert.assertEquals("num permits for method0 should = 10", 10, permits);
    }

    @Test
    public void testGrabAndReleaseConnection() {
        Map<String, Semaphore> maxConnMap = new HashMap<String, Semaphore>();
        maxConnMap.put(METHOD0, new Semaphore(1));
        maxConnMap.put(METHOD1, new Semaphore(1));
        maxConnMap.put(ConnManager.PROP_MAXCONNS, new Semaphore(1));
        ConnManager connManager = new ConnManager(maxConnMap);

        boolean gotConn = connManager.grabConn(METHOD0);
        Assert.assertTrue("should have been able to grab conn for method0", gotConn);
        int permits = connManager.getAvailablePermits(METHOD0);
        Assert.assertEquals("num permits for method0 should = 0", 0, permits);
        gotConn = connManager.grabConn(METHOD0);
        Assert.assertFalse("should not have been able to grab conn for method0", gotConn);

        gotConn = connManager.grabConn(METHOD1);
        Assert.assertTrue("should have been able to grab conn for method1", gotConn);
        permits = connManager.getAvailablePermits(METHOD1);
        Assert.assertEquals("num permits for method1 should = 0", 0, permits);
        gotConn = connManager.grabConn(METHOD1);
        Assert.assertFalse("should not have been able to grab conn for method1", gotConn);

        connManager.releaseConn(METHOD0);
        permits = connManager.getAvailablePermits(METHOD0);
        Assert.assertEquals("num permits for method0 should = 1", 1, permits);
        gotConn = connManager.grabConn(METHOD0);
        Assert.assertTrue("should have been able to grab conn for method0 after it was released", gotConn);

        connManager.releaseConn(METHOD1);
        permits = connManager.getAvailablePermits(METHOD1);
        Assert.assertEquals("num permits for method1 should = 1", 1, permits);
        gotConn = connManager.grabConn(METHOD1);
        Assert.assertTrue("should have been able to grab conn for method0 after it was released", gotConn);
    }

}
