package org.hyperic.hq.api.model.common;

import org.hyperic.hq.api.model.RestApiConstants;
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
public class RegistrationStatus implements Serializable {

    @XmlElement(name = "filter", namespace = RestApiConstants.SCHEMA_NAMESPACE)
    private String filter;

    @XmlElement(namespace = RestApiConstants.SCHEMA_NAMESPACE)
    private long id;

    public RegistrationStatus() {
    }

    public RegistrationStatus(FilterChain filterChain, int registrationID) {
        this.filter = filterChain.toString();
        this.id = registrationID;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
