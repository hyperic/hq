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

import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.ServerBase;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ArrayList;

public class Server extends ServerBase
{
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

    public void setPlatform(Platform platform) {
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

    public void setWasAutodiscovered(boolean wasAutodiscovered) {
        _wasAutodiscovered = wasAutodiscovered;
    }

    public boolean isAutodiscoveryZombie() {
        return _autodiscoveryZombie;
    }

    public void setAutodiscoveryZombie(boolean autodiscoveryZombie) {
        _autodiscoveryZombie = autodiscoveryZombie;
    }

    public ServerType getServerType() {
        return _serverType;
    }

    public void setServerType(ServerType serverType) {
        _serverType = serverType;
    }

    public ConfigResponseDB getConfigResponse() {
        return _configResponse;
    }

    public void setConfigResponse(ConfigResponseDB configResponse) {
        _configResponse = configResponse;
    }

    public Collection getServices() {
        return _services;
    }

    public void setServices(Collection services) {
        _services = services;
    }

    /**
     * @deprecated use getConfigResponse().getId()
     */
    public Integer getConfigResponseId() {
        return _configResponse != null ? _configResponse.getId() : null;
    }

    private ServerLightValue _serverLightValue = new ServerLightValue();

    /**
     * for legacy EJB DTO pattern
     * @deprecated use (this) Server object instead
     * @return
     */
    public ServerLightValue getServerLightValue()
    {
        _serverLightValue.setSortName(getSortName());
        _serverLightValue.setRuntimeAutodiscovery(getRuntimeAutodiscovery());
        _serverLightValue.setWasAutodiscovered(getWasAutodiscovered());
        _serverLightValue.setAutodiscoveryZombie(getAutodiscoveryZombie());
        _serverLightValue.setConfigResponseId(getConfigResponseId());
        _serverLightValue.setModifiedBy(getModifiedBy());
        _serverLightValue.setOwner(getOwner());
        _serverLightValue.setLocation(getLocation());
        _serverLightValue.setName(getName());
        _serverLightValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        _serverLightValue.setInstallPath(getInstallPath());
        _serverLightValue.setDescription(getDescription());
        _serverLightValue.setServicesAutomanaged(getServicesAutomanaged());
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
     * for legacy EJB DTO pattern
     * @deprecated use (this) Server object instead
     * @return
     */
    public ServerValue getServerValue()
    {
        _serverValue.setSortName(getSortName());
        _serverValue.setRuntimeAutodiscovery(getRuntimeAutodiscovery());
        _serverValue.setWasAutodiscovered(getWasAutodiscovered());
        _serverValue.setAutodiscoveryZombie(getAutodiscoveryZombie());
        _serverValue.setConfigResponseId(getConfigResponseId());
        _serverValue.setModifiedBy(getModifiedBy());
        _serverValue.setOwner(getOwner());
        _serverValue.setLocation(getLocation());
        _serverValue.setName(getName());
        _serverValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        _serverValue.setInstallPath(getInstallPath());
        _serverValue.setDescription(getDescription());
        _serverValue.setServicesAutomanaged(getServicesAutomanaged());
        _serverValue.setId(getId());
        _serverValue.setMTime(getMTime());
        _serverValue.setCTime(getCTime());
        if ( getServerType() != null ) {
            _serverValue.setServerType(getServerType().getServerTypeValue());
        }
        else
            _serverValue.setServerType(null);
        if ( getPlatform() != null ) {
            _serverValue.setPlatform(getPlatform().getPlatformLightValue());
        }
        else
            _serverValue.setPlatform(null);
        return _serverValue;
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
        Collection suppSvcTypes = new ArrayList();
        Iterator it = serviceTypes.iterator();
        while(it.hasNext()) {
            ServiceType svcType = (ServiceType)it.next();
            suppSvcTypes.add(svcType.getName());
        }
        return suppSvcTypes;
    }

    public boolean matchesValueObject(ServerValue obj)
    {
        boolean matches = true;
        matches = super.matchesValueObject(obj) &&
            (getName() != null ? this.getName().equals(obj.getName())
                : (obj.getName() == null)) &&
            (getDescription() != null ?
                this.getDescription().equals(obj.getDescription())
                : (obj.getDescription() == null)) &&
            (getLocation() != null ?
                this.getLocation().equals(obj.getLocation())
                : (obj.getLocation() == null)) &&
            (getOwner() != null ? this.getOwner().equals(obj.getOwner())
                : (obj.getOwner() == null)) &&
            (getRuntimeAutodiscovery() == obj.getRuntimeAutodiscovery()) &&
            (getInstallPath().equals(obj.getInstallPath())) &&
            (getAutoinventoryIdentifier() != null ?
                getAutoinventoryIdentifier().equals(
                    obj.getAutoinventoryIdentifier())
                : (obj.getAutoinventoryIdentifier() == null));

        return matches;
    }

    /**
     * Validate a new service value object to be hosted by this server
     * @param sv
     * @exception ValidationException
     */
    public void validateNewService(ServiceValue sv)
        throws ValidationException
    {
        String msg = null;
        // first we check that the server includes the specified type
        if(!isSupportedServiceType(sv.getServiceType())) {
            msg = "ServiceType: " + sv.getServiceType().getName()
                + " not supported by ServerType: "
                + this.getServerType().getName();
        }

        // now deal with the message
        if(msg != null) {
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
    private boolean isSupportedServiceType(ServiceTypeValue stv) {
        boolean REQUIRED;
        // Look up the ServiceTypeLocal
        // ServiceTypeLocal serviceType =
        //    ServiceTypeUtil.getLocalHome().findByPrimaryKey(
        //        stv.getPrimaryKey());
        // now check to see if it is included in the set of
        // supported services
        Collection suppServiceTypes = getSupportedServiceTypes();
        REQUIRED = suppServiceTypes.contains(stv.getName());
        return REQUIRED;
    }

    /**
     * legacy EJB method
     * @param valueHolder
     */
    public void updateServer(ServerValue valueHolder) {
        setDescription( valueHolder.getDescription() );
        setRuntimeAutodiscovery( valueHolder.getRuntimeAutodiscovery() );
        setWasAutodiscovered( valueHolder.getWasAutodiscovered() );
        setAutodiscoveryZombie( valueHolder.getAutodiscoveryZombie() );
        setModifiedBy( valueHolder.getModifiedBy() );
        setOwner( valueHolder.getOwner() );
        setLocation( valueHolder.getLocation() );
        setName( valueHolder.getName() );
        setAutoinventoryIdentifier( valueHolder.getAutoinventoryIdentifier() );
        setInstallPath( valueHolder.getInstallPath() );
        setServicesAutomanaged( valueHolder.getServicesAutomanaged() );
    }

    /**
     * @deprecated use isWasAutodiscovered()
     */
    public boolean getWasAutodiscovered() {
        return isWasAutodiscovered();
    }

    /**
     * @deprecated use isRuntimeAutodiscovery()
     */
    public boolean getRuntimeAutodiscovery() {
        return isRuntimeAutodiscovery();
    }

    /**
     * @deprecated use isAutodiscoveryZombie()
     */
    public boolean getAutodiscoveryZombie() {
        return isAutodiscoveryZombie();
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
}
