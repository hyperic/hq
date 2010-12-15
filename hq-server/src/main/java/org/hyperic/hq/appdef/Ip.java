package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.IpValue;

public class Ip extends AppdefBean {

    private String address;

    private String macAddress;

    private String netmask;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }
    
    /**
     * legacy DTO pattern
     * @deprecated use (this) Ip Object instead
     */
    public IpValue getIpValue()
    {   IpValue ipValue = new IpValue();
        ipValue.setAddress(getAddress());
        ipValue.setNetmask(getNetmask());
        ipValue.setMACAddress(getMacAddress());
        ipValue.setId(getId());
        ipValue.setMTime(getMTime());
        ipValue.setCTime(getCTime());
        return ipValue;
    }


}
