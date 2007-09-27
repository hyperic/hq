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

import org.hyperic.hq.appdef.shared.AppdefResourceValue;

/**
 * This bean used when resources are selected to have their metrics compared
 */
public class ResourceMetricDisplaySummary extends MetricDisplaySummary
    implements Serializable {

    private AppdefResourceValue resource;
    
    /**
     * Constructor for ResourceMetricDisplaySummary.
     */
    public ResourceMetricDisplaySummary() {
        super();
    }

    public ResourceMetricDisplaySummary(MetricDisplaySummary mds) {
        super();
        init(mds);
    }

    public ResourceMetricDisplaySummary(MetricDisplaySummary mds,
                                        AppdefResourceValue resource) {
        super();
        init(mds);
        this.resource = resource;
    }

    private void init(MetricDisplaySummary mds) {
        this.setBeginTimeFrame(mds.getBeginTimeFrame());
        this.setCollectionType(mds.getCollectionType());
        this.setAvailUp(mds.getAvailUp());
        this.setDisplayUnits(mds.getDisplayUnits());
        this.setEndTimeFrame(mds.getEndTimeFrame());
        this.setLabel(mds.getLabel());
        this.setMetricSource(mds.getMetricSource());
        this.setShowNumberCollecting(mds.getShowNumberCollecting());
        this.setTemplateCat(mds.getTemplateCat());
        this.setTemplateId(mds.getTemplateId());
        this.setAvailUnknown(mds.getAvailUnknown());
        this.setAvailDown(mds.getAvailDown());
        this.setUnits(mds.getUnits());
        this.setMetrics(mds.getMetrics());
        this.setDesignated(mds.getDesignated());        
    }

    /**
     * Returns the resource.
     * @return AppdefResourceValue
     */
    public AppdefResourceValue getResource() {
        return resource;
    }

    /**
     * Sets the resource.
     * @param resource The resource to set
     */
    public void setResource(AppdefResourceValue resource) {
        this.resource = resource;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append("(resource=").append(resource);
        sb.append(",super(").append(super.toString());
        sb.append("))");
        return sb.toString();
    }
}
