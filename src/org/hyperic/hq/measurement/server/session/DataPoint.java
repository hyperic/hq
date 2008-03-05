package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

import org.hyperic.hq.product.MetricValue;

/**
 * This object encapsulates all the information needed to call 
 * {@link DataManagerEJBImpl#addData(java.util.List, boolean)
 */
public class DataPoint implements Serializable {
    private Integer     _metricId;
    private MetricValue _val;
    
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
}
