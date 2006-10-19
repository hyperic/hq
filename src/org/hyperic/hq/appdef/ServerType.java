package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.ServerTypePK;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.HashSet;

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

    public ServiceType createServiceType(ServiceTypeValue stv)
    {
        throw new UnsupportedOperationException(
            "use ServiceTypeDAO.createService()");
    }

    /**
     * legacy EJB DTO (value object) pattern
     * @deprecated use (this) ServerType object instead
     */
    public ServerTypeValue getServerTypeValueObject()
    {
        ServerTypeValue vo = new ServerTypeValue();
        vo.setName(getName());
        vo.setSortName(getSortName());
        vo.setDescription(getDescription());
        vo.setPlugin(getPlugin());
        vo.setId(getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        vo.setVirtual(isVirtual());
        return vo;
    }

    private ServerTypeValue serverTypeValue = new ServerTypeValue();
    /**
     * legacy EJB DTO (value object) pattern
     * @deprecated use (this) ServerType object instead
     */
    public ServerTypeValue getServerTypeValue()
    {
        serverTypeValue.setName(getName());
        serverTypeValue.setVirtual(isVirtual());
        serverTypeValue.setSortName(getSortName());
        serverTypeValue.setDescription(getDescription());
        serverTypeValue.setPlugin(getPlugin());
        serverTypeValue.setId(getId());
        serverTypeValue.setMTime(getMTime());
        serverTypeValue.setCTime(getCTime());
        serverTypeValue.removeAllServiceTypeValues();
        Collection types = getServiceTypes();
        if (types != null) {
            Iterator isv = types.iterator();
            while (isv.hasNext()){
                serverTypeValue.addServiceTypeValue(
                    ((ServiceType)isv.next()).getServiceTypeValue());
            }
        }
        serverTypeValue.cleanServiceTypeValue();
        return serverTypeValue;
    }

    public Set getServiceTypeSnapshot()
    {
        if (getServiceTypes() == null) {
            return new LinkedHashSet();
        }
        return new LinkedHashSet(getServiceTypes());
    }
}
