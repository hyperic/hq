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
    private String id;

    public RegistrationID() {
        this.id = String.valueOf(System.currentTimeMillis()) + '_' + String.valueOf(idGenerator.getAndIncrement());
    } 

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
