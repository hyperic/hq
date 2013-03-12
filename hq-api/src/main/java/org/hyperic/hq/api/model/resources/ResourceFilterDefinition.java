package org.hyperic.hq.api.model.resources;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

import java.util.Set;

@XmlRootElement(name="resourceFilterDefinition", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceFilterDefinition", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceFilterDefinition {

    @XmlAttribute
    private Set<Integer> resourceIds;
    
    public Set<Integer> getResourceIds() {
        return resourceIds;
    }

}
