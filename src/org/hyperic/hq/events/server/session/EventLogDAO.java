/*
* NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.dao.HibernateDAO;

public class EventLogDAO extends HibernateDAO {
    private static final String TIMESTAMP = "timestamp";
    private static final String ENTITY_ID = "entityId";
    private static final String ENTITY_TYPE = "entityType";

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

    List findByEntityAndStatus(AppdefEntityID entId, long begin, long end,
                               String status) 
    {
        return createCriteria()
            .add(Expression.eq(ENTITY_TYPE, new Integer(entId.getType())))
            .add(Expression.eq(ENTITY_ID, entId.getId()))
            .add(Expression.eq("status", status))
            .add(Expression.between(TIMESTAMP, new Long(begin), 
                                    new Long(end)))
            .addOrder(Order.desc(TIMESTAMP))
            .list();
    }
    
    List findByEntity(AppdefEntityID entId, long begin, long end,
                      String[] eventTypes) {
        Criteria c = createCriteria()
            .add(Expression.eq(ENTITY_TYPE, new Integer(entId.getType())))
            .add(Expression.eq(ENTITY_ID, entId.getId()))
            .add(Expression.between(TIMESTAMP, new Long(begin), new Long(end)));
        
        if (eventTypes != null && eventTypes.length > 0)
            c.add(Expression.in("type", eventTypes));
        c.addOrder(Order.desc(TIMESTAMP));
        return c.list();
    }
    
    List findLastByEntity(AppdefEntityID entId, long begin) 
    {
        return createCriteria()
            .add(Expression.eq(ENTITY_TYPE, new Integer(entId.getType())))
            .add(Expression.eq(ENTITY_ID, entId.getId()))
            .add(Expression.gt(TIMESTAMP, new Long(begin)))
            .addOrder(Order.desc(TIMESTAMP))
            .setMaxResults(1)
            .list();
    }
    
    List findBySubject(String subject) {
        String sql = "from EventLog e where e.subject = :subject";
        
        return getSession().createQuery(sql)
            .setParameter("subject", subject)
            .list();
    }

    List findByCtime(long begin, long end, String[] eventTypes) {
        Criteria c = createCriteria()
            .add(Expression.between(TIMESTAMP, new Long(begin),
                                    new Long(end)));
        if (eventTypes != null && eventTypes.length > 0) {
            c.add(Expression.in("type", eventTypes));
        }
        c.addOrder(Order.desc(TIMESTAMP));
        return c.list();
    }

    /**
     * Retrieve the minimum timestamp amongst all event logs.
     * 
     * @return The minimum timestamp or <code>-1</code> if there are no 
     *         event logs.
     */
    long getMinimumTimeStamp() {
        String sql = "select min(l.timestamp) from EventLog l";
        
        Long min = (Long)getSession().createQuery(sql).uniqueResult();
        
        if (min == null) {
            return -1;
        } else {
            return min.longValue();
        }
    }
    
    /**
     * Delete event logs in chunks.
     * 
     * @param from The timestamp to delete from.
     * @param to The timestamp to delete to.
     * @param interval The timestamp interval (delta) by which the deletes 
     *                 are chunked.
     * @return The number of event logs deleted.
     */
    int deleteLogs(long from, long to, long interval) {
        String sql = "delete EventLog l where " +
                     "l.timestamp >= :timeStart and l.timestamp <= :timeEnd";

        int rowsDeleted = 0;
        Session session = getSession();
        Query query = session.createQuery(sql);
        
        for (long cursor = from; cursor < to; cursor += interval) {
            long end = Math.min(to, cursor + interval);
            query.setLong("timeStart", cursor);
            query.setLong("timeEnd", end);
            rowsDeleted += query.executeUpdate();
            session.flush();
        }
        
        return rowsDeleted;
    }
    
    /**
     * Insert the event logs in batch, with batch size specified by the 
     * <code>hibernate.jdbc.batch_size</code> configuration property.
     * 
     * @param eventLogs The event logs to insert.
     */
    void insertLogs(EventLog[] eventLogs) {        
        Session session = getSession();
                
        FlushMode flushMode = session.getFlushMode();
        CacheMode cacheMode = session.getCacheMode();

        try {
            session.setFlushMode(FlushMode.MANUAL);
            
            // We do not want to update the 2nd level cache with these event logs
            session.setCacheMode(CacheMode.IGNORE);
          
            for (int i = 0; i < eventLogs.length; i++) {
                save(eventLogs[i]);
            }
            
            session.flush();
            session.clear();
        } finally {
            session.setFlushMode(flushMode);
            session.setCacheMode(cacheMode);
        }
    }
    
    void remove(EventLog l) {
        super.remove(l);
    }
    
    void save(EventLog l) { 
        super.save(l);
    }
}
