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

package org.hyperic.hq.authz.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.common.server.session.Calendar;

public class RoleCalendar
    extends PersistedObject
{
    private Role             _role;
    private Calendar         _calendar;
    private RoleCalendarType _type;
    
    protected RoleCalendar() {}
    
    RoleCalendar(Role role, Calendar calendar, RoleCalendarType type) {
        _role     = role;
        _calendar = calendar;
        _type     = type;
    }
    
    public Role getRole() {
        return _role;
    }
    
    protected void setRole(Role r) {
        _role = r;
    }
    
    public Calendar getCalendar() {
        return _calendar;
    }
    
    protected void setCalendar(Calendar c) {
        _calendar = c;
    }
    
    public RoleCalendarType getType() {
        return _type;
    }
    
    protected int getTypeEnum() {
        return _type.getCode();
    }
    
    protected void setTypeEnum(int type) {
        _type = RoleCalendarType.findByCode(type);
    }
    
    public String toString() {
        return "RoleCalendar[role=" + getRole().getName() + " cal=" + 
               getCalendar().getName() + " type=" + getType() + "]";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof RoleCalendar)) {
            return false;
        }

        RoleCalendar o = (RoleCalendar)obj;
        
        return o.getRole().equals(getRole()) && 
               o.getCalendar().equals(getCalendar()) &&
               o.getType().equals(getType());
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getRole().hashCode();
        result = 37 * result + getCalendar().hashCode();
        result = 37 * result + getType().hashCode();

        return result;
    }
}
