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
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ArrayList;

/**
 * Pojo for hibernate hbm mapping file
 */
public class Server extends ServerBase
{
    private static Log log = LogFactory.getLog(Server.class);

    private Platform platform;
    private boolean runtimeAutodiscovery;
    private boolean wasAutodiscovered;
    private boolean autodiscoveryZombie;
    private ServerType serverType;
    private ConfigResponseDB configResponse;
    private Collection services;

    /**
     * default constructor
     */
    public Server()
    {
        super();
    }

    public Server(Integer id)
    {
        super();
        setId(id);
    }

    public Service createService(ServiceValue sv)
    {
        throw new UnsupportedOperationException(
            "use ServiceDAO.createService()");
    }

    // Property accessors
    public Platform getPlatform()
    {
        return this.platform;
    }

    public void setPlatform(Platform platform)
    {
        this.platform = platform;
    }

    public boolean isRuntimeAutodiscovery()
    {
        return this.runtimeAutodiscovery;
    }

    public void setRuntimeAutodiscovery(boolean runtimeAutodiscovery)
    {
        this.runtimeAutodiscovery = runtimeAutodiscovery;
    }

    public boolean isWasAutodiscovered()
    {
        return this.wasAutodiscovered;
    }

    public void setWasAutodiscovered(boolean wasAutodiscovered)
    {
        this.wasAutodiscovered = wasAutodiscovered;
    }

    public boolean isAutodiscoveryZombie()
    {
        return this.autodiscoveryZombie;
    }

    public void setAutodiscoveryZombie(boolean autodiscoveryZombie)
    {
        this.autodiscoveryZombie = autodiscoveryZombie;
    }

    public ServerType getServerType()
    {
        return this.serverType;
    }

    public void setServerType(ServerType serverType)
    {
        this.serverType = serverType;
    }

    public ConfigResponseDB getConfigResponse()
    {
        return this.configResponse;
    }

    public void setConfigResponse(ConfigResponseDB configResponse)
    {
        this.configResponse = configResponse;
    }

    public Collection getServices()
    {
        return this.services;
    }

    public void setServices(Collection services)
    {
        this.services = services;
    }

    private ServerPK pkey = new ServerPK();
    /**
     * legacy EJB primary key getter
     * @deprecated use getId() instead
     * @return
     */
    public ServerPK getPrimaryKey()
    {
        pkey.setId(getId());
        return pkey;
    }

    /**
     * @deprecated use getConfigResponse().getId()
     * @return
     */
    public Integer getConfigResponseId()
    {
        return configResponse != null ? configResponse.getId() : null;
    }


    private ServerLightValue serverLightValue = new ServerLightValue();
    /**
     * for legacy EJB DTO pattern
     * @deprecated use (this) Server object instead
     * @return
     */
    public ServerLightValue getServerLightValue()
    {
        serverLightValue.setSortName(getSortName());
        serverLightValue.setRuntimeAutodiscovery(getRuntimeAutodiscovery());
        serverLightValue.setWasAutodiscovered(getWasAutodiscovered());
        serverLightValue.setAutodiscoveryZombie(getAutodiscoveryZombie());
        serverLightValue.setConfigResponseId(getConfigResponseId());
        serverLightValue.setModifiedBy(getModifiedBy());
        serverLightValue.setOwner(getOwner());
        serverLightValue.setLocation(getLocation());
        serverLightValue.setName(getName());
        serverLightValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        serverLightValue.setInstallPath(getInstallPath());
        serverLightValue.setDescription(getDescription());
        serverLightValue.setServicesAutomanaged(getServicesAutomanaged());
        serverLightValue.setId(getId());
        serverLightValue.setMTime(getMTime());
        serverLightValue.setCTime(getCTime());
        if ( getServerType() != null ) {
            serverLightValue.setServerType(getServerType().getServerTypeValue());
        }
        else
            serverLightValue.setServerType( null );
        return serverLightValue;
    }

