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
package org.hyperic.hq.measurement.shared;

import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.MonitorableType;

/**
 * Value object for MeasurementTemplate.
 *
 */
public class MeasurementTemplateValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String name;
   private boolean nameHasBeenSet = false;
   private String alias;
   private boolean aliasHasBeenSet = false;
   private String units;
   private boolean unitsHasBeenSet = false;
   private int collectionType;
   private boolean collectionTypeHasBeenSet = false;
   private boolean defaultOn;
   private boolean defaultOnHasBeenSet = false;
   private long defaultInterval;
   private boolean defaultIntervalHasBeenSet = false;
   private boolean designate;
   private boolean designateHasBeenSet = false;
   private String template;
   private boolean templateHasBeenSet = false;
   private byte[] expressionData;
   private boolean expressionDataHasBeenSet = false;
   private java.lang.String plugin;
   private boolean pluginHasBeenSet = false;
   private long ctime;
   private boolean ctimeHasBeenSet = false;
   private long mtime;
   private boolean mtimeHasBeenSet = false;
   private MonitorableType MonitorableType;
   private Category Category;
   
   public MeasurementTemplateValue() {}

   public MeasurementTemplateValue( Integer id,String name,String alias,String units,int collectionType,boolean defaultOn,long defaultInterval,boolean designate,String template,byte[] expressionData,java.lang.String plugin,long ctime,long mtime )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.alias = alias;
	  aliasHasBeenSet = true;
	  this.units = units;
	  unitsHasBeenSet = true;
	  this.collectionType = collectionType;
	  collectionTypeHasBeenSet = true;
	  this.defaultOn = defaultOn;
	  defaultOnHasBeenSet = true;
	  this.defaultInterval = defaultInterval;
	  defaultIntervalHasBeenSet = true;
	  this.designate = designate;
	  designateHasBeenSet = true;
	  this.template = template;
	  templateHasBeenSet = true;
	  this.expressionData = expressionData;
	  expressionDataHasBeenSet = true;
	  this.plugin = plugin;
	  pluginHasBeenSet = true;
	  this.ctime = ctime;
	  ctimeHasBeenSet = true;
	  this.mtime = mtime;
	  mtimeHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public MeasurementTemplateValue( MeasurementTemplateValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.alias = otherValue.alias;
	  aliasHasBeenSet = true;
	  this.units = otherValue.units;
	  unitsHasBeenSet = true;
	  this.collectionType = otherValue.collectionType;
	  collectionTypeHasBeenSet = true;
	  this.defaultOn = otherValue.defaultOn;
	  defaultOnHasBeenSet = true;
	  this.defaultInterval = otherValue.defaultInterval;
	  defaultIntervalHasBeenSet = true;
	  this.designate = otherValue.designate;
	  designateHasBeenSet = true;
	  this.template = otherValue.template;
	  templateHasBeenSet = true;
	  this.expressionData = otherValue.expressionData;
	  expressionDataHasBeenSet = true;
	  this.plugin = otherValue.plugin;
	  pluginHasBeenSet = true;
	  this.ctime = otherValue.ctime;
	  ctimeHasBeenSet = true;
	  this.mtime = otherValue.mtime;
	  mtimeHasBeenSet = true;
	// TODO Clone is better no ?
	  this.MonitorableType = otherValue.MonitorableType;
	  // TODO Clone is better no ?
	  this.Category = otherValue.Category;
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
   public String getAlias()
   {
	  return this.alias;
   }

   public void setAlias( String alias )
   {
	  this.alias = alias;
	  aliasHasBeenSet = true;

   }

   public boolean aliasHasBeenSet(){
	  return aliasHasBeenSet;
   }
   public String getUnits()
   {
	  return this.units;
   }

   public void setUnits( String units )
   {
	  this.units = units;
	  unitsHasBeenSet = true;

   }

   public boolean unitsHasBeenSet(){
	  return unitsHasBeenSet;
   }
   public int getCollectionType()
   {
	  return this.collectionType;
   }

   public void setCollectionType( int collectionType )
   {
	  this.collectionType = collectionType;
	  collectionTypeHasBeenSet = true;

   }

   public boolean collectionTypeHasBeenSet(){
	  return collectionTypeHasBeenSet;
   }
   public boolean getDefaultOn()
   {
	  return this.defaultOn;
   }

   public void setDefaultOn( boolean defaultOn )
   {
	  this.defaultOn = defaultOn;
	  defaultOnHasBeenSet = true;

   }

   public boolean defaultOnHasBeenSet(){
	  return defaultOnHasBeenSet;
   }
   public long getDefaultInterval()
   {
	  return this.defaultInterval;
   }

   public void setDefaultInterval( long defaultInterval )
   {
	  this.defaultInterval = defaultInterval;
	  defaultIntervalHasBeenSet = true;

   }

   public boolean defaultIntervalHasBeenSet(){
	  return defaultIntervalHasBeenSet;
   }
   public boolean getDesignate()
   {
	  return this.designate;
   }

   public void setDesignate( boolean designate )
   {
	  this.designate = designate;
	  designateHasBeenSet = true;

   }

   public boolean designateHasBeenSet(){
	  return designateHasBeenSet;
   }
   public String getTemplate()
   {
	  return this.template;
   }

   public void setTemplate( String template )
   {
	  this.template = template;
	  templateHasBeenSet = true;

   }

   public boolean templateHasBeenSet(){
	  return templateHasBeenSet;
   }
   public byte[] getExpressionData()
   {
	  return this.expressionData;
   }

   public void setExpressionData( byte[] expressionData )
   {
	  this.expressionData = expressionData;
	  expressionDataHasBeenSet = true;

   }

   public boolean expressionDataHasBeenSet(){
	  return expressionDataHasBeenSet;
   }
   public java.lang.String getPlugin()
   {
	  return this.plugin;
   }

   public void setPlugin( java.lang.String plugin )
   {
	  this.plugin = plugin;
	  pluginHasBeenSet = true;

   }

   public boolean pluginHasBeenSet(){
	  return pluginHasBeenSet;
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
   public long getMtime()
   {
	  return this.mtime;
   }

   public void setMtime( long mtime )
   {
	  this.mtime = mtime;
	  mtimeHasBeenSet = true;

   }

   public boolean mtimeHasBeenSet(){
	  return mtimeHasBeenSet;
   }

   public MonitorableType getMonitorableType()
   {
	  return this.MonitorableType;
   }
   public void setMonitorableType( MonitorableType MonitorableType )
   {
	  this.MonitorableType = MonitorableType;
   }
   public Category getCategory()
   {
	  return this.Category;
   }
   public void setCategory( Category Category )
   {
	  this.Category = Category;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "name=" + getName() + " " + "alias=" + getAlias() + " " + "units=" + getUnits() + " " + "collectionType=" + getCollectionType() + " " + "defaultOn=" + getDefaultOn() + " " + "defaultInterval=" + getDefaultInterval() + " " + "designate=" + getDesignate() + " " + "template=" + getTemplate() + " " + "expressionData=" + getExpressionData() + " " + "plugin=" + getPlugin() + " " + "ctime=" + getCtime() + " " + "mtime=" + getMtime());
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
	  if (other instanceof MeasurementTemplateValue)
	  {
		 MeasurementTemplateValue that = (MeasurementTemplateValue) other;
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
	  if (other instanceof MeasurementTemplateValue)
	  {
		 MeasurementTemplateValue that = (MeasurementTemplateValue) other;
		 boolean lEquals = true;
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }
		 if( this.alias == null )
		 {
			lEquals = lEquals && ( that.alias == null );
		 }
		 else
		 {
			lEquals = lEquals && this.alias.equals( that.alias );
		 }
		 if( this.units == null )
		 {
			lEquals = lEquals && ( that.units == null );
		 }
		 else
		 {
			lEquals = lEquals && this.units.equals( that.units );
		 }
		 lEquals = lEquals && this.collectionType == that.collectionType;
		 lEquals = lEquals && this.defaultOn == that.defaultOn;
		 lEquals = lEquals && this.defaultInterval == that.defaultInterval;
		 lEquals = lEquals && this.designate == that.designate;
		 if( this.template == null )
		 {
			lEquals = lEquals && ( that.template == null );
		 }
		 else
		 {
			lEquals = lEquals && this.template.equals( that.template );
		 }
		 lEquals = lEquals && this.expressionData == that.expressionData;
		 if( this.plugin == null )
		 {
			lEquals = lEquals && ( that.plugin == null );
		 }
		 else
		 {
			lEquals = lEquals && this.plugin.equals( that.plugin );
		 }
		 lEquals = lEquals && this.ctime == that.ctime;
		 lEquals = lEquals && this.mtime == that.mtime;
		 if( this.MonitorableType == null )
		 {
			lEquals = lEquals && ( that.MonitorableType == null );
		 }
		 else
		 {
			lEquals = lEquals && this.MonitorableType.equals( that.MonitorableType );
		 }
		 if( this.Category == null )
		 {
			lEquals = lEquals && ( that.Category == null );
		 }
		 else
		 {
			lEquals = lEquals && this.Category.equals( that.Category );
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

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.alias != null) ? this.alias.hashCode() : 0);

      result = 37*result + ((this.units != null) ? this.units.hashCode() : 0);

      result = 37*result + (int) collectionType;

      result = 37*result + (defaultOn ? 0 : 1);

      result = 37*result + (int)(defaultInterval^(defaultInterval>>>32));

      result = 37*result + (designate ? 0 : 1);

      result = 37*result + ((this.template != null) ? this.template.hashCode() : 0);

      for (int i=0;  expressionData != null && i<expressionData.length; i++)
      {
         long l = expressionData[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      result = 37*result + ((this.plugin != null) ? this.plugin.hashCode() : 0);

      result = 37*result + (int)(ctime^(ctime>>>32));

      result = 37*result + (int)(mtime^(mtime>>>32));

	  result = 37*result + ((this.MonitorableType != null) ? this.MonitorableType.hashCode() : 0);
	  result = 37*result + ((this.Category != null) ? this.Category.hashCode() : 0);
	  return result;
   }

}
