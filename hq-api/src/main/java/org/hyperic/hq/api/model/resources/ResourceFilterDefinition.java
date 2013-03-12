package org.hyperic.hq.api.model.resources;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlRootElement(name="resourceFilterDefinition", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceFilterDefinition", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceFilterDefinition {
    @XmlAttribute
    int[] resourceIds;
    
    public int[] getResourceIds() {
        return resourceIds;
    }
    public void setResourceIds(int[] resourceIds) {
        this.resourceIds = resourceIds;
    }
}
