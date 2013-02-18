package org.hyperic.hq.api.model.measurements;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "resourceMeasurementRequest", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceMeasurementRequestType", namespace=RestApiConstants.SCHEMA_NAMESPACE)  
public class ResourceMeasurementRequest extends MeasurementRequest {
    private static final long serialVersionUID = 3235352424289979540L;

    @XmlAttribute
    protected String resourceID;
    
    public ResourceMeasurementRequest() {
        super();
    }
    public ResourceMeasurementRequest(String resourceID, List<String> measurementTemplateNames) {
        super(measurementTemplateNames);
        this.resourceID = resourceID;
    }
    public String getRscId() {
        return resourceID;
    }
    public void setRscId(String resourceID) {
        this.resourceID = resourceID;
    }
}
