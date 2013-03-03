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
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MetricNotification)) {
            return false;
        }
        MetricNotification mo = (MetricNotification) obj;
        return (this.measurementID==null)!=(mo.measurementID==null)?false:((this.measurementID==null&&mo.measurementID==null)?true:this.measurementID.equals(mo.measurementID))
                && (this.measurementName==null)!=(mo.measurementName==null)?false:((this.measurementName==null&&mo.measurementName==null)?true:this.measurementName.equals(mo.measurementName))
                        && (this.resourceID==null)!=(mo.resourceID==null)?false:((this.resourceID==null&&mo.resourceID==null)?true:this.resourceID.equals(mo.resourceID))
                                && (this.metricVal==null)!=(mo.metricVal==null)?false:((this.metricVal==null&&mo.metricVal==null)?true:this.metricVal.equals(mo.metricVal));
    }
}
