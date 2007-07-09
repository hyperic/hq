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

package org.hyperic.hq.common.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hyperic.hibernate.PersistedObject;

public class Calendar
    extends PersistedObject
{
    private String     _name;
    private Collection _entries = new ArrayList();
    
    protected Calendar() {}
    
    Calendar(String name) {
        _name = name;
    }
    
    public String getName() {
        return _name;
    }
    
    protected void setName(String name) {
        _name = name;
    }
    
    public Collection getEntries() {
        return Collections.unmodifiableCollection(_entries);
    }
    
    protected Collection getEntriesBag() {
        return _entries;
    }
    
    boolean removeEntry(CalendarEntry ent) {
        return getEntriesBag().remove(ent);
    }
    
    protected void setEntriesBag(Collection entries) {
        _entries = entries;
    }
    
    WeekEntry addWeekEntry(int weekDay, int startTime, int endTime) {
        WeekEntry res = new WeekEntry(this, weekDay, startTime, endTime);
        
        getEntriesBag().add(res);
        return res;
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
        Calendar o = (Calendar)obj;
        
        return o.getName().equals(getName());
    }

    public int hashCode() {
        int result = 17;

        result = 37*result + _name.hashCode();

        return result;
    }
}
