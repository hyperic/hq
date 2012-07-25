/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

import org.hyperic.hq.product.MetricValue;

/**
 * This object encapsulates all the information needed to call 
 * {@link DataManagerImpl#addData(java.util.List, boolean)
 */
public class DataPoint implements Serializable {
    private final Integer     measurementId;
    private final MetricValue metricVal;
    
    public DataPoint(DataPoint other) {
    	this.metricVal = new MetricValue(other.getMetricValue(), other.getTimestamp());
        this.measurementId = other.getMeasurementId();
    }

    /**
     * 
     * @param measId - measurement Id
     * @param metricVal
     * @param timestamp
     */
    public DataPoint(int measId, double metricVal, long timestamp) {
    	this.metricVal = new MetricValue(metricVal, timestamp);
        this.measurementId = new Integer(measId);
    }
    
    public DataPoint(Integer measurementId, MetricValue metricVal) {
        this.measurementId = measurementId;
        this.metricVal      = metricVal;
    }
    
    public Integer getMeasurementId() {
        return measurementId;
    }
    
    public MetricValue getMetricValue() {
        return this.metricVal;
    }
    
    public double getValue() {
        return this.metricVal.getValue();
    }
    
    public long getTimestamp() {
        return this.metricVal.getTimestamp();
    }
    
    public String toString() {
        return "id=" + measurementId + " val=" + this.metricVal.getValue() + 
            " time=" + this.metricVal.getTimestamp();
    }
    
    /**
     * Checks if metricId && timestamp hashCodes match (not value)
     */
    public int hashCode() {
        return 17 + (37*measurementId.hashCode()) +
            (37*new Long(this.metricVal.getTimestamp()).hashCode());
    }
    
    /**
     * Determines if metricId && timestamps match (not value)
     */
    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (!(rhs instanceof DataPoint)) {
            return false;
        }
        DataPoint d = (DataPoint)rhs;
        if (d.getMeasurementId().equals(getMeasurementId()) &&
            d.getTimestamp() == getTimestamp()) {
            return true;
        }
        return false;
    }
}
