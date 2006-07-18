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

import org.hyperic.hq.events.shared.AlertConditionPK;
import org.hyperic.hq.events.shared.AlertConditionValue;

/** The details for each alert condition, used to communicate with UI
 * @ejb:bean name="AlertCondition"
 *      jndi-name="ejb/events/AlertCondition"
 *      local-jndi-name="LocalAlertCondition"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(a) FROM AlertCondition AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.events.shared.AlertConditionLocal findByTriggerId(java.lang.Integer tid)"
 *      query="SELECT OBJECT(a) FROM AlertCondition AS a WHERE a.triggerId = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="AlertCondition" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_ALERT_CONDITION"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AlertConditionEJBImpl
    extends EntityEJB
    implements EntityBean {

    protected String getSequenceName() {
        return "EAM_ALERT_CONDITION_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /**
     * Id of this AlertCondition
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

    /** Getter for property type.
     * @return Value of type.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract int getType();

    /** Setter for property type.
     * @param type New value of property type.
     * @ejb:interface-method
     */
    public abstract void setType(int type);

    /** Getter for property required.
     * @return Value of required.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract boolean getRequired();

    /** Setter for property required.
     * @param required New value of property required.
     * @ejb:interface-method
     */
    public abstract void setRequired(boolean required);

    /** Getter for property measurementId.
     * @return Value of measurementId.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="measurement_id"
     * @jboss:read-only true
     */
    public abstract int getMeasurementId();

    /** Setter for property measurementId.
     * @param measurementId New value of property measurementId.
     * @ejb:interface-method
     */
    public abstract void setMeasurementId(int measurementId);

    /** Getter for property name.
     * @return Value of name.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getName();

    /** Setter for property name.
     * @param name New value of property name.
     * @ejb:interface-method
     */
    public abstract void setName(String name);

    /** Getter for property comparator.
     * @return Value of comparator.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getComparator();

    /** Setter for property comparator.
     * @param comparator New value of property comparator.
     * @ejb:interface-method
     */
    public abstract void setComparator(String comparator);

    /** Getter for property threshold.
     * @return Value of threshold.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract double getThreshold();

    /** Setter for property threshold.
     * @param threshold New value of property threshold.
     * @ejb:interface-method
     */
    public abstract void setThreshold(double threshold);

    /** Getter for property option (or status).
     * @return Value of option.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="option_status"
     * @jboss:read-only true
     */
    public abstract String getOption();

    /** Setter for property option (or status).
     * @param option New value of property option.
     * @ejb:interface-method
     */
    public abstract void setOption(String option);

    /** Getter for property triggerId.
     * @return Value of triggerId.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="trigger_id"
     * @jboss:read-only true
     */
    public abstract Integer getTriggerId();

    /** Setter for property triggerId.
     * @param triggerId New value of property triggerId.
     * @ejb:interface-method
     */
    public abstract void setTriggerId(Integer triggerId);

    ///////////////////////////////////////
    // associations

    /**
     * Get the Value object for this action
     * @ejb:interface-method
     */
    public abstract AlertConditionValue getAlertConditionValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setAlertConditionValue(AlertConditionValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public AlertConditionPK ejbCreate(AlertConditionValue val)
        throws CreateException 
    {
        setAlertConditionValue(val);
        setId(getNextId());
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(AlertConditionValue val) {}

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

} // end AlertConditionEJBImpl
