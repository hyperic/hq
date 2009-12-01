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
 * Value object for AIServer.
 *
 */
public class AIServerValue
   extends org.hyperic.hq.appdef.shared.AIAppdefResourceValue
   implements java.io.Serializable
{
   private int queueStatus;
   private boolean queueStatusHasBeenSet = false;
   private byte[] customProperties;
   private boolean customPropertiesHasBeenSet = false;
   private byte[] productConfig;
   private boolean productConfigHasBeenSet = false;
   private byte[] controlConfig;
   private boolean controlConfigHasBeenSet = false;
   private byte[] measurementConfig;
   private boolean measurementConfigHasBeenSet = false;
   private byte[] responseTimeConfig;
   private boolean responseTimeConfigHasBeenSet = false;
   private long diff;
   private boolean diffHasBeenSet = false;
   private boolean ignored;
   private boolean ignoredHasBeenSet = false;
   private String serverTypeName;
   private boolean serverTypeNameHasBeenSet = false;
   private java.lang.String name;
   private boolean nameHasBeenSet = false;
   private java.lang.String autoinventoryIdentifier;
   private boolean autoinventoryIdentifierHasBeenSet = false;
   private java.lang.String installPath;
   private boolean installPathHasBeenSet = false;
   private java.lang.String description;
   private boolean descriptionHasBeenSet = false;
   private boolean servicesAutomanaged;
   private boolean servicesAutomanagedHasBeenSet = false;
   private java.lang.Integer id;
   private boolean idHasBeenSet = false;
   private java.lang.Long mTime;
   private boolean mTimeHasBeenSet = false;
   private java.lang.Long cTime;
   private boolean cTimeHasBeenSet = false;

   public AIServerValue()
   {
   }

   public AIServerValue( int queueStatus,byte[] customProperties,byte[] productConfig,byte[] controlConfig,byte[] measurementConfig,byte[] responseTimeConfig,long diff,boolean ignored,String serverTypeName,java.lang.String name,java.lang.String autoinventoryIdentifier,java.lang.String installPath,java.lang.String description,boolean servicesAutomanaged,java.lang.Integer id,java.lang.Long mTime,java.lang.Long cTime )
   {
	  this.queueStatus = queueStatus;
	  queueStatusHasBeenSet = true;
	  this.customProperties = customProperties;
	  customPropertiesHasBeenSet = true;
	  this.productConfig = productConfig;
	  productConfigHasBeenSet = true;
	  this.controlConfig = controlConfig;
	  controlConfigHasBeenSet = true;
	  this.measurementConfig = measurementConfig;
	  measurementConfigHasBeenSet = true;
	  this.responseTimeConfig = responseTimeConfig;
	  responseTimeConfigHasBeenSet = true;
	  this.diff = diff;
	  diffHasBeenSet = true;
	  this.ignored = ignored;
	  ignoredHasBeenSet = true;
	  this.serverTypeName = serverTypeName;
	  serverTypeNameHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.autoinventoryIdentifier = autoinventoryIdentifier;
	  autoinventoryIdentifierHasBeenSet = true;
	  this.installPath = installPath;
	  installPathHasBeenSet = true;
	  this.description = description;
	  descriptionHasBeenSet = true;
	  this.servicesAutomanaged = servicesAutomanaged;
	  servicesAutomanagedHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  this.mTime = mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = cTime;
	  cTimeHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public AIServerValue( AIServerValue otherValue )
   {
	  this.queueStatus = otherValue.queueStatus;
	  queueStatusHasBeenSet = true;
	  this.customProperties = otherValue.customProperties;
	  customPropertiesHasBeenSet = true;
	  this.productConfig = otherValue.productConfig;
	  productConfigHasBeenSet = true;
	  this.controlConfig = otherValue.controlConfig;
	  controlConfigHasBeenSet = true;
	  this.measurementConfig = otherValue.measurementConfig;
	  measurementConfigHasBeenSet = true;
	  this.responseTimeConfig = otherValue.responseTimeConfig;
	  responseTimeConfigHasBeenSet = true;
	  this.diff = otherValue.diff;
	  diffHasBeenSet = true;
	  this.ignored = otherValue.ignored;
	  ignoredHasBeenSet = true;
	  this.serverTypeName = otherValue.serverTypeName;
	  serverTypeNameHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.autoinventoryIdentifier = otherValue.autoinventoryIdentifier;
	  autoinventoryIdentifierHasBeenSet = true;
	  this.installPath = otherValue.installPath;
	  installPathHasBeenSet = true;
	  this.description = otherValue.description;
	  descriptionHasBeenSet = true;
	  this.servicesAutomanaged = otherValue.servicesAutomanaged;
	  servicesAutomanagedHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.mTime = otherValue.mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = otherValue.cTime;
	  cTimeHasBeenSet = true;

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
   public byte[] getCustomProperties()
   {
	  return this.customProperties;
   }

   public void setCustomProperties( byte[] customProperties )
   {
	  this.customProperties = customProperties;
	  customPropertiesHasBeenSet = true;

   }

   public boolean customPropertiesHasBeenSet(){
	  return customPropertiesHasBeenSet;
   }
   public byte[] getProductConfig()
   {
	  return this.productConfig;
   }

   public void setProductConfig( byte[] productConfig )
   {
	  this.productConfig = productConfig;
	  productConfigHasBeenSet = true;

   }

   public boolean productConfigHasBeenSet(){
	  return productConfigHasBeenSet;
   }
   public byte[] getControlConfig()
   {
	  return this.controlConfig;
   }

   public void setControlConfig( byte[] controlConfig )
   {
	  this.controlConfig = controlConfig;
	  controlConfigHasBeenSet = true;

   }

   public boolean controlConfigHasBeenSet(){
	  return controlConfigHasBeenSet;
   }
   public byte[] getMeasurementConfig()
   {
	  return this.measurementConfig;
   }

   public void setMeasurementConfig( byte[] measurementConfig )
   {
	  this.measurementConfig = measurementConfig;
	  measurementConfigHasBeenSet = true;

   }

   //see ScanState; can't setMeasurementConfig(null)
   //or Lather throws IllegalArgumentException
   public void unsetMeasurementConfig(){
       this.measurementConfig = null;
       measurementConfigHasBeenSet = false;
   }

   public boolean measurementConfigHasBeenSet(){
	  return measurementConfigHasBeenSet;
   }
   public byte[] getResponseTimeConfig()
   {
	  return this.responseTimeConfig;
   }

   public void setResponseTimeConfig( byte[] responseTimeConfig )
   {
	  this.responseTimeConfig = responseTimeConfig;
	  responseTimeConfigHasBeenSet = true;

   }

   public boolean responseTimeConfigHasBeenSet(){
	  return responseTimeConfigHasBeenSet;
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
   public String getServerTypeName()
   {
	  return this.serverTypeName;
   }

   public void setServerTypeName( String serverTypeName )
   {
	  this.serverTypeName = serverTypeName;
	  serverTypeNameHasBeenSet = true;

   }

   public boolean serverTypeNameHasBeenSet(){
	  return serverTypeNameHasBeenSet;
   }
   public java.lang.String getName()
   {
	  return this.name;
   }

   public void setName( java.lang.String name )
   {
	  this.name = name;
	  nameHasBeenSet = true;

   }

   public boolean nameHasBeenSet(){
	  return nameHasBeenSet;
   }
   public java.lang.String getAutoinventoryIdentifier()
   {
	  return this.autoinventoryIdentifier;
   }

   public void setAutoinventoryIdentifier( java.lang.String autoinventoryIdentifier )
   {
	  this.autoinventoryIdentifier = autoinventoryIdentifier;
	  autoinventoryIdentifierHasBeenSet = true;

   }

   public boolean autoinventoryIdentifierHasBeenSet(){
	  return autoinventoryIdentifierHasBeenSet;
   }
   public java.lang.String getInstallPath()
   {
	  return this.installPath;
   }

   public void setInstallPath( java.lang.String installPath )
   {
	  this.installPath = installPath;
	  installPathHasBeenSet = true;

   }

   public boolean installPathHasBeenSet(){
	  return installPathHasBeenSet;
   }
   public java.lang.String getDescription()
   {
	  return this.description;
   }

   public void setDescription( java.lang.String description )
   {
	  this.description = description;
	  descriptionHasBeenSet = true;

   }

   public boolean descriptionHasBeenSet(){
	  return descriptionHasBeenSet;
   }
   public boolean getServicesAutomanaged()
   {
	  return this.servicesAutomanaged;
   }

   public void setServicesAutomanaged( boolean servicesAutomanaged )
   {
	  this.servicesAutomanaged = servicesAutomanaged;
	  servicesAutomanagedHasBeenSet = true;

   }

   public boolean servicesAutomanagedHasBeenSet(){
	  return servicesAutomanagedHasBeenSet;
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

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("queueStatus=" + getQueueStatus() + " " + "customProperties=" + getCustomProperties() + " " + "productConfig=" + getProductConfig() + " " + "controlConfig=" + getControlConfig() + " " + "measurementConfig=" + getMeasurementConfig() + " " + "responseTimeConfig=" + getResponseTimeConfig() + " " + "diff=" + getDiff() + " " + "ignored=" + getIgnored() + " " + "serverTypeName=" + getServerTypeName() + " " + "name=" + getName() + " " + "autoinventoryIdentifier=" + getAutoinventoryIdentifier() + " " + "installPath=" + getInstallPath() + " " + "description=" + getDescription() + " " + "servicesAutomanaged=" + getServicesAutomanaged() + " " + "id=" + getId() + " " + "mTime=" + getMTime() + " " + "cTime=" + getCTime());
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
	  if (other instanceof AIServerValue)
	  {
		 AIServerValue that = (AIServerValue) other;
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
	  if (other instanceof AIServerValue)
	  {
		 AIServerValue that = (AIServerValue) other;
		 boolean lEquals = true;
		 lEquals = lEquals && this.queueStatus == that.queueStatus;
		 lEquals = lEquals && this.customProperties == that.customProperties;
		 lEquals = lEquals && this.productConfig == that.productConfig;
		 lEquals = lEquals && this.controlConfig == that.controlConfig;
		 lEquals = lEquals && this.measurementConfig == that.measurementConfig;
		 lEquals = lEquals && this.responseTimeConfig == that.responseTimeConfig;
		 lEquals = lEquals && this.diff == that.diff;
		 lEquals = lEquals && this.ignored == that.ignored;
		 if( this.serverTypeName == null )
		 {
			lEquals = lEquals && ( that.serverTypeName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.serverTypeName.equals( that.serverTypeName );
		 }
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }
		 if( this.autoinventoryIdentifier == null )
		 {
			lEquals = lEquals && ( that.autoinventoryIdentifier == null );
		 }
		 else
		 {
			lEquals = lEquals && this.autoinventoryIdentifier.equals( that.autoinventoryIdentifier );
		 }
		 if( this.installPath == null )
		 {
			lEquals = lEquals && ( that.installPath == null );
		 }
		 else
		 {
			lEquals = lEquals && this.installPath.equals( that.installPath );
		 }
		 if( this.description == null )
		 {
			lEquals = lEquals && ( that.description == null );
		 }
		 else
		 {
			lEquals = lEquals && this.description.equals( that.description );
		 }
		 lEquals = lEquals && this.servicesAutomanaged == that.servicesAutomanaged;
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
      result = 37*result + (int) queueStatus;

      for (int i=0;  customProperties != null && i<customProperties.length; i++)
      {
         long l = customProperties[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      for (int i=0;  productConfig != null && i<productConfig.length; i++)
      {
         long l = productConfig[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      for (int i=0;  controlConfig != null && i<controlConfig.length; i++)
      {
         long l = controlConfig[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      for (int i=0;  measurementConfig != null && i<measurementConfig.length; i++)
      {
         long l = measurementConfig[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      for (int i=0;  responseTimeConfig != null && i<responseTimeConfig.length; i++)
      {
         long l = responseTimeConfig[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      result = 37*result + (int)(diff^(diff>>>32));

      result = 37*result + (ignored ? 0 : 1);

      result = 37*result + ((this.serverTypeName != null) ? this.serverTypeName.hashCode() : 0);

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.autoinventoryIdentifier != null) ? this.autoinventoryIdentifier.hashCode() : 0);

      result = 37*result + ((this.installPath != null) ? this.installPath.hashCode() : 0);

      result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

      result = 37*result + (servicesAutomanaged ? 0 : 1);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

      result = 37*result + ((this.mTime != null) ? this.mTime.hashCode() : 0);

      result = 37*result + ((this.cTime != null) ? this.cTime.hashCode() : 0);

	  return result;
   }

}
