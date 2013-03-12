package org.hyperic.hq.api.model.config;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hyperic.hq.api.model.RestApiConstants;
import org.hyperic.hq.api.model.common.MapPropertiesAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "serverConfig", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ServerConfig {
    
    @XmlAttribute
    private ServerConfigType type;
    
    @XmlAttribute
    private ServerConfigStatus status;
    
    @XmlJavaTypeAdapter(MapPropertiesAdapter.class)
    private HashMap<String,String> properties = new HashMap<String, String>(); 
    
    public ServerConfig(){
    }
    
    public ServerConfig(ServerConfigType type) {
        this.type = type;
    }
    
    public void addProperty(String key, String value) {
        this.getProperties().put(key, value);
    }
    
    public String getProperty(String key) {
        return this.getProperties().get(key);
    }
   
    public ServerConfigType getType() {
        return type;
    }
    
    public void setType(ServerConfigType type) {
        this.type = type;
    }

    public ServerConfigStatus getStatus() {
        return status;
    }

    public void setStatus(ServerConfigStatus status) {
        this.status = status;
    }

    public HashMap<String,String> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String,String> properties) {
        this.properties = properties;
    }
    
    @Override
    public String toString() {
        return "ServerConfig [type=" + type + ", status=" + status + ", properties=" + properties + "]";
    }

    
}
