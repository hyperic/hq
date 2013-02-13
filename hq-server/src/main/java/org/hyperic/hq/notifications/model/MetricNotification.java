package org.hyperic.hq.notifications.model;

import org.hyperic.hq.product.MetricValue;

public class MetricNotification extends BaseNotification {
    protected Integer resourceID;
    protected Integer measurementID;
    protected MetricValue metricVal;
    protected String measurementName;
    
    public MetricNotification(Integer resourceID,Integer measurementID, String measurementName, MetricValue metricVal) {
        super();
        this.resourceID = resourceID;
        this.measurementID=measurementID;
        this.metricVal = metricVal;
        this.measurementName=measurementName;
    }
    public MetricValue getMetricVal() {
        return metricVal;
    }
    public void setMetricVal(MetricValue metricVal) {
        this.metricVal = metricVal;
    }
    public String getMeasurementName() {
        return this.measurementName;
    }
    public Integer getResourceID() {
        return this.resourceID;
    }
    public Integer getMeasurementId() {
        return this.measurementID;
    }
}
