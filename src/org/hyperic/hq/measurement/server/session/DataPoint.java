package org.hyperic.hq.measurement.server.session;

import java.math.BigDecimal;

import org.hyperic.hq.product.MetricValue;

/**
 * This object encapsulates all the information needed to call 
 * {@link DataManagerEJBImpl#addData(java.util.List, boolean)
 */
public class DataPoint {
    private Integer     _metricId;
    private MetricValue _val;
    
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
}
