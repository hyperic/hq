/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.events.shared;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;
import org.hyperic.hq.events.shared.UserAlertPK;

/**
 * Value object for UserAlert.
 *
 */
public class UserAlertValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private Integer userId;
   private boolean userIdHasBeenSet = false;
   private org.hyperic.hq.events.shared.AlertValue Alert;
   private boolean AlertHasBeenSet = false;

   private org.hyperic.hq.events.shared.UserAlertPK pk;

   public UserAlertValue()
   {
	  pk = new org.hyperic.hq.events.shared.UserAlertPK();
   }

   public UserAlertValue( Integer id,Integer userId )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.userId = userId;
	  userIdHasBeenSet = true;
	  pk = new org.hyperic.hq.events.shared.UserAlertPK(this.getId());
   }

   //TODO Cloneable is better than this !
   public UserAlertValue( UserAlertValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.userId = otherValue.userId;
	  userIdHasBeenSet = true;
	// TODO Clone is better no ?
	  this.Alert = otherValue.Alert;
	  AlertHasBeenSet = true;

	  pk = new org.hyperic.hq.events.shared.UserAlertPK(this.getId());
   }

   public org.hyperic.hq.events.shared.UserAlertPK getPrimaryKey()
   {
	  return pk;
   }

   public Integer getId()
   {
	  return this.id;
   }

   public void setId( Integer id )
   {
	  this.id = id;
	  idHasBeenSet = true;

		 pk.setId(id);
   }

   public boolean idHasBeenSet(){
	  return idHasBeenSet;
   }
   public Integer getUserId()
   {
	  return this.userId;
   }

   public void setUserId( Integer userId )
   {
	  this.userId = userId;
	  userIdHasBeenSet = true;

   }

   public boolean userIdHasBeenSet(){
	  return userIdHasBeenSet;
   }

   public org.hyperic.hq.events.shared.AlertValue getAlert()
   {
	  return this.Alert;
   }
   public void setAlert( org.hyperic.hq.events.shared.AlertValue Alert )
   {
	  this.Alert = Alert;
	  AlertHasBeenSet = true;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "userId=" + getUserId());
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
	  if (other instanceof UserAlertValue)
	  {
		 UserAlertValue that = (UserAlertValue) other;
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
	  if (other instanceof UserAlertValue)
	  {
		 UserAlertValue that = (UserAlertValue) other;
		 boolean lEquals = true;
		 if( this.userId == null )
		 {
			lEquals = lEquals && ( that.userId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.userId.equals( that.userId );
		 }
		 if( this.Alert == null )
		 {
			lEquals = lEquals && ( that.Alert == null );
		 }
		 else
		 {
			lEquals = lEquals && this.Alert.equals( that.Alert );
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

      result = 37*result + ((this.userId != null) ? this.userId.hashCode() : 0);

	  result = 37*result + ((this.Alert != null) ? this.Alert.hashCode() : 0);
	  return result;
   }

}
