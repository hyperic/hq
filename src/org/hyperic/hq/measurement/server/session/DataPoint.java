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
