/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ. HQ is free software; you can redistribute it and/or
 * modify it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 */
package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.type.IntegerType;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;


@Repository
public class TriggerDAO
    extends HibernateDAO<RegisteredTrigger> implements TriggerDAOInterface
{
    private final Log log = LogFactory.getLog(TriggerDAO.class);
    
    @Autowired
    public TriggerDAO(SessionFactory sessionFactory) {
        super(RegisteredTrigger.class, sessionFactory);
    }

    public RegisteredTrigger create(RegisteredTriggerValue createInfo) {
        RegisteredTrigger res = new RegisteredTrigger(createInfo);
        save(res);

        // Set the new ID just in case someone wants to use it
        createInfo.setId(res.getId());

        return res;
    }

    public RegisteredTrigger findById(Integer id) {
        return (RegisteredTrigger) super.findById(id);
    }

    public RegisteredTrigger get(Integer id) {
        return (RegisteredTrigger) super.get(id);
    }

    /**
     * Find all the registered triggers associated with the alert definition.
     *
     * @param id The alert definition id.
     * @return The list of associated registered triggers.
     */
    @SuppressWarnings("unchecked")
    public List<RegisteredTrigger> findByAlertDefinitionId(Integer id) {
        String sql = "from RegisteredTrigger rt where rt.alertDefinition.id = :defId";

        return getSession().createQuery(sql).setParameter("defId", id).list();
    }
    
    /**
     * Find all the registered trigger ids associated with the alert definition ids.
     *
     * @param alertDefIds The alert definition ids.
     * @return {@link Map} of alert definition id {@link Integer} 
     *          to {@link List} of trigger id {@link Integer}
     */
    @SuppressWarnings("unchecked")
    public Map<Integer,List<Integer>> findTriggerIdsByAlertDefinitionIds(List<Integer> alertDefIds) {
        if (alertDefIds.isEmpty()) {
            return new HashMap<Integer,List<Integer>>(0,1);
        }
 
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        final String sql = 
            new StringBuilder()
                    .append("SELECT T.ALERT_DEFINITION_ID AS ALERT_DEF_ID, ")
                    .append("T.ID AS TRIGGER_ID ")
                    .append("FROM EAM_REGISTERED_TRIGGER T ")
                    .append("WHERE T.ALERT_DEFINITION_ID IN (:alertDefIds) ")
                    .toString();
        
        Query query = getSession().createSQLQuery(sql)
                            .addScalar("ALERT_DEF_ID", new IntegerType())
                            .addScalar("TRIGGER_ID", new IntegerType());
        
        List<Object[]> triggers = new ArrayList<Object[]>(alertDefIds.size());
        int batchSize = 1000;
        
        if (debug) watch.markTimeBegin("createQuery.list");

        for (int i=0; i<alertDefIds.size(); i+=batchSize) {
            int end = Math.min(i+batchSize, alertDefIds.size());
            List<Integer> list = alertDefIds.subList(i, end);
            query.setParameterList("alertDefIds", list, new IntegerType());
            triggers.addAll(query.list());
        }

        if (debug) watch.markTimeEnd("createQuery.list");

        Map<Integer,List<Integer>> alertDefTriggerMap = new HashMap<Integer,List<Integer>>(alertDefIds.size());
        
        if (debug) watch.markTimeBegin("buildMap");

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
            log.debug("findTriggerIdsByAlertDefinitionIds: " + watch
                            + ", alert definition ids size=" + alertDefIds.size()
                            + ", trigger ids size=" + triggers.size()
                            + ", map size=" + alertDefTriggerMap.size());
        }
        
        return alertDefTriggerMap;
    }



    @SuppressWarnings("unchecked")
    public Set<RegisteredTrigger> findAllEnabledTriggers() {
        final boolean debug = log.isDebugEnabled();
        StopWatch watch = new StopWatch();
       
        // For performance optimization, we want to fetch each trigger's alert
        // def as well as the alert def's alert definition state and conditions
        // in a single query (as they will be used to create
        // AlertConditionEvaluators when creating trigger impls). This query
        // guarantees that when we do trigger.getAlertDefinition().getConditions(),
        // the database is not hit again
        String hql = new StringBuilder(256)
                    .append("from AlertDefinition ad ")
                     .append("join fetch ad.alertDefinitionState ")
                     .append("join fetch ad.conditionsBag c ")
                    .append("join fetch c.trigger ")
                    .append("where ad.enabled = '1'")
                    .append("and ad.deleted = '0' ")
                 .toString();
        if (debug) watch.markTimeBegin("createQuery.list");
        List<AlertDefinition> alertDefs = getSession().createQuery(hql).list();
        if (debug) watch.markTimeEnd("createQuery.list");  
        
        Set<RegisteredTrigger> triggers = new LinkedHashSet<RegisteredTrigger>();
        if (debug) watch.markTimeBegin("addTriggers");
        
        for(AlertDefinition definition : alertDefs) {
            for(AlertCondition condition: definition.getConditionsBag()) {
                triggers.add(condition.getTrigger());
            }
        }
        if (debug) {
            watch.markTimeEnd("addTriggers");
            log.debug("findAllEnabledTriggers: " + watch);
        }
        return triggers;
    }

}
