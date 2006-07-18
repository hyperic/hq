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

/**
 *
 * Contains the information to display for a metric's configuration
 */
public class MetricConfigSummary implements Serializable {
    private Integer templateId;          // The template ID
    private String  label;
    private String  description;
    private String  category;
    private long    interval = 0;        // In milliseconds
    
    /**
     * Default Constructor
     *
     */
    public MetricConfigSummary() {
    }
    
    /**
     * Constructor with id, name and category.
     */
    public MetricConfigSummary(int id, String name, String category) {
        this.templateId = new Integer(id);
        this.label       = name;
        this.category   = category;
    }
    
    /** Old getter for compatibility
     * @deprecated Use getLabel()
     */
    public int getId() {
        return templateId.intValue();
    }

    /** Old getter for compatibility
     * @deprecated Use getLabel()
     */
    public String getName() {
        return label;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String string) {
        category = string;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String string) {
        description = string;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long l) {
        interval = l;
    }

    public Integer getTemplateId() {
        return templateId;
    }
    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
}
