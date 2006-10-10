package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 * 
 */
public class PlatformType extends AppdefResourceType
{
    private String os;
    private String osVersion;
    private String arch;
    private String plugin;
    private Collection servers;
    private Collection platforms;

    /**
     * default constructor
     */
    public PlatformType()
    {
        super();
    }

    // Property accessors
    public String getOs()
    {
        return this.os;
    }

    public void setOs(String os)
    {
        this.os = os;
    }

    public String getOsVersion()
    {
        return this.osVersion;
    }

    public void setOsVersion(String osVersion)
    {
        this.osVersion = osVersion;
    }

    public String getArch()
    {
        return this.arch;
    }

    public void setArch(String arch)
    {
        this.arch = arch;
    }

    public String getPlugin()
    {
        return this.plugin;
    }

    public void setPlugin(String plugin)
    {
        this.plugin = plugin;
    }

    public Collection getServers()
    {
        return this.servers;
    }

    public void setServers(Collection servers)
    {
        this.servers = servers;
    }

    public Collection getPlatforms()
    {
        return this.platforms;
    }

    public void setPlatforms(Collection platforms)
    {
        this.platforms = platforms;
    }
}
