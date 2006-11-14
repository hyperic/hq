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
 * Value object for Baseline.
 *
 */
public class BaselineValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private Integer measurementId;
   private boolean measurementIdHasBeenSet = false;
   private long computeTime;
   private boolean computeTimeHasBeenSet = false;
   private boolean userEntered;
   private boolean userEnteredHasBeenSet = false;
   private Double mean;
   private boolean meanHasBeenSet = false;
   private Double minExpectedValue;
   private boolean minExpectedValueHasBeenSet = false;
   private Double maxExpectedValue;
   private boolean maxExpectedValueHasBeenSet = false;


   public BaselineValue() {}

   public BaselineValue( Integer id,Integer measurementId,long computeTime,boolean userEntered,Double mean,Double minExpectedValue,Double maxExpectedValue )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.measurementId = measurementId;
	  measurementIdHasBeenSet = true;
	  this.computeTime = computeTime;
	  computeTimeHasBeenSet = true;
	  this.userEntered = userEntered;
	  userEnteredHasBeenSet = true;
	  this.mean = mean;
	  meanHasBeenSet = true;
	  this.minExpectedValue = minExpectedValue;
	  minExpectedValueHasBeenSet = true;
	  this.maxExpectedValue = maxExpectedValue;
	  maxExpectedValueHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public BaselineValue( BaselineValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.measurementId = otherValue.measurementId;
	  measurementIdHasBeenSet = true;
	  this.computeTime = otherValue.computeTime;
	  computeTimeHasBeenSet = true;
	  this.userEntered = otherValue.userEntered;
	  userEnteredHasBeenSet = true;
	  this.mean = otherValue.mean;
	  meanHasBeenSet = true;
	  this.minExpectedValue = otherValue.minExpectedValue;
	  minExpectedValueHasBeenSet = true;
	  this.maxExpectedValue = otherValue.maxExpectedValue;
	  maxExpectedValueHasBeenSet = true;
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
   public Integer getMeasurementId()
   {
	  return this.measurementId;
   }

   public void setMeasurementId( Integer measurementId )
   {
	  this.measurementId = measurementId;
	  measurementIdHasBeenSet = true;

   }

   public boolean measurementIdHasBeenSet(){
	  return measurementIdHasBeenSet;
   }
   public long getComputeTime()
   {
	  return this.computeTime;
   }

   public void setComputeTime( long computeTime )
   {
	  this.computeTime = computeTime;
	  computeTimeHasBeenSet = true;

   }

   public boolean computeTimeHasBeenSet(){
	  return computeTimeHasBeenSet;
   }
   public boolean getUserEntered()
   {
	  return this.userEntered;
   }

   public void setUserEntered( boolean userEntered )
   {
	  this.userEntered = userEntered;
	  userEnteredHasBeenSet = true;

   }

   public boolean userEnteredHasBeenSet(){
	  return userEnteredHasBeenSet;
   }
   public Double getMean()
   {
	  return this.mean;
   }

   public void setMean( Double mean )
   {
	  this.mean = mean;
	  meanHasBeenSet = true;

   }

   public boolean meanHasBeenSet(){
	  return meanHasBeenSet;
   }
   public Double getMinExpectedValue()
   {
	  return this.minExpectedValue;
   }

   public void setMinExpectedValue( Double minExpectedValue )
   {
	  this.minExpectedValue = minExpectedValue;
	  minExpectedValueHasBeenSet = true;

   }

   public boolean minExpectedValueHasBeenSet(){
	  return minExpectedValueHasBeenSet;
   }
   public Double getMaxExpectedValue()
   {
	  return this.maxExpectedValue;
   }

   public void setMaxExpectedValue( Double maxExpectedValue )
   {
	  this.maxExpectedValue = maxExpectedValue;
	  maxExpectedValueHasBeenSet = true;

   }

   public boolean maxExpectedValueHasBeenSet(){
	  return maxExpectedValueHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "measurementId=" + getMeasurementId() + " " + "computeTime=" + getComputeTime() + " " + "userEntered=" + getUserEntered() + " " + "mean=" + getMean() + " " + "minExpectedValue=" + getMinExpectedValue() + " " + "maxExpectedValue=" + getMaxExpectedValue());
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
	  if (other instanceof BaselineValue)
	  {
		 BaselineValue that = (BaselineValue) other;
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
	  if (other instanceof BaselineValue)
	  {
		 BaselineValue that = (BaselineValue) other;
		 boolean lEquals = true;
		 if( this.measurementId == null )
		 {
			lEquals = lEquals && ( that.measurementId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.measurementId.equals( that.measurementId );
		 }
		 lEquals = lEquals && this.computeTime == that.computeTime;
		 lEquals = lEquals && this.userEntered == that.userEntered;
		 if( this.mean == null )
		 {
			lEquals = lEquals && ( that.mean == null );
		 }
		 else
		 {
			lEquals = lEquals && this.mean.equals( that.mean );
		 }
		 if( this.minExpectedValue == null )
		 {
			lEquals = lEquals && ( that.minExpectedValue == null );
		 }
		 else
		 {
			lEquals = lEquals && this.minExpectedValue.equals( that.minExpectedValue );
		 }
		 if( this.maxExpectedValue == null )
		 {
			lEquals = lEquals && ( that.maxExpectedValue == null );
		 }
		 else
		 {
			lEquals = lEquals && this.maxExpectedValue.equals( that.maxExpectedValue );
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

      result = 37*result + ((this.measurementId != null) ? this.measurementId.hashCode() : 0);

      result = 37*result + (int)(computeTime^(computeTime>>>32));

      result = 37*result + (userEntered ? 0 : 1);

      result = 37*result + ((this.mean != null) ? this.mean.hashCode() : 0);

      result = 37*result + ((this.minExpectedValue != null) ? this.minExpectedValue.hashCode() : 0);

      result = 37*result + ((this.maxExpectedValue != null) ? this.maxExpectedValue.hashCode() : 0);

	  return result;
   }

}
