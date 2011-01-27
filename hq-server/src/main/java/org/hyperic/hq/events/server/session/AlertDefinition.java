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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertValue;

@MappedSuperclass
abstract public class AlertDefinition implements AlertDefinitionInterface, PerformsEscalations,
    ContainerManagedTimestampTrackable {

    @Column(name = "NAME", length = 255, nullable = false)
    private String name;

    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @Column(name = "MTIME", nullable = false)
    private long mtime;

    @Column(name = "DESCRIPTION", length = 250)
    private String description;

    @Column(name = "PRIORITY", nullable = false)
    private int priority; // XXX -- Needs to default to 1

    @Column(name = "ACTIVE", nullable = false)
    private boolean active; // XXX -- Needs to default to true

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled; // XXX -- Needs to default to true

    @Column(name = "FREQUENCY_TYPE", nullable = false)
    private int frequencyType;

    @Column(name = "COUNT")
    private Long count;

    @Column(name = "TRANGE")
    private Long range;

    @Column(name = "WILL_RECOVER", nullable = false)
    private boolean willRecover;

    @Column(name = "NOTIFY_FILTERED", nullable = false)
    private boolean notifyFiltered;

    @Column(name = "CONTROL_FILTERED", nullable = false)
    private boolean controlFiltered;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @ManyToOne
    @JoinColumn(name = "ESCALATION_ID")
    private Escalation escalation;

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

    Alert createAlert(AlertValue val) {
        Alert res = new Alert(this, val);
        return res;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCtime() {
        return ctime;
    }

    void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getMtime() {
        return mtime;
    }

    void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the same thing as getPriority(), though a typesafe enum
     */
    public AlertSeverity getSeverity() {
        return AlertSeverity.findByCode(getPriority());
    }

    public int getPriority() {
        return priority;
    }

    void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Check if an alert definition is enabled.
     * 
     * @return <code>true</code> if the alert definition is enabled;
     *         <code>false</code> if disabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if an alert definition is active.
     * 
     * @return <code>true</code> if the alert definition is active;
     *         <code>false</code> if inactive.
     */
    public boolean isActive() {
        return active;
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
        this.active = active;
    }

    /**
     * For Hibernate persistence only. Do not call directly.
     */
    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getFrequencyType() {
        return frequencyType;
    }

    void setFrequencyType(int frequencyType) {
        this.frequencyType = frequencyType;
    }

    public long getCount() {
        return count != null ? count.longValue() : 0;
    }

    void setCount(Long count) {
        this.count = count;
    }

    public long getRange() {
        return range != null ? range.longValue() : 0;
    }

    void setRange(Long range) {
        this.range = range;
    }

    public boolean willRecover() {
        return willRecover;
    }

    public boolean isWillRecover() {
        return willRecover;
    }

    void setWillRecover(boolean willRecover) {
        this.willRecover = willRecover;
    }

    public boolean isNotifyFiltered() {
        return notifyFiltered;
    }

    void setNotifyFiltered(boolean notifyFiltered) {
        this.notifyFiltered = notifyFiltered;
    }

    public boolean isControlFiltered() {
        return controlFiltered;
    }

    void setControlFiltered(boolean controlFiltered) {
        this.controlFiltered = controlFiltered;
    }

    public Escalation getEscalation() {
        return escalation;
    }

    void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }

    //TODO is deleted only applicable to Resource alert defs?
    public boolean isDeleted() {
        return deleted;
    }

    void setDeleted(boolean deleted) {
        this.deleted = deleted;
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
    
    abstract Collection<Action> getActionsBag();
    
    abstract Collection<AlertCondition> getConditionsBag();
    
    void addCondition(AlertCondition c) {
        getConditionsBag().add(c);
    }
    
    void removeCondition(AlertCondition c) {
        getConditionsBag().remove(c);
    }
    
    void addAction(Action a) {
        getActionsBag().add(a);
    }

    void removeAction(Action a) {
        getActionsBag().remove(a);
    }
    
    public Collection<AlertCondition> getConditions() {
        return Collections.unmodifiableCollection(getConditionsBag());
    }
    
    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(getActionsBag());
    }
     
    Action createAction(String className, byte[] config, Action parent) {
        Action res = new Action(className, config, parent);
        getActionsBag().add(res);
        return res;
    }

    AlertCondition createCondition(AlertConditionValue condVal, RegisteredTrigger trigger) {
        AlertCondition res = new AlertCondition(condVal, trigger);
        getConditionsBag().add(res);
        return res;
    }
    
    void clearActions() {
        //TODO was this necessary?
//        for (Action act : getActionsBag()) {
//            act.setAlertDefinition(null);
//        }
        getActionsBag().clear();
    }
    
    void clearConditions() {
        for (Iterator it = getConditionsBag().iterator(); it.hasNext();) {
            AlertCondition cond = (AlertCondition) it.next();
            //TODO was this necessary?
            //cond.setAlertDefinition(null);
            cond.setTrigger(null);
        }
        getConditionsBag().clear();
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
    
    public AlertDefinitionValue getAlertDefinitionValue() {
        AlertDefinitionValue value = new AlertDefinitionValue();
        value.setId(getId());
        value.setName(getName() == null ? "" : getName());
        value.setCtime(getCtime());
        value.setMtime(getMtime());
        value.setDescription(getDescription());
        value.setEnabled(isEnabled());
        value.setActive(isActive());
        value.setWillRecover(isWillRecover());
        value.setNotifyFiltered(isNotifyFiltered());
        value.setControlFiltered(isControlFiltered());
        value.setPriority(getPriority());
        value.setFrequencyType(getFrequencyType());
        value.setCount(getCount());
        value.setRange(getRange());
        value.setDeleted(isDeleted());

        if (getEscalation() != null) {
            value.setEscalationId(getEscalation().getId());
        } else {
            value.setEscalationId(null);
        }

        value.removeAllConditions();
        for (Iterator i = getConditions().iterator(); i.hasNext();) {
            AlertCondition c = (AlertCondition) i.next();

            value.addCondition(c.getAlertConditionValue());
        }
        value.cleanCondition();

        value.removeAllActions();
        for (Iterator i = getActions().iterator(); i.hasNext();) {
            Action a = (Action) i.next();

            value.addAction(a.getActionValue());
        }
        value.cleanAction();
        return value;
    }

}
