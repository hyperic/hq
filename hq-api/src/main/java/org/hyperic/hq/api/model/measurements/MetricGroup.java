package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "metricGroup", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MetricGroupType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class MetricGroup extends MetricGroupBase {

    public MetricGroup() {}
}
