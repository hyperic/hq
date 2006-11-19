/**
 *
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
