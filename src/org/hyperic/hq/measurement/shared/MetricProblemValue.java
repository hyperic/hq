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
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * Value object for MetricProblem.
 *
 */
public class MetricProblemValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer measurementId;
   private boolean measurementIdHasBeenSet = false;
   private long timestamp;
   private boolean timestampHasBeenSet = false;
   private int type;
   private boolean typeHasBeenSet = false;
   private Integer additional;
   private boolean additionalHasBeenSet = false;

   public MetricProblemValue() {}

   public MetricProblemValue( Integer measurementId,long timestamp,int type,Integer additional )
   {
	  this.measurementId = measurementId;
	  measurementIdHasBeenSet = true;
	  this.timestamp = timestamp;
	  timestampHasBeenSet = true;
	  this.type = type;
	  typeHasBeenSet = true;
	  this.additional = additional;
	  additionalHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public MetricProblemValue( MetricProblemValue otherValue )
   {
	  this.measurementId = otherValue.measurementId;
	  measurementIdHasBeenSet = true;
	  this.timestamp = otherValue.timestamp;
	  timestampHasBeenSet = true;
	  this.type = otherValue.type;
	  typeHasBeenSet = true;
	  this.additional = otherValue.additional;
	  additionalHasBeenSet = true;
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
   public int getType()
   {
	  return this.type;
   }

   public void setType( int type )
   {
	  this.type = type;
	  typeHasBeenSet = true;
   }

   public boolean typeHasBeenSet(){
	  return typeHasBeenSet;
   }
   public Integer getAdditional()
   {
	  return this.additional;
   }

   public void setAdditional( Integer additional )
   {
	  this.additional = additional;
	  additionalHasBeenSet = true;
   }

   public boolean additionalHasBeenSet(){
	  return additionalHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("measurementId=" + getMeasurementId() + " " + "timestamp=" + getTimestamp() + " " + "type=" + getType() + " " + "additional=" + getAdditional());
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
	  ret = ret && measurementIdHasBeenSet;
	  ret = ret && timestampHasBeenSet;
	  ret = ret && typeHasBeenSet;
	  ret = ret && additionalHasBeenSet;
	  return ret;
   }

   public boolean equals(Object other)
   {
	  if ( ! hasIdentity() ) return false;
	  if (other instanceof MetricProblemValue)
	  {
		 MetricProblemValue that = (MetricProblemValue) other;
		 if ( ! that.hasIdentity() ) return false;
		 boolean lEquals = true;
		 if( this.measurementId == null )
		 {
			lEquals = lEquals && ( that.measurementId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.measurementId.equals( that.measurementId );
		 }
		 lEquals = lEquals && this.timestamp == that.timestamp;
		 lEquals = lEquals && this.type == that.type;
		 if( this.additional == null )
		 {
			lEquals = lEquals && ( that.additional == null );
		 }
		 else
		 {
			lEquals = lEquals && this.additional.equals( that.additional );
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
	  if (other instanceof MetricProblemValue)
	  {
		 MetricProblemValue that = (MetricProblemValue) other;
		 boolean lEquals = true;

		 return lEquals;
	  }
	  else
	  {
		 return false;
	  }
   }

   public int hashCode(){
	  int result = 17;
      result = 37*result + ((this.measurementId != null) ? this.measurementId.hashCode() : 0);

      result = 37*result + (int)(timestamp^(timestamp>>>32));

      result = 37*result + (int) type;

      result = 37*result + ((this.additional != null) ? this.additional.hashCode() : 0);

	  return result;
   }

}
