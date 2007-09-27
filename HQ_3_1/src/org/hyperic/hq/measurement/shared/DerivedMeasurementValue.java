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

import java.util.ArrayList;
import java.util.Collection;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;

/**
 * Value object for DerivedMeasurement.
 *
 */
public class DerivedMeasurementValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private boolean enabled;
   private boolean enabledHasBeenSet = false;
   private long interval;
   private boolean intervalHasBeenSet = false;
   private String formula;
   private boolean formulaHasBeenSet = false;
   private int appdefType;
   private boolean appdefTypeHasBeenSet = false;
   private Integer id;
   private boolean idHasBeenSet = false;
   private Integer instanceId;
   private boolean instanceIdHasBeenSet = false;
   private long mtime;
   private boolean mtimeHasBeenSet = false;
   private org.hyperic.hq.measurement.shared.MeasurementTemplateValue Template;
   private boolean TemplateHasBeenSet = false;
   private org.hyperic.hq.measurement.shared.BaselineValue Baseline;
   private boolean BaselineHasBeenSet = false;

   public DerivedMeasurementValue() {}

   public DerivedMeasurementValue( boolean enabled,long interval,String formula,int appdefType,Integer id,Integer instanceId,long mtime )
   {
	  this.enabled = enabled;
	  enabledHasBeenSet = true;
	  this.interval = interval;
	  intervalHasBeenSet = true;
	  this.formula = formula;
	  formulaHasBeenSet = true;
	  this.appdefType = appdefType;
	  appdefTypeHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  this.instanceId = instanceId;
	  instanceIdHasBeenSet = true;
	  this.mtime = mtime;
	  mtimeHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public DerivedMeasurementValue( DerivedMeasurementValue otherValue )
   {
	  this.enabled = otherValue.enabled;
	  enabledHasBeenSet = true;
	  this.interval = otherValue.interval;
	  intervalHasBeenSet = true;
	  this.formula = otherValue.formula;
	  formulaHasBeenSet = true;
	  this.appdefType = otherValue.appdefType;
	  appdefTypeHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.instanceId = otherValue.instanceId;
	  instanceIdHasBeenSet = true;
	  this.mtime = otherValue.mtime;
	  mtimeHasBeenSet = true;
	// TODO Clone is better no ?
	  this.Template = otherValue.Template;
	  TemplateHasBeenSet = true;
	// TODO Clone is better no ?
	  this.Baseline = otherValue.Baseline;
	  BaselineHasBeenSet = true;
   }

   public boolean getEnabled()
   {
	  return this.enabled;
   }

   public void setEnabled( boolean enabled )
   {
	  this.enabled = enabled;
	  enabledHasBeenSet = true;

   }

   public boolean enabledHasBeenSet(){
	  return enabledHasBeenSet;
   }
   public long getInterval()
   {
	  return this.interval;
   }

   public void setInterval( long interval )
   {
	  this.interval = interval;
	  intervalHasBeenSet = true;

   }

   public boolean intervalHasBeenSet(){
	  return intervalHasBeenSet;
   }
   public String getFormula()
   {
	  return this.formula;
   }

   public void setFormula( String formula )
   {
	  this.formula = formula;
	  formulaHasBeenSet = true;

   }

   public boolean formulaHasBeenSet(){
	  return formulaHasBeenSet;
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
   public Integer getInstanceId()
   {
	  return this.instanceId;
   }

   public void setInstanceId( Integer instanceId )
   {
	  this.instanceId = instanceId;
	  instanceIdHasBeenSet = true;

   }

   public boolean instanceIdHasBeenSet(){
	  return instanceIdHasBeenSet;
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

   public org.hyperic.hq.measurement.shared.MeasurementTemplateValue getTemplate()
   {
	  return this.Template;
   }
   public void setTemplate( org.hyperic.hq.measurement.shared.MeasurementTemplateValue Template )
   {
	  this.Template = Template;
	  TemplateHasBeenSet = true;
   }
   public org.hyperic.hq.measurement.shared.BaselineValue getBaseline()
   {
	  return this.Baseline;
   }
   public void setBaseline( org.hyperic.hq.measurement.shared.BaselineValue Baseline )
   {
	  this.Baseline = Baseline;
	  BaselineHasBeenSet = true;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("enabled=" + getEnabled() + " " + "interval=" + getInterval() + " " + "formula=" + getFormula() + " " + "appdefType=" + getAppdefType() + " " + "id=" + getId() + " " + "instanceId=" + getInstanceId() + " " + "mtime=" + getMtime());
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
	  if (other instanceof DerivedMeasurementValue)
	  {
		 DerivedMeasurementValue that = (DerivedMeasurementValue) other;
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
	  if (other instanceof DerivedMeasurementValue)
	  {
		 DerivedMeasurementValue that = (DerivedMeasurementValue) other;
		 boolean lEquals = true;
		 lEquals = lEquals && this.enabled == that.enabled;
		 lEquals = lEquals && this.interval == that.interval;
		 if( this.formula == null )
		 {
			lEquals = lEquals && ( that.formula == null );
		 }
		 else
		 {
			lEquals = lEquals && this.formula.equals( that.formula );
		 }
		 lEquals = lEquals && this.appdefType == that.appdefType;
		 if( this.instanceId == null )
		 {
			lEquals = lEquals && ( that.instanceId == null );
		 }
		 else
		 {
			lEquals = lEquals && this.instanceId.equals( that.instanceId );
		 }
		 lEquals = lEquals && this.mtime == that.mtime;
		 if( this.Template == null )
		 {
			lEquals = lEquals && ( that.Template == null );
		 }
		 else
		 {
			lEquals = lEquals && this.Template.equals( that.Template );
		 }
		 if( this.Baseline == null )
		 {
			lEquals = lEquals && ( that.Baseline == null );
		 }
		 else
		 {
			lEquals = lEquals && this.Baseline.equals( that.Baseline );
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
      result = 37*result + (enabled ? 0 : 1);

      result = 37*result + (int)(interval^(interval>>>32));

      result = 37*result + ((this.formula != null) ? this.formula.hashCode() : 0);

      result = 37*result + (int) appdefType;

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

      result = 37*result + ((this.instanceId != null) ? this.instanceId.hashCode() : 0);

      result = 37*result + (int)(mtime^(mtime>>>32));

	  result = 37*result + ((this.Template != null) ? this.Template.hashCode() : 0);
	  result = 37*result + ((this.Baseline != null) ? this.Baseline.hashCode() : 0);
	  return result;
   }

}
