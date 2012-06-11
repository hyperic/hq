package org.hyperic.hq.api.model.measurements;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.ResourceType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "resourceMeasurementsRequest", namespace="http://vmware.com/hyperic/hq/5.0/api/rest/v1")
@XmlType(name="resourceMeasurementsRequest", namespace="http://vmware.com/hyperic/hq/5.0/api/rest/v1")
public class ResourceMeasurementsRequest {
	@XmlAttribute 
    private ResourceType resourceType;
	@XmlElement
    private List<String> resourceIdsList;
	@XmlElement
    private List<String> measurementTemplateNames;
	
	public List<String> getMeasurementTemplateNames() {
		return measurementTemplateNames;
	}
	public void setMeasurementTemplateNames(List<String> measurementTemplateNames) {
		this.measurementTemplateNames = measurementTemplateNames;
	}
	public ResourceMeasurementsRequest(){} 
	public ResourceMeasurementsRequest(final ResourceType resourceType, final List<String> resourceIdsList, final List<String> measurementTemplateNames) { 
		this.resourceType = resourceType;
		this.resourceIdsList = resourceIdsList ;
		this.measurementTemplateNames = measurementTemplateNames;
	}
	public List<String> getResourceIdsList() {
		return resourceIdsList;
	}
	public void setResourceIdsList(List<String> resourceIdsList) {
		this.resourceIdsList = resourceIdsList;
	}
    public ResourceType getResourceType() {
		return resourceType;
	}
	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}
}
