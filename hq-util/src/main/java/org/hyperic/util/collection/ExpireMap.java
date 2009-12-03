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

/*
 * Created on Jul 17, 2003
 *
 * An extension of HashMap whose entries expire
 */
package org.hyperic.util.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ExpireMap extends HashMap {

    /**
     *
     * This is the class used as value to the ExpireMap class
     */
    public class Value {
        private Object value;
        private long expiration;
        
        /**
         * No expiration constructor
         * @param value the actual value
         */
        public Value(Object value) {
            // Default expiration in 4 minutes
            this(value, 4 * 60 * 1000);
        }

        /**
         * Constructor with expiration (offset from current time)
         * @param value the actual value
         * @param expiration the offset from current time
         */
        public Value(Object value, long expiration) {
            this.value = value;
            this.expiration = System.currentTimeMillis() + expiration;
        }

        /**
         * Check if key has expired
         * @return true if key has expired
         */
        public boolean hasExpired() {
            return expiration < System.currentTimeMillis();
        }
        
        /**
         * @return the expiration time
         */
        public long getExpiration() {
            return expiration;
        }

        /**
         * @param l the new expiration time
         */
        public void setExpiration(long l) {
            expiration = l;
        }

        /**
         * @return the value object
         */
        public Object getValue() {
            return value;
        }

        /**
         * @param object the new value object
         */
        public void setValue(Object object) {
            value = object;
        }

    }
    
    /* This function should not be called directly, as the values
     * returned are Value objects
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        // Throw out the expired value objects
        Set entries = super.entrySet();
        for (Iterator it = entries.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Value value = (Value) entry.getValue();
            if (value.hasExpired()) {
                it.remove();
            }
        }
        return entries;
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object key) {
        Value value = (Value) super.get(key);
        if (value != null) {
            if (value.hasExpired()) {
                super.remove(key);
            }
            else {
                return value.getValue();
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return (this.get(key) != null);
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        this.entrySet();    // Clean out the expired values
        return super.keySet();
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection values() {
        Set entries = this.entrySet();    // Clean out the expired values
        ArrayList values = new ArrayList();
        for (Iterator it = entries.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Value value = (Value) entry.getValue();
            values.add(value.getValue());
        }
        return values;
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object key, Object value) {
        // Create a new value with default expiration of 4 minutes from now
        return super.put(key, new Value(value));
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object key, Object value, long expiration) {
        return super.put(key, new Value(value, expiration));
    }
    
    // Test cases
    public static void main(String[] args) throws Exception {
        // Add an object to expire in a couple of seconds
        ExpireMap map = new ExpireMap();

        map.put("key", "object", 2000);

        Thread.sleep(1000);
        System.out.println("1 sec: Map contains key: " + map.containsKey("key"));

        Thread.sleep(1000);
        System.out.println("2 sec: Map contains key: " + map.containsKey("key"));

        // Make sure the value object is converted correctly
        map.put("key", "object");
        Object obj = map.get("key");
        System.out.println("Objects are equal: " + obj.equals("object"));
    }

    /**
     * Get an instance of ExpireMap which is backed by a
     * synchronized map
     */
    public static ExpireMap getSynchronizedCache() {
        return (ExpireMap)Collections.synchronizedMap(
                    new ExpireMap());
    }
}
