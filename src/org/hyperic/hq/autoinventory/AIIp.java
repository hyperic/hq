package org.hyperic.hq.autoinventory;

import org.hyperic.hq.appdef.AppdefBean;

/**
 *
 */
public class AIIp extends AppdefBean
{
    private AIPlatform aiqPlatformId;
    private String address;
    private String netmask;
    private String macAddress;
    private Integer queueStatus;
    private long diff;
    private boolean ignored;

    /**
     * default constructor
     */
    public AIIp()
    {
        super();
    }

    public AIPlatform getAiqPlatformId()
    {
        return this.aiqPlatformId;
    }

    public void setAiqPlatformId(AIPlatform aiqPlatformId)
    {
        this.aiqPlatformId = aiqPlatformId;
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

    public String getMacAddress()
    {
        return this.macAddress;
    }

    public void setMacAddress(String macAddress)
    {
        this.macAddress = macAddress;
    }

    public Integer getQueueStatus()
    {
        return this.queueStatus;
    }

    public void setQueueStatus(Integer queueStatus)
    {
        this.queueStatus = queueStatus;
    }

    public long getDiff()
    {
        return this.diff;
    }

    public void setDiff(long diff)
    {
        this.diff = diff;
    }

    public boolean isIgnored()
    {
        return this.ignored;
    }

    public void setIgnored(boolean ignored)
    {
        this.ignored = ignored;
    }

    // TODO: fix equals and hashCode
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof AIIp)) return false;
        AIIp castOther = (AIIp) other;

        return ((this.getAiqPlatformId() == castOther.getAiqPlatformId()) || (this.getAiqPlatformId() != null && castOther.getAiqPlatformId() != null && this.getAiqPlatformId().equals(castOther.getAiqPlatformId())))
               && ((this.getAddress() == castOther.getAddress()) || (this.getAddress() != null && castOther.getAddress() != null && this.getAddress().equals(castOther.getAddress())));
    }

    public int hashCode()
    {
        int result = 17;


        return result;
    }
}
