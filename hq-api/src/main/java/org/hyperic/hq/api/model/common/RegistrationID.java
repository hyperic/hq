package org.hyperic.hq.api.model.common;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="registrationID", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="RegistrationID", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class RegistrationID implements Serializable {
    private static final long serialVersionUID = 56150331462303510L;
    private static final AtomicLong idGenerator = new AtomicLong();
    @XmlElement(namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private long id;

    public RegistrationID() {
        this.id = idGenerator.getAndIncrement();
    } 

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
