/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.galerts.server.session;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertSeverity;

@Entity
@Table(name = "EAM_GALERT_DEFS")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GalertDef implements AlertDefinitionInterface, PerformsEscalations, Serializable {

    @Column(name = "CTIME", nullable = false)
    private long creationTime;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @Column(name = "DESCR")
    private String desc;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ESCALATION_ID")
    @Index(name = "GALERT_DEFS_ESC_ID_IDX")
    private Escalation escalation;

    @Column(name = "GROUP_ID", nullable = false)
    @Index(name = "GALERT_DEFS_GROUP_ID_IDX")
    private Integer group;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "LAST_FIRED")
    private Long lastFired;

    @Column(name = "MTIME", nullable = false)
    private long modifiedTime;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "SEVERITY", nullable = false)
    private int severity;

    @OneToMany(mappedBy = "alertDef", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<ExecutionStrategyInfo> strategies = new HashSet<ExecutionStrategyInfo>();

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected GalertDef() {
    }

    public GalertDef(String name, String desc, AlertSeverity severity, boolean enabled,
                     Integer group) {
        this.name = name;
        this.desc = desc;
        this.severity=severity.getCode();
        this.enabled = enabled;
        this.group = group;
        escalation = null;
        creationTime = System.currentTimeMillis();
        modifiedTime = creationTime;
        deleted = false;
        lastFired = null;
    }

    ExecutionStrategyInfo addPartition(GalertDefPartition partition,
                                       ExecutionStrategyTypeInfo stratType, Crispo stratConfig) {
        ExecutionStrategyInfo strat;

        if (findStrategyByPartition(partition) != null) {
            throw new IllegalStateException("Partition[" + partition + "] " + "already created");
        }

        strat = stratType.createStrategyInfo(this, stratConfig, partition);
        getStrategySet().add(strat);
        return strat;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof GalertDef)) {
            return false;
        }
        Integer objId = ((GalertDef) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    private ExecutionStrategyInfo findStrategyByPartition(GalertDefPartition p) {
        for (Iterator<ExecutionStrategyInfo> i = getStrategies().iterator(); i.hasNext();) {
            ExecutionStrategyInfo strat = (ExecutionStrategyInfo) i.next();

            if (strat.getPartition().equals(p))
                return strat;
        }
        return null;
    }

    public EscalationAlertType getAlertType() {
        return GalertEscalationAlertType.GALERT;
    }

    public long getCtime() {
        return creationTime;
    }

    public AlertDefinitionInterface getDefinitionInfo() {
        return this;
    }

    public String getDescription() {
        return desc;
    }

    public Escalation getEscalation() {
        return escalation;
    }

    public Integer getGroup() {
        return group;
    }

    public Integer getId() {
        return id;
    }

    public Long getLastFired() {
        return lastFired;
    }

    public long getMtime() {
        return modifiedTime;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return getSeverity().getCode();
    }

    public Integer getResource() {
        return getGroup();
    }

    public AlertSeverity getSeverity() {
        return AlertSeverity.findByCode(severity);
    }

    public Set<ExecutionStrategyInfo> getStrategies() {
        return Collections.unmodifiableSet(strategies);
    }

    public ExecutionStrategyInfo getStrategy(GalertDefPartition partition) {
        for (Iterator<ExecutionStrategyInfo> i = getStrategies().iterator(); i.hasNext();) {
            ExecutionStrategyInfo s = (ExecutionStrategyInfo) i.next();

            if (s.getPartition().equals(partition))
                return s;
        }
        return null;
    }

    protected Set<ExecutionStrategyInfo> getStrategySet() {
        return strategies;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isNotifyFiltered() {
        return false;
    }

    public boolean performsEscalations() {
        return true;
    }

    protected void setCtime(long ctime) {
        creationTime = ctime;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    protected void setDescription(String desc) {
        this.desc = desc;
    }

    protected void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }

    protected void setGroup(Integer group) {
        this.group = group;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setLastFired(Long l) {
        lastFired = l;
    }

    protected void setMtime(long mtime) {
        modifiedTime = mtime;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setSeverity(int code) {
        this.severity = code;
    }

    protected void setStrategySet(Set<ExecutionStrategyInfo> strategies) {
        this.strategies = strategies;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String toString() {
        return getName();
    }

}
