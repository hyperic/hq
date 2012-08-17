/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AlertDAO
    extends HibernateDAO<Alert> {

    private AlertActionLogDAO alertActionLogDAO;
    private PermissionManager permissionManager;

    @Autowired
    public AlertDAO(SessionFactory f, AlertActionLogDAO alertActionLogDAO, PermissionManager permissionManager) {
        super(Alert.class, f);
        this.alertActionLogDAO = alertActionLogDAO;
        this.permissionManager = permissionManager;
    }

    int deleteByIds(Integer[] ids) {
        String sql = "delete Alert where id in (:ids)";

        return getSession().createQuery(sql).setParameterList("ids", ids).executeUpdate();
    }

    public Alert get(Integer id) {
        return (Alert) super.get(id);
    }

    /**
     * @param before (in ms) - deletes all alerts with ctime < before
     * @param maxDeletes - max number of rows to delete.  if maxDeletes <= 0, 0 is returned
     * @return number of rows deleted
     */
    @SuppressWarnings("unchecked")
    int deleteAlertsByCreateTime(long before, int maxDeletes) {
        if (maxDeletes <= 0) {
            return 0;
        }
        // don't want to thrash the Alert cache, so select and do an explicit
        // remove() on each Object
        final String hql = new StringBuilder(64)
            .append("from Alert where ")
            .append("ctime < :before and ")
            .append("not id in (select alertId from EscalationState es ")
            .append("where alertTypeEnum = :type)")
            .toString();
        List<Alert> list = null;
        int count = 0;
        // due to
        // http://opensource.atlassian.com/projects/hibernate/browse/HHH-1985
        // need to batch this
        while (list == null || count < maxDeletes) {
            int batchSize = (BATCH_SIZE + count > maxDeletes) ? (maxDeletes - count) : BATCH_SIZE;
            list = getSession().createQuery(hql)
                .setLong("before", before)
                .setInteger("type", ClassicEscalationAlertType.CLASSIC.getCode())
                .setMaxResults(batchSize)
                .list();
            if (list.size() == 0) {
                break;
            }
            alertActionLogDAO.deleteAlertActions(list);
            for (Alert alert : list) {
                count++;
                remove(alert);
            }
            // need to flush or else the removed alerts won't be reflected in the next hql
            getSession().flush();
            if (count >= maxDeletes) {
                break;
            }
        }
        return count;
    }

    public List<Alert> findByResource(Resource res) {
        return findByResource(res, "a.ctime DESC");
    }

    @SuppressWarnings("unchecked")
    List<Alert> findEscalatables() {
        String sql = "from Alert a";

        return getSession().createQuery(sql).list();
    }

    /**
     * @return {@link List} of {@link Alert}s XXX scottmf [HQ-1785] this leads
     *         bloating the session when it queries too many alerts and causes
     *         an OOM. To fix we'd have to do something along the lines of only
     *         querying alertIds and then the caller would have to do a
     *         findById(alertId), process it, then immediately evict from the
     *         session
     */
    @SuppressWarnings("unchecked")
    List<Alert> findByCreateTimeAndPriority(Integer subj, long begin, long end, int priority,
                                            boolean inEsc, boolean notFixed, Integer groupId,
                                            Integer alertDefId, PageInfo pageInfo) {
       
        AlertSortField sort = (AlertSortField) pageInfo.getSort();
       

        String sql = PermissionManagerFactory.getInstance().getAlertsHQL(inEsc, notFixed, groupId,
            null, alertDefId, false) +
                     " order by " +
                     sort.getSortString("a", "d", "r") +
                     (pageInfo.isAscending() ? "" : " DESC");

        // If sorting by something other than date, do a secondary sort by
        // date, descending
        if (!sort.equals(AlertSortField.DATE)) {
            sql += ", " + AlertSortField.DATE.getSortString("a", "d", "r") + " DESC";
        }

        Query q = getSession().createQuery(sql).setLong("begin", begin).setLong("end", end).setInteger(
            "priority", priority);
        // HHQ-2781: acknowledgeable state is stale from query cache
        // .setCacheable(true)
        // .setCacheRegion("Alert.findByCreateTime");

        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subj.intValue()).setParameterList("ops", AuthzConstants.VIEW_ALERTS_OPS);
        }

        return pageInfo.pageResults(q).list();
    }

    Map<Integer,List<Alert>> getUnfixedByResource(Integer subj, long begin, long end, int priority,
                                                  boolean inEsc, boolean notFixed)
    {
        String sql = PermissionManagerFactory.getInstance().getAlertsHQL(inEsc, notFixed, null,
                                                                         null, null, false);

        Query q = getSession().createQuery(sql)
                .setLong("begin", begin)
                .setLong("end", end)
                .setInteger("priority", priority);

        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subj.intValue()).setParameterList("ops", AuthzConstants.VIEW_ALERTS_OPS);
        }

        List<Alert> alerts = q.list();

        Map<Integer,List<Alert>> lastAlerts = new HashMap<Integer,List<Alert>>();
        for (Alert a : alerts ) {
            List<Alert> alertsByResource = lastAlerts.get(a.getAlertDefinition().getResource().getId());
            if (alertsByResource == null) {
                alertsByResource = new ArrayList<Alert>();
                lastAlerts.put(a.getAlertDefinition().getResource().getId(), alertsByResource);
            }
            alertsByResource.add(a);
        }

        return lastAlerts;
    }

    @SuppressWarnings("unchecked")
    public List<Alert> findByAppdefEntityInRange(Resource res, long begin, long end,
                                                 boolean nameSort, boolean asc) {
        String sql = "from Alert a where a.alertDefinition.resource = :res " +
                     "and a.ctime between :begin and :end order by " +
                     (nameSort ? "a.alertDefinition.name" : "a.ctime") + (asc ? " asc" : " desc");

        return getSession().createQuery(sql).setParameter("res", res).setLong("begin", begin)
            .setLong("end", end).list();
    }

    @SuppressWarnings("unchecked")
    private List<Alert> findByResource(Resource res, String orderBy) {
        String sql = "from Alert a WHERE a.alertDefinition.resource = :res " + "ORDER BY " +
                     orderBy;

        return getSession().createQuery(sql).setParameter("res", res).setCacheable(true)
            .setCacheRegion("Alert.findByEntity").list();
    }

    public List<Alert> findByResourceSortByAlertDef(Resource res) {
        return findByResource(res, "a.alertDefinition.name DESC");
    }

    public Alert findByAlertDefinitionAndCtime(AlertDefinition def, long ctime) {
        String sql = "from Alert a WHERE a.alertDefinition = :alertDef " + "and a.ctime = :ctime";

        return (Alert) getSession().createQuery(sql).setParameter("alertDef", def).setLong("ctime",
            ctime).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<Alert> findByAlertDefinition(AlertDefinition def) {
        String sql = "from Alert a WHERE a.alertDefinition = :alertDef";

        return getSession().createQuery(sql).setParameter("alertDef", def).list();
    }

    public Alert findLastByDefinition(AlertDefinition def, boolean fixed) {
        try {
            return (Alert) createCriteria().add(Restrictions.eq("alertDefinition", def)).add(
                Restrictions.eq("fixed", new Boolean(fixed))).addOrder(Order.desc("ctime"))
                .setMaxResults(1).uniqueResult();
        } catch (Exception e) {
            return null;
        }
    }
    
    public Alert findLastByDefinition(AlertDefinition def) {
        try {
            return (Alert) createCriteria()
                .add(Restrictions.eq("alertDefinition", def))
                .addOrder(Order.desc("ctime"))
                .setMaxResults(1)
                .uniqueResult();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Return all last unfixed alerts
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<Integer,Alert> findAllLastUnfixed() {
        String hql = 
            new StringBuilder()
                    .append("select a ")
                    .append("from Alert a ")
                    .append("join a.alertDefinition ad ")
                    .append("where ad.deleted = false ")
                    .append("and a.fixed = false ")
                    .append("order by a.ctime ")
                    .toString();
                
        List<Alert> alerts = createQuery(hql).list();
                
        Map<Integer,Alert> lastAlerts = new HashMap<Integer,Alert>(alerts.size());
        for (Alert a : alerts ) {
            // since it is ordered by ctime in ascending order, the
            // last alert will eventually be put into the map
            lastAlerts.put(a.getAlertDefinition().getId(), a);
        }
        
        return lastAlerts;
    }
    
        /**
         * Return all last fixed alerts for the given resource
         * 
         * @param subject The HQ user
         * @param r The root resource
         * @param fixed Boolean to indicate whether to get fixed or unfixed alerts
         * @return
         */
        @SuppressWarnings("unchecked")
        public Map<Integer,Alert> findLastByResource(Collection<Resource> resources, boolean fixed) {
            final String hql = new StringBuilder(256)
                .append("select max(a.ctime), a ")
                .append("from Alert a ")
                .append("join a.alertDefinition ad ")
                .append("where ad.resource in (:resources) ")
                .append("and ad.deleted = false ")
                .append("and a.fixed = :fixed ")
                .append("group by a")
                .toString();
            final List<Object[]> alerts = createQuery(hql)
                .setBoolean("fixed", fixed)
                .setParameterList("resources", resources)
                .list();
            final Map<Integer,Alert> lastAlerts = new HashMap<Integer,Alert>();
            for (final Object[] o : alerts ) {
                final Alert a = (Alert) o[1];
                lastAlerts.put(a.getAlertDefinition().getId(), a);
            }
            return lastAlerts;
        }

        
        /**
         * @param {@link List} of {@link AlertDefinition}s
         * Deletes all {@link Alert}s associated with the {@link AlertDefinition}s
         */
        int deleteByAlertDefinitions(List<AlertDefinition> alertDefs) {
            String sql = "DELETE FROM Alert WHERE alertDefinition in (:alertDefs)";
            int rtn = 0;
            for (int i=0; i<alertDefs.size(); i+=BATCH_SIZE) {
                int end = Math.min(i+BATCH_SIZE, alertDefs.size());
                rtn += getSession().createQuery(sql)
                    .setParameterList("alertDefs", alertDefs.subList(i, end))
                   .executeUpdate();
            }
            return rtn;
        }
         
        /**
         * Deletes all {@link Alert}s associated with the {@link AlertDefinition}
         */
    int deleteByAlertDefinition(AlertDefinition def) {
        String sql = "DELETE FROM Alert WHERE alertDefinition = :alertDef";

        return getSession().createQuery(sql).setParameter("alertDef", def).executeUpdate();
    }

    public Integer countAlerts(AlertDefinition def) {
        return (Integer) createCriteria().add(Restrictions.eq("alertDefinition", def))
            .setProjection(Projections.rowCount()).uniqueResult();
    }

    public Integer countAlerts(Resource res) {
        return (Integer) createCriteria().createAlias("alertDefinition", "d").add(
            Restrictions.eq("d.resource", res)).setProjection(Projections.rowCount())
            .uniqueResult();
    }

    public void save(Alert alert) {
        super.save(alert);

        AlertDefinition def = alert.getAlertDefinition();

        // Update the last fired time
        if (def.getLastFired() < alert.getCtime())
            def.setLastFired(alert.getCtime());
    }
}
