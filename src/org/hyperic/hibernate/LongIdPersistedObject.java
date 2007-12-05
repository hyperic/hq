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

/**
 * Base class for HQ persisted objects with Long value ids.
 * 
 * @see PersistedObject
 */
public abstract class LongIdPersistedObject 
    implements Serializable {
    
    private final LogicalIdentityHelper idHelper = new LogicalIdentityHelper();
    
    private Long _id;

    // for hibernate optimistic locks -- don't mess with this.
    // Named ugly-style since we already use VERSION in some of our tables.
    // really need to use Long instead of primitive value
    // because the database column can allow null version values.
    // The version column IS NULLABLE for migrated schemas. e.g. HQ upgrade
    // from 2.7.5.
    private Long    _version_;
    
    protected void setId(Long id) {
        _id = id;
    }

    public Long getId() {
        return _id;
    }

    public long get_version_() {
        return _version_ != null ? _version_.longValue() : 0;
    }

    protected void set_version_(Long newVer) {
        _version_ = newVer;
    }
    
    public boolean equals(Object obj) {
        return idHelper.equals(this, obj);
    }

    public int hashCode() {
        return idHelper.hashCode(this);
    }  
    
}
