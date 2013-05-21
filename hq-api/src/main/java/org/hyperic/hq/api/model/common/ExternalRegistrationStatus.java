package org.hyperic.hq.api.model.common;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.measurements.HttpEndpointDefinition;
import org.hyperic.hq.notifications.EndpointStatus;
import org.hyperic.hq.notifications.filtering.FilterChain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "status", namespace = RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name = "RegistrationStatus", namespace = RestApiConstants.SCHEMA_NAMESPACE)
public class ExternalRegistrationStatus implements Serializable {

    @XmlElement(name = "filter", namespace = RestApiConstants.SCHEMA_NAMESPACE)
    private String filter;

    @XmlElement(namespace = RestApiConstants.SCHEMA_NAMESPACE)
    private String id;
    @XmlElement(name = "httpEndpoint")
    protected HttpEndpointDefinition endpoint;
    @XmlElement
    ExternalEndpointStatus endpointStatus;
    
    public ExternalRegistrationStatus() {
    }

    public ExternalRegistrationStatus(HttpEndpointDefinition endpoint, FilterChain filterChain, String registrationID, ExternalEndpointStatus endpointStatus) {
        this.endpoint=endpoint;
        if (filterChain!=null) {
            this.filter = filterChain.toString();
        } else {
            this.filter = null;
        }
        this.id = registrationID;
        this.endpointStatus=endpointStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public HttpEndpointDefinition getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(HttpEndpointDefinition endpoint) {
        this.endpoint = endpoint;
    }
}
