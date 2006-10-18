package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.ServerTypePK;

import java.util.Collection;

/**
 *
 */
public class ServerType extends AppdefResourceType
{
    private String plugin;
    private boolean virtual;
    private Collection platformTypes;
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

    public boolean isVirtual()
    {
        return this.virtual;
    }

    /**
     * legacy EJB getter
     * @deprecated use isVirtual() instead
     * @return
     */
    public boolean getVirtual()
    {
        return this.virtual;
    }

    public void setVirtual(boolean virtual)
    {
        this.virtual = virtual;
    }

    public Collection getPlatformTypes()
    {
        return this.platformTypes;
    }

    public void setPlatformTypes(Collection platformTypes)
    {
        this.platformTypes = platformTypes;
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

    private ServerTypePK pkey=new ServerTypePK();
    /**
     * @deprecated use getId()
     */
    public ServerTypePK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }
}
