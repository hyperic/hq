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

package org.hyperic.hq.authz.shared;

/**
 * Value object for ResourceGroup.
 *
 */
public class ResourceGroupValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private java.lang.String sortName;
   private boolean sortNameHasBeenSet = false;
   private boolean system;
   private boolean systemHasBeenSet = false;
   private int groupType;
   private boolean groupTypeHasBeenSet = false;
   private int groupEntType;
   private boolean groupEntTypeHasBeenSet = false;
   private int groupEntResType;
   private boolean groupEntResTypeHasBeenSet = false;
   private int clusterId;
   private boolean clusterIdHasBeenSet = false;
   private String description;
   private boolean descriptionHasBeenSet = false;
   private String location;
   private boolean locationHasBeenSet = false;
   private String modifiedBy;
   private boolean modifiedByHasBeenSet = false;
   private Long mTime;
   private boolean mTimeHasBeenSet = false;
   private Long cTime;
   private boolean cTimeHasBeenSet = false;
   private String name;
   private boolean nameHasBeenSet = false;
   private Integer id;
   private boolean idHasBeenSet = false;

   public ResourceGroupValue()
   {
   }

   public ResourceGroupValue( java.lang.String sortName,boolean system,int groupType,int groupEntType,int groupEntResType,int clusterId,String description,String location,String modifiedBy,Long mTime,Long cTime,String name,Integer id )
   {
	  this.sortName = sortName;
	  sortNameHasBeenSet = true;
	  this.system = system;
	  systemHasBeenSet = true;
	  this.groupType = groupType;
	  groupTypeHasBeenSet = true;
	  this.groupEntType = groupEntType;
	  groupEntTypeHasBeenSet = true;
	  this.groupEntResType = groupEntResType;
	  groupEntResTypeHasBeenSet = true;
	  this.clusterId = clusterId;
	  clusterIdHasBeenSet = true;
	  this.description = description;
	  descriptionHasBeenSet = true;
	  this.location = location;
	  locationHasBeenSet = true;
	  this.modifiedBy = modifiedBy;
	  modifiedByHasBeenSet = true;
	  this.mTime = mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = cTime;
	  cTimeHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public ResourceGroupValue( ResourceGroupValue otherValue )
   {
	  this.sortName = otherValue.sortName;
	  sortNameHasBeenSet = true;
	  this.system = otherValue.system;
	  systemHasBeenSet = true;
	  this.groupType = otherValue.groupType;
	  groupTypeHasBeenSet = true;
	  this.groupEntType = otherValue.groupEntType;
	  groupEntTypeHasBeenSet = true;
	  this.groupEntResType = otherValue.groupEntResType;
	  groupEntResTypeHasBeenSet = true;
	  this.clusterId = otherValue.clusterId;
	  clusterIdHasBeenSet = true;
	  this.description = otherValue.description;
	  descriptionHasBeenSet = true;
	  this.location = otherValue.location;
	  locationHasBeenSet = true;
	  this.modifiedBy = otherValue.modifiedBy;
	  modifiedByHasBeenSet = true;
	  this.mTime = otherValue.mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = otherValue.cTime;
	  cTimeHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;
   }

   public java.lang.String getSortName()
   {
	  return this.sortName;
   }

   public void setSortName( java.lang.String sortName )
   {
	  this.sortName = sortName;
	  sortNameHasBeenSet = true;

   }

   public boolean sortNameHasBeenSet(){
	  return sortNameHasBeenSet;
   }
   public boolean getSystem()
   {
	  return this.system;
   }

   public void setSystem( boolean system )
   {
	  this.system = system;
	  systemHasBeenSet = true;

   }

   public boolean systemHasBeenSet(){
	  return systemHasBeenSet;
   }
   public int getGroupType()
   {
	  return this.groupType;
   }

   public void setGroupType( int groupType )
   {
	  this.groupType = groupType;
	  groupTypeHasBeenSet = true;

   }

   public boolean groupTypeHasBeenSet(){
	  return groupTypeHasBeenSet;
   }
   public int getGroupEntType()
   {
	  return this.groupEntType;
   }

   public void setGroupEntType( int groupEntType )
   {
	  this.groupEntType = groupEntType;
	  groupEntTypeHasBeenSet = true;

   }

   public boolean groupEntTypeHasBeenSet(){
	  return groupEntTypeHasBeenSet;
   }
   public int getGroupEntResType()
   {
	  return this.groupEntResType;
   }

   public void setGroupEntResType( int groupEntResType )
   {
	  this.groupEntResType = groupEntResType;
	  groupEntResTypeHasBeenSet = true;

   }

   public boolean groupEntResTypeHasBeenSet(){
	  return groupEntResTypeHasBeenSet;
   }
   public int getClusterId()
   {
	  return this.clusterId;
   }

   public void setClusterId( int clusterId )
   {
	  this.clusterId = clusterId;
	  clusterIdHasBeenSet = true;

   }

   public boolean clusterIdHasBeenSet(){
	  return clusterIdHasBeenSet;
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
   public String getLocation()
   {
	  return this.location;
   }

   public void setLocation( String location )
   {
	  this.location = location;
	  locationHasBeenSet = true;

   }

   public boolean locationHasBeenSet(){
	  return locationHasBeenSet;
   }
   public String getModifiedBy()
   {
	  return this.modifiedBy;
   }

   public void setModifiedBy( String modifiedBy )
   {
	  this.modifiedBy = modifiedBy;
	  modifiedByHasBeenSet = true;

   }

   public boolean modifiedByHasBeenSet(){
	  return modifiedByHasBeenSet;
   }
   public Long getMTime()
   {
	  return this.mTime;
   }

   public void setMTime( Long mTime )
   {
	  this.mTime = mTime;
	  mTimeHasBeenSet = true;

   }

   public boolean mTimeHasBeenSet(){
	  return mTimeHasBeenSet;
   }
   public Long getCTime()
   {
	  return this.cTime;
   }

   public void setCTime( Long cTime )
   {
	  this.cTime = cTime;
	  cTimeHasBeenSet = true;

   }

   public boolean cTimeHasBeenSet(){
	  return cTimeHasBeenSet;
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

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("sortName=" + getSortName() + " " + "system=" + getSystem() + " " + "groupType=" + getGroupType() + " " + "groupEntType=" + getGroupEntType() + " " + "groupEntResType=" + getGroupEntResType() + " " + "clusterId=" + getClusterId() + " " + "description=" + getDescription() + " " + "location=" + getLocation() + " " + "modifiedBy=" + getModifiedBy() + " " + "mTime=" + getMTime() + " " + "cTime=" + getCTime() + " " + "name=" + getName() + " " + "id=" + getId());
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
	  if (other instanceof ResourceGroupValue)
	  {
		 ResourceGroupValue that = (ResourceGroupValue) other;
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
	  if (other instanceof ResourceGroupValue)
	  {
		 ResourceGroupValue that = (ResourceGroupValue) other;
		 boolean lEquals = true;
		 if( this.sortName == null )
		 {
			lEquals = lEquals && ( that.sortName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.sortName.equals( that.sortName );
		 }
		 lEquals = lEquals && this.system == that.system;
		 lEquals = lEquals && this.groupType == that.groupType;
		 lEquals = lEquals && this.groupEntType == that.groupEntType;
		 lEquals = lEquals && this.groupEntResType == that.groupEntResType;
		 lEquals = lEquals && this.clusterId == that.clusterId;
		 if( this.description == null )
		 {
			lEquals = lEquals && ( that.description == null );
		 }
		 else
		 {
			lEquals = lEquals && this.description.equals( that.description );
		 }
		 if( this.location == null )
		 {
			lEquals = lEquals && ( that.location == null );
		 }
		 else
		 {
			lEquals = lEquals && this.location.equals( that.location );
		 }
		 if( this.modifiedBy == null )
		 {
			lEquals = lEquals && ( that.modifiedBy == null );
		 }
		 else
		 {
			lEquals = lEquals && this.modifiedBy.equals( that.modifiedBy );
		 }
		 if( this.mTime == null )
		 {
			lEquals = lEquals && ( that.mTime == null );
		 }
		 else
		 {
			lEquals = lEquals && this.mTime.equals( that.mTime );
		 }
		 if( this.cTime == null )
		 {
			lEquals = lEquals && ( that.cTime == null );
		 }
		 else
		 {
			lEquals = lEquals && this.cTime.equals( that.cTime );
		 }
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
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
      result = 37*result + ((this.sortName != null) ? this.sortName.hashCode() : 0);

      result = 37*result + (system ? 0 : 1);

      result = 37*result + (int) groupType;

      result = 37*result + (int) groupEntType;

      result = 37*result + (int) groupEntResType;

      result = 37*result + (int) clusterId;

      result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

      result = 37*result + ((this.location != null) ? this.location.hashCode() : 0);

      result = 37*result + ((this.modifiedBy != null) ? this.modifiedBy.hashCode() : 0);

      result = 37*result + ((this.mTime != null) ? this.mTime.hashCode() : 0);

      result = 37*result + ((this.cTime != null) ? this.cTime.hashCode() : 0);

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

	  return result;
   }

}
