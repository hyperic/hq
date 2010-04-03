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

import java.util.List;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AlertDAO
    extends HibernateDAO<Alert> {

    private AlertActionLogDAO alertActionLogDAO;

    @Autowired
    public AlertDAO(SessionFactory f, AlertActionLogDAO alertActionLogDAO) {
        super(Alert.class, f);
        this.alertActionLogDAO = alertActionLogDAO;
    }

    int deleteByIds(Integer[] ids) {
        String sql = "delete Alert where id in (:ids)";

        return getSession().createQuery(sql).setParameterList("ids", ids).executeUpdate();
    }

    public Alert get(Integer id) {
        return (Alert) super.get(id);
    }

    @SuppressWarnings("unchecked")
    int deleteByCreateTime(long begin, long end) {
        // don't want to thrash the Alert cache, so select and do an explicit
        // remove() on each Object
        final String sql = new StringBuilder().append("from Alert where ").append(
            "ctime between :timeStart and :timeEnd and ").append(
            "not id in (select alertId from EscalationState es ").append(
            "where alertTypeEnum = :type)").toString();

        List<Alert> list = null;
        int rtn = 0;
        // due to
        // http://opensource.atlassian.com/projects/hibernate/browse/HHH-1985
        // need to batch this
        while (list == null || list.size() > 0) {
            list = (List<Alert>) getSession().createQuery(sql).setLong("timeStart", begin).setLong(
                "timeEnd", end).setInteger("type", ClassicEscalationAlertType.CLASSIC.getCode())
                .setMaxResults(1000).list();
            alertActionLogDAO.deleteAlertActions(list);
            for (Alert alert : list) {
                rtn++;
                remove(alert);
            }
        }
        return rtn;
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
        String[] ops = new String[] { AuthzConstants.platformOpManageAlerts,
                                     AuthzConstants.serverOpManageAlerts,
                                     AuthzConstants.serviceOpManageAlerts };
        AlertSortField sort = (AlertSortField) pageInfo.getSort();
        Query q;

        String sql = PermissionManagerFactory.getInstance().getAlertsHQL(inEsc, notFixed, groupId,
            alertDefId, false) +
                     " order by " +
                     sort.getSortString("a", "d", "r") +
                     (pageInfo.isAscending() ? "" : " DESC");

        // If sorting by something other than date, do a secondary sort by
        // date, descending
        if (!sort.equals(AlertSortField.DATE)) {
            sql += ", " + AlertSortField.DATE.getSortString("a", "d", "r") + " DESC";
        }

        q = getSession().createQuery(sql).setLong("begin", begin).setLong("end", end).setInteger(
            "priority", priority);
        // HHQ-2781: acknowledgeable state is stale from query cache
        // .setCacheable(true)
        // .setCacheRegion("Alert.findByCreateTime");

        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subj.intValue()).setParameterList("ops", ops);
        }

        return pageInfo.pageResults(q).list();
    }

    Number countByCreateTimeAndPriority(Integer subj, long begin, long end, int priority,
                                         boolean inEsc, boolean notFixed, Integer groupId,
                                         Integer alertDefId) {
        String[] ops = new String[] { AuthzConstants.platformOpManageAlerts,
                                     AuthzConstants.serverOpManageAlerts,
                                     AuthzConstants.serviceOpManageAlerts };
        Query q;

        String sql = PermissionManagerFactory.getInstance().getAlertsHQL(inEsc, notFixed, groupId,
            alertDefId, true);

        q = getSession().createQuery(sql).setLong("begin", begin).setLong("end", end).setInteger(
            "priority", priority);

        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subj.intValue()).setParameterList("ops", ops);
        }

        return (Number) q.uniqueResult();
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
