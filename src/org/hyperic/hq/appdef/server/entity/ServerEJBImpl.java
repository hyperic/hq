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

package org.hyperic.hq.appdef.server.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.dao.ConfigResponseDAO;
import org.hyperic.hibernate.dao.ServiceDAO;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Service;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.hq.appdef.shared.PlatformLocal;
import org.hyperic.hq.appdef.shared.PlatformLocalHome;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformUtil;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.ServerTypeLocal;
import org.hyperic.hq.appdef.shared.ServerTypeLocalHome;
import org.hyperic.hq.appdef.shared.ServerTypePK;
import org.hyperic.hq.appdef.shared.ServerTypeUtil;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceLocal;
import org.hyperic.hq.appdef.shared.ServiceLocalHome;
import org.hyperic.hq.appdef.shared.ServiceTypeLocal;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceUtil;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
/**
 * This is the ServerEJB implementation.
 * @ejb:bean name="Server"
 *      jndi-name="ejb/appdef/Server"
 *      local-jndi-name="LocalServer"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:interface local-extends="org.hyperic.hq.appdef.shared.AppdefResourceLocal"
 * 
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(s) FROM Server AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.serverType.virtual = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.serverType.virtual = false ORDER BY s.sortName"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.serverType.virtual = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.serverType.virtual = false ORDER BY s.sortName DESC"
 *
 * @ejb:finder signature="java.util.Collection findByType(java.lang.Integer sTypeId)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.serverType.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.List findByPlatform_orderName(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE AND s.platform.id = ?1"
 * @jboss:query signature="java.util.List findByPlatform_orderName(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.platform.id = ?1 ORDER BY s.sortName"
 *
 *  @ejb:finder signature="java.util.List findByPlatform_orderName(java.lang.Integer id, java.lang.Boolean virtual)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE AND s.platform.id = ?1 AND s.serverType.virtual = ?2"
 * @jboss:query signature="java.util.List findByPlatform_orderName(java.lang.Integer id, java.lang.Boolean virtual)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.platform.id = ?1 AND s.serverType.virtual = ?2 ORDER BY s.sortName"
 *
 *
 * @ejb:finder signature="java.util.List findByPlatformAndType_orderName(java.lang.Integer id, java.lang.Integer tid)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.platform.id = ?1 AND s.serverType.id = ?2"
 * @jboss:query signature="java.util.List findByPlatformAndType_orderName(java.lang.Integer id, java.lang.Integer tid)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.platform.id = ?1 AND s.serverType.id = ?2 ORDER BY s.sortName"
 *
 * @ejb:finder signature="java.util.List findByPlatformAndType_orderName(java.lang.Integer id, java.lang.Integer tid, java.lang.Boolean isVirtual)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.platform.id = ?1 AND s.serverType.id = ?2"
 * @jboss:query signature="java.util.List findByPlatformAndType_orderName(java.lang.Integer id, java.lang.Integer tid, java.lang.Boolean isVirtual)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.platform.id = ?1 AND s.serverType.id = ?2 AND s.serverType.virtual = ?3 ORDER BY s.sortName"
 *
 * @ejb:finder signature="java.util.List findByName(java.lang.String name)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE s.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByName(java.lang.String name)"
 *      query="SELECT OBJECT(s) FROM Server AS s WHERE LCASE(s.name) = LCASE(?1)"
 *
 * @ejb:value-object name="ServerLight" match="light" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue"
 *      cacheable="true" cacheDuration="600000"
 * @ejb:value-object name="Server" match="*" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_SERVER"
 * @jboss:create-table false
 * @jboss:remove-table false
 *      
 */
