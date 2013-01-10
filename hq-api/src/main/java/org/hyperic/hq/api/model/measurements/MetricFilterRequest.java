package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.resources.ResourceFilterDefinitioin;
import org.hyperic.hq.notifications.filtering.MetricFilterByResource;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="metricFilterRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MetricFilterRequestType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class MetricFilterRequest {
    @XmlElement(name="resourceFilter", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private ResourceFilterDefinitioin resourceFilterDef;
    @XmlElement(name="metricFilter", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private MetricFilterDefinition metricFilterDef;

    public static boolean validate(MetricFilterRequest metricFilterRequest) {
        return metricFilterRequest!=null && ResourceFilterDefinitioin.validate(metricFilterRequest.resourceFilterDef);
    }
    public ResourceFilterDefinitioin getResourceFilterDefinition() {
        return resourceFilterDef;
    }
    public MetricFilterDefinition getMetricFilterDefinition() {
        return metricFilterDef;
    }
}