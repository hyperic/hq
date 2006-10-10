package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class ServiceType extends AppdefResourceType
{
    private String plugin;
    private boolean finternal;
    private ServerType serverType;
    private TierType tierType;
    private Collection applications;
    private Collection appServices;
    private Collection services;
    private Collection svcClusters;

    /**
     * default constructor
     */
    public ServiceType()
    {
        super();
    }

    // Property accessors
    public String getPlugin()
    {
        return this.plugin;
    }

    public void setPlugin(String plugin)
    {
        this.plugin = plugin;
    }

    public boolean isFinternal()
    {
        return this.finternal;
    }

    public void setFinternal(boolean finternal)
    {
        this.finternal = finternal;
    }

    public ServerType getServerType()
    {
        return this.serverType;
    }

    public void setServerType(ServerType serverType)
    {
        this.serverType = serverType;
    }

    public TierType getTierType()
    {
        return this.tierType;
    }

    public void setTierType(TierType tierType)
    {
        this.tierType = tierType;
    }

    public Collection getApplications()
    {
        return this.applications;
    }

    public void setApplications(Collection applications)
    {
        this.applications = applications;
    }

    public Collection getAppServices()
    {
        return this.appServices;
    }

    public void setAppServices(Collection appServices)
    {
        this.appServices = appServices;
    }

    public Collection getServices()
    {
        return this.services;
    }

    public void setServices(Collection services)
    {
        this.services = services;
    }

    public Collection getSvcClusters()
    {
        return this.svcClusters;
    }

    public void setSvcClusters(Collection svcClusters)
    {
        this.svcClusters = svcClusters;
    }
}
