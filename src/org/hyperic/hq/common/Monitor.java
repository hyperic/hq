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

package org.hyperic.hq.common;

import java.util.HashMap;

/**
 * Utility class with a singleton object which allows callers to 
 * set/get/incr/decr values within a hash.  Used for monitoring HQ.
 */
public class Monitor {
    private static final Monitor instance = new Monitor();

    private HashMap vals;

    private Monitor(){
        this.vals = new HashMap();
    }

    /**
     * Get the instance of the Monitor for the current classloader.
     */
    public static Monitor getInstance(){
        return Monitor.instance;
    }

    public HashMap getAllValues(){
        synchronized(this.vals){
            return (HashMap)this.vals.clone();
        }
    }


    /**
     * Get a value for the specified key.
     *
     * @param key Key to get the value for
     * 
     * @return The Double value which matches the key -- else null if
     *         it has not yet been set.
     */
    public Double getValue(String key){
        synchronized(this.vals){
            return (Double)this.vals.get(key);
        }
    }

    /**
     * Set a value for the specified key.
     *
     * @param key Key to set the value for
     * @param val Value to set
     */
    public void setValue(String key, double val){
        synchronized(this.vals){
            this.vals.put(key, new Double(val));
        }
    }

    /**
     * Set a value for the specified key.
     *
     * @param key Key to set the value for
     * @param val Value to set -- cannot be null
     */
    public void setValue(String key, Double val){
        if(val == null){
            throw new IllegalArgumentException("Value cannot be null");
        }

        synchronized(this.vals){
            this.vals.put(key, val);
        }
    }

    /**
     * Atomically increment the value associated with the passed key.
     * If the key cannot be found, it is assumed to be 0
     *
     * @param key Key to increment the value of
     */
    public void incrementValue(String key){
        Double val;
        double v;

        synchronized(this.vals){
            if((val = (Double)this.vals.get(key)) == null){
                v = 0;
            } else {
                v = val.doubleValue();
            }

            this.vals.put(key, new Double(v + 1));
        }
    }

    /**
     * Atomically decrement the value associated with the passed key.
     * If the key cannot be found, it is assumed to be 0
     *
     * @param key Key to decrement the value of
     */
    public void decrementValue(String key){
        Double val;
        double v;

        synchronized(this.vals){
            if((val = (Double)this.vals.get(key)) == null){
                v = 0;
            } else {
                v = val.doubleValue();
            }

            this.vals.put(key, new Double(v - 1));
        }
    }
}
