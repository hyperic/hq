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

package org.hyperic.hq.measurement.server.session;

import org.hyperic.hibernate.PersistedObject;

public class Baseline extends PersistedObject
    implements java.io.Serializable {
    
    // Fields
    private Measurement _measurement;
    private long _computeTime;
    private boolean _userEntered = false;
    private Double _mean;
    private Double _minExpectedVal;
    private Double _maxExpectedVal;

    // Constructors
    public Baseline() {
    }

    public Baseline(Measurement measurement, long computeTime,
                    boolean userEntered, Double mean, Double minExpectedVal,
                    Double maxExpectedVal) {
        _measurement = measurement;
        _computeTime = computeTime;
        _userEntered = userEntered;
        _mean = mean;
        _minExpectedVal = minExpectedVal;
        _maxExpectedVal = maxExpectedVal;
    }

    // Property accessors
    public Measurement getMeasurement() {
        return _measurement;
    }
    
    public void setMeasurement(Measurement measurement) {
        _measurement = measurement;
    }

    public long getComputeTime() {
        return _computeTime;
    }
    
    public void setComputeTime(long computeTime) {
        _computeTime = computeTime;
    }

    public boolean isUserEntered() {
        return _userEntered;
    }
    
    public void setUserEntered(boolean userEntered) {
        _userEntered = userEntered;
    }

    public Double getMean() {
        return _mean;
    }
    
    protected void setMean(Double mean) {
        _mean = mean;
    }

    public Double getMinExpectedVal() {
        return _minExpectedVal;
    }
    
    protected void setMinExpectedVal(Double minExpectedVal) {
        _minExpectedVal = minExpectedVal;
    }

    public Double getMaxExpectedVal() {
        return _maxExpectedVal;
    }
    
    protected void setMaxExpectedVal(Double maxExpectedVal) {
        _maxExpectedVal = maxExpectedVal;
    }

    /**
     * Update a Baseline
     */
    public void update(long computeTime, boolean userEntered,
                       Double mean, Double minExpectedValue,
                       Double maxExpectedValue) {
        setComputeTime(computeTime);
        setUserEntered(userEntered);
        setMean(mean);
        setMinExpectedVal(minExpectedValue);
        setMaxExpectedVal(maxExpectedValue);
    }
}


