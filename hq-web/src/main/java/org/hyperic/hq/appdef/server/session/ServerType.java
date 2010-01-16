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

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class ServerType extends AppdefResourceType
{
    private String _plugin;
    private boolean _virtual;
    private Collection _platformTypes = new HashSet();
    private Collection _servers = new ArrayList();
    private Collection _serviceTypes = new ArrayList();

    public ServerType() {
        super();
    }

    public int getAuthzType() {
        return AuthzConstants.authzServerProto.intValue();
    }
    
    // Property accessors
    public String getPlugin() {
        return _plugin;
    }

    public void setPlugin(String plugin) {
        _plugin = plugin;
    }

    public boolean isVirtual() {
        return _virtual;
    }

    /**
     * legacy getter
     * @deprecated use isVirtual() instead
     * @return
     */
    public boolean getVirtual() {
        return _virtual;
    }

    public void setVirtual(boolean virtual) {
        _virtual = virtual;
    }

    public Collection<PlatformType> getPlatformTypes() {
        return _platformTypes;
    }

    public void setPlatformTypes(Collection platformTypes) {
        _platformTypes = platformTypes;
    }

    public void addPlatformType(PlatformType ptype) {
        _platformTypes.add(ptype);
        ptype.getServerTypesBag().add(this);
    }

    public Collection<Server> getServers() {
        return _servers;
    }

    public void setServers(Collection servers) {
        _servers = servers;
    }

    public Collection<ServiceType> getServiceTypes() {
        return _serviceTypes;
    }

    public void setServiceTypes(Collection serviceTypes) {
        _serviceTypes = serviceTypes;
    }

    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_SERVER;
    }

    private ServerTypeValue _serverTypeValue = new ServerTypeValue();
    /**
     * legacy DTO (value object) pattern
     * @deprecated use (this) ServerType object instead
     */
    public ServerTypeValue getServerTypeValue()
    {
        _serverTypeValue.setName(getName());
        _serverTypeValue.setVirtual(isVirtual());
        _serverTypeValue.setSortName(getSortName());
        _serverTypeValue.setDescription(getDescription());
        _serverTypeValue.setPlugin(getPlugin());
        _serverTypeValue.setId(getId());
        _serverTypeValue.setMTime(getMTime());
        _serverTypeValue.setCTime(getCTime());
        return _serverTypeValue;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ServerType == false)
            return false;
        
        return ((ServerType)obj).getName().equals(getName());
    }
    
    public int hashCode() {
        return getName().hashCode();
    }

    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
        return getServerTypeValue();
    }
}
