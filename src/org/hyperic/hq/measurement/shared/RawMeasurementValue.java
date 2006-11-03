/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.measurement.shared;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;

/**
 * Value object for RawMeasurement.
 *
 */
public class RawMeasurementValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private String dsn;
   private boolean dsnHasBeenSet = false;
   private Integer id;
   private boolean idHasBeenSet = false;
   private Integer instanceId;
   private boolean instanceIdHasBeenSet = false;
   private long mtime;
   private boolean mtimeHasBeenSet = false;
   private org.hyperic.hq.measurement.shared.MeasurementTemplateValue Template;
   private boolean TemplateHasBeenSet = false;

   public RawMeasurementValue() {}

   public RawMeasurementValue( String dsn,Integer id,Integer instanceId,long mtime )
   {
	  this.dsn = dsn;
	  dsnHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  this.instanceId = instanceId;
	  instanceIdHasBeenSet = true;
	  this.mtime = mtime;
	  mtimeHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public RawMeasurementValue( RawMeasurementValue otherValue )
   {
	  this.dsn = otherValue.dsn;
	  dsnHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.instanceId = otherValue.instanceId;
	  instanceIdHasBeenSet = true;
	  this.mtime = otherValue.mtime;
	  mtimeHasBeenSet = true;
	// TODO Clone is better no ?
	  this.Template = otherValue.Template;
	  TemplateHasBeenSet = true;
   }

   public String getDsn()
   {
	  return this.dsn;
   }

   public void setDsn( String dsn )
   {
	  this.dsn = dsn;
	  dsnHasBeenSet = true;

   }

   public boolean dsnHasBeenSet(){
	  return dsnHasBeenSet;
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

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("dsn=" + getDsn() + " " + "id=" + getId() + " " + "instanceId=" + getInstanceId() + " " + "mtime=" + getMtime());
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
	  if (other instanceof RawMeasurementValue)
	  {
		 RawMeasurementValue that = (RawMeasurementValue) other;
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
	  if (other instanceof RawMeasurementValue)
	  {
		 RawMeasurementValue that = (RawMeasurementValue) other;
		 boolean lEquals = true;
		 if( this.dsn == null )
		 {
			lEquals = lEquals && ( that.dsn == null );
		 }
		 else
		 {
			lEquals = lEquals && this.dsn.equals( that.dsn );
		 }
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

		 return lEquals;
	  }
	  else
	  {
		 return false;
	  }
   }

   public int hashCode(){
	  int result = 17;
      result = 37*result + ((this.dsn != null) ? this.dsn.hashCode() : 0);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

      result = 37*result + ((this.instanceId != null) ? this.instanceId.hashCode() : 0);

      result = 37*result + (int)(mtime^(mtime>>>32));

	  result = 37*result + ((this.Template != null) ? this.Template.hashCode() : 0);
	  return result;
   }

}
