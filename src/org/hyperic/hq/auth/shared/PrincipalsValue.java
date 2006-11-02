/*
 * xdoclet generated code.
 * legacy DTO pattern (targeted to be replaced with hibernate pojo).
 */
package org.hyperic.hq.auth.shared;

/**
 * Value object for Principals.
 *
 */
public class PrincipalsValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private Integer id;
   private boolean idHasBeenSet = false;
   private String principal;
   private boolean principalHasBeenSet = false;
   private String password;
   private boolean passwordHasBeenSet = false;

   private org.hyperic.hq.auth.shared.PrincipalsPK pk;

   public PrincipalsValue()
   {
	  pk = new org.hyperic.hq.auth.shared.PrincipalsPK();
   }

   public PrincipalsValue( Integer id,String principal,String password )
   {
	  this.id = id;
	  idHasBeenSet = true;
	  this.principal = principal;
	  principalHasBeenSet = true;
	  this.password = password;
	  passwordHasBeenSet = true;
	  pk = new org.hyperic.hq.auth.shared.PrincipalsPK(this.getId());
   }

   //TODO Cloneable is better than this !
   public PrincipalsValue( PrincipalsValue otherValue )
   {
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	  this.principal = otherValue.principal;
	  principalHasBeenSet = true;
	  this.password = otherValue.password;
	  passwordHasBeenSet = true;

	  pk = new org.hyperic.hq.auth.shared.PrincipalsPK(this.getId());
   }

   public org.hyperic.hq.auth.shared.PrincipalsPK getPrimaryKey()
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
   public String getPrincipal()
   {
	  return this.principal;
   }

   public void setPrincipal( String principal )
   {
	  this.principal = principal;
	  principalHasBeenSet = true;

   }

   public boolean principalHasBeenSet(){
	  return principalHasBeenSet;
   }
   public String getPassword()
   {
	  return this.password;
   }

   public void setPassword( String password )
   {
	  this.password = password;
	  passwordHasBeenSet = true;

   }

   public boolean passwordHasBeenSet(){
	  return passwordHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("id=" + getId() + " " + "principal=" + getPrincipal() + " " + "password=" + getPassword());
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
	  if (other instanceof PrincipalsValue)
	  {
		 PrincipalsValue that = (PrincipalsValue) other;
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
	  if (other instanceof PrincipalsValue)
	  {
		 PrincipalsValue that = (PrincipalsValue) other;
		 boolean lEquals = true;
		 if( this.principal == null )
		 {
			lEquals = lEquals && ( that.principal == null );
		 }
		 else
		 {
			lEquals = lEquals && this.principal.equals( that.principal );
		 }
		 if( this.password == null )
		 {
			lEquals = lEquals && ( that.password == null );
		 }
		 else
		 {
			lEquals = lEquals && this.password.equals( that.password );
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

      result = 37*result + ((this.principal != null) ? this.principal.hashCode() : 0);

      result = 37*result + ((this.password != null) ? this.password.hashCode() : 0);

	  return result;
   }

}
