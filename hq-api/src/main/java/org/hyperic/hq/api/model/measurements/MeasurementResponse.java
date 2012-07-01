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
@XmlRootElement(name="measurementResponse", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MeasurementResponseType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class MeasurementResponse extends BatchResponseBase {
    @XmlElementWrapper(name="measurements",namespace=RestApiConstants.SCHEMA_NAMESPACE)
	@XmlElement(name="measurement",namespace=RestApiConstants.SCHEMA_NAMESPACE)
	private List<Measurement> measurements = new ArrayList<Measurement>();
	
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj==null || !(obj instanceof MeasurementResponse)) {return false;}
		MeasurementResponse other = (MeasurementResponse) obj;
	    return this.measurements==null?other.measurements==null:(other.measurements!=null && this.measurements.equals(other.measurements));
	}
}
