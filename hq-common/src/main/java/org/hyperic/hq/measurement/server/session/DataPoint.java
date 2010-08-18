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
    private final Integer     _metricId;
    private final MetricValue _val;
    
    public DataPoint(int metricId, double val, long timestamp) {
        _val = new MetricValue(val, timestamp);
        _metricId = new Integer(metricId);
    }
    
    public DataPoint(Integer metricId, MetricValue val) {
        _metricId = metricId;
        _val      = val;
    }
    
    public Integer getMetricId() {
        return _metricId;
    }
    
    public MetricValue getMetricValue() {
        return _val;
    }
    
    public double getValue() {
        return _val.getValue();
    }
    
    public long getTimestamp() {
        return _val.getTimestamp();
    }
    
    public String toString() {
        return "id=" + _metricId + " val=" + _val.getValue() + 
            " time=" + _val.getTimestamp();
    }
    
    /**
     * Checks if metricId && timestamp hashCodes match (not value)
     */
    public int hashCode() {
        return 17 + (37*_metricId.hashCode()) +
            (37*new Long(_val.getTimestamp()).hashCode());
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
        if (d.getMetricId().equals(getMetricId()) &&
            d.getTimestamp() == getTimestamp()) {
            return true;
        }
        return false;
    }
}
