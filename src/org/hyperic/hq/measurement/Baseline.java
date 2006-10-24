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

package org.hyperic.hq.measurement;

import org.hyperic.hibernate.PersistedObject;

public class Baseline extends PersistedObject
    implements java.io.Serializable {
    
    // Fields
    private DerivedMeasurement _derivedMeasurement;
    private long _computeTime;
    private boolean _userEntered;
    private double _mean;
    private double _minExpectedVal;
    private double _maxExpectedVal;

    // Constructors
    public Baseline() {
    }

    public Baseline(long computeTime, boolean userEntered) {
        _computeTime = computeTime;
        _userEntered = userEntered;
    }

    public Baseline(DerivedMeasurement derivedMeasurement, long computeTime,
                    boolean userEntered, double mean, double minExpectedVal,
                    double maxExpectedVal) {
        _derivedMeasurement = derivedMeasurement;
        _computeTime = computeTime;
        _userEntered = userEntered;
        _mean = mean;
        _minExpectedVal = minExpectedVal;
        _maxExpectedVal = maxExpectedVal;
    }
    
   
    // Property accessors
    public DerivedMeasurement getDerivedMeasurement() {
        return _derivedMeasurement;
    }
    
    public void setDerivedMeasurement(DerivedMeasurement derivedMeasurement) {
        _derivedMeasurement = derivedMeasurement;
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

    public double getMean() {
        return _mean;
    }
    
    public void setMean(double mean) {
        _mean = mean;
    }

    public double getMinExpectedVal() {
        return _minExpectedVal;
    }
    
    public void setMinExpectedVal(double minExpectedVal) {
        _minExpectedVal = minExpectedVal;
    }

    public double getMaxExpectedVal() {
        return _maxExpectedVal;
    }
    
    public void setMaxExpectedVal(double maxExpectedVal) {
        _maxExpectedVal = maxExpectedVal;
    }
}


