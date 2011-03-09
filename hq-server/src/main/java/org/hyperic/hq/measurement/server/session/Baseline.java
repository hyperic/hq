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

package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;

@Entity
@Table(name = "EAM_MEASUREMENT_BL")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Baseline implements Serializable {

    @Column(name = "COMPUTE_TIME", nullable = false)
    @Index(name = "METRIC_BASELINE_CALCULATED_IDX")
    private long computeTime;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "MAX_EXPECTED_VAL")
    private Double maxExpectedVal;

    @Column(name = "MEAN")
    private Double mean;

    @ManyToOne
    @JoinColumn(name = "MEASUREMENT_ID")
    @Index(name = "METRIC_BASELINE_CALCULATED_IDX")
    @Fetch(FetchMode.JOIN)
    private Measurement measurement;

    @Column(name = "MIN_EXPECTED_VAL")
    private Double minExpectedVal;

    @Column(name = "USER_ENTERED", nullable = false)
    private boolean userEntered = false;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    public Baseline() {
    }

    public Baseline(Measurement measurement, long computeTime, boolean userEntered, Double mean,
                    Double minExpectedVal, Double maxExpectedVal) {
        this.measurement = measurement;
        this.computeTime = computeTime;
        this.userEntered = userEntered;
        this.mean = mean;
        this.minExpectedVal = minExpectedVal;
        this.maxExpectedVal = maxExpectedVal;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Baseline)) {
            return false;
        }
        Integer objId = ((Baseline) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    public long getComputeTime() {
        return computeTime;
    }

    public Integer getId() {
        return id;
    }

    public Double getMaxExpectedVal() {
        return maxExpectedVal;
    }

    public Double getMean() {
        return mean;
    }

    // Property accessors
    public Measurement getMeasurement() {
        return measurement;
    }

    public Double getMinExpectedVal() {
        return minExpectedVal;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    public boolean isUserEntered() {
        return userEntered;
    }

    public void setComputeTime(long computeTime) {
        this.computeTime = computeTime;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setMaxExpectedVal(Double maxExpectedVal) {
        this.maxExpectedVal = maxExpectedVal;
    }

    protected void setMean(Double mean) {
        this.mean = mean;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    protected void setMinExpectedVal(Double minExpectedVal) {
        this.minExpectedVal = minExpectedVal;
    }

    public void setUserEntered(boolean userEntered) {
        this.userEntered = userEntered;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Update a Baseline
     */
    public void update(long computeTime, boolean userEntered, Double mean, Double minExpectedValue,
                       Double maxExpectedValue) {
        setComputeTime(computeTime);
        setUserEntered(userEntered);
        setMean(mean);
        setMinExpectedVal(minExpectedValue);
        setMaxExpectedVal(maxExpectedValue);
    }

}
