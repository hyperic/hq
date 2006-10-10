package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class Service extends AppdefResource
{
    private boolean autoDiscoveryZombie;
    private boolean serviceRt;
    private boolean endUserRt;
    private Service parentService;
    private Server server;
    private ServiceType serviceType;
    private SvcCluster svcCluster;
    private ConfigResponse configResponse;
    private Collection appServices;

    /**
     * default constructor
     */
    public Service()
    {
        super();
    }

    public boolean isAutoDiscoveryZombie()
    {
        return this.autoDiscoveryZombie;
    }

    public void setAutoDiscoveryZombie(boolean autoDiscoveryZombie)
    {
        this.autoDiscoveryZombie = autoDiscoveryZombie;
    }

    public boolean isServiceRt()
    {
        return this.serviceRt;
    }

    public void setServiceRt(boolean serviceRt)
    {
        this.serviceRt = serviceRt;
    }

    public boolean isEndUserRt()
    {
        return this.endUserRt;
    }

    public void setEndUserRt(boolean endUserRt)
    {
        this.endUserRt = endUserRt;
    }

    public Service getParentService()
    {
        return this.parentService;
    }

    public void setParentService(Service parentService)
    {
        this.parentService = parentService;
    }

    public Server getServer()
    {
        return this.server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    public ServiceType getServiceType()
    {
        return this.serviceType;
    }

    public void setServiceType(ServiceType serviceType)
    {
        this.serviceType = serviceType;
    }

    public SvcCluster getSvcCluster()
    {
        return this.svcCluster;
    }

    public void setSvcCluster(SvcCluster svcCluster)
    {
        this.svcCluster = svcCluster;
    }

    public ConfigResponse getConfigResponse()
    {
        return this.configResponse;
    }

    public void setConfigResponse(ConfigResponse configResponse)
    {
        this.configResponse = configResponse;
    }

    public Collection getAppServices()
    {
        return this.appServices;
    }

    public void setAppServices(Collection appServices)
    {
        this.appServices = appServices;
    }
}
