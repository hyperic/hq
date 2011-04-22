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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Index;
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
import org.hyperic.hq.measurement.MeasurementConstants;


@MappedSuperclass
abstract public class AlertDefinition implements AlertDefinitionInterface, PerformsEscalations,
    ContainerManagedTimestampTrackable, Serializable {

    @Column(name = "ACTIVE", nullable = false)
    private boolean active; // XXX -- Needs to default to true

    @Column(name = "CONTROL_FILTERED", nullable = false)
    private boolean controlFiltered;

    @Column(name = "COUNTER")
    private Long count;

    @Column(name = "CTIME", nullable = false)
    private long ctime;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @Column(name = "DESCRIPTION", length = 250)
    private String description;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled; // XXX -- Needs to default to true

    @ManyToOne
    @JoinColumn(name = "ESCALATION_ID")
    @Index(name = "ALERT_DEF_ESC_ID_IDX")
    private Escalation escalation;

    @Column(name = "FREQUENCY_TYPE", nullable = false)
    private int frequencyType;

    @Column(name = "MTIME", nullable = false)
    private long mtime;

    @Column(name = "NAME", length = 255, nullable = false)
    private String name;

    @Column(name = "NOTIFY_FILTERED", nullable = false)
    private boolean notifyFiltered;

    @Column(name = "PRIORITY", nullable = false)
    @Index(name = "ALERT_DEF_CHILD_IDX")
    private int priority; // XXX -- Needs to default to 1

    @Column(name = "TRANGE")
    private Long range;

    @Column(name = "WILL_RECOVER", nullable = false)
    private boolean willRecover;

    public AlertDefinition() {
    }

    public void addAction(Action a) {
        getActionsBag().add(a);
    }

    public void addCondition(AlertCondition c) {
        getConditionsBag().add(c);
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

    void clearActions() {
        // TODO was this necessary?
        // for (Action act : getActionsBag()) {
        // act.setAlertDefinition(null);
        // }
        getActionsBag().clear();
    }

    void clearConditions() {
        for (AlertCondition cond : getConditionsBag()) {
            // TODO was this necessary?
            // cond.setAlertDefinition(null);
            cond.setTrigger(null);
        }
        getConditionsBag().clear();
    }

    Action createAction(String className, byte[] config, Action parent) {
        Action res = new Action(className, config, parent);
        getActionsBag().add(res);
        return res;
    }

    Alert createAlert(AlertValue val) {
        Alert res = new Alert(this, val);
        return res;
    }

    AlertCondition createCondition(AlertConditionValue condVal, RegisteredTrigger trigger) {
        AlertCondition res = new AlertCondition(condVal, trigger);
        getConditionsBag().add(res);
        return res;
    }

    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(getActionsBag());
    }

    abstract Collection<Action> getActionsBag();

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
        for (AlertCondition c : getConditions()) {
            value.addCondition(c.getAlertConditionValue());
        }
        value.cleanCondition();

        value.removeAllActions();
        for (Action a : getActions()) {
            value.addAction(a.getActionValue());
        }
        value.cleanAction();
        return value;
    }

    public EscalationAlertType getAlertType() {
        return ClassicEscalationAlertType.CLASSIC;
    }

    public Collection<AlertCondition> getConditions() {
        return Collections.unmodifiableCollection(getConditionsBag());
    }

    abstract Collection<AlertCondition> getConditionsBag();

    public long getCount() {
        return count != null ? count.longValue() : 0;
    }

    public long getCtime() {
        return ctime;
    }

    public AlertDefinitionInterface getDefinitionInfo() {
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Escalation getEscalation() {
        return escalation;
    }

    public int getFrequencyType() {
        return frequencyType;
    }

    public long getMtime() {
        return mtime;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public long getRange() {
        return range != null ? range.longValue() : 0;
    }

    /**
     * Returns the same thing as getPriority(), though a typesafe enum
     */
    public AlertSeverity getSeverity() {
        return AlertSeverity.findByCode(getPriority());
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
        if (getConditions().size() == 1) {
            for ( AlertCondition cond : getConditions()) {
                if (cond != null
                    && MeasurementConstants.CAT_AVAILABILITY.equalsIgnoreCase(cond.getName())) {
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

    public boolean isControlFiltered() {
        return controlFiltered;
    }

    // TODO is deleted only applicable to Resource alert defs?
    public boolean isDeleted() {
        return deleted;
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

    public boolean isNotifyFiltered() {
        return notifyFiltered;
    }

    /**
     * Check if the alert definition is a recovery alert
     */
    public boolean isRecoveryDefinition() {
        for (AlertCondition cond : getConditionsBag()) {
            if (cond.getType() == EventConstants.TYPE_ALERT) {
                return true;
            }
        }
        return false;
    }

    public boolean isWillRecover() {
        return willRecover;
    }

    public boolean performsEscalations() {
        return true;
    }

    void removeAction(Action a) {
        getActionsBag().remove(a);
    }

    void removeCondition(AlertCondition c) {
        getConditionsBag().remove(c);
    }

    /**
     * For Hibernate persistence only. Do not call directly.
     */
    void setActive(boolean active) {
        this.active = active;
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

    void setControlFiltered(boolean controlFiltered) {
        this.controlFiltered = controlFiltered;
    }

    void setCount(Long count) {
        this.count = count;
    }

    void setCtime(long ctime) {
        this.ctime = ctime;
    }

    void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    void setDescription(String description) {
        this.description = description;
    }

    /**
     * For Hibernate persistence only. Do not call directly.
     */
    void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }

    void setFrequencyType(int frequencyType) {
        this.frequencyType = frequencyType;
    }

    void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public void setName(String name) {
        this.name = name;
    }

    void setNotifyFiltered(boolean notifyFiltered) {
        this.notifyFiltered = notifyFiltered;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    void setRange(Long range) {
        this.range = range;
    }

    void setWillRecover(boolean willRecover) {
        this.willRecover = willRecover;
    }

    public String toString() {
        return "alertDef [" + this.getName() + "]";
    }
    
    public boolean willRecover() {
        return willRecover;
    }


}
