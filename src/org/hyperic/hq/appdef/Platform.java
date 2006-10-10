package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class Platform extends AppdefResource
{
    private String fqDN;
    private String certDN;
    private String comment;
    private Integer cpuCount;
    private PlatformType platformType;
    private ConfigResponse configResponse;
    private Agent agent;
    private Collection ips;
    private Collection servers;

    /**
     * default constructor
     */
    public Platform()
    {
        super();
    }

    public String getFqDN()
    {
        return this.fqDN;
    }

    public void setFqDN(String fqDN)
    {
        this.fqDN = fqDN;
    }

    public String getCertDN()
    {
        return this.certDN;
    }

    public void setCertDN(String certDN)
    {
        this.certDN = certDN;
    }

    public String getComment()
    {
        return this.comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public Integer getCpuCount()
    {
        return this.cpuCount;
    }

    public void setCpuCount(Integer cpuCount)
    {
        this.cpuCount = cpuCount;
    }

    public PlatformType getPlatformType()
    {
        return this.platformType;
    }

    public void setPlatformType(PlatformType platformType)
    {
        this.platformType = platformType;
    }

    public ConfigResponse getConfigResponse()
    {
        return this.configResponse;
    }

    public void setConfigResponse(ConfigResponse configResponse)
    {
        this.configResponse = configResponse;
    }

    public Agent getAgent()
    {
        return this.agent;
    }

    public void setAgent(Agent agent)
    {
        this.agent = agent;
    }

    public Collection getIps()
    {
        return this.ips;
    }

    public void setIps(Collection ips)
    {
        this.ips = ips;
    }

    public Collection getServers()
    {
        return this.servers;
    }

    public void setServers(Collection servers)
    {
        this.servers = servers;
    }
}
