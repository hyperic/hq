package org.hyperic.hq.api.model.resources;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.ConfigurationValue;
import org.hyperic.hq.api.model.RestApiConstants;

@XmlRootElement(name="ComplexIp", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ComplexIpType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ComplexIp extends ConfigurationValue implements Serializable  {
    
    private static final long serialVersionUID = 8273033074458085922L;

    private String netmask;
    private String mac;
    private String address;
 
    public ComplexIp() {  }
    
    public ComplexIp(String netmask, String mac, String address) {
        this.netmask = netmask;
        this.mac = mac;
        this.address = address;
    }

    public String getNetmask() {
        return netmask;
    }
    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }
    public String getMac() {
        return mac;
    }
    public void setMac(String mac) {
        this.mac = mac;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Ip [address=" + address + ", netmask=" + netmask + ", mac=" + mac + "]";
    }

    @Override
    public int hashCode() {
        int result = 7;
        result = 31 * result + (null == this.address ? 1 : this.address.hashCode());
        result = 31 * result + (null == this.mac ? 1 : this.mac.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if ((null == obj) || !(obj instanceof ComplexIp)) return false;
        ComplexIp ci = (ComplexIp)obj;
        return  equalsNillable(this.mac, ci.mac)
                && equalsNillable(this.address, ci.address)
                && equalsNillable(this.netmask, ci.netmask);
    }
    
    private static boolean equalsNillable(Object o1, Object o2) {
        return ((o1 == o2) || (o1 != null && o1.equals(o2)));
    }

}

