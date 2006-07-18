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

import org.hyperic.hq.events.shared.ActionPK;
import org.hyperic.hq.events.shared.ActionValue;

/** Storage of alert definition action configurations in binary data
 * @ejb:bean name="Action"
 *      jndi-name="ejb/events/Action"
 *      local-jndi-name="LocalAction"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(a) FROM Action AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByAlertId(java.lang.Integer aid)"
 *      query="SELECT OBJECT(a) FROM Action AS a, Alert AS al, IN (al.actionLogs) AS l
               WHERE a.id = l.actionId AND al.id = ?1"
 *
 * @ejb:finder signature="java.util.Collection findChildrenActions(java.lang.Integer aid)"
 *      query="SELECT OBJECT(a) FROM Action AS a WHERE a.parentId = ?1"
 *
 * @ejb:value-object name="Action" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_ACTION"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class ActionEJBImpl
    extends EntityEJB
    implements EntityBean {

    protected String getSequenceName() {
        return "EAM_ACTION_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /**
     * Id of this Action
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract Integer getId();

    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /** Getter for property classname.
     * @return Value of classname.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getClassname();

    /** Setter for property classname.
     * @param classname New value of property classname.
     * @ejb:interface-method
     */
    public abstract void setClassname(String name);

    /** Getter for property config.
     * @return Value of config.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract byte[] getConfig();

    /** Setter for property config.
     * @param config New value of property config.
     * @ejb:interface-method
     */
    public abstract void setConfig(byte[] config);

    /** Parent ID of this Action (if any)
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:column-name name="parent_id"
     * @jboss:read-only true
     */
    public abstract Integer getParentId();

    /** Setter for property parentId.
     *
     * @ejb:interface-method
     */
    public abstract void setParentId(Integer parentId);

    ///////////////////////////////////////
    // associations

    /**
     * Get the Value object for this action
     * @ejb:interface-method
     */
    public abstract ActionValue getActionValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setActionValue(ActionValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public ActionPK ejbCreate(ActionValue val) throws CreateException 
    {
        setActionValue(val);
        setId(getNextId());
        
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(ActionValue val) {}

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
