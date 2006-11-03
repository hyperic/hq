/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.measurement.shared;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * Value object for MeasurementArg.
 *
 */
public class MeasurementArgValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private Integer placement;
   private boolean placementHasBeenSet = false;
   private Integer ticks;
   private boolean ticksHasBeenSet = false;
   private Float weight;
   private boolean weightHasBeenSet = false;
   private Integer previous;
   private boolean previousHasBeenSet = false;
   private org.hyperic.hq.measurement.shared.MeasurementTemplateValue MeasurementTemplateArg;
   private boolean MeasurementTemplateArgHasBeenSet = false;

   public MeasurementArgValue() {}

   public MeasurementArgValue( Integer id,Integer placement,Integer ticks,Float weight,Integer previous )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.placement = placement;
	  placementHasBeenSet = true;
	  this.ticks = ticks;
	  ticksHasBeenSet = true;
	  this.weight = weight;
	  weightHasBeenSet = true;
	  this.previous = previous;
	  previousHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public MeasurementArgValue( MeasurementArgValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.placement = otherValue.placement;
	  placementHasBeenSet = true;
	  this.ticks = otherValue.ticks;
	  ticksHasBeenSet = true;
	  this.weight = otherValue.weight;
	  weightHasBeenSet = true;
	  this.previous = otherValue.previous;
	  previousHasBeenSet = true;
	// TODO Clone is better no ?
	  this.MeasurementTemplateArg = otherValue.MeasurementTemplateArg;
	  MeasurementTemplateArgHasBeenSet = true;
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
   public Integer getPlacement()
   {
	  return this.placement;
   }

   public void setPlacement( Integer placement )
   {
	  this.placement = placement;
	  placementHasBeenSet = true;

   }

   public boolean placementHasBeenSet(){
	  return placementHasBeenSet;
   }
   public Integer getTicks()
   {
	  return this.ticks;
   }

   public void setTicks( Integer ticks )
   {
	  this.ticks = ticks;
	  ticksHasBeenSet = true;

   }

   public boolean ticksHasBeenSet(){
	  return ticksHasBeenSet;
   }
   public Float getWeight()
   {
	  return this.weight;
   }

   public void setWeight( Float weight )
   {
	  this.weight = weight;
	  weightHasBeenSet = true;

   }

   public boolean weightHasBeenSet(){
	  return weightHasBeenSet;
   }
   public Integer getPrevious()
   {
	  return this.previous;
   }

   public void setPrevious( Integer previous )
   {
	  this.previous = previous;
	  previousHasBeenSet = true;

   }

   public boolean previousHasBeenSet(){
	  return previousHasBeenSet;
   }

   public org.hyperic.hq.measurement.shared.MeasurementTemplateValue getMeasurementTemplateArg()
   {
	  return this.MeasurementTemplateArg;
   }
   public void setMeasurementTemplateArg( org.hyperic.hq.measurement.shared.MeasurementTemplateValue MeasurementTemplateArg )
   {
	  this.MeasurementTemplateArg = MeasurementTemplateArg;
	  MeasurementTemplateArgHasBeenSet = true;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "placement=" + getPlacement() + " " + "ticks=" + getTicks() + " " + "weight=" + getWeight() + " " + "previous=" + getPrevious());
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
	  if (other instanceof MeasurementArgValue)
	  {
		 MeasurementArgValue that = (MeasurementArgValue) other;
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
	  if (other instanceof MeasurementArgValue)
	  {
		 MeasurementArgValue that = (MeasurementArgValue) other;
		 boolean lEquals = true;
		 if( this.placement == null )
		 {
			lEquals = lEquals && ( that.placement == null );
		 }
		 else
		 {
			lEquals = lEquals && this.placement.equals( that.placement );
		 }
		 if( this.ticks == null )
		 {
			lEquals = lEquals && ( that.ticks == null );
		 }
		 else
		 {
			lEquals = lEquals && this.ticks.equals( that.ticks );
		 }
		 if( this.weight == null )
		 {
			lEquals = lEquals && ( that.weight == null );
		 }
		 else
		 {
			lEquals = lEquals && this.weight.equals( that.weight );
		 }
		 if( this.previous == null )
		 {
			lEquals = lEquals && ( that.previous == null );
		 }
		 else
		 {
			lEquals = lEquals && this.previous.equals( that.previous );
		 }
		 if( this.MeasurementTemplateArg == null )
		 {
			lEquals = lEquals && ( that.MeasurementTemplateArg == null );
		 }
		 else
		 {
			lEquals = lEquals && this.MeasurementTemplateArg.equals( that.MeasurementTemplateArg );
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

      result = 37*result + ((this.placement != null) ? this.placement.hashCode() : 0);

      result = 37*result + ((this.ticks != null) ? this.ticks.hashCode() : 0);

      result = 37*result + ((this.weight != null) ? this.weight.hashCode() : 0);

      result = 37*result + ((this.previous != null) ? this.previous.hashCode() : 0);

	  result = 37*result + ((this.MeasurementTemplateArg != null) ? this.MeasurementTemplateArg.hashCode() : 0);
	  return result;
   }

}
