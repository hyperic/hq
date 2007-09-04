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

/**
 * Value object for AlertDefinition.
 *
 */
public class AlertDefinitionValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String name;
   private boolean nameHasBeenSet = false;
   private long ctime;
   private boolean ctimeHasBeenSet = false;
   private long mtime;
   private boolean mtimeHasBeenSet = false;
   private Integer parentId;
   private boolean parentIdHasBeenSet = false;
   private java.lang.String description;
   private boolean descriptionHasBeenSet = false;
   private boolean enabled;
   private boolean enabledHasBeenSet = false;
   private boolean willRecover;
   private boolean willRecoverHasBeenSet = false;
   private boolean notifyFiltered;
   private boolean notifyFilteredHasBeenSet = false;
   private boolean controlFiltered;
   private boolean controlFilteredHasBeenSet = false;
   private int priority;
   private boolean priorityHasBeenSet = false;
   private int appdefId;
   private boolean appdefIdHasBeenSet = false;
   private int appdefType;
   private boolean appdefTypeHasBeenSet = false;
   private int frequencyType;
   private boolean frequencyTypeHasBeenSet = false;
   private long count;
   private boolean countHasBeenSet = false;
   private long range;
   private boolean rangeHasBeenSet = false;
   private int actOnTriggerId;
   private boolean actOnTriggerIdHasBeenSet = false;
   private Integer escalationId;
   private boolean escalationIdHasBeenSet = false;
   private boolean deleted;
   private boolean deletedHasBeenSet = false;
   private java.util.Collection Triggers = new java.util.ArrayList();
   private java.util.Collection Conditions = new java.util.ArrayList();
   private java.util.Collection Actions = new java.util.ArrayList();

   public AlertDefinitionValue()
   {
   }

   public AlertDefinitionValue( Integer id,String name,long ctime,long mtime,Integer parentId,java.lang.String description,boolean enabled,boolean willRecover,boolean notifyFiltered,boolean controlFiltered,int priority,int appdefId,int appdefType,int frequencyType,long count,long range,int actOnTriggerId,boolean deleted )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.ctime = ctime;
	  ctimeHasBeenSet = true;
	  this.mtime = mtime;
	  mtimeHasBeenSet = true;
	  this.parentId = parentId;
	  parentIdHasBeenSet = true;
	  this.description = description;
	  descriptionHasBeenSet = true;
	  this.enabled = enabled;
	  enabledHasBeenSet = true;
	  this.willRecover = willRecover;
	  willRecoverHasBeenSet = true;
	  this.notifyFiltered = notifyFiltered;
	  notifyFilteredHasBeenSet = true;
	  this.controlFiltered = controlFiltered;
	  controlFilteredHasBeenSet = true;
	  this.priority = priority;
	  priorityHasBeenSet = true;
	  this.appdefId = appdefId;
	  appdefIdHasBeenSet = true;
	  this.appdefType = appdefType;
	  appdefTypeHasBeenSet = true;
	  this.frequencyType = frequencyType;
	  frequencyTypeHasBeenSet = true;
	  this.count = count;
	  countHasBeenSet = true;
	  this.range = range;
	  rangeHasBeenSet = true;
	  this.actOnTriggerId = actOnTriggerId;
	  actOnTriggerIdHasBeenSet = true;
	  this.deleted = deleted;
	  deletedHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public AlertDefinitionValue( AlertDefinitionValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.ctime = otherValue.ctime;
	  ctimeHasBeenSet = true;
	  this.mtime = otherValue.mtime;
	  mtimeHasBeenSet = true;
	  this.parentId = otherValue.parentId;
	  parentIdHasBeenSet = true;
	  this.description = otherValue.description;
	  descriptionHasBeenSet = true;
	  this.enabled = otherValue.enabled;
	  enabledHasBeenSet = true;
	  this.willRecover = otherValue.willRecover;
	  willRecoverHasBeenSet = true;
	  this.notifyFiltered = otherValue.notifyFiltered;
	  notifyFilteredHasBeenSet = true;
	  this.controlFiltered = otherValue.controlFiltered;
	  controlFilteredHasBeenSet = true;
	  this.priority = otherValue.priority;
	  priorityHasBeenSet = true;
	  this.appdefId = otherValue.appdefId;
	  appdefIdHasBeenSet = true;
	  this.appdefType = otherValue.appdefType;
	  appdefTypeHasBeenSet = true;
	  this.frequencyType = otherValue.frequencyType;
	  frequencyTypeHasBeenSet = true;
	  this.count = otherValue.count;
	  countHasBeenSet = true;
	  this.range = otherValue.range;
	  rangeHasBeenSet = true;
	  this.actOnTriggerId = otherValue.actOnTriggerId;
	  actOnTriggerIdHasBeenSet = true;
	  this.deleted = otherValue.deleted;
	  deletedHasBeenSet = true;
	// TODO Clone is better no ?
	  this.Triggers = otherValue.Triggers;
	// TODO Clone is better no ?
	  this.Conditions = otherValue.Conditions;
	// TODO Clone is better no ?
	  this.Actions = otherValue.Actions;

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
   public String getName()
   {
	  return this.name;
   }

   public void setName( String name )
   {
	  this.name = name;
	  nameHasBeenSet = true;

   }

   public boolean nameHasBeenSet(){
	  return nameHasBeenSet;
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
   
   /**
    * Set the mtime to the current time if it has not been set yet.
    */
   public void initializeMTimeToNow() {
       if (getMtime() == 0) {
           setMtime(System.currentTimeMillis());
       }
   }
   
   public long getMtime()
   {
	  return this.mtime;
   }

   public void setMtime( long mtime )
   {
	  this.mtime = mtime;
	  mtimeHasBeenSet = true;

   }

   public boolean mtimeHasBeenSet(){
	  return mtimeHasBeenSet;
   }
   public Integer getParentId()
   {
	  return this.parentId;
   }

   public void setParentId( Integer parentId )
   {
	  this.parentId = parentId;
	  parentIdHasBeenSet = true;

   }

   public boolean parentIdHasBeenSet(){
	  return parentIdHasBeenSet;
   }
   public java.lang.String getDescription()
   {
	  return this.description;
   }

   public void setDescription( java.lang.String description )
   {
	  this.description = description;
	  descriptionHasBeenSet = true;

   }

   public boolean descriptionHasBeenSet(){
	  return descriptionHasBeenSet;
   }
   public boolean getEnabled()
   {
	  return this.enabled;
   }

   public void setEnabled( boolean enabled )
   {
	  this.enabled = enabled;
	  enabledHasBeenSet = true;

   }

   public boolean enabledHasBeenSet(){
	  return enabledHasBeenSet;
   }
   public boolean getWillRecover()
   {
	  return this.willRecover;
   }

   public void setWillRecover( boolean willRecover )
   {
	  this.willRecover = willRecover;
	  willRecoverHasBeenSet = true;

   }

   public boolean willRecoverHasBeenSet(){
	  return willRecoverHasBeenSet;
   }
   public boolean getNotifyFiltered()
   {
	  return this.notifyFiltered;
   }

   public void setNotifyFiltered( boolean notifyFiltered )
   {
	  this.notifyFiltered = notifyFiltered;
	  notifyFilteredHasBeenSet = true;

   }

   public boolean notifyFilteredHasBeenSet(){
	  return notifyFilteredHasBeenSet;
   }
   public boolean getControlFiltered()
   {
	  return this.controlFiltered;
   }

   public void setControlFiltered( boolean controlFiltered )
   {
	  this.controlFiltered = controlFiltered;
	  controlFilteredHasBeenSet = true;

   }

   public boolean controlFilteredHasBeenSet(){
	  return controlFilteredHasBeenSet;
   }
   public int getPriority()
   {
	  return this.priority;
   }

   public void setPriority( int priority )
   {
	  this.priority = priority;
	  priorityHasBeenSet = true;

   }

   public boolean priorityHasBeenSet(){
	  return priorityHasBeenSet;
   }
   public int getAppdefId()
   {
	  return this.appdefId;
   }

   public void setAppdefId( int appdefId )
   {
	  this.appdefId = appdefId;
	  appdefIdHasBeenSet = true;

   }

   public boolean appdefIdHasBeenSet(){
	  return appdefIdHasBeenSet;
   }
   public int getAppdefType()
   {
	  return this.appdefType;
   }

   public void setAppdefType( int appdefType )
   {
	  this.appdefType = appdefType;
	  appdefTypeHasBeenSet = true;

   }

   public boolean appdefTypeHasBeenSet(){
	  return appdefTypeHasBeenSet;
   }
   public int getFrequencyType()
   {
	  return this.frequencyType;
   }

   public void setFrequencyType( int frequencyType )
   {
	  this.frequencyType = frequencyType;
	  frequencyTypeHasBeenSet = true;

   }

   public boolean frequencyTypeHasBeenSet(){
	  return frequencyTypeHasBeenSet;
   }
   public long getCount()
   {
	  return this.count;
   }

   public void setCount( long count )
   {
	  this.count = count;
	  countHasBeenSet = true;

   }

   public boolean countHasBeenSet(){
	  return countHasBeenSet;
   }
   public long getRange()
   {
	  return this.range;
   }

   public void setRange( long range )
   {
	  this.range = range;
	  rangeHasBeenSet = true;

   }

   public boolean rangeHasBeenSet(){
	  return rangeHasBeenSet;
   }
   public int getActOnTriggerId()
   {
	  return this.actOnTriggerId;
   }

   public void setActOnTriggerId( int actOnTriggerId )
   {
	  this.actOnTriggerId = actOnTriggerId;
	  actOnTriggerIdHasBeenSet = true;

   }

   public boolean actOnTriggerIdHasBeenSet(){
	  return actOnTriggerIdHasBeenSet;
   }
   public boolean getDeleted()
   {
	  return this.deleted;
   }

   public void setDeleted( boolean deleted )
   {
	  this.deleted = deleted;
	  deletedHasBeenSet = true;

   }

   public boolean deletedHasBeenSet(){
	  return deletedHasBeenSet;
   }

   protected java.util.Collection addedTriggers = new java.util.ArrayList();
   protected java.util.Collection removedTriggers = new java.util.ArrayList();
   protected java.util.Collection updatedTriggers = new java.util.ArrayList();

   public java.util.Collection getAddedTriggers() { return addedTriggers; }
   public java.util.Collection getRemovedTriggers() { return removedTriggers; }
   public java.util.Collection getUpdatedTriggers() { return updatedTriggers; }

   public org.hyperic.hq.events.shared.RegisteredTriggerValue[] getTriggers()
   {
	  return (org.hyperic.hq.events.shared.RegisteredTriggerValue[])this.Triggers.toArray(new org.hyperic.hq.events.shared.RegisteredTriggerValue[Triggers.size()]);
   }

   public void addTrigger(org.hyperic.hq.events.shared.RegisteredTriggerValue added)
   {
	  this.Triggers.add(added);
	  if ( ! this.addedTriggers.contains(added))
		 this.addedTriggers.add(added);
   }

   public void removeTrigger(org.hyperic.hq.events.shared.RegisteredTriggerValue removed)
   {
	  this.Triggers.remove(removed);
	  this.removedTriggers.add(removed);
	  if (this.addedTriggers.contains(removed))
		 this.addedTriggers.remove(removed);
	  if (this.updatedTriggers.contains(removed))
		 this.updatedTriggers.remove(removed);
   }

   public void removeAllTriggers()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.Triggers.clear();
   }

   public void updateTrigger(org.hyperic.hq.events.shared.RegisteredTriggerValue updated)
   {
	  if ( ! this.updatedTriggers.contains(updated))
		 this.updatedTriggers.add(updated);
   }

   public void cleanTrigger(){
	  this.addedTriggers = new java.util.ArrayList();
	  this.removedTriggers = new java.util.ArrayList();
	  this.updatedTriggers = new java.util.ArrayList();
   }

   public void copyTriggersFrom(org.hyperic.hq.events.shared.AlertDefinitionValue from)
   {
	  // TODO Clone the List ????
	  this.Triggers = from.Triggers;
   }
   protected java.util.Collection addedConditions = new java.util.ArrayList();
   protected java.util.Collection removedConditions = new java.util.ArrayList();
   protected java.util.Collection updatedConditions = new java.util.ArrayList();

   public java.util.Collection getAddedConditions() { return addedConditions; }
   public java.util.Collection getRemovedConditions() { return removedConditions; }
   public java.util.Collection getUpdatedConditions() { return updatedConditions; }

   public org.hyperic.hq.events.shared.AlertConditionValue[] getConditions()
   {
	  return (org.hyperic.hq.events.shared.AlertConditionValue[])this.Conditions.toArray(new org.hyperic.hq.events.shared.AlertConditionValue[Conditions.size()]);
   }

   public void addCondition(org.hyperic.hq.events.shared.AlertConditionValue added)
   {
	  this.Conditions.add(added);
	  if ( ! this.addedConditions.contains(added))
		 this.addedConditions.add(added);
   }

   public void removeCondition(org.hyperic.hq.events.shared.AlertConditionValue removed)
   {
	  this.Conditions.remove(removed);
	  this.removedConditions.add(removed);
	  if (this.addedConditions.contains(removed))
		 this.addedConditions.remove(removed);
	  if (this.updatedConditions.contains(removed))
		 this.updatedConditions.remove(removed);
   }

   public void removeAllConditions()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.Conditions.clear();
   }

   public void updateCondition(org.hyperic.hq.events.shared.AlertConditionValue updated)
   {
	  if ( ! this.updatedConditions.contains(updated))
		 this.updatedConditions.add(updated);
   }

   public void cleanCondition(){
	  this.addedConditions = new java.util.ArrayList();
	  this.removedConditions = new java.util.ArrayList();
	  this.updatedConditions = new java.util.ArrayList();
   }

   public void copyConditionsFrom(org.hyperic.hq.events.shared.AlertDefinitionValue from)
   {
	  // TODO Clone the List ????
	  this.Conditions = from.Conditions;
   }
   protected java.util.Collection addedActions = new java.util.ArrayList();
   protected java.util.Collection removedActions = new java.util.ArrayList();
   protected java.util.Collection updatedActions = new java.util.ArrayList();

   public java.util.Collection getAddedActions() { return addedActions; }
   public java.util.Collection getRemovedActions() { return removedActions; }
   public java.util.Collection getUpdatedActions() { return updatedActions; }

   public org.hyperic.hq.events.shared.ActionValue[] getActions()
   {
	  return (org.hyperic.hq.events.shared.ActionValue[])this.Actions.toArray(new org.hyperic.hq.events.shared.ActionValue[Actions.size()]);
   }

   public void addAction(org.hyperic.hq.events.shared.ActionValue added)
   {
	  this.Actions.add(added);
	  if ( ! this.addedActions.contains(added))
		 this.addedActions.add(added);
   }

   public void removeAction(org.hyperic.hq.events.shared.ActionValue removed)
   {
	  this.Actions.remove(removed);
	  this.removedActions.add(removed);
	  if (this.addedActions.contains(removed))
		 this.addedActions.remove(removed);
	  if (this.updatedActions.contains(removed))
		 this.updatedActions.remove(removed);
   }

    public boolean isEscalationIdHasBeenSet()
    {
        return escalationIdHasBeenSet;
    }

    public void setEscalationIdHasBeenSet(boolean escalationIdHasBeenSet)
    {
        this.escalationIdHasBeenSet = escalationIdHasBeenSet;
    }

    public Integer getEscalationId()
    {
        return escalationId;
    }

    public void setEscalationId(Integer escalationId)
    {
        this.escalationId = escalationId;
    }

    public void removeAllActions()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.Actions.clear();
   }

   public void updateAction(org.hyperic.hq.events.shared.ActionValue updated)
   {
	  if ( ! this.updatedActions.contains(updated))
		 this.updatedActions.add(updated);
   }

   public void cleanAction(){
	  this.addedActions = new java.util.ArrayList();
	  this.removedActions = new java.util.ArrayList();
	  this.updatedActions = new java.util.ArrayList();
   }

   public void copyActionsFrom(org.hyperic.hq.events.shared.AlertDefinitionValue from)
   {
	  // TODO Clone the List ????
	  this.Actions = from.Actions;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "name=" + getName() + " " + "ctime=" + getCtime() + " " + "mtime=" + getMtime() + " " + "parentId=" + getParentId() + " " + "description=" + getDescription() + " " + "enabled=" + getEnabled() + " " + "willRecover=" + getWillRecover() + " " + "notifyFiltered=" + getNotifyFiltered() + " " + "controlFiltered=" + getControlFiltered() + " " + "priority=" + getPriority() + " " + "appdefId=" + getAppdefId() + " " + "appdefType=" + getAppdefType() + " " + "frequencyType=" + getFrequencyType() + " " + "count=" + getCount() + " " + "range=" + getRange() + " " + "actOnTriggerId=" + getActOnTriggerId() + " " + "deleted=" + getDeleted());
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
	  if (other instanceof AlertDefinitionValue)
	  {
		 AlertDefinitionValue that = (AlertDefinitionValue) other;
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
	  if (other instanceof AlertDefinitionValue)
	  {
		 AlertDefinitionValue that = (AlertDefinitionValue) other;
		 boolean lEquals = true;
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }
		 lEquals = lEquals && this.ctime == that.ctime;
		 lEquals = lEquals && this.mtime == that.mtime;
		 if( this.parentId == null )
		 {
			lEquals = lEquals && ( that.parentId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.parentId.equals( that.parentId );
		 }
		 if( this.description == null )
		 {
			lEquals = lEquals && ( that.description == null );
		 }
		 else
		 {
			lEquals = lEquals && this.description.equals( that.description );
		 }
		 lEquals = lEquals && this.enabled == that.enabled;
		 lEquals = lEquals && this.willRecover == that.willRecover;
		 lEquals = lEquals && this.notifyFiltered == that.notifyFiltered;
		 lEquals = lEquals && this.controlFiltered == that.controlFiltered;
		 lEquals = lEquals && this.priority == that.priority;
		 lEquals = lEquals && this.appdefId == that.appdefId;
		 lEquals = lEquals && this.appdefType == that.appdefType;
		 lEquals = lEquals && this.frequencyType == that.frequencyType;
		 lEquals = lEquals && this.count == that.count;
		 lEquals = lEquals && this.range == that.range;
		 lEquals = lEquals && this.actOnTriggerId == that.actOnTriggerId;
		 lEquals = lEquals && this.deleted == that.deleted;
		 if( this.getTriggers() == null )
		 {
			lEquals = lEquals && ( that.getTriggers() == null );
		 }
		 else
		 {
            // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
            // - javier 7/16/03
            java.util.Collection cmr1 = java.util.Arrays.asList(this.getTriggers());
            java.util.Collection cmr2 = java.util.Arrays.asList(that.getTriggers());
			// lEquals = lEquals && java.util.Arrays.equals(this.getTriggers() , that.getTriggers()) ;
            lEquals = lEquals && cmr1.containsAll(cmr2);
		 }
		 if( this.getConditions() == null )
		 {
			lEquals = lEquals && ( that.getConditions() == null );
		 }
		 else
		 {
            // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
            // - javier 7/16/03
            java.util.Collection cmr1 = java.util.Arrays.asList(this.getConditions());
            java.util.Collection cmr2 = java.util.Arrays.asList(that.getConditions());
			// lEquals = lEquals && java.util.Arrays.equals(this.getConditions() , that.getConditions()) ;
            lEquals = lEquals && cmr1.containsAll(cmr2);
		 }
		 if( this.getActions() == null )
		 {
			lEquals = lEquals && ( that.getActions() == null );
		 }
		 else
		 {
            // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
            // - javier 7/16/03
            java.util.Collection cmr1 = java.util.Arrays.asList(this.getActions());
            java.util.Collection cmr2 = java.util.Arrays.asList(that.getActions());
			// lEquals = lEquals && java.util.Arrays.equals(this.getActions() , that.getActions()) ;
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

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + (int)(ctime^(ctime>>>32));

      result = 37*result + (int)(mtime^(mtime>>>32));

      result = 37*result + ((this.parentId != null) ? this.parentId.hashCode() : 0);

      result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

      result = 37*result + (enabled ? 0 : 1);

      result = 37*result + (willRecover ? 0 : 1);

      result = 37*result + (notifyFiltered ? 0 : 1);

      result = 37*result + (controlFiltered ? 0 : 1);

      result = 37*result + (int) priority;

      result = 37*result + (int) appdefId;

      result = 37*result + (int) appdefType;

      result = 37*result + (int) frequencyType;

      result = 37*result + (int)(count^(count>>>32));

      result = 37*result + (int)(range^(range>>>32));

      result = 37*result + (int) actOnTriggerId;

      result = 37*result + (deleted ? 0 : 1);

	  result = 37*result + ((this.getTriggers() != null) ? this.getTriggers().hashCode() : 0);
	  result = 37*result + ((this.getConditions() != null) ? this.getConditions().hashCode() : 0);
	  result = 37*result + ((this.getActions() != null) ? this.getActions().hashCode() : 0);
	  return result;
   }

}
