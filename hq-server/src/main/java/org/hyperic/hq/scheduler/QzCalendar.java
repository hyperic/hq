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

package org.hyperic.hq.scheduler;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name="QRTZ_CALENDARS")
public class QzCalendar  implements Serializable {

    @Id
    @Column(name="CALENDAR_NAME",length=200,nullable=false)
    private String calendarName;
    
    @Basic(fetch=FetchType.LAZY)
    @Lob
    @Column(name="CALENDAR",nullable=false,columnDefinition="BLOB")
    private byte[] calendar;

    public QzCalendar() {
    }

    public String getCalendarName() {
        return calendarName;
    }
    
    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public byte[] getCalendar() {
        return calendar;
    }
    
    public void setCalendar(byte[] calendar) {
        this.calendar = calendar;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((calendarName == null) ? 0 : calendarName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QzCalendar other = (QzCalendar) obj;
        if (calendarName == null) {
            if (other.calendarName != null)
                return false;
        } else if (!calendarName.equals(other.calendarName))
            return false;
        return true;
    }
    
    
}


