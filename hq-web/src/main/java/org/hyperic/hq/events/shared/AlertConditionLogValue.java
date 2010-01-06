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
 * Value object for AlertConditionLog.
 *
 */
public class AlertConditionLogValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String value;
   private boolean valueHasBeenSet = false;
   private org.hyperic.hq.events.shared.AlertConditionValue Condition;
   private boolean ConditionHasBeenSet = false;

   public AlertConditionLogValue()
   {
   }

   public AlertConditionLogValue( Integer id,String value )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.value = value;
	  valueHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public AlertConditionLogValue( AlertConditionLogValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.value = otherValue.value;
	  valueHasBeenSet = true;
	// TODO Clone is better no ?
	  this.Condition = otherValue.Condition;
	  ConditionHasBeenSet = true;

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
   public String getValue()
   {
	  return this.value;
   }

   public void setValue( String value )
   {
	  this.value = value;
	  valueHasBeenSet = true;

   }

   public boolean valueHasBeenSet(){
	  return valueHasBeenSet;
   }

   public org.hyperic.hq.events.shared.AlertConditionValue getCondition()
   {
	  return this.Condition;
   }
   public void setCondition( org.hyperic.hq.events.shared.AlertConditionValue Condition )
   {
	  this.Condition = Condition;
	  ConditionHasBeenSet = true;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "value=" + getValue());
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
	  if (other instanceof AlertConditionLogValue)
	  {
		 AlertConditionLogValue that = (AlertConditionLogValue) other;
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
	  if (other instanceof AlertConditionLogValue)
	  {
		 AlertConditionLogValue that = (AlertConditionLogValue) other;
		 boolean lEquals = true;
		 if( this.value == null )
		 {
			lEquals = lEquals && ( that.value == null );
		 }
		 else
		 {
			lEquals = lEquals && this.value.equals( that.value );
		 }
		 if( this.Condition == null )
		 {
			lEquals = lEquals && ( that.Condition == null );
		 }
		 else
		 {
			lEquals = lEquals && this.Condition.equals( that.Condition );
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

      result = 37*result + ((this.value != null) ? this.value.hashCode() : 0);

	  result = 37*result + ((this.Condition != null) ? this.Condition.hashCode() : 0);
	  return result;
   }

}
