package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.IpValue;

/**
 *
 */
public class Ip extends AppdefBean
{
    private Platform platform;
    private String address;
    private String netmask;
    private String MACAddress;

    /**
     * default constructor
     */
    public Ip()
    {
        super();
    }

    public Platform getPlatform()
    {
        return this.platform;
    }

    public void setPlatform(Platform platform)
    {
        this.platform = platform;
    }

    public String getAddress()
    {
        return this.address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getNetmask()
    {
        return this.netmask;
    }

    public void setNetmask(String netmask)
    {
        this.netmask = netmask;
    }

    public String getMACAddress()
    {
        return this.MACAddress;
    }

    public void setMACAddress(String MACAddress)
    {
        this.MACAddress = MACAddress;
    }

    /**
     * convenience method for copying simple values
     * from the legacy EJB Value Object
     *
     * @deprecated
     * @param valueHolder
     */
    public void setIpValue(IpValue valueHolder)
    {
        setAddress( valueHolder.getAddress() );
        setNetmask( valueHolder.getNetmask() );
        setMACAddress( valueHolder.getMACAddress() );
    }

    private IpValue ipValue = new IpValue();
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) Ip Object instead
     * @return
     */
    public IpValue getIpValue()
    {
        ipValue.setAddress(getAddress());
        ipValue.setNetmask(getNetmask());
        ipValue.setMACAddress(getMACAddress());
        ipValue.setId(getId());
        ipValue.setMTime(getMTime());
        ipValue.setCTime(getCTime());
        return ipValue;
    }

    // TODO: fix equals and hashCode
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof Ip)) return false;
        Ip castOther = (Ip) other;

        return ((this.getPlatform() == castOther.getPlatform()) || (this.getPlatform() != null && castOther.getPlatform() != null && this.getPlatform().equals(castOther.getPlatform())))
               && ((this.getAddress() == castOther.getAddress()) || (this.getAddress() != null && castOther.getAddress() != null && this.getAddress().equals(castOther.getAddress())));
    }

    public int hashCode()
    {
        int result = 17;
        return result;
    }
}
