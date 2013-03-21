package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="metricFilterDefinition", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MetricFilterDefinition", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class MetricFilterDefinition {
    @XmlAttribute
    protected Boolean isIndicator = null;

    public Boolean getIsIndicator() {
        return isIndicator;
    }
    public void setIsIndicator(Boolean isIndicator) {
        this.isIndicator = isIndicator;
    }
}
