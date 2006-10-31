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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

public class AuthzSubject extends AuthzNamedEntity
{

    private String dsn;
    private Integer cid;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private String smsAddress;
    private String phoneNumber;
    private String department;
    private boolean active = true;
    private boolean system = false;
    private Resource resource;
    private Collection roles = new ArrayList();
    private Collection userConfigs = new ArrayList();

    private AuthzSubjectValue authzSubjectValue = new AuthzSubjectValue();
     // Constructors

    /** default constructor */
    public AuthzSubject() {
        super();
    }

	/** minimal constructor */
    public AuthzSubject(AuthzSubjectValue val) {
        setAuthzSubjectValue(val);
    }

    /** full constructor */
    public AuthzSubject(String name, String dsn, Integer cid,
                        String firstName, String lastName, String emailAddress,
                        String smsAddress, String phoneNumber,
                        String department, boolean factive, boolean fsystem,
                        Resource resourceId, Collection roles,
                        Collection userConfigs) {
        super(name);
        this.dsn = dsn;
        this.cid = cid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
        this.smsAddress = smsAddress;
        this.phoneNumber = phoneNumber;
        this.department = department;
        this.active = factive;
        this.system = fsystem;
        this.resource = resourceId;
        this.roles = roles;
        this.userConfigs = userConfigs;
    }

    public String getAuthDsn()
    {
        return dsn;
    }

    public void setAuthDsn(String val)
    {
        dsn = val;
    }

    public Integer getCid()
    {
        return cid;
    }

    public void setCid(Integer val)
    {
        cid = val;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String val)
    {
        firstName = val;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String val)
    {
        lastName = val;
    }

    public String getEmailAddress()
    {
        return emailAddress;
    }

    public void setEmailAddress(String val)
    {
        emailAddress = val;
    }

    public String getSMSAddress()
    {
        return smsAddress;
    }

    public void setSMSAddress(String val)
    {
        smsAddress = val;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String val)
    {
        phoneNumber = val;
    }

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String val)
    {
        department = val;
    }

    public boolean isActive()
    {
        return active;
    }

    public boolean getActive()
    {
        return isActive();
    }

    public void setActive(boolean val)
    {
        active = val;
    }

    public boolean isSystem()
    {
        return system;
    }

    public boolean getSystem()
    {
        return isSystem();
    }

    public void setSystem(boolean val)
    {
        system = val;
    }

    public Resource getResource()
    {
        return resource;
    }

    public void setResource(Resource val)
    {
        resource = val;
    }
    public Collection getRoles() {
        return roles;
    }

    public void setRoles(Collection val)
    {
        roles = val;
    }

    public void addRole(Role role)
    {
        roles.add(role);
    }

    public void removeRole(Role role)
    {
        roles.remove(role);
    }

    public void removeAllRoles()
    {
        roles.clear();
    }

    public Collection getUserConfigs()
    {
        return userConfigs;
    }

    public void setUserConfigs(Collection val)
    {
        userConfigs = val;
    }

    /**
     * @deprecated use (this) AuthzSubject instead
     */
    public AuthzSubjectValue getAuthzSubjectValue()
    {
        authzSubjectValue.setActive(isActive());
        authzSubjectValue.setAuthDsn(getAuthDsn());
        authzSubjectValue.setDepartment(getDepartment());
        authzSubjectValue.setEmailAddress(getEmailAddress());
        authzSubjectValue.setFirstName(getFirstName());
        authzSubjectValue.setId(getId());
        authzSubjectValue.setName(getName());
        authzSubjectValue.setPhoneNumber(getPhoneNumber());
        authzSubjectValue.setSMSAddress(getSMSAddress());
        authzSubjectValue.setSortName(getSortName());
        authzSubjectValue.setSystem(isSystem());

        return authzSubjectValue;
    }

    public void setAuthzSubjectValue(AuthzSubjectValue authzSubjectValue)
    {
        setActive(authzSubjectValue.getActive());
        setAuthDsn(authzSubjectValue.getAuthDsn());
        setDepartment(authzSubjectValue.getDepartment());
        setEmailAddress(authzSubjectValue.getEmailAddress());
        setFirstName(authzSubjectValue.getFirstName());
        setId(authzSubjectValue.getId());
        setName(authzSubjectValue.getName());
        setPhoneNumber(authzSubjectValue.getPhoneNumber());
        setSMSAddress(authzSubjectValue.getSMSAddress());
        setSystem(authzSubjectValue.getSystem());
    }

    public Object getValueObject()
    {
        return getAuthzSubjectValue();
    }

    public boolean isRoot()
    {
        return getId().equals(AuthzConstants.rootSubjectId);
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof AuthzSubject) || !super.equals(obj)) {
            return false;
        }
        AuthzSubject o = (AuthzSubject) obj;
        return
            ((dsn == o.getAuthDsn()) ||
             (dsn != null && o.getAuthDsn() != null &&
              dsn.equals(o.getAuthDsn())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37 * result + (dsn != null ? dsn.hashCode() : 0);

        return result;
    }
}


