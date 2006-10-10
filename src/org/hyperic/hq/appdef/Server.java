package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class Server extends AppdefResource
{
    private Platform platform;
    private String autoInventoryIdentifier;
    private boolean runTimeAutoDiscovery;
    private boolean wasAutoDiscovered;
    private boolean servicesAutoManaged;
    private boolean autoDiscoveryZombie;
    private String installPath;
    private ServerType serverType;
    private ConfigResponse configResponse;
    private Collection aiServices;
    private Collection services;

    /**
     * default constructor
     */
    public Server()
    {
        super();
    }

    // Property accessors
    public Platform getPlatform()
    {
        return this.platform;
    }

    public void setPlatform(Platform platform)
    {
        this.platform = platform;
    }

    public String getAutoInventoryIdentifier()
    {
        return this.autoInventoryIdentifier;
    }

    public void setAutoInventoryIdentifier(String autoInventoryIdentifier)
    {
        this.autoInventoryIdentifier = autoInventoryIdentifier;
    }

    public boolean isRunTimeAutoDiscovery()
    {
        return this.runTimeAutoDiscovery;
    }

    public void setRunTimeAutoDiscovery(boolean runTimeAutoDiscovery)
    {
        this.runTimeAutoDiscovery = runTimeAutoDiscovery;
    }

    public boolean isWasAutoDiscovered()
    {
        return this.wasAutoDiscovered;
    }

    public void setWasAutoDiscovered(boolean wasAutoDiscovered)
    {
        this.wasAutoDiscovered = wasAutoDiscovered;
    }

    public boolean isServicesAutoManaged()
    {
        return this.servicesAutoManaged;
    }

    public void setServicesAutoManaged(boolean servicesAutoManaged)
    {
        this.servicesAutoManaged = servicesAutoManaged;
    }

    public boolean isAutoDiscoveryZombie()
    {
        return this.autoDiscoveryZombie;
    }

    public void setAutoDiscoveryZombie(boolean autoDiscoveryZombie)
    {
        this.autoDiscoveryZombie = autoDiscoveryZombie;
    }

    public String getInstallPath()
    {
        return this.installPath;
    }

    public void setInstallPath(String installPath)
    {
        this.installPath = installPath;
    }

    public ServerType getServerType()
    {
        return this.serverType;
    }

    public void setServerType(ServerType serverType)
    {
        this.serverType = serverType;
    }

    public ConfigResponse getConfigResponse()
    {
        return this.configResponse;
    }

    public void setConfigResponse(ConfigResponse configResponse)
    {
        this.configResponse = configResponse;
    }

    public Collection getAiServices()
    {
        return this.aiServices;
    }

    public void setAiServices(Collection aiServices)
    {
        this.aiServices = aiServices;
    }

    public Collection getServices()
    {
        return this.services;
    }

    public void setServices(Collection services)
    {
        this.services = services;
    }
}
