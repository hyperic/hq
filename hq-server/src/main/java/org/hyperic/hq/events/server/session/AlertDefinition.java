/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ. HQ is free software; you can redistribute it and/or
 * modify it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 */

package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.measurement.MeasurementConstants;

public class AlertDefinition
    extends PersistedObject implements AlertDefinitionInterface, PerformsEscalations,
    ContainerManagedTimestampTrackable {
    private String _name;
    private long _ctime;
    private long _mtime;
    private AlertDefinition _parent;
    private Collection _children = new ArrayList();
    private String _description;
    private int _priority; // XXX -- Needs to default to 1
    private boolean _active; // XXX -- Needs to default to true
    private boolean _enabled; // XXX -- Needs to default to true
    private int _frequencyType;
    private Long _count; // can't use primitive.
    private Long _range; // can't use primitive.
    private boolean _willRecover; // XXX -- Default to false
    private boolean _notifyFiltered; // XXX - default to false
    private boolean _controlFiltered; // XXX -- default to false
    private boolean _deleted; // XXX -- default to false
    private Collection _conditions = new ArrayList();
    private Collection _triggers = new ArrayList();
    private Collection<Action> _actions = new ArrayList<Action>();
    private Escalation _escalation;
    private Resource _resource;
    private AlertDefinitionState _state;

    private AlertDefinitionValue _value;

    public AlertDefinition() {
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

    AlertCondition createCondition(AlertConditionValue condVal, RegisteredTrigger trigger) {
        AlertCondition res = new AlertCondition(this, condVal, trigger);
        _conditions.add(res);
        return res;
    }

    Action createAction(String className, byte[] config, Action parent) {
        Action res = new Action(this, className, config, parent);
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

    public void setName(String name) {
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

    public Collection<AlertDefinition> getChildren() {
        List children = new ArrayList();
        for (Iterator it = _children.iterator(); it.hasNext();) {
            AlertDefinition child = (AlertDefinition) it.next();
            if (child.isDeleted() || child.getResource() == null)
                continue;
            children.add(child);
        }
        return Collections.unmodifiableCollection(children);
    }

    Collection<AlertDefinition> getChildrenBag() {
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

    public Integer getAppdefId() {
        return getResource().getInstanceId();
    }

    public int getAppdefType() {
        String rtName = getResource().getResourceType().getName();
        if (rtName.equals(AuthzConstants.platformResType)) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        } else if (rtName.equals(AuthzConstants.serverResType)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        } else if (rtName.equals(AuthzConstants.serviceResType)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        } else if (rtName.equals(AuthzConstants.policyResType)) {
            return AppdefEntityConstants.APPDEF_TYPE_POLICY;
        } else if (rtName.equals(AuthzConstants.applicationResType)) {
            return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
        } else if (rtName.equals(AuthzConstants.groupResType)) {
            return AppdefEntityConstants.APPDEF_TYPE_GROUP;
        } else if (rtName.equals(AuthzConstants.platformPrototypeTypeName)) {
            return AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
        } else if (rtName.equals(AuthzConstants.serverPrototypeTypeName)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVER;
        } else if (rtName.equals(AuthzConstants.servicePrototypeTypeName)) {
            return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        } else {
            throw new IllegalArgumentException(rtName + " is not a valid Appdef Resource Type");
        }
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
     *         <code>false</code> if inactive.
     */
    public boolean isActive() {
        return _active;
    }

    /**
     * Check if the alert definition is a recovery alert
     */
    public boolean isRecoveryDefinition() {
        for (Iterator it = getConditionsBag().iterator(); it.hasNext();) {
            AlertCondition cond = (AlertCondition) it.next();
            if (cond.getType() == EventConstants.TYPE_ALERT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Activate or deactivate an alert definition.
     * 
     * @param activate <code>true</code> to activate the alert definition;
     *        <code>false</code> to deactivate the alert definition.
     */
    public void setActiveStatus(boolean activate) {
        setEnabled(activate);
        setActive(activate);
    }

    /**
     * Enable or disable the alert definition. This operation will not succeed
     * if the alert definition is not active.
     * 
     * @param enabled <code>true</code> to enable the alert definition;
     *        <code>false</code> to disable the alert definition.
     * @return <code>true</code> if the operation succeeded, meaning the enabled
     *         status was set; <code>false</code> if it wasn't set.
     */
    public boolean setEnabledStatus(boolean enabled) {
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

    public void setResource(Resource resource) {
        _resource = resource;
    }

    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(_actions);
    }

    Collection<Action> getActionsBag() {
        return _actions;
    }

    void setActionsBag(Collection<Action> actions) {
        _actions = actions;
    }

    void clearActions() {
        for (Action act : _actions) {
            act.setAlertDefinition(null);
        }
        _actions.clear();
    }

    public Collection<AlertCondition> getConditions() {
        return Collections.unmodifiableCollection(_conditions);
    }

    Collection<AlertCondition> getConditionsBag() {
        return _conditions;
    }

    void setConditionsBag(Collection conditions) {
        _conditions = conditions;
    }

    void clearConditions() {
        for (Iterator it = _conditions.iterator(); it.hasNext();) {
            AlertCondition cond = (AlertCondition) it.next();
            cond.setAlertDefinition(null);
            cond.setTrigger(null);
        }
        _conditions.clear();
    }

    public Collection<RegisteredTrigger> getTriggers() {
        return Collections.unmodifiableCollection(_triggers);
    }

    Collection getTriggersBag() {
        return _triggers;
    }

    void setTriggersBag(Collection triggers) {
        _triggers = triggers;
    }

    void clearTriggers() {
        for (Iterator it = _conditions.iterator(); it.hasNext();) {
            AlertCondition cond = (AlertCondition) it.next();
            cond.setTrigger(null);
        }
        // the triggersBag parent-child relationship is set to
        // cascade="all-delete-orphan", so clearing the triggers
        // collection will also delete the triggers from the db
        _triggers.clear();
    }

    public boolean isResourceTypeDefinition() {
        return getParent() != null && getParent().getId().equals(new Integer(0));
    }

    /**
     * Check if an alert definition is configured for only availability.
     * 
     * @param up Indicates where the availability condition is up (true) or down
     *        (false)
     * @return <code>true</code> if the alert definition has an availability
     *         condition.
     */
    public boolean isAvailability(boolean up) {
        boolean isAvail = false;

        // ignore multi-conditional alert definitions
        if (_conditions.size() == 1) {
            for (Iterator cit = _conditions.iterator(); cit.hasNext();) {
                AlertCondition cond = (AlertCondition) cit.next();

                if (cond != null && cond.getName() != null &&
                    cond.getName().toUpperCase().contains(MeasurementConstants.CAT_AVAILABILITY.toUpperCase())) {

                    if ("=".equals(cond.getComparator())) {
                        if (up) {
                            if (cond.getThreshold() == MeasurementConstants.AVAIL_UP) {
                                isAvail = true;
                                break;
                            }
                        } else {
                            if (cond.getThreshold() == MeasurementConstants.AVAIL_DOWN) {
                                isAvail = true;
                                break;
                            }
                        }
                    } else if ("!=".equals(cond.getComparator())) {
                        if (up) {
                            if (cond.getThreshold() == MeasurementConstants.AVAIL_DOWN) {
                                isAvail = true;
                                break;
                            }
                        } else {
                            if (cond.getThreshold() == MeasurementConstants.AVAIL_UP) {
                                isAvail = true;
                                break;
                            }
                        }
                    } else if ("<".equals(cond.getComparator())) {
                        if (!up) {
                            if (cond.getThreshold() <= MeasurementConstants.AVAIL_UP
                                && cond.getThreshold() > MeasurementConstants.AVAIL_DOWN) {
                                isAvail = true;
                                break;
                            }
                        }
                    } else if (">".equals(cond.getComparator())) {
                        if (up) {
                            if (cond.getThreshold() >= MeasurementConstants.AVAIL_DOWN
                                && cond.getThreshold() < MeasurementConstants.AVAIL_UP) {
                                isAvail = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return isAvail;
    }

    public AppdefEntityID getAppdefEntityId() {
        return new AppdefEntityID(getAppdefType(), getAppdefId());
    }

    /**
     * Get the time that the alert definition last fired.
     */
    public long getLastFired() {
        return getAlertDefinitionState().getLastFired();
    }

    void setLastFired(long lastFired) {
        getAlertDefinitionState().setLastFired(lastFired);
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

        if (getEscalation() != null) {
            _value.setEscalationId(getEscalation().getId());
        } else {
            _value.setEscalationId(null);
        }

        _value.removeAllTriggers();
        for (Iterator i = getTriggers().iterator(); i.hasNext();) {
            RegisteredTrigger t = (RegisteredTrigger) i.next();
            _value.addTrigger(t.getRegisteredTriggerValue());
        }
        _value.cleanTrigger();

        _value.removeAllConditions();
        for (Iterator i = getConditions().iterator(); i.hasNext();) {
            AlertCondition c = (AlertCondition) i.next();

            _value.addCondition(c.getAlertConditionValue());
        }
        _value.cleanCondition();

        _value.removeAllActions();
        for (Iterator i = getActions().iterator(); i.hasNext();) {
            Action a = (Action) i.next();

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
