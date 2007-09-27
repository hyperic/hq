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
 * Value object for AlertCondition.
 *
 */
public class AlertConditionValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private int type;
   private boolean typeHasBeenSet = false;
   private boolean required;
   private boolean requiredHasBeenSet = false;
   private int measurementId;
   private boolean measurementIdHasBeenSet = false;
   private String name;
   private boolean nameHasBeenSet = false;
   private String comparator;
   private boolean comparatorHasBeenSet = false;
   private double threshold;
   private boolean thresholdHasBeenSet = false;
   private String option;
   private boolean optionHasBeenSet = false;
   private Integer triggerId;
   private boolean triggerIdHasBeenSet = false;


   public AlertConditionValue()
   {
   }

   public AlertConditionValue( Integer id,int type,boolean required,int measurementId,String name,String comparator,double threshold,String option,Integer triggerId )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.type = type;
	  typeHasBeenSet = true;
	  this.required = required;
	  requiredHasBeenSet = true;
	  this.measurementId = measurementId;
	  measurementIdHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.comparator = comparator;
	  comparatorHasBeenSet = true;
	  this.threshold = threshold;
	  thresholdHasBeenSet = true;
	  this.option = option;
	  optionHasBeenSet = true;
	  this.triggerId = triggerId;
	  triggerIdHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public AlertConditionValue( AlertConditionValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.type = otherValue.type;
	  typeHasBeenSet = true;
	  this.required = otherValue.required;
	  requiredHasBeenSet = true;
	  this.measurementId = otherValue.measurementId;
	  measurementIdHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.comparator = otherValue.comparator;
	  comparatorHasBeenSet = true;
	  this.threshold = otherValue.threshold;
	  thresholdHasBeenSet = true;
	  this.option = otherValue.option;
	  optionHasBeenSet = true;
	  this.triggerId = otherValue.triggerId;
	  triggerIdHasBeenSet = true;

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
   public boolean getRequired()
   {
	  return this.required;
   }

   public void setRequired( boolean required )
   {
	  this.required = required;
	  requiredHasBeenSet = true;

   }

   public boolean requiredHasBeenSet(){
	  return requiredHasBeenSet;
   }
   public int getMeasurementId()
   {
	  return this.measurementId;
   }

   public void setMeasurementId( int measurementId )
   {
	  this.measurementId = measurementId;
	  measurementIdHasBeenSet = true;

   }

   public boolean measurementIdHasBeenSet(){
	  return measurementIdHasBeenSet;
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
   public String getComparator()
   {
	  return this.comparator;
   }

   public void setComparator( String comparator )
   {
	  this.comparator = comparator;
	  comparatorHasBeenSet = true;

   }

   public boolean comparatorHasBeenSet(){
	  return comparatorHasBeenSet;
   }
   public double getThreshold()
   {
	  return this.threshold;
   }

   public void setThreshold( double threshold )
   {
	  this.threshold = threshold;
	  thresholdHasBeenSet = true;

   }

   public boolean thresholdHasBeenSet(){
	  return thresholdHasBeenSet;
   }
   public String getOption()
   {
	  return this.option;
   }

   public void setOption( String option )
   {
	  this.option = option;
	  optionHasBeenSet = true;

   }

   public boolean optionHasBeenSet(){
	  return optionHasBeenSet;
   }
   public Integer getTriggerId()
   {
	  return this.triggerId;
   }

   public void setTriggerId( Integer triggerId )
   {
	  this.triggerId = triggerId;
	  triggerIdHasBeenSet = true;

   }

   public boolean triggerIdHasBeenSet(){
	  return triggerIdHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "type=" + getType() + " " + "required=" + getRequired() + " " + "measurementId=" + getMeasurementId() + " " + "name=" + getName() + " " + "comparator=" + getComparator() + " " + "threshold=" + getThreshold() + " " + "option=" + getOption() + " " + "triggerId=" + getTriggerId());
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
	  if (other instanceof AlertConditionValue)
	  {
		 AlertConditionValue that = (AlertConditionValue) other;
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
	  if (other instanceof AlertConditionValue)
	  {
		 AlertConditionValue that = (AlertConditionValue) other;
		 boolean lEquals = true;
		 lEquals = lEquals && this.type == that.type;
		 lEquals = lEquals && this.required == that.required;
		 lEquals = lEquals && this.measurementId == that.measurementId;
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }
		 if( this.comparator == null )
		 {
			lEquals = lEquals && ( that.comparator == null );
		 }
		 else
		 {
			lEquals = lEquals && this.comparator.equals( that.comparator );
		 }
		 lEquals = lEquals && this.threshold == that.threshold;
		 if( this.option == null )
		 {
			lEquals = lEquals && ( that.option == null );
		 }
		 else
		 {
			lEquals = lEquals && this.option.equals( that.option );
		 }
		 if( this.triggerId == null )
		 {
			lEquals = lEquals && ( that.triggerId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.triggerId.equals( that.triggerId );
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

      result = 37*result + (int) type;

      result = 37*result + (required ? 0 : 1);

      result = 37*result + (int) measurementId;

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.comparator != null) ? this.comparator.hashCode() : 0);

      {
         long l = Double.doubleToLongBits(threshold);
         result = 37*result + (int)(l^(l>>>32));
      }

      result = 37*result + ((this.option != null) ? this.option.hashCode() : 0);

      result = 37*result + ((this.triggerId != null) ? this.triggerId.hashCode() : 0);

	  return result;
   }

}
