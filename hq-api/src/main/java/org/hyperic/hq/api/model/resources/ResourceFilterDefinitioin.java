package org.hyperic.hq.api.model.resources;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlRootElement(name="resourceFilterDefinitioin", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceFilterDefinitioin", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceFilterDefinitioin {
    @XmlAttribute
    String name;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
