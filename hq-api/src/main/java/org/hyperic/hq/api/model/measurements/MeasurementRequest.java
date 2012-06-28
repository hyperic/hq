package org.hyperic.hq.api.model.measurements;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "measurementRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MeasurementRequestType", namespace=RestApiConstants.SCHEMA_NAMESPACE)  
public class MeasurementRequest {
	@XmlElement
    private String resourceId;
	@XmlElementWrapper(name="templateNames", namespace=RestApiConstants.SCHEMA_NAMESPACE)
	@XmlElement(name="templateName", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private List<String> measurementTemplateNames;
	
	public List<String> getMeasurementTemplateNames() {
		return measurementTemplateNames;
	}
	public void setMeasurementTemplateNames(List<String> measurementTemplateNames) {
		this.measurementTemplateNames = measurementTemplateNames;
	}
	public MeasurementRequest(){} 
	
	public MeasurementRequest(final String resourceId, final List<String> measurementTemplateNames) { 
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
