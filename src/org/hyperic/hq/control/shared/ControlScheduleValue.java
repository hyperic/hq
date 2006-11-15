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
 * xdoclet generated file
 * legacy DTO pattern (targeted to be replaced with hibernate pojo)
 */
package org.hyperic.hq.control.shared;

import org.hyperic.hq.scheduler.ScheduleValue;

/**
 * Value object for ControlSchedule.
 *
 */
public class ControlScheduleValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private Integer entityType;
   private boolean entityTypeHasBeenSet = false;
   private Integer entityId;
   private boolean entityIdHasBeenSet = false;
   private String subject;
   private boolean subjectHasBeenSet = false;
   private ScheduleValue scheduleValue;
   private boolean scheduleValueHasBeenSet = false;
   private byte[] scheduleValueBytes;
   private boolean scheduleValueBytesHasBeenSet = false;
   private String triggerName;
   private boolean triggerNameHasBeenSet = false;
   private String jobName;
   private boolean jobNameHasBeenSet = false;
   private long nextFireTime;
   private boolean nextFireTimeHasBeenSet = false;
   private String jobOrderData;
   private boolean jobOrderDataHasBeenSet = false;
   private String action;
   private boolean actionHasBeenSet = false;

   public ControlScheduleValue()
   {
   }

   public ControlScheduleValue( Integer id,Integer entityType,Integer entityId,String subject,ScheduleValue scheduleValue,byte[] scheduleValueBytes,String triggerName,String jobName,long nextFireTime,String jobOrderData,String action )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.entityType = entityType;
	  entityTypeHasBeenSet = true;
	  this.entityId = entityId;
	  entityIdHasBeenSet = true;
	  this.subject = subject;
	  subjectHasBeenSet = true;
	  this.scheduleValue = scheduleValue;
	  scheduleValueHasBeenSet = true;
	  this.scheduleValueBytes = scheduleValueBytes;
	  scheduleValueBytesHasBeenSet = true;
	  this.triggerName = triggerName;
	  triggerNameHasBeenSet = true;
	  this.jobName = jobName;
	  jobNameHasBeenSet = true;
	  this.nextFireTime = nextFireTime;
	  nextFireTimeHasBeenSet = true;
	  this.jobOrderData = jobOrderData;
	  jobOrderDataHasBeenSet = true;
	  this.action = action;
	  actionHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public ControlScheduleValue( ControlScheduleValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.entityType = otherValue.entityType;
	  entityTypeHasBeenSet = true;
	  this.entityId = otherValue.entityId;
	  entityIdHasBeenSet = true;
	  this.subject = otherValue.subject;
	  subjectHasBeenSet = true;
	  this.scheduleValue = otherValue.scheduleValue;
	  scheduleValueHasBeenSet = true;
	  this.scheduleValueBytes = otherValue.scheduleValueBytes;
	  scheduleValueBytesHasBeenSet = true;
	  this.triggerName = otherValue.triggerName;
	  triggerNameHasBeenSet = true;
	  this.jobName = otherValue.jobName;
	  jobNameHasBeenSet = true;
	  this.nextFireTime = otherValue.nextFireTime;
	  nextFireTimeHasBeenSet = true;
	  this.jobOrderData = otherValue.jobOrderData;
	  jobOrderDataHasBeenSet = true;
	  this.action = otherValue.action;
	  actionHasBeenSet = true;
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
   public Integer getEntityType()
   {
	  return this.entityType;
   }

   public void setEntityType( Integer entityType )
   {
	  this.entityType = entityType;
	  entityTypeHasBeenSet = true;

   }

   public boolean entityTypeHasBeenSet(){
	  return entityTypeHasBeenSet;
   }
   public Integer getEntityId()
   {
	  return this.entityId;
   }

   public void setEntityId( Integer entityId )
   {
	  this.entityId = entityId;
	  entityIdHasBeenSet = true;

   }

   public boolean entityIdHasBeenSet(){
	  return entityIdHasBeenSet;
   }
   public String getSubject()
   {
	  return this.subject;
   }

   public void setSubject( String subject )
   {
	  this.subject = subject;
	  subjectHasBeenSet = true;

   }

   public boolean subjectHasBeenSet(){
	  return subjectHasBeenSet;
   }
   public ScheduleValue getScheduleValue()
   {
	  return this.scheduleValue;
   }

   public void setScheduleValue( ScheduleValue scheduleValue )
   {
	  this.scheduleValue = scheduleValue;
	  scheduleValueHasBeenSet = true;

   }

   public boolean scheduleValueHasBeenSet(){
	  return scheduleValueHasBeenSet;
   }
   public byte[] getScheduleValueBytes()
   {
	  return this.scheduleValueBytes;
   }

   public void setScheduleValueBytes( byte[] scheduleValueBytes )
   {
	  this.scheduleValueBytes = scheduleValueBytes;
	  scheduleValueBytesHasBeenSet = true;

   }

   public boolean scheduleValueBytesHasBeenSet(){
	  return scheduleValueBytesHasBeenSet;
   }
   public String getTriggerName()
   {
	  return this.triggerName;
   }

   public void setTriggerName( String triggerName )
   {
	  this.triggerName = triggerName;
	  triggerNameHasBeenSet = true;

   }

   public boolean triggerNameHasBeenSet(){
	  return triggerNameHasBeenSet;
   }
   public String getJobName()
   {
	  return this.jobName;
   }

   public void setJobName( String jobName )
   {
	  this.jobName = jobName;
	  jobNameHasBeenSet = true;

   }

   public boolean jobNameHasBeenSet(){
	  return jobNameHasBeenSet;
   }
   public long getNextFireTime()
   {
	  return this.nextFireTime;
   }

   public void setNextFireTime( long nextFireTime )
   {
	  this.nextFireTime = nextFireTime;
	  nextFireTimeHasBeenSet = true;

   }

   public boolean nextFireTimeHasBeenSet(){
	  return nextFireTimeHasBeenSet;
   }
   public String getJobOrderData()
   {
	  return this.jobOrderData;
   }

   public void setJobOrderData( String jobOrderData )
   {
	  this.jobOrderData = jobOrderData;
	  jobOrderDataHasBeenSet = true;

   }

   public boolean jobOrderDataHasBeenSet(){
	  return jobOrderDataHasBeenSet;
   }
   public String getAction()
   {
	  return this.action;
   }

   public void setAction( String action )
   {
	  this.action = action;
	  actionHasBeenSet = true;

   }

   public boolean actionHasBeenSet(){
	  return actionHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "entityType=" + getEntityType() + " " + "entityId=" + getEntityId() + " " + "subject=" + getSubject() + " " + "scheduleValue=" + getScheduleValue() + " " + "scheduleValueBytes=" + getScheduleValueBytes() + " " + "triggerName=" + getTriggerName() + " " + "jobName=" + getJobName() + " " + "nextFireTime=" + getNextFireTime() + " " + "jobOrderData=" + getJobOrderData() + " " + "action=" + getAction());
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
	  if (other instanceof ControlScheduleValue)
	  {
		 ControlScheduleValue that = (ControlScheduleValue) other;
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
	  if (other instanceof ControlScheduleValue)
	  {
		 ControlScheduleValue that = (ControlScheduleValue) other;
		 boolean lEquals = true;
		 if( this.entityType == null )
		 {
			lEquals = lEquals && ( that.entityType == null );
		 }
		 else
		 {
			lEquals = lEquals && this.entityType.equals( that.entityType );
		 }
		 if( this.entityId == null )
		 {
			lEquals = lEquals && ( that.entityId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.entityId.equals( that.entityId );
		 }
		 if( this.subject == null )
		 {
			lEquals = lEquals && ( that.subject == null );
		 }
		 else
		 {
			lEquals = lEquals && this.subject.equals( that.subject );
		 }
		 if( this.scheduleValue == null )
		 {
			lEquals = lEquals && ( that.scheduleValue == null );
		 }
		 else
		 {
			lEquals = lEquals && this.scheduleValue.equals( that.scheduleValue );
		 }
		 lEquals = lEquals && this.scheduleValueBytes == that.scheduleValueBytes;
		 if( this.triggerName == null )
		 {
			lEquals = lEquals && ( that.triggerName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.triggerName.equals( that.triggerName );
		 }
		 if( this.jobName == null )
		 {
			lEquals = lEquals && ( that.jobName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.jobName.equals( that.jobName );
		 }
		 lEquals = lEquals && this.nextFireTime == that.nextFireTime;
		 if( this.jobOrderData == null )
		 {
			lEquals = lEquals && ( that.jobOrderData == null );
		 }
		 else
		 {
			lEquals = lEquals && this.jobOrderData.equals( that.jobOrderData );
		 }
		 if( this.action == null )
		 {
			lEquals = lEquals && ( that.action == null );
		 }
		 else
		 {
			lEquals = lEquals && this.action.equals( that.action );
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

      result = 37*result + ((this.entityType != null) ? this.entityType.hashCode() : 0);

      result = 37*result + ((this.entityId != null) ? this.entityId.hashCode() : 0);

      result = 37*result + ((this.subject != null) ? this.subject.hashCode() : 0);

      result = 37*result + ((this.scheduleValue != null) ? this.scheduleValue.hashCode() : 0);

      for (int i=0;  scheduleValueBytes != null && i<scheduleValueBytes.length; i++)
      {
         long l = scheduleValueBytes[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      result = 37*result + ((this.triggerName != null) ? this.triggerName.hashCode() : 0);

      result = 37*result + ((this.jobName != null) ? this.jobName.hashCode() : 0);

      result = 37*result + (int)(nextFireTime^(nextFireTime>>>32));

      result = 37*result + ((this.jobOrderData != null) ? this.jobOrderData.hashCode() : 0);

      result = 37*result + ((this.action != null) ? this.action.hashCode() : 0);

	  return result;
   }

}
