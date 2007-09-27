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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;

import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;

public class ServiceType extends AppdefResourceType
{
    private String _plugin;
    private boolean _isInternal;
    private ServerType _serverType;
    private Collection _applicationTypes = new HashSet();
    private Collection _appServices = new HashSet();
    private Collection _services = new ArrayList();
    private Collection _serviceClusters = new ArrayList();

    public ServiceType() {
        super();
    }

    public String getPlugin() {
        return _plugin;
    }

    public void setPlugin(String plugin) {
        _plugin = plugin;
    }

    public boolean isIsInternal() {
        return _isInternal;
    }

    /**
     * legacy EJB getter
     * @deprecated use isIsInternal() instead
     */
    public boolean getIsInternal() {
        return _isInternal;
    }

    public void setIsInternal(boolean internal) {
        _isInternal = internal;
    }

    public ServerType getServerType() {
        return _serverType;
    }

    public void setServerType(ServerType serverType) {
        _serverType = serverType;
    }

    public Collection getApplicationTypes() {
        return _applicationTypes;
    }

    public void setApplicationTypes(Collection applications) {
        _applicationTypes = applications;
    }

    public Collection getAppServices() {
        return _appServices;
    }

    public void setAppServices(Collection appServices) {
        _appServices = appServices;
    }

    public Collection getServices() {
        return _services;
    }

    public void setServices(Collection services) {
        _services = services;
    }

    public Collection getServiceClusters() {
        return _serviceClusters;
    }

    public void setServiceClusters(Collection serviceClusters) {
        _serviceClusters = serviceClusters;
    }

    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
    }

    private ServiceTypeValue _serviceTypeValue = null;
    /**
     * legacy EJB DTO pattern
     * @deprecated use (this) ServiceType object instead
     * @return
     */
    public ServiceTypeValue getServiceTypeValue() {
        if (_serviceTypeValue == null) {
            _serviceTypeValue = new ServiceTypeValue();
            _serviceTypeValue.setName(getName());
            _serviceTypeValue.setSortName(getSortName());
            _serviceTypeValue.setDescription(getDescription());
            _serviceTypeValue.setPlugin(getPlugin());
            _serviceTypeValue.setIsInternal(getIsInternal());
            _serviceTypeValue.setId(getId());
            _serviceTypeValue.setMTime(getMTime());
            _serviceTypeValue.setCTime(getCTime());
        }
        return _serviceTypeValue;
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) ServiceType setters instead
     */
    public void setServiceTypeValue(ServiceTypeValue val) {
        setName( val.getName() );
        setSortName( val.getSortName() );
        setDescription( val.getDescription() );
        setPlugin( val.getPlugin() );
        setIsInternal( val.getIsInternal() );
        setModifiedTime(val.getMTime());
        setCreationTime(val.getCTime());
    }

    public boolean equals(Object obj) {
        return (obj instanceof ServiceType) && super.equals(obj);
    }

    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
        return getServiceTypeValue();
    }
}
