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
	private double  high;
	@XmlAttribute
    private double  low;

    public Metric() {}
    
    public double getHighValue() {
		return high;
	}
	public void setHighValue(double highValue) {
		this.high = highValue;
	}
	public double getLowValue() {
		return low;
	}
	public void setLowValue(double lowValue) {
		this.low = lowValue;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj==null || !(obj instanceof Metric)) { return false;}
	    Metric other = (Metric) obj;
	    return this.high==other.high
	            && this.low==other.low;
	}
}
