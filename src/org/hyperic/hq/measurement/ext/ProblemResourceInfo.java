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

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * This is used to determine to identify which concrete resources have problem indicators.
 */
public class ProblemResourceInfo implements Comparable, Serializable {

    private AppdefEntityID entityId = null;
    private int oobCount = 0;
    private int alertCount = 0;
    private long earliest = Long.MAX_VALUE;
    private long latest = Long.MIN_VALUE;
    private boolean oobCountSet = false;
    private boolean alertCountSet = false;
    
    /**
     * Construct a problem resource with data pulled from the database
     * 
     * @param anEntityId
     * @param theOobCount
     * @param theAlertCount
     * @param theEarliest
     */
    public ProblemResourceInfo(AppdefEntityID anEntityId, Integer theOobCount, 
                               Integer theAlertCount, long theEarliest,
                               long theLatest) {
        entityId = anEntityId;
        
        if (theOobCount != null)
            this.setOobCount(theOobCount.intValue());
        
        if (theAlertCount != null)
            this.setAlertCount(theAlertCount.intValue());
        
        earliest = theEarliest;
        latest = theLatest;
    }
    
    /**
     * Construct a problem resource from its set of problem metrics 
     * 
     * @param anEntityId
     * @param someProblems
     */
    public ProblemResourceInfo(AppdefEntityID anEntityId, 
                               ProblemMetricInfo[] someProblems) {
        entityId = anEntityId;
        pickProblems(someProblems);
    }
    
    private void pickProblems(ProblemMetricInfo[] someProblems) {
        for (int i = 0; i < someProblems.length; i++) {
            ProblemMetricInfo info = someProblems[i];
            earliest = Math.min(earliest, info.getProblemTime());
            oobCount =+ info.getOobCount();
            alertCount =+ info.getAlertCount();
        }
    }


    /**
     * Depending compare resource's problems based on when it started producing
     * alerts or started having metrics out of bounds.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        ProblemResourceInfo other = (ProblemResourceInfo)o; // ClassCastException?
        if (this.earliest == other.earliest)
            return 0;
        return this.earliest < other.earliest ? -1 : 1;        
    }

    /**
     * The number of alerts that the resource has associated with it during the timeframe
     * this was constructed for
     * 
     * @return int
     */
    public int getAlertCount() {
        return alertCount;
    }

    /**
     * The number of value out-of-range events that the resource has associated with it during 
     * the timeframe this was constructed for
     * 
     * @return int
     */
    public int getOobCount() {
        return oobCount;
    }

    /**
     * The time within the constructed timeframe when a problem was first detected
     * 
     * @return long
     */
    public long getEarliest() {
        return earliest;
    }

    /**
     * The ID of the problematic resource
     * 
     * @return AppdefEntityID
     */
    public AppdefEntityID getEntityId() {
        return entityId;
    }

    public boolean isAlertCountSet() {
        return alertCountSet;
    }

    public boolean isOobCountSet() {
        return oobCountSet;
    }

    public void setAlertCount(int alertCount) {
        alertCountSet = true;
        this.alertCount = alertCount;
    }

    public void setOobCount(int oobCount) {
        oobCountSet = true;
        this.oobCount = oobCount;
    }

    public void setEarliest(long earliest) {
        this.earliest = earliest;
    }

    public long getLatest() {
        return latest;
    }
    
    public void setLatest(long latest) {
        this.latest = latest;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append("( entityId = ").append(entityId);
        sb.append(", alertCount = ").append(alertCount);
        sb.append(", oobCount = ").append(oobCount);
        sb.append(", earliest = ").append(earliest);
        sb.append(", latest = ").append(latest);
        sb.append(" )");
        return sb.toString();
    }
}
