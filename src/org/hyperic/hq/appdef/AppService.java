package org.hyperic.hq.appdef;

import java.util.Collection;

/**
 *
 */
public class AppService extends AppdefBean
{
    private Service service;
    private SvcCluster cluster;
    private Application application;
    private boolean isCluster;
    private String modifiedBy;
    private boolean fentryPoint;
    private ServiceType serviceType;
    private Collection dependents;

    /**
     * default constructor
     */
    public AppService()
    {
        super();
    }

    public Service getService()
    {
        return this.service;
    }

    public void setService(Service service)
    {
        this.service = service;
    }

    public SvcCluster getCluster()
    {
        return this.cluster;
    }

    public void setCluster(SvcCluster cluster)
    {
        this.cluster = cluster;
    }

    public Application getApplication()
    {
        return this.application;
    }

    public void setApplication(Application application)
    {
        this.application = application;
    }

    public boolean isIsCluster()
    {
        return this.isCluster;
    }

    public void setIsCluster(boolean isCluster)
    {
        this.isCluster = isCluster;
    }

    public String getModifiedBy()
    {
        return this.modifiedBy;
    }

    public void setModifiedBy(String modifiedBy)
    {
        this.modifiedBy = modifiedBy;
    }

    public boolean isFentryPoint()
    {
        return this.fentryPoint;
    }

    public void setFentryPoint(boolean fentryPoint)
    {
        this.fentryPoint = fentryPoint;
    }

    public ServiceType getServiceType()
    {
        return this.serviceType;
    }

    public void setServiceType(ServiceType serviceType)
    {
        this.serviceType = serviceType;
    }

    public Collection getDependents()
    {
        return this.dependents;
    }

    public void setDependents(Collection dependents)
    {
        this.dependents = dependents;
    }

    // TODO: fix equals and hashCode
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof AppService)) return false;
        AppService castOther = (AppService) other;

        return ((this.getService() == castOther.getService()) || (this.getService() != null && castOther.getService() != null && this.getService().equals(castOther.getService())))
               && ((this.getCluster() == castOther.getCluster()) || (this.getCluster() != null && castOther.getCluster() != null && this.getCluster().equals(castOther.getCluster())))
               && ((this.getApplication() == castOther.getApplication()) || (this.getApplication() != null && castOther.getApplication() != null && this.getApplication().equals(castOther.getApplication())));
    }

    public int hashCode()
    {
        int result = 17;


        return result;
    }
}
