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

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.control.shared.ControlScheduleValue;

/**
 * Bean to hold alert values from AlertValue and AlertDefinitionValue
 * **/

public final class DashboardControlBean implements java.io.Serializable{
    /** Holds value of property resource. */
    private AppdefResourceValue resource;
    
    /** Holds value of property control. */
    private ControlScheduleValue control;
    
    public DashboardControlBean() {
    //empty constructor
    }
    


   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");	  
    	  str.append("}");
	  return(str.toString());
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
   
   /** Getter for property control.
    * @return Value of property control.
    *
    */
   public ControlScheduleValue getControl() {
       return this.control;
   }
   
   /** Setter for property control.
    * @param control New value of property control.
    *
    */
   public void setControl(ControlScheduleValue control) {
       this.control = control;
   }
   
}

// EOF
