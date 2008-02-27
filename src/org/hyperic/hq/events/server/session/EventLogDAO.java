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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.EdgePermCheck;
import org.hyperic.hq.dao.HibernateDAO;

public class EventLogDAO extends HibernateDAO {
    private static final List VIEW_PERMISSIONS = 
        Arrays.asList(new String[] { 
            AuthzConstants.platformOpViewPlatform,
            AuthzConstants.serverOpViewServer,
            AuthzConstants.serviceOpViewService,
        });
    
    private static final String TIMESTAMP = "timestamp";

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

    List findByEntityAndStatus(Resource r, AuthzSubject user, 
                               long begin, long end,
                               String status) 
    {
        EdgePermCheck wherePermCheck =
            getPermissionManager().makePermCheckSql("rez");
        String hql = "select l from EventLog l " +
            "join l.resource rez " +
            wherePermCheck +
            "and l.timestamp between :begin and :end " + 
            "and l.status = :status " +
            "order by l.timestamp";
        
        Query q = createQuery(hql)
            .setParameter("status", status)
            .setLong("begin", begin)
            .setLong("end", end);
        return wherePermCheck.addQueryParameters(q, user, r, 0, 
                                                 VIEW_PERMISSIONS).list();
    }
    
    List findByEntity(AuthzSubject subject, Resource r, long begin, long end,
                      Collection eventTypes)
    {
        EdgePermCheck wherePermCheck = 
            getPermissionManager().makePermCheckSql("rez");
        String hql = " select l from EventLog l " + 
            "join l.resource rez " +
            wherePermCheck +
            "and l.timestamp between :begin and :end "; 
        
        if (!eventTypes.isEmpty())
            hql += "and l.type in (:eventTypes) ";
        
        hql += "order by l.timestamp"; 
        
        Query q = createQuery(hql)
            .setLong("begin", begin)
            .setLong("end", end);
        
        if (!eventTypes.isEmpty()) 
            q.setParameterList("eventTypes", eventTypes);

        return wherePermCheck.addQueryParameters(q, subject, r,
                                                 0, VIEW_PERMISSIONS).list();
    }
    
    List findLastByType(Resource proto) {
        String hql = "from EventLog as el " + 
            "  where " +  
            "(el.resource, el.timestamp) in ( " + 
            "     select resource, max(timestamp) from EventLog el " +
            "     where el.resource.prototype = :proto " + 
            "     group by el.resource " + 
            ")";
        
        return createQuery(hql)
            .setParameter("proto", proto)
            .list();
    }
    
    List findBySubject(String subject) {
        String sql = "from EventLog e where e.subject = :subject";
        
        return getSession().createQuery(sql)
            .setParameter("subject", subject)
            .list();
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
     * Retrieve the total number of event logs.
     * 
     * @return The total number of event logs.
     */
    int getTotalNumberLogs() {
        String sql = "select count(*) from EventLog";
        
        Integer result = (Integer)getSession().createQuery(sql).uniqueResult();
        
        return result.intValue();
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
    
    boolean[] logsExistPerInterval(Resource resource, AuthzSubject subject,
                                   long begin, long end,  
                                   int intervals) 
    {
        EdgePermCheck wherePermCheck = 
            getPermissionManager().makePermCheckSql("rez");
        String hql = "select i.I from Number i " +
            "where I < :intervals " +
            " and exists (" +
            "    select l.id from EventLog l " +
            "    join l.resource rez  " +
            wherePermCheck +
            "     and timestamp between (:begin + (:interval * I))"+
            "                    and ((:begin + (:interval * (I + 1))) - 1) " +
            " ) "; 
    
        long interval = (end - begin) / intervals;
        boolean[] eventLogsInIntervals = new boolean[intervals];
    
        Query q = createQuery(hql)
            .setLong("begin", begin)
            .setInteger("intervals", intervals)
            .setLong("interval", interval);
        
        List result = 
            wherePermCheck.addQueryParameters(q, subject, resource, 
                                              0, VIEW_PERMISSIONS).list();
        
        for (Iterator i=result.iterator(); i.hasNext(); ) {
            Number n = (Number)i.next();
        
            eventLogsInIntervals[n.intValue()] = true;
        }        
        return eventLogsInIntervals;
    }
    
    void remove(EventLog l) {
        super.remove(l);
    }
    
    void save(EventLog l) { 
        super.save(l);
    }
}
