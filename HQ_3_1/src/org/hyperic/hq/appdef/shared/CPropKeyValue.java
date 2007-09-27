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
 * xdoclet generated code.
 * legacy DTO pattern (targeted to be replaced with hibernate pojo).
 */
package org.hyperic.hq.appdef.shared;

/**
 * Value object for CPropKey.
 *
 */
public class CPropKeyValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private int appdefType;
   private boolean appdefTypeHasBeenSet = false;
   private int appdefTypeId;
   private boolean appdefTypeIdHasBeenSet = false;
   private String key;
   private boolean keyHasBeenSet = false;
   private String description;
   private boolean descriptionHasBeenSet = false;

   public CPropKeyValue()
   {
   }

   public CPropKeyValue( Integer id,int appdefType,int appdefTypeId,String key,String description )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.appdefType = appdefType;
	  appdefTypeHasBeenSet = true;
	  this.appdefTypeId = appdefTypeId;
	  appdefTypeIdHasBeenSet = true;
	  this.key = key;
	  keyHasBeenSet = true;
	  this.description = description;
	  descriptionHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public CPropKeyValue( CPropKeyValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.appdefType = otherValue.appdefType;
	  appdefTypeHasBeenSet = true;
	  this.appdefTypeId = otherValue.appdefTypeId;
	  appdefTypeIdHasBeenSet = true;
	  this.key = otherValue.key;
	  keyHasBeenSet = true;
	  this.description = otherValue.description;
	  descriptionHasBeenSet = true;

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
   public int getAppdefTypeId()
   {
	  return this.appdefTypeId;
   }

   public void setAppdefTypeId( int appdefTypeId )
   {
	  this.appdefTypeId = appdefTypeId;
	  appdefTypeIdHasBeenSet = true;

   }

   public boolean appdefTypeIdHasBeenSet(){
	  return appdefTypeIdHasBeenSet;
   }
   public String getKey()
   {
	  return this.key;
   }

   public void setKey( String key )
   {
	  this.key = key;
	  keyHasBeenSet = true;

   }

   public boolean keyHasBeenSet(){
	  return keyHasBeenSet;
   }
   public String getDescription()
   {
	  return this.description;
   }

   public void setDescription( String description )
   {
	  this.description = description;
	  descriptionHasBeenSet = true;

   }

   public boolean descriptionHasBeenSet(){
	  return descriptionHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "appdefType=" + getAppdefType() + " " + "appdefTypeId=" + getAppdefTypeId() + " " + "key=" + getKey() + " " + "description=" + getDescription());
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
	  if (other instanceof CPropKeyValue)
	  {
		 CPropKeyValue that = (CPropKeyValue) other;
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
	  if (other instanceof CPropKeyValue)
	  {
		 CPropKeyValue that = (CPropKeyValue) other;
		 boolean lEquals = true;
		 lEquals = lEquals && this.appdefType == that.appdefType;
		 lEquals = lEquals && this.appdefTypeId == that.appdefTypeId;
		 if( this.key == null )
		 {
			lEquals = lEquals && ( that.key == null );
		 }
		 else
		 {
			lEquals = lEquals && this.key.equals( that.key );
		 }
		 if( this.description == null )
		 {
			lEquals = lEquals && ( that.description == null );
		 }
		 else
		 {
			lEquals = lEquals && this.description.equals( that.description );
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

      result = 37*result + (int) appdefType;

      result = 37*result + (int) appdefTypeId;

      result = 37*result + ((this.key != null) ? this.key.hashCode() : 0);

      result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

	  return result;
   }

}
