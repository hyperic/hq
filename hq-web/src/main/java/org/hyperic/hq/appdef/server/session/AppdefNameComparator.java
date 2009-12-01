/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.Comparator;

/**
 * Class compares AppdefResource.getName() and AppdefResourceType.getName()
 */
class AppdefNameComparator implements Comparator {
    final boolean _asc;
    AppdefNameComparator(boolean ascending) {
        _asc = ascending;
    }
    public int compare(Object arg0, Object arg1) {
        if ((!(arg0 instanceof AppdefResource) ||
             !(arg1 instanceof AppdefResource)) &&
            (!(arg0 instanceof AppdefResourceType) ||
             !(arg1 instanceof AppdefResourceType))) {
                throw new ClassCastException();
        }
        String s0, s1;
        if (arg0 instanceof AppdefResource) {
            s0 = ((AppdefResource)arg0).getName();
            s1 = ((AppdefResource)arg1).getName();
        } else {
            s0 = ((AppdefResourceType)arg0).getName();
            s1 = ((AppdefResourceType)arg1).getName();
        }
        return (_asc) ? s0.compareToIgnoreCase(s1): s1.compareToIgnoreCase(s0);
    }
}
