package org.hyperic.hq.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.measurements.RawMetric;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "notificationsReport", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="NotificationsReport Type", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class NotificationsReport {
    @XmlElementWrapper(name="inventory", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name="resource", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    protected List<Resource> inventory = new ArrayList<Resource>();
    @XmlElementWrapper(name="metrics", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name="metric", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    protected List<RawMetric> metrics = new ArrayList<RawMetric>();

    public NotificationsReport() {}

    public void add(Resource r) {
        this.inventory.add(r);
    }

    public void add(RawMetric metricWithId) {
        this.metrics.add(metricWithId);
    }
}
