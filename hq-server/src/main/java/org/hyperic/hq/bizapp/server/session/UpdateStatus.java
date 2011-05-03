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

package org.hyperic.hq.bizapp.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(name = "EAM_UPDATE_STATUS")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UpdateStatus implements Serializable {
    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_UPDATE_STATUS_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "IGNORED", nullable = false)
    private boolean ignored;

    @Column(name = "REPORT", length = 4000)
    private String report;

    @Column(name = "UPMODE", nullable = false)
    private int updateMode;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected UpdateStatus() {
    }

    UpdateStatus(String report, UpdateStatusMode mode) {
        this.report = report;
        updateMode = mode.getCode();
        ignored = false;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof UpdateStatus)) {
            return false;
        }
        Integer objId = ((UpdateStatus) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    public Integer getId() {
        return id;
    }

    public UpdateStatusMode getMode() {
        return UpdateStatusMode.findByCode(updateMode);
    }

    public String getReport() {
        return report;
    }

    protected int getUpdateMode() {
        return updateMode;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    void setMode(UpdateStatusMode mode) {
        updateMode = mode.getCode();
    }

    protected void setReport(String report) {
        this.report = report;
    }

    protected void setUpdateModeEnum(int mode) {
        updateMode = mode;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
