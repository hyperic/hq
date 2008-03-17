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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.EdgePermCheck;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.events.EventLogStatus;

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

    public static class ResourceEventLog {
        private Resource _r;
        private EventLog _e;
        
        ResourceEventLog(Resource r, EventLog e) {
            _r = r;
            _e = e;
        }
        
        public Resource getResource() {
            return _r;
        }
        
        public EventLog getEventLog() {
            return _e;
        }
    }
         
    /**
     * Gets a list of {@link ResourceEventLog}s.  Most arguments
     * are required.  pInfo is required to have a sort field of
     * type {@link EventLogSortField}
     * 
     * @param typeClass  Not required.  If specified, the results will
     *                   all be of this class (.org.hy...ResourceLogEvent)
     * @param inGroups   Not required.  If specified, a list of 
     *                   {@link ResourceGroup}s which will contain the resulting
     *                   logs 
     */
    List findLogs(long begin, long end, PageInfo pInfo,
                  EventLogStatus maxStatus, String typeClass, 
                  Collection inGroups)
     {  
        EventLogSortField sort = (EventLogSortField)pInfo.getSort();
        String groupFilterSql = " and g.id in (:inGroups) ";
        
        if (inGroups == null || inGroups.isEmpty())
            groupFilterSql = "";
        
        String sql = "select {e.*}, r.* " + 
            "from EAM_RESOURCE r " + 
            "    join EAM_RESOURCE_TYPE rt on r.resource_type_id = rt.id " + 
            "    join EAM_EVENT_LOG e on e.resource_id = r.id " + 
            "where " +
            "    e.timestamp between :begin and :end and " +
            "    exists (select rgm.resource_id from EAM_RES_GRP_RES_MAP rgm "+
            "        join EAM_RESOURCE_GROUP g on rgm.resource_group_id = g.id"+
            "        where rgm.resource_id = r.id " + groupFilterSql + ") and "+
            "    case " + 
            "        when e.status = 'ANY' then -1 " + 
            "        when e.status = 'ERR' then 3 " + 
            "        when e.status = 'WRN' then 4 " + 
            "        when e.status = 'INF' then 6 " + 
            "        when e.status = 'DBG' then 7 " + 
            "        else -1 " +
            "    end <= :maxStatus ";
        
        if (typeClass != null) {
            sql += "    and type = :type ";
        }
        
        sql += " order by " + sort.getSortString("r", "e") + 
            (pInfo.isAscending() ? "" : " DESC");
        
        if (!sort.equals(EventLogSortField.DATE)) {
            sql += ", " + EventLogSortField.DATE.getSortString("r", "e") + 
                " DESC";
        }
        
        Query q = getSession().createSQLQuery(sql)
            .addEntity("e", EventLog.class)
            .setLong("begin", begin)
            .setLong("end", end)
            .setInteger("maxStatus", maxStatus.getCode());
             
        if (typeClass != null) {
            q.setString("type", typeClass);
        }
             
        if (inGroups != null && !inGroups.isEmpty()) {
            List inGroupIds = new ArrayList(inGroups.size());
            for (Iterator i=inGroups.iterator(); i.hasNext(); ) {
                ResourceGroup g = (ResourceGroup)i.next();
                inGroupIds.add(g.getId());
            }
            q.setParameterList("inGroups", inGroupIds);
        }
        
        List vals = pInfo.pageResults(q).list();
        List res = new ArrayList(vals.size());
        for (Iterator i=vals.iterator(); i.hasNext(); ) { 
            EventLog e = (EventLog)i.next();
            res.add(new ResourceEventLog(e.getResource(), e));
        }
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
