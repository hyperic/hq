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

package org.hyperic.hq.appdef.shared;

import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import java.io.Serializable;

/**
 * Value object for GroupType
 *
 */
public class GroupTypeValue extends AppdefResourceTypeValue
       implements Serializable
{
   private String name;
   private String description;
   private Integer id;
   private Long mTime;
   private Long cTime;

   public GroupTypeValue() { }

   public GroupTypeValue(String name) {
       this.name = name;
   }

   public String getName() {
	  return this.name;
   }

   public void setName( String name ) {
	  this.name = name;
   }

   public String getDescription() {
	  return this.description;
   }

   public void setDescription( String description ) {
	  this.description = description;
   }


   public Integer getId() {
	  return this.id;
   }

   public void setId( Integer id ) {
	  this.id = id;
   }

   public Long getMTime() {
	  return this.mTime;
   }

   public void setMTime( Long mTime ) {
	  this.mTime = mTime;
   }

   public Long getCTime() {
	  return this.cTime;
   }

   public void setCTime( Long cTime ) {
	  this.cTime = cTime;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("name=" + getName() + " " + "description=" + getDescription() + " " + "id=" + getId() + " " + "mTime=" + getMTime() + " " + "cTime=" + getCTime());
	  str.append('}');

	  return(str.toString());
   }

    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_GROUP;
    }

}
