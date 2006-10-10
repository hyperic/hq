package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class ServerType extends AppdefResourceType
{
    private String plugin;
    private boolean fvirtual;
    private Collection platforms;
    private Collection servers;
    private Collection serviceTypes;

    /**
     * default constructor
     */
    public ServerType()
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

    public boolean isFvirtual()
    {
        return this.fvirtual;
    }

    public void setFvirtual(boolean fvirtual)
    {
        this.fvirtual = fvirtual;
    }

    public Collection getPlatforms()
    {
        return this.platforms;
    }

    public void setPlatforms(Collection platforms)
    {
        this.platforms = platforms;
    }

    public Collection getServers()
    {
        return this.servers;
    }

    public void setServers(Collection servers)
    {
        this.servers = servers;
    }

    public Collection getServiceTypes()
    {
        return this.serviceTypes;
    }

    public void setServiceTypes(Collection serviceTypes)
    {
        this.serviceTypes = serviceTypes;
    }
}
