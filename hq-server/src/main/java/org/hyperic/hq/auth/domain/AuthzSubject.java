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

package org.hyperic.hq.auth.domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OptimisticLock;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.inventory.domain.Resource;

@Entity
@Table(name = "EAM_SUBJECT", uniqueConstraints = { @UniqueConstraint(columnNames = { "NAME", "DSN" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AuthzSubject implements Serializable {

    @Column(name = "FACTIVE", nullable = false)
    private boolean active;

    @Column(name = "DEPARTMENT", length = 100)
    private String department;

    @Column(name = "DSN", length = 100, nullable = false)
    private String dsn;

    @Column(name = "EMAIL_ADDRESS", length = 100)
    private String emailAddress;

    @Column(name = "FIRST_NAME", length = 100)
    private String firstName;

    @Column(name = "HTML_EMAIL", nullable = false)
    private boolean htmlEmail;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "LAST_NAME", length = 100)
    private String lastName;

    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "PHONE_NUMBER", length = 100)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PREF_CRISPO_ID")
    @Index(name = "PREF_CRISPO_ID_IDX")
    private Crispo prefs;

    @OneToMany(fetch=FetchType.EAGER)
    @OptimisticLock(excluded = true)
    @JoinTable(name = "OWNED_RESOURCES", joinColumns = { @JoinColumn(name = "SUBJECT_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_ID") })
    private Set<Resource> ownedResources = new HashSet<Resource>();

    @ManyToMany(fetch = FetchType.LAZY)
    @OptimisticLock(excluded = true)
    @JoinTable(name = "EAM_SUBJECT_ROLE_MAP", joinColumns = { @JoinColumn(name = "SUBJECT_ID") }, inverseJoinColumns = { @JoinColumn(name = "ROLE_ID") })
    private Set<Role> roles;

    @Column(name = "SMS_ADDRESS", length = 100)
    private String smsAddress;

    @Column(name = "SORT_NAME", length = 100)
    private String sortName;

    @Column(name = "FSYSTEM", nullable = false)
    private boolean system;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected AuthzSubject() {
    }

    public AuthzSubject(boolean active, String authDsn, String dept, String email, boolean useHtml,
                        String first, String last, String name, String phone, String sms,
                        boolean system) {
        setActive(active);
        setAuthDsn(authDsn);
        setDepartment(dept);
        setEmailAddress(email);
        setHtmlEmail(useHtml);
        setFirstName(first);
        setLastName(last);
        setName(name);
        setPhoneNumber(phone);
        setSMSAddress(sms);
        setSystem(system);
    }

    public AuthzSubject(Integer id) {
        this.id = id;
    }

    public void addOwnedResource(Resource resource) {
        ownedResources.add(resource);
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AuthzSubject other = (AuthzSubject) obj;
        if (dsn == null) {
            if (other.dsn != null)
                return false;
        } else if (!dsn.equals(other.dsn))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public boolean getActive() {
        return isActive();
    }

    public String getAuthDsn() {
        return dsn;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    public boolean getHtmlEmail() {
        return isHtmlEmail();
    }

    public Integer getId() {
        return id;
    }

    public String getLastName() {
        return lastName;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Crispo getPrefs() {
        return prefs;
    }

    public Set<Resource> getOwnedResources() {
        return ownedResources;
    }

    public Collection<Role> getRoles() {
        return roles;
    }

    public String getSMSAddress() {
        return smsAddress;
    }

    public String getSortName() {
        return sortName;
    }

    public boolean getSystem() {
        return isSystem();
    }

    public long getVersion() {
        return version != null ? version.longValue() : 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dsn == null) ? 0 : dsn.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isHtmlEmail() {
        return htmlEmail;
    }

    public boolean isRoot() {
        return getId().equals(AuthzConstants.rootSubjectId);
    }

    public boolean isSystem() {
        return system;
    }

    public void removeAllRoles() {
        roles.clear();
    }

    public void removeOwnedResource(Resource resource) {
        ownedResources.remove(resource);
    }

    public void removeRole(Role role) {
        roles.remove(role);
    }

    public void setActive(boolean val) {
        active = val;
    }

    public void setAuthDsn(String val) {
        dsn = val;
    }

    public void setDepartment(String val) {
        department = val;
    }

    public void setEmailAddress(String val) {
        emailAddress = val;
    }

    public void setFirstName(String val) {
        firstName = val;
    }

    public void setHtmlEmail(boolean useHtml) {
        htmlEmail = useHtml;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLastName(String val) {
        lastName = val;
    }

    public void setName(String name) {
        if (name == null)
            name = "";
        this.name = name;
        setSortName(name);
    }

    public void setPhoneNumber(String val) {
        phoneNumber = val;
    }

    public void setPrefs(Crispo c) {
        prefs = c;
    }

    public void setRoles(Set<Role> val) {
        roles = val;
    }

    public void setSMSAddress(String val) {
        smsAddress = val;
    }

    public void setSortName(String sortName) {
        this.sortName = sortName != null ? sortName.toUpperCase() : null;
    }

    public void setSystem(boolean val) {
        system = val;
    }

    protected void setVersion_(Long newVer) {
        version = newVer;
    }

}
