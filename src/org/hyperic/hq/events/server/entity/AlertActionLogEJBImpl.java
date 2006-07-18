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

import org.hyperic.hq.events.shared.AlertActionLogPK;
import org.hyperic.hq.events.shared.AlertActionLogValue;

/** The connection details required to retrieve monitor values
 * @ejb:bean name="AlertActionLog"
 *      jndi-name="ejb/events/AlertActionLog"
 *      local-jndi-name="LocalAlertActionLogn"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(a) FROM AlertActionLog AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="AlertActionLog" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_ALERT_ACTION_LOG"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class AlertActionLogEJBImpl
    extends EntityEJB
    implements EntityBean {

    protected String getSequenceName() {
        return "EAM_ALERT_ACTION_LOG_ID_SEQ";
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
     * @jboss:read-only true
     */
    public abstract Integer getId();

    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /** Getter for property detail.
     * @return Value of property detail.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getDetail();
    
    /** Setter for property detail.
     * @param detail New value of property detail.
     * @ejb:interface-method
     */
    public abstract void setDetail(String detail);

    /** Getter for property actionId.
     * @return Value of actionId.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="action_id"
     * @jboss:read-only true
     */
    public abstract Integer getActionId();

    /** Setter for property actionId.
     * @param actionId New value of property actionId.
     * @ejb:interface-method
     */
    public abstract void setActionId(Integer actionId);

    ///////////////////////////////////////
    // associations

    /**
     * Get the Value object for this alert
     * @ejb:interface-method
     */
    public abstract AlertActionLogValue getAlertActionLogValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setAlertActionLogValue(AlertActionLogValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public AlertActionLogPK ejbCreate(AlertActionLogValue val)
        throws CreateException
    {
        setAlertActionLogValue(val);
        setId(getNextId());                 // Now set the ID
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(AlertActionLogValue val) {}

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
