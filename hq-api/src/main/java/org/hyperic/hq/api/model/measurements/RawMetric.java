package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.Notification;
import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rawMetric", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="RawMetricType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class RawMetric extends Notification {
    @XmlAttribute
    protected double  value;
    @XmlAttribute
    protected long timestamp; 
    @XmlAttribute
    protected String measurementName;
    @XmlAttribute
    protected Integer resourceID;
    @XmlAttribute
    protected Integer measurementID;
    @XmlAttribute
    protected String measurementType;
    @XmlAttribute
    protected String category;
    @XmlAttribute
    protected String units;
    
    public double getValue() {
        return value;
    }
    public void setValue(double value) {
        this.value = value;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public Integer getResourceID() {
        return resourceID;
    }
    public void setResourceID(Integer resourceID) {
        this.resourceID = resourceID;
    }
    public String getMeasurementName() {
        return measurementName;
    }
    public void setMeasurementName(String measurementName) {
        this.measurementName = measurementName;
    }
    public void setMeasurementID(Integer measurementId) {
        this.measurementID = measurementId;
    }
    public void setMeasurementType(String measurementType) {
        this.measurementType = measurementType;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public void setUnits(String units) {
        this.units = units;
    }
}
