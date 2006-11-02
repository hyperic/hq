/*
 * xdoclet generated file
 * legacy DTO pattern (targeted for replacement with hibernate pojo)
 */
package org.hyperic.hq.authz.shared;

/**
 * Value object for AuthzSubject.
 *
 */
public class AuthzSubjectValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private java.lang.String sortName;
   private boolean sortNameHasBeenSet = false;
   private boolean active;
   private boolean activeHasBeenSet = false;
   private boolean system;
   private boolean systemHasBeenSet = false;
   private String authDsn;
   private boolean authDsnHasBeenSet = false;
   private String emailAddress;
   private boolean emailAddressHasBeenSet = false;
   private String sMSAddress;
   private boolean sMSAddressHasBeenSet = false;
   private String firstName;
   private boolean firstNameHasBeenSet = false;
   private String lastName;
   private boolean lastNameHasBeenSet = false;
   private String phoneNumber;
   private boolean phoneNumberHasBeenSet = false;
   private String department;
   private boolean departmentHasBeenSet = false;
   private String name;
   private boolean nameHasBeenSet = false;
   private Integer id;
   private boolean idHasBeenSet = false;

   private org.hyperic.hq.authz.shared.AuthzSubjectPK pk;

   public AuthzSubjectValue()
   {
	  pk = new org.hyperic.hq.authz.shared.AuthzSubjectPK();
   }

   public AuthzSubjectValue( java.lang.String sortName,boolean active,boolean system,String authDsn,String emailAddress,String sMSAddress,String firstName,String lastName,String phoneNumber,String department,String name,Integer id )
   {
	  this.sortName = sortName;
	  sortNameHasBeenSet = true;
	  this.active = active;
	  activeHasBeenSet = true;
	  this.system = system;
	  systemHasBeenSet = true;
	  this.authDsn = authDsn;
	  authDsnHasBeenSet = true;
	  this.emailAddress = emailAddress;
	  emailAddressHasBeenSet = true;
	  this.sMSAddress = sMSAddress;
	  sMSAddressHasBeenSet = true;
	  this.firstName = firstName;
	  firstNameHasBeenSet = true;
	  this.lastName = lastName;
	  lastNameHasBeenSet = true;
	  this.phoneNumber = phoneNumber;
	  phoneNumberHasBeenSet = true;
	  this.department = department;
	  departmentHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  pk = new org.hyperic.hq.authz.shared.AuthzSubjectPK(this.getId());
   }

   //TODO Cloneable is better than this !
   public AuthzSubjectValue( AuthzSubjectValue otherValue )
   {
	  this.sortName = otherValue.sortName;
	  sortNameHasBeenSet = true;
	  this.active = otherValue.active;
	  activeHasBeenSet = true;
	  this.system = otherValue.system;
	  systemHasBeenSet = true;
	  this.authDsn = otherValue.authDsn;
	  authDsnHasBeenSet = true;
	  this.emailAddress = otherValue.emailAddress;
	  emailAddressHasBeenSet = true;
	  this.sMSAddress = otherValue.sMSAddress;
	  sMSAddressHasBeenSet = true;
	  this.firstName = otherValue.firstName;
	  firstNameHasBeenSet = true;
	  this.lastName = otherValue.lastName;
	  lastNameHasBeenSet = true;
	  this.phoneNumber = otherValue.phoneNumber;
	  phoneNumberHasBeenSet = true;
	  this.department = otherValue.department;
	  departmentHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;

	  pk = new org.hyperic.hq.authz.shared.AuthzSubjectPK(this.getId());
   }

   public org.hyperic.hq.authz.shared.AuthzSubjectPK getPrimaryKey()
   {
	  return pk;
   }

   public java.lang.String getSortName()
   {
	  return this.sortName;
   }

   public void setSortName( java.lang.String sortName )
   {
	  this.sortName = sortName;
	  sortNameHasBeenSet = true;

   }

   public boolean sortNameHasBeenSet(){
	  return sortNameHasBeenSet;
   }
   public boolean getActive()
   {
	  return this.active;
   }

   public void setActive( boolean active )
   {
	  this.active = active;
	  activeHasBeenSet = true;

   }

   public boolean activeHasBeenSet(){
	  return activeHasBeenSet;
   }
   public boolean getSystem()
   {
	  return this.system;
   }

   public void setSystem( boolean system )
   {
	  this.system = system;
	  systemHasBeenSet = true;

   }

   public boolean systemHasBeenSet(){
	  return systemHasBeenSet;
   }
   public String getAuthDsn()
   {
	  return this.authDsn;
   }

   public void setAuthDsn( String authDsn )
   {
	  this.authDsn = authDsn;
	  authDsnHasBeenSet = true;

   }

   public boolean authDsnHasBeenSet(){
	  return authDsnHasBeenSet;
   }
   public String getEmailAddress()
   {
	  return this.emailAddress;
   }

   public void setEmailAddress( String emailAddress )
   {
	  this.emailAddress = emailAddress;
	  emailAddressHasBeenSet = true;

   }

   public boolean emailAddressHasBeenSet(){
	  return emailAddressHasBeenSet;
   }
   public String getSMSAddress()
   {
	  return this.sMSAddress;
   }

   public void setSMSAddress( String sMSAddress )
   {
	  this.sMSAddress = sMSAddress;
	  sMSAddressHasBeenSet = true;

   }

   public boolean sMSAddressHasBeenSet(){
	  return sMSAddressHasBeenSet;
   }
   public String getFirstName()
   {
	  return this.firstName;
   }

   public void setFirstName( String firstName )
   {
	  this.firstName = firstName;
	  firstNameHasBeenSet = true;

   }

   public boolean firstNameHasBeenSet(){
	  return firstNameHasBeenSet;
   }
   public String getLastName()
   {
	  return this.lastName;
   }

   public void setLastName( String lastName )
   {
	  this.lastName = lastName;
	  lastNameHasBeenSet = true;

   }

   public boolean lastNameHasBeenSet(){
	  return lastNameHasBeenSet;
   }
   public String getPhoneNumber()
   {
	  return this.phoneNumber;
   }

   public void setPhoneNumber( String phoneNumber )
   {
	  this.phoneNumber = phoneNumber;
	  phoneNumberHasBeenSet = true;

   }

   public boolean phoneNumberHasBeenSet(){
	  return phoneNumberHasBeenSet;
   }
   public String getDepartment()
   {
	  return this.department;
   }

   public void setDepartment( String department )
   {
	  this.department = department;
	  departmentHasBeenSet = true;

   }

   public boolean departmentHasBeenSet(){
	  return departmentHasBeenSet;
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

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("sortName=" + getSortName() + " " + "active=" + getActive() + " " + "system=" + getSystem() + " " + "authDsn=" + getAuthDsn() + " " + "emailAddress=" + getEmailAddress() + " " + "sMSAddress=" + getSMSAddress() + " " + "firstName=" + getFirstName() + " " + "lastName=" + getLastName() + " " + "phoneNumber=" + getPhoneNumber() + " " + "department=" + getDepartment() + " " + "name=" + getName() + " " + "id=" + getId());
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
	  if (other instanceof AuthzSubjectValue)
	  {
		 AuthzSubjectValue that = (AuthzSubjectValue) other;
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
	  if (other instanceof AuthzSubjectValue)
	  {
		 AuthzSubjectValue that = (AuthzSubjectValue) other;
		 boolean lEquals = true;
		 if( this.sortName == null )
		 {
			lEquals = lEquals && ( that.sortName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.sortName.equals( that.sortName );
		 }
		 lEquals = lEquals && this.active == that.active;
		 lEquals = lEquals && this.system == that.system;
		 if( this.authDsn == null )
		 {
			lEquals = lEquals && ( that.authDsn == null );
		 }
		 else
		 {
			lEquals = lEquals && this.authDsn.equals( that.authDsn );
		 }
		 if( this.emailAddress == null )
		 {
			lEquals = lEquals && ( that.emailAddress == null );
		 }
		 else
		 {
			lEquals = lEquals && this.emailAddress.equals( that.emailAddress );
		 }
		 if( this.sMSAddress == null )
		 {
			lEquals = lEquals && ( that.sMSAddress == null );
		 }
		 else
		 {
			lEquals = lEquals && this.sMSAddress.equals( that.sMSAddress );
		 }
		 if( this.firstName == null )
		 {
			lEquals = lEquals && ( that.firstName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.firstName.equals( that.firstName );
		 }
		 if( this.lastName == null )
		 {
			lEquals = lEquals && ( that.lastName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.lastName.equals( that.lastName );
		 }
		 if( this.phoneNumber == null )
		 {
			lEquals = lEquals && ( that.phoneNumber == null );
		 }
		 else
		 {
			lEquals = lEquals && this.phoneNumber.equals( that.phoneNumber );
		 }
		 if( this.department == null )
		 {
			lEquals = lEquals && ( that.department == null );
		 }
		 else
		 {
			lEquals = lEquals && this.department.equals( that.department );
		 }
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
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
      result = 37*result + ((this.sortName != null) ? this.sortName.hashCode() : 0);

      result = 37*result + (active ? 0 : 1);

      result = 37*result + (system ? 0 : 1);

      result = 37*result + ((this.authDsn != null) ? this.authDsn.hashCode() : 0);

      result = 37*result + ((this.emailAddress != null) ? this.emailAddress.hashCode() : 0);

      result = 37*result + ((this.sMSAddress != null) ? this.sMSAddress.hashCode() : 0);

      result = 37*result + ((this.firstName != null) ? this.firstName.hashCode() : 0);

      result = 37*result + ((this.lastName != null) ? this.lastName.hashCode() : 0);

      result = 37*result + ((this.phoneNumber != null) ? this.phoneNumber.hashCode() : 0);

      result = 37*result + ((this.department != null) ? this.department.hashCode() : 0);

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

	  return result;
   }

}
