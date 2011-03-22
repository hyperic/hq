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
package org.hyperic.hq.escalation.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.auth.domain.AuthzSubject;

/**
 * The escalation state ties an escalation chain to an alert definition.
 */
@Entity
@Table(name = "EAM_ESCALATION_STATE", uniqueConstraints = { @UniqueConstraint(name = "alert_def_id_key", columnNames = { "ALERT_DEF_ID",
                                                                                                                        "ALERT_TYPE" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EscalationState implements Serializable {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACKNOWLEDGED_BY")
    @Index(name = "ACKNOWLEDGED_BY_IDX")
    private AuthzSubject acknowledgedBy;

    @Column(name = "ALERT_DEF_ID", nullable = false)
    private int alertDefId;

    @Column(name = "ALERT_ID", nullable = false)
    private int alertId;

    @Column(name = "ALERT_TYPE", nullable = false)
    private int alertType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ESCALATION_ID", nullable = false)
    private Escalation escalation;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NEXT_ACTION_IDX", nullable = false)
    private int nextAction;

    @Column(name = "NEXT_ACTION_TIME", nullable = false)
    private long nextActionTime;

    private transient boolean paused;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected EscalationState() {
    }

    public EscalationState(Escalatable alert) {
        PerformsEscalations def = alert.getDefinition();
        escalation = def.getEscalation();
        nextAction = 0;
        nextActionTime = System.currentTimeMillis();
        alertDefId = def.getId().intValue();
        alertType = def.getAlertType().getCode();
        alertId = alert.getId().intValue();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof EscalationState)) {
            return false;
        }
        Integer objId = ((EscalationState) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    public AuthzSubject getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public int getAlertDefinitionId() {
        return alertDefId;
    }

    public int getAlertId() {
        return alertId;
    }

    public EscalationAlertType getAlertType() {
        return EscalationAlertType.findByCode(alertType);
    }

    public Escalation getEscalation() {
        return escalation;
    }

    public Integer getId() {
        return id;
    }

    public int getNextAction() {
        return nextAction;
    }

    public long getNextActionTime() {
        return nextActionTime;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setAcknowledgedBy(AuthzSubject subject) {
        acknowledgedBy = subject;
    }

    protected void setAlertDefinitionId(int alertDefinitionId) {
        alertDefId = alertDefinitionId;
    }

    protected void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    protected void setAlertType(int typeCode) {
        alertType = typeCode;
    }

    protected void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setNextAction(int nextAction) {
        this.nextAction = nextAction;
    }

    protected void setNextActionTime(long nextActionTime) {
        this.nextActionTime = nextActionTime;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
