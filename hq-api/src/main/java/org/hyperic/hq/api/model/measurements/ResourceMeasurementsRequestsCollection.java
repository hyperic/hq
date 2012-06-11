package org.hyperic.hq.api.model.measurements;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="resourceMeasurementsRequestsCollection", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="resourceMeasurementsRequestsCollection", namespace="http://vmware.com/hyperic/hq/5.0/api/rest/v1")
public class ResourceMeasurementsRequestsCollection {   //change to measurementResources
	public ResourceMeasurementsRequestsCollection() {
		super();
	}

	public List<ResourceMeasurementsRequest> getResourceMeasurementsRequestList() {
		return resourceMeasurementsRequestList;
	}

	public void setResourceMeasurementsRequestList(
			List<ResourceMeasurementsRequest> resourceMeasurementsRequestList) {
		this.resourceMeasurementsRequestList = resourceMeasurementsRequestList;
	}

	public ResourceMeasurementsRequestsCollection(
			List<ResourceMeasurementsRequest> resourceMeasurementsRequestList) {
		super();
		this.resourceMeasurementsRequestList = resourceMeasurementsRequestList;
	}

	@XmlElement
	List<ResourceMeasurementsRequest> resourceMeasurementsRequestList;
}
