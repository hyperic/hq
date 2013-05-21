package org.hyperic.hq.api.model.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="EndpointStatus", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ExternalEndpointStatus", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ExternalEndpointStatus {
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String INVALID = "INVALID";
    
    @XmlAttribute(name="creationTime")
    protected Long creationTime;
    @XmlAttribute(name="status")
    protected String status;
    @XmlAttribute(name="last-successful")
    protected Long successfulTime;
    @XmlAttribute(name="last-failure")
    protected Long failureTime;
    @XmlAttribute (name="message")
    protected String message;
    
    public void setLastSuccessful(long time) {
        this.successfulTime=time;
    }
    public void setLastFailure(long time) {
        this.failureTime=time;
    }
    public void setStatus(String status) {
        this.status=status;
    }
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
