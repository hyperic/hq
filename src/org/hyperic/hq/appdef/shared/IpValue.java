/*
 * xdoclet generated code.
 * legacy DTO pattern (targeted to be replaced with hibernate pojo).
 */
package org.hyperic.hq.appdef.shared;

/**
 * Value object for Ip.
 *
 */
public class IpValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private java.lang.String address;
   private boolean addressHasBeenSet = false;
   private java.lang.String netmask;
   private boolean netmaskHasBeenSet = false;
   private java.lang.String mACAddress;
   private boolean mACAddressHasBeenSet = false;
   private java.lang.Integer id;
   private boolean idHasBeenSet = false;
   private java.lang.Long mTime;
   private boolean mTimeHasBeenSet = false;
   private java.lang.Long cTime;
   private boolean cTimeHasBeenSet = false;

   private org.hyperic.hq.appdef.shared.IpPK pk;

   public IpValue()
   {
	  pk = new org.hyperic.hq.appdef.shared.IpPK();
   }

   public IpValue( java.lang.String address,java.lang.String netmask,java.lang.String mACAddress,java.lang.Integer id,java.lang.Long mTime,java.lang.Long cTime )
   {
	  this.address = address;
	  addressHasBeenSet = true;
	  this.netmask = netmask;
	  netmaskHasBeenSet = true;
	  this.mACAddress = mACAddress;
	  mACAddressHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  this.mTime = mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = cTime;
	  cTimeHasBeenSet = true;
	  pk = new org.hyperic.hq.appdef.shared.IpPK(this.getId());
   }

   //TODO Cloneable is better than this !
   public IpValue( IpValue otherValue )
   {
	  this.address = otherValue.address;
	  addressHasBeenSet = true;
	  this.netmask = otherValue.netmask;
	  netmaskHasBeenSet = true;
	  this.mACAddress = otherValue.mACAddress;
	  mACAddressHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.mTime = otherValue.mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = otherValue.cTime;
	  cTimeHasBeenSet = true;

	  pk = new org.hyperic.hq.appdef.shared.IpPK(this.getId());
   }

   public org.hyperic.hq.appdef.shared.IpPK getPrimaryKey()
   {
	  return pk;
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
   public java.lang.String getNetmask()
   {
	  return this.netmask;
   }

   public void setNetmask( java.lang.String netmask )
   {
	  this.netmask = netmask;
	  netmaskHasBeenSet = true;

   }

   public boolean netmaskHasBeenSet(){
	  return netmaskHasBeenSet;
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
   public java.lang.Integer getId()
   {
	  return this.id;
   }

   public void setId( java.lang.Integer id )
   {
	  this.id = id;
	  idHasBeenSet = true;

		 pk.setId(id);
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
	  mTimeHasBeenSet = true;

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
	  cTimeHasBeenSet = true;

   }

   public boolean cTimeHasBeenSet(){
	  return cTimeHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("address=" + getAddress() + " " + "netmask=" + getNetmask() + " " + "mACAddress=" + getMACAddress() + " " + "id=" + getId() + " " + "mTime=" + getMTime() + " " + "cTime=" + getCTime());
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
	  if (other instanceof IpValue)
	  {
		 IpValue that = (IpValue) other;
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
	  if (other instanceof IpValue)
	  {
		 IpValue that = (IpValue) other;
		 boolean lEquals = true;
		 if( this.address == null )
		 {
			lEquals = lEquals && ( that.address == null );
		 }
		 else
		 {
			lEquals = lEquals && this.address.equals( that.address );
		 }
		 if( this.netmask == null )
		 {
			lEquals = lEquals && ( that.netmask == null );
		 }
		 else
		 {
			lEquals = lEquals && this.netmask.equals( that.netmask );
		 }
		 if( this.mACAddress == null )
		 {
			lEquals = lEquals && ( that.mACAddress == null );
		 }
		 else
		 {
			lEquals = lEquals && this.mACAddress.equals( that.mACAddress );
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

      result = 37*result + ((this.netmask != null) ? this.netmask.hashCode() : 0);

      result = 37*result + ((this.mACAddress != null) ? this.mACAddress.hashCode() : 0);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

      result = 37*result + ((this.mTime != null) ? this.mTime.hashCode() : 0);

      result = 37*result + ((this.cTime != null) ? this.cTime.hashCode() : 0);

	  return result;
   }

}
