package org.hyperic.hq.appdef;

import java.io.Serializable;

/**
 *
 */
public class ServiceDepMap implements Serializable
{
    private Integer id;
    private long creationTime;
    private long modifiedTime;
    private AppService appService;
    private AppService dependentService;

    /**
     * default constructor
     */
    public ServiceDepMap()
    {
        super();
    }

    // Property accessors
    public Integer getId()
    {
        return this.id;
    }

    private void setId(Integer id)
    {
        this.id = id;
    }

    public long getCreationTime()
    {
        return this.creationTime;
    }

    public void setCreationTime(long creationTime)
    {
        this.creationTime = creationTime;
    }

    public long getModifiedTime()
    {
        return this.modifiedTime;
    }

    public void setModifiedTime(long modifiedTime)
    {
        this.modifiedTime = modifiedTime;
    }

    public AppService getAppService()
    {
        return this.appService;
    }

    public void setAppService(AppService appService)
    {
        this.appService = appService;
    }

    public AppService getDependentService()
    {
        return this.dependentService;
    }

    public void setDependentService(AppService dependentService)
    {
        this.dependentService = dependentService;
    }


}
