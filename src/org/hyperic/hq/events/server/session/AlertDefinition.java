/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2007], Hyperic, Inc. 
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

package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertValue;

public class AlertDefinition 
    extends PersistedObject 
    implements AlertDefinitionInterface, 
               PerformsEscalations, 
               ContainerManagedTimestampTrackable
{
    private String            _name;
    private long              _ctime;
    private long              _mtime;
    private AlertDefinition   _parent;
    private Collection        _children = new ArrayList();
    private String            _description;
    private int               _priority;  // XXX -- Needs to default to 1
    private int               _appdefId;
    private int               _appdefType;
    private boolean           _active;   // XXX -- Needs to default to true     
    private boolean           _enabled;   // XXX -- Needs to default to true
    private int               _frequencyType;
    private Long              _count;  // can't use primitive.
    private Long              _range;  // can't use primitive.
    private boolean           _willRecover;  // XXX -- Default to false
    private boolean           _notifyFiltered;  // XXX - default to false
    private boolean           _controlFiltered; // XXX -- default to false
    private RegisteredTrigger _actOnTrigger;
    private boolean           _deleted; // XXX -- default to false
    private Collection        _conditions = new ArrayList();
    private Collection        _triggers = new ArrayList();
    private Collection        _actions = new ArrayList();
    private Escalation        _escalation;
    private Resource          _resource;
    private AlertDefinitionState _state;

    private AlertDefinitionValue      _value;
    
    AlertDefinition() {
    }
    
    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedCreationTime() {
        return true;
    }
    
    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>false</code> by default.
     */
    public boolean allowContainerManagedLastModifiedTime() {
        return false;
    }

    AlertCondition createCondition(AlertConditionValue condVal,
                                             RegisteredTrigger trigger)
    {
        AlertCondition res = new AlertCondition(this, condVal, trigger);
        _conditions.add(res);
        return res;
    }

    Action createAction(ActionValue actVal, Action parent) {
        Action res = new Action(this, actVal, parent);
        _actions.add(res);
        return res;
    }
    
    Alert createAlert(AlertValue val) {
        Alert res = new Alert(this, val);
        return res;
    }
    
    void addTrigger(RegisteredTrigger t) {
        _triggers.add(t);
    }
    
    void removeTrigger(RegisteredTrigger t) {
        _triggers.remove(t);
    }

    void addCondition(AlertCondition c) {
        _conditions.add(c);
    }
    
    void removeCondition(AlertCondition c) {
        _conditions.remove(c);
    }

    void addAction(Action a) {
        _actions.add(a);
    }
    
    void removeAction(Action a) {
        _actions.remove(a);
    }
    
    public String getName() {
        return _name;
    }

    void setName(String name) {
        _name = name;
    }

    public long getCtime() {
        return _ctime;
    }

    void setCtime(long ctime) {
        _ctime = ctime;
    }

    public long getMtime() {
        return _mtime;
    }

    void setMtime(long mtime) {
        _mtime = mtime;
    }

    public AlertDefinition getParent() {
        return _parent;
    }

    void setParent(AlertDefinition parent) {
        _parent = parent;
    }

    public Collection getChildren() {
        return Collections.unmodifiableCollection(_children);
    }
    
    Collection getChildrenBag() {
        return _children;
    }
    
    void setChildrenBag(Collection c) {
        _children = c;
    }
    
    void removeChild(AlertDefinition child) {
        _children.remove(child);
    }
    
    void clearChildren() {
        _children.clear();
    }
    
    public String getDescription() {
        return _description;
    }

    void setDescription(String description) {
        _description = description;
    }

    /**
     * Returns the same thing as getPriority(), though a typesafe enum
     */
    public AlertSeverity getSeverity() {
        return AlertSeverity.findByCode(getPriority());
    }
    
    public int getPriority() {
        return _priority;
    }

    void setPriority(int priority) {
        _priority = priority;
    }

    public int getAppdefId() {
        return _appdefId;
    }

    void setAppdefId(int appdefId) {
        _appdefId = appdefId;
    }

    public int getAppdefType() {
        return _appdefType;
    }

    void setAppdefType(int appdefType) {
        _appdefType = appdefType;
    }
        
    /**
     * Check if an alert definition is enabled.
     * 
     * @return <code>true</code> if the alert definition is enabled; 
     *         <code>false</code> if disabled.
     */
    public boolean isEnabled() {
        return _enabled;
    }
    
    /**
     * Check if an alert definition is active.
     * 
     * @return <code>true</code> if the alert definition is active;
     *          <code>false</code> if inactive.
     */    
    boolean isActive() {
        return _active;
    }
    
    /**
     * As an application user, set the alert definition enabled status. Also 
     * sets the active status to the same value.
     * 
     * @param enabled <code>true</code> to enable the alert definition;
     *                <code>false</code> to disable the alert definition.
     */
    public void setEnabledByUser(boolean enabled) {
        setEnabled(enabled);
        setActive(enabled);
    }

    /**
     * As the system, set the alert definition enabled status. The enabled 
     * status will not be set if an application user has already disabled the 
     * alert definition.
     * 
     * @param enabled <code>true</code> to enable the alert definition;
     *                <code>false</code> to disable the alert definition.
     * @return <code>true</code> if the enabled status was set;
     *         <code>false</code> if it wasn't set.                     
     */
    public boolean setEnabledBySystem(boolean enabled) {
        boolean statusSet = false;
        
        if (isActive()) {
            setEnabled(enabled);
            statusSet = true;
        }
        
        return statusSet;
    }
    
    /**
     * For Hibernate persistence only. Do not call directly.
     */
    void setActive(boolean active) {
        _active = active;
    }
    
    /**
     * For Hibernate persistence only. Do not call directly.
     */
    void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    public int getFrequencyType() {
        return _frequencyType;
    }

    void setFrequencyType(int frequencyType) {
        _frequencyType = frequencyType;
    }

    public long getCount() {
        return _count != null ? _count.longValue() : 0;
    }

    void setCount(Long count) {
        _count = count;
    }

    public long getRange() {
        return _range != null ? _range.longValue() : 0;
    }

    void setRange(Long range) {
        _range = range;
    }

    public boolean willRecover() {
        return _willRecover;
    }
    
    public boolean isWillRecover() {
        return _willRecover;
    }

    void setWillRecover(boolean willRecover) {
        _willRecover = willRecover;
    }

    public boolean isNotifyFiltered() {
        return _notifyFiltered;
    }

    void setNotifyFiltered(boolean notifyFiltered) {
        _notifyFiltered = notifyFiltered;
    }

    public boolean isControlFiltered() {
        return _controlFiltered;
    }

    void setControlFiltered(boolean controlFiltered) {
        _controlFiltered = controlFiltered;
    }

    public RegisteredTrigger getActOnTrigger() {
        return _actOnTrigger;
    }

    void setActOnTrigger(RegisteredTrigger actOnTrigger) {
        _actOnTrigger = actOnTrigger;
    }

    public Escalation getEscalation() {
        return _escalation;
    }
    
    void setEscalation(Escalation escalation) {
        _escalation = escalation;
    }

    public boolean isDeleted() {
        return _deleted;
    }

    void setDeleted(boolean deleted) {
        _deleted = deleted;
    }

    public Resource getResource() {
        return _resource;
    }
    
    void setResource(Resource resource) {
        _resource = resource;
    }

    public Collection getActions() {
        return Collections.unmodifiableCollection(_actions);
    }
    
    Collection getActionsBag() {
        return _actions;
    }

    void setActionsBag(Collection actions) {
        _actions = actions;
    }

    void clearActions() {
        for (Iterator it = _actions.iterator(); it.hasNext(); ) {
            Action act = (Action) it.next();
            act.setAlertDefinition(null);
        }
        _actions.clear();
    }

    public Collection getConditions() {
        return Collections.unmodifiableCollection(_conditions);
    }
    
    Collection getConditionsBag() {
        return _conditions;
    }

    void setConditionsBag(Collection conditions) {
        _conditions = conditions;
    }

    void clearConditions() {
        for (Iterator it = _conditions.iterator(); it.hasNext(); ) {
            AlertCondition cond = (AlertCondition) it.next();
            cond.setAlertDefinition(null);
            cond.setTrigger(null);
        }
        _conditions.clear();
    }

    public Collection getTriggers() {
        return Collections.unmodifiableCollection(_triggers);
    }
    
    Collection getTriggersBag() {
        return _triggers;
    }

    void setTriggersBag(Collection triggers) {
        _triggers = triggers;
    }

    void clearTriggers() {
        for (Iterator it = _triggers.iterator(); it.hasNext(); ) {
            RegisteredTrigger trigger = (RegisteredTrigger) it.next();
            trigger.setAlertDefinition(null);
        }
        _triggers.clear();
    }

    public boolean isResourceTypeDefinition() {
        return getParent() != null && 
               getParent().getId().equals(new Integer(0));
    }

    public AppdefEntityID getAppdefEntityId() {
        return new AppdefEntityID(getAppdefType(), getAppdefId());
    }
    
    /**
     * Get the time that the alert definition last fired.
     */
    public long getLastFired() {
        if (_state != null)
            return _state.getLastFired();
        
        return 0;
    }
    
    void setLastFired(long lastFired) {
        _state.setLastFired(lastFired);
    }
    
    public AlertDefinitionState getAlertDefinitionState() {
        return _state;
    }
    
    void setAlertDefinitionState(AlertDefinitionState state) {
        _state = state;
    }

    public AlertDefinitionValue getAlertDefinitionValue() {
        if (_value == null)
            _value = new AlertDefinitionValue();

        _value.setId(getId());
        _value.setName(getName() == null ? "" : getName());
        _value.setCtime(getCtime());
        _value.setMtime(getMtime());
        _value.setParentId(getParent() == null ? null : getParent().getId());
        _value.setDescription(getDescription());
        _value.setEnabled(isEnabled());
        _value.setActive(isActive());
        _value.setWillRecover(isWillRecover());
        _value.setNotifyFiltered(isNotifyFiltered());
        _value.setControlFiltered(isControlFiltered());
        _value.setPriority(getPriority());
        _value.setAppdefId(getAppdefId());
        _value.setAppdefType(getAppdefType());
        _value.setFrequencyType(getFrequencyType());
        _value.setCount(getCount());
        _value.setRange(getRange());
        _value.setDeleted(isDeleted());
        if (getActOnTrigger() != null) {
            _value.setActOnTriggerId(getActOnTrigger().getId().intValue());
        }
        if (getEscalation() != null) {
            _value.setEscalationId(getEscalation().getId());
        }
        else {
            _value.setEscalationId(null);
        }

        _value.removeAllTriggers();
        for (Iterator i=getTriggers().iterator(); i.hasNext(); ) {
            RegisteredTrigger t = (RegisteredTrigger)i.next();
            _value.addTrigger(t.getRegisteredTriggerValue());
        }
        _value.cleanTrigger();
        
        _value.removeAllConditions();
        for (Iterator i=getConditions().iterator(); i.hasNext(); ) {
            AlertCondition c = (AlertCondition)i.next();

            _value.addCondition(c.getAlertConditionValue());
        }
        _value.cleanCondition();
        
        _value.removeAllActions();
        for (Iterator i=getActions().iterator(); i.hasNext(); ) {
            Action a = (Action)i.next();
            
            _value.addAction(a.getActionValue());
        }
        _value.cleanAction();
        return _value;
    }

    public EscalationAlertType getAlertType() {
        return ClassicEscalationAlertType.CLASSIC;
    }

    public AlertDefinitionInterface getDefinitionInfo() {
        return this;
    }

    public boolean performsEscalations() {
        return true;
    }
    
    public String toString() {
        return "alertDef [" + this.getName() + "]";
    }
}
