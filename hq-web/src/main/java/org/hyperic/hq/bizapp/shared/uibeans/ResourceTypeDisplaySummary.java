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

    private AppdefResourceTypeValue _resourceType = null;
    private Integer _appdefTypeId     = null;
    private Double  _availability     = null;
    private Integer _availTempl       = null;
    private Double  _throughput       = null; // aka "usage"
    private String  _throughputUnits;
    private Integer _throughputTempl  = null;
    private Double  _performance      = null;
    private String  _performanceUnits = null;
    private Integer _performTempl     = null;
    private Integer _numResources     = null;
    
    public ResourceTypeDisplaySummary() {
        super();
    }

    /**
     * Returns a constant describing the summarized
     * resources. Examples of summary types are "autogroup", "cluster"
     * and "singleton".
     *
     * @see UIConstants
     */
    public abstract int getSummaryType();

    public Double getAvailability() {
        return _availability;
    }
    
    public void setAvailability(Double availability) {
        _availability = availability;
    }
    
    /**
     * @return Double The value for the Throughput metric
     */
    public Double getThroughput() {
        return _throughput;
    }

    public void setThroughput(Double throughput) {
        _throughput = throughput;
    }
    
    /**
     * Returns the performance for the resource.  Performance is a unit measured
     * for the resource type that is user specified (not per resource or per
     * user but per resource type).
     * 
     * @return Double
     */
    public Double getPerformance() {
        return _performance;
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
        _performance = performance;
    }

    public AppdefResourceTypeValue getResourceType() {
        return _resourceType;
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
        _resourceType = resourceType;
    }

    public Integer getNumResources() {
        return _numResources;
    }

    public void setNumResources(Integer i) {
        _numResources = i;
    }

    public Integer getAppdefTypeId() {
        if (_appdefTypeId == null && _resourceType != null)
            _appdefTypeId = new Integer(_resourceType.getAppdefType());
        return _appdefTypeId;
    }

    public void setAppdefTypeId(Integer integer) {
        _appdefTypeId = integer;
    }

    public String getThroughputUnits() {
        return _throughputUnits;
    }

    public void setThroughputUnits(String throughputUnits) {
        _throughputUnits = throughputUnits;
    }

    public Integer getAvailTempl() {
        return _availTempl;
    }

    public void setAvailTempl(Integer availTempl) {
        _availTempl = availTempl;
    }

    public Integer getPerformTempl() {
        return _performTempl;
    }

    public void setPerformTempl(Integer performTempl) {
        _performTempl = performTempl;
    }

    public void setPerformanceUnits(String units) {
        _performanceUnits = units;
    }

    public String getPerformanceUnits() {
        return _performanceUnits;
    }

    public Integer getThroughputTempl() {
        return _throughputTempl;
    }

    public void setThroughputTempl(Integer throughputTempl) {
        _throughputTempl = throughputTempl;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append("(resourceType=").append(_resourceType);
        sb.append(",throughput=").append(_throughput);
        sb.append(",throughputUnits=").append(_throughputUnits);
        sb.append(",performance=").append(_performance);
        sb.append(",appdefTypeId=").append(_appdefTypeId);
        sb.append(",numResources=").append(_numResources);
        sb.append(",availTempl=").append(_availTempl);
        sb.append(",throughputTempl=").append(_throughputTempl);
        sb.append(",perfTempl=").append(_performTempl);
        sb.append("))");
        return sb.toString();
    }

    public int compareTo(Object o) {
        String one, two;
        
        /**
         * Dear reader.  This shitty code is courtesy of whoever decided to make
         * the singleton display summary have it's own concept of a 'name'
         */
        if (this instanceof SingletonDisplaySummary)
            one = ((SingletonDisplaySummary)this).getEntityName();
        else
            one = this.getResourceType().getName();
            
        if (o instanceof SingletonDisplaySummary)
            two = ((SingletonDisplaySummary)o).getEntityName();
        else
            two = ((ResourceTypeDisplaySummary)o).getResourceType().getName();

        return one.compareTo(two);
    }
}
