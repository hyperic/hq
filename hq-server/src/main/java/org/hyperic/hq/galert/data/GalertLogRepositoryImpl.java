package org.hyperic.hq.galert.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

public class GalertLogRepositoryImpl implements GalertLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<GalertLog> findByCreateTimeAndPriority(long begin, long end,
                                                       AlertSeverity severity,
                                                       boolean inEscalation, boolean notFixed,
                                                       Integer groupId, Integer galertDefId,
                                                       Pageable pageable) {
        long total = entityManager
            .createQuery(
                getCreateTimeAndPriorityQuery(begin, end, severity, inEscalation, notFixed,
                    groupId, galertDefId, true), Long.class).setParameter("begin", begin)
            .setParameter("end", end).setParameter("priority", severity.getCode())
            .getSingleResult();
        String query = getCreateTimeAndPriorityQuery(begin, end, severity, inEscalation, notFixed,
            groupId, galertDefId, false);
        Iterator<Order> orders = pageable.getSort().iterator();
        while (orders.hasNext()) {
            Order order = orders.next();
            query += " order by " + order.getProperty() + " " + order.getDirection();
        }
        List<GalertLog> results = entityManager.createQuery(query, GalertLog.class)
            .setParameter("begin", begin).setParameter("end", end)
            .setParameter("priority", severity.getCode()).setFirstResult(pageable.getOffset())
            .setMaxResults(pageable.getPageSize()).getResultList();
        return new PageImpl<GalertLog>(results, pageable, total);
    }

    public List<GalertLog> findByCreateTimeAndPriority(long begin, long end,
                                                       AlertSeverity severity,
                                                       boolean inEscalation, boolean notFixed,
                                                       Integer groupId, Integer galertDefId,
                                                       Sort sort) {
        String query = getCreateTimeAndPriorityQuery(begin, end, severity, inEscalation, notFixed,
            groupId, galertDefId, false);
        Iterator<Order> orders = sort.iterator();
        while (orders.hasNext()) {
            Order order = orders.next();
            query += " order by " + order.getProperty() + " " + order.getDirection();
        }
        return entityManager.createQuery(query, GalertLog.class).setParameter("begin", begin)
            .setParameter("end", end).setParameter("priority", severity.getCode()).getResultList();
    }

    private String getCreateTimeAndPriorityQuery(long begin, long end, AlertSeverity severity,
                                                 boolean inEscalation, boolean notFixed,
                                                 Integer groupId, Integer galertDefId, boolean count) {
        // TODO this query used to do perm checking with a passed-in subject ID
        return "select " + (count ? "count(a)" : "a") + " from " +
               (inEscalation ? "EscalationState es, " : "") + "GalertLog a " + "join a.def d " +
               "where " + (groupId != null ? " d.group.id = " + groupId + " and " : "") +
               "a.timestamp between :begin and :end " + (notFixed ? " and a.fixed = false " : "") +
               (galertDefId == null ? "" : "and d.id = " + galertDefId + " ") +
               "and d.severity >= :priority " +
               (inEscalation ? "and a.id = es.alertId and " + "es.alertDefinitionId = d.id " : "");
    }

    public Page<GalertLog> findByGroupAndTimestampBetween(ResourceGroup group, long begin,
                                                          long end, Pageable pageable) {
        long total = entityManager
            .createQuery(
                "select count(l) from GalertLog l where l.def.group = :group and l.timestamp >= :begin and l.timestamp <= :end",
                Long.class).setParameter("group", group).setParameter("begin", begin)
            .setParameter("end", end).getSingleResult();
        if (total == 0) {
            return new PageImpl<GalertLog>(new ArrayList<GalertLog>(0), pageable, 0);
        }
        String query = "select l from GalertLog l where l.def.group = :group and l.timestamp >= :begin and l.timestamp <= :end";
        Iterator<Order> orders = pageable.getSort().iterator();
        while (orders.hasNext()) {
            Order order = orders.next();
            query += " order by " + order.getProperty() + " " + order.getDirection();
        }
        List<GalertLog> results = entityManager.createQuery(query, GalertLog.class)
            .setParameter("group", group).setParameter("begin", begin).setParameter("end", end)
            .setFirstResult(pageable.getOffset()).setMaxResults(pageable.getPageSize())
            .getResultList();
        return new PageImpl<GalertLog>(results, pageable, total);
    }

    public GalertLog findLastByDefinition(GalertDef def, boolean fixed) {
        List<GalertLog> logs = entityManager
            .createQuery(
                "select l from GalertLog l where l.def=:def and l.fixed=:fixed order by l.timestamp DESC",
                GalertLog.class).setParameter("def", def).setParameter("fixed", fixed)
            .setMaxResults(1).getResultList();
        if (logs.isEmpty()) {
            return null;
        }
        return logs.get(0);
    }

}
