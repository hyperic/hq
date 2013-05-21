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

import java.util.Collection;

import org.hyperic.hq.product.PlatformDetector;
import static org.hyperic.hq.appdef.shared.AIQueueConstants.Q_STATUS_ADDED;
import static org.hyperic.hq.appdef.shared.AIQueueConstants.Q_STATUS_CHANGED;
import static org.hyperic.hq.appdef.shared.AIQueueConstants.Q_STATUS_REMOVED;

/**
 * Value object for AIPlatform.
 *
 */
public class AIPlatformValue
   extends org.hyperic.hq.appdef.shared.AIAppdefResourceValue
   implements java.io.Serializable
{
   private String agentToken;
   private boolean agentTokenHasBeenSet = false;
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
   private long diff;
   private boolean diffHasBeenSet = false;
   private boolean ignored;
   private boolean ignoredHasBeenSet = false;
   private String platformTypeName;
   private boolean platformTypeNameHasBeenSet = false;
   private Long lastApproved;
   private boolean lastApprovedHasBeenSet = false;
   private java.lang.String certdn;
   private boolean certdnHasBeenSet = false;
   private java.lang.String fqdn;
   private boolean fqdnHasBeenSet = false;
   private java.lang.String name;
   private boolean nameHasBeenSet = false;
   private java.lang.String location;
   private boolean locationHasBeenSet = false;
   private java.lang.String description;
   private boolean descriptionHasBeenSet = false;
   private java.lang.Integer cpuCount;
   private boolean cpuCountHasBeenSet = false;
   private java.lang.Integer id;
   private boolean idHasBeenSet = false;
   private java.lang.Long mTime;
   private boolean mTimeHasBeenSet = false;
   private java.lang.Long cTime;
   private boolean cTimeHasBeenSet = false;
   private java.util.Collection AIIpValues = new java.util.HashSet();
   private Collection AIServerValues = new java.util.HashSet();

   public AIPlatformValue()
   {
   }

   public AIPlatformValue( String agentToken,int queueStatus,byte[] customProperties,byte[] productConfig,byte[] controlConfig,byte[] measurementConfig,long diff,boolean ignored,String platformTypeName,Long lastApproved,java.lang.String certdn,java.lang.String fqdn,java.lang.String name,java.lang.String location,java.lang.String description,java.lang.Integer cpuCount,java.lang.Integer id,java.lang.Long mTime,java.lang.Long cTime )
   {
	  this.agentToken = agentToken;
	  agentTokenHasBeenSet = true;
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
	  this.diff = diff;
	  diffHasBeenSet = true;
	  this.ignored = ignored;
	  ignoredHasBeenSet = true;
	  this.platformTypeName = platformTypeName;
	  platformTypeNameHasBeenSet = true;
	  this.lastApproved = lastApproved;
	  lastApprovedHasBeenSet = true;
	  this.certdn = certdn;
	  certdnHasBeenSet = true;
	  this.fqdn = fqdn;
	  fqdnHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.location = location;
	  locationHasBeenSet = true;
	  this.description = description;
	  descriptionHasBeenSet = true;
	  this.cpuCount = cpuCount;
	  cpuCountHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  this.mTime = mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = cTime;
	  cTimeHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public AIPlatformValue( AIPlatformValue otherValue )
   {
	  this.agentToken = otherValue.agentToken;
	  agentTokenHasBeenSet = true;
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
	  this.diff = otherValue.diff;
	  diffHasBeenSet = true;
	  this.ignored = otherValue.ignored;
	  ignoredHasBeenSet = true;
	  this.platformTypeName = otherValue.platformTypeName;
	  platformTypeNameHasBeenSet = true;
	  this.lastApproved = otherValue.lastApproved;
	  lastApprovedHasBeenSet = true;
	  this.certdn = otherValue.certdn;
	  certdnHasBeenSet = true;
	  this.fqdn = otherValue.fqdn;
	  fqdnHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.location = otherValue.location;
	  locationHasBeenSet = true;
	  this.description = otherValue.description;
	  descriptionHasBeenSet = true;
	  this.cpuCount = otherValue.cpuCount;
	  cpuCountHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.mTime = otherValue.mTime;
	  mTimeHasBeenSet = true;
	  this.cTime = otherValue.cTime;
	  cTimeHasBeenSet = true;
	// TODO Clone is better no ?
	  this.AIIpValues = otherValue.AIIpValues;
	// TODO Clone is better no ?
	  this.AIServerValues = otherValue.AIServerValues;
       super.setAutoApprove(otherValue.isAutoApprove());
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

   public boolean measurementConfigHasBeenSet(){
	  return measurementConfigHasBeenSet;
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
   public String getPlatformTypeName()
   {
	  return this.platformTypeName;
   }

   public void setPlatformTypeName( String platformTypeName )
   {
	  this.platformTypeName = platformTypeName;
	  platformTypeNameHasBeenSet = true;

   }

   public boolean platformTypeNameHasBeenSet(){
	  return platformTypeNameHasBeenSet;
   }
   public Long getLastApproved()
   {
	  return this.lastApproved;
   }

   public void setLastApproved( Long lastApproved )
   {
	  this.lastApproved = lastApproved;
	  lastApprovedHasBeenSet = true;

   }

   public boolean lastApprovedHasBeenSet(){
	  return lastApprovedHasBeenSet;
   }
   public java.lang.String getCertdn()
   {
	  return this.certdn;
   }

   public void setCertdn( java.lang.String certdn )
   {
	  this.certdn = certdn;
	  certdnHasBeenSet = true;

   }

   public boolean certdnHasBeenSet(){
	  return certdnHasBeenSet;
   }
   public java.lang.String getFqdn()
   {
	  return this.fqdn;
   }

   public void setFqdn( java.lang.String fqdn )
   {
	  this.fqdn = fqdn;
	  fqdnHasBeenSet = true;

   }

   public boolean fqdnHasBeenSet(){
	  return fqdnHasBeenSet;
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
   public java.lang.String getLocation()
   {
	  return this.location;
   }

   public void setLocation( java.lang.String location )
   {
	  this.location = location;
	  locationHasBeenSet = true;

   }

   public boolean locationHasBeenSet(){
	  return locationHasBeenSet;
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
   public java.lang.Integer getCpuCount()
   {
	  return this.cpuCount;
   }

   public void setCpuCount( java.lang.Integer cpuCount )
   {
	  this.cpuCount = cpuCount;
	  cpuCountHasBeenSet = true;

   }

   public boolean cpuCountHasBeenSet(){
	  return cpuCountHasBeenSet;
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

   protected java.util.Collection addedAIIpValues = new java.util.HashSet();
   protected java.util.Collection removedAIIpValues = new java.util.HashSet();
   protected java.util.Collection updatedAIIpValues = new java.util.HashSet();

   public java.util.Collection getAddedAIIpValues() { return addedAIIpValues; }
   public java.util.Collection getRemovedAIIpValues() { return removedAIIpValues; }
   public java.util.Collection getUpdatedAIIpValues() { return updatedAIIpValues; }

   public org.hyperic.hq.appdef.shared.AIIpValue[] getAIIpValues()
   {
	  return (org.hyperic.hq.appdef.shared.AIIpValue[])this.AIIpValues.toArray(new org.hyperic.hq.appdef.shared.AIIpValue[AIIpValues.size()]);
   }

    public void addAIIpValue(org.hyperic.hq.appdef.shared.AIIpValue added) {
        if (!this.AIIpValues.contains(added)) {
            this.AIIpValues.add(added);
        }
        int ipStatus = added.getQueueStatus();
        if (ipStatus == Q_STATUS_ADDED) {
            this.addedAIIpValues.add(added);
        } else if (ipStatus == Q_STATUS_CHANGED) {
            this.updatedAIIpValues.add(added);
        } else if (ipStatus == Q_STATUS_REMOVED) {
            this.removedAIIpValues.add(added);
        }
    }

   public void removeAIIpValue(org.hyperic.hq.appdef.shared.AIIpValue removed)
   {
	  this.AIIpValues.remove(removed);
	  this.removedAIIpValues.add(removed);
	  if (this.addedAIIpValues.contains(removed))
		 this.addedAIIpValues.remove(removed);
	  if (this.updatedAIIpValues.contains(removed))
		 this.updatedAIIpValues.remove(removed);
   }

   public void removeAllAIIpValues()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.AIIpValues.clear();
   }

   public void updateAIIpValue(org.hyperic.hq.appdef.shared.AIIpValue updated)
   {
	  if ( ! this.updatedAIIpValues.contains(updated))
		 this.updatedAIIpValues.add(updated);
   }

   public void cleanAIIpValue(){
	  this.addedAIIpValues = new java.util.HashSet();
	  this.removedAIIpValues = new java.util.HashSet();
	  this.updatedAIIpValues = new java.util.HashSet();
   }

   public void copyAIIpValuesFrom(org.hyperic.hq.appdef.shared.AIPlatformValue from)
   {
	  // TODO Clone the List ????
	  this.AIIpValues = from.AIIpValues;
   }
   protected Collection addedAIServerValues = new java.util.HashSet();
   protected Collection removedAIServerValues = new java.util.HashSet();
   protected Collection updatedAIServerValues = new java.util.HashSet();

   public Collection getAddedAIServerValues() { return addedAIServerValues; }
   public Collection getRemovedAIServerValues() { return removedAIServerValues; }
   public Collection getUpdatedAIServerValues() { return updatedAIServerValues; }

   public org.hyperic.hq.appdef.shared.AIServerValue[] getAIServerValues()
   {
	  return (org.hyperic.hq.appdef.shared.AIServerValue[])this.AIServerValues.toArray(new org.hyperic.hq.appdef.shared.AIServerValue[AIServerValues.size()]);
   }

   public void addAIServerValue(org.hyperic.hq.appdef.shared.AIServerValue added)
   {
	  this.AIServerValues.add(added);
	  if ( ! this.addedAIServerValues.contains(added))
		 this.addedAIServerValues.add(added);
   }

   public void removeAIServerValue(org.hyperic.hq.appdef.shared.AIServerValue removed)
   {
	  this.AIServerValues.remove(removed);
	  this.removedAIServerValues.add(removed);
	  if (this.addedAIServerValues.contains(removed))
		 this.addedAIServerValues.remove(removed);
	  if (this.updatedAIServerValues.contains(removed))
		 this.updatedAIServerValues.remove(removed);
   }

   public void removeAllAIServerValues()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.AIServerValues.clear();
   }

   public void updateAIServerValue(org.hyperic.hq.appdef.shared.AIServerValue updated)
   {
	  if ( ! this.updatedAIServerValues.contains(updated))
		 this.updatedAIServerValues.add(updated);
   }

   public void cleanAIServerValue(){
	  this.addedAIServerValues = new java.util.HashSet();
	  this.removedAIServerValues = new java.util.HashSet();
	  this.updatedAIServerValues = new java.util.HashSet();
   }

   public void copyAIServerValuesFrom(org.hyperic.hq.appdef.shared.AIPlatformValue from)
   {
	  // TODO Clone the List ????
	  this.AIServerValues = from.AIServerValues;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("agentToken=" + getAgentToken() + " " + "queueStatus=" + getQueueStatus() + " " + "customProperties=" + getCustomProperties() + " " + "productConfig=" + getProductConfig() + " " + "controlConfig=" + getControlConfig() + " " + "measurementConfig=" + getMeasurementConfig() + " " + "diff=" + getDiff() + " " + "ignored=" + getIgnored() + " " + "platformTypeName=" + getPlatformTypeName() + " " + "lastApproved=" + getLastApproved() + " " + "certdn=" + getCertdn() + " " + "fqdn=" + getFqdn() + " " + "name=" + getName() + " " + "location=" + getLocation() + " " + "description=" + getDescription() + " " + "cpuCount=" + getCpuCount() + " " + "id=" + getId() + " " + "mTime=" + getMTime() + " " + "cTime=" + getCTime());
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
	  if (other instanceof AIPlatformValue)
	  {
		 AIPlatformValue that = (AIPlatformValue) other;
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
	  if (other instanceof AIPlatformValue)
	  {
		 AIPlatformValue that = (AIPlatformValue) other;
		 boolean lEquals = true;
		 if( this.agentToken == null )
		 {
			lEquals = lEquals && ( that.agentToken == null );
		 }
		 else
		 {
			lEquals = lEquals && this.agentToken.equals( that.agentToken );
		 }
		 lEquals = lEquals && this.queueStatus == that.queueStatus;
		 lEquals = lEquals && this.customProperties == that.customProperties;
		 lEquals = lEquals && this.productConfig == that.productConfig;
		 lEquals = lEquals && this.controlConfig == that.controlConfig;
		 lEquals = lEquals && this.measurementConfig == that.measurementConfig;
		 lEquals = lEquals && this.diff == that.diff;
		 lEquals = lEquals && this.ignored == that.ignored;
		 if( this.platformTypeName == null )
		 {
			lEquals = lEquals && ( that.platformTypeName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.platformTypeName.equals( that.platformTypeName );
		 }
		 if( this.lastApproved == null )
		 {
			lEquals = lEquals && ( that.lastApproved == null );
		 }
		 else
		 {
			lEquals = lEquals && this.lastApproved.equals( that.lastApproved );
		 }
		 if( this.certdn == null )
		 {
			lEquals = lEquals && ( that.certdn == null );
		 }
		 else
		 {
			lEquals = lEquals && this.certdn.equals( that.certdn );
		 }
		 if( this.fqdn == null )
		 {
			lEquals = lEquals && ( that.fqdn == null );
		 }
		 else
		 {
			lEquals = lEquals && this.fqdn.equals( that.fqdn );
		 }
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }
		 if( this.location == null )
		 {
			lEquals = lEquals && ( that.location == null );
		 }
		 else
		 {
			lEquals = lEquals && this.location.equals( that.location );
		 }
		 if( this.description == null )
		 {
			lEquals = lEquals && ( that.description == null );
		 }
		 else
		 {
			lEquals = lEquals && this.description.equals( that.description );
		 }
		 if( this.cpuCount == null )
		 {
			lEquals = lEquals && ( that.cpuCount == null );
		 }
		 else
		 {
			lEquals = lEquals && this.cpuCount.equals( that.cpuCount );
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
		 if( this.getAIIpValues() == null )
		 {
			lEquals = lEquals && ( that.getAIIpValues() == null );
		 }
		 else
		 {
            // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
            // - javier 7/16/03
            java.util.Collection cmr1 = java.util.Arrays.asList(this.getAIIpValues());
            java.util.Collection cmr2 = java.util.Arrays.asList(that.getAIIpValues());
			// lEquals = lEquals && java.util.Arrays.equals(this.getAIIpValues() , that.getAIIpValues()) ;
            lEquals = lEquals && cmr1.containsAll(cmr2);
		 }
		 if( this.getAIServerValues() == null )
		 {
			lEquals = lEquals && ( that.getAIServerValues() == null );
		 }
		 else
		 {
            // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
            // - javier 7/16/03
            java.util.Collection cmr1 = java.util.Arrays.asList(this.getAIServerValues());
            java.util.Collection cmr2 = java.util.Arrays.asList(that.getAIServerValues());
			// lEquals = lEquals && java.util.Arrays.equals(this.getAIServerValues() , that.getAIServerValues()) ;
            lEquals = lEquals && cmr1.containsAll(cmr2);
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
      result = 37*result + ((this.agentToken != null) ? this.agentToken.hashCode() : 0);

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

      result = 37*result + (int)(diff^(diff>>>32));

      result = 37*result + (ignored ? 0 : 1);

      result = 37*result + ((this.platformTypeName != null) ? this.platformTypeName.hashCode() : 0);

      result = 37*result + ((this.lastApproved != null) ? this.lastApproved.hashCode() : 0);

      result = 37*result + ((this.certdn != null) ? this.certdn.hashCode() : 0);

      result = 37*result + ((this.fqdn != null) ? this.fqdn.hashCode() : 0);

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.location != null) ? this.location.hashCode() : 0);

      result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

      result = 37*result + ((this.cpuCount != null) ? this.cpuCount.hashCode() : 0);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

      result = 37*result + ((this.mTime != null) ? this.mTime.hashCode() : 0);

      result = 37*result + ((this.cTime != null) ? this.cTime.hashCode() : 0);

	  result = 37*result + ((this.getAIIpValues() != null) ? this.getAIIpValues().hashCode() : 0);
	  result = 37*result + ((this.getAIServerValues() != null) ? this.getAIServerValues().hashCode() : 0);
	  return result;
   }
   
   public boolean isPlatformDevice() {
       if (!(this instanceof AIPlatformValue)) {
           return false;
       }
       AIPlatformValue platform = (AIPlatformValue)this;
       //XXX add device flag to appdef schema, this works fine for now.
       return !PlatformDetector.isSupportedPlatform(platform.getPlatformTypeName());
   }

}
