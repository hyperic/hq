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
public class MeasurementsRequest {
	@XmlElement
    private String resourceId;
	@XmlElement
    private List<String> measurementTemplateNames;
	
	public List<String> getMeasurementTemplateNames() {
		return measurementTemplateNames;
	}
	public void setMeasurementTemplateNames(List<String> measurementTemplateNames) {
		this.measurementTemplateNames = measurementTemplateNames;
	}
	public MeasurementsRequest(){} 
	public MeasurementsRequest(final String resourceId, final List<String> measurementTemplateNames) { 
		this.resourceId = resourceId ;
		this.measurementTemplateNames = measurementTemplateNames;
	}
	public String getResourceId() {
		return this.resourceId;
	}
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
}
