package org.hyperic.hq.api.model.measurements;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "measurement", namespace=RestApiConstants.SCHEMA_NAMESPACE)
//@XmlType(name=???, namespace=RestApiConstants.SCHEMA_NAMESPACE)?????
public class Measurement {
	@XmlAttribute
	private long interval;
	@XmlAttribute
	private String name;
	@XmlElement
	private List<Metric> metrics;
	
	public Measurement() {}

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
}
