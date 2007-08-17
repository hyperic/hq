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
package org.hyperic.hq.events.server.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.dao.HibernateDAO;

public class EventLogDAO extends HibernateDAO {
    public EventLogDAO(DAOFactory f) {
        super(EventLog.class, f);
    }

    public EventLog findById(Integer id) {
        return (EventLog)super.findById(id);
    }

    EventLog create(EventLog res) {
        save(res);
        return res;
    }

    List findByEntity(AppdefEntityID ent) {
        return findByEntity(ent, "desc");
    }

    List findByEntityOrderTSAsc(AppdefEntityID ent) {
        return findByEntity(ent, "");
    }

    private List findByEntity(AppdefEntityID ent, String order) {
        String sql = "from EventLog l where l.entityType = :eType " +
            "and l.entityId = :eId order by l.timestamp " + order;
    
        return getSession().createQuery(sql)
            .setInteger("eType", ent.getType())
            .setInteger("eId", ent.getID())
            .list();
    }
    
    List findByEntityAndStatus(AppdefEntityID entId, long begin, long end,
                               String status) 
    {
        return createCriteria()
            .add(Expression.eq("entityType", new Integer(entId.getType())))
            .add(Expression.eq("entityId", entId.getId()))
            .add(Expression.eq("status", status))
            .add(Expression.between("timestamp", new Long(begin), 
                                    new Long(end)))
            .addOrder(Order.desc("timestamp"))
            .list();
    }
    
    List findByEntity(AppdefEntityID entId, long begin, long end,
                      String[] eventTypes) {
        Criteria c = createCriteria()
            .add(Expression.eq("entityType", new Integer(entId.getType())))
            .add(Expression.eq("entityId", entId.getId()))
            .add(Expression.between("timestamp", new Long(begin), 
                                    new Long(end)));
        
        if (eventTypes != null && eventTypes.length > 0)
            c.add(Expression.in("type", eventTypes));
        c.addOrder(Order.desc("timestamp"));
        return c.list();
    }
    
    List findBySubject(String subject) {
        String sql = "from EventLog e where e.subject = :subject";
        
        return getSession().createQuery(sql)
            .setParameter("subject", subject)
            .list();
    }

    List findByCtime(long begin, long end, String[] eventTypes) {
        Criteria c = createCriteria()
            .add(Expression.between("timestamp", new Long(begin),
                                    new Long(end)));
        if (eventTypes != null && eventTypes.length > 0) {
            c.add(Expression.in("type", eventTypes));
        }
        c.addOrder(Order.desc("timestamp"));
        return c.list();
    }

    void deleteLogs(long begin, long end) {
        String sql = "from EventLog l " + 
            "where l.timestamp between :beg and :end";

        Collection c = getSession().createQuery(sql)
            .setLong("beg", begin)
            .setLong("end", end)
            .list();
        
        for (Iterator i=c.iterator(); i.hasNext(); ) {
            EventLog l = (EventLog)i.next();
            
            remove(l);
        }
    }
    
    void remove(EventLog l) {
        super.remove(l);
    }
    
    void save(EventLog l) { 
        super.save(l);
    }
}
