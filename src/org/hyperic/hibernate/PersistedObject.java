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

import java.io.Serializable;

import org.hyperic.dao.DAOFactory;

/**
 * Base class for all HQ persisted objects.
 * 
 * Some of these methods are marked as protected.  This allows Hibernate to
 * pull & set values (due to its fancy runtime subclassing), but also 
 * restricts other rogue objects from doing bad things like setting the ID
 * & version #.
 */
public abstract class PersistedObject 
    implements Serializable
{
    private Integer _id;

    // for hibernate optimistic locks -- don't mess with this.
    // Named ugly-style since we already use VERSION in some of our tables.
    private long    _version_;

    // XXX -- This is public for now, but should be made more private later
    public void setId(Integer id) {
        _id = id;
    }

    public Integer getId() {
        return _id;
    }

    public long get_version_() {
        return _version_;
    }

    protected void set_version_(long newVer) {
        _version_ = newVer;
    }

    /**
     * We provide this method to the Hibernate objects, in order to allow them
     * to populate their IDs when new objects are created.  Should generally
     * not be used.
     */
    protected void save(PersistedObject o) {
        DAOFactory.getDAOFactory().getCurrentSession().save(o);
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof PersistedObject)) {
            return false;
        }
        PersistedObject o = (PersistedObject)obj;
        return _id == o.getId() ||
               (_id != null && o.getId() != null && _id.equals(o.getId()));
    }

    public int hashCode() {
        int result = 17;

        result = 37*result + (_id != null ? _id.hashCode() : 0);

        return result;
    }
}
