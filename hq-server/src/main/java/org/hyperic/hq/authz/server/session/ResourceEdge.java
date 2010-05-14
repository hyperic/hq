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

public class ResourceEdge extends PersistedObject {
    private Resource         _from;
    private Resource         _to;
    private int              _distance;
    private ResourceRelation _relation;

    protected ResourceEdge() {
    }

    ResourceEdge(Resource from, Resource to, int distance, 
                 ResourceRelation relation) 
    {
        _from     = from;
        _to       = to;
        _distance = distance;
        _relation = relation;
    }
    
    public Resource getFrom() {
        return _from;
    }
    
    protected void setFrom(Resource from) {
        _from = from;
    }
    
    public Resource getTo() {
        return _to;
    }
    
    protected void setTo(Resource to) {
        _to = to;
    }
    
    public int getDistance() {
        return _distance;
    }
    
    protected void setDistance(int d) {
        _distance = d;
    }
    
    public ResourceRelation getRelation() {
        return _relation;
    }
    
    protected void setRelation(ResourceRelation r) {
        _relation = r;
    }
    
    public int hashCode() {
        int result = 17;

        result = 37 * result + _from.hashCode();
        result = 37 * result + _to.hashCode();
        result = 37 * result + _distance;
        result = 37 * result + _relation.hashCode();

        return result;
    }
    
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || !(obj instanceof ResourceEdge))
            return false;

        ResourceEdge o = (ResourceEdge)obj;
        
        return o.getFrom().equals(getFrom()) &&
               o.getTo().equals(getTo()) &&
               o.getDistance() == getDistance() &&
               o.getRelation().equals(getRelation());
    }
}
