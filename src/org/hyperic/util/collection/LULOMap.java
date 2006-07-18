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

package org.hyperic.util.collection;

/** Last used last out map, a size is required for the construction so that
 * we can expire entries
 *
 */
public class LULOMap extends java.util.LinkedHashMap {
    
    /** Holds value of property maxsize. */
    private int maxsize = Integer.MAX_VALUE;
    
    /** Creates a new instance of FIFOHashMap */
    private LULOMap() {
    }
    
    public LULOMap(int size) {
        this.maxsize = size;
    }
    
    protected boolean removeEldestEntry(java.util.Map.Entry eldest) {
        return size() > maxsize;
    }

    /** Overrides LinkedHashMap's get() method to re-insert object when fetched
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object arg) {
        Object obj = super.get(arg);
        if (obj != null)
            this.put(arg, obj);
        return obj;
    }
    
    /** Overrides LinkedHashMap's put() method so that re-inserts will change
     * the insert order
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object arg, Object obj) {
        // First remove it
        Object old = this.remove(arg);
        // Then insert it
        super.put(arg, obj);
        return old;
    }
}

