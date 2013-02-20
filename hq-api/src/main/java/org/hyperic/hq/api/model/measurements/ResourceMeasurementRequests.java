package org.hyperic.hq.api.model.measurements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "resourceMeasurementRequests", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceMeasurementRequestsType", namespace=RestApiConstants.SCHEMA_NAMESPACE)  
public class ResourceMeasurementRequests implements Serializable {
    private static final long serialVersionUID = 4181731710495097644L;
    
    @XmlElementWrapper(name="resources", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name="resource", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    protected List<ResourceMeasurementRequest> resourceMeasurementRequests = new ArrayList<ResourceMeasurementRequest>();

    public ResourceMeasurementRequests() {}
    public ResourceMeasurementRequests(List<ResourceMeasurementRequest> resourceMeasurementRequests) {
        super();
        this.resourceMeasurementRequests = resourceMeasurementRequests;
    }
    public List<ResourceMeasurementRequest> getMeasurementRequests() {
        return resourceMeasurementRequests;
    }
    public void setMeasurementRequests(List<ResourceMeasurementRequest> resourceMeasurementRequests) {
        this.resourceMeasurementRequests = resourceMeasurementRequests;
    }
    public void addMeasurementRequests(ResourceMeasurementRequest resourceMeasurementRequest) {
        if (this.resourceMeasurementRequests==null) {
            this.resourceMeasurementRequests=new ArrayList<ResourceMeasurementRequest>();
        }
        this.resourceMeasurementRequests.add(resourceMeasurementRequest);
    }
}
