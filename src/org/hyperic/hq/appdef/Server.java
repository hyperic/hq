package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.ServerPK;

import java.util.Collection;

/**
 * Pojo for hibernate hbm mapping file
 * TODO: fix equals and hashCode()
 */
public class Server extends AppdefResource
{
    private Platform platform;
    private String autoinventoryIdentifier;
    private boolean runtimeAutodiscovery;
    private boolean wasAutodiscovered;
    private boolean servicesAutomanaged;
    private boolean autodiscoveryZombie;
    private String installPath;
    private ServerType serverType;
    private ConfigResponseDB configResponse;
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

    public String getAutoinventoryIdentifier()
    {
        return this.autoinventoryIdentifier;
    }

    public void setAutoinventoryIdentifier(String autoinventoryIdentifier)
    {
        this.autoinventoryIdentifier = autoinventoryIdentifier;
    }

    public boolean isRuntimeAutodiscovery()
    {
        return this.runtimeAutodiscovery;
    }

    public void setRuntimeAutodiscovery(boolean runtimeAutodiscovery)
    {
        this.runtimeAutodiscovery = runtimeAutodiscovery;
    }

    public boolean isWasAutodiscovered()
    {
        return this.wasAutodiscovered;
    }

    public void setWasAutodiscovered(boolean wasAutodiscovered)
    {
        this.wasAutodiscovered = wasAutodiscovered;
    }

    public boolean isServicesAutomanaged()
    {
        return this.servicesAutomanaged;
    }

    public void setServicesAutomanaged(boolean servicesAutomanaged)
    {
        this.servicesAutomanaged = servicesAutomanaged;
    }

    public boolean isAutodiscoveryZombie()
    {
        return this.autodiscoveryZombie;
    }

    public void setAutodiscoveryZombie(boolean autodiscoveryZombie)
    {
        this.autodiscoveryZombie = autodiscoveryZombie;
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

    public ConfigResponseDB getConfigResponse()
    {
        return this.configResponse;
    }

    public void setConfigResponse(ConfigResponseDB configResponse)
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

    private ServerPK pkey = new ServerPK();
    /**
     * legacy EJB primary key getter
     * @deprecated use getId() instead
     * @return
     */
    public ServerPK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }
}
