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

package org.hyperic.hq.galerts.server.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.server.session.Action;

@Entity
@Table(name = "EAM_GALERT_LOGS")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GalertLog implements AlertInterface, Serializable {
    public static int MAX_LONG_REASON = 2048;
    public static int MAX_SHORT_REASON = 256;

    @OneToMany(mappedBy = "galertLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<GalertActionLog> actionLog = new ArrayList<GalertActionLog>();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<GalertAuxLog> auxLogs = new ArrayList<GalertAuxLog>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEF_ID", nullable = false)
    @Index(name = "GALERT_LOGS_DEF_ID_IDX")
    private GalertDef def;

    @Column(name = "FIXED", nullable = false)
    private boolean fixed;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "LONG_REASON", nullable = false, length = 2048)
    private String longReason;

    private transient GalertDefPartition partition;

    @SuppressWarnings("unused")
    @Column(name = "PARTITION", nullable = false)
    private int partitionEnum;

    @Column(name = "SHORT_REASON", nullable = false, length = 256)
    private String shortReason;

    @Column(name = "TIMESTAMP", nullable = false)
    @Index(name = "EAM_GALERT_LOGS_TIME_IDX")
    private long timestamp;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected GalertLog() {
    }

    public GalertLog(GalertDef def, ExecutionReason reason, long timestamp) {
        if (reason.getShortReason().length() > MAX_SHORT_REASON ||
            reason.getLongReason().length() > MAX_LONG_REASON) {
            throw new IllegalArgumentException("Reason is too long");
        }

        this.def = def;
        this.timestamp = timestamp;
        shortReason = reason.getShortReason();
        longReason = reason.getLongReason();
        partition = reason.getPartition();
    }

    GalertAuxLog addAuxLog(AlertAuxLog auxLog, GalertAuxLog parent) {
        GalertAuxLog res = new GalertAuxLog(this, auxLog, parent);

        getAuxLogBag().add(res);
        return res;
    }

    GalertActionLog createActionLog(String detail, Action action, AuthzSubject subject) {
        GalertActionLog res = new GalertActionLog(this, detail, action, subject);

        actionLog.add(res);
        return res;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o == null || o instanceof GalertLog == false)
            return false;

        GalertLog oe = (GalertLog) o;

        return oe.getTimestamp() == getTimestamp() && oe.getShortReason().equals(getShortReason());
    }

    public Collection<GalertActionLog> getActionLog() {
        return Collections.unmodifiableCollection(actionLog);
    }

    protected Collection<GalertActionLog> getActionLogBag() {
        return actionLog;
    }

    public GalertDef getAlertDef() {
        return def;
    }

    public AlertDefinitionInterface getAlertDefinitionInterface() {
        return getAlertDef();
    }

    protected List<GalertAuxLog> getAuxLogBag() {
        return auxLogs;
    }

    /**
     * Gets the {@link GalertAuxLog}s associated with this alert.
     */
    public List<GalertAuxLog> getAuxLogs() {
        return Collections.unmodifiableList(getAuxLogBag());
    }

    public PerformsEscalations getDefinition() {
        return getAlertDef();
    }

    public Integer getId() {
        return id;
    }

    public String getLongReason() {
        return longReason;
    }

    protected int getPartitionEnum() {
        return partition.getCode();
    }

    public String getShortReason() {
        return shortReason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + (int) getTimestamp();
        hash = hash * 31 + getShortReason().hashCode();
        return hash;
    }

    public boolean isFixed() {
        return fixed;
    }

    protected void setActionLogBag(Collection<GalertActionLog> actionLog) {
        this.actionLog = actionLog;
    }

    protected void setAlertDef(GalertDef def) {
        this.def = def;
    }

    protected void setAuxLogBag(List<GalertAuxLog> auxLogs) {
        this.auxLogs = auxLogs;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setLongReason(String txt) {
        longReason = txt;
    }

    protected void setPartitionEnum(int code) {
        partition = GalertDefPartition.findByCode(code);
    }

    protected void setShortReason(String txt) {
        shortReason = txt;
    }

    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
