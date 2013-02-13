package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="MetricType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class Metric extends RawMetric {
	@XmlAttribute
	private double  highValue;
	@XmlAttribute
    private double  lowValue;

    public Metric() {}
    
    public double getHighValue() {
		return highValue;
	}
	public void setHighValue(double highValue) {
		this.highValue = highValue;
	}
	public double getLowValue() {
		return lowValue;
	}
	public void setLowValue(double lowValue) {
		this.lowValue = lowValue;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj==null || !(obj instanceof Metric)) { return false;}
	    Metric other = (Metric) obj;
	    return this.highValue==other.highValue
	            && this.lowValue==other.lowValue;
	}
}
