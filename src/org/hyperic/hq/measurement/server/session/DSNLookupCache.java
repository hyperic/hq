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
 * DSNLookupCache.java

 *
 * Created on September 19, 2002, 2:21 PM
 */

package org.hyperic.hq.measurement.server.session;

import org.hyperic.util.collection.FIFOMap;
import java.util.Collections;
import java.util.Map;

/** A static cache singleton object that can keep track of the
 * mapping between DerivedMeasurement ID's, DSN's, and the
 * RawMeasurement ID's.
 */
public class DSNLookupCache {
    
    /** Cache of the lookup results */
    private Map cache = new FIFOMap(300);
    
    /** The singleton */
    private static DSNLookupCache singleton = new DSNLookupCache();

    /** Creates a new instance of DSNLookupCache */
    private DSNLookupCache() {
        // synchronize the map structure since FIFOMap is not sync'd
        cache = Collections.synchronizedMap(cache);
    }
    
    /** Singleton accessor method
     * @return the singleton DSNLookupCache instance
     */
    public static DSNLookupCache getInstance() {
        return DSNLookupCache.singleton;
    }
    
    /** Get the RawMeasurementID if available in cache
     * @param mid the DerivedMeasurement ID
     * @param dsn the DSN string
     * @return the RawMeasurement ID
     */    
    public Integer get(Integer mid, String dsn) {
        // Create the lookup key first
        DSNLookupKey key = new DSNLookupKey(mid, dsn);
        return (Integer) this.cache.get(key);
    }
    
    /** Push the resolved RawMeasurement ID into the cache
     * @param mid the DerivedMeasurement ID
     * @param dsn the DSN string
     * @param rid the RawMeasurement ID
     * @return
     */    
    public Integer put(Integer mid, String dsn, Integer rid) {
        // Create the lookup key first
        DSNLookupKey key = new DSNLookupKey(mid, dsn);
        return (Integer) this.cache.put(key, rid);
    }
    
    /** Create a unique object that contains the DerivedMeasurement ID
     * and the DSN for the lookup key into the cache
     */    
    public class DSNLookupKey {
        
        /** Holds value of property dsn. */
        private String dsn;
        
        /** Holds value of property mid. */
        private Integer mid;
        
        /** Creates a new instance of DSNLookupKey
         * @param mid the DerivedMeasurement ID
         * @param dsn the DSN string
         */
        public DSNLookupKey(Integer mid, String dsn) {
            this.mid = mid;
            this.dsn = dsn;
        }
    
        /** Indicates whether some other object is "equal to" this one.
         * <p>
         * The <code>equals</code> method implements an equivalence relation:
         * <ul>
         * <li>It is <i>reflexive</i>: for any reference value <code>x</code>,
         *     <code>x.equals(x)</code> should return <code>true</code>.
         * <li>It is <i>symmetric</i>: for any reference values <code>x</code> and
         *     <code>y</code>, <code>x.equals(y)</code> should return
         *     <code>true</code> if and only if <code>y.equals(x)</code> returns
         *     <code>true</code>.
         * <li>It is <i>transitive</i>: for any reference values <code>x</code>,
         *     <code>y</code>, and <code>z</code>, if <code>x.equals(y)</code>
         *     returns  <code>true</code> and <code>y.equals(z)</code> returns
         *     <code>true</code>, then <code>x.equals(z)</code> should return
         *     <code>true</code>.
         * <li>It is <i>consistent</i>: for any reference values <code>x</code>
         *     and <code>y</code>, multiple invocations of <tt>x.equals(y)</tt>
         *     consistently return <code>true</code> or consistently return
         *     <code>false</code>, provided no information used in
         *     <code>equals</code> comparisons on the object is modified.
         * <li>For any non-null reference value <code>x</code>,
         *     <code>x.equals(null)</code> should return <code>false</code>.
         * </ul>
         * <p>
         * The <tt>equals</tt> method for class <code>Object</code> implements
         * the most discriminating possible equivalence relation on objects;
         * that is, for any reference values <code>x</code> and <code>y</code>,
         * this method returns <code>true</code> if and only if <code>x</code> and
         * <code>y</code> refer to the same object (<code>x==y</code> has the
         * value <code>true</code>).
         * <p>
         * Note that it is generally necessary to override the <tt>hashCode</tt>
         * method whenever this method is overridden, so as to maintain the
         * general contract for the <tt>hashCode</tt> method, which states
         * that equal objects must have equal hash codes.
         *
         * @param   obj   the reference object with which to compare.
         * @return  <code>true</code> if this object is the same as the obj
         *          argument; <code>false</code> otherwise.
         * @see     #hashCode()
         * @see     java.util.Hashtable
         *
         */
        public boolean equals(Object obj) {
            boolean retValue;
            
            retValue = super.equals(obj);
            
            if (!retValue && (obj instanceof DSNLookupKey)) {
                DSNLookupKey cmp = (DSNLookupKey) obj;
                retValue = this.mid.equals(cmp.getMid()) &&
                           this.dsn.equals(cmp.getDsn());
            }
            
            return retValue;
        }
        
        /** Getter for property dsn.
         * @return Value of property dsn.
         *
         */
        public String getDsn() {
            return this.dsn;
        }
        
        /** Setter for property dsn.
         * @param dsn New value of property dsn.
         *
         */
        public void setDsn(String dsn) {
            this.dsn = dsn;
        }
        
        /** Getter for property mid.
         * @return Value of property mid.
         *
         */
        public Integer getMid() {
            return this.mid;
        }
        
        /** Setter for property mid.
         * @param mid New value of property mid.
         *
         */
        public void setMid(Integer mid) {
            this.mid = mid;
        }

        public int hashCode() {
            int result = 17;
            result = 37*result + ((this.dsn != null) ? this.dsn.hashCode() : 0);
            result = 37*result + ((this.mid != null) ? this.mid.hashCode() : 0);
            return result;
        }
        
    }
    
}
