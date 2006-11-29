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

package org.hyperic.hq.ui.beans;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/*******************************************************************************
 * Bean to hold alert values from AlertValue and AlertDefinitionValue
 ******************************************************************************/

public final class AlertDefinitionBean {
    private long ctime;

    private Integer alertDefId;

    private String name;

    private String description;

    private boolean enabled;

    private Integer type;

    private Integer parentId;

    public AlertDefinitionBean(Integer alertDefId, long ctime, String name,
                               String description, boolean enabled,
                               Integer parentId) {
        this.alertDefId = alertDefId;
        this.ctime = ctime;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.parentId = parentId;
    }

    public Integer getAlertDefId() {
        return alertDefId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public long getCtime() {
        return ctime;
    }

    public Integer getType() {
        return type;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setAppdefEntityID(AppdefEntityID aeid) {
        this.type = new Integer(aeid.getType());
    }
}
