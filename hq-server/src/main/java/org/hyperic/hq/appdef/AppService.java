/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2008], Hyperic, Inc. 
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

import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceDAO;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.server.session.ServiceTypeDAO;
import org.hyperic.hq.appdef.shared.AppServiceValue;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.context.Bootstrap;

public class AppService
    extends AppdefBean {
    private Service _service;
    private ResourceGroup _resourceGroup;
    private Application _application;
    private boolean _isGroup;
    private String _modifiedBy;
    private boolean _isEntryPoint = true;
    private ServiceType _serviceType;
    private Collection _appSvcDependencies;

    public AppService() {
        super();
    }

    public AppService(Integer id) {
        super(id);
    }

    public Service getService() {
        return _service;
    }

    public void setService(Service service) {
        _service = service;
    }

    public ResourceGroup getResourceGroup() {
        return _resourceGroup;
    }

    public void setResourceGroup(ResourceGroup group) {
        _resourceGroup = group;
    }

    public Application getApplication() {
        return _application;
    }

    public void setApplication(Application application) {
        _application = application;
    }

    public boolean isIsGroup() {
        return _isGroup;
    }

    public void setIsGroup(boolean isGroup) {
        _isGroup = isGroup;
    }

    public String getModifiedBy() {
        return _modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        _modifiedBy = modifiedBy;
    }

    public boolean isEntryPoint() {
        return _isEntryPoint;
    }

    public void setEntryPoint(boolean entryPoint) {
        _isEntryPoint = entryPoint;
    }

    public ServiceType getServiceType() {
        return _serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        _serviceType = serviceType;
    }

    public Collection getAppSvcDependencies() {
        return _appSvcDependencies;
    }

    public void setAppSvcDependencies(Collection dependents) {
        _appSvcDependencies = dependents;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AppService) || !super.equals(obj)) {
            return false;
        }
        AppService o = (AppService) obj;
        return (_service == o.getService() || (_service != null && o.getService() != null && _service
            .equals(o.getService()))) &&
               (_resourceGroup == o.getResourceGroup() || (_resourceGroup != null &&
                                                           o.getResourceGroup() != null && _resourceGroup
                   .equals(o.getResourceGroup()))) &&
               (_application == o.getApplication() || (_application != null &&
                                                       o.getApplication() != null && _application
                   .equals(o.getApplication())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + (_service != null ? _service.hashCode() : 0);
        result = 37 * result + (_application != null ? _application.hashCode() : 0);
        result = 37 * result + (_resourceGroup != null ? _resourceGroup.hashCode() : 0);

        return result;
    }

    private AppServiceValue appServiceValue = new AppServiceValue();

    /**
     * @deprecated use (this) AppService object instead
     */
    public AppServiceValue getAppServiceValue() {
        appServiceValue.setIsCluster(isIsGroup());
        appServiceValue.setIsEntryPoint(isEntryPoint());
        appServiceValue.setId(getId());
        appServiceValue.setMTime(getMTime());
        appServiceValue.setCTime(getCTime());
        if (getService() != null)
            appServiceValue.setService(getService());
        else
            appServiceValue.setService(null);
        if (getServiceType() != null)
            appServiceValue.setServiceType(getServiceType());
        else
            appServiceValue.setServiceType(null);
        return appServiceValue;
    }

    public void setAppServiceValue(AppServiceValue value) {
        setIsGroup(value.getIsCluster());
        setEntryPoint(value.getIsEntryPoint());

        if (value.getService() != null) {
            Integer i = value.getService().getId();
            Service s = Bootstrap.getBean(ServiceDAO.class).findById(i);
            setService(s);
        }

        if (value.getServiceCluster() != null) {
            Integer i = value.getServiceCluster().getId();
            ResourceGroup gr = Bootstrap.getBean(ResourceGroupDAO.class).findById(i);
            setResourceGroup(gr);
        }

        if (value.getServiceType() != null) {
            Integer i = value.getServiceType().getId();
            ServiceType st = Bootstrap.getBean(ServiceTypeDAO.class).findById(i);
            setServiceType(st);
        }
    }
}
