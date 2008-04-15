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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;

/**
 * An object for maintaining a group of metrics derived from method invocation 
 * times on the instrumented method. This object maintains its own private 
 * internal queue such that the metric values in the group will be updated 
 * only after the internal queue is flushed. Flushing occurs automatically 
 * when the queue capacity is reached but it may be invoked explicitly by any 
 * client wishing to obtain the most recent snapshot of metrics.
 * 
 * Each metric in the group (num invocations/max,min,avg invocation time) is 
 * guaranteed to be consistent with the current flushed state of the group. 
 * There is no guarantee that the metrics within the group will be consistent 
 * with respect to one another especially if there are concurrent threads 
 * flushing and querying the group.
 */
public class MethodInvocationMetricsGroup {

    /**
     * The system property key for the capacity of the internal metrics queue.
     */
    public static final String QUEUE_CAPACITY = 
        "org.hq.diagnostic.method.invocation.metrics.queue.capacity";
    
    /**
     * The default queue capacity (1000).
     */
    public static final int DEFAULT_QUEUE_CAPACITY = 1000;
    
    private static int _queueSize = 
        Integer.getInteger(QUEUE_CAPACITY, DEFAULT_QUEUE_CAPACITY).intValue();
    
    private final Object _lock = new Object();
    
    private final LinkedBlockingQueue _queue;
    
    private final String _metricGroupName;
    private final int _queueCapacity;
    
    private long _numInvocations;
    private long _maxInvocationTime;
    private long _minInvocationTime;
    private long _totalInvocationTime;
    
    
    /**
     * Creates an instance using the invocation time queue capacity specified 
     * by the {@link #QUEUE_CAPACITY} system property value.
     * 
     * @param metricGroupName The metric group name.
     */
    public MethodInvocationMetricsGroup(String metricGroupName) {
        this(metricGroupName, _queueSize);
    }
    
    /**
     * Creates an instance.
     * 
     * @param metricGroupName The metric group name. A <code>null</code> value 
     *                        will be converted to the empty string.
     * @param queueCapacity The capacity of the invocation time queue.
     * @throws IllegalArgumentException if the capacity is not greater than zero. 
     */
    public MethodInvocationMetricsGroup(String metricGroupName, int queueCapacity) {
        if (metricGroupName == null) {
            _metricGroupName = "";
        } else {
            _metricGroupName = metricGroupName;            
        }
        
        _queueCapacity = queueCapacity;
        _queue = new LinkedBlockingQueue(_queueCapacity);
    }
    
    /**
     * @return The metric group name.
     */
    public String getMetricGroupName() {
        return _metricGroupName;
    }
    
    /**
     * @return The capacity of the invocation time queue.
     */
    public int getQueueCapacity() {
        return _queueCapacity;
    }
   
    /**
     * Add an invocation time to the internal queue, flushing the queue if the 
     * capacity is reached.
     * 
     * @param invocationTime The invocation time.
     */
    public void addInvocationTime(long invocationTime) {
        Long latestInvocationTime = new Long(invocationTime);
        
        if (!_queue.offer(latestInvocationTime)) {
            flush(latestInvocationTime, true);
        }
    }
        
    /**
     * Flush the metric group, updating the metrics in the group with the 
     * invocation times currently stored in the queue.
     */
    public void flush() {
        flush(null, false);
    }
    
    /**
     * @return The total number of added invocation time metrics.
     */
    public long getNumberInvocations() {
        synchronized (_lock) {
            return _numInvocations;            
        }
    }
    
    /**
     * @return The maximum invocation time metric.
     */
    public long getMaxInvocationTime() {
        synchronized (_lock) {
            return _maxInvocationTime;
        }
    }
    
    /**
     * @return The minimum invocation time metric.
     */
    public long getMinInvocationTime() {
        synchronized (_lock) {
            return _minInvocationTime;
        }
    }
    
    /**
     * @return The average invocation time metric or {@link Double#NaN} if no 
     *         invocation time metric has been added.
     */
    public double getAverageInvocationTime() {
        synchronized (_lock) {
            if (_numInvocations==0) {
                return Double.NaN;
            }
            
            return (double)_totalInvocationTime/(double)_numInvocations;
        }        
    }
    
    /**
     * Reset all metrics in the group.
     */
    public void reset() {
        synchronized (_lock) {
            _numInvocations = 0;
            _maxInvocationTime = 0;
            _minInvocationTime = 0;
            _totalInvocationTime = 0;
        }
    }
    
    private void flush(Long latestInvocationTime, boolean includeLatest) {
        synchronized (_lock) {
            List invocationTimes = new ArrayList();
            _queue.drainTo(invocationTimes);
            
            if (includeLatest) {
                invocationTimes.add(latestInvocationTime);
            }
            
            for (Iterator it = invocationTimes.iterator(); it.hasNext();) {
                Long invocationTime = (Long) it.next();
                updateMetrics(invocationTime.longValue());
            }
        }         
    }
    
    private void updateMetrics(long nextInvocationTime) {
        _numInvocations++;
        _totalInvocationTime+=nextInvocationTime;
        
        // handle overflows
        if (_numInvocations < 0 || _totalInvocationTime < 0) {
            reset();
            _numInvocations = 1;
            _totalInvocationTime = nextInvocationTime;
        }
        
        if (nextInvocationTime > _maxInvocationTime) {
            _maxInvocationTime = nextInvocationTime;
        }
        
        if (_numInvocations == 1) {
            // we always prime min invocation time with the first value
            _minInvocationTime = nextInvocationTime;
        } else if (nextInvocationTime < _minInvocationTime) {
            _minInvocationTime = nextInvocationTime;
        }        
    }    

}
