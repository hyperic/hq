package org.hyperic.hq.notifications.model;

import org.hyperic.hq.product.MetricValue;

public class MetricNotification implements INotification {
    public Integer getMeasurementId() {
        return measurementId;
    }
    public MetricNotification(Integer resourceId, Integer measurementId, MetricValue metricVal) {
        super();
        this.measurementId = measurementId;
        this.metricVal = metricVal;
    }
    public Integer metricNotification() {
        return measurementId;
    }
    public void setMeasurementId(Integer measurementId) {
        this.measurementId = measurementId;
    }
    public MetricValue getMetricVal() {
        return metricVal;
    }
    public void setMetricVal(MetricValue metricVal) {
        this.metricVal = metricVal;
    }
    protected Integer measurementId;
    protected MetricValue metricVal;
}
