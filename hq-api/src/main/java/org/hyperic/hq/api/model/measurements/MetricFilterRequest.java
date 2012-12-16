package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.resources.ResourceFilterDefinitioin;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="filterRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="FilterRequestType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class MetricFilterRequest {
    @XmlAttribute
    private ResourceFilterDefinitioin resourceFilter;
}
