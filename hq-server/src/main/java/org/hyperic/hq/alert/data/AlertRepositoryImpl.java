package org.hyperic.hq.alert.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertInfo;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

public class AlertRepositoryImpl implements AlertRepositoryCustom {

    private static final int BATCH_SIZE = 1000;

    private AlertActionLogRepository alertActionLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public AlertRepositoryImpl(AlertActionLogRepository alertActionLogRepository) {
        this.alertActionLogRepository = alertActionLogRepository;
    }

    public int deleteAlertsByCreateTime(long before, int maxDeletes) {
        if (maxDeletes <= 0) {
            return 0;
        }
        // don't want to thrash the Alert cache, so select and do an explicit
        // remove() on each Object
        final String hql = new StringBuilder(64).append("from Alert where ")
            .append("ctime < :before and ")
            .append("not id in (select alertId from EscalationState es ")
            .append("where alertTypeEnum = :type)").toString();
        List<Alert> list = null;
        int count = 0;
        // due to
        // http://opensource.atlassian.com/projects/hibernate/browse/HHH-1985
        // need to batch this
        while (list == null || count < maxDeletes) {
            int batchSize = (BATCH_SIZE + count > maxDeletes) ? (maxDeletes - count) : BATCH_SIZE;
            list = entityManager.createQuery(hql, Alert.class).setParameter("before", before)
                .setParameter("type", ClassicEscalationAlertType.CLASSIC.getCode())
                .setMaxResults(batchSize).getResultList();
            if (list.size() == 0) {
                break;
            }
            alertActionLogRepository.deleteAlertActions(list);
            for (Alert alert : list) {
                count++;
                entityManager.remove(alert);
            }
            // need to flush or else the removed alerts won't be reflected in
            // the next hql
            entityManager.flush();
            if (count >= maxDeletes) {
                break;
            }
        }
        return count;
    }

    public int deleteByAlertDefinition(AlertDefinition def) {
        String sql = "DELETE FROM Alert a WHERE a.alertDefinition = :alertDef";
        return entityManager.createQuery(sql).setParameter("alertDef", def).executeUpdate();
    }

    public Page<Alert> findByCreateTimeAndPriority(long begin, long end, int priority,
                                                   boolean inEsc, boolean notFixed,
                                                   Integer groupId, Integer alertDefId,
                                                   Pageable pageable) {
        long total = getCountByCreateTimeAndPriority(begin, end, priority, inEsc, notFixed, groupId, alertDefId);
        if (total == 0) {
            return new PageImpl<Alert>(new ArrayList<Alert>(0), pageable, 0);
        }
        String sql = getAlertSql(begin, end, priority, inEsc, notFixed, groupId, alertDefId, false);
        Iterator<Order> orders = pageable.getSort().iterator();
        while (orders.hasNext()) {
            Order order = orders.next();
            sql += " order by " + order.getProperty() + " " + order.getDirection();
        }

        TypedQuery<Alert> q = entityManager.createQuery(sql, Alert.class)
            .setParameter("begin", begin).setParameter("end", end)
            .setParameter("priority", priority);
        // TODO there used to be a subject ID in AlertDAO and perms were checked
        q.setFirstResult(pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        return new PageImpl<Alert>(q.getResultList(),pageable,total);
    }
    
    

    public List<Alert> findByCreateTimeAndPriority(long begin, long end, int priority,
                                                   boolean inEsc, boolean notFixed,
                                                   Integer groupId, Integer alertDefId, Sort sort) {
        long total = getCountByCreateTimeAndPriority(begin, end, priority, inEsc, notFixed, groupId, alertDefId);
        if (total == 0) {
            return new ArrayList<Alert>(0);
        }
        String sql = getAlertSql(begin, end, priority, inEsc, notFixed, groupId, alertDefId, false);
        Iterator<Order> orders = sort.iterator();
        while (orders.hasNext()) {
            Order order = orders.next();
            sql += " order by " + order.getProperty() + " " + order.getDirection();
        }

        return entityManager.createQuery(sql, Alert.class)
            .setParameter("begin", begin).setParameter("end", end)
            .setParameter("priority", priority).getResultList();
        // TODO there used to be a subject ID in AlertDAO and perms were checked
    }

    public List<Alert> findByResourceInRange(Resource res, long begin, long end, boolean nameSort,
                                             boolean asc) {
        String sql = "from Alert a where a.alertDefinition.resource = :res " +
                     "and a.ctime between :begin and :end order by " +
                     (nameSort ? "a.alertDefinition.name" : "a.ctime") + (asc ? " asc" : " desc");

        return entityManager.createQuery(sql, Alert.class).setParameter("res", res)
            .setParameter("begin", begin).setParameter("end", end).getResultList();
    }

    public Alert findLastByDefinition(AlertDefinition def, boolean fixed) {
        try {
            return entityManager
                .createQuery(
                    "select a from Alert a where a.alertDefinition=:def and a.fixed=:fixed order by ctime DESC",
                    Alert.class).setParameter("alertDefinition", def).setParameter("fixed", fixed)
                .setMaxResults(1).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private String getAlertSql(long begin, long end, int priority, boolean inEsc, boolean notFixed,
                               Integer groupId, Integer alertDefId, boolean count) {
        return "select " +
               (count ? "count(a)" : "a") +
               " from " +
               (inEsc ? "EscalationState es, " : "") +
               "Alert a " +
               "join a.alertDefinition d " +
               "join d.resource r where " +
               (groupId == null ? "" : "exists (select rg from r.groupBag rg " +
                                       "where rg.group.id = " + groupId + ") and ") +
               "a.ctime between :begin and :end and " + (notFixed ? " a.fixed = false and " : "") +
               (alertDefId == null ? "" : "d.id = " + alertDefId + " and ") +
               "d.priority >= :priority " +
               (inEsc ? "and a.id = es.alertId and " + "es.alertDefinitionId = d.id " : "");
    }

    public long getCountByCreateTimeAndPriority(long begin, long end, int priority, boolean inEsc,
                                                boolean notFixed, Integer groupId,
                                                Integer alertDefId) {
        String sql = getAlertSql(begin, end, priority, inEsc, notFixed, groupId, alertDefId, true);
        return entityManager.createQuery(sql, Long.class).setParameter("begin", begin)
            .setParameter("end", end).setParameter("priority", priority).getSingleResult();
    }

    public Map<Integer, Map<AlertInfo, Integer>> getUnfixedAlertInfoAfter(long ctime) {
        final String hql = new StringBuilder(128).append("SELECT alertDefinition.id, id, ctime ")
            .append("FROM Alert WHERE ctime >= :ctime and fixed = '0' ").append("ORDER BY ctime")
            .toString();
        final List<Object[]> list = entityManager.createQuery(hql, Object[].class)
            .setParameter("ctime", ctime).getResultList();
        final Map<Integer, Map<AlertInfo, Integer>> alerts = new HashMap<Integer, Map<AlertInfo, Integer>>(
            list.size());
        for (Object[] obj : list) {
            Map<AlertInfo, Integer> tmp = alerts.get(obj[0]);
            if (tmp == null) {
                tmp = new HashMap<AlertInfo, Integer>();
                alerts.put((Integer) obj[0], tmp);
            }
            final AlertInfo ai = new AlertInfo((Integer) obj[0], (Long) obj[2]);
            tmp.put(ai, (Integer) obj[1]);
        }
        return alerts;
    }

}
