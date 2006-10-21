/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc. 
 * This file is part of HQ.         
 *  
 * HQ is free software; you can redistribute it and/or modify 
 * it under the terms version 2 of the GNU General Public License as 
 * published by the Free Software Foundation. This program is distributed 
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details. 
 *                
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 
 * USA. 
 */

package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.ServiceClusterPK;
import org.hyperic.hq.appdef.shared.ApplicationPK;
import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppSvcClustIncompatSvcException;

import java.util.Collection;
import java.util.List;

/**
 *
 */
public class ServiceCluster extends AppdefBean
{
    private String name;
    private String sortName;
    private String description;
    private Integer groupId;
    private ServiceType serviceType;
    private Collection appServices;
    private Collection services;

    // Constructors

    /**
     * default constructor
     */
    public ServiceCluster()
    {
        super();
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
        setSortName(name);
    }

    public String getSortName()
    {
        return this.sortName;
    }

    public void setSortName(String sortName)
    {
        this.sortName = (sortName!=null ? sortName.toUpperCase() : null);
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Integer getGroupId()
    {
        return this.groupId;
    }

    public void setGroupId(Integer groupId)
    {
        this.groupId = groupId;
    }

    public ServiceType getServiceType()
    {
        return this.serviceType;
    }

    public void setServiceType(ServiceType serviceType)
    {
        this.serviceType = serviceType;
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

    public boolean equals(Object obj)
    {
        if (!super.equals(obj) || !(obj instanceof ServiceCluster)) {
            return false;
        }
        ServiceCluster o = (ServiceCluster)obj;
        return (name==o.getName() || (name!=null && o.getName()!=null &&
                                      name.equals(o.getName())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (name != null ? name.hashCode() : 0);
        
        return result;
    }

    private ServiceClusterPK pkey = new ServiceClusterPK();
    /**
     * legacy EJB primary key getter
     * @deprecated use getId() instead
     * @return
     */
    public ServiceClusterPK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }

    public AppSvcDependency addDependentService(ApplicationPK appPK,
                                                ServicePK depPK)
    {
        throw new UnsupportedOperationException(
            "use AppServiceDAO.addDependentService()");
    }

    public AppSvcDependency addDependentServiceCluster(ApplicationPK appPK,
                                                       ServiceClusterPK depPK)
    {
        throw new UnsupportedOperationException(
            "use AppServiceDAO.addDependentServiceCluster()");
    }

    public void addService(Integer serviceId)
    {
        throw new UnsupportedOperationException(
            "use ServiceClusterDAO.addService()");
    }

    public void removeService(Integer serviceId)
    {
        throw new UnsupportedOperationException(
            "use ServiceClusterDAO.removeService()");
    }

    public void updateCluster(ServiceClusterValue serviceCluster,
                              List serviceIds)
    {
        throw new UnsupportedOperationException(
            "use ServiceClusterDAO.updateCluster()");
    }

    private ServiceClusterValue serviceClusterValue = new ServiceClusterValue();
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) ServiceCluster object instead
     * @return
     */
    public ServiceClusterValue getServiceClusterValue()
    {
        serviceClusterValue.setName(getName());
        serviceClusterValue.setDescription(getDescription());
        serviceClusterValue.setGroupId(getGroupId());
        serviceClusterValue.setOwner("");
        serviceClusterValue.setModifiedBy("");
        serviceClusterValue.setLocation("");
        serviceClusterValue.setId(getId());
        serviceClusterValue.setMTime(getMTime());
        serviceClusterValue.setCTime(getCTime());
        if ( getServiceType() != null )
            serviceClusterValue.setServiceType(
                getServiceType().getServiceTypeValue() );
        else
            serviceClusterValue.setServiceType( null );
        return serviceClusterValue;
    }

    public void setServiceClusterValue(ServiceClusterValue val)
    {
        setName( val.getName() );
        setSortName( val.getSortName() );
        setDescription( val.getDescription() );
        setGroupId( val.getGroupId() );

        if (val.getServiceType() != null) {
            ServiceType st = new ServiceType();
            st.setId(val.getServiceType().getId());
            setServiceType(st);
        }
    }

    /**
     * Validate a new service to be added to this cluster. This enforces
     * service type compatibility as well as only allowing services to be in
     * one cluster at a time
     */
    public void validateMemberService(Service aService)
        throws AppSvcClustDuplicateAssignException,
               AppSvcClustIncompatSvcException
    {
        // validate its not assigned to a cluster already or if it is
        // its assigned to this cluster.
        if(aService.getServiceCluster() != null &&
           !aService.getServiceCluster().equals(this)) {
            throw new AppSvcClustDuplicateAssignException ("Service: "
                + aService.getId()
                + " is already assigned to a cluster");
        }
        // validate compatibility
        if(!getServiceType().equals(aService.getServiceType())) {
            throw new AppSvcClustIncompatSvcException("Service: "+ aService.getId()

                + " has type: " + aService.getServiceType().getName()
                + " which does not match the clusters service type: "
                + this.getServiceType());
        }
    }
}
