package org.hyperic.hq.api.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.resources.ComplexIp;

import edu.emory.mathcs.backport.java.util.Arrays;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="PropertyList", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="PropertyListType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class PropertyList implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3950475755942564324L;
    
    @XmlElementWrapper(name="propertiesList", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name = "aProperty", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private List<ConfigurationValue> properties ;

    public PropertyList() {    }
        
    public PropertyList(Collection<ConfigurationValue> properties) {
        init();
        this.properties.addAll(properties);
    }
    
    public PropertyList(Object[] properties) {
        if (null != properties) {            
            this.properties = Arrays.asList(properties);
        }
    }    

    public List<ConfigurationValue> getProperties() {
        return properties;
    }

    public void setProperties(List<ConfigurationValue> properties) {
        this.properties = properties;
    } 
    
    public void addProperty(ConfigurationValue property) {
        init();
        this.properties.add(property);
    }

    public void addAll(Collection<ConfigurationValue> properties) {
        init();
        this.properties.addAll(properties);
        
    }    
    
    private void init() {
        if (null == this.properties) {
            this.properties = new ArrayList<ConfigurationValue>();
        }
    }

    
    @Override
    public String toString() {
        return this.properties.toString(); 
    }//EOM 
    
    
}
