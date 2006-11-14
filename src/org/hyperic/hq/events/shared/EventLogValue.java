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
import javax.ejb.RemoveException;

/**
 * Value object for EventLog.
 *
 */
public class EventLogValue
   extends java.lang.Object
   implements java.io.Serializable, org.hyperic.util.data.IEventPoint
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String subject;
   private boolean subjectHasBeenSet = false;
   private int entityType;
   private boolean entityTypeHasBeenSet = false;
   private int entityId;
   private boolean entityIdHasBeenSet = false;
   private String detail;
   private boolean detailHasBeenSet = false;
   private long timestamp;
   private boolean timestampHasBeenSet = false;
   private String type;
   private boolean typeHasBeenSet = false;
   private String status;
   private boolean statusHasBeenSet = false;
   private int eventID;
   private boolean eventIDHasBeenSet = false;

   public EventLogValue()
   {
   }

   public EventLogValue( Integer id,String subject,int entityType,int entityId,String detail,long timestamp,String type,String status,int eventID )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.subject = subject;
	  subjectHasBeenSet = true;
	  this.entityType = entityType;
	  entityTypeHasBeenSet = true;
	  this.entityId = entityId;
	  entityIdHasBeenSet = true;
	  this.detail = detail;
	  detailHasBeenSet = true;
	  this.timestamp = timestamp;
	  timestampHasBeenSet = true;
	  this.type = type;
	  typeHasBeenSet = true;
	  this.status = status;
	  statusHasBeenSet = true;
	  this.eventID = eventID;
	  eventIDHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public EventLogValue( EventLogValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.subject = otherValue.subject;
	  subjectHasBeenSet = true;
	  this.entityType = otherValue.entityType;
	  entityTypeHasBeenSet = true;
	  this.entityId = otherValue.entityId;
	  entityIdHasBeenSet = true;
	  this.detail = otherValue.detail;
	  detailHasBeenSet = true;
	  this.timestamp = otherValue.timestamp;
	  timestampHasBeenSet = true;
	  this.type = otherValue.type;
	  typeHasBeenSet = true;
	  this.status = otherValue.status;
	  statusHasBeenSet = true;
	  this.eventID = otherValue.eventID;
	  eventIDHasBeenSet = true;

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
   public int getEntityType()
   {
	  return this.entityType;
   }

   public void setEntityType( int entityType )
   {
	  this.entityType = entityType;
	  entityTypeHasBeenSet = true;

   }

   public boolean entityTypeHasBeenSet(){
	  return entityTypeHasBeenSet;
   }
   public int getEntityId()
   {
	  return this.entityId;
   }

   public void setEntityId( int entityId )
   {
	  this.entityId = entityId;
	  entityIdHasBeenSet = true;

   }

   public boolean entityIdHasBeenSet(){
	  return entityIdHasBeenSet;
   }
   public String getDetail()
   {
	  return this.detail;
   }

   public void setDetail( String detail )
   {
	  this.detail = detail;
	  detailHasBeenSet = true;

   }

   public boolean detailHasBeenSet(){
	  return detailHasBeenSet;
   }
   public long getTimestamp()
   {
	  return this.timestamp;
   }

   public void setTimestamp( long timestamp )
   {
	  this.timestamp = timestamp;
	  timestampHasBeenSet = true;

   }

   public boolean timestampHasBeenSet(){
	  return timestampHasBeenSet;
   }
   public String getType()
   {
	  return this.type;
   }

   public void setType( String type )
   {
	  this.type = type;
	  typeHasBeenSet = true;

   }

   public boolean typeHasBeenSet(){
	  return typeHasBeenSet;
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
   public int getEventID()
   {
	  return this.eventID;
   }

   public void setEventID( int eventID )
   {
	  this.eventID = eventID;
	  eventIDHasBeenSet = true;

   }

   public boolean eventIDHasBeenSet(){
	  return eventIDHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "subject=" + getSubject() + " " + "entityType=" + getEntityType() + " " + "entityId=" + getEntityId() + " " + "detail=" + getDetail() + " " + "timestamp=" + getTimestamp() + " " + "type=" + getType() + " " + "status=" + getStatus() + " " + "eventID=" + getEventID());
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
	  if (other instanceof EventLogValue)
	  {
		 EventLogValue that = (EventLogValue) other;
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
	  if (other instanceof EventLogValue)
	  {
		 EventLogValue that = (EventLogValue) other;
		 boolean lEquals = true;
		 if( this.subject == null )
		 {
			lEquals = lEquals && ( that.subject == null );
		 }
		 else
		 {
			lEquals = lEquals && this.subject.equals( that.subject );
		 }
		 lEquals = lEquals && this.entityType == that.entityType;
		 lEquals = lEquals && this.entityId == that.entityId;
		 if( this.detail == null )
		 {
			lEquals = lEquals && ( that.detail == null );
		 }
		 else
		 {
			lEquals = lEquals && this.detail.equals( that.detail );
		 }
		 lEquals = lEquals && this.timestamp == that.timestamp;
		 if( this.type == null )
		 {
			lEquals = lEquals && ( that.type == null );
		 }
		 else
		 {
			lEquals = lEquals && this.type.equals( that.type );
		 }
		 if( this.status == null )
		 {
			lEquals = lEquals && ( that.status == null );
		 }
		 else
		 {
			lEquals = lEquals && this.status.equals( that.status );
		 }
		 lEquals = lEquals && this.eventID == that.eventID;

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

      result = 37*result + ((this.subject != null) ? this.subject.hashCode() : 0);

      result = 37*result + (int) entityType;

      result = 37*result + (int) entityId;

      result = 37*result + ((this.detail != null) ? this.detail.hashCode() : 0);

      result = 37*result + (int)(timestamp^(timestamp>>>32));

      result = 37*result + ((this.type != null) ? this.type.hashCode() : 0);

      result = 37*result + ((this.status != null) ? this.status.hashCode() : 0);

      result = 37*result + (int) eventID;

	  return result;
   }

}
