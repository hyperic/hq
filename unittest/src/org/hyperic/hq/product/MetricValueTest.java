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

package org.hyperic.hq.product;

import junit.framework.TestCase;

/**
 * Tests the MetricValue class.
 */
public class MetricValueTest extends TestCase {

    /**
     * Creates an instance.
     *
     * @param name
     */
    public MetricValueTest(String name) {
        super(name);
    }
    
    /**
     * If the timestamp is not specified and set to the current system time 
     * then it's next to impossible to guarantee logical equality.
     */
    public void testEqualsTimestampNotSpecified() throws Exception {
        long value = 1000;
        MetricValue mval1 = new MetricValue(value);
        
        // sleep 10 msec to make sure we get a new system time value
        Thread.sleep(10);
        
        MetricValue mval2 = new MetricValue(value);
        
        assertFalse(mval1.equals(mval2));
    }
    
    /**
     * With the value and timestamp specified, you should be able to predict 
     * equality and comparable values.
     */
    public void testEqualsTimestampSpecified() {
        long value1 = 1000;
        long value2 = 2000;
        long timestamp1 = 1234;
        long timestamp2 = 3245;
        
        MetricValue mval1 = new MetricValue(value1, timestamp1);
        MetricValue mval2 = new MetricValue(value1, timestamp1);
        
        MetricValue mval3 = new MetricValue(value1, timestamp2);
        MetricValue mval4 = new MetricValue(value2, timestamp1);
        
        // check equal instances
        assertTrue(mval1.equals(mval1));
        assertTrue(mval1.equals(mval2));
        assertTrue(mval2.equals(mval1));

        // equal instances should have comparable values
        assertEquals(0, mval1.compareTo(mval1));
        assertEquals(0, mval1.compareTo(mval2));
        assertEquals(0, mval2.compareTo(mval1));
        
        // now check not equal instances
        assertFalse(mval1.equals(mval3));
        assertFalse(mval1.equals(mval4));
        assertFalse(mval3.equals(mval4));
    }
    
    /**
     * The exact same instance of MetricValue.NONE should be equal to itself 
     * and compareTo() should = zero.
     */
    public void testEqualsSameInstanceOfMetricValueNone() {
        assertEquals("The same instance of MetricValue.NONE should equal itself.", 
                MetricValue.NONE, MetricValue.NONE);
        
        // equal instances should have comparable values
        assertEquals(0, MetricValue.NONE.compareTo(MetricValue.NONE));
    }
        
    /**
     * Two different metric values both with MetricValue.VALUE_NONE are considered 
     * to be equal if their timestamps are the same, but if their timestamps 
     * are different, they are not equal.     
     */
    public void testEqualsDifferentInstanceOfMetricValueNone() throws Exception {
        long timestamp1 = 1000;
        long timestamp2 = 2000;
        
        MetricValue none1 = new MetricValue(MetricValue.VALUE_NONE, timestamp1);
        MetricValue none2 = new MetricValue(MetricValue.VALUE_NONE, timestamp1);
        
        MetricValue none3 = new MetricValue(MetricValue.VALUE_NONE, timestamp2);

        // check equal instances
        assertTrue(none1.equals(none1));
        assertTrue(none1.equals(none2));
        assertTrue(none2.equals(none1));

        // equal instances should have comparable values
        assertEquals(0, none1.compareTo(none1));
        assertEquals(0, none1.compareTo(none2));
        assertEquals(0, none2.compareTo(none1));
        
        // now check not equal instances
        assertFalse(none1.equals(none3));
        assertFalse(none3.equals(none1));
    }
    
    /**
     * The exact same instance of MetricValue.FUTURE should be equal to itself 
     * and compareTo() should = zero.
     */
    public void testEqualsSameInstanceOfMetricValueFuture() {
        assertEquals("The same instance of MetricValue.FUTURE should equal itself.", 
                MetricValue.FUTURE, MetricValue.FUTURE);
        
        // equal instances should have comparable values
        assertEquals(0, MetricValue.FUTURE.compareTo(MetricValue.FUTURE));
    }
        
    /**
     * Two different metric values both with MetricValue.VALUE_FUTURE are considered 
     * to be equal if their timestamps are the same, but if their timestamps 
     * are different, they are not equal.     
     */
    public void testEqualsDifferentInstanceOfMetricValueFuture() throws Exception {
        long timestamp1 = 1000;
        long timestamp2 = 2000;
        
        MetricValue future1 = new MetricValue(MetricValue.VALUE_FUTURE, timestamp1);
        MetricValue future2 = new MetricValue(MetricValue.VALUE_FUTURE, timestamp1);
        
        MetricValue future3 = new MetricValue(MetricValue.VALUE_FUTURE, timestamp2);

        // check equal instances
        assertTrue(future1.equals(future1));
        assertTrue(future1.equals(future2));
        assertTrue(future2.equals(future1));

        // equal instances should have comparable values
        assertEquals(0, future1.compareTo(future1));
        assertEquals(0, future1.compareTo(future2));
        assertEquals(0, future2.compareTo(future1));
        
        // now check not equal instances
        assertFalse(future1.equals(future3));
        assertFalse(future3.equals(future1));
    }    

}
