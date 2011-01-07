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

import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.inventory.domain.Resource;

@Entity
@Table(name = "EAM_MEASUREMENT")
public class Measurement implements ContainerManagedTimestampTrackable, Serializable {
    
    //TODO don't really need instanceId anymore b/c we have Resource
    @Column(name = "INSTANCE_ID", nullable = false)
    private int instanceId;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_ID")
    private MeasurementTemplate template;

    @Column(name = "MTIME", nullable = false)
    private long mtime;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled=true;

    @Column(name = "COLL_INTERVAL", nullable = false)
    private long interval;

    @Column(name = "DSN", nullable = false, length = 2048)
    private String dsn;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="MEASUREMENT_ID")
    private Collection<Baseline> baselinesBag;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="MEASUREMENT_ID")
    private Collection<AvailabilityDataRLE> availabilityData;

    @ManyToOne
    @JoinColumn(name = "RESOURCE_ID")
    private Resource resource;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "VERSION_COL")
    @Version
    private Long version;

    public Measurement() {
    }

    public Measurement(Integer instanceId, MeasurementTemplate template) {
        this.instanceId = instanceId;
        this.template = template;
    }

    public Measurement(Integer instanceId, MeasurementTemplate template, long interval) {
        this(instanceId, template);
        this.interval = interval;
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>false</code> by default.
     */
    public boolean allowContainerManagedCreationTime() {
        return false;
    }

    /**
     * @see org.hyperic.hibernate.ContainerManagedTimestampTrackable#allowContainerManagedLastModifiedTime()
     * @return <code>true</code> by default.
     */
    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    public Integer getInstanceId() {
        return instanceId;
    }

    public MeasurementTemplate getTemplate() {
        return template;
    }

    protected void setTemplate(MeasurementTemplate template) {
        this.template = template;
    }

    public long getMtime() {
        return mtime;
    }

    protected void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public Resource getResource() {
        return resource;
    }

    void setResource(Resource resource) {
        this.resource = resource;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getInterval() {
        return interval;
    }

    protected void setInterval(long interval) {
        this.interval = interval;
    }

    public String getDsn() {
        return dsn;
    }

    protected void setDsn(String dsn) {
        this.dsn = dsn;
    }

    public AppdefEntityID getEntityId() {
        return new AppdefEntityID(getAppdefType(), getInstanceId());
    }

    public int getAppdefType() {
        return getTemplate().getMonitorableType().getAppdefType();
    }

    protected void setBaselinesBag(Collection<Baseline> baselines) {
        this.baselinesBag = baselines;
    }

    protected Collection<AvailabilityDataRLE> getAvailabilityData() {
        return availabilityData;
    }

    protected void setAvailabilityData(Collection<AvailabilityDataRLE> availabilityData) {
        this.availabilityData = availabilityData;
    }

    protected Collection<Baseline> getBaselinesBag() {
        return baselinesBag;
    }

    public Collection<Baseline> getBaselines() {
        return Collections.unmodifiableCollection(baselinesBag);
    }

    public void setBaseline(Baseline b) {
        final Collection<Baseline> baselines = getBaselinesBag();
        if (!baselines.isEmpty())
            baselines.clear();

        if (b != null)
            baselines.add(b);
    }

    public Baseline getBaseline() {
        if (getBaselinesBag().isEmpty())
            return null;
        else
            return (Baseline) getBaselinesBag().iterator().next();
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
    
    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }

    public String toString() {
        return getId().toString();
    }
}
