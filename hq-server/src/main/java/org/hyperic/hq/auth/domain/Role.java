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

import java.io.Serializable;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "EAM_ROLE")
public class Role implements Serializable {

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<RoleCalendar> calendars;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "SORT_NAME", length = 100)
    private String sortName;

    @ManyToMany
    @JoinTable(name = "EAM_SUBJECT_ROLE_MAP", joinColumns = { @JoinColumn(name = "ROLE_ID") }, inverseJoinColumns = { @JoinColumn(name = "SUBJECT_ID") })
    private Collection<AuthzSubject> subjects;

    @Column(name = "FSYSTEM")
    private boolean system;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    public Role() {
        super();
    }

    public void addCalendar(RoleCalendar c) {
        getCalendarBag().add(c);
    }

    public void clearCalendars() {
        getCalendarBag().clear();
    }

    public void clearSubjects() {
        for (AuthzSubject s : getSubjects()) {
            s.removeRole(this);
        }
        getSubjects().clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Role other = (Role) obj;
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

    Collection<RoleCalendar> getCalendarBag() {
        return calendars;
    }

    public Collection<RoleCalendar> getCalendars() {
        return Collections.unmodifiableCollection(calendars);
    }

    /**
     * Get a collection of {@link Calendar}s of the specified type for the role.
     */
    public Collection<Calendar> getCalendars(RoleCalendarType type) {
        List<Calendar> res = new ArrayList<Calendar>();

        for (RoleCalendar c : getCalendars()) {
            if (c.getType().equals(type))
                res.add(c.getCalendar());
        }
        return res;
    }

    public String getDescription() {
        return description;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // TODO - possibly model allowable operations such as create/modify/delete
    // etc

    // void addOperation(Operation op) {
    // getOperations().add(op);
    // }
    //
    // public Collection<Operation> getOperations() {
    // //TODO graph access
    // return new HashSet<Operation>();
    // }
    //
    // void setOperations(Collection<Operation> val) {
    // //TODO graph access
    // }

    public String getSortName() {
        return sortName;
    }

    public Collection<AuthzSubject> getSubjects() {
        return subjects;
    }

    public long getVersion() {
        return version != null ? version.longValue() : 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public boolean isSystem() {
        return system;
    }

    public boolean removeCalendar(RoleCalendar c) {
        return getCalendarBag().remove(c);
    }

    void setCalendarBag(Collection<RoleCalendar> c) {
        this.calendars = c;
    }

    public void setDescription(String val) {
        this.description = val;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        if (name == null)
            name = "";
        this.name = name;
        setSortName(name);
    }

    public void setSortName(String sortName) {
        this.sortName = sortName != null ? sortName.toUpperCase() : null;
    }

    public void setSubjects(Collection<AuthzSubject> val) {
        this.subjects = val;
    }

    public void setSystem(boolean fsystem) {
        this.system = fsystem;
    }

    protected void setVersion(Long newVer) {
        version = newVer;
    }

}
