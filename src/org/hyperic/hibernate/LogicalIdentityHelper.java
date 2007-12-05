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

/**
 * A helper class used to check equality and generate hash codes for persisted 
 * objects that use a numeric primary key as their logical identifier.
 */
class LogicalIdentityHelper {
    
    
    public LogicalIdentityHelper() {
    }
    
    /**
     * Check if the object to check is equal to this object.
     * 
     * @param thisObject This object.
     * @param toCheck The object to check.
     * @return <code>true</code> if the object to check is equal to this object.
     */
    public boolean equals(PersistedObject thisObject, Object toCheck) {
        if (this == toCheck) {
            return true;
        }
        if (toCheck == null || !(toCheck instanceof PersistedObject)) {
            return false;
        }
        PersistedObject o = (PersistedObject)toCheck;
        
        return equals(thisObject.getId(), o.getId());
    }    
    
    /**
     * Check if the object to check is equal to this object.
     * 
     * @param thisObject This object.
     * @param toCheck The object to check.
     * @return <code>true</code> if the object to check is equal to this object.
     */
    public boolean equals(LongIdPersistedObject thisObject, Object toCheck) {
        if (this == toCheck) {
            return true;
        }
        if (toCheck == null || !(toCheck instanceof LongIdPersistedObject)) {
            return false;
        }
        LongIdPersistedObject o = (LongIdPersistedObject)toCheck;
                
        return equals(thisObject.getId(), o.getId());
    }
    
    private boolean equals(Number thisObjectId, Number toCheckObjectId) {
        return thisObjectId == toCheckObjectId ||
               (thisObjectId != null && 
                toCheckObjectId != null && 
                thisObjectId.equals(toCheckObjectId));        
    }
    
    /**
     * Generate a hash code for this object.
     * 
     * @param thisObject This object.
     * @return The hash code for this object.
     */
    public int hashCode(PersistedObject thisObject) {
        return hashCode(thisObject.getId());
    }    

    /**
     * Generate a hash code for this object.
     * 
     * @param thisObject This object.
     * @return The hash code for this object.
     */
    public int hashCode(LongIdPersistedObject thisObject) {
        return hashCode(thisObject.getId());
    }
    
    private int hashCode(Number num) {
        int result = 17;
        
        result = 37*result + (num != null ? num.hashCode() : 0);

        return result;        
    }

}
