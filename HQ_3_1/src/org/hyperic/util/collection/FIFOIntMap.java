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

import java.util.ArrayList;

/**
 *
 * Use the fast IntHashMap as underlying data structure
 */
public class FIFOIntMap extends IntHashMap {
    // Keep an ArrayList to see which value is oldest
    private ArrayList order;
    private int max;
    
    public FIFOIntMap(int size) {
        max = size;
        order = new ArrayList(max);
    }

    public Object put(int key, Object value) {
        if (order.size() >= max) {
            // See if it's already in the map
            if (!this.containsKey(key)) {
                Integer old = (Integer) order.remove(0);
                this.remove(old.intValue());
                order.add(new Integer(key));
            }
        }

        return super.put(key, value);
    }
    
    public Object put(Integer key, Object value) {
        return this.put(key.intValue(), value);
    }
    
    public Object get(Integer key) {
        return this.get(key.intValue());
    }
    
    public Object remove(Integer key) {
        return this.remove(key.intValue());
    }
    
    public boolean containsKey(Integer key) {
        return this.containsKey(key.intValue());
    }
}
