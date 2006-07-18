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

package org.hyperic.hq.product;

import java.util.Map;
import java.util.Properties;

public class MetricProperties extends Properties {

    private transient int hashCode = -1;
    
    //copied from Properties.java to shutup eclipse warning
    private static final long serialVersionUID = 4112578634029874840L;
    
    public MetricProperties() {
        super();
    }

    public MetricProperties(Properties defaults) {
        super(defaults);
    }

    void setDefaults(Properties defaults) {
        this.hashCode = -1;
        this.defaults = defaults;
    }

    //it is rare plugins would modify the properties
    //but if they do, force re-calcuation of the hashCode
    public Object setProperty(String key, String value) {
        this.hashCode = -1;
        return super.setProperty(key, value);
    }

    public void clear() {
        this.hashCode = -1;
        super.clear();
    }

    public Object put(Object key, Object value) {
        this.hashCode = -1;
        return super.put(key, value);
    }

    public void putAll(Map t) {
        this.hashCode = -1;
        super.putAll(t);
    }

    public Object remove(Object key) {
        this.hashCode = -1;
        return super.remove(key);
    }

    public boolean equals(Object o) {
        //XXX optimize.  see comment below
        return super.equals(o);
    }

    //Collector uses MetricProperties objects
    //as the key into its Map of Collectors
    //since a Metric's Properties shouldn't change,
    //cache the hashCode calcuation.
    public int hashCode() {
        if (this.hashCode == -1) {
            this.hashCode = super.hashCode();
            if ((this.defaults != null) &&
                 (this.defaults.size() != 0))
            {
                this.hashCode ^= this.defaults.hashCode();
            }
        }
        return this.hashCode;
    }
}
