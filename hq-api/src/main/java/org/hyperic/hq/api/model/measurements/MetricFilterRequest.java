package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.resources.ResourceFilterDefinition;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="registration", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MetricFilterRequestType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class MetricFilterRequest {
    @XmlElement(name="resourceFilter", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private ResourceFilterDefinition resourceFilterDef;
    @XmlElement(name="metricFilter", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private MetricFilterDefinition metricFilterDef;
    @XmlElement(name="httpEndpoint", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private HttpEndpointDefinition httpEndpointDef;

    public ResourceFilterDefinition getResourceFilterDefinition() {
        return resourceFilterDef;
    }
    public MetricFilterDefinition getMetricFilterDefinition() {
        return metricFilterDef;
    }
    public HttpEndpointDefinition getHttpEndpointDef() {
        return httpEndpointDef;
    }
}