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

import org.hyperic.hq.measurement.ext.ProblemMetricInfo;

/**
 *
 * UI bean for problem metrics
 */
public class ProblemMetricSummary implements Serializable {
    private String name = "Unknown";
    private String type = "Unknown";
    private Integer id = new Integer(0);
    private Integer templateId = new Integer(0);
    private int alertCount = 0;
    private int oobCount = 0;
    private long earliest;
    private int entityCount = 1;
    private boolean single = true;
    private String appdefKey = null;

    public ProblemMetricSummary() {
    }
    
    public ProblemMetricSummary(ProblemMetricInfo info) {
        this.name = info.getMeasurementTemplate().getName();
        this.templateId = info.getMeasurementTemplate().getId();
        this.id = info.getMetricId();
        this.alertCount = info.getAlertCount();
        this.oobCount = info.getOobCount();
        this.earliest = info.getProblemTime();
        this.entityCount = info.getProblemEntitiesSize();
    }

    public ProblemMetricSummary(MetricDisplaySummary summary) {
        this.name = summary.getLabel();
        this.templateId = summary.getTemplateId();
        this.alertCount = summary.getAlertCount();
        this.oobCount = summary.getOobCount();
        this.earliest = 0;
        this.entityCount = summary.getAvailUp().intValue();
        this.type = summary.getMetricSource();
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the tid.
     */
    public Integer getTemplateId() {
        return templateId;
    }

    /**
     * @param tid The tid to set.
     */
    public void setTemplateId(Integer tid) {
        this.templateId = tid;
    }

    /**
     * @param id The id to set.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return Returns the id.
     */
    public Integer getId() {
        return id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
    
    /**
     * @return Returns the alertCount.
     */
    public int getAlertCount() {
        return alertCount;
    }
    /**
     * @param alertCount The alertCount to set.
     */
    public void setAlertCount(int alertCount) {
        this.alertCount = alertCount;
    }

    /**
     * @return Returns the oobCount.
     */
    public int getOobCount() {
        return oobCount;
    }
    /**
     * @param oobCount The oobCount to set.
     */
    public void setOobCount(int oobCount) {
        this.oobCount = oobCount;
    }
    /**
     * @return Returns the earliest.
     */
    public long getEarliest() {
        return earliest;
    }
    /**
     * @param earliest The earliest to set.
     */
    public void setEarliest(long earliest) {
        this.earliest = earliest;
    }
    /**
     * @return Returns the entityCount.
     */
    public int getEntityCount() {
        return entityCount;
    }
    /**
     * @param entityCount The entityCount to set.
     */
    public void setEntityCount(int entityCount) {
        this.entityCount = entityCount;
    }

    public String getAppdefKey() {
        return appdefKey;
    }
    
    public void setSingleAppdefKey(String appdefKey) {
        this.single = true;
        this.appdefKey = appdefKey;
    }
    
    public void setMultipleAppdefKey(String appdefKey) {
        this.single = false;
        this.appdefKey = appdefKey;
    }
    
    public boolean getSingle() {
        return single;
    }
}
