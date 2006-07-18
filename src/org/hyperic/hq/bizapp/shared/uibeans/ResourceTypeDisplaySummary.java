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

package org.hyperic.hq.bizapp.shared.uibeans;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;

/**
 * Represents the resource current health summary displayed on the "Current
 * Health" views for a resource type.  
 * 
 * Note: the "performanceThreshold" and "thruputThreshold" levels are unlikely
 * v1.0 features, so any effort beyond stub implementation until they're
 * scheduled for release is probably unnecessary.
 */
public abstract class ResourceTypeDisplaySummary
    implements Serializable, Comparable {

    private AppdefResourceTypeValue resourceType = null;
    private Integer appdefTypeId     = null;
    private Double  availability     = null;
    private Integer availTempl       = null;
    private Double  throughput       = null;        // aka "usage"
    private String  throughputUnits;
    private Integer throughputTempl  = null;
    private Double  performance      = null;        // aka "response time"
    private Integer performTempl     = null;
    private Integer numResources     = null;
    
    /**
     * Constructor for ResourceTypeDisplaySummary.
     */
    public ResourceTypeDisplaySummary() {
        super();
    }

    /**
     * Returns a constant describing the summarized
     * resources. Examples of summary types are "autogroup", "cluster"
     * and "singleton".
     *
     * @see org.hyperic.hq.bizapp.shared.UIConstants
     */
    public abstract int getSummaryType();

    public Double getAvailability() {
        return availability;
    }
    
    public void setAvailability(Double availability) {
        this.availability = availability;
    }
    
    /**
     * Method getThruput.
     * 
     * Returns the accumulated thruput for the resource
     * 
     * @return Double
     */
    public Double getThroughput() {
        return throughput;
    }
    
    /**
     * Method setThruput.
     * 
     * Assigns the accumulated thruput for the resource
     * 
     * @param throughput The throughput to set
     */
    public void setThroughput(Double throughput) {
        this.throughput = throughput;
    }
    
    /**
     * Method getPerformance.
     * 
     * Returns the performance for the resource.  Performance is a unit measured
     * for the resource type that is user specified (not per resource or per
     * user but per resource type).
     * 
     * @return Double
     */
    public Double getPerformance() {
        return performance;
    }
    
    /**
     * Method setPerformance.
     * 
     * Assigns the performance for the resource.  Performance is a unit measured
     * for the resource type that is user specified (not per resource or per
     * user but per resource type).
     * 
     * @param performance The performance to set
     */
    public void setPerformance(Double performance) {
        this.performance = performance;
    }

    /**
     * Returns the resourceType.
     * 
     * Returns the name of the resource type for display in UI i.e. "Apache
     * Virtual Host" or "Weblogic war"
     * 
     * @return AppdefResourceTypeValue
     */
    public AppdefResourceTypeValue getResourceType() {
        return resourceType;
    }

    /**
     * Sets the resourceType.
     * 
     * Assigns the name of the resource type for display in UI i.e. "Apache
     * Virtual Host" or "Weblogic war"
     * 
     * @param resourceType The resourceType to set
     */
    public void setResourceType(AppdefResourceTypeValue resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Returns the number of resources represented by the summary.
     */
    public Integer getNumResources() {
        return numResources;
    }

    /**
     * Sets the number of resources represented by the summary.
     *
     * @param i the number of resources
     */
    public void setNumResources(Integer i) {
        numResources = i;
    }

    /**
     * @return the AppdefEntityId type id
     */
    public Integer getAppdefTypeId() {
        if (appdefTypeId == null && resourceType != null)
            appdefTypeId = new Integer(resourceType.getAppdefTypeId());
        return appdefTypeId;
    }

    /**
     * @param integer
     */
    public void setAppdefTypeId(Integer integer) {
        appdefTypeId = integer;
    }

    /**
     * Get throughputUnits.
     * @return throughputUnits as String.
     */
    public String getThroughputUnits() {
        return throughputUnits;
    }
    
    /**
     * Set throughputUnits.
     *
     * @param throughputUnits the value to set.
     */
    public void setThroughputUnits(String throughputUnits) {
        this.throughputUnits = throughputUnits;
    }

    public Integer getAvailTempl() {
        return availTempl;
    }
    public void setAvailTempl(Integer availTempl) {
        this.availTempl = availTempl;
    }
    public Integer getPerformTempl() {
        return performTempl;
    }
    public void setPerformTempl(Integer performTempl) {
        this.performTempl = performTempl;
    }
    public Integer getThroughputTempl() {
        return throughputTempl;
    }
    public void setThroughputTempl(Integer throughputTempl) {
        this.throughputTempl = throughputTempl;
    }
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append("(resourceType=").append(resourceType);
        sb.append(",throughput=").append(throughput);
        sb.append(",throughputUnits=").append(throughputUnits);
        sb.append(",performance=").append(performance);
        sb.append(",appdefTypeId=").append(appdefTypeId);
        sb.append(",numResources=").append(numResources);
        sb.append(",availTempl=").append(availTempl);
        sb.append(",throughputTempl=").append(throughputTempl);
        sb.append(",perfTempl=").append(performTempl);
        sb.append("))");
        return sb.toString();
    }

    public int compareTo(Object o) {
        ResourceTypeDisplaySummary rtds = (ResourceTypeDisplaySummary) o;
        return this.getResourceType().getName().compareTo(
            rtds.getResourceType().getName());
    }
    
}
