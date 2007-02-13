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

/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.events.shared;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * Value object for Alert.
 *
 */
public class AlertValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private boolean fixed;
   private boolean acknowledgeable;
   private Integer id;
   private boolean idHasBeenSet = false;
   private Integer alertDefId;
   private boolean alertDefIdHasBeenSet = false;
   private long ctime;
   private boolean ctimeHasBeenSet = false;
   private java.util.Collection ConditionLogs = new java.util.ArrayList();
   private java.util.Collection ActionLogs = new java.util.ArrayList();
   private java.util.Collection EscalationLogs = new java.util.ArrayList();

   public AlertValue()
   {
   }

   public AlertValue( Integer id,Integer alertDefId,long ctime )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.alertDefId = alertDefId;
	  alertDefIdHasBeenSet = true;
	  this.ctime = ctime;
	  ctimeHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public AlertValue( AlertValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.alertDefId = otherValue.alertDefId;
	  alertDefIdHasBeenSet = true;
	  this.ctime = otherValue.ctime;
	  ctimeHasBeenSet = true;
	// TODO Clone is better no ?
	  this.ConditionLogs = otherValue.ConditionLogs;
	// TODO Clone is better no ?
	  this.ActionLogs = otherValue.ActionLogs;
	  this.EscalationLogs = otherValue.EscalationLogs;
   }

    public boolean isFixed()
    {
        return fixed;
    }

    public void setFixed(boolean fixed)
    {
        this.fixed = fixed;
    }

    public boolean isAcknowledgeable() {
        return acknowledgeable;
    }

    public void setAcknowledgeable(boolean acknowledgeable) {
        this.acknowledgeable = acknowledgeable;
    }

    public Integer getId()
   {
	  return this.id;
   }

   public void setId( Integer id )
   {
	  this.id = id;
	  idHasBeenSet = true;

   }

   public boolean idHasBeenSet(){
	  return idHasBeenSet;
   }
   public Integer getAlertDefId()
   {
	  return this.alertDefId;
   }

   public void setAlertDefId( Integer alertDefId )
   {
	  this.alertDefId = alertDefId;
	  alertDefIdHasBeenSet = true;

   }

   public boolean alertDefIdHasBeenSet(){
	  return alertDefIdHasBeenSet;
   }
   public long getCtime()
   {
	  return this.ctime;
   }

   public void setCtime( long ctime )
   {
	  this.ctime = ctime;
	  ctimeHasBeenSet = true;

   }

   public boolean ctimeHasBeenSet(){
	  return ctimeHasBeenSet;
   }

   protected java.util.Collection addedConditionLogs = new java.util.ArrayList();
   protected java.util.Collection removedConditionLogs = new java.util.ArrayList();
   protected java.util.Collection updatedConditionLogs = new java.util.ArrayList();

   public java.util.Collection getAddedConditionLogs() { return addedConditionLogs; }
   public java.util.Collection getRemovedConditionLogs() { return removedConditionLogs; }
   public java.util.Collection getUpdatedConditionLogs() { return updatedConditionLogs; }

   public org.hyperic.hq.events.shared.AlertConditionLogValue[] getConditionLogs()
   {
	  return (org.hyperic.hq.events.shared.AlertConditionLogValue[])this.ConditionLogs.toArray(new org.hyperic.hq.events.shared.AlertConditionLogValue[ConditionLogs.size()]);
   }

   public void addConditionLog(org.hyperic.hq.events.shared.AlertConditionLogValue added)
   {
	  this.ConditionLogs.add(added);
	  if ( ! this.addedConditionLogs.contains(added))
		 this.addedConditionLogs.add(added);
   }

   public void removeConditionLog(org.hyperic.hq.events.shared.AlertConditionLogValue removed)
   {
	  this.ConditionLogs.remove(removed);
	  this.removedConditionLogs.add(removed);
	  if (this.addedConditionLogs.contains(removed))
		 this.addedConditionLogs.remove(removed);
	  if (this.updatedConditionLogs.contains(removed))
		 this.updatedConditionLogs.remove(removed);
   }

   public void removeAllConditionLogs()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.ConditionLogs.clear();
   }

   public void updateConditionLog(org.hyperic.hq.events.shared.AlertConditionLogValue updated)
   {
	  if ( ! this.updatedConditionLogs.contains(updated))
		 this.updatedConditionLogs.add(updated);
   }

   public void cleanConditionLog(){
	  this.addedConditionLogs = new java.util.ArrayList();
	  this.removedConditionLogs = new java.util.ArrayList();
	  this.updatedConditionLogs = new java.util.ArrayList();
   }

   public void copyConditionLogsFrom(org.hyperic.hq.events.shared.AlertValue from)
   {
	  // TODO Clone the List ????
	  this.ConditionLogs = from.ConditionLogs;
   }
   protected java.util.Collection addedActionLogs = new java.util.ArrayList();
   protected java.util.Collection removedActionLogs = new java.util.ArrayList();
   protected java.util.Collection updatedActionLogs = new java.util.ArrayList();

   public java.util.Collection getAddedActionLogs() { return addedActionLogs; }
   public java.util.Collection getRemovedActionLogs() { return removedActionLogs; }
   public java.util.Collection getUpdatedActionLogs() { return updatedActionLogs; }

   public org.hyperic.hq.events.shared.AlertActionLogValue[] getActionLogs()
   {
      return (org.hyperic.hq.events.shared.AlertActionLogValue[])this.ActionLogs.toArray(new org.hyperic.hq.events.shared.AlertActionLogValue[ActionLogs.size()]);
   }

   public void addActionLog(org.hyperic.hq.events.shared.AlertActionLogValue added)
   {
      this.ActionLogs.add(added);
      if ( ! this.addedActionLogs.contains(added))
         this.addedActionLogs.add(added);
   }

   public void removeActionLog(org.hyperic.hq.events.shared.AlertActionLogValue removed)
   {
      this.ActionLogs.remove(removed);
      this.removedActionLogs.add(removed);
      if (this.addedActionLogs.contains(removed))
         this.addedActionLogs.remove(removed);
      if (this.updatedActionLogs.contains(removed))
         this.updatedActionLogs.remove(removed);
   }

   public void removeAllActionLogs()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.ActionLogs.clear();
   }

   public void updateActionLog(org.hyperic.hq.events.shared.AlertActionLogValue updated)
   {
      if ( ! this.updatedActionLogs.contains(updated))
         this.updatedActionLogs.add(updated);
   }

   public void cleanActionLog(){
      this.addedActionLogs = new java.util.ArrayList();
      this.removedActionLogs = new java.util.ArrayList();
      this.updatedActionLogs = new java.util.ArrayList();
   }

   public void copyActionLogsFrom(org.hyperic.hq.events.shared.AlertValue from)
   {
      // TODO Clone the List ????
      this.ActionLogs = from.ActionLogs;
   }

   protected java.util.Collection addedEscalationLogs = new java.util.ArrayList();
   protected java.util.Collection removedEscalationLogs = new java.util.ArrayList();
   protected java.util.Collection updatedEscalationLogs = new java.util.ArrayList();

   public java.util.Collection getAddedEscalationLogs() { return addedEscalationLogs; }
   public java.util.Collection getRemovedEscalationLogs() { return removedEscalationLogs; }
   public java.util.Collection getUpdatedEscalationLogs() { return updatedEscalationLogs; }

   public org.hyperic.hq.events.shared.AlertActionLogValue[] getEscalationLogs()
   {
      return (org.hyperic.hq.events.shared.AlertActionLogValue[])this.EscalationLogs.toArray(new org.hyperic.hq.events.shared.AlertActionLogValue[EscalationLogs.size()]);
   }

   public void addEscalationLog(org.hyperic.hq.events.shared.AlertActionLogValue added)
   {
      this.EscalationLogs.add(added);
      if ( ! this.addedEscalationLogs.contains(added))
         this.addedEscalationLogs.add(added);
   }

   public void removeEscalationLog(org.hyperic.hq.events.shared.AlertActionLogValue removed)
   {
      this.EscalationLogs.remove(removed);
      this.removedEscalationLogs.add(removed);
      if (this.addedEscalationLogs.contains(removed))
         this.addedEscalationLogs.remove(removed);
      if (this.updatedEscalationLogs.contains(removed))
         this.updatedEscalationLogs.remove(removed);
   }

   public void removeAllEscalationLogs()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.EscalationLogs.clear();
   }

   public void updateEscalationLog(org.hyperic.hq.events.shared.AlertActionLogValue updated)
   {
      if ( ! this.updatedEscalationLogs.contains(updated))
         this.updatedEscalationLogs.add(updated);
   }

   public void cleanEscalationLog(){
      this.addedEscalationLogs = new java.util.ArrayList();
      this.removedEscalationLogs = new java.util.ArrayList();
      this.updatedEscalationLogs = new java.util.ArrayList();
   }

   public void copyEscalationLogsFrom(org.hyperic.hq.events.shared.AlertValue from)
   {
      // TODO Clone the List ????
      this.EscalationLogs = from.EscalationLogs;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "alertDefId=" + getAlertDefId() + " " + "ctime=" + getCtime());
	  str.append('}');

	  return(str.toString());
   }

   /**
	* A Value object have an identity if its attributes making its Primary Key
	* has all been set.  One object without identity is never equal to any other
	* object.
	*
	* @return true if this instance have an identity.
	*/
   protected boolean hasIdentity()
   {
	  boolean ret = true;
	  ret = ret && idHasBeenSet;
	  return ret;
   }

   public boolean equals(Object other)
   {
	  if ( ! hasIdentity() ) return false;
	  if (other instanceof AlertValue)
	  {
		 AlertValue that = (AlertValue) other;
		 if ( ! that.hasIdentity() ) return false;
		 boolean lEquals = true;
		 if( this.id == null )
		 {
			lEquals = lEquals && ( that.id == null );
		 }
		 else
		 {
			lEquals = lEquals && this.id.equals( that.id );
		 }

		 lEquals = lEquals && isIdentical(that);

		 return lEquals;
	  }
	  else
	  {
		 return false;
	  }
   }

   public boolean isIdentical(Object other)
   {
	  if (other instanceof AlertValue)
	  {
		 AlertValue that = (AlertValue) other;
		 boolean lEquals = true;
		 if( this.alertDefId == null )
		 {
			lEquals = lEquals && ( that.alertDefId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.alertDefId.equals( that.alertDefId );
		 }
		 lEquals = lEquals && this.ctime == that.ctime;
		 if( this.getConditionLogs() == null )
		 {
			lEquals = lEquals && ( that.getConditionLogs() == null );
		 }
		 else
		 {
            // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
            // - javier 7/16/03
            java.util.Collection cmr1 = java.util.Arrays.asList(this.getConditionLogs());
            java.util.Collection cmr2 = java.util.Arrays.asList(that.getConditionLogs());
			// lEquals = lEquals && java.util.Arrays.equals(this.getConditionLogs() , that.getConditionLogs()) ;
            lEquals = lEquals && cmr1.containsAll(cmr2);
		 }
		 if( this.getActionLogs() == null )
		 {
			lEquals = lEquals && ( that.getActionLogs() == null );
		 }
		 else
		 {
            // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
            // - javier 7/16/03
            java.util.Collection cmr1 = java.util.Arrays.asList(this.getActionLogs());
            java.util.Collection cmr2 = java.util.Arrays.asList(that.getActionLogs());
			// lEquals = lEquals && java.util.Arrays.equals(this.getActionLogs() , that.getActionLogs()) ;
            lEquals = lEquals && cmr1.containsAll(cmr2);
		 }

		 return lEquals;
	  }
	  else
	  {
		 return false;
	  }
   }

   public int hashCode(){
	  int result = 17;
      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

      result = 37*result + ((this.alertDefId != null) ? this.alertDefId.hashCode() : 0);

      result = 37*result + (int)(ctime^(ctime>>>32));

	  result = 37*result + ((this.getConditionLogs() != null) ? this.getConditionLogs().hashCode() : 0);
	  result = 37*result + ((this.getActionLogs() != null) ? this.getActionLogs().hashCode() : 0);
	  return result;
   }

}
