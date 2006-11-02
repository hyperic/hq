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
 * Value object for AlertActionLog.
 *
 */
public class AlertActionLogValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String detail;
   private boolean detailHasBeenSet = false;
   private Integer actionId;
   private boolean actionIdHasBeenSet = false;


   public AlertActionLogValue()
   {
   }

   public AlertActionLogValue( Integer id,String detail,Integer actionId )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.detail = detail;
	  detailHasBeenSet = true;
	  this.actionId = actionId;
	  actionIdHasBeenSet = true;
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
