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

package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionBasicValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

public class AlertDefinition 
    extends PersistedObject
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
    private boolean           _enabled;   // XXX -- Needs to default to true
    private int               _frequencyType;
    private long              _count;
    private long              _range;
    private boolean           _willRecover;  // XXX -- Default to false
    private boolean           _notifyFiltered;  // XXX - default to false
    private boolean           _controlFiltered; // XXX -- default to false
    private RegisteredTrigger _actOnTrigger;
    private boolean           _deleted; // XXX -- default to false
    private Collection        _conditions = new ArrayList();
    private Collection        _triggers = new ArrayList();
    private Collection        _actions = new ArrayList();
    private Escalation        _escalation;

    private AlertDefinitionValue      _value;
    private AlertDefinitionBasicValue _basicValue;
    
    protected AlertDefinition() {
    }

    protected AlertCondition createCondition(AlertConditionValue condVal,
                                             RegisteredTrigger trigger)
    {
        AlertCondition res = new AlertCondition(this, condVal, trigger);
        
        save(res);
        _conditions.add(res);
        return res;
    }

    protected Action createAction(ActionValue actVal, Action parent) {
        Action res = new Action(this, actVal, parent);
        
        save(res);
        _actions.add(res);
        return res;
    }
    
    protected Alert createAlert(AlertValue val) {
        Alert res = new Alert(this, val);
        
        save(res);
        return res;
    }
    
    protected void addTrigger(RegisteredTrigger t) {
        _triggers.add(t);
    }
    
    protected void removeTrigger(RegisteredTrigger t) {
        _triggers.remove(t);
    }

    protected void clearActions() {
        _actions.clear();
    }
    
    protected void clearConditions() {
        _conditions.clear();
    }
    
    protected void addCondition(AlertCondition c) {
        _conditions.add(c);
    }
    
    protected void removeCondition(AlertCondition c) {
        _conditions.remove(c);
    }

    protected void addAction(Action a) {
        _actions.add(a);
    }
    
    protected void removeAction(Action a) {
        _actions.remove(a);
    }
    
    public String getName() {
        return _name;
    }

    protected void setName(String name) {
        _name = name;
    }

    public long getCtime() {
        return _ctime;
    }

    protected void setCtime(long ctime) {
        _ctime = ctime;
    }

    public long getMtime() {
        return _mtime;
    }

    protected void setMtime(long mtime) {
        _mtime = mtime;
    }

    public AlertDefinition getParent() {
        return _parent;
    }

    protected void setParent(AlertDefinition parent) {
        _parent = parent;
    }

    public Collection getChildren() {
        return Collections.unmodifiableCollection(_children);
    }
    
    protected Collection getChildrenBag() {
        return _children;
    }
    
    protected void setChildrenBag(Collection c) {
        _children = c;
    }
    
    public String getDescription() {
        return _description;
    }

    protected void setDescription(String description) {
        _description = description;
    }

    public int getPriority() {
        return _priority;
    }

    protected void setPriority(int priority) {
        _priority = priority;
    }

    public int getAppdefId() {
        return _appdefId;
    }

    protected void setAppdefId(int appdefId) {
        _appdefId = appdefId;
    }

    public int getAppdefType() {
        return _appdefType;
    }

    protected void setAppdefType(int appdefType) {
        _appdefType = appdefType;
    }

    public boolean isEnabled() {
        return _enabled;
    }

    protected void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    public int getFrequencyType() {
        return _frequencyType;
    }

    protected void setFrequencyType(int frequencyType) {
        _frequencyType = frequencyType;
    }

    public long getCount() {
        return _count;
    }

    protected void setCount(long count) {
        _count = count;
    }

    public long getRange() {
        return _range;
    }

    protected void setRange(long range) {
        _range = range;
    }

    public boolean willRecover() {
        return _willRecover;
    }
    
    public boolean isWillRecover() {
        return _willRecover;
    }

    protected void setWillRecover(boolean willRecover) {
        _willRecover = willRecover;
    }

    public boolean isNotifyFiltered() {
        return _notifyFiltered;
    }

    protected void setNotifyFiltered(boolean notifyFiltered) {
        _notifyFiltered = notifyFiltered;
    }

    public boolean isControlFiltered() {
        return _controlFiltered;
    }

    protected void setControlFiltered(boolean controlFiltered) {
        _controlFiltered = controlFiltered;
    }

    public RegisteredTrigger getActOnTrigger() {
        return _actOnTrigger;
    }

    protected void setActOnTrigger(RegisteredTrigger actOnTrigger) {
        _actOnTrigger = actOnTrigger;
    }

    public Escalation getEscalation()
    {
        return _escalation;
    }

    public void setEscalation(Escalation _escalation)
    {
        this._escalation = _escalation;
    }

    public boolean isDeleted() {
        return _deleted;
    }

    protected void setDeleted(boolean deleted) {
        _deleted = deleted;
    }

    public Collection getActions() {
        return Collections.unmodifiableCollection(_actions);
    }
    
    protected Collection getActionsBag() {
        return _actions;
    }

    protected void setActionsBag(Collection actions) {
        _actions = actions;
    }

    public Collection getConditions() {
        return Collections.unmodifiableCollection(_conditions);
    }
    
    protected Collection getConditionsBag() {
        return _conditions;
    }

    protected void setConditionsBag(Collection conditions) {
        _conditions = conditions;
    }

    public Collection getTriggers() {
        return Collections.unmodifiableCollection(_triggers);
    }
    
    protected Collection getTriggersBag() {
        return _triggers;
    }

    protected void setTriggersBag(Collection triggers) {
        _triggers = triggers;
    }
    
    public boolean isResourceTypeDefinition() {
        return getParent() != null && 
               getParent().getId().equals(new Integer(0));
    }

    public AppdefEntityID getAppdefEntityId() {
        return new AppdefEntityID(getAppdefType(), getAppdefId());
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

    protected void setAlertDefinitionValueNoRels(AlertDefinitionValue val) {
        AlertDefinitionDAO aDAO = DAOFactory.getDAOFactory().getAlertDefDAO();
        TriggerDAO tDAO = DAOFactory.getDAOFactory().getTriggerDAO();
        
        setName(val.getName());
        setCtime(val.getCtime());
        setMtime(val.getMtime());
        setParent(val.getParentId() == null ? null : 
                  aDAO.findById(val.getParentId()));
        setDescription(val.getDescription());
        setEnabled(val.getEnabled());
        setWillRecover(val.getWillRecover());
        setNotifyFiltered(val.getNotifyFiltered() );
        setControlFiltered(val.getControlFiltered() );
        setPriority(val.getPriority());
        setAppdefId(val.getAppdefId());
        setAppdefType(val.getAppdefType());
        setFrequencyType(val.getFrequencyType());
        setCount(val.getCount());
        setRange(val.getRange());
        setDeleted(val.getDeleted());
        if (val.actOnTriggerIdHasBeenSet()) {
            setActOnTrigger(tDAO.findById(new Integer(val.getActOnTriggerId())));
        }
        else {
            setActOnTrigger(null);
        }
    }

    protected void setAlertDefinitionValue(AlertDefinitionValue val) {
        AlertConditionDAO cDAO = DAOFactory.getDAOFactory().getAlertConditionDAO();
        ActionDAO actDAO = DAOFactory.getDAOFactory().getActionDAO();
        TriggerDAO tDAO = DAOFactory.getDAOFactory().getTriggerDAO();
        
        setAlertDefinitionValueNoRels(val);

        for (Iterator i=val.getAddedTriggers().iterator(); i.hasNext(); ) {
            RegisteredTriggerValue tVal = (RegisteredTriggerValue)i.next();
            RegisteredTrigger t = tDAO.findById(tVal.getId());
            
            addTrigger(t);
        }
        
        for (Iterator i=val.getRemovedTriggers().iterator(); i.hasNext(); ) {
            RegisteredTriggerValue tVal = (RegisteredTriggerValue)i.next();
            RegisteredTrigger t = tDAO.findById(tVal.getId());
            
            removeTrigger(t);
        }
        
        for (Iterator i=val.getAddedConditions().iterator(); i.hasNext(); ) {
            AlertConditionValue cVal = (AlertConditionValue)i.next();
            AlertCondition c = cDAO.findById(cVal.getId());
            
            addCondition(c);
        }

        for (Iterator i=val.getRemovedConditions().iterator(); i.hasNext(); ) {
            AlertConditionValue cVal = (AlertConditionValue)i.next();
            AlertCondition c = cDAO.findById(cVal.getId());
            
            removeCondition(c);
        }

        for (Iterator i=val.getAddedActions().iterator(); i.hasNext(); ) {
            ActionValue aVal = (ActionValue)i.next();
            Action a = actDAO.findById(aVal.getId());
            
            addAction(a);
        }

        for (Iterator i=val.getRemovedActions().iterator(); i.hasNext(); ) {
            ActionValue aVal = (ActionValue)i.next();
            Action a = actDAO.findById(aVal.getId());
            
            removeAction(a);
        }
    }
    
    protected AlertDefinitionBasicValue getAlertDefinitionBasicValue() {
        if (_basicValue == null) {
            _basicValue = new AlertDefinitionBasicValue();
        }

        _basicValue.setId(getId());
        _basicValue.setName(getName() == null ? "" : getName());
        _basicValue.setCtime(getCtime());
        _basicValue.setMtime(getMtime());        
        _basicValue.setParentId(getParent() == null ? null : 
                                getParent().getId());
        _basicValue.setDescription(getDescription());
        _basicValue.setEnabled(isEnabled());
        _basicValue.setWillRecover(isWillRecover());
        _basicValue.setNotifyFiltered(isNotifyFiltered());
        _basicValue.setControlFiltered(isControlFiltered());
        _basicValue.setPriority(getPriority());
        _basicValue.setAppdefId(getAppdefId());
        _basicValue.setAppdefType(getAppdefType());
        _basicValue.setFrequencyType(getFrequencyType());
        _basicValue.setCount(getCount());
        _basicValue.setRange(getRange());
        _basicValue.setActOnTriggerId(getActOnTrigger().getId().intValue());
        _basicValue.setDeleted(isDeleted());

        _basicValue.removeAllTriggers();
        for (Iterator i=getTriggers().iterator(); i.hasNext(); ) {
            RegisteredTrigger t = (RegisteredTrigger)i.next();
            _basicValue.addTrigger(t.getRegisteredTriggerValue());
        }
        _basicValue.cleanTrigger();

        _basicValue.removeAllConditions();
        for (Iterator i=getConditions().iterator(); i.hasNext(); ) {
            AlertCondition c = (AlertCondition)i.next();
            _basicValue.addCondition(c.getAlertConditionValue());
        }
        _basicValue.cleanCondition();
        
        _basicValue.removeAllActions();
        for (Iterator i=getActions().iterator(); i.hasNext(); ) {
            Action a = (Action)i.next();
            _basicValue.addAction(a.getActionValue());
        }
        _basicValue.cleanAction();
        return _basicValue;
    }
}
