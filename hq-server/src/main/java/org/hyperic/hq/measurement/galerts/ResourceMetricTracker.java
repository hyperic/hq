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

package org.hyperic.hq.measurement.galerts;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.MetricValue;

/**
 * Tracks chronologically the metrics for a given resource.
 */
class ResourceMetricTracker {
    
    private static final Log _log = 
        LogFactory.getLog(ResourceMetricTracker.class);
    
    private final ComparisonOperator _comparator;
    
    private final Float _violatingMetricValue;
    
    private final boolean _isNotReportingOffending;
    
    private final LinkedList _chronOrderedValues;
            
    /**
     * Creates an instance.
     *
     * @param comparator The comparator used to check for violating the trigger 
     *                   condition.
     * @param violatingMetricValue The violating metric value.
     * @param isNotReportingOffending <code>true</code> if not reporting 
     *                                resources are considered offending; 
     *                                <code>false</code> otherwise.                         
     */
    public ResourceMetricTracker(ComparisonOperator comparator,
                                 Float violatingMetricValue,
                                 boolean isNotReportingOffending) {
        _comparator = comparator;
        _violatingMetricValue = violatingMetricValue;
        _isNotReportingOffending = isNotReportingOffending;
        _chronOrderedValues = new LinkedList();
    }
    
    /**
     * Track a new metric value.
     * 
     * @param value The metric value.                            
     */
    public void trackMetricValue(MetricValue value) {
        removeNewerTrackedMetrics(value);
        _chronOrderedValues.add(value);
    }
    
    /**
     * @return The current number of tracked metrics.
     */
    public int getNumberOfTrackedMetrics() {
        return _chronOrderedValues.size();
    }
    
    /**
     * Search for the first metric value violating the trigger condition in 
     * the given time window. Any metric value older than the window start 
     * time will be removed from tracking automatically.
     * 
     * @param startTime The start timestamp for the window (inclusive).
     * @param endTime The end timestamp for the window (inclusive).
     * @return The violating metric value or <code>null</code> if no metric 
     *         violated in the time window. {@link MetricValue#NONE} is returned 
     *         if not reporting resources are considered offending and there 
     *         is no metric value in the time window.
     */
    public MetricValue searchForViolatingMetricInWindow(long startTime, long endTime) {
        boolean debug = _log.isDebugEnabled();
        boolean hasReportedInWindow = false;
        
        for (Iterator iter = _chronOrderedValues.iterator(); iter.hasNext();) {
            MetricValue metric = (MetricValue) iter.next();
            
            long metricTimestamp = metric.getTimestamp();
            
            if (metricTimestamp < startTime) {
                if (debug) {
                    _log.debug("Aging out metrics older than the current " +
                    		   "window start time: metric timestamp="+
                               metricTimestamp+", start time="+startTime);                    
                }
                
                iter.remove();
            } else if (metric.getTimestamp() <= endTime) {
                hasReportedInWindow = true;
                
                if (_comparator.isTrue(new Float(metric.getValue()),
                                       _violatingMetricValue)) {
                    return metric;                    
                }
            } // else The metric value is in the future - evaluate later          
        }
        
        if (_isNotReportingOffending && !hasReportedInWindow) {
            return MetricValue.NONE;
        } else {
            // no violating metrics were found in the window
            return null;
        }
    }
    
    
    /**
     * If there are any newer (or with the same timestamp) tracked metrics than 
     * this one that just came in, remove those newer tracked metrics. This 
     * accounts for the backfilled case where the backfilled metrics reported 
     * one value, but then the agent starts reporting again with the "real" 
     * values that should preempt the backfilled values.
     * 
     * @param value The metric value that just came in.
     */
    private void removeNewerTrackedMetrics(MetricValue value) {
        if (_chronOrderedValues.isEmpty()) {
            return;
        }
        
        if (value.getTimestamp() <= 
            ((MetricValue)_chronOrderedValues.getLast()).getTimestamp()) {
            
            _log.debug("Removing tracked metrics newer than timestamp="+
                        value.getTimestamp());   

            ListIterator iter = 
                _chronOrderedValues.listIterator(_chronOrderedValues.size());

            while (iter.hasPrevious()) {
                MetricValue trackedValue = (MetricValue) iter.previous();

                if (trackedValue.getTimestamp() >= value.getTimestamp()) {
                    iter.remove();
                } else {
                    return;
                }
            }
        }        
    }    
    
}
