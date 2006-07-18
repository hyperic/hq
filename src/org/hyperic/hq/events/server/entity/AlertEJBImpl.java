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

import org.hyperic.hq.events.shared.AlertPK;
import org.hyperic.hq.events.shared.AlertValue;

/** The connection details required to retrieve monitor values
 * @ejb:bean name="Alert"
 *      jndi-name="ejb/events/Alert"
 *      local-jndi-name="LocalAlert"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.List findAll()"
 *      query="SELECT OBJECT(a) FROM Alert AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findByAlertDefinition(java.lang.Integer id)"
 *      query="SELECT OBJECT(a) FROM Alert AS a WHERE a.alertDefId = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.events.shared.AlertLocal findByAlertDefinitionAndCtime(java.lang.Integer id, long ctime)"
 *      query="SELECT OBJECT(a) FROM Alert AS a WHERE a.alertDefId = ?1 AND a.ctime = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.List findByAppdefEntity(int type, int id)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad WHERE ad.appdefType = ?1 AND ad.appdefId = ?2 AND a.alertDefId = ad.id"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAppdefEntity(int type, int id)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad
                 WHERE ad.appdefType = ?1 AND ad.appdefId = ?2 AND
                       a.alertDefId = ad.id ORDER BY a.ctime DESC"
 *
 * @ejb:finder signature="java.util.List findByAppdefEntityInRange(int type, int id, long begin, long end)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad WHERE ad.appdefType = ?1 AND ad.appdefId = ?2 AND a.alertDefId = ad.id AND a.ctime BETWEEN ?3 AND ?4"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAppdefEntityInRange(int type, int id, long begin, long end)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad
                 WHERE ad.appdefType = ?1 AND ad.appdefId = ?2 AND a.ctime BETWEEN ?3 AND ?4 AND
                       a.alertDefId = ad.id ORDER BY a.ctime DESC"
 *
 * @ejb:finder signature="java.util.List findByAppdefEntitySortByAlertDef(int type, int id)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad WHERE ad.appdefType = ?1 AND ad.appdefId = ?2 AND a.alertDefId = ad.id"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAppdefEntitySortByAlertDef(int type, int id)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad
                 WHERE ad.appdefType = ?1 AND ad.appdefId = ?2 AND
                       a.alertDefId = ad.id ORDER BY ad.name DESC"
 *
 * @ejb:finder signature="java.util.List findByAppdefEntityInRangeSortByAlertDef(int type, int id, long begin, long end)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad WHERE ad.appdefType = ?1 AND ad.appdefId = ?2 AND a.alertDefId = ad.id AND a.ctime BETWEEN ?3 AND ?4"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAppdefEntityInRangeSortByAlertDef(int type, int id, long begin, long end)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad
                 WHERE ad.appdefType = ?1 AND ad.appdefId = ?2 AND a.ctime BETWEEN ?3 AND ?4 AND
                       a.alertDefId = ad.id ORDER BY ad.name DESC"
 *
 * @ejb:finder signature="java.util.List findByCreateTime(long begin, long end)"
 *      query="SELECT OBJECT(a) FROM Alert AS a WHERE a.ctime BETWEEN ?1 AND ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByCreateTime(long begin, long end)"
 *      query="SELECT OBJECT(a) FROM Alert AS a
                 WHERE a.ctime BETWEEN ?1 AND ?2 ORDER BY a.ctime DESC"
 *
 * @ejb:finder signature="java.util.List findByCreateTimeAndPriority(long begin, long end, int priority)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad WHERE a.ctime BETWEEN ?1 AND ?2 AND a.alertDefId = ad.id AND (ad.priority = ?3 OR ad.priority > ?3)"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByCreateTimeAndPriority(long begin, long end, int priority)"
 *      query="SELECT OBJECT(a) FROM Alert AS a, AlertDefinition AS ad
                 WHERE a.ctime BETWEEN ?1 AND ?2 AND a.alertDefId = ad.id AND (ad.priority = ?3 OR ad.priority > ?3) ORDER BY a.ctime DESC"
 *
 * @ejb:finder signature="java.util.List findByAlertDefinitionAndCreateTime(java.lang.Integer id, long begin, long end)"
 *      query="SELECT OBJECT(a) FROM Alert AS a WHERE a.alertDefId = ?1 AND a.ctime BETWEEN ?2 AND ?3"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAlertDefinitionAndCreateTime(java.lang.Integer id, long begin, long end)"
 *      query="SELECT OBJECT(a) FROM Alert AS a
                 WHERE a.alertDefId = ?1 AND a.ctime BETWEEN ?2 AND ?3 ORDER BY a.ctime DESC"
 *
 * @ejb:value-object name="Alert" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_ALERT"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class AlertEJBImpl extends EntityEJB implements EntityBean {

    protected String getSequenceName() {
        return "EAM_ALERT_ID_SEQ";
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

    /** Getter for property alertDefId.
     * De-normalized so that there's no dependency if alertdef is deleted
     * @return Value of property alertDefId.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="alert_definition_id"
     * @jboss:read-only true
     */
    public abstract Integer getAlertDefId();
    
    /** Setter for property alertDefId.
     * @param alertDefId New value of property alertDefId.
     * @ejb:interface-method
     */
    public abstract void setAlertDefId(Integer alertDefId);

    /** Getter for property ctime.
     * @return Value of property ctime.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract long getCtime();
    
    /** Setter for property ctime.
     * @param ctime New value of property ctime.
     * @ejb:interface-method
     */
    public abstract void setCtime(long ctime);

    /**
     * @ejb:interface-method
     * @ejb:relation
     *      name="Alert-ConditionLogs"
     *      role-name="one-alert-has-many-conditionLogs"
     *      target-ejb="AlertConditionLog"
     *      target-role-name="each-conditionLog-has-one-alert"
     *      target-cascade-delete="yes"
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.events.shared.AlertConditionLogValue"
     *      aggregate-name="ConditionLog"
     *      members="org.hyperic.hq.events.shared.AlertConditionLogLocal"
     *      members-name="ConditionLog"
     * @jboss:target-relation
     *      fk-column="alert_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     *
     */
    public abstract java.util.Collection getConditionLogs();

    /**
     * @ejb:interface-method
     */
    public abstract void setConditionLogs(java.util.Collection items);

    /**
     * @ejb:interface-method
     * @ejb:relation
     *      name="Alert-ActionLogs"
     *      role-name="one-alert-has-many-actionLogs"
     *      target-ejb="AlertActionLog"
     *      target-role-name="each-alertActionLog-has-one-alert"
     *      target-cascade-delete="yes"
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.events.shared.AlertActionLogValue"
     *      aggregate-name="ActionLog"
     *      members="org.hyperic.hq.events.shared.AlertActionLogLocal"
     *      members-name="ActionLog"
     * @jboss:target-relation
     *      fk-column="alert_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     *
     */
    public abstract java.util.Collection getActionLogs();
    
    /**
     * @ejb:interface-method
     */
    public abstract void setActionLogs(java.util.Collection items);

    /**
     * Get the Value object for this alert
     * @ejb:interface-method
     */
    public abstract AlertValue getAlertValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setAlertValue(AlertValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public AlertPK ejbCreate(AlertValue val)
        throws CreateException {
        // Make sure there are no condition or action logs in the value object
        val.cleanConditionLog();
        val.cleanActionLog();
        
        // Now just set the entire value object
        setAlertValue(val);

        // Set the ID
        setId(super.getNextId());

        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(AlertValue val) {}

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
