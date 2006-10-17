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

import javax.ejb.*;
import java.rmi.RemoteException;
import java.util.*;
import org.hyperic.hq.appdef.shared.ApplicationTypeValue;
import org.hyperic.hq.appdef.shared.ApplicationTypePK;
import org.hyperic.hq.appdef.shared.ServiceTypePK;
import org.hyperic.hq.appdef.shared.ServiceTypeLocal;
import org.hyperic.hq.appdef.shared.ServiceTypeUtil;
import org.hyperic.hq.appdef.ServiceType;
import org.hyperic.dao.DAOFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ApplicationTypeEJB  implementaiton.
 * @ejb:bean name="ApplicationType"
 *      jndi-name="ejb/appdef/ApplicationType"
 *      local-jndi-name="LocalApplicationType"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(appType) FROM ApplicationType AS appType"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ApplicationTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(appType) FROM ApplicationType AS appType WHERE appType.name = ?1"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.ApplicationTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(appType) FROM ApplicationType AS appType WHERE LCASE(appType.name) = LCASE(?1)"
 *
 * @ejb:value-object name="ApplicationType" match="*" instantiation="eager" extends="org.hyperic.hq.appdef.shared.AppdefResourceTypeValue"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_APPLICATION_TYPE"
 * @jboss:create-table false
 * @jboss:remove-table false
 *      
 */

public abstract class ApplicationTypeEJBImpl extends AppdefEntityBean
implements EntityBean {

    public final String ctx = PlatformTypeEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_APPDEF_RESOURCETYPE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog("org.hyperic.hq.appdef.server.entity.ApplicationTypeEJBImpl");

    public ApplicationTypeEJBImpl() {
    }

    /**
     * Name of this ApplicationType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method
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
     * Description of this ApplicationType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     */
    public abstract void setDescription(java.lang.String desc);

    /**
     * Get the value object
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract ApplicationTypeValue getApplicationTypeValue();

    /**
     * The create method
     * @ejb:create-method
     */
    public ApplicationTypePK ejbCreate(org.hyperic.hq.appdef.shared.ApplicationTypeValue appType)
        throws CreateException {
            super.ejbCreate(ctx, SEQUENCE_NAME);
            setName(appType.getName());
            if (appType.getName()!=null)
                setSortName(appType.getName().toUpperCase());
            setDescription(appType.getDescription());
            return null;
    }

    public void ejbPostCreate(org.hyperic.hq.appdef.shared.ApplicationTypeValue appType)
    {
    }

    /**
     * Get the server types for this app type
     * @ejb:interface-method
     * @ejb:relation
     *      name="ApplicationType-ServiceType"
     *      role-name="many-ApplicationTypes-have-many-ServiceTypes"
     * @jboss:relation-table
     *      table-name="EAM_APP_TYPE_SERVICE_TYPE_MAP"
     *      create-table="false"
     *      remove-table="false"
     * @jboss:relation
     *      fk-column="SERVICE_TYPE_ID"
     *      related-pk-field="id"
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract java.util.Set getServiceTypes();

    /**
     * Set the server types for this app type
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceTypes(java.util.Set stypes);

    /**
     * Check to see if a service type is supported by this application type
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     * @return boolean - true if its supported, false otherwise.. including 
     * cases where the service type can not be found.
     */
    public boolean supportsServiceType(ServiceTypePK stPK) {
        boolean supports = false;
        try {
            // first look up the ServiceTypeLocal
            ServiceType serviceType =
                DAOFactory.getDAOFactory()
                    .getServiceTypeDAO().findById(stPK.getId());
            supports = this.getServiceTypes().contains(serviceType);
        } catch (Exception e) {
            log.error("Caught exception while checking supportsServiceType: " + stPK, e);
        }
        return supports;
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
