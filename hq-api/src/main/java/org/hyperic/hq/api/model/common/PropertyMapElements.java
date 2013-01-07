package org.hyperic.hq.api.model.common;
 
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

class PropertyMapElements {
    @XmlAttribute
    public String key;
    @XmlValue
    public String value;
    
    private PropertyMapElements() {}
    
    public PropertyMapElements(String key, String value) {
        this.key   = key;
        this.value = value;
    }
}
