package org.hyperic.hq.api.model.config;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlType(namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlEnum
public enum ServerConfigType {
    VCENTER("vCenter"),
    LDAP("LDAP"),
    SERVER_GUID("Server_GUID");  

    private String type;
    
    private ServerConfigType() {
    }
    
    ServerConfigType(String type) {
        this.setType(type);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
