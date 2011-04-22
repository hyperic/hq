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

package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

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
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;

@Entity
@Table(name = "EAM_MEASUREMENT", uniqueConstraints = { @UniqueConstraint(columnNames = { "RESOURCE_ID",
                                                                                        "TEMPLATE_ID" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Measurement implements ContainerManagedTimestampTrackable, Serializable {

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "measurement")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<AvailabilityDataRLE> availabilityData;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "measurement")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<Baseline> baselinesBag = new HashSet<Baseline>();

    @Column(name = "DSN", nullable = false, length = 2048)
    private String dsn;

    @Column(name = "ENABLED", nullable = false)
    @Index(name = "MEAS_ENABLED_IDX")
    private boolean enabled = true;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "COLL_INTERVAL", nullable = false)
    private long interval;

    @Column(name = "MTIME", nullable = false)
    private long mtime;

    @Column(name = "RESOURCE_ID")
    @Index(name = "MEAS_RES_IDX")
    private Integer resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_ID", nullable = false)
    @Index(name = "MEAS_TEMPLATE_ID")
    private MeasurementTemplate template;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    public Measurement() {
    }

    public Measurement(Integer resource, MeasurementTemplate template) {
        this.resource = resource;
        this.template = template;
    }

    public Measurement(Integer resource, MeasurementTemplate template, long interval) {
        this(resource, template);
        this.interval = interval;
    }

    public boolean allowContainerManagedCreationTime() {
        return false;
    }

    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Measurement other = (Measurement) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    protected Collection<AvailabilityDataRLE> getAvailabilityData() {
        return availabilityData;
    }

    public Baseline getBaseline() {
        if (getBaselinesBag().isEmpty())
            return null;
        else
            return (Baseline) getBaselinesBag().iterator().next();
    }

    public Collection<Baseline> getBaselines() {
        return Collections.unmodifiableCollection(baselinesBag);
    }

    protected Collection<Baseline> getBaselinesBag() {
        return baselinesBag;
    }

    public String getDsn() {
        return dsn;
    }

    public Integer getId() {
        return id;
    }

    public long getInterval() {
        return interval;
    }

    public long getMtime() {
        return mtime;
    }

    public Integer getResource() {
        return resource;
    }

    public MeasurementTemplate getTemplate() {
        return template;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected void setAvailabilityData(Collection<AvailabilityDataRLE> availabilityData) {
        this.availabilityData = availabilityData;
    }

    protected void setBaselinesBag(Collection<Baseline> baselines) {
        this.baselinesBag = baselines;
    }
    
    public void setBaseline(Baseline b) {
        if (!baselinesBag.isEmpty()) {
            baselinesBag.clear();
        }
        if (b != null) {
            baselinesBag.add(b);
        }
    }


    public void setDsn(String dsn) {
        this.dsn = dsn;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setInterval(long interval) {
        this.interval = interval;
    }

    protected void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public void setResource(Integer resource) {
        this.resource = resource;
    }

    protected void setTemplate(MeasurementTemplate template) {
        this.template = template;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String toString() {
        return getId().toString();
    }

}
