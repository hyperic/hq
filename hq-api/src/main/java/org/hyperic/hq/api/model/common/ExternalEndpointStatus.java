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
    @XmlAttribute(name="creationTime")
    protected long creationTime;
    @XmlAttribute(name="status")
    protected String status;
    @XmlAttribute(name="last-successful")
    protected long successfulTime;
    @XmlAttribute(name="last-failure")
    protected long failureTime;
    
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
}
