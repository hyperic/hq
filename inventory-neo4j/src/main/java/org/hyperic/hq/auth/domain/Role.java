/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.calendar.domain.Calendar;
import org.springframework.data.graph.annotation.NodeEntity;

@NodeEntity(partial=true)
@Entity
@Table(name="EAM_ROLE")
public class Role  {
    
    @Column(name="DESCRIPTION",length=100)
    private String     description;
    
    @Column(name="FSYSTEM")
    private boolean    system;
     
    @ManyToMany
    @JoinTable(name="EAM_SUBJECT_ROLE_MAP",joinColumns = {@JoinColumn(name="ROLE_ID")},
    inverseJoinColumns = {@JoinColumn(name="SUBJECT_ID")})
    private Collection<AuthzSubject> subjects;
    
    @OneToMany(cascade=CascadeType.ALL)
    @JoinTable(name="EAM_ROLE_CALENDARS",joinColumns = {@JoinColumn(name="ROLE_ID")})
    @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
    private Collection<RoleCalendar> calendars;
      
    @Column(name="NAME",length=100,nullable=false,unique=true)
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
    private Long   version;


    public Role() {
        super();
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
    
    public String getDescription() {
        return description;
    }
    
    void setDescription(String val) {
        this.description = val;
    }

    public boolean isSystem() {
        return system;
    }
    
    void setSystem(boolean fsystem) {
        this.system = fsystem;
    }
    //TODO - possibly model allowable operations such as create/modify/delete etc

//    void addOperation(Operation op) {
//        getOperations().add(op);
//    }
//    
//    public Collection<Operation> getOperations() {
//        //TODO graph access
//        return new HashSet<Operation>();
//    }
//    
//    void setOperations(Collection<Operation> val) {
//        //TODO graph access
//    }
    
    public Collection<AuthzSubject> getSubjects() {
        return subjects;
    }
    
    void setSubjects(Collection<AuthzSubject> val) {
        this.subjects = val;
    }
    
    Collection<RoleCalendar> getCalendarBag() {
        return calendars;
    }
    
    void setCalendarBag(Collection<RoleCalendar> c) {
        this.calendars = c;
    }
    
    /**
     * Get a collection of {@link Calendar}s of the specified type for the
     * role.  
     */
    public Collection<Calendar> getCalendars(RoleCalendarType type) {
        List<Calendar> res = new ArrayList<Calendar>();
        
        for (RoleCalendar c : getCalendars()) {
            if (c.getType().equals(type))
                res.add(c.getCalendar());
        }
        return res;
    }
    
    public Collection<RoleCalendar> getCalendars() {
        return Collections.unmodifiableCollection(calendars);
    }
    
    void addCalendar(RoleCalendar c) {
        getCalendarBag().add(c);
    }
    
    void clearCalendars() {
        getCalendarBag().clear();
    }
    
    boolean removeCalendar(RoleCalendar c) {
        return getCalendarBag().remove(c);
    }

    void clearSubjects() {
        for (AuthzSubject s : getSubjects() ) { 
            s.removeRole(this);
        }
        getSubjects().clear();
    }
    
    public long getVersion() {
        return version != null ? version.longValue() : 0;
    }

    protected void setVersion(Long newVer) {
        version = newVer;
    }
}

