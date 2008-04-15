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

package org.hyperic.hq.common;

import junit.framework.TestCase;

/**
 * Tests the MethodInvocationMetricsGroup class.
 */
public class MethodInvocationMetricsGroup_test extends TestCase {

    /**
     * Creates an instance.
     *
     * @param name
     */
    public MethodInvocationMetricsGroup_test(String name) {
        super(name);
    }
    
    /**
     * Expect IllegalArgumentException.
     */
    public void testIllegalQueueCapacity() throws Exception {
        try {
            new MethodInvocationMetricsGroup("myGroup", -1);
            fail("Expect IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expect IllegalArgumentException instead of: "+e);
        }
        
        try {
            new MethodInvocationMetricsGroup("myGroup", 0);
            fail("Expect IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected outcome
        } catch (Exception e) {
            fail("Expect IllegalArgumentException instead of: "+e);
        }
    }
    
    public void testSetGroupName() {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        assertEquals("", metricsGroup.getMetricGroupName());
        
        MethodInvocationMetricsGroup newMetricsGroup = 
            new MethodInvocationMetricsGroup("myGroup");
        
        assertEquals("myGroup", newMetricsGroup.getMetricGroupName());
    }
    
    public void testGetMetricsNoInvocationTimesAdded() {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        assertNoMetrics(metricsGroup);
    }
    
    public void testAddInvocationsPriorToFlushBelowQueueCapacity() {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        assertEquals(MethodInvocationMetricsGroup.DEFAULT_QUEUE_CAPACITY, 
                metricsGroup.getQueueCapacity());
        
        assertTrue(metricsGroup.getQueueCapacity() > 2);
        
        metricsGroup.addInvocationTime(1);
        metricsGroup.addInvocationTime(2);
        
        assertNoMetrics(metricsGroup);
    }
    
    public void testAddInvocationsPriorToFlushExceedQueueCapacity() {
        int capacity = 3;
        
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null, capacity);
        
        assertEquals(capacity, metricsGroup.getQueueCapacity());
        
        metricsGroup.addInvocationTime(0);
        metricsGroup.addInvocationTime(1);
        metricsGroup.addInvocationTime(2);
        
        // the capacity is now full - the next time we add an invocation 
        // time there will be metrics
        assertNoMetrics(metricsGroup);
        
        metricsGroup.addInvocationTime(3);
        
        assertEquals(4, metricsGroup.getNumberInvocations());
        assertEquals(3, metricsGroup.getMaxInvocationTime());
        assertEquals(0, metricsGroup.getMinInvocationTime());
        assertEquals(1.5, metricsGroup.getAverageInvocationTime(), 0.0);               
    }
    
    public void testAddInvocationsSynchronously() {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        metricsGroup.addInvocationTimeSynch(0);
        metricsGroup.addInvocationTimeSynch(1);
        metricsGroup.addInvocationTimeSynch(2);        
        metricsGroup.addInvocationTimeSynch(3);
                
        assertEquals(4, metricsGroup.getNumberInvocations());
        assertEquals(3, metricsGroup.getMaxInvocationTime());
        assertEquals(0, metricsGroup.getMinInvocationTime());
        assertEquals(1.5, metricsGroup.getAverageInvocationTime(), 0.0);          
    }
    
    public void testAddInvocationsFlushExplicitly() {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        assertTrue(metricsGroup.getQueueCapacity() > 4);
        
        metricsGroup.addInvocationTime(0);
        metricsGroup.addInvocationTime(1);
        metricsGroup.addInvocationTime(2);        
        metricsGroup.addInvocationTime(3);
        
        // there should be no metrics before the flush
        assertNoMetrics(metricsGroup);
        
        metricsGroup.flush();
        
        assertEquals(4, metricsGroup.getNumberInvocations());
        assertEquals(3, metricsGroup.getMaxInvocationTime());
        assertEquals(0, metricsGroup.getMinInvocationTime());
        assertEquals(1.5, metricsGroup.getAverageInvocationTime(), 0.0);          
    }
    
