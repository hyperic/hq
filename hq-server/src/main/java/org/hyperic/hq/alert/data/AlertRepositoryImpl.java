package org.hyperic.hq.alert.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertInfo;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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

    public long countByCreateTimeAndPriority(long begin, long end, int priority, boolean inEsc,
                                             boolean notFixed, Integer groupId, Integer alertDefId) {
        Query query = getAlertQuery(begin, end, priority, inEsc, notFixed, groupId, alertDefId,
            true, null);
        return (Long) query.getSingleResult();
    }

    public int deleteByAlertDefinition(AlertDefinition def) {
        String sql = "DELETE FROM Alert a WHERE a.alertDefinition = :alertDef";
        return entityManager.createQuery(sql).setParameter("alertDef", def).executeUpdate();
    }

    public int deleteByCreateTime(long before, int maxDeletes) {
        if (maxDeletes <= 0) {
            return 0;
        }
        // don't want to thrash the Alert cache, so select and do an explicit
        // remove() on each Object
        final String hql = new StringBuilder(64).append("select a from Alert a where ")
            .append("a.ctime < :before and ")
            .append("not id in (select es.alertId from EscalationState es ")
            .append("where es.alertType = :type)").toString();
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
            alertActionLogRepository.deleteByAlerts(list);
            for (Alert alert : list) {
                count++;
                entityManager.remove(entityManager.contains(alert) ? alert : entityManager
                    .merge(alert));
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

    @SuppressWarnings("unchecked")
    public Page<Alert> findByCreateTimeAndPriority(long begin, long end, int priority,
                                                   boolean inEsc, boolean notFixed,
                                                   Integer groupId, Integer alertDefId,
                                                   Pageable pageable) {
        long total = countByCreateTimeAndPriority(begin, end, priority, inEsc, notFixed, groupId,
            alertDefId);
        if (total == 0) {
            return new PageImpl<Alert>(new ArrayList<Alert>(0), pageable, 0);
        }

        Query query = getAlertQuery(begin, end, priority, inEsc, notFixed, groupId, alertDefId,
            false, pageable.getSort());
        // TODO there used to be a subject ID in AlertDAO and perms were checked
        query.setFirstResult(pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return new PageImpl<Alert>(query.getResultList(), pageable, total);
    }

    @SuppressWarnings("unchecked")
    public List<Alert> findByCreateTimeAndPriority(long begin, long end, int priority,
                                                   boolean inEsc, boolean notFixed,
                                                   Integer groupId, Integer alertDefId, Sort sort) {
        long total = countByCreateTimeAndPriority(begin, end, priority, inEsc, notFixed, groupId,
            alertDefId);
        if (total == 0) {
            return new ArrayList<Alert>(0);
        }
        Query query = getAlertQuery(begin, end, priority, inEsc, notFixed, groupId, alertDefId,
            false, sort);
        // TODO there used to be a subject ID in AlertDAO and perms were checked
        return query.getResultList();

    }

    public List<Alert> findByResourceInRange(Resource res, long begin, long end, boolean nameSort,
                                             boolean asc) {
        String sql = "select a from Alert a where a.alertDefinition.resource = :res " +
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
                    Alert.class).setParameter("def", def).setParameter("fixed", fixed)
                .setMaxResults(1).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    private Query getAlertQuery(long begin, long end, int priority, boolean inEsc,
                                boolean notFixed, Integer groupId, Integer alertDefId,
                                boolean count, Sort sort) {
        List<Integer> memberIds = new ArrayList<Integer>();
        if (groupId != null) {
            ResourceGroup group = entityManager.find(ResourceGroup.class, groupId);
            if (group != null) {
                // TODO used to call attach on the group if using DAO
                Set<Resource> members = group.getMembers();
                for (Resource member : members) {
                    memberIds.add(member.getId());
                }
            }
        }
        String ql = "select " + (count ? "count(a)" : "a") + " from " +
                    (inEsc ? "EscalationState es, " : "") + "Alert a " +
                    "join a.alertDefinition d " + "join d.resource r where " +
                    (groupId == null ? "" : "r.id in (:resourceIds) and ") +
                    "a.ctime between :begin and :end and " +
                    (notFixed ? " a.fixed = false and " : "") +
                    (alertDefId == null ? "" : "d.id = " + alertDefId + " and ") +
                    "d.priority >= :priority " +
                    (inEsc ? "and a.id = es.alertId and " + "es.alertDefId = d.id " : "");
        if (sort != null) {
            Iterator<Order> orders = sort.iterator();
            while (orders.hasNext()) {
                Order order = orders.next();
                ql += " order by " + order.getProperty() + " " + order.getDirection();
            }
        }
        Query query = entityManager.createQuery(ql).setParameter("begin", begin)
            .setParameter("end", end).setParameter("priority", priority);
        if (groupId != null) {
            query.setParameter("resourceIds", memberIds);
        }
        return query;
    }

    public long getOldestUnfixedAlertTime() {
        long alertCount = entityManager.createQuery("select count(a) from Alert a", Long.class)
            .getSingleResult();
        if (alertCount == 0) {
            return 0;
        }
        return entityManager.createQuery("select min(a.ctime) from Alert a where a.fixed = false",
            Long.class).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Map<AlertInfo, Integer>> getUnfixedAlertInfoAfter(long ctime) {
        final String hql = new StringBuilder(128)
            .append("SELECT a.alertDefinition.id, a.id, a.ctime ")
            .append("FROM Alert a WHERE a.ctime >= :ctime and a.fixed = false ")
            .append("ORDER BY a.ctime").toString();
        final List<Object[]> list = entityManager.createQuery(hql).setParameter("ctime", ctime)
            .getResultList();
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

    public boolean isAckable(Alert alert) {
        long escalationStates = entityManager
            .createQuery(
                "select count(e) from EscalationState e where e.alertId = :id and e.alertType = -559038737",
                Long.class).setParameter("id", alert.getId()).getSingleResult();
        if (escalationStates == 0) {
            return false;
        }
        List<AuthzSubject> ackedBy = entityManager
            .createQuery(
                "select e.acknowledgedBy from EscalationState e where e.alertId = :id and e.alertType = -559038737",
                AuthzSubject.class).setParameter("id", alert.getId()).getResultList();
        if (ackedBy.isEmpty()) {
            return true;
        }
        return false;
    }

}
