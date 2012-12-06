package org.hyperic.hq.api.model.cloud;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "cloudConfiguration", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class CloudConfiguration implements Serializable{
   
    private static final long serialVersionUID = 1L;

    @XmlElement(name="username", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private String username;
    
    @XmlElement(name="password", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private String password;
    
    @XmlElement(name="url", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    private String url;

    public CloudConfiguration() {
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
}
