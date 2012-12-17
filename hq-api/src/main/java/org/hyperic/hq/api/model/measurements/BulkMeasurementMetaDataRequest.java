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
@XmlRootElement(name="bulkMeasurementMetaDataRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="BulkMeasurementMetaDataRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class BulkMeasurementMetaDataRequest {
    @XmlElementWrapper(name="measurementIds", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name="measurement", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    protected List<ID> mids;

    public BulkMeasurementMetaDataRequest() {
        this.mids = new ArrayList<ID>();
    }
    public BulkMeasurementMetaDataRequest(List<ID> mids) {
        this.mids = mids;
    }
    public List<ID> getMids() {
        return mids;
    }
    public void setMids(List<ID> mids) {
        this.mids = mids;
    }
}
