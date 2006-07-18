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

import java.rmi.RemoteException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.ServerTypeLocal;
import org.hyperic.hq.appdef.shared.ServerTypePK;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeLocal;
import org.hyperic.hq.appdef.shared.ServiceTypeUtil;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ServerTypeEJB implementaiton.
 * @ejb:bean name="ServerType"
 *      jndi-name="ejb/appdef/ServerType"
 *      local-jndi-name="LocalServerType"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(st) FROM ServerType AS st"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(st) FROM ServerType AS st order by st.sortName"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ServerTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(st) FROM ServerType AS st WHERE st.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.ServerTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(st) FROM ServerType AS st WHERE LCASE(st.name) = LCASE(?1)"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ServerTypeLocal findByNameAndPlugin(java.lang.String name, java.lang.String plugin)"
 *      query="SELECT OBJECT(st) FROM ServerType AS st WHERE st.name = ?1 AND st.plugin = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.ServerTypeLocal findByNameAndPlugin(java.lang.String name, java.lang.String plugin)"
 *      query="SELECT OBJECT(st) FROM ServerType AS st WHERE LCASE(st.name) = LCASE(?1) AND st.plugin = ?2"
 *
 * @ejb:finder signature="java.util.Collection findByPlugin(java.lang.String plugin)"
 *      query="SELECT OBJECT(st) FROM ServerType AS st WHERE st.plugin = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="ServerType" match="*" instantiation="eager" extends="org.hyperic.hq.appdef.shared.AppdefResourceTypeValue" cacheable="true" cacheDuration="5000"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_SERVER_TYPE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class ServerTypeEJBImpl extends AppdefEntityBean
implements EntityBean {

    public final String SEQUENCE_NAME = "EAM_SERVER_TYPE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(ServerTypeEJBImpl.class.getName());

    public ServerTypeEJBImpl() {
    }

    /**
     * Name of this ServerType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method
     *
     */
    public abstract void setName(java.lang.String name);

    /**
     * Is this server type a virtual server (artificial container)
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     * @jboss:column-name name="FVIRTUAL"
     */
    public abstract boolean getVirtual();
    
    /**
     * ejb:interface
     */
    public abstract void setVirtual(boolean virtual);
    
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
     * Description of this ServerTyoe
     * @ejb:persistent-field
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     */
    public abstract void setDescription(String desc);

    /**
     * Get the value object
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     * @deprecated THIS METHOD CAUSES DEADLOCKS. DO NOT USE IT
     */
    public abstract ServerTypeValue getServerTypeValue();

    /**
     * Plugin associated with this PlatformType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract java.lang.String getPlugin();
    /**
     * @ejb:interface-method
     *
     */
    public abstract void setPlugin(java.lang.String plugin);

    /**
     * Get all Servers of this ServerType
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @ejb:relation
     *      name="Server-ServerType"
     *      role-name="one-ServerType-has-many-Servers"
     * @jboss:read-only true
     */
    public abstract java.util.Set getServers();

    /**
     * Set all Servers of this ServerType.... 
     * @ejb:interface-method
     */
    public abstract void setServers(java.util.Set servers);

    /**
     * Get the ServiceTypes for this ServerType
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @ejb:relation
     *      name="ServerType-ServiceType"
     *      role-name="one-ServerType-has-many-ServiceTypes"
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.appdef.shared.ServiceTypeValue"
     *      aggregate-name="ServiceTypeValue"
     *      members="org.hyperic.hq.appdef.shared.ServiceTypeLocal"
     *      members-name="ServiceType"
     * @jboss:read-only true
     */
    public abstract java.util.Set getServiceTypes();

    /**
     * Set the ServiceTypes for this Server
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceTypes(java.util.Set serviceTypes);

    /**
     * Get the platformTypes which support this server type
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServerType-PlatformType"
     *      role-name="many-ServerTypes-have-many-PlatformTypes"
     * @ejb:transaction type="REQUIRED"
     * @jboss:relation-table
     *      table-name="EAM_PLATFORM_SERVER_TYPE_MAP"
     *      create-table="false"
     *      remove-table="false"
     * @jboss:relation
     *      fk-column="PLATFORM_TYPE_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract java.util.Set getPlatformTypes();

    /**
     * Set the platform types which this server type supports
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setPlatformTypes(java.util.Set platformTypes);

    /**
     * Create a service type for this server type   
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ServiceTypeLocal createServiceType(ServiceTypeValue stv)
        throws CreateException {

        try {
            // first create the service type
            ServiceTypeLocal stl = ServiceTypeUtil.getLocalHome().create(stv);
            // now set the server type to this
            stl.setServerType((ServerTypeLocal)this.getSelfLocal());
            return stl;
        } catch (NamingException e) {
            log.error("Failed to look up LocalHome in createServiceType", e);
            throw new CreateException("Failed to look up LocalHome in createServiceType: "
                + e.getMessage());
        }
    }
    
    /**
     * Get the non-cmr'd ServerTypeValue object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * 
     */
    public ServerTypeValue getServerTypeValueObject() {
        ServerTypeValue vo = new ServerTypeValue();
        vo.setName(getName());
        vo.setSortName(getSortName());
        vo.setDescription(getDescription());
        vo.setPlugin(getPlugin());
        vo.setId(((ServerTypePK)this.getSelfLocal().getPrimaryKey()).getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        vo.setVirtual(getVirtual());
        return vo;
    }
    
    /**
     * Get a snapshot of the ServiceTypeLocal collection
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * 
     */
    public Set getServiceTypeSnapshot() {
        return new LinkedHashSet(getServiceTypes());
    }
    
    /**
     * The create method using the data object
     * @param ServerTypeValue
     * @return ServerTypePK
     * @ejb:create-method
     * @ejb:transaction type="REQUIRED"
     */
    public ServerTypePK ejbCreate(org.hyperic.hq.appdef.shared.ServerTypeValue serverType) 
        throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, SEQUENCE_NAME);
            setName(serverType.getName());
            if (serverType.getName()!=null)
                setSortName(serverType.getName().toUpperCase());
            setDescription(serverType.getDescription());
            setPlugin(serverType.getPlugin());
            setVirtual(serverType.getVirtual());
            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            return null;
    }
    public void ejbPostCreate(ServerTypeValue serverType)
    {
        
    }

    public void ejbActivate() throws RemoteException
    {
    }

    public void ejbPassivate() throws RemoteException
    {
    }

    public void ejbLoad() throws RemoteException
    {
    }

    public void ejbStore() throws RemoteException
    {
        String name = getName();
        if (name != null) setSortName(name.toUpperCase());
        else setSortName(null);
    }

    public void ejbRemove() throws RemoteException, RemoveException
    {
    }

}
