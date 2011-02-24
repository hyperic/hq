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

import java.util.Collection;
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
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OptimisticLock;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.config.domain.Crispo;
import org.springframework.data.graph.annotation.NodeEntity;

@Entity
@Table(name="EAM_SUBJECT")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@NodeEntity(partial=true)
public class AuthzSubject  {
    
    @Column(name="DSN",length=100,nullable=false)
    private String     dsn;
    
    @Column(name="FIRST_NAME",length=100)
    private String     firstName;
    
    @Column(name="LAST_NAME",length=100)
    private String     lastName;
    
    @Column(name="EMAIL_ADDRESS",length=100)
    private String     emailAddress;
    
    @Column(name="SMS_ADDRESS",length=100)
    private String     smsAddress;
    
    @Column(name="PHONE_NUMBER",length=100)
    private String     phoneNumber;
    
    @Column(name="DEPARTMENT",length=100)
    private String  department;
    
    @Column(name="FACTIVE",nullable=false)
    private boolean    active;
    
    @Column(name="FSYSTEM",nullable=false)
    private boolean    system;
    
    @Column(name="HTML_EMAIL",nullable=false)
    private boolean    htmlEmail;
     
    @ManyToMany(fetch=FetchType.LAZY)
    @OptimisticLock(excluded = true)
    @JoinTable(name="EAM_SUBJECT_ROLE_MAP",joinColumns = {@JoinColumn(name="SUBJECT_ID")},
    inverseJoinColumns = {@JoinColumn(name="ROLE_ID")})
    private Set<Role> roles;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="PREF_CRISPO_ID")
    @Index(name="PREF_CRISPO_ID_IDX")
    private Crispo     prefs;
    
    @Column(name="NAME",length=100,nullable=false)
    private String name;
    
    @Column(name="SORT_NAME",length=100)
    private String sortName;
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL")
    @Version
    private Long version;
    

    protected AuthzSubject() {
    }

    public AuthzSubject(boolean active, String authDsn, String dept,
                        String email, boolean useHtml, String first, 
                        String last, String name, String phone, String sms,
                        boolean system) 
    {
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
        this.id=id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null)
            name = "";
        this.name = name;
        setSortName(name);
    }

    public String getSortName() {
        return sortName;
    }

    public void setSortName(String sortName) {
        this.sortName = sortName != null ? sortName.toUpperCase() : null;
    }

    public String getAuthDsn() {
        return dsn;
    }

    protected void setAuthDsn(String val) {
        dsn = val;
    }

    public String getFirstName() {
        return firstName;
    }

    protected void setFirstName(String val) {
        firstName = val;
    }

    public String getLastName() {
        return lastName;
    }

    protected void setLastName(String val) {
        lastName = val;
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }
    
    public String getEmailAddress() {
        return emailAddress;
    }

    protected void setEmailAddress(String val) {
        emailAddress = val;
    }

    public String getSMSAddress() {
        return smsAddress;
    }

    protected void setSMSAddress(String val) {
        smsAddress = val;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    protected void setPhoneNumber(String val) {
        phoneNumber = val;
    }

    public String getDepartment() {
        return department;
    }

    protected void setDepartment(String val) {
        department = val;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getActive() {
        return isActive();
    }

    protected void setActive(boolean val) {
        active = val;
    }

    public boolean isSystem() {
        return system;
    }

    public boolean getSystem() {
        return isSystem();
    }
    
    public boolean isHtmlEmail() {
        return htmlEmail;
    }
    
    public boolean getHtmlEmail() {
        return isHtmlEmail();
    }

    protected void setHtmlEmail(boolean useHtml) {
        htmlEmail = useHtml;
    }

    protected void setSystem(boolean val) {
        system = val;
    }
    
    public Collection<Role> getRoles() {
        return roles;
    }

    protected void setRoles(Set<Role> val) {
        roles = val;
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public void removeRole(Role role) {
        roles.remove(role);
    }

    public void removeAllRoles() {
        roles.clear();
    }

    public Crispo getPrefs() {
        return prefs;
    }
    
    protected void setPrefs(Crispo c) {
        prefs = c;
    }
    
    public long getVersion() {
        return version != null ? version.longValue() : 0;
    }

    protected void setVersion_(Long newVer) {
        version = newVer;
    }
    
   

    public boolean isRoot() {
        return getId().equals(AuthzConstants.rootSubjectId);
    }

    //TODO had to remove equals and hashCode b/c can't override in NodeBacked
}
