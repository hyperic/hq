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


   public ActionValue()
   {
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
