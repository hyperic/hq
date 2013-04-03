package org.hyperic.hq.api.model.resources;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.measurements.HttpEndpointDefinition;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="registration", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceFilterRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceFilterRequest {

    @XmlElement(name="resourceFilter", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private ResourceFilterDefinition resourceFilterDef;

    @XmlElement(name="httpEndpoint", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private HttpEndpointDefinition httpEndpointDef;

    public ResourceFilterDefinition getResourceFilterDefinition() {
        return resourceFilterDef;
    }

    public HttpEndpointDefinition getHttpEndpointDef() {
        return httpEndpointDef;
    }
}