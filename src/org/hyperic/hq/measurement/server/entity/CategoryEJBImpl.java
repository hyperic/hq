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

import org.hyperic.hq.measurement.shared.CategoryPK;
import org.hyperic.hq.measurement.shared.CategoryValue;

/** The connection details required to retrieve monitor values
 * @ejb:bean name="Category"
 *      jndi-name="ejb/measurement/Category"
 *      local-jndi-name="LocalCategory"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.CategoryLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(m) FROM Category as m WHERE m.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="Category" match="*" instantiation="eager" cacheable="true" cacheDuration="240000"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_MEASUREMENT_CAT"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class CategoryEJBImpl extends EntityEJB implements EntityBean {

    protected String getSequenceName() {
        return "EAM_MEASUREMENT_CAT_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /**
     * Id of this PlatformType
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

    ///////////////////////////////////////
    // associations

    /**
     * Get the Value object for this measurement
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract CategoryValue getCategoryValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setCategoryValue(CategoryValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public CategoryPK ejbCreate(String name) throws CreateException {
        setId(getNextId());
        setName(name);
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(String name) {}

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
    public void ejbLoad() throws RemoteException {}

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

} // end CategoryEJBImpl
