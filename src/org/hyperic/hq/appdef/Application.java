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

import org.hyperic.hq.appdef.shared.ServicePK;
import org.hyperic.hq.appdef.shared.ServiceClusterPK;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.ApplicationPK;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 *
 */
public class Application extends AppdefResource
{
    private String engContact;
    private String opsContact;
    private String businessContact;
    private ApplicationType applicationType;
    private Collection appServices;

    /**
     * default constructor
     */
    public Application()
    {
        super();
    }

    public Application(Integer id)
    {
        super();
        setId(id);
    }

    public String getEngContact()
    {
        return this.engContact;
    }

    public void setEngContact(String engContact)
    {
        this.engContact = engContact;
    }

    public String getOpsContact()
    {
        return this.opsContact;
    }

    public void setOpsContact(String opsContact)
    {
        this.opsContact = opsContact;
    }

    public String getBusinessContact()
    {
        return this.businessContact;
    }

    public void setBusinessContact(String businessContact)
    {
        this.businessContact = businessContact;
    }

    public ApplicationType getApplicationType()
    {
        return this.applicationType;
    }

    public void setApplicationType(ApplicationType applicationType)
    {
        this.applicationType = applicationType;
    }

    public Collection getAppServices()
    {
        return this.appServices;
    }

    public void setAppServices(Collection appServices)
    {
        this.appServices = appServices;
    }

    public AppService addEntryPoint(ServicePK aService)
    {
        throw new UnsupportedOperationException(
            "use AppServiceDAO.createService()");
    }

    public AppService addServiceCluster(ServiceClusterPK aClusterPK)
    {
        throw new UnsupportedOperationException(
            "use AppServiceDAO.createService()");
    }

    public AppService addService(ServicePK aService)
    {
        throw new UnsupportedOperationException(
            "use AppServiceDAO.createService()");
    }

    public Set getAppServiceSnapshot()
    {
        if (getAppServices() == null) {
            return new LinkedHashSet();
        }
        return new LinkedHashSet(getAppServices());
    }

    private ApplicationValue applicationValue = new ApplicationValue();
    /**
     * @deprecated use (this) Application object instead
     * @return
     */
    public ApplicationValue getApplicationValue()
    {
        applicationValue.setName(getName());
        applicationValue.setSortName(getSortName());
        applicationValue.setModifiedBy(getModifiedBy());
        applicationValue.setOwner(getOwner());
        applicationValue.setLocation(getLocation());
        applicationValue.setEngContact(getEngContact());
        applicationValue.setOpsContact(getOpsContact());
        applicationValue.setBusinessContact(getBusinessContact());
        applicationValue.setDescription(getDescription());
        applicationValue.setId(getId());
        applicationValue.setMTime(getMTime());
        applicationValue.setCTime(getCTime());
        applicationValue.removeAllAppServiceValues();
        if (getAppServices() != null) {
            Iterator iAppServiceValue = getAppServices().iterator();
            while (iAppServiceValue.hasNext()){
                applicationValue.addAppServiceValue(
                    ((AppService)iAppServiceValue.next()).getAppServiceValue() );
            }
        }
        applicationValue.cleanAppServiceValue();
        if ( getApplicationType() != null )
            applicationValue.setApplicationType(
                getApplicationType().getApplicationTypeValue() );
        else
            applicationValue.setApplicationType( null );
        return applicationValue;
    }

    public ApplicationValue getApplicationValueObject()
    {
        ApplicationValue vo = new ApplicationValue();
        vo.setName(getName());
        vo.setSortName(getSortName());
        vo.setModifiedBy(getModifiedBy());
        vo.setOwner(getOwner());
        vo.setLocation(getLocation());
        vo.setEngContact(getEngContact());
        vo.setOpsContact(getOpsContact());
        vo.setBusinessContact(getBusinessContact());
        vo.setDescription(getDescription());
        vo.setId(getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        if ( getApplicationType() != null )
            vo.setApplicationType(
                getApplicationType().getApplicationTypeValue() );
        else
            vo.setApplicationType( null );
        return vo;
    }

    private ApplicationPK pkey = new ApplicationPK();
    /**
     * @deprecated use getId()
     * @return
     */
    public ApplicationPK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof Application) && super.equals(obj);
    }
}
