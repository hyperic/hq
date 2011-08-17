/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.agent.server;

import java.lang.reflect.Array;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gemstone.gemfire.cache.CacheException;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.query.Query;
import com.gemstone.gemfire.cache.query.QueryService;
import com.gemstone.gemfire.cache.query.SelectResults;
import com.gemstone.gemfire.internal.Assert;

/** 
 * A queue whose contents are stored in a GemFire Region. 
 *
 * If a problem is encountered while accessing the underlying
 * <code>Region</code>, a {@link RegionQueueException} is thrown.
 *
 */
public class RegionQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private final static Log log = 
        LogFactory.getLog(RegionQueue.class);

    /**
     * The maximum number of elements allowed in this queue.
     * Note: this is NOT the capacity of the region.
     */
    private final int capacity;

    /**
     * The region that stores the contents of this queue.
     * The keys of this region are increasing Long objects
     * providing FIFO ordering of queue elements.
     */
    private final Region<Long, E> region;

    /** Tail index of the queue. */
    private long tailIdx;

    /** Head index of the queue. */
    private long headIdx;

    /** The number of elements in this queue */
    private int size;

    /** The number of remaining slots in this queue */
    private int remaining;

    /** Coordinator for waiting threads. */
    private Object monitor = new Object();

    /** Flag telling if queue has been initialized. */
    private boolean initialized = false;

    /** Init lock */
    private Object lock = new Object();
    
    /** Gemfire query service */
    private QueryService queryService;

    /**
     * Creates a new <code>RegionQueue</code> that stores elements in a
     * given <code>Region</code>.
     *
     * @param capacity
     *        The maximum number of elements that can be stored in the
     *        queue before {@link #put} blocks.
     * @param region
     *        The region in which the queue's elements are stored
     *
     * @throws IllegalArgumentException
     *         If <code>capacity</code> is less than or equal to zero
     */
    public RegionQueue(int capacity, Region<Long, E> region, QueryService queryService) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " +
                    capacity);
        }

        this.capacity = capacity;
        this.region = region;
        this.queryService = queryService;
    }

    /**
     * Initialises this queue state from region where
     * data is kept. 
     */
    private void initialize() {
        
        synchronized (lock) {
            if (this.initialized) {
                return;
            }

            log.info("Initializing spool for region " + region.getFullPath());

            Collection<Long> keys = this.region.keySet();
            this.size = keys.size();
            this.remaining = this.capacity - this.size;

            long smallest = Long.MAX_VALUE;
            long largest = 0L;

            for (long key : keys) {
                if (key < smallest) {
                    smallest = key;
                }

                if (key > largest) {
                    largest = key;
                }
            }

            if (smallest == Long.MAX_VALUE) {
                this.headIdx = 10L;

            } else {
                this.headIdx = smallest;
            }

            if (largest == 0L) {
                this.tailIdx = 10L;

            } else {
                this.tailIdx = largest;
            }

            this.initialized = true;
        }
    }

    /**
     * Closes this RegionQueue and releases all underlying
     * resources such as the region that holds the contents of the queue.
     */
    public void close() {
        this.region.localDestroyRegion();
    }

    /**
     * Implements the put/offer behavior.  Caller should be synchronized
     * on monitor.
     */
    private void insert(E element) {
        remaining--;
        Long key = new Long(tailIdx);
        tailIdx++;
        region.put(key, element);
    }

    /**
     * Implements the take/poll behavior.  Caller should be synchronized
     * on this.
     */
    private E extract() {
        --size;
        Long key = new Long(headIdx);
        E element;
        try {
            element = this.region.get(key);
            this.region.destroy(key);

        } catch (CacheException ex) {
            String s = "While accessing key \"" + key + "\" in region " +
            this.region.getFullPath();
            throw new RegionQueueException(s, ex);
        }
        headIdx++;
        return element;
    }

    /**
     * Increments the number of empty slots and notifies threads that
     * are waiting for space.
     */
    private void incEmptySlots() {
        synchronized (monitor) {
            ++remaining;
            monitor.notify();
        }
    }

    /**
     * Increments the number of used slots and notifies threads that
     * are waiting for space.
     */
    private void incUsedSlots() {
        synchronized (this) {
            ++size;
            this.notify();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.BlockingQueue#put(java.lang.Object)
     */
    public void put(E o) throws InterruptedException {
        if (o == null) throw new NullPointerException();
        if (Thread.interrupted()) throw new InterruptedException();

        initialize();

        synchronized (monitor) {
            while (remaining <= 0) {
                try {
                    monitor.wait();

                } catch (InterruptedException ex) {
                    monitor.notify();
                    throw ex;
                }
            }
            insert(o);
        }
        incUsedSlots();
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.BlockingQueue#take()
     */
    public E take() throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();

        initialize();

        E old = null;
        synchronized (this) {
            while (size <= 0) {
                try {
                    this.wait();

                } catch (InterruptedException ex) {
                    this.notify();
                    throw ex;
                }
            }
            old = extract();
        }

        incEmptySlots();
        return old;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Queue#peek()
     */
    public E peek() {
        initialize();

        synchronized (this) {
            if (size > 0) {
                Long key = new Long(headIdx);
                return region.get(key);
            } else {
                return null;
            }
        }
    }

    @Override
    public boolean isEmpty() {
        initialize();
        return this.size == 0;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Queue#poll()
     */
    public E poll() {
        initialize();

        E old = null;

        synchronized (this) {
            if (size <= 0) {
                return null;
            }
            old = extract();
        }

        incEmptySlots();
        Assert.assertTrue(old != null,
        "Should have had an element available in queue");
        return old;
    }

    @Override
    public int size() {
        initialize();
        return this.size;
    }

    @SuppressWarnings("unchecked")
    private <E> E[] fillArray(E[] fill) {
        synchronized (this) {
            int resultSize = size;
            if (resultSize < 0) {
                resultSize = 0;
            }
            if (fill == null) {
                fill = (E[]) new Object[resultSize];
            } else if (fill.length != resultSize) {
                if (fill.length < resultSize) {
                    Class<?> componentType = fill.getClass().getComponentType();
                    fill = (E[]) Array.newInstance(componentType, resultSize);
                } else {
                    Arrays.fill(fill, resultSize, fill.length-1, null);
                }
            }
            long idx = headIdx;
            for (int i = 0; i < resultSize; i++) {
                Long key = new Long(idx);
                fill[i] = (E) this.region.get(key);
                idx++;
            }
        }
        return fill;
    }

    @Override
    public <E> E[] toArray(E[] array) {
        initialize();
        return fillArray(array);
    }

    @Override
    public Object[] toArray() {
        initialize();
        return fillArray(null);
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.BlockingQueue#poll(long, java.util.concurrent.TimeUnit)
     */
    public E poll(long timeout, TimeUnit unit)
    throws InterruptedException {

        if (Thread.interrupted()) throw new InterruptedException();

        initialize();

        E old = null;

        long msecs = TimeUnit.MILLISECONDS.convert(timeout, unit);
        long start = (msecs <= 0)? 0 : System.currentTimeMillis();
        long waitTime = msecs;

        synchronized (this) {
            while (size <= 0) {
                if (waitTime <= 0) {
                    return null;
                }

                try {
                    this.wait(waitTime);

                } catch (InterruptedException ex) {
                    this.notify();
                    throw ex;
                }

                waitTime = msecs - (System.currentTimeMillis() - start);
            }
            old = extract();
        }

        incEmptySlots();
        Assert.assertTrue(old != null,
        "Should have had an element available in queue");
        return old;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Queue#offer(java.lang.Object)
     */
    public boolean offer(E o) {
        if (o == null) throw new NullPointerException();
        initialize();
        synchronized (monitor) {
            if (remaining > 0) {
                insert(o);
                incUsedSlots();
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object, long, java.util.concurrent.TimeUnit)
     */
    public boolean offer(E o, long timeout, TimeUnit unit)
    throws InterruptedException {

        if (o == null) throw new NullPointerException();
        if (Thread.interrupted()) throw new InterruptedException();

        initialize();

        long msecs = TimeUnit.MILLISECONDS.convert(timeout, unit);
        long start = (msecs <= 0)? 0 : System.currentTimeMillis();
        long waitTime = msecs;

        synchronized (monitor) {
            while (remaining <= 0) {
                if (waitTime <= 0) {
                    return false;
                }

                try {
                    monitor.wait(waitTime);

                } catch (InterruptedException ex) {
                    monitor.notify();
                    throw ex;
                }
                waitTime = msecs - (System.currentTimeMillis() - start);
            }
            insert(o);
        }

        incUsedSlots();
        return true;
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.BlockingQueue#remainingCapacity()
     */
    public int remainingCapacity() {
        initialize();
        return this.capacity - size();
    }

    @Override
    public Iterator<E> iterator() {
        
        String queryStr = "SELECT DISTINCT * FROM " + region.getFullPath()  + ".entries entry ORDER BY entry.key";
        Query query = queryService.newQuery(queryStr);

        try {
            SelectResults results = (SelectResults)query.execute();
            return new SelectResultsIterator(results, region);
        } catch (Exception e) {
        }

        return null;
    }

    public Iterator<E> queryIterator(String query) {
        throw new UnsupportedOperationException("Not implemented yet");   
    }

    public int drainTo(Collection<? super E> c) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns description of this region based queue. 
     */
    @Override
    public String toString() {
        return "RegionQueue for Region: " + region 
        + " capacity: " + capacity
        + (this.initialized ? " initialized" : " uninitialized");
    }

    public String toDebugString() {
        return "RegionQueue for Region: " + region 
        + " capacity: " + capacity
        + " size: " + size
        + " remaining: " + remaining
        + " tailIdx: " + tailIdx
        + " headIdx: " + headIdx
        + " region size:" + region.size()
        + (this.initialized ? " initialized" : " uninitialized");
    }

    public class SelectResultsIterator implements Iterator {

        private Iterator iter;
        private Region.Entry current;
        private Region r;
        
        private SelectResultsIterator(SelectResults results, Region r) {
            this.iter = results.iterator();
            this.r = r;
        }
        
        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() {
            current = (Region.Entry)iter.next();
            return current.getValue();
        }

        public void remove() {
            
            log.info("Queue status before remove: " + toDebugString());
            
            if(current != null) {
                Long curKey = (Long)current.getKey();
                Long key = new Long(headIdx);
                if(curKey.equals(key)) {
                    poll();
                } else {
                    log.info("NOTHERE1: " + curKey + "/" + key);
                    throw new RegionQueueException("Tried to remove non head item from iterator.");
                }
            }
            
            log.info("Queue status after remove: " + toDebugString());

        }
        
    }

}
