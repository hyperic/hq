/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2009], Hyperic, Inc. 
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.ServerBase;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.HasAuthzOperations;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class Server extends ServerBase
    implements HasAuthzOperations
{
    private static final Map _authOps;
    static {
        _authOps = new HashMap();
        
        _authOps.put("create",       AuthzConstants.serverOpCreateServer);
        _authOps.put("modify",       AuthzConstants.serverOpModifyServer);
        _authOps.put("remove",       AuthzConstants.serverOpRemoveServer);
        _authOps.put("addService",   AuthzConstants.serverOpAddService);
        _authOps.put("view",         AuthzConstants.serverOpViewServer);
        _authOps.put("monitor",      AuthzConstants.serverOpMonitorServer);
        _authOps.put("control",      AuthzConstants.serverOpControlServer);
        _authOps.put("modifyAlerts", AuthzConstants.serverOpManageAlerts);
    }
    
    private Platform _platform;
    private boolean _runtimeAutodiscovery;
    private boolean _wasAutodiscovered;
    private boolean _autodiscoveryZombie;
    private ServerType _serverType;
    private ConfigResponseDB _configResponse;
    private Collection _services = new ArrayList();
    public Server() {
        super();
    }

    public Server(Integer id) {
        super();
        setId(id);
    }

    public Platform getPlatform() {
        return _platform;
    }

    void setPlatform(Platform platform) {
        _platform = platform;
    }

    public boolean isRuntimeAutodiscovery() {
        return _runtimeAutodiscovery;
    }

    public void setRuntimeAutodiscovery(boolean runtimeAutodiscovery) {
        _runtimeAutodiscovery = runtimeAutodiscovery;
    }

    public boolean isWasAutodiscovered() {
        return _wasAutodiscovered;
    }

    void setWasAutodiscovered(boolean wasAutodiscovered) {
        _wasAutodiscovered = wasAutodiscovered;
    }

    public boolean isAutodiscoveryZombie() {
        return _autodiscoveryZombie;
    }

    void setAutodiscoveryZombie(boolean autodiscoveryZombie) {
        _autodiscoveryZombie = autodiscoveryZombie;
    }

    public ServerType getServerType() {
        return _serverType;
    }

    void setServerType(ServerType serverType) {
        _serverType = serverType;
    }

    public ConfigResponseDB getConfigResponse() {
        return _configResponse;
    }

    void setConfigResponse(ConfigResponseDB configResponse) {
        _configResponse = configResponse;
    }

    public Collection<Service> getServices() {
        return _services;
    }

    void setServices(Collection services) {
        _services = services;
    }
    
    public void addService(Service s) {
        _services.add(s);
    }

    private ServerLightValue _serverLightValue = new ServerLightValue();

    /**
     * for legacy DTO pattern
     * @deprecated use (this) Server object instead
     * @return
     */
    public ServerLightValue getServerLightValue()
    {
        _serverLightValue.setSortName(getSortName());
        _serverLightValue.setRuntimeAutodiscovery(isRuntimeAutodiscovery());
        _serverLightValue.setWasAutodiscovered(isWasAutodiscovered());
        _serverLightValue.setAutodiscoveryZombie(isAutodiscoveryZombie());
        _serverLightValue.setConfigResponseId(_configResponse != null ?
                                              _configResponse.getId() : null);
        _serverLightValue.setModifiedBy(getModifiedBy());
        _serverLightValue.setOwner(getResource().getOwner().getName());
        _serverLightValue.setLocation(getLocation());
        _serverLightValue.setName(getName());
        _serverLightValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        _serverLightValue.setInstallPath(getInstallPath());
        _serverLightValue.setDescription(getDescription());
        _serverLightValue.setServicesAutomanaged(isServicesAutomanaged());
        _serverLightValue.setId(getId());
        _serverLightValue.setMTime(getMTime());
        _serverLightValue.setCTime(getCTime());
        if (getServerType() != null) {
            _serverLightValue.setServerType(getServerType().getServerTypeValue());
        }
        else
            _serverLightValue.setServerType(null);
        return _serverLightValue;
    }

    private ServerValue _serverValue = new ServerValue();
    /**
     * for legacy DTO pattern
     * @deprecated use (this) Server object instead
     * @return
     */
    public ServerValue getServerValue()
    {
        _serverValue.setSortName(getSortName());
        _serverValue.setRuntimeAutodiscovery(isRuntimeAutodiscovery());
        _serverValue.setWasAutodiscovered(isWasAutodiscovered());
        _serverValue.setAutodiscoveryZombie(isAutodiscoveryZombie());
        _serverValue.setConfigResponseId(_configResponse != null ?
                                         _configResponse.getId() : null);
        _serverValue.setModifiedBy(getModifiedBy());
        _serverValue.setOwner(getOwner());
        _serverValue.setLocation(getLocation());
        _serverValue.setName(getName());
        _serverValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        _serverValue.setInstallPath(getInstallPath());
        _serverValue.setDescription(getDescription());
        _serverValue.setServicesAutomanaged(isServicesAutomanaged());
        _serverValue.setId(getId());
        _serverValue.setMTime(getMTime());
        _serverValue.setCTime(getCTime());
        if ( getServerType() != null ) {
            _serverValue.setServerType(getServerType().getServerTypeValue());
        }
        else
            _serverValue.setServerType(null);
        if ( getPlatform() != null ) {
            _serverValue.setPlatform(getPlatform());
        }
        else
            _serverValue.setPlatform(null);
        return _serverValue;
    }

    private String getOwner() {
        return getResource() != null && getResource().getOwner() != null ?
                getResource().getOwner().getName() : "";
    }

    public Set getServiceSnapshot()
    {
        if (getServices() == null) {
            return new LinkedHashSet();
        }
        return new LinkedHashSet(getServices());
    }

    public Collection getSupportedServiceTypes()
    {
        // first get our service type
        ServerType myType = getServerType();
        // now get the service types
        Collection serviceTypes = myType.getServiceTypes();
        // now turn em into beans
        Collection suppSvcTypes = new HashSet();
        Iterator it = serviceTypes.iterator();
        while (it.hasNext()) {
            ServiceType svcType = (ServiceType) it.next();
            suppSvcTypes.add(svcType.getName());
        }
        return suppSvcTypes;
    }

    @Override
    public Map<String, String> changedProperties(AppdefResourceValue appdefResourceValue) {

        ServerValue serverValue = (ServerValue) appdefResourceValue;
        Map<String, String> changedProps = super.changedProperties(serverValue);

        if (!ObjectUtils.equals(getName(), serverValue.getName())) {
            changedProps.put("Name", serverValue.getName());
        }
        if (!ObjectUtils.equals(getDescription(), serverValue.getDescription())) {
            changedProps.put("Description", serverValue.getDescription());
        }
        if (!ObjectUtils.equals(getLocation(), serverValue.getLocation())) {
            changedProps.put("Location", serverValue.getLocation());
        }
        if (!ObjectUtils.equals(isRuntimeAutodiscovery(), serverValue.getRuntimeAutodiscovery())) {
            changedProps.put("RuntimeAutodiscovery", String.valueOf(serverValue.getRuntimeAutodiscovery()));
        }
        if (!ObjectUtils.equals(getInstallPath(), serverValue.getInstallPath())) {
            changedProps.put("InstallPath", serverValue.getInstallPath());
        }
        if (!ObjectUtils.equals(getAutoinventoryIdentifier(), serverValue.getAutoinventoryIdentifier())) {
            changedProps.put("AutoinventoryIdentifier", serverValue.getAutoinventoryIdentifier());
        }
        return changedProps;
    }

    /**
     * Validate a new service value object to be hosted by this server
     * @param sv
     * @exception ValidationException
     */
    void validateNewService(ServiceValue sv)
        throws ValidationException
    {
        // first we check that the server includes the specified type
        final String serviceType = sv.getServiceType().getName();
        if (!isSupportedServiceType(serviceType)) {
            String msg = "ServiceType: " + serviceType +
                         " not supported by ServerType: " +
                         getServerType().getName();
            throw new ValidationException(msg);
        }
    }

    /**
     * Check if the servertype of this server supports a ServiceType
     * This method will return false if any exceptions occur when
     * the lookup is performed.
     * @param stv - the type to check
     * @return boolean - true if its supported, false otherwise
     */
    private boolean isSupportedServiceType(String stv) {
        // check to see if it is included in the set of supported services
        Collection suppServiceTypes = getSupportedServiceTypes();
        return suppServiceTypes.contains(stv);
    }

    /**
     * legacy method
     * @param valueHolder
     */
    void updateServer(ServerValue valueHolder) {
        setDescription( valueHolder.getDescription() );
        setRuntimeAutodiscovery( valueHolder.getRuntimeAutodiscovery() );
        setWasAutodiscovered( valueHolder.getWasAutodiscovered() );
        setAutodiscoveryZombie( valueHolder.getAutodiscoveryZombie() );
        setModifiedBy( valueHolder.getModifiedBy() );
        setLocation( valueHolder.getLocation() );
        setName( valueHolder.getName() );
        setAutoinventoryIdentifier( valueHolder.getAutoinventoryIdentifier() );
        setInstallPath( valueHolder.getInstallPath() );
        setServicesAutomanaged( valueHolder.getServicesAutomanaged() );
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Server) || !super.equals(obj)) {
            return false;
        }
        Server o = (Server)obj;
        return
            ((_platform == o.getPlatform()) ||
             (_platform!=null && o.getPlatform()!=null &&
              _platform.equals(o.getPlatform())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37*result + (_platform != null ? _platform.hashCode() : 0);

        return result;
    }

    public AppdefResourceType getAppdefResourceType() {
       return _serverType;
    }

    public AppdefResourceValue getAppdefResourceValue() {
        return getServerValue();
    }

    protected String _getAuthzOp(String op) {
        return (String)_authOps.get(op);
    }
    
    public String toString() {
        return (null != getId()) ? getId().toString() : "null";
    }
}
