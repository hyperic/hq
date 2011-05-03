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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Parameter;

@Entity
@Table(name = "EAM_CALENDAR_ENT")
@Inheritance(strategy = InheritanceType.JOINED)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public abstract class CalendarEntry {
    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_CALENDAR_ENT_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "VERSION_COL",nullable=false)
    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "CALENDAR_ID", nullable = false)
    @Index(name = "CALENDAR_ID_IDX")
    private Calendar calendar;

    protected CalendarEntry() {
    }

    CalendarEntry(Calendar c) {
        calendar = c;
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

    public Calendar getCalendar() {
        return calendar;
    }

    protected void setCalendar(Calendar c) {
        calendar = c;
    }

    public abstract boolean containsTime(long time);

    public int hashCode() {
        int result = 17;

        result = 37 * result + super.hashCode();
        result = 37 * result + calendar.hashCode();
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || obj instanceof CalendarEntry == false)
            return false;

        CalendarEntry ent = (CalendarEntry) obj;
        return ent.getCalendar().equals(calendar) && super.equals(obj);
    }
}
