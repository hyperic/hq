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
 * Value object for MonitorableType.
 *
 */
public class MonitorableTypeValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String name;
   private boolean nameHasBeenSet = false;
   private int appdefType;
   private boolean appdefTypeHasBeenSet = false;
   private java.lang.String plugin;
   private boolean pluginHasBeenSet = false;

   public MonitorableTypeValue() {}

   public MonitorableTypeValue( Integer id,String name,int appdefType,java.lang.String plugin )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.appdefType = appdefType;
	  appdefTypeHasBeenSet = true;
	  this.plugin = plugin;
	  pluginHasBeenSet = true;
   }

   //TODO Cloneable is better than this !
   public MonitorableTypeValue( MonitorableTypeValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.appdefType = otherValue.appdefType;
	  appdefTypeHasBeenSet = true;
	  this.plugin = otherValue.plugin;
	  pluginHasBeenSet = true;
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

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "name=" + getName() + " " + "appdefType=" + getAppdefType() + " " + "plugin=" + getPlugin());
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
	  if (other instanceof MonitorableTypeValue)
	  {
		 MonitorableTypeValue that = (MonitorableTypeValue) other;
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
	  if (other instanceof MonitorableTypeValue)
	  {
		 MonitorableTypeValue that = (MonitorableTypeValue) other;
		 boolean lEquals = true;
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }
		 lEquals = lEquals && this.appdefType == that.appdefType;
		 if( this.plugin == null )
		 {
			lEquals = lEquals && ( that.plugin == null );
		 }
		 else
		 {
			lEquals = lEquals && this.plugin.equals( that.plugin );
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

      result = 37*result + (int) appdefType;

      result = 37*result + ((this.plugin != null) ? this.plugin.hashCode() : 0);

	  return result;
   }

}
