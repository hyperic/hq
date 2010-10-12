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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.EdgePermCheck;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.PermissionManager.RolePermNativeSQL;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventLogStatus;
import org.hyperic.hq.measurement.server.session.Number;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class EventLogDAO
    extends HibernateDAO<EventLog> {
    private final String TABLE_EVENT_LOG = "EAM_EVENT_LOG";
    private final String TABLE_EAM_NUMBERS = "EAM_NUMBERS";
    private PermissionManager permissionManager;
    private final Log log = LogFactory.getLog(EventLogDAO.class.getName());

    private static final List<String> VIEW_PERMISSIONS = Arrays
        .asList(new String[] { AuthzConstants.platformOpViewPlatform,
                              AuthzConstants.serverOpViewServer,
                              AuthzConstants.serviceOpViewService,
                              AuthzConstants.groupOpViewResourceGroup, });
    
    private static final List<String> MANAGE_ALERT_PERMISSIONS = Arrays.asList(new String[] {
                             AuthzConstants.platformOpManageAlerts,
                             AuthzConstants.serverOpManageAlerts,
                             AuthzConstants.serviceOpManageAlerts,
                             AuthzConstants.groupOpManageAlerts
    });

    @Autowired
    public EventLogDAO(SessionFactory f, PermissionManager permissionManager) {
        super(EventLog.class, f);
        this.permissionManager = permissionManager;
    }

    EventLog create(EventLog res) {
        if (res.getResource() != null)
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
    
    EventLog findLog(String typeClass, int instanceId, long timestamp) {
        String hql = "select l from EventLog l where l.timestamp = :ts " +
                     "and l.instanceId = :instId and l.type = :type";
        Query q = createQuery(hql)
        .setLong("ts", timestamp)
        .setInteger("instId", instanceId)
        .setString("type",typeClass);
        
        List<EventLog> events = q.list();
        if(events.isEmpty()) {
            return null;
        }
        if(events.size() > 1) {
            log.warn("Found multiple log entries matching the specified " +
                     "criteria (typeClass=" + typeClass +", instanceId=" + instanceId + 
                     ", timestamp=" + timestamp + "). Returning the first one.");
        }
        return (EventLog) events.iterator().next();
    }

    /**
     * Gets a list of {@link ResourceEventLog}s. Most arguments are required.
     * pInfo is required to have a sort field of type {@link EventLogSortField}
     * 
     * @param typeClass Not required. If specified, the results will all be of
     *        this class (.org.hy...ResourceLogEvent)
     * @param inGroups Not required. If specified, a list of
     *        {@link ResourceGroup}s which will contain the resulting logs
     */
    List<ResourceEventLog> findLogs(AuthzSubject subject, long begin, long end, PageInfo pInfo,
                                    EventLogStatus maxStatus, String typeClass,
                                    Collection<ResourceGroup> inGroups) {
        EventLogSortField sort = (EventLogSortField) pInfo.getSort();
        boolean doGroupFilter = false;
        String groupFilterSql;

        RolePermNativeSQL roleSql = PermissionManagerFactory.getInstance()
            .getRolePermissionNativeSQL("r", "e", "subject", "opListVR", "opListMA");

        if (inGroups == null || inGroups.isEmpty())
            groupFilterSql = "";
        else {
            doGroupFilter = true;
            groupFilterSql = " and exists ( "
                             + "select rgm.resource_id from EAM_RES_GRP_RES_MAP rgm "
                             + " join EAM_RESOURCE_GROUP g on rgm.resource_group_id = g.id "
                             + " where rgm.resource_id = r.id and g.id in (:inGroups) "
                             + "union all " + " select g2.resource_id from EAM_RESOURCE_GROUP g2 "
                             + " where g2.resource_id = r.id and g2.id in (:inGroups) " + ") ";
        }

        String sql = "select {e.*}, r.* " + "from EAM_RESOURCE r " +
                     "    join EAM_RESOURCE_TYPE rt on r.resource_type_id = rt.id " +
                     "    join EAM_EVENT_LOG e on e.resource_id = r.id " + "where " +
                     "    e.timestamp between :begin and :end " + groupFilterSql +
                     roleSql.getSQL() + " and " + "    case " +
                     "        when e.status = 'ANY' then -1 " +
                     "        when e.status = 'ERR' then 3 " +
                     "        when e.status = 'WRN' then 4 " +
                     "        when e.status = 'INF' then 6 " +
                     "        when e.status = 'DBG' then 7 " + "        else -1 " +
                     "    end <= :maxStatus ";

        if (typeClass != null) {
            sql += "    and type = :type ";
        }

        sql += " order by " + sort.getSortString("r", "e") + (pInfo.isAscending() ? "" : " DESC");

        if (!sort.equals(EventLogSortField.DATE)) {
            sql += ", " + EventLogSortField.DATE.getSortString("r", "e") + " DESC";
        }

        Query q = getSession().createSQLQuery(sql).addEntity("e", EventLog.class).setLong("begin",
            begin).setLong("end", end).setInteger("maxStatus", maxStatus.getCode());
        roleSql.bindParams(q, subject, VIEW_PERMISSIONS, MANAGE_ALERT_PERMISSIONS);

        if (typeClass != null) {
            q.setString("type", typeClass);
        }

        if (doGroupFilter) {
            List<Integer> inGroupIds = new ArrayList<Integer>(inGroups.size());
            for (ResourceGroup g : inGroups) {
                inGroupIds.add(g.getId());
            }
            q.setParameterList("inGroups", inGroupIds);
        }

        List<EventLog> vals = pInfo.pageResults(q).list();
        List<ResourceEventLog> res = new ArrayList<ResourceEventLog>(vals.size());
        for (EventLog e : vals) {
            res.add(new ResourceEventLog(e.getResource(), e));
        }
        return res;
    }
    
    /**
     * @return 0 if there are no unfixed alerts
     */
    private final long getOldestUnfixedAlertTime() {
        Object o = getSession()
            .createQuery("select min(ctime) from Alert where fixed = '0'")
            .uniqueResult();
        if (o == null) {
            return 0;
        }
        return ((Long)o).longValue();
    }
    
    /**
     * @return {@link Map} of {@link Integer} = AlertDefitionId to
     *  {@link Map} of <br>
     *   key {@link AlertInfo} <br>
     *   value {@link Integer} AlertId
     */
    @SuppressWarnings("unchecked")
    private final Map<Integer,Map<AlertInfo,Integer>> getUnfixedAlertInfoAfter(long ctime) {
        final String hql = new StringBuilder(128)
            .append("SELECT alertDefinition.id, id, ctime ")
            .append("FROM Alert WHERE ctime >= :ctime and fixed = '0' ")
            .append("ORDER BY ctime")
            .toString();
        final List<Object[]> list = getSession()
            .createQuery(hql)
            .setLong("ctime", ctime)
            .list();
        final Map<Integer,Map<AlertInfo,Integer>> alerts = new HashMap<Integer,Map<AlertInfo,Integer>>(list.size());
        for (Object[] obj : list) {
            Map<AlertInfo,Integer> tmp = alerts.get(obj[0]);
            if (tmp == null) {
                tmp = new HashMap<AlertInfo,Integer>();
                alerts.put((Integer)obj[0], tmp);
            }
            final AlertInfo ai = new AlertInfo((Integer)obj[0], (Long)obj[2]);
            tmp.put(ai, (Integer)obj[1]);
        }
        return alerts;
    }
    
    private class AlertInfo {
        private final Integer _alertDefId;
        private final Long _ctime;
        AlertInfo(Integer alertDefId, Long ctime) {
            _alertDefId = alertDefId;
            _ctime = ctime;
        }
        AlertInfo(Integer alertDefId, long ctime) {
            _alertDefId = alertDefId;
            _ctime = new Long(ctime);
        }
        Integer getAlertDefId() {
            return _alertDefId;
        }
        Long getCtime() {
            return _ctime;
        }
        public boolean equals(Object rhs) {
            if (rhs == this) {
                return true;
            }
            if (rhs instanceof AlertInfo) {
                AlertInfo obj = (AlertInfo)rhs;
                return obj.getCtime().equals(_ctime) &&
                       obj.getAlertDefId().equals(_alertDefId);
            }
            return false;
        }
        public int hashCode() {
            return 17*_alertDefId.hashCode() + _ctime.hashCode();
        }
    }
    
    /**
     * Find unfixed AlertFiredEvent event logs for each alert definition in the list
     * 
     * @param alertDefinitionIds The list of alert definition ids
     * 
     * @return {@link Map} of {@link Integer} = AlertDefinitionId to
     *  {@link AlertFiredEvent}
     */
    @SuppressWarnings("unchecked")
    Map<Integer,AlertFiredEvent> findUnfixedAlertFiredEventLogs() {        
        final Map<Integer,AlertFiredEvent> rtn = new HashMap<Integer,AlertFiredEvent>();
        final long ctime = getOldestUnfixedAlertTime();
        if (ctime == 0) {
            return new HashMap<Integer,AlertFiredEvent>(0,1);
        }
        final Map<Integer,Map<AlertInfo,Integer>> alerts = getUnfixedAlertInfoAfter(ctime);
        final String hql = new StringBuilder(256)
            .append("FROM EventLog ")
            .append("WHERE timestamp >= :ctime AND type = :type ")
            .append("AND instanceId is not null")
            .toString();
        final List<EventLog> list = getSession()
            .createQuery(hql)
            .setString("type", AlertFiredEvent.class.getName())
            .setLong("ctime", ctime)
            .list();
        for (EventLog log  : list ) {
            if (log == null || log.getInstanceId() == null) {
                continue;
            }
            final Map<AlertInfo,Integer> objs = alerts.get(log.getInstanceId());
            if (objs == null) {
                continue;
            }
            final Integer alertDefId = log.getInstanceId();
            final long timestamp     = log.getTimestamp();
            final Integer alertId =
                objs.get(new AlertInfo(alertDefId, timestamp));
            if (alertId == null) {
                continue;
            }
            if (log.getResource().isInAsyncDeleteState()) {
                continue;
            }
            AlertFiredEvent alertFired = 
                createAlertFiredEvent(alertDefId, alertId, log);
            rtn.put(alertDefId, alertFired);
        }
        return rtn;
    }
    
    private final AlertFiredEvent createAlertFiredEvent(Integer alertDefId,
                                                        Integer alertId,
                                                        EventLog eventLog) {
        return new AlertFiredEvent(alertId, alertDefId, 
            AppdefUtil.newAppdefEntityId(eventLog.getResource()), eventLog.getSubject(),
            eventLog.getTimestamp(), eventLog.getDetail());
    }

    List<EventLog> findByEntityAndStatus(Resource r, AuthzSubject user, long begin, long end,
                                         String status) {
        EdgePermCheck wherePermCheck = permissionManager.makePermCheckHql("rez", false);
        String hql = "select l from EventLog l " + "join l.resource rez " + wherePermCheck +
                     "and l.timestamp between :begin and :end " + "and l.status = :status " +
                     "order by l.timestamp";

        Query q = createQuery(hql).setParameter("status", status).setLong("begin", begin).setLong(
            "end", end);
        return wherePermCheck.addQueryParameters(q, user, r, 0, VIEW_PERMISSIONS).list();
    }

    List<EventLog> findByEntity(AuthzSubject subject, Resource r, long begin, long end,
                                Collection<String> eventTypes) {
        EdgePermCheck wherePermCheck = permissionManager.makePermCheckHql("rez", false);
        String hql = " select l from EventLog l " + "join l.resource rez " + wherePermCheck +
                     "and l.timestamp between :begin and :end ";

        if (!eventTypes.isEmpty())
            hql += "and l.type in (:eventTypes) ";

        hql += "order by l.timestamp";

        Query q = createQuery(hql).setLong("begin", begin).setLong("end", end);

        if (!eventTypes.isEmpty())
            q.setParameterList("eventTypes", eventTypes);

        return wherePermCheck.addQueryParameters(q, subject, r, 0, VIEW_PERMISSIONS).list();
    }

    List<EventLog> findByGroup(Resource g, long begin, long end, Collection<String> eventTypes) {
        String hql = "select l from EventLog l join l.resource res "
                     + "left outer join res.groupBag gb " + "left outer join gb.group g "
                     + "where (l.resource = :r or g.resource = :r) "
                     + "and l.timestamp between :begin and :end ";

        if (!eventTypes.isEmpty())
            hql += "and l.type in (:eventTypes) ";

        hql += "order by l.timestamp";

        Query q = createQuery(hql).setParameter("r", g).setLong("begin", begin).setLong("end", end);

        if (!eventTypes.isEmpty())
            q.setParameterList("eventTypes", eventTypes);

        return q.list();
    }

    List<EventLog> findLastByType(Resource proto) {
        String hql = "select {ev.*} from EAM_EVENT_LOG ev, "
                     + "(select resource_id, max(EAM_EVENT_LOG.timestamp) as maxt "
                     + "from EAM_EVENT_LOG, EAM_RESOURCE res " + "where res.id = resource_id and "
                     + "res.proto_id=:proto group by resource_id) l "
                     + "where l.resource_id = ev.resource_id and l.maxt = ev.timestamp";

        return getSession().createSQLQuery(hql).addEntity("ev", EventLog.class).setInteger("proto",
            proto.getId().intValue()).list();
    }

    List findBySubject(String subject) {
        String sql = "from EventLog e where e.subject = :subject";

        return getSession().createQuery(sql).setParameter("subject", subject).list();
    }

    /**
     * Retrieve the minimum timestamp amongst all event logs.
     * 
     * @return The minimum timestamp or <code>-1</code> if there are no event
     *         logs.
     */
    long getMinimumTimeStamp() {
        String sql = "select min(l.timestamp) from EventLog l";

        Long min = (Long) getSession().createQuery(sql).uniqueResult();

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

        java.lang.Number result = (java.lang.Number) getSession().createQuery(sql).uniqueResult();
        return result.intValue();
    }

    /**
     * Delete event logs by resource. TODO: Chunking?
     * 
     * @param r The resource in which to delete event logs
     * @return The number of entries deleted.
     */
    int deleteLogs(Resource r) {
        String sql = "delete EventLog l where resource = :resource";

        return getSession().createQuery(sql).setParameter("resource", r).executeUpdate();
    }

    /**
     * Delete event logs in chunks.
     * 
     * @param from The timestamp to delete from.
     * @param to The timestamp to delete to.
     * @param interval The timestamp interval (delta) by which the deletes are
     *        chunked.
     * @return The number of event logs deleted.
     */
    int deleteLogs(long from, long to, long interval) {
        String sql = "delete EventLog l where "
                     + "l.timestamp >= :timeStart and l.timestamp <= :timeEnd";

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

            // We do not want to update the 2nd level cache with these event
            // logs
            session.setCacheMode(CacheMode.IGNORE);

            for (int i = 0; i < eventLogs.length; i++) {
                create(eventLogs[i]);
            }

            session.flush();
            session.clear();
        } finally {
            session.setFlushMode(flushMode);
            session.setCacheMode(cacheMode);
        }
    }

    private String getLogsExistSQL(Resource resource, long begin, long end, int intervals,
                                   EdgePermCheck wherePermCheck) {
        HQDialect dialect = getHQDialect();
        StringBuilder sql = new StringBuilder();
        String resVar = wherePermCheck.getResourceVar();
        String permSql = wherePermCheck.getSql();
        if (!dialect.useEamNumbers()) {
            for (int i = 0; i < intervals; i++) {
                sql.append("(SELECT ").append(i).append(" AS I FROM ").append(TABLE_EVENT_LOG)
                    .append(" evlog").append(" JOIN EAM_RESOURCE ").append(resVar).append(" on ")
                    .append("evlog.resource_id = ").append(resVar).append(".id").append(permSql)
                    .append(" AND timestamp BETWEEN (:begin + (:interval * ").append(i).append(
                        ")) AND ((:begin + (:interval * (").append(i).append(" + 1))) - 1)")
                    .append(" AND ").append(resVar).append(".id = :resourceId ").append(
                        dialect.getLimitString(1)).append(')');
                if (i < intervals - 1) {
                    sql.append(" UNION ALL ");
                }
            }
        } else {
            sql.append("SELECT i AS I FROM ").append(TABLE_EAM_NUMBERS).append(" WHERE i < ")
                .append(intervals).append(" AND EXISTS (").append("SELECT 1 FROM ").append(
                    TABLE_EVENT_LOG).append(" evlog").append(" JOIN EAM_RESOURCE ").append(resVar)
                .append(" on ").append("evlog.resource_id = ").append(resVar).append(".id").append(
                    permSql).append(" AND timestamp BETWEEN (:begin + (:interval").append(
                    " * i)) AND ((:begin + (:interval").append(" * (i + 1))) - 1)").append(" AND ")
                .append(resVar).append(".id = :resourceId ").append(dialect.getLimitString(1))
                .append(')');
        }
        return sql.toString();
    }

    boolean[] logsExistPerInterval(Resource resource, AuthzSubject subject, long begin, long end,
                                   int intervals) {
        long interval = (end - begin) / intervals;
        EdgePermCheck wherePermCheck = permissionManager.makePermCheckSql("rez", false);

        String sql = getLogsExistSQL(resource, begin, end, intervals, wherePermCheck);
        Query q = getSession().createSQLQuery(sql).addEntity("I",
            org.hyperic.hq.measurement.server.session.Number.class).setInteger("resourceId",
            resource.getId().intValue()).setLong("begin", begin).setLong("interval", interval);
        List result = wherePermCheck.addQueryParameters(q, subject, resource, 0, VIEW_PERMISSIONS)
            .list();

        boolean[] eventLogsInIntervals = new boolean[intervals];

        for (Iterator i = result.iterator(); i.hasNext();) {
            Number n = (Number) i.next();
            eventLogsInIntervals[(int) n.getI()] = true;
        }
        return eventLogsInIntervals;
    }

}
