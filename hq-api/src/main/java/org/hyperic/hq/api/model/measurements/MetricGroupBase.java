package org.hyperic.hq.api.model.measurements;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.resources.ComplexIp;

@XmlSeeAlso({Measurement.class,MetricGroup.class})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="BaseMetricGroupType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class MetricGroupBase {
    @XmlAttribute
    private String alias;
    @XmlAttribute
    private String name;
    @XmlElementWrapper(name="metrics", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name="metric", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private List<? extends RawMetric> metrics;
    
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
    public List<? extends RawMetric> getMetrics() {
        return metrics;
    }
    public void setMetrics(List<? extends RawMetric> metrics) {
        this.metrics = metrics;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj==null || !(obj instanceof Measurement)) {
            return false;
        }
        MetricGroupBase other = (MetricGroupBase) obj;
        return this.metrics==null?other.metrics==null:(other.metrics!=null && this.metrics.equals(other.metrics));
    }
}
