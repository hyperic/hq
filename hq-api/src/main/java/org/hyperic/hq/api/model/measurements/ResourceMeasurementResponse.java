package org.hyperic.hq.api.model.measurements;
  
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="resourceMeasurementResponse", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ResourceMeasurementResponseType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ResourceMeasurementResponse extends MetricResponse {
    @XmlAttribute
    protected String resource;

    public ResourceMeasurementResponse() {}
    
    public ResourceMeasurementResponse(String rscId) {
        super();
        this.resource=rscId;
    }

    public String getResourceId() {
        return resource;
    }

    public void setResourceId(String rscId) {
        this.resource = rscId;
    }

}
