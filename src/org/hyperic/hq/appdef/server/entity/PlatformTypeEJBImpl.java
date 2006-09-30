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
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import java.util.Set;
import java.util.LinkedHashSet;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentLocal;
import org.hyperic.hq.appdef.shared.AgentLocalHome;
import org.hyperic.hq.appdef.shared.AgentPK;
import org.hyperic.hq.appdef.shared.AgentUtil;
import org.hyperic.hq.appdef.shared.PlatformLocal;
import org.hyperic.hq.appdef.shared.PlatformLocalHome;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformUtil;
import org.hyperic.hq.appdef.shared.PlatformValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the PlatformTypeBean implementaiton.
 * @ejb:bean name="PlatformType"
 *      jndi-name="ejb/appdef/PlatformType"
 *      local-jndi-name="LocalPlatformType"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *       
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(p) from PlatformType AS p"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(p) from PlatformType AS p ORDER BY p.sortName"
 * 
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.PlatformTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(p) from PlatformType AS p WHERE p.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.PlatformTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(p) from PlatformType AS p WHERE LCASE(p.name) = LCASE(?1)"
 *
 * @ejb:finder signature="java.util.Collection findByPlugin(java.lang.String plugin)"
 *      query="SELECT OBJECT(a) FROM PlatformType AS a WHERE a.plugin = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="PlatformType" match="*" instantiation="eager" extends="org.hyperic.hq.appdef.shared.AppdefResourceTypeValue" cacheable="true" cacheDuration="5000"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_PLATFORM_TYPE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class PlatformTypeEJBImpl extends AppdefEntityBean 
implements EntityBean {

    public final String SEQUENCE_NAME = "EAM_APPDEF_RESOURCE_TYPE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog("org.hyperic.hq.appdef.server.entity.PlatformTypeEJBImpl");

    public PlatformTypeEJBImpl() {
    }

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
     * Name of this PlatformType
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
     * Description of this PlatformType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     *
     */
    public abstract void setDescription(java.lang.String description);

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
     * Get the value object
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @deprecated THIS METHOD CAUSES DEADLOCKS. DO NOT USE IT
     * @jboss:read-only true
     */
    public abstract PlatformTypeValue getPlatformTypeValue();

    /**
     * Get the serverTypes which this PlatformType supports
     * @ejb:interface-method
     * @ejb:relation
     *      name="ServerType-PlatformType"
     *      role-name="many-PlatformTypes-have-many-ServerTypes"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:relation-table
     *      table-name="EAM_PLATFORM_SERVER_TYPE_MAP"
     *      create-table="false"
     *      remove-table="false"
     * @jboss:relation
     *      fk-column="SERVER_TYPE_ID"
     *      related-pk-field="id"
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.appdef.shared.ServerTypeValue"
     *      aggregate-name="ServerTypeValue"
     *      members="org.hyperic.hq.appdef.shared.ServerTypeLocal"
     *      members-name="ServerType"
     * @jboss:read-only true
     */
    public abstract java.util.Set getServerTypes();

    /**
     * Set the serverTypes which this platformType supports
     * @ejb:interface-method
     */
    public abstract void setServerTypes(java.util.Set serverTypes);

    /**
     * The create method using the data object
     * @param PlatformTypeValue
     * @return PlatformTypePK
     * @ejb:create-method
     */
    public PlatformTypePK ejbCreate(org.hyperic.hq.appdef.shared.PlatformTypeValue platformType) 
        throws CreateException {
            super.ejbCreate(ctx, SEQUENCE_NAME);
            setName(platformType.getName());
            setSortName(platformType.getName().toUpperCase());
            setPlugin(platformType.getPlugin());
            return null;
    }
    public void ejbPostCreate(PlatformTypeValue platformType)
    {
    }

    /**
     * Create a Platform Object with this as its type
     * @param PlatformValue
     * @return PlatformLocal
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PlatformLocal createPlatform(PlatformValue platform,
                                        AgentPK agent) 
        throws CreateException {
            try {
                // get the value object for this
                PlatformTypeValue ptValue = this.getPlatformTypeValueObject();
                platform.setPlatformType(ptValue);
                // now let's get the home interface
                PlatformLocalHome pLHome = PlatformUtil.getLocalHome();
                // and call create
                return pLHome.create(platform, agent);

            } catch (javax.naming.NamingException e) {
                log.error("Naming Exception in createPlatform.", e);
                throw new CreateException("Unable to create Platform for PlatformType: " + this.getName() + ": " + e.getMessage());
            }
    }

    /**
     * Create a Platform Object from an AIPlatform with this as its type
     * @param aiplatform The AIPlatform to create the platform from.
     * @param initialOwner the name of the user that will be the initial owner.
     * @return PlatformLocal
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PlatformLocal createPlatform(AIPlatformValue aiplatform, 
                                        String initialOwner) 
        throws CreateException {
            try {
                // let's get the home interface
                PlatformLocalHome pLHome = PlatformUtil.getLocalHome();
                // and call create
                return pLHome.create(aiplatform, initialOwner);

            } catch (javax.naming.NamingException e) {
                log.error("Naming Exception in createPlatform.", e);
                throw new CreateException("Unable to create Platform for PlatformType: " + this.getName() + ": " + e.getMessage());
            }
    }

    public void ejbActivate() throws RemoteException {}

    /**
     * Get a non-cmr'd PlatformTypeValue
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     * 
     */
    public PlatformTypeValue getPlatformTypeValueObject() {
        PlatformTypeValue vo = new PlatformTypeValue();
        vo.setSortName(getSortName());
        vo.setName(getName());
        vo.setDescription(getDescription());
        vo.setPlugin(getPlugin());
        vo.setId(((PlatformTypePK)this.getSelfLocal().getPrimaryKey()).getId());
        vo.setMTime(getMTime());
        vo.setCTime(getCTime());
        return vo;
    }
    
    /**
     * Get a snapshot of the ServerType CMR
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public Set getServerTypeSnapshot() {
        return new LinkedHashSet(getServerTypes());
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

    public void ejbRemove() throws RemoteException, RemoveException {}

}
