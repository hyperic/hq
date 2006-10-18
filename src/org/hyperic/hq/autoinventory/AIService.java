package org.hyperic.hq.autoinventory;

import org.hyperic.hq.appdef.AppdefBean;
import org.hyperic.hq.appdef.Server;

/**
 *
 */
public class AIService extends AppdefBean
{
    private String name;
    private String description;
    private String serviceTypeName;
    private Integer queueStatus;
    private long diff;
    private boolean ignored;
    private byte[] customProperties;
    private byte[] productConfig;
    private byte[] controlConfig;
    private byte[] measurementConfig;
    private byte[] responseTimeConfig;
    private Server server;

    /**
     * default constructor
     */
    public AIService()
    {
        super();
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

    public String getServiceTypeName()
    {
        return this.serviceTypeName;
    }

    public void setServiceTypeName(String serviceTypeName)
    {
        this.serviceTypeName = serviceTypeName;
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

    public byte[] getMeasurementConfig()
    {
        return this.measurementConfig;
    }

    public void setMeasurementConfig(byte[] measurementConfig)
    {
        this.measurementConfig = measurementConfig;
    }

    public byte[] getResponseTimeConfig()
    {
        return this.responseTimeConfig;
    }

    public void setResponseTimeConfig(byte[] responseTimeConfig)
    {
        this.responseTimeConfig = responseTimeConfig;
    }

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }
}
