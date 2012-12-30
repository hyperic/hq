package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="RawMetricType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class RawMetric {
    @XmlAttribute
    protected double  value;
    @XmlAttribute
    protected long timestamp; 
    @XmlAttribute
    protected Integer measurementId;
    
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
    public Integer getMeasurementId() {
        return measurementId;
    }
    public void setMeasurementId(Integer measurementId) {
        this.measurementId = measurementId;
    }
//    @Override
//    public boolean equals(Object obj) {
//        if (obj==null || !(obj instanceof RawMetric)) { return false;}
//        RawMetric other = (RawMetric) obj;
//        return this.value==other.value
//                && this.timestamp==other.timestamp;
//    }
}
