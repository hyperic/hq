package org.hyperic.hq.api.model.measurements;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.resources.BatchResponseBase;
import org.hyperic.hq.api.model.resources.FailedResource;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="resourceMeasurementResponses", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceMeasurementResponsesType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceMeasurementBatchResponse extends BatchResponseBase {
    @XmlElementWrapper(name="resources",namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name="resource",namespace=RestApiConstants.SCHEMA_NAMESPACE)
    protected List<ResourceMeasurementResponse> responses = new ArrayList<ResourceMeasurementResponse>();
    
    public ResourceMeasurementBatchResponse() {
        super();
    }
    public ResourceMeasurementBatchResponse(ExceptionToErrorCodeMapper exceptionToErrorCodeMapper) {
        super(exceptionToErrorCodeMapper);
    }
    public ResourceMeasurementBatchResponse(List<FailedResource> failedResources) {
        super(failedResources);
    }

    public List<ResourceMeasurementResponse> getResponses() {
        return responses;
    }
    public void setResponses(List<ResourceMeasurementResponse> responses) {
        this.responses = responses;
    }
    public void addResponse(ResourceMeasurementResponse response) {
        if (this.responses==null) {
            this.responses = new ArrayList<ResourceMeasurementResponse>();
        }
        this.responses.add(response);
    }
}
