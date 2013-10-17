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
 * Value object for AIIp.
 *
 */
public class AIIpValue
   extends org.hyperic.hq.appdef.shared.AIAppdefResourceValue
   implements java.io.Serializable
{
   private int queueStatus;
   private boolean queueStatusHasBeenSet = false;
   private long diff;
   private boolean diffHasBeenSet = false;
   private boolean ignored;
   private boolean ignoredHasBeenSet = false;
   private java.lang.String address;
   private boolean addressHasBeenSet = false;
   private java.lang.String mACAddress;
   private boolean mACAddressHasBeenSet = false;
   private java.lang.String netmask;
   private boolean netmaskHasBeenSet = false;
   private java.lang.Integer id;
   private boolean idHasBeenSet = false;
   private java.lang.Long mTime;
   private boolean mTimeHasBeenSet = false;
   private java.lang.Long cTime;
   private boolean cTimeHasBeenSet = false;

   public AIIpValue()
   {
   }

   public AIIpValue( int queueStatus,long diff,boolean ignored,java.lang.String address,java.lang.String mACAddress,java.lang.String netmask,java.lang.Integer id,java.lang.Long mTime,java.lang.Long cTime )
   {
	  setQueueStatus(queueStatus);
	  setDiff(diff);
	  setIgnored(ignored);
	  setAddress(address);
	  setMACAddress(mACAddress);
	  setNetmask(netmask);
	  setId(id);
	  setMTime(mTime);
	  setCTime(cTime);
   }

   //TODO Cloneable is better than this !
   public AIIpValue( AIIpValue otherValue )
   {
	  setQueueStatus(otherValue.queueStatus);
	  setDiff(otherValue.diff);
	  setIgnored(otherValue.ignored);
	  setAddress(otherValue.address);
	  setMACAddress(otherValue.mACAddress);
	  setNetmask(otherValue.netmask);
	  setId(otherValue.id);
	  setMTime(otherValue.mTime);
	  setCTime(otherValue.cTime);
   }

   public int getQueueStatus()
   {
	  return this.queueStatus;
   }

   public void setQueueStatus( int queueStatus )
   {
	  this.queueStatus = queueStatus;
	  queueStatusHasBeenSet = true;
   }

   public boolean queueStatusHasBeenSet(){
	  return queueStatusHasBeenSet;
   }

   public long getDiff()
   {
	  return this.diff;
   }

   public void setDiff( long diff )
   {
	  this.diff = diff;
	  diffHasBeenSet = true;
   }

   public boolean diffHasBeenSet(){
	  return diffHasBeenSet;
   }

   public boolean getIgnored()
   {
	  return this.ignored;
   }

   public void setIgnored( boolean ignored )
   {
	  this.ignored = ignored;
	  ignoredHasBeenSet = true;
   }

   public boolean ignoredHasBeenSet(){
	  return ignoredHasBeenSet;
   }

   public java.lang.String getAddress()
   {
	  return this.address;
   }

   public void setAddress( java.lang.String address )
   {
	  this.address = address;
	  addressHasBeenSet = true;
   }

   public boolean addressHasBeenSet(){
	  return addressHasBeenSet;
   }

   public java.lang.String getMACAddress()
   {
	  return this.mACAddress;
   }

   public void setMACAddress( java.lang.String mACAddress )
   {
	  this.mACAddress = mACAddress;
	  mACAddressHasBeenSet = true;
   }

   public boolean mACAddressHasBeenSet(){
	  return mACAddressHasBeenSet;
   }

   public java.lang.String getNetmask()
   {
	  return this.netmask;
   }

   public void setNetmask( java.lang.String netmask )
   {
	  this.netmask = netmask;
	  if (netmask!=null){
	      netmaskHasBeenSet = true;
	  } else {
	      netmaskHasBeenSet = false;
	  }
   }

   public boolean netmaskHasBeenSet(){
	  return netmaskHasBeenSet;
   }

   public java.lang.Integer getId()
   {
	  return this.id;
   }

   public void setId( java.lang.Integer id )
   {
	  this.id = id;
	  if(id!=null){
	      idHasBeenSet = true;
	  } else {
	      idHasBeenSet = false;
	  }
   }

   public boolean idHasBeenSet(){
	  return idHasBeenSet;
   }

   public java.lang.Long getMTime()
   {
	  return this.mTime;
   }

   public void setMTime( java.lang.Long mTime )
   {
	  this.mTime = mTime;
	  if (cTime!=null){
	      mTimeHasBeenSet = true;
	  } else {
	      mTimeHasBeenSet = false;
	  }
   }

   public boolean mTimeHasBeenSet(){
	  return mTimeHasBeenSet;
   }

   public java.lang.Long getCTime()
   {
	  return this.cTime;
   }

   public void setCTime( java.lang.Long cTime )
   {
	  this.cTime = cTime;
	  if (cTime!=null){
	      cTimeHasBeenSet = true;
	  } else {
	      cTimeHasBeenSet = false;
	  }
   }

   public boolean cTimeHasBeenSet(){
	  return cTimeHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("queueStatus=" + getQueueStatus() + " " + "diff=" + getDiff() + " " + "ignored=" + getIgnored() + " " + "address=" + getAddress() + " " + "mACAddress=" + getMACAddress() + " " + "netmask=" + getNetmask() + " " + "id=" + getId() + " " + "mTime=" + getMTime() + " " + "cTime=" + getCTime());
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
	  if (other instanceof AIIpValue)
	  {
		 AIIpValue that = (AIIpValue) other;
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
	  if (other instanceof AIIpValue)
	  {
		 AIIpValue that = (AIIpValue) other;
		 boolean lEquals = true;
		 lEquals = lEquals && this.queueStatus == that.queueStatus;
		 lEquals = lEquals && this.diff == that.diff;
		 lEquals = lEquals && this.ignored == that.ignored;
		 if( this.address == null )
		 {
			lEquals = lEquals && ( that.address == null );
		 }
		 else
		 {
			lEquals = lEquals && this.address.equals( that.address );
		 }
		 if( this.mACAddress == null )
		 {
			lEquals = lEquals && ( that.mACAddress == null );
		 }
		 else
		 {
			lEquals = lEquals && this.mACAddress.equals( that.mACAddress );
		 }
		 if( this.netmask == null )
		 {
			lEquals = lEquals && ( that.netmask == null );
		 }
		 else
		 {
			lEquals = lEquals && this.netmask.equals( that.netmask );
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

		 return lEquals;
	  }
	  else
	  {
		 return false;
	  }
   }

   public int hashCode(){
	  int result = 17;
      result = 37*result + ((this.address != null) ? this.address.hashCode() : 0);
      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);
	  return result;
   }

}
