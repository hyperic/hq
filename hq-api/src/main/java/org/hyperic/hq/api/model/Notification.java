package org.hyperic.hq.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.measurements.RawMetric;


@XmlSeeAlso({ID.class,ResourceModel.class,RawMetric.class})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="NotificationType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class Notification {

}
