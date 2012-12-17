package org.hyperic.hq.api.model.measurements;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "measurement", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MeasurementType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class Measurement {
	@XmlAttribute
	private long interval;
    @XmlAttribute
    private String alias;
    @XmlAttribute
    private String name;
    @XmlAttribute
    private Double avg = null;	
	@XmlElementWrapper(name="metrics", namespace=RestApiConstants.SCHEMA_NAMESPACE)
	@XmlElement(name="metric", namespace=RestApiConstants.SCHEMA_NAMESPACE)
	private List<Metric> metrics;
	
	public Measurement() {}
    /**
     * order of metrics does matter
     */
    @Override
    public boolean equals(Object obj) {
        if (obj==null || !(obj instanceof Measurement)) {
            return false;
        }
        Measurement other = (Measurement) obj;
        return (this.alias==null?other.alias==null:(other.alias!=null && this.alias.equals(other.alias))
                && this.name==null?other.name==null:(other.name!=null && this.name.equals(other.name))
                && this.interval == other.interval
                && this.avg == other.avg
                && this.metrics==null?other.metrics==null:(other.metrics!=null && this.metrics.equals(other.metrics)));
    }
	public List<Metric> getMetrics() {
		return metrics;
	}
	public void setMetrics(List<Metric> metrics) {
		this.metrics = metrics;
	}
	public long getInterval() {
		return interval;
	}
	public void setInterval(long interval) {
		this.interval = interval;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
    public double getAvg() {
        return avg.doubleValue();
    }
    public void setAvg(double avg) {
        this.avg = new Double(avg);
    }
    public void setAvg(Double avg) {
        this.avg = avg;
    }
    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }
}
