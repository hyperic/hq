package org.hyperic.hq.alert.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.server.session.AlertCondition;
import org.hyperic.hq.events.server.session.RegisteredTrigger;
import org.hyperic.hq.events.server.session.ResourceAlertDefinition;
import org.hyperic.util.timer.StopWatch;

public class RegisteredTriggerRepositoryImpl implements RegisteredTriggerRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private final Log log = LogFactory.getLog(RegisteredTriggerRepositoryImpl.class);

    public Set<RegisteredTrigger> findAllEnabledTriggers() {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        // For performance optimization, we want to fetch each trigger's alert
        // def as well as the alert def's alert definition state and conditions
        // in a single query (as they will be used to create
        // AlertConditionEvaluators when creating trigger impls). This query
        // guarantees that when we do
        // trigger.getAlertDefinition().getConditions(),
        // the database is not hit again
        String hql = new StringBuilder(256).append("select ad from ResourceAlertDefinition ad ")
            .append("join fetch ad.alertDefinitionState ").append("join fetch ad.conditionsBag c ")
            .append("join fetch c.trigger ").append("where ad.enabled = true")
            .append(" and ad.deleted = false ").toString();
        if (debug)
            watch.markTimeBegin("createQuery.list");
        List<ResourceAlertDefinition> alertDefs = entityManager.createQuery(hql,
            ResourceAlertDefinition.class).getResultList();
        if (debug)
            watch.markTimeEnd("createQuery.list");

        Set<RegisteredTrigger> triggers = new LinkedHashSet<RegisteredTrigger>();
        if (debug)
            watch.markTimeBegin("addTriggers");

        for (ResourceAlertDefinition definition : alertDefs) {
            for (AlertCondition condition : definition.getConditionsBag()) {
                triggers.add(condition.getTrigger());
            }
        }
        if (debug) {
            watch.markTimeEnd("addTriggers");
            log.debug("findAllEnabledTriggers: " + watch);
        }
        return triggers;
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, List<Integer>> findTriggerIdsByAlertDefinitionIds(List<Integer> alertDefIds) {
        if (alertDefIds.isEmpty()) {
            return new HashMap<Integer, List<Integer>>(0, 1);
        }

        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        final String sql = new StringBuilder()
            .append("SELECT T.ALERT_DEFINITION_ID AS ALERT_DEF_ID, ").append("T.ID AS TRIGGER_ID ")
            .append("FROM EAM_REGISTERED_TRIGGER T ")
            .append("WHERE T.ALERT_DEFINITION_ID IN (:alertDefIds) ").toString();

        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> triggers = new ArrayList<Object[]>(alertDefIds.size());
        int batchSize = 1000;

        if (debug)
            watch.markTimeBegin("createQuery.list");

        for (int i = 0; i < alertDefIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, alertDefIds.size());
            List<Integer> list = alertDefIds.subList(i, end);
            query.setParameter("alertDefIds", list);
            triggers.addAll(query.getResultList());
        }

        if (debug)
            watch.markTimeEnd("createQuery.list");

        Map<Integer, List<Integer>> alertDefTriggerMap = new HashMap<Integer, List<Integer>>(
            alertDefIds.size());

        if (debug)
            watch.markTimeBegin("buildMap");

        for (Object[] o : triggers) {
            Integer alertDefId = (Integer) o[0];
            Integer triggerId = (Integer) o[1];

            List<Integer> trigList = alertDefTriggerMap.get(alertDefId);

            if (trigList == null) {
                trigList = new ArrayList<Integer>();
                alertDefTriggerMap.put(alertDefId, trigList);
            }
            trigList.add(triggerId);
        }

        if (debug) {
            watch.markTimeEnd("buildMap");
            log.debug("findTriggerIdsByAlertDefinitionIds: " + watch +
                      ", alert definition ids size=" + alertDefIds.size() + ", trigger ids size=" +
                      triggers.size() + ", map size=" + alertDefTriggerMap.size());
        }

        return alertDefTriggerMap;
    }
}
