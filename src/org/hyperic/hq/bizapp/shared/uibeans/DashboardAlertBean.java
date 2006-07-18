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
 * **/

public final class DashboardAlertBean extends java.lang.Object implements java.io.Serializable{
    
    private long ctime ;
    private Integer alertDefId ;
    private Integer alertId;
    private Integer type;

    /** Holds value of property alertDefName. */
    private String alertDefName;    
    
    /** Holds value of property resource. */
    private AppdefResourceValue resource;
    
    public DashboardAlertBean() {
    //empty constructor
    }
    

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime=ctime;
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
    

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");
	  str.append("id=" + getAlertId() + " " + "alertDefId=" + getAlertDefId() + " " + "ctime=" + getCtime());
    	  str.append('}');

	  return(str.toString());
   }

   /** Getter for property alertDefName.
    * @return Value of property alertDefName.
    *
    */
   public String getAlertDefName() {
       return this.alertDefName;
   }   
   
   /** Setter for property alertDefName.
    * @param alertDefName New value of property alertDefName.
    *
    */
   public void setAlertDefName(String alertDefName) {
       this.alertDefName = alertDefName;
   }
   
   /** Getter for property resource.
    * @return Value of property resource.
    *
    */
   public AppdefResourceValue getResource() {
       return this.resource;
   }
   
   /** Setter for property resource.
    * @param resource New value of property resource.
    *
    */
   public void setResource(AppdefResourceValue resource) {
       this.resource = resource;
   }
   
}

// EOF
