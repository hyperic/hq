package org.hyperic.hq.api.model.measurements;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.ID;
import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="bulkResourceMeasurementRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="BulkResourceMeasurementRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class BulkResourceMeasurementRequest {
    @XmlElementWrapper(name="resourceIds", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name="resource", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    protected List<ID> rids;

    public BulkResourceMeasurementRequest() {
        this.rids = new ArrayList<ID>();
    }
    public BulkResourceMeasurementRequest(List<ID> rids) {
        this.rids = rids;
    }
    public List<ID> getRids() {
        return rids;
    }
    public void setRids(List<ID> rids) {
        this.rids = rids;
    }
}
