package org.hyperic.hq.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "id", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="IDType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ID extends Notification {
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @XmlAttribute
    protected Integer id;
}
