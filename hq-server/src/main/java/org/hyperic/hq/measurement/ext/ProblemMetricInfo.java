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

package org.hyperic.hq.measurement.ext;

import java.io.Serializable;
import java.util.HashSet;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;

/**
 * Encapsulate measurement information for problem metrics
 */
public class ProblemMetricInfo implements Comparable, Serializable {
    public static final int FLAG_ALERTS     = 0x00001;
    public static final int FLAG_HIGH       = 0x00010;
    public static final int FLAG_LOW        = 0x00100;
    public static final int FLAG_OOB        = 0x01000;
    public static final int FLAG_OUTLIER    = 0x10000;
    
    private MeasurementTemplate tmpl;
    private double[] data;
    private int      alertCount = 0;
    private HashSet  aids       = new HashSet(1);      // Start with size of 1
    private int      probFlag   = 0;
    private int      overCount  = 0;
    private int      underCount = 0;
    private long     earliest   = Long.MAX_VALUE;
    private Integer  metricId   = new Integer(0);
    
    public ProblemMetricInfo() {
    }
    
    public ProblemMetricInfo(MeasurementTemplate tmpl){
        this.setMeasurementTemplate(tmpl);
    }

    public ProblemMetricInfo(MeasurementTemplate tmpl, AppdefEntityID aid){
        this(tmpl);
        this.aids.add(aid);
    }

    public ProblemMetricInfo(MeasurementTemplate tmpl, AppdefEntityID aid,
                             double[] data) {
        this(tmpl, aid);
        this.setMeasurementData(data);
    }

    public MeasurementTemplate getMeasurementTemplate() {
        return this.tmpl;
    }
    
    public void setMeasurementTemplate(MeasurementTemplate val) {
        this.tmpl = val;
    }
    
    public double[] getMeasurementData() {
        return this.data;
    }

    public void setMeasurementData(double[] val) {
        this.data = val;
    }

    public int getProblemType() {
        return this.probFlag;
    }

    public long getProblemTime() {
        return this.earliest;
    }
    
    public void setProblemType(int i) {
        this.probFlag = i;
    }

    public int getAlertCount() {
        return this.alertCount;
    }

    public void setAlertCount(int i) {
        this.alertCount = i;
        if (i > 0) {
            this.probFlag |= ProblemMetricInfo.FLAG_ALERTS;
        }
        else {
            this.probFlag &= ~ProblemMetricInfo.FLAG_ALERTS;
        }
    }

    /**
     * @return the array of appdef entities that have problems
     */
    public HashSet getProblemEntities() {
        return aids;
    }

    /**
     * @return the array of appdef entities that have problems
     */
    public AppdefEntityID[] getProblemEntitiesArray() {
        return (AppdefEntityID[]) this.aids.toArray(
            new AppdefEntityID[this.aids.size()]);
    }

    /**
     * @return the number of appdef entities that have problems
     */
    public int getProblemEntitiesSize() {
        return this.aids.size();
    }
    
    /**
     * @return problem type of alerts by performing bitwise operation on flag
     */
    public boolean hasAlerts() {
        return (this.probFlag & FLAG_ALERTS) == FLAG_ALERTS;
    }

    /**
     * @return problem type of over max by performing bitwise operation on flag
     */
    public boolean hasHigh() {
        return (this.probFlag & FLAG_HIGH) == FLAG_HIGH;
    }

    /**
     * @return problem type of under min by performing bitwise operation on flag
     */
    public boolean hasLow() {
        return (this.probFlag & FLAG_LOW) == FLAG_LOW;
    }

    /** 
     * Accumulate the problems from another entity
     * @param pmi the additional ProblemMetricInfo to add to the current one
     */
    public void addToProblem(ProblemMetricInfo pmi) {
        this.aids.addAll(pmi.getProblemEntities());
        this.probFlag |= pmi.getProblemType();
        this.alertCount += pmi.getAlertCount();
        this.overCount += pmi.getOverCount();
        this.underCount += pmi.getUnderCount();
    }
    
    public int getOobCount() {
        return this.overCount + this.underCount;
    }
    
    public int getOverCount() {
        return this.overCount;
    }

    public int getUnderCount() {
        return this.underCount;
    }

    public void setOverCount(int i) {
        this.overCount = i;
        if (i > 0) {
            this.probFlag |= ProblemMetricInfo.FLAG_OOB;
        }
        else {
            this.probFlag &= ~ProblemMetricInfo.FLAG_OOB;
        }
    }
    
    public void setUnderCount(int i) {
        this.underCount = i;
        if (i > 0) {
            this.probFlag |= ProblemMetricInfo.FLAG_OOB;
        }
        else {
            this.probFlag &= ~ProblemMetricInfo.FLAG_OOB;
        }
    }

    public void setProblemCount(ProblemCount cnt) {
        switch (cnt.getProblemType()) {
            case MeasurementConstants.PROBLEM_TYPE_ALERT:
                this.setAlertCount(cnt.getProblemCount());
                break;
            case MeasurementConstants.PROBLEM_TYPE_OVER:
                this.setOverCount(cnt.getProblemCount());
                break;
            case MeasurementConstants.PROBLEM_TYPE_UNDER:
                this.setUnderCount(cnt.getProblemCount());
                break;
            default:
                break;
        }
        this.registerProblemTime(cnt.getEarliestTime());
    }
    
    /**
     * @param metricId The metricId to set.
     */
    public void setMetricId(Integer metricId) {
        this.metricId = metricId;
    }

    /**
     * @return Returns the metricId.
     */
    public Integer getMetricId() {
        return metricId;
    }


    /**
     * Add outlier entity
     */
    public void addOutlier(AppdefEntityID aeid) {
        this.aids.add(aeid);
        this.probFlag |= FLAG_OUTLIER;
    }

    /**
     * Register another problem time, compare it to existing earliest time
     */
    public void registerProblemTime(long time) {
        this.earliest = Math.min(this.earliest, time);
    }

    /* 
     * This is used to compare problem metrics so that they can be ordered
     */
    public int compareTo(Object o) {
        if (o instanceof ProblemMetricInfo) {
            ProblemMetricInfo other = (ProblemMetricInfo) o;

            // For now, we are only comparing time
            return
                (new Long(this.earliest)).compareTo(new Long(other.earliest));
        }

        throw new IllegalArgumentException(
            "Cannot compare ProblemMetricInfo to a different object");
    }
}
