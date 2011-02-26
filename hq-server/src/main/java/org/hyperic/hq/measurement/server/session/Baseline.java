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
@Table(name="EAM_MEASUREMENT_BL")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Baseline implements Serializable {
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @ManyToOne
    @JoinColumn(name="MEASUREMENT_ID",unique=true)
    @Index(name="METRIC_BASELINE_CALCULATED_IDX")
    @Fetch(FetchMode.JOIN)
    private Measurement measurement;
    
    @Column(name="COMPUTE_TIME",nullable=false)
    @Index(name="METRIC_BASELINE_CALCULATED_IDX")
    private long computeTime;
    
    @Column(name="USER_ENTERED",nullable=false)
    private boolean userEntered = false;
    
    @Column(name="MEAN")
    private Double mean;
    
    @Column(name="MIN_EXPECTED_VAL")
    private Double minExpectedVal;
    
    @Column(name="MAX_EXPECTED_VAL")
    private Double maxExpectedVal;

    public Baseline() {
    }

    public Baseline(Measurement measurement, long computeTime,
                    boolean userEntered, Double mean, Double minExpectedVal,
                    Double maxExpectedVal) {
        this.measurement = measurement;
        this.computeTime = computeTime;
        this.userEntered = userEntered;
        this.mean = mean;
        this.minExpectedVal = minExpectedVal;
        this.maxExpectedVal = maxExpectedVal;
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

    // Property accessors
    public Measurement getMeasurement() {
        return measurement;
    }
    
    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    public long getComputeTime() {
        return computeTime;
    }
    
    public void setComputeTime(long computeTime) {
        this.computeTime = computeTime;
    }

    public boolean isUserEntered() {
        return userEntered;
    }
    
    public void setUserEntered(boolean userEntered) {
        this.userEntered = userEntered;
    }

    public Double getMean() {
        return mean;
    }
    
    protected void setMean(Double mean) {
        this.mean = mean;
    }

    public Double getMinExpectedVal() {
        return minExpectedVal;
    }
    
    protected void setMinExpectedVal(Double minExpectedVal) {
        this.minExpectedVal = minExpectedVal;
    }

    public Double getMaxExpectedVal() {
        return maxExpectedVal;
    }
    
    protected void setMaxExpectedVal(Double maxExpectedVal) {
        this.maxExpectedVal = maxExpectedVal;
    }

    /**
     * Update a Baseline
     */
    public void update(long computeTime, boolean userEntered,
                       Double mean, Double minExpectedValue,
                       Double maxExpectedValue) {
        setComputeTime(computeTime);
        setUserEntered(userEntered);
        setMean(mean);
        setMinExpectedVal(minExpectedValue);
        setMaxExpectedVal(maxExpectedValue);
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Baseline)) {
            return false;
        }
        Integer objId = ((Baseline)obj).getId();
  
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


