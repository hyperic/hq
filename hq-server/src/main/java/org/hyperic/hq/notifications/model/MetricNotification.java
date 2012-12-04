package org.hyperic.hq.notifications.model;

import org.hyperic.hq.product.MetricValue;

public class MetricNotification implements INotification {
    public MetricNotification(Integer resourceId, Integer measurementId, MetricValue metricVal) {
        super();
        this.resourceId = resourceId;
        this.measurementId = measurementId;
        this.metricVal = metricVal;
    }
    public Integer getResourceId() {
        return resourceId;
    }
    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }
    public Integer getMeasurementId() {
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
    protected Integer resourceId;
    protected Integer measurementId;
    protected MetricValue metricVal;
}
