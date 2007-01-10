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

package org.hyperic.hibernate;

import java.util.LinkedHashMap;
import java.util.Map;

public class LockSet extends LinkedHashMap
{
    private static final int DEFAULT_MAX_SIZE = 0;  // unlimited

    private int maxSize;

    public LockSet()
    {
        this(DEFAULT_MAX_SIZE);
    }

    public LockSet(int maxSize)
    {
        super();
        this.maxSize = maxSize;
    }

    public synchronized Object getLock(Object key)
    {
        Object lock = get(key);
        if (lock == null) {
            lock = new Object();
            put(key, lock);
        }
        return lock;
    }

    public Object getLock(long longVal)
    {
        return getLock(new Long(longVal));
    }

    public Object getLock(int intVal)
    {
        return getLock(new Integer(intVal));
    }

    protected boolean removeEldestEntry(Map.Entry eldest)
    {
        if (maxSize <= 0) {
            return false;
        }
        return size() > maxSize;
    }
}