    /**
     * The min invocation time should always be set initially to the first 
     * collected invocation time.
     */
    public void testMinInvocationTimeInitializedCorrectly() {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        metricsGroup.addInvocationTime(1000);
        
        metricsGroup.flush();
        
        assertEquals(1, metricsGroup.getNumberInvocations());
        assertEquals(1000, metricsGroup.getMaxInvocationTime());
        assertEquals(1000, metricsGroup.getMinInvocationTime());
        assertEquals(1000.0, metricsGroup.getAverageInvocationTime(), 0.0);          
    }
    
    public void testMetricCalculations() {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        metricsGroup.addInvocationTime(4000);        
        metricsGroup.addInvocationTime(3000);        
        metricsGroup.addInvocationTime(2000);
        
        metricsGroup.flush();
        
        assertEquals(3, metricsGroup.getNumberInvocations());
        assertEquals(4000, metricsGroup.getMaxInvocationTime());
        assertEquals(2000, metricsGroup.getMinInvocationTime());
        assertEquals(3000.0, metricsGroup.getAverageInvocationTime(), 0.0);
        
        // now add some more invocation times and flush again
        metricsGroup.addInvocationTime(5000);
        metricsGroup.addInvocationTime(1000);
        
        metricsGroup.flush();
        
        assertEquals(5, metricsGroup.getNumberInvocations());
        assertEquals(5000, metricsGroup.getMaxInvocationTime());
        assertEquals(1000, metricsGroup.getMinInvocationTime());
        assertEquals(3000.0, metricsGroup.getAverageInvocationTime(), 0.0);
    }
    
    public void testResetMetrics() {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        metricsGroup.addInvocationTime(1000);
        
        metricsGroup.flush();
        
        assertEquals(1, metricsGroup.getNumberInvocations());
        assertEquals(1000, metricsGroup.getMaxInvocationTime());
        assertEquals(1000, metricsGroup.getMinInvocationTime());
        assertEquals(1000.0, metricsGroup.getAverageInvocationTime(), 0.0);  
        
        // now reset the metrics
        metricsGroup.reset();
        
        assertNoMetrics(metricsGroup);
    }
    
    /**
     * Test the performance of adding method invocation times synchronously, 
     * evaluating the group metrics immediately.
     */
    public void testSynchInsertionPerformance() throws Exception {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        int numThreads = 10;
        int numInserts = 100000;
        
        InsertionThread[] threads = new InsertionThread[numThreads];
        
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new InsertionThread(metricsGroup, numInserts, false);
        }
        
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        
        System.out.println("total insert time (sync): "+(System.currentTimeMillis()-start));
    }
    
    /**
     * Test the performance of adding method invocation times asynchronously, 
     * evaluating the group metrics only on flush.
     */
    public void testAsyncInsertionPerformance() throws Exception {
        MethodInvocationMetricsGroup metricsGroup = 
            new MethodInvocationMetricsGroup(null);
        
        int numThreads = 10;
        int numInserts = 100000;
        
        InsertionThread[] threads = new InsertionThread[numThreads];
        
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new InsertionThread(metricsGroup, numInserts, true);
        }
        
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        
        System.out.println("total insert time (async): "+(System.currentTimeMillis()-start));        
    }
    
    private static class InsertionThread extends Thread {
        
        private final MethodInvocationMetricsGroup _metricsGroup;
        private final int _numInserts;
        private final boolean _async;
        
        public InsertionThread(MethodInvocationMetricsGroup metricsGroup, 
                               int numInserts, 
                               boolean async) {
            super("Insertion Thread");
            setDaemon(true);
            _metricsGroup = metricsGroup;
            _numInserts = numInserts;
            _async = async;
        }
        
        public void run() {            
            for (int i = 0; i < _numInserts; i++) {
                
                if (_async) {
                    _metricsGroup.addInvocationTime(i);                    
                } else {
                    _metricsGroup.addInvocationTimeSynch(i);
                }
            }
            
            if (_async) {
                _metricsGroup.flush();
            }
        }
    }
    
    
    private void assertNoMetrics(MethodInvocationMetricsGroup metricsGroup) {
        assertEquals(0, metricsGroup.getNumberInvocations());
        assertEquals(0, metricsGroup.getMaxInvocationTime());
        assertEquals(0, metricsGroup.getMinInvocationTime());
        assertTrue(Double.isNaN(metricsGroup.getAverageInvocationTime()));        
    }    

}
