/*
 * xdoclet generated code.
 * legacy DTO pattern (targeted to be replaced with hibernate pojo).
 */
package org.hyperic.hq.appdef.shared;

/**
 * Value object for ConfigResponse.
 *
 */
public class ConfigResponseValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private byte[] productResponse;
   private boolean productResponseHasBeenSet = false;
   private byte[] controlResponse;
   private boolean controlResponseHasBeenSet = false;
   private byte[] measurementResponse;
   private boolean measurementResponseHasBeenSet = false;
   private byte[] autoinventoryResponse;
   private boolean autoinventoryResponseHasBeenSet = false;
   private byte[] responseTimeResponse;
   private boolean responseTimeResponseHasBeenSet = false;
   private boolean userManaged;
   private boolean userManagedHasBeenSet = false;
   private String validationError;
   private boolean validationErrorHasBeenSet = false;

   private org.hyperic.hq.appdef.shared.ConfigResponsePK pk;

   public ConfigResponseValue()
   {
	  pk = new org.hyperic.hq.appdef.shared.ConfigResponsePK();
   }

   public ConfigResponseValue( Integer id,byte[] productResponse,byte[] controlResponse,byte[] measurementResponse,byte[] autoinventoryResponse,byte[] responseTimeResponse,boolean userManaged,String validationError )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.productResponse = productResponse;
	  productResponseHasBeenSet = true;
	  this.controlResponse = controlResponse;
	  controlResponseHasBeenSet = true;
	  this.measurementResponse = measurementResponse;
	  measurementResponseHasBeenSet = true;
	  this.autoinventoryResponse = autoinventoryResponse;
	  autoinventoryResponseHasBeenSet = true;
	  this.responseTimeResponse = responseTimeResponse;
	  responseTimeResponseHasBeenSet = true;
	  this.userManaged = userManaged;
	  userManagedHasBeenSet = true;
	  this.validationError = validationError;
	  validationErrorHasBeenSet = true;
	  pk = new org.hyperic.hq.appdef.shared.ConfigResponsePK(this.getId());
   }

   //TODO Cloneable is better than this !
   public ConfigResponseValue( ConfigResponseValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.productResponse = otherValue.productResponse;
	  productResponseHasBeenSet = true;
	  this.controlResponse = otherValue.controlResponse;
	  controlResponseHasBeenSet = true;
	  this.measurementResponse = otherValue.measurementResponse;
	  measurementResponseHasBeenSet = true;
	  this.autoinventoryResponse = otherValue.autoinventoryResponse;
	  autoinventoryResponseHasBeenSet = true;
	  this.responseTimeResponse = otherValue.responseTimeResponse;
	  responseTimeResponseHasBeenSet = true;
	  this.userManaged = otherValue.userManaged;
	  userManagedHasBeenSet = true;
	  this.validationError = otherValue.validationError;
	  validationErrorHasBeenSet = true;

	  pk = new org.hyperic.hq.appdef.shared.ConfigResponsePK(this.getId());
   }

   public org.hyperic.hq.appdef.shared.ConfigResponsePK getPrimaryKey()
   {
	  return pk;
   }

   public Integer getId()
   {
	  return this.id;
   }

   public void setId( Integer id )
   {
	  this.id = id;
	  idHasBeenSet = true;

		 pk.setId(id);
   }

   public boolean idHasBeenSet(){
	  return idHasBeenSet;
   }
   public byte[] getProductResponse()
   {
	  return this.productResponse;
   }

   public void setProductResponse( byte[] productResponse )
   {
	  this.productResponse = productResponse;
	  productResponseHasBeenSet = true;

   }

   public boolean productResponseHasBeenSet(){
	  return productResponseHasBeenSet;
   }
   public byte[] getControlResponse()
   {
	  return this.controlResponse;
   }

   public void setControlResponse( byte[] controlResponse )
   {
	  this.controlResponse = controlResponse;
	  controlResponseHasBeenSet = true;

   }

   public boolean controlResponseHasBeenSet(){
	  return controlResponseHasBeenSet;
   }
   public byte[] getMeasurementResponse()
   {
	  return this.measurementResponse;
   }

   public void setMeasurementResponse( byte[] measurementResponse )
   {
	  this.measurementResponse = measurementResponse;
	  measurementResponseHasBeenSet = true;

   }

   public boolean measurementResponseHasBeenSet(){
	  return measurementResponseHasBeenSet;
   }
   public byte[] getAutoinventoryResponse()
   {
	  return this.autoinventoryResponse;
   }

   public void setAutoinventoryResponse( byte[] autoinventoryResponse )
   {
	  this.autoinventoryResponse = autoinventoryResponse;
	  autoinventoryResponseHasBeenSet = true;

   }

   public boolean autoinventoryResponseHasBeenSet(){
	  return autoinventoryResponseHasBeenSet;
   }
   public byte[] getResponseTimeResponse()
   {
	  return this.responseTimeResponse;
   }

   public void setResponseTimeResponse( byte[] responseTimeResponse )
   {
	  this.responseTimeResponse = responseTimeResponse;
	  responseTimeResponseHasBeenSet = true;

   }

   public boolean responseTimeResponseHasBeenSet(){
	  return responseTimeResponseHasBeenSet;
   }
   public boolean getUserManaged()
   {
	  return this.userManaged;
   }

   public void setUserManaged( boolean userManaged )
   {
	  this.userManaged = userManaged;
	  userManagedHasBeenSet = true;

   }

   public boolean userManagedHasBeenSet(){
	  return userManagedHasBeenSet;
   }
   public String getValidationError()
   {
	  return this.validationError;
   }

   public void setValidationError( String validationError )
   {
	  this.validationError = validationError;
	  validationErrorHasBeenSet = true;

   }

   public boolean validationErrorHasBeenSet(){
	  return validationErrorHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "productResponse=" + getProductResponse() + " " + "controlResponse=" + getControlResponse() + " " + "measurementResponse=" + getMeasurementResponse() + " " + "autoinventoryResponse=" + getAutoinventoryResponse() + " " + "responseTimeResponse=" + getResponseTimeResponse() + " " + "userManaged=" + getUserManaged() + " " + "validationError=" + getValidationError());
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
	  if (other instanceof ConfigResponseValue)
	  {
		 ConfigResponseValue that = (ConfigResponseValue) other;
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
	  if (other instanceof ConfigResponseValue)
	  {
		 ConfigResponseValue that = (ConfigResponseValue) other;
		 boolean lEquals = true;
		 lEquals = lEquals && this.productResponse == that.productResponse;
		 lEquals = lEquals && this.controlResponse == that.controlResponse;
		 lEquals = lEquals && this.measurementResponse == that.measurementResponse;
		 lEquals = lEquals && this.autoinventoryResponse == that.autoinventoryResponse;
		 lEquals = lEquals && this.responseTimeResponse == that.responseTimeResponse;
		 lEquals = lEquals && this.userManaged == that.userManaged;
		 if( this.validationError == null )
		 {
			lEquals = lEquals && ( that.validationError == null );
		 }
		 else
		 {
			lEquals = lEquals && this.validationError.equals( that.validationError );
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

      for (int i=0;  productResponse != null && i<productResponse.length; i++)
      {
         long l = productResponse[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      for (int i=0;  controlResponse != null && i<controlResponse.length; i++)
      {
         long l = controlResponse[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      for (int i=0;  measurementResponse != null && i<measurementResponse.length; i++)
      {
         long l = measurementResponse[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      for (int i=0;  autoinventoryResponse != null && i<autoinventoryResponse.length; i++)
      {
         long l = autoinventoryResponse[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      for (int i=0;  responseTimeResponse != null && i<responseTimeResponse.length; i++)
      {
         long l = responseTimeResponse[i];
         result = 37*result + (int)(l^(l>>>32));
      }

      result = 37*result + (userManaged ? 0 : 1);

      result = 37*result + ((this.validationError != null) ? this.validationError.hashCode() : 0);

	  return result;
   }

}
