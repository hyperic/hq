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

package org.hyperic.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * The important thing about an ArraySet is that it preserves the
 * insertion order.  If you update this class, please keep that in mind :)
 */
public class ArraySet extends ArrayList implements Set {

    protected TreeSet set = null;

    public ArraySet () {
        super();
    }

    public ArraySet (Comparator c) {
        set = new TreeSet(c);
    }

    public boolean add (Object o) {
        if (set == null) {
            if (contains(o)) return false;
            return super.add(o);
        } else {
            if (set.contains(o)) return false;
            set.add(o);
            return super.add(o);
        }
    }
    public boolean addAll(Collection c) {
        Iterator i = c.iterator();
        boolean changed = false;
        while (i.hasNext()) {
            if (add(i.next())) changed = true;
        }
        return changed;
    }

    public boolean contains (Object o) {
        if (set == null) return super.contains(o);
        return set.contains(o);
    }
    public boolean containsAll(Collection c) {
        Iterator i = c.iterator();
        while (i.hasNext()) {
            if (!contains(i.next())) return false;
        }
        return true;
    }
}
