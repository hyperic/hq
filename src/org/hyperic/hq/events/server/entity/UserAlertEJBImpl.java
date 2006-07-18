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

package org.hyperic.hq.events.server.entity;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

import org.hyperic.hq.events.shared.AlertLocal;
import org.hyperic.hq.events.shared.UserAlertPK;

/** The bean that relates an alert to a user
 * @ejb:bean name="UserAlert"
 *      jndi-name="ejb/events/UserAlert"
 *      local-jndi-name="LocalUserAlert"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.List findAll()"
 *      query="SELECT OBJECT(a) FROM UserAlert AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findByUser(java.lang.Integer id)"
 *      query="SELECT OBJECT(a) FROM UserAlert AS a WHERE a.userId = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="UserAlert" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_USER_ALERT"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class UserAlertEJBImpl extends EntityEJB implements EntityBean {

    protected String getSequenceName() {
        return "EAM_USER_ALERT_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /**
     * Id of this Action
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS" 
     * @jboss:read-only true
     */
    public abstract Integer getId();

    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /** Getter for property userId.
     * @return Value of property userId.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS" 
     * @jboss:column-name name="user_id"
     * @jboss:read-only true
     */
    public abstract Integer getUserId();

    /** Setter for property userId.
     * @param userId New value of property userId.
     * @ejb:interface-method
     */
    public abstract void setUserId(Integer userId);

    /** Getter for property alert.
     * @return Value of property alert.
     * @ejb:interface-method
     * @ejb:relation
     *      name="User-Alert"
     *      role-name="one-userAlert-has-one-alert"
     *      cascade-delete="yes"
     *      target-ejb="Alert"
     *      target-role-name="one-alert-has-many-userAlerts"
     *      target-multiple="yes"
     * @ejb:value-object match="*"
     *      aggregate="org.hyperic.hq.events.shared.AlertValue"
     *      aggregate-name="Alert"
     * @ejb:transaction type="SUPPORTS" 
     * @jboss:relation
     *      fk-column="alert_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     *
     */
    public abstract AlertLocal getAlert();

    /** Setter for property alert.
     * @param alert New value of property alert.
     * @ejb:interface-method
     */
    public abstract void setAlert(AlertLocal alert);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public UserAlertPK ejbCreate(Integer uid, AlertLocal a)
        throws CreateException {
        // Set the ID
        setId(getNextId());

        // Set the user ID
        setUserId(uid);
        
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(Integer uid, AlertLocal a) {
        // Set the alert
        setAlert(a);
    }

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

} // end ActionEJBImpl
