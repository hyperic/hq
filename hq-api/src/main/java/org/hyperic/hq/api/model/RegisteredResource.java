package org.hyperic.hq.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "resource", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceTypeType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class RegisteredResource extends Resource {
    @XmlAttribute
    protected Integer registrationID;
    public RegisteredResource(String id) {
        super(id);
    }
    public Integer getRegistrationID() {
        return registrationID;
    }
    public void setRegistrationID(Integer registrationID) {
        this.registrationID = registrationID;
    }
}
