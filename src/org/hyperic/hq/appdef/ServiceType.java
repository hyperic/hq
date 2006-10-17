package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.ServiceTypePK;
import org.hyperic.hq.appdef.shared.ServiceTypeLocal;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeLocal;

import java.util.Collection;

/**
 *
 */
public class ServiceType extends AppdefResourceType
{
    private String plugin;
    private boolean isInternal;
    private ServerType serverType;
    private TierType tierType;
    private Collection applicationTypes;
    private Collection appServices;
    private Collection services;
    private Collection serviceClusters;

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

    public boolean isIsInternal()
    {
        return this.isInternal;
    }

    /**
     * legacy EJB getter
     * @deprecated use isIsInternal() instead
     * @return
     */
    public boolean getIsInternal()
    {
        return this.isInternal;
    }

    public void setIsInternal(boolean internal)
    {
        this.isInternal = internal;
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

    public Collection getApplicationTypes()
    {
        return this.applicationTypes;
    }

    public void setApplicationTypes(Collection applications)
    {
        this.applicationTypes = applications;
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

    public Collection getServiceClusters()
    {
        return this.serviceClusters;
    }

    public void setServiceClusters(Collection serviceClusters)
    {
        this.serviceClusters = serviceClusters;
    }

    private ServiceTypePK pkey = new ServiceTypePK();
    /**
     * legacy EJB primary key getter
     * @deprecated use getId() instead
     * @return
     */
    public ServiceTypePK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }

    private ServiceTypeValue serviceTypeValue = new ServiceTypeValue();
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) ServiceType object instead
     * @return
     */
    public ServiceTypeValue getServiceTypeValue()
    {
        serviceTypeValue.setName(getName());
        serviceTypeValue.setSortName(getSortName());
        serviceTypeValue.setDescription(getDescription());
        serviceTypeValue.setPlugin(getPlugin());
        serviceTypeValue.setIsInternal(getIsInternal());
        serviceTypeValue.setId(getId());
        serviceTypeValue.setMTime(getMTime());
        serviceTypeValue.setCTime(getCTime());
        return serviceTypeValue;
    }

    /**
     * legacy DTO pattern
     * @depreacated use (this) ServiceType setters instead
     * @param val
     */
    public void setServiceTypeValue(ServiceTypeValue val)
    {
        setName( val.getName() );
        setSortName( val.getSortName() );
        setDescription( val.getDescription() );
        setPlugin( val.getPlugin() );
        setIsInternal( val.getIsInternal() );
    }

    /**
     * legacy EJB setter
     * @deprecated use setServerType(ServerType) instead
     * @param serverType
     */
    public void setServerType(ServerTypeLocal serverType)
    {
        if (serverType != null && serverType.getId() != null) {
            ServerType st = new ServerType();
            st.setId(serverType.getId());
            setServerType(st);
        }
    }
}
