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
