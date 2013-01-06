package org.hyperic.hq.api.model.common;
 
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.hyperic.hq.api.model.PropertyList;

class PropertyListMapElements {
    @XmlAttribute
    public String key;
    @XmlElement
    public PropertyList value;
    
    private PropertyListMapElements() {}
    
    public PropertyListMapElements(String key, PropertyList value) {
        this.key   = key;
        this.value = value;
    }
    
}
