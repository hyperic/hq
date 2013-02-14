package org.hyperic.hq.api.model.resources;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.resources.ResourceFilterDefinitioin;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="resourceFilterRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceFilterRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceFilterRequest {
    @XmlElement(name="resourceFilter", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private ResourceFilterDefinitioin resourceFilterDef;

    public ResourceFilterDefinitioin getResourceFilterDefinition() {
        return resourceFilterDef;
    }
}