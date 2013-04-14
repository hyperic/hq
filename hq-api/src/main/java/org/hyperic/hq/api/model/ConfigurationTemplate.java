package org.hyperic.hq.api.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="ConfigurationTemplate", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ConfigurationTemplateType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ConfigurationTemplate implements Serializable {
       
    /**
     * 
     */
    private static final long serialVersionUID = 2573311944909039187L;
    
    @XmlElementWrapper(name="optionsList", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name = "configOption", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private List<ConfigurationOption> configurationOptions ;

    public List<ConfigurationOption> getConfigurationOptions() {
        return configurationOptions;
    }

    public void setConfigurationOptions(List<ConfigurationOption> configurationOptions) {
        this.configurationOptions = configurationOptions;
    }     


}
