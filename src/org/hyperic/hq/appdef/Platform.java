package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.PlatformPK;

import java.util.Collection;

/**
 *
 */
public class Platform extends AppdefResource
{
    private String fqdn;
    private String certdn;
    private String commentText;
    private Integer cpuCount;
    private PlatformType platformType;
    private ConfigResponseDB configResponse;
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

    public String getFqdn()
    {
        return this.fqdn;
    }

    public void setFqdn(String fqDN)
    {
        this.fqdn = fqDN;
    }

    public String getCertdn()
    {
        return this.certdn;
    }

    public void setCertdn(String certDN)
    {
        this.certdn = certDN;
    }

    public String getCommentText()
    {
        return this.commentText;
    }

    public void setCommentText(String comment)
    {
        this.commentText = comment;
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

    public ConfigResponseDB getConfigResponse()
    {
        return this.configResponse;
    }

    public void setConfigResponse(ConfigResponseDB configResponse)
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

    private PlatformPK pkey = new PlatformPK();
    /**
     * legacy EJB primary key getter
     * @deprecated use getId() instead
     * @return
     */
    public PlatformPK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }
}