public abstract class ServerEJBImpl 
    extends ServerBaseBean implements EntityBean {

    private final String ctx = ServerEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_APPDEF_RESOURCE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(ServerEJBImpl.class.getName());
    
    private final int CACHE_TIMEOUT = 15000;
    private ServerValue cachedVO = null;
    private long cacheCreateTime;
    
    public ServerEJBImpl() {}

    /**
     * Sort name of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="SORT_NAME"
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getSortName();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setSortName (java.lang.String sortName);

    /**
     * Is runtime-autodiscovery enabled for this server?
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract boolean getRuntimeAutodiscovery();
    /**
     * @ejb:interface-method
     */
    public abstract void setRuntimeAutodiscovery(boolean runtimeDiscovery);

    
    /**
     * @return true if this server was created as a result of a runtime
     * autodiscovery scan.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract boolean getWasAutodiscovered();
    /**
     * @ejb:interface-method
     */
    public abstract void setWasAutodiscovered(boolean wasAutodiscovered);

    /**
     * Is this Server an autodiscovery zombie?  Zombies
     * are servers that still exist in appdef, but are marked
     * zombies because runtime autodiscovery no longer sees
     * them in its scans
     * @ejb:persistent-field
     * @jboss:column-name name="AUTODISCOVERY_ZOMBIE"
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="light"
     * @jboss:read-only true
     */
    public abstract boolean getAutodiscoveryZombie();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAutodiscoveryZombie(boolean z);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="CONFIG_RESPONSE_ID"
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.Integer getConfigResponseId();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setConfigResponseId (java.lang.Integer crif);

    /** 
     * Get the value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract ServerValue getServerValue();

    /** 
     * Get the light value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract ServerLightValue getServerLightValue();

    /**
     * Set the value object. This method does *NOT* update any of the CMR's
     * that the value object may contain. This is for speed/locking reasons.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public void updateServer( ServerValue valueHolder ) {
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
         setMTime( valueHolder.getMTime() );
         setCTime( valueHolder.getCTime() );
    }
                    
    /**
     * modified by of this server
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="MODIFIED_BY"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getModifiedBy();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setModifiedBy(java.lang.String modifier);

    /**
     * owner of this server
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="OWNER"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getOwner();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setOwner(java.lang.String owner);

    /**
     * Location of this server
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="LOCATION"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getLocation();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setLocation(java.lang.String location);

    /**
     * Get the ServerType of this Server
     * @ejb:interface-method 
     * @ejb:relation
     *      name="Server-ServerType"
     *      role-name="one-Server-has-one-ServerType"
     * @ejb:value-object match="*"
     *      compose="org.hyperic.hq.appdef.shared.ServerTypeValue"
     *      compose-name="ServerType"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="server_type_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.ServerTypeLocal getServerType(); 

    /**
     * Set the ServerType of this Server
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setServerType(org.hyperic.hq.appdef.shared.ServerTypeLocal Server);

    /**
     * Get the Platform of this Server
     * @ejb:interface-method
     * @ejb:relation
     *      name="Platform-Server"
     *      role-name="one-Server-has-one-Platform"
     *      cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object
     *      compose="org.hyperic.hq.appdef.shared.PlatformLightValue"
     *      compose-name="Platform"
     * @jboss:relation
     *      fk-column="platform_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.PlatformLocal getPlatform();
    
    /**
     * Set the Platform for this server
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setPlatform(org.hyperic.hq.appdef.shared.PlatformLocal platform);

    /**
     * Get the Services for this Server
     * @ejb:interface-method
     * @ejb:relation
     *      name="Server-Service"
     *      role-name="one-Server-has-many-Services"
     * @ejb:transaction type="SUPPORTS"
     *
     * @ejb:value-object
     *      type="Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.appdef.shared.ServiceLightValue"
     *      aggregate-name="ServiceValue"
     *      members="org.hyperic.hq.appdef.shared.ServiceLocal"
     *      members-name="Service"
     * @jboss:read-only true
     */
    public abstract java.util.Set getServices();

    /**
     * Set the Services for this Server
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setServices(java.util.Set svcs);

    /**
     * Get the supported service types for this server instance
     * @return Collection - a collection of ServiceTypeValues
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     * @jboss:read-only true
     */
    public Collection getSupportedServiceTypes() throws FinderException {
        // first get our service type
        ServerTypeLocal myType = this.getServerType();
        // now get the service types
        Set serviceTypes = myType.getServiceTypes();
        // now turn em into beans
        Collection suppSvcTypes = new ArrayList();
        Iterator it = serviceTypes.iterator();
        while(it.hasNext()) {
            ServiceTypeLocal svcType = (ServiceTypeLocal)it.next();
            suppSvcTypes.add(svcType.getServiceTypeValue());
        }
        return suppSvcTypes;
    }

    /**
     * The create method
     * @ejb:create-method
     * @ejb:transaction type="REQUIRED"
     */
    public ServerPK ejbCreate(org.hyperic.hq.appdef.shared.ServerValue server) 
        throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, SEQUENCE_NAME,
                            server.getInstallPath(),
                            server.getAutoinventoryIdentifier(),
                            server.getServicesAutomanaged(),
                            server.getName());
            if (server.getName()!=null)
                setSortName(server.getName().toUpperCase());
            setDescription(server.getDescription());
            setRuntimeAutodiscovery(server.getRuntimeAutodiscovery());
            setWasAutodiscovered(server.getWasAutodiscovered());
            setAutodiscoveryZombie(false);
            setOwner(server.getOwner());
            setLocation(server.getLocation());
            setModifiedBy(server.getModifiedBy());
            if(log.isDebugEnabled()) {
                log.debug("Finished ejbCreate");
            }
            return null;
    }

    public void ejbPostCreate(org.hyperic.hq.appdef.shared.ServerValue server) 
        throws CreateException {
         try {
             if(log.isDebugEnabled()) {
                log.debug("Begin ejbPostCreate");
             }
             // now we lookup the platform so we can set it
             PlatformLocalHome pHome = PlatformUtil.getLocalHome();
             PlatformLightValue pvalue = server.getPlatform();
             PlatformPK platPK = pvalue.getPrimaryKey();
             PlatformLocal pLocal= pHome.findByPrimaryKey(platPK);
             setPlatform(pLocal);

            // now look up the serverType so we can set it

            ServerTypeLocalHome stHome = ServerTypeUtil.getLocalHome();
            ServerTypeValue stv = server.getServerType();
            ServerTypePK typePK = stv.getPrimaryKey();
            ServerTypeLocal stype = stHome.findByPrimaryKey(typePK);
            setServerType(stype);

            // Setup config response entries
             ConfigResponseDAO dao = DAOFactory.getDAOFactory()
                 .getConfigResponseDAO();
             ConfigResponseDB cLocal = new ConfigResponseDB();
             dao.save(cLocal);
            // TODO: get rid of this when hibernate migration is complete
             dao.getSession().flush();
             
             setConfigResponseId(cLocal.getId());

            if(log.isDebugEnabled()) {
                log.debug("Set Platform to: " + pLocal);
                log.debug("Set ServerType to: " + stype);
                log.debug("Completed ejbPostCreate");
             }
         } catch (NamingException e) {
                log.error("Unable to get LocalHome in ejbPostCreate", e);
                throw new CreateException("Unable to find a LocalHome");
         } catch (FinderException e) {
                log.error("Unable to find dependent object", e);
                throw new CreateException("Unable to find dependent object: "   
                    + e.getMessage());

         }

    }
    // HELPER METHODS

    /**
     * Create a Service instance hosted by this server.
     * @ejb:interface-method
     * @ejb:tranasaction type="REQUIRED"
     * @param ServiceValue - the value object
     * @throws CreateException -
     * @throws ValidationException
     */
    public Service createService(ServiceValue sv)
        throws CreateException, ValidationException {
        // validate the service
        this.validateNewService(sv);
        // get the Service home
        ServiceDAO sLHome =
            DAOFactory.getDAOFactory().getServiceDAO();                // create it
        return sLHome.create(sv, this.getPK());
    }

    /**
     * Validate a new service value object to be hosted by this server
     * @param ServiceValue
     * @exception ValidationException 
     */
    /**
     * @param sv
     * @throws ValidationException
     */
    private void validateNewService(ServiceValue sv) throws ValidationException {
        String msg = null;
        // first we check that the server includes the specified type
        if(!this.isSupportedServiceType(sv.getServiceType())) {
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
     * @param ServiceTypeValue - the type to check
     * @return boolean - true if its supported, false otherwise
     */
    private boolean isSupportedServiceType(ServiceTypeValue stv) {
        boolean REQUIRED = false;
        try {
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
            Collection suppServiceTypes = this.getSupportedServiceTypes();
            REQUIRED = suppServiceTypes.contains(stv);        
        } catch (javax.ejb.FinderException e) {
            log.error("Unable to find ServiceType: " + stv);
        }
        if(log.isDebugEnabled()) {
            log.debug("isSupportedServiceType returning: " + REQUIRED);
        }
        return REQUIRED;
    }



    /**     
     * get the primary key for this server. no idea why this isnt
     * provided automatically
     */
    private ServerPK getPK() {
        return new ServerPK(this.getId());
    }

    /**
     * @return the AppdefResourceType
     */
    public javax.ejb.EJBLocalObject getAppdefResourceType()
    {
        return this.getServerType();
    }
    
    /** Get an upcasted reference to our resource type value.
     * @return the "type value" value object upcasted to its
     *         abstract base class for use in agnostic context. */
    public AppdefResourceTypeValue getAppdefResourceTypeValue () {
        return (AppdefResourceTypeValue) getServerType()
            .getServerTypeValueObject();
    }

    /**
     * Compare this entity bean to a value object
     * @return true if the service value matches this entity
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public boolean matchesValueObject(ServerValue obj) {
        boolean matches = true;
        matches = super.matchesValueObject(obj) && 
            (this.getName() != null ? this.getName().equals(obj.getName()) 
                : (obj.getName() == null)) &&
            (this.getDescription() != null ? 
                this.getDescription().equals(obj.getDescription()) 
                : (obj.getDescription() == null)) &&
            (this.getLocation() != null ? 
                this.getLocation().equals(obj.getLocation())
                : (obj.getLocation() == null)) &&
            (this.getOwner() != null ? this.getOwner().equals(obj.getOwner())
                : (obj.getOwner() == null)) &&
            (this.getRuntimeAutodiscovery() == obj.getRuntimeAutodiscovery()) &&    
            (this.getInstallPath().equals(obj.getInstallPath())) &&
            (this.getAutoinventoryIdentifier() != null ?
                this.getAutoinventoryIdentifier().equals(
                                obj.getAutoinventoryIdentifier())
                : (obj.getAutoinventoryIdentifier() == null));
                                
        return matches;
    }
    // END HELPER METHODS

    /**
     * Get the light server value sans-CMR's
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public ServerValue getServerValueObject() {
        if( cachedVO == null )
        {
            cachedVO = new ServerValue();
        } else if ((System.currentTimeMillis() - cacheCreateTime) < 
            CACHE_TIMEOUT){
            return cachedVO;
        }
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
        vo.setId(((ServerPK)this.getSelfLocal().getPrimaryKey()).getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        vo.setConfigResponseId(getConfigResponseId());
        ServerTypeLocal stype = getServerType();
        if ( stype != null )
            vo.setServerType( stype.getServerTypeValueObject() );
        else
            vo.setServerType( null );
        PlatformLocal plat = getPlatform();    
        if ( plat != null) {
            vo.setPlatform(plat.getPlatformLightValue());            
        }
        this.cacheCreateTime = System.currentTimeMillis();            
        return vo;            
    }
    
    /**
     * Get a snapshot of the serviceLocals associated with this server
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public Set getServiceSnapshot() {
        return new LinkedHashSet(getServices());
    }

    // EJB SUPPORT METHODS

    public void ejbActivate() throws RemoteException
    {
    }

    public void ejbPassivate() throws RemoteException
    {
        this.cachedVO = null;
    }

    public void ejbLoad() throws RemoteException
    {
        this.cachedVO = null;
    }

    public void ejbStore() throws RemoteException
    {
        this.cachedVO = null;
        String name = getName();
        if (name != null) setSortName(name.toUpperCase());
        else setSortName(null);
    }

    public void ejbRemove() throws RemoteException, RemoveException
    {
    }

    // END EJB SUPPORT METHODS

}
