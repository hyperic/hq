package org.hyperic.hq.api.model.measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.RestApiConstants;
//import org.hyperic.hq.api.model.resources.BatchResponse;
import org.hyperic.hq.api.model.resources.BatchResponseBase;
import org.hyperic.hq.api.model.resources.FailedResource;
//import org.hyperic.hq.api.model.resources.NullArgumentException;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="resourcesMeasurementsBatchResponse", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="resourcesMeasurementsBatchResponse", namespace="http://vmware.com/hyperic/hq/5.0/api/rest/v1")
public class MeasurementResponse extends BatchResponseBase {
//	@XmlElement
//    private List<Resource> resources;
	private List<Measurement> measurements = new List<Measurement>;
	
	public MeasurementResponse(
			ExceptionToErrorCodeMapper exceptionToErrorCodeMapper) {
		super(exceptionToErrorCodeMapper);
	}

	public MeasurementResponse(List<FailedResource> failedResources) {
		super(failedResources);
	}

	public MeasurementResponse() {
		super();
	}

	public void add(Measurement msmt) {
		this.measurements.add(msmt);
	}
	
	public List<Measurement> getMeasurements() {
		return this.measurements;
	}
	
	public void putMetrics(Measurement msmt, List<Metric> metrics) {
		
	}
	
//    public ResourcesMeasurementsBatchResponse(BatchResponse<Resource> batchResponse, ExceptionToErrorCodeMapper exceptionToErrorCodeMapper) {
//        if (null == exceptionToErrorCodeMapper) {
//            throw new NullArgumentException("exceptionToErrorCodeMap");
//        }
//            if (null != batchResponse.getResponse()) {
//        if (null != batchResponse) {
//            	resources = batchResponse.getResponse();
//            }  
//            
//            Map<String,Exception> failedIds = batchResponse.getFailedIds(); 
//            if (null != failedIds) {
//                List<FailedResource> failedResources = new ArrayList<FailedResource>(failedIds.size());
//                for (Entry<String,Exception> failedIdException : failedIds.entrySet()) {
//                    Exception exception = failedIdException.getValue();
//                    String resourceId = failedIdException.getKey();
//                    failedResources.add(new FailedResource(resourceId, exceptionToErrorCodeMapper.getErrorCode(exception), exception.getMessage()));
//                }
//                super.setFailedResources(failedResources);
//            }                                
//        }
//    }

	@Override
	public boolean equals(Object obj) {
		return ;super.equals(obj);
	}
}
