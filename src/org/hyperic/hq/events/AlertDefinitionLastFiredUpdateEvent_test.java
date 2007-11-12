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

package org.hyperic.hq.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

/**
 * Tests the AlertDefinitionLastFiredUpdateEvent class.
 */
public class AlertDefinitionLastFiredUpdateEvent_test extends TestCase {

    public AlertDefinitionLastFiredUpdateEvent_test(String name) {
        super(name);
    }
    
    /**
     * Test compareTo() and that comparable objects are equal.
     */
    public void testCompareToAndEquals() {
        Integer id1 = new Integer(1);
        Integer id2 = new Integer(2);
        
        long time1 = 1000;
        long time2 = 2000;
        
        AlertDefinitionLastFiredUpdateEvent event1 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time1);
        
        AlertDefinitionLastFiredUpdateEvent event2 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time1);
        
        AlertDefinitionLastFiredUpdateEvent event3 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time2);
        
        AlertDefinitionLastFiredUpdateEvent event4 = 
            new AlertDefinitionLastFiredUpdateEvent(id2, time1);

        assertEquals(0, event1.compareTo(event1));
        assertTrue(event1.equals(event1));
        
        assertEquals(0, event1.compareTo(event2));
        assertTrue(event1.equals(event2));
        
        assertTrue(event1.compareTo(event3) < 0);
        assertTrue(event3.compareTo(event1) > 0);
        assertFalse(event1.equals(event3));
        
        assertTrue(event1.compareTo(event4) < 0);
        assertTrue(event4.compareTo(event1) > 0);
        assertFalse(event1.equals(event4));
        
        // note that we compare last fired times first, then alert def ids.
        assertTrue(event3.compareTo(event4) > 0);
        assertTrue(event4.compareTo(event3) < 0);
        assertFalse(event3.equals(event4));
    }
    
    /**
     * Test that equal objects have equal hash codes.
     */
    public void testHashCode() {
        Integer id1 = new Integer(1);
      
        long time1 = 1000;
        
        AlertDefinitionLastFiredUpdateEvent event1 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time1);
        
        AlertDefinitionLastFiredUpdateEvent event2 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time1);
        
        assertTrue(event1.equals(event1));
        assertEquals(event1.hashCode(), event1.hashCode());
        
        assertTrue(event1.equals(event2));
        assertEquals(event1.hashCode(), event2.hashCode());
    }
    
    /**
     * Test sorting the alert def events, largest to smallest. We sort first 
     * by last fired time, then by alert def id. This is how we sort these 
     * events in the alert def last fired time updater.
     */
    public void testSortingAlertDefEvents() {     
        Integer id1 = new Integer(1);
        Integer id2 = new Integer(2);
        
        long time1 = 1000;
        long time2 = 2000;
        
        AlertDefinitionLastFiredUpdateEvent event1 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time1);
        
        AlertDefinitionLastFiredUpdateEvent event2 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time1);
        
        AlertDefinitionLastFiredUpdateEvent event3 = 
            new AlertDefinitionLastFiredUpdateEvent(id1, time2);
        
        AlertDefinitionLastFiredUpdateEvent event4 = 
            new AlertDefinitionLastFiredUpdateEvent(id2, time1);
        
        AlertDefinitionLastFiredUpdateEvent event5 = 
            new AlertDefinitionLastFiredUpdateEvent(id2, time2);
        
        // sorting largest to smallest - use a reverse order comparator
        Set orderedEvents = new TreeSet(Collections.reverseOrder());
        
        orderedEvents.add(event1);
        orderedEvents.add(event2);
        orderedEvents.add(event3);
        orderedEvents.add(event4);
        orderedEvents.add(event5);
        
        // this is a set so there should be only one instance of equal elements
        // in the collection
        orderedEvents.add(event1);
        orderedEvents.add(event4);

        List doOrdering = new ArrayList(orderedEvents);
        
        List expectedOrder = new ArrayList();
        expectedOrder.add(event5);
        expectedOrder.add(event3);
        expectedOrder.add(event4);
        expectedOrder.add(event1);
        
        assertEquals(expectedOrder, doOrdering);
    }
    
}
