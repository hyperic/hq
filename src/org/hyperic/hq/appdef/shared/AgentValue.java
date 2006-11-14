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
 * Value object for Agent.
 *
 */
public class AgentValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private String address;
   private boolean addressHasBeenSet = false;
   private int port;
   private boolean portHasBeenSet = false;
   private String authToken;
   private boolean authTokenHasBeenSet = false;
   private String agentToken;
   private boolean agentTokenHasBeenSet = false;
   private String version;
   private boolean versionHasBeenSet = false;
   private java.lang.Integer id;
   private boolean idHasBeenSet = false;
   private java.lang.Long mTime;
   private boolean mTimeHasBeenSet = false;
   private java.lang.Long cTime;
   private boolean cTimeHasBeenSet = false;
   private org.hyperic.hq.appdef.shared.AgentTypeValue AgentType;
   private boolean AgentTypeHasBeenSet = false;


   public AgentValue()
   {
   }

   public AgentValue( String address,int port,String authToken,String agentToken,String version,java.lang.Integer id,java.lang.Long mTime,java.lang.Long cTime )
   {
	  this.address = address;
	  addressHasBeenSet = true;
	  this.port = port;
	  portHasBeenSet = true;
	  this.authToken = authToken;
	  authTokenHasBeenSet = true;
	  this.agentToken = agentToken;
	  agentTokenHasBeenSet = true;
	  this.version = version;
	  versionHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  this.mTime = mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = cTime;
	  cTimeHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public AgentValue( AgentValue otherValue )
   {
	  this.address = otherValue.address;
	  addressHasBeenSet = true;
	  this.port = otherValue.port;
	  portHasBeenSet = true;
	  this.authToken = otherValue.authToken;
	  authTokenHasBeenSet = true;
	  this.agentToken = otherValue.agentToken;
	  agentTokenHasBeenSet = true;
	  this.version = otherValue.version;
	  versionHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.mTime = otherValue.mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = otherValue.cTime;
	  cTimeHasBeenSet = true;
	// TODO Clone is better no ?
	  this.AgentType = otherValue.AgentType;
	  AgentTypeHasBeenSet = true;

   }

   public String getAddress()
   {
	  return this.address;
   }

   public void setAddress( String address )
   {
	  this.address = address;
	  addressHasBeenSet = true;

   }

   public boolean addressHasBeenSet(){
	  return addressHasBeenSet;
   }
   public int getPort()
   {
	  return this.port;
   }

   public void setPort( int port )
   {
	  this.port = port;
	  portHasBeenSet = true;

   }

   public boolean portHasBeenSet(){
	  return portHasBeenSet;
   }
   public String getAuthToken()
   {
	  return this.authToken;
   }

   public void setAuthToken( String authToken )
   {
	  this.authToken = authToken;
	  authTokenHasBeenSet = true;

   }

   public boolean authTokenHasBeenSet(){
	  return authTokenHasBeenSet;
   }
   public String getAgentToken()
   {
	  return this.agentToken;
   }

   public void setAgentToken( String agentToken )
   {
	  this.agentToken = agentToken;
	  agentTokenHasBeenSet = true;

   }

   public boolean agentTokenHasBeenSet(){
	  return agentTokenHasBeenSet;
   }
   public String getVersion()
   {
	  return this.version;
   }

   public void setVersion( String version )
   {
	  this.version = version;
	  versionHasBeenSet = true;

   }

   public boolean versionHasBeenSet(){
	  return versionHasBeenSet;
   }
   public java.lang.Integer getId()
   {
	  return this.id;
   }

   public void setId( java.lang.Integer id )
   {
	  this.id = id;
	  idHasBeenSet = true;

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

   public org.hyperic.hq.appdef.shared.AgentTypeValue getAgentType()
   {
	  return this.AgentType;
   }
   public void setAgentType( org.hyperic.hq.appdef.shared.AgentTypeValue AgentType )
   {
	  this.AgentType = AgentType;
	  AgentTypeHasBeenSet = true;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("address=" + getAddress() + " " + "port=" + getPort() + " " + "authToken=" + getAuthToken() + " " + "agentToken=" + getAgentToken() + " " + "version=" + getVersion() + " " + "id=" + getId() + " " + "mTime=" + getMTime() + " " + "cTime=" + getCTime());
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
	  if (other instanceof AgentValue)
	  {
		 AgentValue that = (AgentValue) other;
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
	  if (other instanceof AgentValue)
	  {
		 AgentValue that = (AgentValue) other;
		 boolean lEquals = true;
		 if( this.address == null )
		 {
			lEquals = lEquals && ( that.address == null );
		 }
		 else
		 {
			lEquals = lEquals && this.address.equals( that.address );
		 }
		 lEquals = lEquals && this.port == that.port;
		 if( this.authToken == null )
		 {
			lEquals = lEquals && ( that.authToken == null );
		 }
		 else
		 {
			lEquals = lEquals && this.authToken.equals( that.authToken );
		 }
		 if( this.agentToken == null )
		 {
			lEquals = lEquals && ( that.agentToken == null );
		 }
		 else
		 {
			lEquals = lEquals && this.agentToken.equals( that.agentToken );
		 }
		 if( this.version == null )
		 {
			lEquals = lEquals && ( that.version == null );
		 }
		 else
		 {
			lEquals = lEquals && this.version.equals( that.version );
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
		 if( this.AgentType == null )
		 {
			lEquals = lEquals && ( that.AgentType == null );
		 }
		 else
		 {
			lEquals = lEquals && this.AgentType.equals( that.AgentType );
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

      result = 37*result + (int) port;

      result = 37*result + ((this.authToken != null) ? this.authToken.hashCode() : 0);

      result = 37*result + ((this.agentToken != null) ? this.agentToken.hashCode() : 0);

      result = 37*result + ((this.version != null) ? this.version.hashCode() : 0);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

      result = 37*result + ((this.mTime != null) ? this.mTime.hashCode() : 0);

      result = 37*result + ((this.cTime != null) ? this.cTime.hashCode() : 0);

	  result = 37*result + ((this.AgentType != null) ? this.AgentType.hashCode() : 0);
	  return result;
   }

}
