/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.appdef.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.appdef.galerts.ResourceAuxLog;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;

@Entity
@Table(name = "EAM_RESOURCE_AUX_LOGS")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ResourceAuxLogPojo implements Serializable {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AUX_LOG_ID", nullable = false)
    @Index(name = "METRIC_AUX_LOG_ID_IDX")
    private GalertAuxLog auxLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEF_ID", nullable = false)
    @Index(name = "RSRC_AUX_LOG_IDX")
    private GalertDef def;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "RESOURCE_ID", nullable = false)
    private int resourceId;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected ResourceAuxLogPojo() {
    }

    public ResourceAuxLogPojo(GalertAuxLog log, ResourceAuxLog logInfo, GalertDef def) {
        auxLog = log;
        resourceId = logInfo.getEntity().getID();
        this.def = def;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o == null || o instanceof ResourceAuxLogPojo == false)
            return false;

        ResourceAuxLogPojo oe = (ResourceAuxLogPojo) o;

        return oe.getAuxLog().equals(getAuxLog()) && oe.getResourceId().equals(getResourceId());
    }

    public GalertDef getAlertDef() {
        return def;
    }

    public GalertAuxLog getAuxLog() {
        return auxLog;
    }

    public Integer getId() {
        return id;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + getAuxLog().hashCode();
        hash = hash * 31 + getResourceId().hashCode();
        return hash;
    }

    protected void setAlertDef(GalertDef def) {
        this.def = def;
    }

    protected void setAuxLog(GalertAuxLog log) {
        auxLog = log;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}