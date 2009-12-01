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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A singleton that can enqueue objects on a thread local for later retrieval in 
 * batch.
 */
public class ThreadLocalQueue {

    private static final ThreadLocalQueue INSTANCE = new ThreadLocalQueue();

    private static final ThreadLocal THREADLOCAL_QUEUE = new ThreadLocal();

    
    /**
     * @return The instance.
     */
    public static ThreadLocalQueue getInstance() {
        return INSTANCE;
    }
    
    /**
     * Private constructor for singleton.
     */
    private ThreadLocalQueue() {
    }
        
    /**
     * Enqueue an object on the thread local.
     * 
     * @param object The object.
     * @throws NullPointerException if the object is <code>null</code>.
     */
    public void enqueueObject(Object object) {
        if (object == null) {
            throw new NullPointerException("trying to enqueue null.");
        }
        
        List queue = (List)THREADLOCAL_QUEUE.get();
        
        if (queue == null) {
            queue = new LinkedList();
            THREADLOCAL_QUEUE.set(queue);
        }
        
        queue.add(object);
    }
    
    /**
     * Retrieve all the objects enqueued on the thread local.
     * 
     * @return A list containing the enqueued objects.
     */
    public List getEnqueuedObjects() {
        List queue = (List)THREADLOCAL_QUEUE.get();
        
        if (queue != null && !queue.isEmpty()) {
            return new ArrayList(queue);
        } else {
            return Collections.EMPTY_LIST;
        }
    }
        
    /**
     * Clear the events objects from the thread local.
     */
    public void clearEnqueuedObjects() {
        THREADLOCAL_QUEUE.set(null);
    }

}
