package org.hyperic.hq.event.data;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hyperic.hibernate.DialectAccessor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Number;
import org.springframework.beans.factory.annotation.Autowired;

public class EventLogRepositoryImpl implements EventLogRepositoryCustom {

    private static final String TABLE_EAM_NUMBERS = "EAM_NUMBERS";
    private static final String TABLE_EVENT_LOG = "EAM_EVENT_LOG";

    private DialectAccessor dialectAccessor;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public EventLogRepositoryImpl(DialectAccessor dialectAccessor) {
        this.dialectAccessor = dialectAccessor;
    }

    public int deleteLogsInTimeRange(long from, long to) {
        // Now that we have valid from/to values, figure out what the
        // interval is (don't loop more than 60 times)
        long interval = Math.max(MeasurementConstants.DAY, (to - from) / 60);
        String sql = "delete EventLog l where "
                     + "l.timestamp >= :timeStart and l.timestamp <= :timeEnd";
        int rowsDeleted = 0;
        Query query = entityManager.createQuery(sql);
        for (long cursor = from; cursor < to; cursor += interval) {
            long end = Math.min(to, cursor + interval);
            query.setParameter("timeStart", cursor);
            query.setParameter("timeEnd", end);
            rowsDeleted += query.executeUpdate();
            entityManager.flush();
        }
        return rowsDeleted;
    }

    private String getLogsExistSQL(Resource resource, long begin, long end, int intervals) {
        HQDialect dialect = dialectAccessor.getHQDialect();
        StringBuilder sql = new StringBuilder();
        // String resVar = wherePermCheck.getResourceVar();
        // String permSql = wherePermCheck.getSql();
        if (!dialect.useEamNumbers()) {
            for (int i = 0; i < intervals; i++) {
                sql.append("(SELECT ").append(i).append(" AS I FROM ").append(TABLE_EVENT_LOG)
                    .append(" evlog").append(" where evlog.resource_id = ").append(":resourceId")
                    .append(" AND timestamp BETWEEN (:begin + (:interval * ").append(i)
                    .append(")) AND ((:begin + (:interval * (").append(i).append(" + 1))) - 1) ")
                    .append("ORDER BY I ").append(dialect.getLimitString(1)).append(')');
                if (i < intervals - 1) {
                    sql.append(" UNION ALL ");
                }
            }
        } else {
            sql.append("SELECT i AS I FROM ").append(TABLE_EAM_NUMBERS).append(" WHERE i < ")
                .append(intervals).append(" AND EXISTS (").append("SELECT 1 FROM ")
                .append(TABLE_EVENT_LOG).append(" evlog").append("where evlog.resource_id = ")
                .append(":resourceId").append(" AND timestamp BETWEEN (:begin + (:interval")
                .append(" * i)) AND ((:begin + (:interval").append(" * (i + 1))) - 1) ")
                .append("ORDER BY I ").append(dialect.getLimitString(1)).append(')');
        }

        return sql.toString();
    }

    public void insertLogs(EventLog[] eventLogs) {
        FlushModeType flushMode = entityManager.getFlushMode();
        try {
            entityManager.setFlushMode(FlushModeType.COMMIT);

            for (int i = 0; i < eventLogs.length; i++) {
                if (eventLogs[i].getResource() != null) {
                    entityManager.persist(eventLogs[i]);
                }
            }
            entityManager.flush();
            entityManager.clear();
        } finally {
            entityManager.setFlushMode(flushMode);
        }
    }

    @SuppressWarnings("unchecked")
    public boolean[] logsExistPerInterval(Resource resource, long begin, long end, int intervals) {
        long interval = (end - begin) / intervals;
        // TODO EventLogManager indicates that descendents will be taken into
        // consideration, but below passing of false only uses distance of 0
        // on ResourceEdge table, creating unnecessary join. Implementing
        // solution using only the resource
        // EdgePermCheck wherePermCheck =
        // permissionManager.makePermCheckSql("rez", false);
        String sql = getLogsExistSQL(resource, begin, end, intervals);
        List<Number> results = entityManager.createNativeQuery(sql, Number.class)
            .setParameter("resourceId", resource.getId().intValue()).setParameter("begin", begin)
            .setParameter("interval", interval).getResultList();
        // TODO impl permission checking in EE
        // List result = wherePermCheck.addQueryParameters(q, subject, resource,
        // 0, VIEW_PERMISSIONS)
        // .list();
        boolean[] eventLogsInIntervals = new boolean[intervals];
        for (Number n : results) {
            eventLogsInIntervals[(int) n.getI()] = true;
        }
        return eventLogsInIntervals;
    }

}
