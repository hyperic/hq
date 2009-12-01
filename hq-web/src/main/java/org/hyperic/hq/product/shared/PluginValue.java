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
package org.hyperic.hq.product.shared;

/**
 * Value object for Plugin.
 *
 */
public class PluginValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String name;
   private boolean nameHasBeenSet = false;
   private String path;
   private boolean pathHasBeenSet = false;
   private String mD5;
   private boolean mD5HasBeenSet = false;
   private long ctime;
   private boolean ctimeHasBeenSet = false;

   public PluginValue()
   {
   }

   public PluginValue( Integer id,String name,String path,String mD5,long ctime )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.path = path;
	  pathHasBeenSet = true;
	  this.mD5 = mD5;
	  mD5HasBeenSet = true;
	  this.ctime = ctime;
	  ctimeHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public PluginValue( PluginValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.path = otherValue.path;
	  pathHasBeenSet = true;
	  this.mD5 = otherValue.mD5;
	  mD5HasBeenSet = true;
	  this.ctime = otherValue.ctime;
	  ctimeHasBeenSet = true;
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
   public String getPath()
   {
	  return this.path;
   }

   public void setPath( String path )
   {
	  this.path = path;
	  pathHasBeenSet = true;

   }

   public boolean pathHasBeenSet(){
	  return pathHasBeenSet;
   }
   public String getMD5()
   {
	  return this.mD5;
   }

   public void setMD5( String mD5 )
   {
	  this.mD5 = mD5;
	  mD5HasBeenSet = true;

   }

   public boolean mD5HasBeenSet(){
	  return mD5HasBeenSet;
   }
   public long getCtime()
   {
	  return this.ctime;
   }

   public void setCtime( long ctime )
   {
	  this.ctime = ctime;
	  ctimeHasBeenSet = true;

   }

   public boolean ctimeHasBeenSet(){
	  return ctimeHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "name=" + getName() + " " + "path=" + getPath() + " " + "mD5=" + getMD5() + " " + "ctime=" + getCtime());
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
	  if (other instanceof PluginValue)
	  {
		 PluginValue that = (PluginValue) other;
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
	  if (other instanceof PluginValue)
	  {
		 PluginValue that = (PluginValue) other;
		 boolean lEquals = true;
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }
		 if( this.path == null )
		 {
			lEquals = lEquals && ( that.path == null );
		 }
		 else
		 {
			lEquals = lEquals && this.path.equals( that.path );
		 }
		 if( this.mD5 == null )
		 {
			lEquals = lEquals && ( that.mD5 == null );
		 }
		 else
		 {
			lEquals = lEquals && this.mD5.equals( that.mD5 );
		 }
		 lEquals = lEquals && this.ctime == that.ctime;

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

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.path != null) ? this.path.hashCode() : 0);

      result = 37*result + ((this.mD5 != null) ? this.mD5.hashCode() : 0);

      result = 37*result + (int)(ctime^(ctime>>>32));

	  return result;
   }

}
