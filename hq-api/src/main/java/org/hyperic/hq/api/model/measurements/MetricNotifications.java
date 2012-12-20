package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "metricNotifications", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MetricsNotifications Type", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class MetricNotifications extends MetricGroupBase {

    public MetricNotifications() {}
}
