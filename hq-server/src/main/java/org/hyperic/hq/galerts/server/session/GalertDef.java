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
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;

@Entity
@Table(name="EAM_GALERT_DEFS")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class GalertDef implements AlertDefinitionInterface, PerformsEscalations, Serializable {
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @Column(name="NAME",nullable=false)
    private String name;
    
    @Column(name="DESCR")
    private String desc;
    
    @Column(name="SEVERITY",nullable=false)
    @SuppressWarnings("unused")
    private int severityEnum;
    
    private transient AlertSeverity severity;
    
    @Column(name="ENABLED",nullable=false)
    private boolean enabled;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="GROUP_ID",nullable=false)
    @Index(name="GALERT_DEFS_GROUP_ID_IDX")
    private ResourceGroup group;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ESCALATION_ID")
    @Index(name="GALERT_DEFS_ESC_ID_IDX")
    private Escalation escalation;
    
    @OneToMany(mappedBy="alertDef",cascade=CascadeType.ALL,orphanRemoval=true)
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    private Set<ExecutionStrategyInfo> strategies = new HashSet<ExecutionStrategyInfo>();
    
    @Column(name="CTIME",nullable=false)
    private long creationTime;
    
    @Column(name="MTIME",nullable=false)
    private long modifiedTime;
    
    @Column(name="DELETED",nullable=false)
    private boolean deleted;
    
    @Column(name="LAST_FIRED")
    private Long lastFired;

    protected GalertDef() {
    }

    GalertDef(String name, String desc, AlertSeverity severity, boolean enabled,
              ResourceGroup group) {
        this.name = name;
        this.desc = desc;
        this.severity = severity;
        this.enabled = enabled;
        this.group = group;
        escalation = null;
        creationTime = System.currentTimeMillis();
        modifiedTime = creationTime;
        deleted = false;
        lastFired = null;
    }
    
    

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    ExecutionStrategyInfo addPartition(GalertDefPartition partition,
                                       ExecutionStrategyTypeInfo stratType, Crispo stratConfig) {
        ExecutionStrategyInfo strat;

        if (findStrategyByPartition(partition) != null) {
            throw new IllegalStateException("Partition[" + partition + "] " +
                                            "already created");
        }

        strat = stratType.createStrategyInfo(this, stratConfig, partition);
        getStrategySet().add(strat);
        return strat;
    }

    private ExecutionStrategyInfo findStrategyByPartition(GalertDefPartition p) {
        for (Iterator<ExecutionStrategyInfo> i = getStrategies().iterator(); i.hasNext();) {
            ExecutionStrategyInfo strat = (ExecutionStrategyInfo) i.next();

            if (strat.getPartition().equals(p))
                return strat;
        }
        return null;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return desc;
    }

    protected void setDescription(String desc) {
        this.desc = desc;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected int getSeverityEnum() {
        return severity.getCode();
    }

    protected void setSeverityEnum(int code) {
        severity = AlertSeverity.findByCode(code);
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    void setSeverity(AlertSeverity severity) {
        setSeverityEnum(severity.getCode());
    }

    public ResourceGroup getGroup() {
        return group;
    }

    protected void setGroup(ResourceGroup group) {
        this.group = group;
    }

    public Escalation getEscalation() {
        return escalation;
    }

    protected void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }

    protected Set<ExecutionStrategyInfo> getStrategySet() {
        return strategies;
    }

    protected void setStrategySet(Set<ExecutionStrategyInfo> strategies) {
        this.strategies = strategies;
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

    public AppdefEntityID getAppdefID() {
        return AppdefUtil.newAppdefEntityId(getResource());
    }

    public int getAppdefId() {
        return getGroup().getId().intValue();
    }

    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_GROUP;
    }

    public int getPriority() {
        return getSeverity().getCode();
    }

    public boolean isNotifyFiltered() {
        return false;
    }

    public long getCtime() {
        return creationTime;
    }

    protected void setCtime(long ctime) {
        creationTime = ctime;
    }

    public long getMtime() {
        return modifiedTime;
    }

    protected void setMtime(long mtime) {
        modifiedTime = mtime;
    }

    protected void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    protected void setLastFired(Long l) {
        lastFired = l;
    }

    public Long getLastFired() {
        return lastFired;
    }

    public EscalationAlertType getAlertType() {
        return GalertEscalationAlertType.GALERT;
    }

    public AlertDefinitionInterface getDefinitionInfo() {
        return this;
    }

    public boolean performsEscalations() {
        return true;
    }

    public String toString() {
        return getName();
    }

    public Resource getResource() {
        return getGroup();
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof GalertDef)) {
            return false;
        }
        Integer objId = ((GalertDef)obj).getId();
  
        return getId() == objId ||
        (getId() != null && 
         objId != null && 
         getId().equals(objId));     
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + (getId() != null ? getId().hashCode() : 0);
        return result;      
    }

}
