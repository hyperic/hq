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

import org.hyperic.hq.authz.server.session.Resource;

/**
 * This bean used when resources are selected to have their metrics compared
 */
public class ResourceMetricDisplaySummary extends MetricDisplaySummary
    implements Serializable {

    private Resource _resource;
    
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
                                        Resource resource) {
        super();
        init(mds);
        _resource = resource;
    }

    private void init(MetricDisplaySummary mds) {
        setBeginTimeFrame(mds.getBeginTimeFrame());
        setCollectionType(mds.getCollectionType());
        setAvailUp(mds.getAvailUp());
        setDisplayUnits(mds.getDisplayUnits());
        setEndTimeFrame(mds.getEndTimeFrame());
        setLabel(mds.getLabel());
        setMetricSource(mds.getMetricSource());
        setShowNumberCollecting(mds.getShowNumberCollecting());
        setTemplateCat(mds.getTemplateCat());
        setTemplateId(mds.getTemplateId());
        setAvailUnknown(mds.getAvailUnknown());
        setAvailDown(mds.getAvailDown());
        setUnits(mds.getUnits());
        setMetrics(mds.getMetrics());
        setDesignated(mds.getDesignated());        
    }

    /**
     * Returns the resource.
     * @return AppdefResourceValue
     */
    public Resource getResource() {
        return _resource;
    }

    /**
     * Sets the resource.
     * @param resource The resource to set
     */
    public void setResource(Resource resource) {
        _resource = resource;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(getClass().getName());
        sb.append("(resource=").append(_resource);
        sb.append(",super(").append(super.toString());
        sb.append("))");
        return sb.toString();
    }
}
