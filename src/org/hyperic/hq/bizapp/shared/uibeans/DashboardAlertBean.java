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

import org.hyperic.hq.appdef.shared.AppdefResourceValue;

/**
 * Bean to hold alert values from AlertValue and AlertDefinitionValue
 **/
public final class DashboardAlertBean
    implements java.io.Serializable
{
    private long ctime ;
    private Integer alertDefId ;
    private Integer alertId;
    private String alertDefName;
    private AppdefResourceValue resource;
    
    public DashboardAlertBean() {}

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }
    
    public Integer getAlertId() {
        return alertId;
    }
    
    public void setAlertId(Integer alertId) {
        this.alertId = alertId;
    }
    
    public Integer getAlertDefId() {
        return alertDefId;
    }

    public void setAlertDefId(Integer alertDefId) {
        this.alertDefId = alertDefId;
    }

    public String getAlertDefName() {
        return this.alertDefName;
    }

    public void setAlertDefName(String alertDefName) {
        this.alertDefName = alertDefName;
    }

    public AppdefResourceValue getResource() {
        return this.resource;
    }

    public void setResource(AppdefResourceValue resource) {
        this.resource = resource;
    }
    
    public String toString()
    {
	    StringBuffer str = new StringBuffer("[");
	    str.append("id=")
         .append(getAlertId())
         .append("alertDefId=")
         .append(getAlertDefId())
         .append("ctime=")
         .append(getCtime())
         .append("]");

        return(str.toString());
    }
}
