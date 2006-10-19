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

import java.util.Collection;

/**
 *
 */
public class AppService extends AppdefBean
{
    private Service service;
    private ServiceCluster serviceCluster;
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

        return ((this.getService() == castOther.getService()) ||
                (this.getService() != null &&
                 castOther.getService() != null &&
                 this.getService().equals(castOther.getService())))
               && ((this.getServiceCluster() == castOther.getServiceCluster()) ||
                   (this.getServiceCluster() != null &&
                    castOther.getServiceCluster() != null &&
                    this.getServiceCluster().equals(castOther.getServiceCluster())))
               && ((this.getApplication() == castOther.getApplication()) ||
                   (this.getApplication() != null &&
                    castOther.getApplication() != null &&
                    this.getApplication().equals(castOther.getApplication())));
    }

    public int hashCode()
    {
        int result = 17;


        return result;
    }
}
