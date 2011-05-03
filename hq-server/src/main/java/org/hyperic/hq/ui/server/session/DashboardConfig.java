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

package org.hyperic.hq.ui.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Parameter;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.util.config.ConfigResponse;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "EAM_DASH_CONFIG")
@DiscriminatorColumn(name = "CONFIG_TYPE", length = 255)
public abstract class DashboardConfig implements Serializable {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CRISPO_ID", nullable = false)
    @Index(name = "DASH_CONFIG_CRISPO_ID_IDX")
    private Crispo config;

    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_DASH_CONFIG_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected DashboardConfig() {
    }

    protected DashboardConfig(String name, Crispo config) {
        this.name = name;
        this.config = config;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o == null || o instanceof DashboardConfig == false)
            return false;

        DashboardConfig oe = (DashboardConfig) o;

        if (!getName().equals(oe.getName()))
            return false;

        if (getCrispo().getId() != oe.getCrispo().getId())
            return false;

        return true;
    }

    public ConfigResponse getConfig() {
        return config.toResponse();
    }

    protected Crispo getCrispo() {
        return config;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int hash = 17;

        hash = hash * 37 + getName().hashCode();
        hash = hash * 37 + (getCrispo() != null ? getCrispo().hashCode() : 0);
        return hash;
    }

    public abstract boolean isEditable(AuthzSubject by);

    protected void setCrispo(Crispo config) {
        this.config = config;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setName(String n) {
        name = n;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
