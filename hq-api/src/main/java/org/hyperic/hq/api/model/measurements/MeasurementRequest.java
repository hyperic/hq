package org.hyperic.hq.api.model.measurements;
 
import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "measurementRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="MeasurementRequestType", namespace=RestApiConstants.SCHEMA_NAMESPACE)  
public class MeasurementRequest implements Serializable {
    private static final long serialVersionUID = 2232715262706967461L;
    
    @XmlElementWrapper(name="measurementNames", namespace=RestApiConstants.SCHEMA_NAMESPACE)
	@XmlElement(name="measurement", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private List<String> measurementTemplateNames;
	
	public List<String> getMeasurementTemplateNames() {
		return measurementTemplateNames;
	}
	public void setMeasurementTemplateNames(List<String> measurementTemplateNames) {
		this.measurementTemplateNames = measurementTemplateNames;
	}
	public MeasurementRequest(){} 
	
	public MeasurementRequest(List<String> measurementTemplateNames) { 
		this.measurementTemplateNames = measurementTemplateNames;
	}
}
