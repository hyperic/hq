package org.hyperic.hq.api.model.resources;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.common.RegistrationID;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.appdef.shared.BatchResponse;

@XmlRootElement(name="registeredResourceBatchResponse", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="RegisteredResourceBatchResponse", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class RegisteredResourceBatchResponse extends ResourceBatchResponse {
    @XmlElement
    protected RegistrationID registrationID;
    
    public RegisteredResourceBatchResponse() {
    }
    public RegisteredResourceBatchResponse(List<ResourceModel> resourcesAddedToInventory,
            List<FailedResource> failedResources) {
        super(resourcesAddedToInventory, failedResources);
    }
    public RegisteredResourceBatchResponse(ExceptionToErrorCodeMapper exceptionToErrorCodeMapper) {
        super(exceptionToErrorCodeMapper);
    }
    public RegisteredResourceBatchResponse(BatchResponse<ResourceModel> batchResponse,
            ExceptionToErrorCodeMapper exceptionToErrorCodeMapper) {
        super(batchResponse, exceptionToErrorCodeMapper);
    }
    public RegistrationID getRegId() {
        return registrationID;
    }

    public void setRegId(RegistrationID registrationID) {
        this.registrationID = registrationID;
    }
}
