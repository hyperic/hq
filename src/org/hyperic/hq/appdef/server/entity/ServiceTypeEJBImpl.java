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

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.hyperic.hq.appdef.shared.ServiceTypePK;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ServiceTypeEJB implementaiton.
 * @ejb:bean name="ServiceType"
 *      jndi-name="ejb/appdef/ServiceType"
 *      local-jndi-name="LocalServiceType"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(st) FROM ServiceType AS st"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(st) FROM ServiceType AS st ORDER BY st.sortName"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ServiceTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(st) FROM ServiceType AS st WHERE st.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.ServiceTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(st) FROM ServiceType AS st WHERE LCASE(st.name) = LCASE(?1)"
 * 
 * @ejb:finder signature="java.util.Collection findByPlugin(java.lang.String plugin)"
 *      query="SELECT OBJECT(st) FROM ServiceType AS st WHERE st.plugin = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByServerType_orderName_asc(int serverType)"
 *      query="SELECT OBJECT(st) FROM ServiceType AS st WHERE st.serverType.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findByServerType_orderName_asc(int serverType)"
 *      query="SELECT OBJECT(st) FROM ServiceType AS st WHERE st.serverType.id = ?1 ORDER BY st.name"
 *
 * @ejb:finder signature="java.util.Collection findVirtualServiceTypesByPlatform(int platformId)"
 *      query="SELECT OBJECT(svct) FROM ServiceType AS svct, Server AS svr WHERE svct.serverType = svr.serverType AND svr.serverType.virtual = true AND svr.platform.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findVirtualServiceTypesByPlatform(int platformId)"
 *      query="SELECT OBJECT(svct) FROM ServiceType AS svct, Server AS svr WHERE svct.serverType = svr.serverType AND svr.serverType.virtual = true AND svr.platform.id = ?1 ORDER BY svct.name"
 *
 * @ejb:value-object name="ServiceType" match="*" instantiation="eager" extends="org.hyperic.hq.appdef.shared.AppdefResourceTypeValue" cacheable="true" cacheDuration="5000"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_SERVICE_TYPE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class ServiceTypeEJBImpl extends AppdefEntityBean
implements EntityBean {

    public final String SEQUENCE_NAME = "EAM_SERVICE_TYPE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(ServiceTypeEJBImpl.class);

    public ServiceTypeEJBImpl() {
    }

    /**
     * Name of this ServiceType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     *
     */
    public abstract void setName(java.lang.String name);

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
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setDescription(String desc);

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
     * get the internal flag
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     * @jboss:column-name name="FINTERNAL"
     */
    public abstract boolean getIsInternal();
    /**
     * set the internal flag
     * @ejb:interface-method
     */
    public abstract void setIsInternal(boolean bool);

    /**
     * Get the value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract ServiceTypeValue getServiceTypeValue();

    /**
     * Set the value object
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceTypeValue(ServiceTypeValue val);

    /**
     * Get ServerType of this ServiceType
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServerType-ServiceType"
     *      role-name="one-ServiceType-has-one-ServerType"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="server_type_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.ServerTypeLocal getServerType();

    /**
     * Set the ServerType of this ServiceType.... 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setServerType(org.hyperic.hq.appdef.shared.ServerTypeLocal serverType);

    /**
     * Get the app services of this ServiceType
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServiceType-AppService"
     *      role-name="one-ServiceType-has-many-AppServices"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.util.Set getAppServices();

    /**
     * Set the app services of this ServiceType
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */

    public abstract void setAppServices(java.util.Set appServices);
    /**
     * Get the services of this ServicType
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServiceType-Service"
     *      role-name="one-ServiceType-has-many-Services"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.util.Set getServices();

    /**
     * Set the services of this ServiceType
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServices(java.util.Set services);

    /**
     * Get the ServiceClusters of this ServicType
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServiceType-ServiceCluster"
     *      role-name="one-ServiceType-has-many-ServiceClusters"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.util.Set getServiceClusters();

    /**
     * Set the services of this ServiceType
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceClusters(java.util.Set serviceClusters);    

    /**
     * Get the application types for this server type
     * @ejb:interface-method
     * @ejb:relation
     *      name="ApplicationType-ServiceType"
     *      role-name="many-ServiceTypes-have-many-ApplicationTypes"
     * @jboss:relation-table
     *      table-name="EAM_APP_TYPE_SERVICE_TYPE_MAP"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation
     *      fk-column="APPLICATION_TYPE_ID"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract java.util.Set getApplicationTypes();

    /**
     * Set the application types of this service types
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setApplicationTypes(java.util.Set appT);

    /**
     * The create method using the data object
     * @param ServiceTypeValue
     * @return ServiceTypePK
     * @ejb:create-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public ServiceTypePK ejbCreate(org.hyperic.hq.appdef.shared.ServiceTypeValue serviceType) 
        throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, SEQUENCE_NAME);
            setName(serviceType.getName());
            if (serviceType.getName()!=null)
                setSortName(serviceType.getName().toUpperCase());
            setDescription(serviceType.getDescription());
            setIsInternal(serviceType.getIsInternal());
            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            setPlugin(serviceType.getPlugin());
            return null;
    }
    public void ejbPostCreate(ServiceTypeValue serviceType) 
        throws CreateException {
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
