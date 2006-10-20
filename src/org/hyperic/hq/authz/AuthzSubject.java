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

package org.hyperic.hq.authz;

import java.util.Collection;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

public class AuthzSubject extends AuthzNamedEntity
    implements java.io.Serializable {

    private String dsn;
     private Integer cid;
     private String sortName;
     private String firstName;
     private String lastName;
     private String emailAddress;
     private String smsAddress;
     private String phoneNumber;
     private String department;
     private boolean factive;
     private boolean fsystem;
     private Resource resourceId;
     private Collection roles;
     private Collection userConfigs;
     
     private AuthzSubjectValue authzSubjectValue = new AuthzSubjectValue();

     // Constructors

    /** default constructor */
    public AuthzSubject() {
    }

	/** minimal constructor */
    public AuthzSubject(AuthzSubjectValue val) {
        setAuthzSubjectValue(val);
    }
    
    /** full constructor */
    public AuthzSubject(String name, String dsn, Integer cid, String sortName,
                        String firstName, String lastName, String emailAddress,
                        String smsAddress, String phoneNumber,
                        String department, boolean factive, boolean fsystem,
                        Resource resourceId, Collection roles,
                        Collection userConfigs) {
        super(name);
        this.dsn = dsn;
        this.cid = cid;
        this.sortName = sortName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
        this.smsAddress = smsAddress;
        this.phoneNumber = phoneNumber;
        this.department = department;
        this.factive = factive;
        this.fsystem = fsystem;
        this.resourceId = resourceId;
        this.roles = roles;
        this.userConfigs = userConfigs;
    }
       
    public String getDsn() {
        return this.dsn;
    }
    
    public void setDsn(String dsn) {
        this.dsn = dsn;
    }
    public Integer getCid() {
        return this.cid;
    }
    
    public void setCid(Integer cid) {
        this.cid = cid;
    }
    public String getSortName() {
        return this.sortName;
    }
    
    public void setSortName(String sortName) {
        this.sortName = sortName;
    }
    public String getFirstName() {
        return this.firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return this.lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getEmailAddress() {
        return this.emailAddress;
    }
    
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    public String getSmsAddress() {
        return this.smsAddress;
    }
    
    public void setSmsAddress(String smsAddress) {
        this.smsAddress = smsAddress;
    }
    public String getPhoneNumber() {
        return this.phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public String getDepartment() {
        return this.department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    public boolean isFactive() {
        return this.factive;
    }
    
    public void setFactive(boolean factive) {
        this.factive = factive;
    }
    public boolean isFsystem() {
        return this.fsystem;
    }
    
    public void setFsystem(boolean fsystem) {
        this.fsystem = fsystem;
    }
    public Resource getResourceId() {
        return this.resourceId;
    }
    
    public void setResourceId(Resource resourceId) {
        this.resourceId = resourceId;
    }
    public Collection getRoles() {
        return this.roles;
    }
    
    public void setRoles(Collection roles) {
        this.roles = roles;
    }
    public Collection getUserConfigs() {
        return this.userConfigs;
    }
    
    public void setUserConfigs(Collection userConfigs) {
        this.userConfigs = userConfigs;
    }

    public AuthzSubjectValue getAuthzSubjectValue() {
        authzSubjectValue.setActive(isFactive());
        authzSubjectValue.setAuthDsn(getDsn());
        authzSubjectValue.setDepartment(getDepartment());
        authzSubjectValue.setEmailAddress(getEmailAddress());
        authzSubjectValue.setFirstName(getFirstName());
        authzSubjectValue.setId(getId());
        authzSubjectValue.setName(getName());
        authzSubjectValue.setPhoneNumber(getPhoneNumber());
        authzSubjectValue.setSMSAddress(getSmsAddress());
        authzSubjectValue.setSortName(getSortName());
        authzSubjectValue.setSystem(isFsystem());
        
        return authzSubjectValue;
    }

    public void setAuthzSubjectValue(AuthzSubjectValue authzSubjectValue) {
        setFactive(authzSubjectValue.getActive());
        setDsn(authzSubjectValue.getAuthDsn());
        setDepartment(authzSubjectValue.getDepartment());
        setEmailAddress(authzSubjectValue.getEmailAddress());
        setFirstName(authzSubjectValue.getFirstName());
        setId(authzSubjectValue.getId());
        setName(authzSubjectValue.getName());
        setPhoneNumber(authzSubjectValue.getPhoneNumber());
        setSmsAddress(authzSubjectValue.getSMSAddress());
        setSortName(authzSubjectValue.getSortName());
        setFsystem(authzSubjectValue.getSystem());        
    }
    
    public boolean isRoot() {
        return getId().equals(AuthzConstants.rootSubjectId);
    }

    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof AuthzSubject) ) return false;
        AuthzSubject castOther = ( AuthzSubject ) other; 
         
        return ( (this.getName()==castOther.getName()) || ( this.getName()!=null && castOther.getName()!=null && this.getName().equals(castOther.getName()) ) )
 && ( (this.getDsn()==castOther.getDsn()) || ( this.getDsn()!=null && castOther.getDsn()!=null && this.getDsn().equals(castOther.getDsn()) ) );
   }
   
    public int hashCode() {
        int result = 17;
        return result;
    }
}


