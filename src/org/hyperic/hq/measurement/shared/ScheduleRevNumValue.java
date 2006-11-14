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
package org.hyperic.hq.measurement.shared;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * Value object for ScheduleRevNum.
 *
 */
public class ScheduleRevNumValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private int appdefType;
   private boolean appdefTypeHasBeenSet = false;
   private int instanceId;
   private boolean instanceIdHasBeenSet = false;
   private int sRN;
   private boolean sRNHasBeenSet = false;
   private long minInterval;
   private boolean minIntervalHasBeenSet = false;
   private long lastReported;
   private boolean lastReportedHasBeenSet = false;
   private boolean pending;
   private boolean pendingHasBeenSet = false;

   public ScheduleRevNumValue() {}

   public ScheduleRevNumValue( int appdefType,int instanceId,int sRN,long minInterval,long lastReported,boolean pending )
   {
	  this.appdefType = appdefType;
	  appdefTypeHasBeenSet = true;
	  this.instanceId = instanceId;
	  instanceIdHasBeenSet = true;
	  this.sRN = sRN;
	  sRNHasBeenSet = true;
	  this.minInterval = minInterval;
	  minIntervalHasBeenSet = true;
	  this.lastReported = lastReported;
	  lastReportedHasBeenSet = true;
	  this.pending = pending;
	  pendingHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public ScheduleRevNumValue( ScheduleRevNumValue otherValue )
   {
	  this.appdefType = otherValue.appdefType;
	  appdefTypeHasBeenSet = true;
	  this.instanceId = otherValue.instanceId;
	  instanceIdHasBeenSet = true;
	  this.sRN = otherValue.sRN;
	  sRNHasBeenSet = true;
	  this.minInterval = otherValue.minInterval;
	  minIntervalHasBeenSet = true;
	  this.lastReported = otherValue.lastReported;
	  lastReportedHasBeenSet = true;
	  this.pending = otherValue.pending;
	  pendingHasBeenSet = true;
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
   public int getInstanceId()
   {
	  return this.instanceId;
   }

   public void setInstanceId( int instanceId )
   {
	  this.instanceId = instanceId;
	  instanceIdHasBeenSet = true;
   }

   public boolean instanceIdHasBeenSet(){
	  return instanceIdHasBeenSet;
   }
   public int getSRN()
   {
	  return this.sRN;
   }

   public void setSRN( int sRN )
   {
	  this.sRN = sRN;
	  sRNHasBeenSet = true;

   }

   public boolean sRNHasBeenSet(){
	  return sRNHasBeenSet;
   }
   public long getMinInterval()
   {
	  return this.minInterval;
   }

   public void setMinInterval( long minInterval )
   {
	  this.minInterval = minInterval;
	  minIntervalHasBeenSet = true;

   }

   public boolean minIntervalHasBeenSet(){
	  return minIntervalHasBeenSet;
   }
   public long getLastReported()
   {
	  return this.lastReported;
   }

   public void setLastReported( long lastReported )
   {
	  this.lastReported = lastReported;
	  lastReportedHasBeenSet = true;

   }

   public boolean lastReportedHasBeenSet(){
	  return lastReportedHasBeenSet;
   }
   public boolean getPending()
   {
	  return this.pending;
   }

   public void setPending( boolean pending )
   {
	  this.pending = pending;
	  pendingHasBeenSet = true;

   }

   public boolean pendingHasBeenSet(){
	  return pendingHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("appdefType=" + getAppdefType() + " " + "instanceId=" + getInstanceId() + " " + "sRN=" + getSRN() + " " + "minInterval=" + getMinInterval() + " " + "lastReported=" + getLastReported() + " " + "pending=" + getPending());
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
	  ret = ret && appdefTypeHasBeenSet;
	  ret = ret && instanceIdHasBeenSet;
	  return ret;
   }

   public boolean equals(Object other)
   {
	  if ( ! hasIdentity() ) return false;
	  if (other instanceof ScheduleRevNumValue)
	  {
		 ScheduleRevNumValue that = (ScheduleRevNumValue) other;
		 if ( ! that.hasIdentity() ) return false;
		 boolean lEquals = true;
		 lEquals = lEquals && this.appdefType == that.appdefType;
		 lEquals = lEquals && this.instanceId == that.instanceId;

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
	  if (other instanceof ScheduleRevNumValue)
	  {
		 ScheduleRevNumValue that = (ScheduleRevNumValue) other;
		 boolean lEquals = true;
		 lEquals = lEquals && this.sRN == that.sRN;
		 lEquals = lEquals && this.minInterval == that.minInterval;
		 lEquals = lEquals && this.lastReported == that.lastReported;
		 lEquals = lEquals && this.pending == that.pending;

		 return lEquals;
	  }
	  else
	  {
		 return false;
	  }
   }

   public int hashCode(){
	  int result = 17;
      result = 37*result + (int) appdefType;

      result = 37*result + (int) instanceId;

      result = 37*result + (int) sRN;

      result = 37*result + (int)(minInterval^(minInterval>>>32));

      result = 37*result + (int)(lastReported^(lastReported>>>32));

      result = 37*result + (pending ? 0 : 1);

	  return result;
   }

}
