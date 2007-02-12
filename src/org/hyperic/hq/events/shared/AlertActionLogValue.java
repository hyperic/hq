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

import java.io.Serializable;

public class AlertActionLogValue
   implements Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String detail;
   private boolean detailHasBeenSet = false;
   private Integer actionId;
   private boolean actionIdHasBeenSet = false;
   private long timeStamp;

   public AlertActionLogValue()
   {
   }

   public AlertActionLogValue( Integer id,String detail,Integer actionId,
                               long timeStamp)
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.detail = detail;
	  detailHasBeenSet = true;
	  this.actionId = actionId;
	  actionIdHasBeenSet = true;
      this.timeStamp = timeStamp;
   }

   //TODO Cloneable is better than this !
   public AlertActionLogValue( AlertActionLogValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.detail = otherValue.detail;
	  detailHasBeenSet = true;
	  this.actionId = otherValue.actionId;
	  actionIdHasBeenSet = true;
      this.timeStamp = otherValue.timeStamp;
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
   public Integer getActionId()
   {
	  return this.actionId;
   }

   public void setActionId( Integer actionId )
   {
	  this.actionId = actionId;
	  actionIdHasBeenSet = true;
   }

   public long getTimeStamp() {
       return this.timeStamp;
   }
   
   public void setTimeStamp(long timeStamp) {
       this.timeStamp = timeStamp;
   }
   
   public boolean actionIdHasBeenSet(){
	  return actionIdHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "detail=" + getDetail() + " " + "actionId=" + getActionId());
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
	  if (other instanceof AlertActionLogValue)
	  {
		 AlertActionLogValue that = (AlertActionLogValue) other;
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
	  if (other instanceof AlertActionLogValue)
	  {
		 AlertActionLogValue that = (AlertActionLogValue) other;
		 boolean lEquals = true;
		 if( this.detail == null )
		 {
			lEquals = lEquals && ( that.detail == null );
		 }
		 else
		 {
			lEquals = lEquals && this.detail.equals( that.detail );
		 }
		 if( this.actionId == null )
		 {
			lEquals = lEquals && ( that.actionId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.actionId.equals( that.actionId );
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

      result = 37*result + ((this.detail != null) ? this.detail.hashCode() : 0);

      result = 37*result + ((this.actionId != null) ? this.actionId.hashCode() : 0);

	  return result;
   }

}
