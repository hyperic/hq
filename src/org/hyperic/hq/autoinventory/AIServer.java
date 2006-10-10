package org.hyperic.hq.autoinventory;

import org.hyperic.hq.appdef.AppdefBean;

/**
 *
 */
public class AIServer extends AppdefBean
{
    private AIPlatform aiqPlatformId;
    private String autoInventoryIdentifier;
    private String name;
    private String description;
    private Character active;
    private String serverTypeName;
    private String installPath;
    private boolean servicesAutoManaged;
    private byte[] customProperties;
    private byte[] productConfig;
    private byte[] controlConfig;
    private byte[] responseTime_Config;
    private byte[] measurementConfig;
    private Integer queueStatus;
    private long diff;
    private boolean ignored;

    /**
     * default constructor
     */
    public AIServer()
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

    public String getAutoInventoryIdentifier()
    {
        return this.autoInventoryIdentifier;
    }

    public void setAutoInventoryIdentifier(String autoInventoryIdentifier)
    {
        this.autoInventoryIdentifier = autoInventoryIdentifier;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Character getActive()
    {
        return this.active;
    }

    public void setActive(Character active)
    {
        this.active = active;
    }

    public String getServerTypeName()
    {
        return this.serverTypeName;
    }

    public void setServerTypeName(String serverTypeName)
    {
        this.serverTypeName = serverTypeName;
    }

    public String getInstallPath()
    {
        return this.installPath;
    }

    public void setInstallPath(String installPath)
    {
        this.installPath = installPath;
    }

    public boolean isServicesAutoManaged()
    {
        return this.servicesAutoManaged;
    }

    public void setServicesAutoManaged(boolean servicesAutoManaged)
    {
        this.servicesAutoManaged = servicesAutoManaged;
    }

    public byte[] getCustomProperties()
    {
        return this.customProperties;
    }

    public void setCustomProperties(byte[] customProperties)
    {
        this.customProperties = customProperties;
    }

    public byte[] getProductConfig()
    {
        return this.productConfig;
    }

    public void setProductConfig(byte[] productConfig)
    {
        this.productConfig = productConfig;
    }

    public byte[] getControlConfig()
    {
        return this.controlConfig;
    }

    public void setControlConfig(byte[] controlConfig)
    {
        this.controlConfig = controlConfig;
    }

    public byte[] getResponseTime_Config()
    {
        return this.responseTime_Config;
    }

    public void setResponseTime_Config(byte[] responseTime_Config)
    {
        this.responseTime_Config = responseTime_Config;
    }

    public byte[] getMeasurementConfig()
    {
        return this.measurementConfig;
    }

    public void setMeasurementConfig(byte[] measurementConfig)
    {
        this.measurementConfig = measurementConfig;
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

    // TODO: equals and hashCode
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof AIServer)) return false;
        AIServer castOther = (AIServer) other;

        return ((this.getAiqPlatformId() == castOther.getAiqPlatformId()) || (this.getAiqPlatformId() != null && castOther.getAiqPlatformId() != null && this.getAiqPlatformId().equals(castOther.getAiqPlatformId())))
               && ((this.getAutoInventoryIdentifier() == castOther.getAutoInventoryIdentifier()) || (this.getAutoInventoryIdentifier() != null && castOther.getAutoInventoryIdentifier() != null && this.getAutoInventoryIdentifier().equals(castOther.getAutoInventoryIdentifier())));
    }

    public int hashCode()
    {
        int result = 17;


        return result;
    }
}
