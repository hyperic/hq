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

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppServiceValue;

import java.util.Collection;

/**
 * Pojo for hibernate hbm mapping file
 */
public class AppService extends AppdefBean
{
    private Service service;
    private ServiceCluster serviceCluster;
    private Application application;
    private boolean isCluster;
    private String modifiedBy;
    private boolean isEntryPoint = true;
    private ServiceType serviceType;
    private Collection appSvcDependencies;

    /**
     * default constructor
     */
    public AppService()
    {
        super();
    }

    public AppService(Integer id)
    {
        super(id);
    }

    public Service getService()
    {
        return this.service;
    }

    public void setService(Service service)
    {
        this.service = service;
    }

    public ServiceCluster getServiceCluster()
    {
        return this.serviceCluster;
    }

    public void setServiceCluster(ServiceCluster cluster)
    {
        this.serviceCluster = cluster;
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

    public boolean isEntryPoint()
    {
        return this.isEntryPoint;
    }

    public void setEntryPoint(boolean entryPoint)
    {
        this.isEntryPoint = entryPoint;
    }

    public ServiceType getServiceType()
    {
        return this.serviceType;
    }

    public void setServiceType(ServiceType serviceType)
    {
        this.serviceType = serviceType;
    }

    public Collection getAppSvcDependencies()
    {
        return this.appSvcDependencies;
    }

    public void setAppSvcDependencies(Collection dependents)
    {
        this.appSvcDependencies = dependents;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof AppService) || !super.equals(obj)) {
            return false;
        }
        AppService o = (AppService)obj;
        return (service == o.getService() ||
                (service!=null && o.getService()!=null &&
                 service.equals(o.getService())))
               &&
               (serviceCluster == o.getServiceCluster() ||
                (serviceCluster!=null && o.getServiceCluster()!=null &&
                 serviceCluster.equals(o.getServiceCluster())))
               &&
               (application == o.getApplication() ||
                (application!=null && o.getApplication()!=null &&
                 application.equals(o.getApplication())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (service!=null ? service.hashCode() : 0);
        result = 37*result + (application!=null ? application.hashCode() : 0);
        result = 37*result + (serviceCluster!=null?serviceCluster.hashCode():0);

        return result;
    }

    private AppServiceValue appServiceValue = new AppServiceValue();
    /**
     * @deprecated use (this) AppService object instead
     */
    public AppServiceValue getAppServiceValue()
    {
        appServiceValue.setIsCluster(isIsCluster());
        appServiceValue.setIsEntryPoint(isEntryPoint());
        appServiceValue.setId(getId());
        appServiceValue.setMTime(getMTime());
        appServiceValue.setCTime(getCTime());
        if ( getService() != null )
            appServiceValue.setService(
                getService().getServiceLightValue() );
        else
            appServiceValue.setService( null );
        if ( getServiceCluster() != null )
            appServiceValue.setServiceCluster(
                getServiceCluster().getServiceClusterValue() );
        else
            appServiceValue.setServiceCluster( null );
        if ( getServiceType() != null )
            appServiceValue.setServiceType(
                getServiceType().getServiceTypeValue() );
        else
            appServiceValue.setServiceType( null );
        return appServiceValue;
    }

    public void setAppServiceValue(AppServiceValue value) {
        setIsCluster( value.getIsCluster() );
        setEntryPoint( value.getIsEntryPoint() );

        if (value.getService() != null) {
            Integer i = value.getService().getId();
            Service s = DAOFactory.getDAOFactory().getServiceDAO().findById(i);
            setService(s);
        }

        if (value.getServiceCluster() != null) {
            Integer i = value.getServiceCluster().getId();
            ServiceCluster sc = 
                DAOFactory.getDAOFactory().getServiceClusterDAO().findById(i);
            setServiceCluster(sc);
        }

        if (value.getServiceType() != null) {
            Integer i = value.getServiceType().getId();
            ServiceType st = 
                DAOFactory.getDAOFactory().getServiceTypeDAO().findById(i);
            setServiceType(st);
        }
    }

}
