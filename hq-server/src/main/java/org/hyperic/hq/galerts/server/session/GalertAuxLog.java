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
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertAuxLogProvider;

@Entity
@Table(name = "EAM_GALERT_AUX_LOGS")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GalertAuxLog implements Serializable {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GALERT_ID", nullable = false)
    @Index(name = "AUX_LOG_GALERT_ID")
    private GalertLog alert;

    @Column(name = "AUXTYPE", nullable = false)
    private int auxType;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<GalertAuxLog> children;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEF_ID", nullable = false)
    @Index(name = "AUX_LOG_DEF_IDX")
    private GalertDef def;

    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT")
    @Index(name = "GALERT_AUX_LOGS_PARENT_IDX")
    private GalertAuxLog parent;

    @Column(name = "TIMESTAMP", nullable = false)
    private long timestamp;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected GalertAuxLog() {
    }

    public GalertAuxLog(GalertLog alert, AlertAuxLog log, GalertAuxLog parent) {
        timestamp = log.getTimestamp();
        this.alert = alert;
        if (log.getProvider() == null)
            auxType = 0;
        else
            auxType = log.getProvider().getCode();
        description = log.getDescription();
        this.parent = parent;
        children = new ArrayList<GalertAuxLog>();

        if (parent != null) {
            parent.getChildrenBag().add(this);
        }
        def = alert.getAlertDef();
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o == null || o instanceof GalertLog == false)
            return false;

        GalertAuxLog oe = (GalertAuxLog) o;

        return oe.getTimestamp() == getTimestamp() &&
               oe.getAlert().equals(getAlert()) &&
               oe.getDescription().equals(getDescription()) &&
               ((oe.getParent() == getParent()) || getParent() != null &&
                                                   getParent().equals(oe.getParent()));
    }

    public GalertLog getAlert() {
        return alert;
    }

    public GalertDef getAlertDef() {
        return def;
    }

    protected int getAuxType() {
        return auxType;
    }

    public Collection<GalertAuxLog> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    protected Collection<GalertAuxLog> getChildrenBag() {
        return children;
    }

    public String getDescription() {
        return description;
    }

    public Integer getId() {
        return id;
    }

    public GalertAuxLog getParent() {
        return parent;
    }

    public AlertAuxLogProvider getProvider() {
        return AlertAuxLogProvider.findByCode(getAuxType());
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
        hash = hash * 31 + getAlert().hashCode();
        hash = hash * 31 + getDescription().hashCode();
        hash = hash * 31 + (getParent() == null ? 0 : getParent().hashCode());
        return hash;
    }

    protected void setAlert(GalertLog alert) {
        this.alert = alert;
    }

    protected void setAlertDef(GalertDef def) {
        this.def = def;
    }

    protected void setAuxType(int auxType) {
        this.auxType = auxType;
    }

    protected void setChildrenBag(Collection<GalertAuxLog> c) {
        children = c;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setParent(GalertAuxLog parent) {
        this.parent = parent;
    }

    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
