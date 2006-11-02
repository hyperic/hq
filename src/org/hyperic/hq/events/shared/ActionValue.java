/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.events.shared;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;
import org.hyperic.hq.events.shared.ActionPK;

/**
 * Value object for Action.
 *
 */
public class ActionValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String classname;
   private boolean classnameHasBeenSet = false;
   private byte[] config;
   private boolean configHasBeenSet = false;
   private Integer parentId;
   private boolean parentIdHasBeenSet = false;

   private org.hyperic.hq.events.shared.ActionPK pk;

   public ActionValue()
   {
	  pk = new org.hyperic.hq.events.shared.ActionPK();
   }

   public ActionValue( Integer id,String classname,byte[] config,Integer parentId )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.classname = classname;
	  classnameHasBeenSet = true;
	  this.config = config;
	  configHasBeenSet = true;
	  this.parentId = parentId;
	  parentIdHasBeenSet = true;
	  pk = new org.hyperic.hq.events.shared.ActionPK(this.getId());
   }

   //TODO Cloneable is better than this !
   public ActionValue( ActionValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.classname = otherValue.classname;
	  classnameHasBeenSet = true;
	  this.config = otherValue.config;
	  configHasBeenSet = true;
	  this.parentId = otherValue.parentId;
	  parentIdHasBeenSet = true;

	  pk = new org.hyperic.hq.events.shared.ActionPK(this.getId());
   }

   public org.hyperic.hq.events.shared.ActionPK getPrimaryKey()
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
   public String getClassname()
   {
	  return this.classname;
   }

   public void setClassname( String classname )
   {
	  this.classname = classname;
	  classnameHasBeenSet = true;

   }

   public boolean classnameHasBeenSet(){
	  return classnameHasBeenSet;
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
   public Integer getParentId()
   {
	  return this.parentId;
   }

   public void setParentId( Integer parentId )
   {
	  this.parentId = parentId;
	  parentIdHasBeenSet = true;

   }

   public boolean parentIdHasBeenSet(){
	  return parentIdHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "classname=" + getClassname() + " " + "config=" + getConfig() + " " + "parentId=" + getParentId());
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
	  if (other instanceof ActionValue)
	  {
		 ActionValue that = (ActionValue) other;
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
	  if (other instanceof ActionValue)
	  {
		 ActionValue that = (ActionValue) other;
		 boolean lEquals = true;
		 if( this.classname == null )
		 {
			lEquals = lEquals && ( that.classname == null );
		 }
		 else
		 {
			lEquals = lEquals && this.classname.equals( that.classname );
		 }
		 lEquals = lEquals && this.config == that.config;
		 if( this.parentId == null )
		 {
			lEquals = lEquals && ( that.parentId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.parentId.equals( that.parentId );
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

      result = 37*result + ((this.classname != null) ? this.classname.hashCode() : 0);

      for (int i=0;  config != null && i<config.length; i++)
      {
         long l = config[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      result = 37*result + ((this.parentId != null) ? this.parentId.hashCode() : 0);

	  return result;
   }

}
