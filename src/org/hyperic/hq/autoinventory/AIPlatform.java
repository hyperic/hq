package org.hyperic.hq.autoinventory;

import org.hyperic.hq.appdef.AppdefBean;

import java.util.Collection;

/**
 *
 */
public class AIPlatform extends AppdefBean
{
    private String name;
    private String description;
    private String os;
    private String osversion;
    private String arch;
    private String fqDN;
    private String agentToken;
    private String certDN;
    private Integer queueStatus;
    private long diff;
    private boolean ignored;
    private long lastApproved;
    private String location;
    private Integer cpuSpeed;
    private Integer cpuCount;
    private Integer ram;
    private String gateway;
    private String dhcpServer;
    private String dnsServer;
    private byte[] customProperties;
    private byte[] productConfig;
    private byte[] controlConfig;
    private byte[] measurementConfig;
    private Collection aiips;
    private Collection aiservers;

    /**
     * default constructor
     */
    public AIPlatform()
    {
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

    public String getOs()
    {
        return this.os;
    }

    public void setOs(String os)
    {
        this.os = os;
    }

    public String getOsversion()
    {
        return this.osversion;
    }

    public void setOsversion(String osversion)
    {
        this.osversion = osversion;
    }

    public String getArch()
    {
        return this.arch;
    }

    public void setArch(String arch)
    {
        this.arch = arch;
    }

    public String getFqDN()
    {
        return this.fqDN;
    }

    public void setFqDN(String fqDN)
    {
        this.fqDN = fqDN;
    }

    public String getAgentToken()
    {
        return this.agentToken;
    }

    public void setAgentToken(String agentToken)
    {
        this.agentToken = agentToken;
    }

    public String getCertDN()
    {
        return this.certDN;
    }

    public void setCertDN(String certDN)
    {
        this.certDN = certDN;
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

    public long getLastApproved()
    {
        return this.lastApproved;
    }

    public void setLastApproved(long lastApproved)
    {
        this.lastApproved = lastApproved;
    }

    public String getLocation()
    {
        return this.location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public Integer getCpuSpeed()
    {
        return this.cpuSpeed;
    }

    public void setCpuSpeed(Integer cpuSpeed)
    {
        this.cpuSpeed = cpuSpeed;
    }

    public Integer getCpuCount()
    {
        return this.cpuCount;
    }

    public void setCpuCount(Integer cpuCount)
    {
        this.cpuCount = cpuCount;
    }

    public Integer getRam()
    {
        return this.ram;
    }

    public void setRam(Integer ram)
    {
        this.ram = ram;
    }

    public String getGateway()
    {
        return this.gateway;
    }

    public void setGateway(String gateway)
    {
        this.gateway = gateway;
    }

    public String getDhcpServer()
    {
        return this.dhcpServer;
    }

    public void setDhcpServer(String dhcpServer)
    {
        this.dhcpServer = dhcpServer;
    }

    public String getDnsServer()
    {
        return this.dnsServer;
    }

    public void setDnsServer(String dnsServer)
    {
        this.dnsServer = dnsServer;
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

    public Collection getAiips()
    {
        return this.aiips;
    }

    public void setAiips(Collection aiips)
    {
        this.aiips = aiips;
    }

    public Collection getAiservers()
    {
        return this.aiservers;
    }

    public void setAiservers(Collection aiservers)
    {
        this.aiservers = aiservers;
    }
}