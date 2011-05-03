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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Parameter;
import org.hyperic.hq.alert.data.RegisteredTriggerRepository;
import org.hyperic.hq.common.EntityNotFoundException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.util.units.FormattedNumber;

@Entity
@Table(name = "EAM_ALERT_CONDITION")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AlertCondition implements Serializable {

    @Column(name = "COMPARATOR", length = 2)
    private String comparator;

    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_ALERT_CONDITION_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @OneToMany(mappedBy = "condition", cascade = CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<AlertConditionLog> logEntries;

    @Column(name = "MEASUREMENT_ID")
    private int measurementId;

    @Column(name = "NAME", length = 100)
    private String name;

    @Column(name = "OPTION_STATUS", length = 25)
    private String optionStatus;

    @Column(name = "REQUIRED", nullable = false)
    private boolean required;

    @Column(name = "THRESHOLD")
    private double threshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRIGGER_ID")
    @Index(name = "ALERT_COND_TRIGGER_ID_IDX")
    private RegisteredTrigger trigger;

    @Column(name = "TYPE", nullable = false)
    private int type;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    public AlertCondition() {
    }

    @SuppressWarnings("unchecked")
    AlertCondition(AlertConditionValue val, RegisteredTrigger trigger) {
        type = val.getType();
        required = val.getRequired();
        measurementId = val.getMeasurementId();
        name = val.getName();
        comparator = val.getComparator();
        threshold = val.getThreshold();
        optionStatus = val.getOption();
        this.trigger = trigger;
        logEntries = Collections.EMPTY_LIST;
    }

    public String describe(Measurement dm) {
        StringBuffer text = new StringBuffer();
        switch (getType()) {
            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
                text.append(getName()).append(" ").append(getComparator()).append(" ");

                if (getType() == EventConstants.TYPE_BASELINE) {
                    text.append(getThreshold());
                    text.append("% of ");

                    if (MeasurementConstants.BASELINE_OPT_MAX.equals(getOptionStatus())) {
                        text.append("Max Value");
                    } else if (MeasurementConstants.BASELINE_OPT_MIN.equals(getOptionStatus())) {
                        text.append("Min Value");
                    } else {
                        text.append("Baseline");
                    }
                } else {
                    FormattedNumber th = UnitsConvert.convert(getThreshold(), dm.getTemplate()
                        .getUnits());
                    text.append(th.toString());
                }
                break;
            case EventConstants.TYPE_CONTROL:
                text.append(getName());
                break;
            case EventConstants.TYPE_CHANGE:
                text.append(getName()).append(" value changed");
                break;
            case EventConstants.TYPE_CUST_PROP:
                text.append(getName()).append(" value changed");
                break;
            case EventConstants.TYPE_LOG:
                text.append("Event/Log Level(")
                    .append(ResourceLogEvent.getLevelString(Integer.parseInt(getName())))
                    .append(")");
                if (getOptionStatus() != null && getOptionStatus().length() > 0) {
                    text.append(" and matching substring ").append('"').append(getOptionStatus())
                        .append('"');
                }
                break;
            case EventConstants.TYPE_CFG_CHG:
                text.append("Config changed");
                if (getOptionStatus() != null && getOptionStatus().length() > 0) {
                    text.append(": ").append(getOptionStatus());
                }
                break;
            default:
                break;
        }

        return text.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AlertCondition)) {
            return false;
        }
        Integer objId = ((AlertCondition) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    public AlertConditionValue getAlertConditionValue() {
        AlertConditionValue value = new AlertConditionValue();
        value.setId(getId());
        value.setType(getType());
        value.setRequired(isRequired());
        value.setMeasurementId(getMeasurementId());
        value.setName(getName() == null ? "" : getName());
        value.setComparator(getComparator() == null ? "" : getComparator());
        value.setThreshold(getThreshold());
        value.setOption(getOptionStatus() == null ? "" : getOptionStatus());
        if (getTrigger() != null) {
            value.setTriggerId(getTrigger().getId());
        }
        return value;
    }

    public String getComparator() {
        return comparator;
    }

    public Integer getId() {
        return id;
    }

    public Collection<AlertConditionLog> getLogEntries() {
        return Collections.unmodifiableCollection(logEntries);
    }

    protected Collection<AlertConditionLog> getLogEntriesBag() {
        return logEntries;
    }

    public int getMeasurementId() {
        return measurementId;
    }

    public String getName() {
        return name;
    }

    public String getOptionStatus() {
        return optionStatus;
    }

    public double getThreshold() {
        return threshold;
    }

    public RegisteredTrigger getTrigger() {
        return trigger;
    }

    public int getType() {
        return type;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    public boolean isRequired() {
        return required;
    }

    public void setAlertConditionValue(AlertConditionValue val) {
        RegisteredTriggerRepository tDAO = Bootstrap.getBean(RegisteredTriggerRepository.class);

        setType(val.getType());
        setRequired(val.getRequired());
        setMeasurementId(val.getMeasurementId());
        setName(val.getName());
        setComparator(val.getComparator());
        setThreshold(val.getThreshold());
        setOptionStatus(val.getOption());
        RegisteredTrigger trigger = tDAO.findOne(val.getTriggerId());
        if(trigger == null) {
            throw new EntityNotFoundException("Registered Trigger with ID: " + 
                val.getTriggerId() + " was not found");
        }
        setTrigger(trigger);
    }

    protected void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setLogEntriesBag(Collection<AlertConditionLog> logEntries) {
        this.logEntries = logEntries;
    }

    protected void setMeasurementId(int measurementId) {
        this.measurementId = measurementId;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setOptionStatus(String optionStatus) {
        this.optionStatus = optionStatus;
    }

    protected void setRequired(boolean required) {
        this.required = required;
    }

    protected void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setTrigger(RegisteredTrigger trigger) {
        this.trigger = trigger;
    }

    protected void setType(int type) {
        this.type = type;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