    private ServerValue serverValue = new ServerValue();
    /**
     * for legacy EJB DTO pattern
     * @deprecated use (this) Server object instead
     * @return
     */
    public ServerValue getServerValue()
    {
        serverValue.setSortName(getSortName());
        serverValue.setRuntimeAutodiscovery(getRuntimeAutodiscovery());
        serverValue.setWasAutodiscovered(getWasAutodiscovered());
        serverValue.setAutodiscoveryZombie(getAutodiscoveryZombie());
        serverValue.setConfigResponseId(getConfigResponseId());
        serverValue.setModifiedBy(getModifiedBy());
        serverValue.setOwner(getOwner());
        serverValue.setLocation(getLocation());
        serverValue.setName(getName());
        serverValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        serverValue.setInstallPath(getInstallPath());
        serverValue.setDescription(getDescription());
        serverValue.setServicesAutomanaged(getServicesAutomanaged());
        serverValue.setId(getId());
        serverValue.setMTime(getMTime());
        serverValue.setCTime(getCTime());
        serverValue.removeAllServiceValues();
        Collection services = getServices();
        if (services != null) {
            Iterator isv = services.iterator();
            while (isv.hasNext()){
                serverValue.addServiceValue(((Service)isv.next()).getServiceLightValue());
            }
        }
        serverValue.cleanServiceValue();
        if ( getServerType() != null ) {
            serverValue.setServerType(getServerType().getServerTypeValue());
        }
        else
            serverValue.setServerType( null );
        if ( getPlatform() != null ) {
            serverValue.setPlatform(getPlatform().getPlatformLightValue());
        }
        else
            serverValue.setPlatform( null );
        return serverValue;
    }

    /**
     * for legacy EJB DTO pattern
     * @deprecated use (this) Server object instead
     * @return
     */
    public ServerValue getServerValueObject()
    {
        ServerValue vo = new ServerValue();
        vo.setSortName(getSortName());
        vo.setDescription(getDescription());
        vo.setRuntimeAutodiscovery(getRuntimeAutodiscovery());
        vo.setWasAutodiscovered(getWasAutodiscovered());
        vo.setAutodiscoveryZombie(getAutodiscoveryZombie());
        vo.setModifiedBy(getModifiedBy());
        vo.setOwner(getOwner());
        vo.setLocation(getLocation());
        vo.setName(getName());
        vo.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        vo.setInstallPath(getInstallPath());
        vo.setServicesAutomanaged(getServicesAutomanaged());
        vo.setId(getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        vo.setConfigResponseId(getConfigResponseId());
        ServerType stype = getServerType();
        if ( stype != null ) {
            vo.setServerType( stype.getServerTypeValueObject() );
        }
        else
            vo.setServerType( null );
        Platform plat = getPlatform();
        if ( plat != null) {
            vo.setPlatform(plat.getPlatformLightValue());
        }
        return vo;
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
    private boolean isSupportedServiceType(ServiceTypeValue stv)
    {
        boolean REQUIRED = false;
        // Look up the ServiceTypeLocal
        // ServiceTypeLocal serviceType =
        //    ServiceTypeUtil.getLocalHome().findByPrimaryKey(
        //        stv.getPrimaryKey());
        // now check to see if it is included in the set of
        // supported services
        if(log.isDebugEnabled()) {
            log.debug("Checking to see if Server: " + getName()
                      + " supports service type: " + stv);
        }
        Collection suppServiceTypes = getSupportedServiceTypes();
        REQUIRED = suppServiceTypes.contains(stv.getName());
        if(log.isDebugEnabled()) {
            log.debug("isSupportedServiceType returning: " + REQUIRED);
        }
        return REQUIRED;
    }

    /**
     * legacy EJB method
     * @param valueHolder
     */
    public void updateServer(ServerValue valueHolder)
    {
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
     * @return
     */
    public boolean getWasAutodiscovered()
    {
        return isWasAutodiscovered();
    }

    /**
     * @deprecated use isRuntimeAutodiscovery()
     * @return
     */
    public boolean getRuntimeAutodiscovery()
    {
        return isRuntimeAutodiscovery();
    }

    /**
     * @deprecated use isAutodiscoveryZombie()
     * @return
     */
    public boolean getAutodiscoveryZombie()
    {
        return isAutodiscoveryZombie();
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Server) || !super.equals(obj)) {
            return false;
        }
        Server o = (Server)obj;
        return
            ((platform == o.getPlatform()) ||
             (platform!=null && o.getPlatform()!=null &&
              platform.equals(o.getPlatform())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (platform != null ? platform.hashCode() : 0);

        return result;
    }
}
