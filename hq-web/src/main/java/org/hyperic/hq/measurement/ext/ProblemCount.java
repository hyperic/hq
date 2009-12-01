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

/**
 *
 * Keep track of the number of problems and timestamp
 */
public class ProblemCount {
    private Integer metricId  = null;
    private int  problemCount = 0;
    private int  problemType  = 0;
    private long earliest     = Long.MAX_VALUE;
    
    public ProblemCount(Integer mid, int type, int count, long time) {
        this.metricId     = mid;
        this.problemType  = type;
        this.problemCount = count;
        this.earliest     = time;
    }

    /**
     * @return Returns the metricId.
     */
    public Integer getMetricId() {
        return metricId;
    }

    /**
     * @return Returns the problemType.
     */
    public int getProblemType() {
        return problemType;
    }
    public long getEarliestTime() {
        return earliest;
    }

    public int getProblemCount() {
        return problemCount;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(" : ( earliest =").append(earliest);
        sb.append(",  problemCount =").append(problemCount);
        sb.append(",  problemType =").append(problemType);
        sb.append(" )");
        return sb.toString();
    }
}
