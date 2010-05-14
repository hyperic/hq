/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2007], Hyperic, Inc. 
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

import java.util.ArrayList;
import java.util.Collection;

import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.server.session.ServiceTypeDAO;
import org.hyperic.hq.appdef.shared.AppSvcClustIncompatSvcException;
import org.hyperic.hq.appdef.shared.ServiceClusterValue;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.context.Bootstrap;

public class ServiceCluster extends AppdefBean
{
    private String        _name;
    private String        _sortName;
    private String        _description;
    private ResourceGroup _group;
    private ServiceType   _serviceType;
    private Collection    _appServices = new ArrayList();
    private Collection    _services = new ArrayList();

    public ServiceCluster() {
        super();
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
        setSortName(name);
    }

    public String getSortName() {
        return _sortName;
    }

    public void setSortName(String sortName) {
        _sortName = (sortName!=null ? sortName.toUpperCase() : null);
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }

    public ResourceGroup getGroup() {
        return _group;
    }

    public void setGroup(ResourceGroup group) {
        _group = group;
    }

    public ServiceType getServiceType() {
        return _serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        _serviceType = serviceType;
    }

    public Collection getAppServices() {
        return _appServices;
    }

    public void setAppServices(Collection appServices) {
        _appServices = appServices;
    }

    public void clearAppServices() {
        _appServices.clear();
    }
    
    public void clearServices() {
        _services.clear();
    }
    
    public Collection<Service> getServices() {
        return _services;
    }

    public void setServices(Collection services) {
        _services = services;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceCluster) || !super.equals(obj)) {
            return false;
        }
        ServiceCluster o = (ServiceCluster)obj;
        return (_name==o.getName() || (_name!=null && o.getName()!=null &&
                                       _name.equals(o.getName())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37*result + (_name != null ? _name.hashCode() : 0);
        
        return result;
    }

    private ServiceClusterValue _svcClusterVal = new ServiceClusterValue();
    
    /**
     * legacy DTO pattern
     * @deprecated use (this) ServiceCluster object instead
     */
    public ServiceClusterValue getServiceClusterValue() {
        _svcClusterVal.setName(getName());
        _svcClusterVal.setDescription(getDescription());
        _svcClusterVal.setGroupId(getGroup().getId());
        _svcClusterVal.setOwner("");
        _svcClusterVal.setModifiedBy("");
        _svcClusterVal.setLocation("");
        _svcClusterVal.setId(getId());
        _svcClusterVal.setMTime(getMTime());
        _svcClusterVal.setCTime(getCTime());
        if ( getServiceType() != null )
            _svcClusterVal.setServiceType(
                getServiceType().getServiceTypeValue() );
        else
            _svcClusterVal.setServiceType( null );
        return _svcClusterVal;
    }

    public void setServiceClusterValue(ServiceClusterValue val) {
        setName(val.getName());
        setSortName(val.getSortName());
        setDescription(val.getDescription());

        if (val.getServiceType() != null) {
            Integer stid = val.getServiceType().getId();
            ServiceType st = 
                Bootstrap.getBean(ServiceTypeDAO.class).findById(stid);
            setServiceType(st);
        }
    }
    
    /**
     * Checks whether aService's Service Type is compatible with this cluster.
     * @throws AppSvcClustIncompatSvcException if aService is incompatible
     * with this Service Cluster
     */
    public void validateMemberService(Service aService)
        throws  AppSvcClustIncompatSvcException
    {
        // validate compatibility
        if(!getServiceType().equals(aService.getServiceType())) {
            String msg =  "Service: " + aService.getId()
                + " has type: " + aService.getServiceType().getName()
                + " which does not match the clusters service type: "
                + this.getServiceType();
            throw new AppSvcClustIncompatSvcException(msg);
        }
    }
}
