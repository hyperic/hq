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

package org.hyperic.hq.measurement.server.entity;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

import org.hyperic.hq.measurement.shared.MonitorableTypePK;
import org.hyperic.hq.measurement.shared.MonitorableTypeValue;

/** The connection details required to retrieve monitor values
 * @ejb:bean name="MonitorableType"
 *      jndi-name="ejb/measurement/MonitorableType"
 *      local-jndi-name="LocalMonitorableType"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.MonitorableTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(m) FROM MonitorableType as m WHERE m.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(m) FROM MonitorableType as m"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @ejb:value-object name="MonitorableType" match="*" instantiation="eager" cacheable="true" cacheDuration="120000"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_MONITORABLE_TYPE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class MonitorableTypeEJBImpl
    extends EntityEJB
    implements EntityBean {

    protected String getSequenceName() {
        return "EAM_MONITORABLE_TYPE_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /**
     * Id of this MonitorableType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getId();
    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /** Getter for property name.
     * @return Value of property name.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract String getName();
    /** Setter for property name.
     * @param name New value of property name.
     * @ejb:interface-method
     */
    public abstract void setName(String name);

    /** Getter for property AppdefType.
     * @return Value of property AppdefType.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="appdef_type"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract int getAppdefType();
    /** Setter for property AppdefType.
     * @param appdefType New value of property AppdefType.
     * @ejb:interface-method
     */
    public abstract void setAppdefType(int appdefType);

    /**
     * Plugin associated with this PlatformType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract java.lang.String getPlugin();
    /**
     * @ejb:interface-method
     *
     */
    public abstract void setPlugin(java.lang.String plugin);

    ///////////////////////////////////////
    // associations

    /**
     * Get the Value object for this measurement
     * @ejb:interface-method
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract MonitorableTypeValue getMonitorableTypeValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setMonitorableTypeValue(MonitorableTypeValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public MonitorableTypePK ejbCreate(String name, int appdefType,
                                       String plugin)
        throws CreateException {
        setId(getNextId());
        setName(name);
        setAppdefType(appdefType);
        setPlugin(plugin);
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(String name, int appdefType,
                              String plugin) {}

    /**
     * @see javax.ejb.EntityBean#ejbActivate()
     */
    public void ejbActivate() throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#ejbPassivate()
     */
    public void ejbPassivate() throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#ejbLoad()
     */
    public void ejbLoad() throws RemoteException {
    }

    /**
     * @see javax.ejb.EntityBean#ejbStore()
     */
    public void ejbStore() throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#ejbRemove()
     */
    public void ejbRemove() throws RemoteException, RemoveException {}

    /**
     * @see javax.ejb.EntityBean#setEntityContext()
     */
    public void setEntityContext(EntityContext ctx) throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#unsetEntityContext()
     */
    public void unsetEntityContext() throws RemoteException {}

} // end MonitorableTypeEJBImpl
