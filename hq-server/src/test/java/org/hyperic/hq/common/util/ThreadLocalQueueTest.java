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

package org.hyperic.hq.common.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests the ThreadLocalEventQueue class.
 */
public class ThreadLocalQueueTest extends TestCase {

    /**
     * Creates an instance.
     *
     * @param name
     */
    public ThreadLocalQueueTest(String name) {
        super(name);
    }
    
    public void testEmptyQueue() {
        ThreadLocalQueue queue = ThreadLocalQueue.getInstance();
        
        queue.clearEnqueuedObjects();
        
        assertTrue("queue should be empty.", queue.getEnqueuedObjects().isEmpty());
    }
    
    public void testNullObjectEnqueue() {
        ThreadLocalQueue queue = ThreadLocalQueue.getInstance();
        
        try {
            queue.enqueueObject(null);
            fail("Expected NullPointerException.");
        } catch (NullPointerException e) {
            // expected outcome
        }
    }

    public void testSingleThreadedEnqueue() {
        ThreadLocalQueue queue = ThreadLocalQueue.getInstance();
        
        queue.clearEnqueuedObjects();
        
        List objects = new ArrayList();
        objects.add("one");
        objects.add("two");
        
        for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
            String object = (String) iterator.next();
            queue.enqueueObject(object);
        }
        
        assertFalse("queue should not be empty.", queue.getEnqueuedObjects().isEmpty());
        
        List enqueued = new ArrayList(queue.getEnqueuedObjects());
        
        assertEquals(objects, enqueued);
        
        queue.clearEnqueuedObjects();
        
        assertTrue("queue should be empty.", queue.getEnqueuedObjects().isEmpty());
    }
    
    public void testMultiThreadedEnqueue() throws Exception {
        
        final List objects1 = new ArrayList();
        objects1.add("one");
        objects1.add("two");
        
        final List objects2 = new ArrayList();
        objects2.add("three");
        objects2.add("four");
        objects2.add("five");
        
        final List enqueued1 = new ArrayList();
        
        final List enqueued2 = new ArrayList();
        
        Runnable runnable1 = new Runnable() {
            public void run() {
                ThreadLocalQueue queue = ThreadLocalQueue.getInstance();
                
                for (Iterator iterator = objects1.iterator(); iterator.hasNext();) {
                    String object = (String) iterator.next();
                    queue.enqueueObject(object);
                }
                
                enqueued1.addAll(queue.getEnqueuedObjects());
            }
        };
        
        Runnable runnable2 = new Runnable() {
            public void run() {
                ThreadLocalQueue queue = ThreadLocalQueue.getInstance();
                
                for (Iterator iterator = objects2.iterator(); iterator.hasNext();) {
                    String object = (String) iterator.next();
                    queue.enqueueObject(object);
                }
                
                enqueued2.addAll(queue.getEnqueuedObjects());
            }
        };
        
        Thread thread1 = new Thread(runnable1);
        thread1.setDaemon(true);
        
        Thread thread2 = new Thread(runnable2);
        thread2.setDaemon(true);
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        assertEquals(objects1, enqueued1);
        assertEquals(objects2, enqueued2);
        
        // queue for current thread should be empty
        ThreadLocalQueue queue = ThreadLocalQueue.getInstance();
        
        assertTrue("queue should be empty.", queue.getEnqueuedObjects().isEmpty());
    }
    
}
