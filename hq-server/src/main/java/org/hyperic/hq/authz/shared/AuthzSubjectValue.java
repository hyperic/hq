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

package org.hyperic.hq.authz.shared;

import org.hyperic.hq.authz.server.session.AuthzSubject;

/**
 * Value object for AuthzSubject.
 *
 */
public class AuthzSubjectValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private java.lang.String sortName;
   private boolean active;
   private boolean system;
   private String authDsn;
   private String emailAddress;
   private boolean htmlEmail;
   private String sMSAddress;
   private String firstName;
   private String lastName;
   private String phoneNumber;
   private String department;
   private String name;
   private Integer id;
   private boolean idHasBeenSet;

   public AuthzSubjectValue() {
    }

   public java.lang.String getSortName()
   {
	  return this.sortName;
   }

   public void setSortName( java.lang.String sortName )
   {
	  this.sortName = sortName;

   }

   public boolean getActive()
   {
	  return this.active;
   }

   public void setActive( boolean active )
   {
	  this.active = active;

   }

    public boolean getSystem()
   {
	  return this.system;
   }

   public void setSystem( boolean system )
   {
	  this.system = system;
   }

   public String getAuthDsn()
   {
	  return this.authDsn;
   }

   public void setAuthDsn( String authDsn )
   {
	  this.authDsn = authDsn;
   }

   public String getEmailAddress()
   {
	  return this.emailAddress;
   }

   public void setEmailAddress( String emailAddress )
   {
	  this.emailAddress = emailAddress;
   }

   public boolean isHtmlEmail() {
    return htmlEmail;
}

public void setHtmlEmail(boolean htmlEmail) {
    this.htmlEmail = htmlEmail;
}

public String getSMSAddress()
   {
	  return this.sMSAddress;
   }

   public void setSMSAddress( String sMSAddress )
   {
	  this.sMSAddress = sMSAddress;
   }

   public String getFirstName()
   {
	  return this.firstName;
   }

   public void setFirstName( String firstName )
   {
	  this.firstName = firstName;
   }

   public String getLastName()
   {
	  return this.lastName;
   }

   public void setLastName( String lastName )
   {
	  this.lastName = lastName;
   }

   public String getPhoneNumber()
   {
	  return this.phoneNumber;
   }

   public void setPhoneNumber( String phoneNumber )
   {
	  this.phoneNumber = phoneNumber;
   }

   public String getDepartment()
   {
	  return this.department;
   }

   public void setDepartment( String department )
   {
	  this.department = department;
   }

   public String getName()
   {
	  return this.name;
   }

   public void setName( String name )
   {
	  this.name = name;
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

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("sortName=" + getSortName() + " " + "active=" + getActive() + " " + "system=" + getSystem() + " " + "authDsn=" + getAuthDsn() + " " + "emailAddress=" + getEmailAddress() + " " + "sMSAddress=" + getSMSAddress() + " " + "firstName=" + getFirstName() + " " + "lastName=" + getLastName() + " " + "phoneNumber=" + getPhoneNumber() + " " + "department=" + getDepartment() + " " + "name=" + getName() + " " + "id=" + getId());
	  str.append('}');

	  return(str.toString());
   }


   public AuthzSubject getAuthzSubject() {
       
       AuthzSubject _valueObj = new AuthzSubject(getActive(), getAuthDsn(), getDepartment(), getEmailAddress(), isHtmlEmail(), getFirstName(), getLastName(), getFirstName(), getPhoneNumber(), getSMSAddress(), getSystem());

       return _valueObj;
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
