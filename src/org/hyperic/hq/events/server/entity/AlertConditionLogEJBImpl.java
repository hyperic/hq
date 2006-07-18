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
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.events.shared.AlertConditionLocal;
import org.hyperic.hq.events.shared.AlertConditionLocalHome;
import org.hyperic.hq.events.shared.AlertConditionLogPK;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertConditionPK;
import org.hyperic.hq.events.shared.AlertConditionUtil;

/** The connection details required to retrieve monitor values
 * @ejb:bean name="AlertConditionLog"
 *      jndi-name="ejb/events/AlertConditionLog"
 *      local-jndi-name="LocalAlertConditionLog"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(a) FROM AlertConditionLog AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="AlertConditionLog" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_ALERT_CONDITION_LOG"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class AlertConditionLogEJBImpl extends EntityEJB
    implements EntityBean {

    protected String getSequenceName() {
        return "EAM_ALERT_CONDITION_LOG_ID_SEQ";
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

    /** Getter for property value.
     * @return Value of property value.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getValue();
    
    /** Setter for property value.
     * @param value New value of property value.
     * @ejb:interface-method
     */
    public abstract void setValue(String value);

    ///////////////////////////////////////
    // associations

    /** Getter for property condition.
     * @return Value of property condition.
     * @ejb:interface-method
     * @ejb:relation
     *      name="AlertCondition-AlertConditionLog"
     *      role-name="one-alertConditionLog-has-one-alertCondition"
     *      target-ejb="AlertCondition"
     *      target-role-name="one-alertCondition-has-many-alertConditionLogs"
     *      target-multiple="yes"
     * @ejb:value-object match="*"
     *      aggregate="org.hyperic.hq.events.shared.AlertConditionValue"
     *      aggregate-name="Condition"
     * @jboss:relation
     *      fk-column="condition_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     *
     */
    public abstract AlertConditionLocal getCondition();

    /** Setter for property condition.
     * @param condition New value of property condition.
     * @ejb:interface-method
     */
    public abstract void setCondition(AlertConditionLocal cond);

    ///////////////////////////////////////
    // value getter/setter

    /**
     * Get the Value object for this alert
     * @ejb:interface-method
     */
    public abstract AlertConditionLogValue getAlertConditionLogValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setAlertConditionLogValue(AlertConditionLogValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public AlertConditionLogPK ejbCreate(AlertConditionLogValue val)
        throws CreateException
    {
        final int MAX_LOG_LENGTH = 250;
        if (val.getValue() != null) {
            if (val.getValue().length() < MAX_LOG_LENGTH)
                setValue(val.getValue());
            else
                setValue(val.getValue().substring(0, MAX_LOG_LENGTH));
        }
        
        setId(getNextId());                 // Now set the ID
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(AlertConditionLogValue val) 
        throws CreateException {
        try {
            AlertConditionPK pk =
                new AlertConditionPK(val.getCondition().getId());
            
            AlertConditionLocalHome home = AlertConditionUtil.getLocalHome();
            
            AlertConditionLocal relation = home.findByPrimaryKey(pk);
            setCondition(relation);
        } catch (NamingException e) {
            throw new CreateException(e.getMessage());
        } catch (FinderException e) {
            throw new CreateException(e.getMessage());
        }
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
