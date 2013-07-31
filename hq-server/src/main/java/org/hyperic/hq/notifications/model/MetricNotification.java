package org.hyperic.hq.notifications.model;

import org.hyperic.hq.product.MetricValue;

public class MetricNotification extends BaseNotification {
    protected Integer resourceID;
    protected Integer measurementID;
    protected MetricValue metricVal;
    protected String measurementName;
    protected String msmtType;
    protected String catName;
    protected String units;
    
    public MetricNotification(Integer resourceID,Integer measurementID, String measurementName, String msmtType, String catName, String units, MetricValue metricVal) {
        super();
        this.resourceID = resourceID;
        this.measurementID=measurementID;
        this.metricVal = metricVal;
        this.measurementName=measurementName;
        this.msmtType = msmtType;
        this.catName = catName;
        this.units = units;
    }
    public String getUnits() {
        return units;
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
    public String getMeasurementType() {
        return msmtType;
    }    
    public String getCategory() {
        return this.catName;
    }
    public String toString() {
        return new StringBuilder()
            .append("measurementId=").append(measurementID)
            .append(",resourceId=").append(resourceID)
            .append(",timestamp=").append(metricVal.getTimestamp())
            .append(",value=").append(metricVal.getValue())
            .toString();
    }
    
    public int hashCode() {
        Long timestamp = metricVal.getTimestamp();
        return 7 + (7*measurementID.hashCode()) + (7*timestamp.hashCode());
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MetricNotification) {
            MetricNotification m = (MetricNotification) o;
            return m.measurementID.equals(measurementID) && m.metricVal.getTimestamp() == metricVal.getTimestamp();
        }
        return false;
    }
}
