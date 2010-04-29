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

package org.hyperic.hq.authz.server.session;

import org.hyperic.hibernate.PersistedObject;

public class GroupMember
    extends PersistedObject
{
    private ResourceGroup _group;
    private Resource      _resource;
    private long          _entryTime;
    
    protected GroupMember() {
    }

    GroupMember(ResourceGroup group, Resource r) {
        _group     = group;
        _resource  = r;
        _entryTime = System.currentTimeMillis();
    }

    public long getEntryTime() {
        return _entryTime;
    }
    
    protected void setEntryTime(long t) {
        _entryTime = t;
    }
    
    public ResourceGroup getGroup() {
        return _group;
    }
    
    protected void setGroup(ResourceGroup g) {
        _group = g;
    }
    
    public Resource getResource() {
        return _resource;
    }
    
    protected void setResource(Resource r) {
        _resource = r;
    }

    public int hashCode() {
        int result = 17;
        
        result = 37*result + _group.hashCode();
        result = 37*result + _resource.hashCode();

        return result;        
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        
        if (obj instanceof GroupMember == false)
            return false;
        
        GroupMember o = (GroupMember)obj;
        return o.getGroup().equals(getGroup()) &&
            o.getResource().equals(getResource());
    }
}
