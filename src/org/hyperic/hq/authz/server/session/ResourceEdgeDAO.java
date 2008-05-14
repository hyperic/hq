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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.dao.HibernateDAO;

public class ResourceEdgeDAO 
    extends HibernateDAO
{
    ResourceEdgeDAO(DAOFactory f) { 
        super(ResourceEdge.class, f);
    }

    ResourceEdge create(Resource from, Resource to, int distance,
                        ResourceRelation relation) 
    {
        ResourceEdge res = new ResourceEdge(from, to, distance, relation);
        
        save(res);
        return res;
    }
    
    Map findDescendantMap(Resource r) {
        String sql = "select e.to, e.distance from ResourceEdge e " +
                     "where e.from = :from " + 
                     "and distance > :distance";
                     
        List vals = getSession().createQuery(sql)
                                .setParameter("from", r)
                                .setInteger("distance", 0)
                                .list();
        return convertDistanceListToMap(vals);
    }
    
    Map convertDistanceListToMap(List vals) {
        Map res = new HashMap();
        
        for (Iterator i=vals.iterator(); i.hasNext(); ) {
            Object[] ent = (Object[])i.next();
            Resource r = (Resource)ent[0];
            Integer distance = (Integer)ent[1];
            Collection c = (Collection)res.get(distance);
            
            if (c == null) {
                c = new ArrayList();
                res.put(distance, c);
            }
            c.add(r);
        }
        return res;
    }
    
    Collection findDescendantEdges(Resource r) {
        String sql = "from ResourceEdge e where e.from = :from " + 
                     "and distance > :distance";

        return getSession().createQuery(sql)
                .setParameter("from", r)
                .setInteger("distance", 0)
                .list();
    }

    Collection findAncestorEdges(Resource r) {
        String sql = "from ResourceEdge e where e.from = :from " + 
                     "and distance < :distance";

        return getSession().createQuery(sql)
                .setParameter("from", r)
                .setInteger("distance", 0)
                .list();
    }
    
    void deleteEdges(Resource r) {
        String sql = "delete ResourceEdge where to_id = :to or from_id = :from)";
        getSession().createQuery(sql)
                    .setParameter("to", r)
                    .setParameter("from", r)
                    .executeUpdate();
    }
}
