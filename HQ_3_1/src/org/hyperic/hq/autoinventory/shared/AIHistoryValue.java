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
 * xdoclet generated code.
 * legacy DTO pattern (targeted to be replaced with hibernate pojo).
 */
package org.hyperic.hq.autoinventory.shared;

import org.hyperic.hq.autoinventory.ScanConfigurationCore;

/**
 * Value object for AIHistory.
 *
 */
public class AIHistoryValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private Integer groupId;
   private boolean groupIdHasBeenSet = false;
   private Integer batchId;
   private boolean batchIdHasBeenSet = false;
   private Integer entityType;
   private boolean entityTypeHasBeenSet = false;
   private Integer entityId;
   private boolean entityIdHasBeenSet = false;
   private String entityName;
   private boolean entityNameHasBeenSet = false;
   private String subject;
   private boolean subjectHasBeenSet = false;
   private Boolean scheduled;
   private boolean scheduledHasBeenSet = false;
   private long dateScheduled;
   private boolean dateScheduledHasBeenSet = false;
   private long startTime;
   private boolean startTimeHasBeenSet = false;
   private long endTime;
   private boolean endTimeHasBeenSet = false;
   private long duration;
   private boolean durationHasBeenSet = false;
   private String message;
   private boolean messageHasBeenSet = false;
   private String description;
   private boolean descriptionHasBeenSet = false;
   private String status;
   private boolean statusHasBeenSet = false;
   private String scanName;
   private boolean scanNameHasBeenSet = false;
   private String scanDesc;
   private boolean scanDescHasBeenSet = false;
   private byte[] config;
   private boolean configHasBeenSet = false;
   private ScanConfigurationCore configObj;
   private boolean configObjHasBeenSet = false;

   public AIHistoryValue()
   {
   }

   public AIHistoryValue( Integer id,Integer groupId,Integer batchId,Integer entityType,Integer entityId,String entityName,String subject,Boolean scheduled,long dateScheduled,long startTime,long endTime,long duration,String message,String description,String status,String scanName,String scanDesc,byte[] config,ScanConfigurationCore configObj )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.groupId = groupId;
	  groupIdHasBeenSet = true;
	  this.batchId = batchId;
	  batchIdHasBeenSet = true;
	  this.entityType = entityType;
	  entityTypeHasBeenSet = true;
	  this.entityId = entityId;
	  entityIdHasBeenSet = true;
	  this.entityName = entityName;
	  entityNameHasBeenSet = true;
	  this.subject = subject;
	  subjectHasBeenSet = true;
	  this.scheduled = scheduled;
	  scheduledHasBeenSet = true;
	  this.dateScheduled = dateScheduled;
	  dateScheduledHasBeenSet = true;
	  this.startTime = startTime;
	  startTimeHasBeenSet = true;
	  this.endTime = endTime;
	  endTimeHasBeenSet = true;
	  this.duration = duration;
	  durationHasBeenSet = true;
	  this.message = message;
	  messageHasBeenSet = true;
	  this.description = description;
	  descriptionHasBeenSet = true;
	  this.status = status;
	  statusHasBeenSet = true;
	  this.scanName = scanName;
	  scanNameHasBeenSet = true;
	  this.scanDesc = scanDesc;
	  scanDescHasBeenSet = true;
	  this.config = config;
	  configHasBeenSet = true;
	  this.configObj = configObj;
	  configObjHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public AIHistoryValue( AIHistoryValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.groupId = otherValue.groupId;
	  groupIdHasBeenSet = true;
	  this.batchId = otherValue.batchId;
	  batchIdHasBeenSet = true;
	  this.entityType = otherValue.entityType;
	  entityTypeHasBeenSet = true;
	  this.entityId = otherValue.entityId;
	  entityIdHasBeenSet = true;
	  this.entityName = otherValue.entityName;
	  entityNameHasBeenSet = true;
	  this.subject = otherValue.subject;
	  subjectHasBeenSet = true;
	  this.scheduled = otherValue.scheduled;
	  scheduledHasBeenSet = true;
	  this.dateScheduled = otherValue.dateScheduled;
	  dateScheduledHasBeenSet = true;
	  this.startTime = otherValue.startTime;
	  startTimeHasBeenSet = true;
	  this.endTime = otherValue.endTime;
	  endTimeHasBeenSet = true;
	  this.duration = otherValue.duration;
	  durationHasBeenSet = true;
	  this.message = otherValue.message;
	  messageHasBeenSet = true;
	  this.description = otherValue.description;
	  descriptionHasBeenSet = true;
	  this.status = otherValue.status;
	  statusHasBeenSet = true;
	  this.scanName = otherValue.scanName;
	  scanNameHasBeenSet = true;
	  this.scanDesc = otherValue.scanDesc;
	  scanDescHasBeenSet = true;
	  this.config = otherValue.config;
	  configHasBeenSet = true;
	  this.configObj = otherValue.configObj;
	  configObjHasBeenSet = true;
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
   public Integer getGroupId()
   {
	  return this.groupId;
   }

   public void setGroupId( Integer groupId )
   {
	  this.groupId = groupId;
	  groupIdHasBeenSet = true;

   }

   public boolean groupIdHasBeenSet(){
	  return groupIdHasBeenSet;
   }
   public Integer getBatchId()
   {
	  return this.batchId;
   }

   public void setBatchId( Integer batchId )
   {
	  this.batchId = batchId;
	  batchIdHasBeenSet = true;

   }

   public boolean batchIdHasBeenSet(){
	  return batchIdHasBeenSet;
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
   public String getEntityName()
   {
	  return this.entityName;
   }

   public void setEntityName( String entityName )
   {
	  this.entityName = entityName;
	  entityNameHasBeenSet = true;

   }

   public boolean entityNameHasBeenSet(){
	  return entityNameHasBeenSet;
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
   public Boolean getScheduled()
   {
	  return this.scheduled;
   }

   public void setScheduled( Boolean scheduled )
   {
	  this.scheduled = scheduled;
	  scheduledHasBeenSet = true;

   }

   public boolean scheduledHasBeenSet(){
	  return scheduledHasBeenSet;
   }
   public long getDateScheduled()
   {
	  return this.dateScheduled;
   }

   public void setDateScheduled( long dateScheduled )
   {
	  this.dateScheduled = dateScheduled;
	  dateScheduledHasBeenSet = true;

   }

   public boolean dateScheduledHasBeenSet(){
	  return dateScheduledHasBeenSet;
   }
   public long getStartTime()
   {
	  return this.startTime;
   }

   public void setStartTime( long startTime )
   {
	  this.startTime = startTime;
	  startTimeHasBeenSet = true;

   }

   public boolean startTimeHasBeenSet(){
	  return startTimeHasBeenSet;
   }
   public long getEndTime()
   {
	  return this.endTime;
   }

   public void setEndTime( long endTime )
   {
	  this.endTime = endTime;
	  endTimeHasBeenSet = true;

   }

   public boolean endTimeHasBeenSet(){
	  return endTimeHasBeenSet;
   }
   public long getDuration()
   {
	  return this.duration;
   }

   public void setDuration( long duration )
   {
	  this.duration = duration;
	  durationHasBeenSet = true;

   }

   public boolean durationHasBeenSet(){
	  return durationHasBeenSet;
   }
   public String getMessage()
   {
	  return this.message;
   }

   public void setMessage( String message )
   {
	  this.message = message;
	  messageHasBeenSet = true;

   }

   public boolean messageHasBeenSet(){
	  return messageHasBeenSet;
   }
   public String getDescription()
   {
	  return this.description;
   }

   public void setDescription( String description )
   {
	  this.description = description;
	  descriptionHasBeenSet = true;

   }

   public boolean descriptionHasBeenSet(){
	  return descriptionHasBeenSet;
   }
   public String getStatus()
   {
	  return this.status;
   }

   public void setStatus( String status )
   {
	  this.status = status;
	  statusHasBeenSet = true;

   }

   public boolean statusHasBeenSet(){
	  return statusHasBeenSet;
   }
   public String getScanName()
   {
	  return this.scanName;
   }

   public void setScanName( String scanName )
   {
	  this.scanName = scanName;
	  scanNameHasBeenSet = true;

   }

   public boolean scanNameHasBeenSet(){
	  return scanNameHasBeenSet;
   }
   public String getScanDesc()
   {
	  return this.scanDesc;
   }

   public void setScanDesc( String scanDesc )
   {
	  this.scanDesc = scanDesc;
	  scanDescHasBeenSet = true;

   }

   public boolean scanDescHasBeenSet(){
	  return scanDescHasBeenSet;
   }
   public byte[] getConfig()
   {
	  return this.config;
   }

   public void setConfig( byte[] config )
   {
	  this.config = config;
	  configHasBeenSet = true;

   }

   public boolean configHasBeenSet(){
	  return configHasBeenSet;
   }
   public ScanConfigurationCore getConfigObj()
   {
	  return this.configObj;
   }

   public void setConfigObj( ScanConfigurationCore configObj )
   {
	  this.configObj = configObj;
	  configObjHasBeenSet = true;

   }

   public boolean configObjHasBeenSet(){
	  return configObjHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "groupId=" + getGroupId() + " " + "batchId=" + getBatchId() + " " + "entityType=" + getEntityType() + " " + "entityId=" + getEntityId() + " " + "entityName=" + getEntityName() + " " + "subject=" + getSubject() + " " + "scheduled=" + getScheduled() + " " + "dateScheduled=" + getDateScheduled() + " " + "startTime=" + getStartTime() + " " + "endTime=" + getEndTime() + " " + "duration=" + getDuration() + " " + "message=" + getMessage() + " " + "description=" + getDescription() + " " + "status=" + getStatus() + " " + "scanName=" + getScanName() + " " + "scanDesc=" + getScanDesc() + " " + "config=" + getConfig() + " " + "configObj=" + getConfigObj());
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
	  if (other instanceof AIHistoryValue)
	  {
		 AIHistoryValue that = (AIHistoryValue) other;
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
	  if (other instanceof AIHistoryValue)
	  {
		 AIHistoryValue that = (AIHistoryValue) other;
		 boolean lEquals = true;
		 if( this.groupId == null )
		 {
			lEquals = lEquals && ( that.groupId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.groupId.equals( that.groupId );
		 }
		 if( this.batchId == null )
		 {
			lEquals = lEquals && ( that.batchId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.batchId.equals( that.batchId );
		 }
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
		 if( this.entityName == null )
		 {
			lEquals = lEquals && ( that.entityName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.entityName.equals( that.entityName );
		 }
		 if( this.subject == null )
		 {
			lEquals = lEquals && ( that.subject == null );
		 }
		 else
		 {
			lEquals = lEquals && this.subject.equals( that.subject );
		 }
		 if( this.scheduled == null )
		 {
			lEquals = lEquals && ( that.scheduled == null );
		 }
		 else
		 {
			lEquals = lEquals && this.scheduled.equals( that.scheduled );
		 }
		 lEquals = lEquals && this.dateScheduled == that.dateScheduled;
		 lEquals = lEquals && this.startTime == that.startTime;
		 lEquals = lEquals && this.endTime == that.endTime;
		 lEquals = lEquals && this.duration == that.duration;
		 if( this.message == null )
		 {
			lEquals = lEquals && ( that.message == null );
		 }
		 else
		 {
			lEquals = lEquals && this.message.equals( that.message );
		 }
		 if( this.description == null )
		 {
			lEquals = lEquals && ( that.description == null );
		 }
		 else
		 {
			lEquals = lEquals && this.description.equals( that.description );
		 }
		 if( this.status == null )
		 {
			lEquals = lEquals && ( that.status == null );
		 }
		 else
		 {
			lEquals = lEquals && this.status.equals( that.status );
		 }
		 if( this.scanName == null )
		 {
			lEquals = lEquals && ( that.scanName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.scanName.equals( that.scanName );
		 }
		 if( this.scanDesc == null )
		 {
			lEquals = lEquals && ( that.scanDesc == null );
		 }
		 else
		 {
			lEquals = lEquals && this.scanDesc.equals( that.scanDesc );
		 }
		 lEquals = lEquals && this.config == that.config;
		 if( this.configObj == null )
		 {
			lEquals = lEquals && ( that.configObj == null );
		 }
		 else
		 {
			lEquals = lEquals && this.configObj.equals( that.configObj );
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

      result = 37*result + ((this.groupId != null) ? this.groupId.hashCode() : 0);

      result = 37*result + ((this.batchId != null) ? this.batchId.hashCode() : 0);

      result = 37*result + ((this.entityType != null) ? this.entityType.hashCode() : 0);

      result = 37*result + ((this.entityId != null) ? this.entityId.hashCode() : 0);

      result = 37*result + ((this.entityName != null) ? this.entityName.hashCode() : 0);

      result = 37*result + ((this.subject != null) ? this.subject.hashCode() : 0);

      result = 37*result + ((this.scheduled != null) ? this.scheduled.hashCode() : 0);

      result = 37*result + (int)(dateScheduled^(dateScheduled>>>32));

      result = 37*result + (int)(startTime^(startTime>>>32));

      result = 37*result + (int)(endTime^(endTime>>>32));

      result = 37*result + (int)(duration^(duration>>>32));

      result = 37*result + ((this.message != null) ? this.message.hashCode() : 0);

      result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

      result = 37*result + ((this.status != null) ? this.status.hashCode() : 0);

      result = 37*result + ((this.scanName != null) ? this.scanName.hashCode() : 0);

      result = 37*result + ((this.scanDesc != null) ? this.scanDesc.hashCode() : 0);

      for (int i=0;  config != null && i<config.length; i++)
      {
         long l = config[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      result = 37*result + ((this.configObj != null) ? this.configObj.hashCode() : 0);

	  return result;
   }

}
