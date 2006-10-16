package org.hyperic.hq.appdef;

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
