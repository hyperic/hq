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
package org.hyperic.util.stats;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Maintains a list of {@link DataPoint}s which all lie within a specific
 * time range.  This class is useful for storing 'the last 30 minutes of data'
 * and rolling the oldest data points off.
 * 
 * This class relies on datapoints being placed into the collector in
 * time-sorted order.
 */
public class TimeWindowCollector {
    private final LinkedList _dataPoints = new LinkedList();
    private final long _windowSize;
    private boolean _hasRolled = false;
    
    /**
     * @param windowSize The size of the window (in millis) that all the
     *                   encapsulated datapoints should be within.
     */
    public TimeWindowCollector(long windowSize) {
        _windowSize = windowSize;
    }

    /**
     * Add a new datapoint to the collection -- does not roll off old
     * datapoints. 
     */
    public void addPoint(long time, double value) {
        addPoint(new DataPoint(time, value));
    }
    
    /**
     * @see #addPoint(long, double)
     */
    public void addPoint(DataPoint pt) {
        synchronized (_dataPoints) {
            _dataPoints.add(pt);
        }
    }

    /**
     * Returns true if data has been rolled off the back of the time-window.
     * (i.e. removeOldPoints() has been invoked and removed something) 
     */
    public boolean hasRolled() {
        synchronized (_dataPoints) {
            return _hasRolled;
        }
    }
    
    /**
     * Get a sum of all the encapsulated data points
     */
    public double getSum() {
        double res = 0.0;
        
        synchronized (_dataPoints) {
            for (Iterator i=_dataPoints.iterator(); i.hasNext(); ) {
                DataPoint pt = (DataPoint)i.next();
                res += pt.getValue();
            }
        }
        return res;
    }

    /**
     * Remove old datapoints.  This method uses the time of the most recent
     * datapoint as the end of the time range.  
     * 
     * All datapoints older than datapoints[-1] - windowSize will be removed.
     */
    public void removeOldPoints() {
        synchronized (_dataPoints) {
            if (_dataPoints.isEmpty())
                return;
            
            DataPoint last = (DataPoint)_dataPoints.getLast();
            removeOldPoints(last.getTime());
        }
    }
    
    /**
     * Remove old datapoints.  
     * 
     * All datapoints older than currentTime - windowSize will be removed.
     */
    public void removeOldPoints(long currentTime) {
        long oldestTimeAllowed = currentTime - _windowSize;
        
        synchronized (_dataPoints) {
            for (Iterator i=_dataPoints.iterator(); i.hasNext(); ) {
                DataPoint pt = (DataPoint)i.next();
                
                if (pt.getTime() < oldestTimeAllowed) {
                    i.remove();
                    _hasRolled = true;
                } else {
                    break;
                }
            }            
        }
    }
}
