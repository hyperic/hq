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
public class Measurement extends MetricGroupBase {
    @XmlAttribute
    protected Integer id;
    @XmlAttribute
    private String alias;
    @XmlAttribute
    private String name;
	@XmlAttribute
	private Long interval;
    @XmlAttribute
    private Double avg;	
	
	public Measurement() {}
//    /**
//     * order of metrics does matter
//     */
//    @Override
//    public boolean equals(Object obj) {
//        if (obj==null || !(obj instanceof Measurement)) {
//            return false;
//        }
//        Measurement other = (Measurement) obj;
//        return (super.equals(other)
//                && this.alias==null?other.alias==null:(other.alias!=null && this.alias.equals(other.alias))
//                && this.name==null?other.name==null:(other.name!=null && this.name.equals(other.name))
//                && this.interval == other.interval
//                && this.avg == other.avg);
//    }
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }
    public Long getInterval() {
		return interval;
	}
	public void setInterval(Long interval) {
		this.interval = interval;
	}
    public Double getAvg() {
        return avg;
    }
    public void setAvg(Double avg) {
        this.avg = avg;
    }
}
