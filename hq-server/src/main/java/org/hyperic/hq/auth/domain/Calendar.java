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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(name = "EAM_CALENDAR", uniqueConstraints = { @UniqueConstraint(name = "calendar_name_idx", columnNames = { "NAME" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Calendar {
    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_CALENDAR_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "VERSION_COL",nullable=false)
    @Version
    private Long version;

    @Column(name = "NAME", nullable = false)
    private String name;

    @OneToMany(cascade = { CascadeType.ALL }, mappedBy = "calendar", orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Collection<CalendarEntry> entries = new ArrayList<CalendarEntry>();

    protected Calendar() {
    }

    public Calendar(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public Collection<CalendarEntry> getEntries() {
        return Collections.unmodifiableCollection(entries);
    }

    public Collection<CalendarEntry> getEntriesBag() {
        return entries;
    }

    public boolean removeEntry(CalendarEntry ent) {
        return getEntriesBag().remove(ent);
    }

    protected void setEntriesBag(Collection<CalendarEntry> entries) {
        this.entries = entries;
    }

    public WeekEntry addWeekEntry(int weekDay, int startTime, int endTime) {
        WeekEntry res = new WeekEntry(this, weekDay, startTime, endTime);

        getEntriesBag().add(res);
        return res;
    }

    public boolean containsTime(long time) {
        for (CalendarEntry ent : getEntries()) {
            if (ent.containsTime(time)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "Calendar[" + getName() + "]";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Calendar)) {
            return false;
        }
        Calendar o = (Calendar) obj;

        return o.getName().equals(getName());
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + name.hashCode();

        return result;
    }
}
