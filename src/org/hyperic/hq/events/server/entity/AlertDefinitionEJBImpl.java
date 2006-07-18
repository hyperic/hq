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
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.hyperic.hq.events.shared.AlertDefinitionPK;
import org.hyperic.hq.events.shared.AlertDefinitionValue;

/** The connection details required to retrieve monitor values
 * @ejb:bean name="AlertDefinition"
 *      jndi-name="ejb/events/AlertDefinition"
 *      local-jndi-name="LocalAlertDefinition"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.List findAll()"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.deleted = false AND (NOT a.parentId = 0 OR a.parentId IS NULL)"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.List findChildAlertDefinitions(java.lang.Integer id)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.parentId = ?1 AND a.deleted = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.List findByAppdefEntity(int type, int id)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.appdefType = ?1 AND a.appdefId = ?2 AND a.deleted = false AND (NOT a.parentId = 0 OR a.parentId IS NULL)"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAppdefEntity(int type, int id)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.appdefType = ?1 AND a.appdefId = ?2 AND a.deleted = false AND (NOT a.parentId = 0 OR a.parentId IS NULL) 
               ORDER BY a.name"
 *
 * @ejb:finder signature="java.util.List findByAppdefEntitySortByCtime(int type, int id)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.appdefType = ?1 AND a.appdefId = ?2 AND a.deleted = false AND (NOT a.parentId = 0 OR a.parentId IS NULL)"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAppdefEntitySortByCtime(int type, int id)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.appdefType = ?1 AND a.appdefId = ?2 AND a.deleted = false AND (NOT a.parentId = 0 OR a.parentId IS NULL) 
               ORDER BY a.ctime"
 *
 * @ejb:finder signature="java.util.List findByAppdefEntityType(int type, int id)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.appdefType = ?1 AND a.appdefId = ?2 AND a.deleted = false AND a.parentId = 0"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAppdefEntityType(int type, int id)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.appdefType = ?1 AND a.appdefId = ?2 AND a.deleted = false AND a.parentId = 0 
               ORDER BY a.name"
 *
 * @ejb:finder signature="java.util.List findEntityChildAlertDefinitions(int type, int id, java.lang.Integer parentId)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.appdefType = ?1 AND a.appdefId = ?2 AND a.deleted = false AND a.parentId = ?3"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.events.shared.AlertDefinitionLocal findEntityChildAlertDefinition(int type, int id, java.lang.Integer parentId)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a
               WHERE a.appdefType = ?1 AND a.appdefId = ?2 AND a.deleted = false AND a.parentId = ?3"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.events.shared.AlertDefinitionLocal findByTrigger(java.lang.Integer tid)"
 *      query="SELECT OBJECT(a) FROM AlertDefinition a, IN (a.triggers) t 
               WHERE t.id = ?1 AND a.deleted = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="AlertDefinition" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_ALERT_DEFINITION"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AlertDefinitionEJBImpl
    extends EntityEJB
    implements EntityBean {

    protected String getSequenceName() {
        return "EAM_ALERT_DEFINITION_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /** Id of this AlertDefinition
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract Integer getId();

    /** Setter for property id.
     *
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /** Name of this AlertDefinition
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getName();

    /** Setter for property name.
     *
     * @ejb:interface-method
     */
    public abstract void setName(String name);

    /** Getter for property ctime.
     * @return Value of property ctime.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract long getCtime();

    /** Setter for property ctime.
     * @param ctime New value of property ctime.
     *
     * @ejb:interface-method
     */
    public abstract void setCtime(long ctime);

    /** Getter for property mtime.
     * @return Value of property mtime.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract long getMtime();

    /** Setter for property mtime.
     * @param mtime New value of property mtime.
     *
     * @ejb:interface-method
     */
    public abstract void setMtime(long mtime);

    /** Parent ID of this AlertDefinition (if any)
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

    /**
     * Description of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract java.lang.String getDescription();
    /**
     * @ejb:interface-method
     */
    public abstract void setDescription(String desc);
    
    /** Getter for property enabled.
     * @return Value of property enabled.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract boolean getEnabled();
    
    /** Setter for property enabled.
     * @param enabled New value of property enabled.
     *
     * @ejb:interface-method
     */
    public abstract void setEnabled(boolean enabled);
    
    /** Getter for property willRecover.
     * @return Value of property willRecover.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:column-name name="will_recover"
     * @jboss:read-only true
     */
    public abstract boolean getWillRecover();

    /** Setter for property willRecover.
     * @param willRecover New value of property willRecover.
     *
     * @ejb:interface-method
     */
    public abstract void setWillRecover(boolean willRecover);
    
    /** Getter for property notifyFiltered.
     * @return Value of property notifyFiltered.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:column-name name="notify_filtered"
     * @jboss:read-only true
     */
    public abstract boolean getNotifyFiltered();

    /** Setter for property notifyFiltered.
     * @param notifyFiltered New value of property notifyFiltered.
     *
     * @ejb:interface-method
     */
    public abstract void setNotifyFiltered(boolean notifyFiltered);
    
    /** Getter for property controlFiltered.
     * @return Value of property controlFiltered.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:column-name name="control_filtered"
     * @jboss:read-only true
     */
    public abstract boolean getControlFiltered();

    /** Setter for property controlFiltered.
     * @param controlFiltered New value of property controlFiltered.
     *
     * @ejb:interface-method
     */
    public abstract void setControlFiltered(boolean controlFiltered);
    
    /**
     * @return priority of the alert
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract int getPriority();

    /**
     * @ejb:interface-method
     */
    public abstract void setPriority(int priority);

    /** Getter for property appdefId.
     * @return Value of property appdefId.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:column-name name="appdef_id"
     * @jboss:read-only true
     */
    public abstract int getAppdefId();
    
    /** Setter for property appdefId.
     * @param appdefId New value of property appdefId.
     *
     * @ejb:interface-method
     */
    public abstract void setAppdefId(int appdefId);
    
    /** Getter for property appdefType.
     * @return Value of property appdefType.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:column-name name="appdef_type"
     * @jboss:read-only true
     */
    public abstract int getAppdefType();
    
    /** Setter for property appdefType.
     * @param appdefType New value of property appdefType.
     *
     * @ejb:interface-method
     */
    public abstract void setAppdefType(int appdefType);

    /** Getter for property frequencyType.
     * @return Value of property frequencyType.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:column-name name="frequency_type"
     * @jboss:read-only true
     */
    public abstract int getFrequencyType();
    
    /** Setter for property frequencyType.
     * @param frequencyType New value of property frequencyType.
     *
     * @ejb:interface-method
     */
    public abstract void setFrequencyType(int frequencyType);

    /** Getter for property count.
     * @return Value of property count.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract long getCount();
    
    /** Setter for property count.
     * @param count New value of property count.
     *
     * @ejb:interface-method
     */
    public abstract void setCount(long count);

    /** Getter for property range.
     * @return Value of property range.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract long getRange();
    
    /** Setter for property range.
     * @param range New value of property range.
     *
     * @ejb:interface-method
     */
    public abstract void setRange(long range);

    /** Getter for property actOnTriggerId.
     * @return Value of property actOnTriggerId.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:column-name name="act_on_trigger_id"
     * @jboss:read-only true
     */
    public abstract int getActOnTriggerId();
    
    /** Setter for property actOnTriggerId.
     * @param actOnTriggerId New value of property actOnTriggerId.
     *
     * @ejb:interface-method
     */
    public abstract void setActOnTriggerId(int actOnTriggerId);

    /** Getter for property deleted.
     * @return Value of property deleted.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract boolean getDeleted();

    /** Setter for property deleted.
     * @param deleted New value of property deleted.
     *
     * @ejb:interface-method
     */
    public abstract void setDeleted(boolean deleted);

    ///////////////////////////////////////
    // associations

    /**
     * @ejb:interface-method
     * @ejb:relation
     *      name="AlertDefinition-Triggers"
     *      role-name="one-alertDefinition-has-many-triggers"
     *      target-ejb="RegisteredTrigger"
     *      target-role-name="each-trigger-has-one-alertDefinition"
     *      target-cascade-delete="yes"
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.events.shared.RegisteredTriggerValue"
     *      aggregate-name="Trigger"
     *      members="org.hyperic.hq.events.shared.RegisteredTriggerLocal"
     *      members-name="Trigger"
     * @jboss:target-relation
     *      fk-column="alert_definition_id"
     *      related-pk-field="id"
     *
     */
    public abstract java.util.Collection getTriggers();
    
    /**
     * @ejb:interface-method
     */
    public abstract void setTriggers(java.util.Collection items);

    /**
     * @ejb:interface-method
     * @ejb:relation
     *      name="AlertDefinition-Conditions"
     *      role-name="one-alertDefinition-has-many-conditions"
     *      target-ejb="AlertCondition"
     *      target-role-name="each-condition-has-one-alertDefinition"
     *      target-cascade-delete="yes"
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.events.shared.AlertConditionValue"
     *      aggregate-name="Condition"
     *      members="org.hyperic.hq.events.shared.AlertConditionLocal"
     *      members-name="Condition"
     * @jboss:target-relation
     *      fk-column="alert_definition_id"
     *      related-pk-field="id"
     *
     */
    public abstract java.util.Collection getConditions();
    
    /**
     * @ejb:interface-method
     */
    public abstract void setConditions(java.util.Collection items);
    
    /**
     * @ejb:interface-method
     * @ejb:relation
     *      name="AlertDefinition-Actions"
     *      role-name="one-alertDefinition-has-many-actions"
     *      target-ejb="Action"
     *      target-role-name="each-action-has-one-alertDefinition"
     *      target-cascade-delete="yes"
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.events.shared.ActionValue"
     *      aggregate-name="Action"
     *      members="org.hyperic.hq.events.shared.ActionLocal"
     *      members-name="Action"
     * @jboss:target-relation
     *      fk-column="alert_definition_id"
     *      related-pk-field="id"
     *
     */
    public abstract java.util.Collection getActions();
    
    /**
     * @ejb:interface-method
     */
    public abstract void setActions(java.util.Collection items);
    
    ///////////////////////////////////////
    // associations

    /**
     * Get the Value object for this trigger
     * @ejb:interface-method
     */
    public abstract AlertDefinitionValue getAlertDefinitionValue();

    /**
     * @ejb:interface-method
     */
    public abstract void setAlertDefinitionValue(
        AlertDefinitionValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public AlertDefinitionPK ejbCreate(AlertDefinitionValue val,
                                       List conditions,
                                       List triggers,
                                       List actions)
        throws CreateException 
    {
        // Set CMR's to empty
        val.cleanAction();
        val.cleanCondition();
        val.cleanTrigger();
        
        // Set the entire value object
        this.setAlertDefinitionValue(val);
        
        // Get the current time for both creation and modification
        long current = System.currentTimeMillis();
        this.setCtime(current);
        this.setMtime(current);

        // Get the next ID
        this.setId(getNextId());

        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(AlertDefinitionValue val,
                              List conditions,
                              List triggers,
                              List actions) {
        // Save the conditions
        if (conditions != null)
            this.setConditions(conditions);

        if (triggers != null)
            this.setTriggers(triggers);
            
        if (actions != null)
            this.setActions(actions);
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
     * @see javax.ejb.EntityBean#unsetEntityContext()
     */
    public void unsetEntityContext() throws RemoteException {}
    
} // end AlertDefinitionEJBImpl
